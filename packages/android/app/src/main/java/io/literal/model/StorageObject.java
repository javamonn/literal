package io.literal.model;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.Pair;
import android.webkit.MimeTypeMap;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import io.literal.lib.Callback;
import io.literal.lib.Callback3;
import io.literal.lib.ContentResolverLib;
import io.literal.repository.ErrorRepository;
import io.literal.repository.StorageRepository;
import kotlin.jvm.functions.Function3;

public class StorageObject implements Parcelable {
    public enum Type {
        ARCHIVE,
        SCREENSHOT;
    }
    public enum Status {
        SYNCHRONIZED,
        UPLOAD_REQUIRED,
        DOWNLOAD_REQUIRED,
        MEMORY_ONLY
    }
    public static final String CACHE_CONTROL = "public, max-age=604800, immutable";
    public static String KEY_ID = "KEY_ID";
    public static String KEY_TYPE = "KEY_TYPE";
    public static String KEY_STATUS = "KEY_STATUS";
    public static String KEY_FILE = "KEY_FILE";
    public static String KEY_URI = "KEY_URI";
    public static String KEY_CONTENT_TYPE = "KEY_CONTENT_TYPE";

    private static final Pattern ARCHIVES_PATH_PATTERN = Pattern.compile(".+/archives/(.+)");
    private static final Pattern SHARED_PUBLIC_READ_PATTERN = Pattern.compile("shared-public-read/onboarding/(.+)");
    private static final Pattern SCREENSHOT_PATH_PATTERN = Pattern.compile(".+/screenshots/(.+)");

    private Type type;
    private String id;
    private Status status;
    private File file;
    private AmazonS3URI uri;
    private Format contentType;

    public StorageObject(Type type, String id, Status status, File file, AmazonS3URI uri) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.file = file;
        this.uri = uri;
    }

    protected StorageObject(Parcel in) {
        Bundle input = new Bundle();
        input.readFromParcel(in);

        this.id = input.getString(KEY_ID);
        this.type = (Type) input.getSerializable(KEY_TYPE);
        this.status = (Status) input.getSerializable(KEY_STATUS);
        this.file = new File(input.getString(KEY_FILE));
        this.uri = new AmazonS3URI(input.getString(KEY_URI));
        this.contentType = (Format) input.getSerializable(KEY_CONTENT_TYPE);

    }

    public static final Creator<StorageObject> CREATOR = new Creator<StorageObject>() {
        @Override
        public StorageObject createFromParcel(Parcel in) {
            return new StorageObject(in);
        }

        @Override
        public StorageObject[] newArray(int size) {
            return new StorageObject[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle output = new Bundle();
        output.putString(KEY_ID, id);
        output.putSerializable(KEY_TYPE, type);
        output.putSerializable(KEY_STATUS, status);
        output.putString(KEY_FILE, file.getAbsolutePath());
        output.putString(KEY_URI, uri.toString());
        output.putSerializable(KEY_CONTENT_TYPE, contentType);
        output.writeToParcel(dest, flags);
    }

    public static StorageObject create(File file, Status status) {
        Pair<Type, String> parsedPath = parsePath(file.getPath());
        if (parsedPath == null) {
            return null;
        }
        return new StorageObject(parsedPath.first, parsedPath.second, status, file, null);
    }

    public static StorageObject create(AmazonS3URI uri, Status status) {
        Pair<Type, String> parsedPath = parsePath(uri.getKey());
        if (parsedPath == null) {
            return null;
        }
       return new StorageObject(parsedPath.first, parsedPath.second, status, null, uri);
    }

    public static StorageObject createFromContentResolverURI(Context context, StorageObject.Type type, Uri uri) {
        String id = UUID.randomUUID().toString();
        File file = ContentResolverLib.toFile(
                context,
                StorageObject.getDirectory(context, type),
                uri,
                id
        );
        Format contentType = Format.fromMimeType(context.getContentResolver().getType(uri));
        StorageObject storageObject = new StorageObject(type, id, Status.UPLOAD_REQUIRED, file, null);
        storageObject.setContentType(contentType);

        return storageObject;
    }

    public static StorageObject create(@Nullable Context context, URI uri) {
        try {
            if (uri.getScheme().equals("file")) {
                // Ensure file is within the application cache directory.
                if (context != null && !uri.getPath().startsWith(context.getCacheDir().toURI().getPath())) {
                    return null;
                }

                return create(new File(uri), Status.UPLOAD_REQUIRED);
            } else if (uri.getScheme().equals("https") || uri.getScheme().equals("s3")) {
                AmazonS3URI amazonS3URI = new AmazonS3URI(uri);
                // Ensure S3 URI is within the application bucket.
                if (context != null && !amazonS3URI.getBucket().equals(StorageRepository.getBucketName(context))) {
                    return null;
                }

                return create(new AmazonS3URI(uri), Status.DOWNLOAD_REQUIRED);
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static StorageObject create(URI uri) {
        return StorageObject.create(null, uri);
    }

    public static Optional<StorageObject> create(Context context, Uri uri) {
        StorageObject inst = null;
        try {
            inst = StorageObject.create(context, new URI(uri.toString()));
        } catch (URISyntaxException e) {
            ErrorRepository.captureException(e);
        }

        return Optional.ofNullable(inst);
    }

    public void ensureDownloadRequired(Context context) {
        if (status.equals(Status.DOWNLOAD_REQUIRED) && getFile(context).exists()) {
            setStatus(Status.SYNCHRONIZED);
        }
        setStatus(Status.DOWNLOAD_REQUIRED);
    }

    private static Pair<Type, String> parsePath(String path) {
        Matcher archiveMatcher = ARCHIVES_PATH_PATTERN.matcher(path);
        Matcher screenshotMatcher = SCREENSHOT_PATH_PATTERN.matcher(path);
        Matcher sharedPublicReadMatcher = SHARED_PUBLIC_READ_PATTERN.matcher(path);

        if (archiveMatcher.find()) {
            return new Pair<>(Type.ARCHIVE, archiveMatcher.group(1));
        } else if (screenshotMatcher.find()) {
            return new Pair<>(Type.SCREENSHOT, screenshotMatcher.group(1));
        } else if (sharedPublicReadMatcher.find()) {
            return new Pair<>(Type.ARCHIVE, sharedPublicReadMatcher.group(1));
        }

        return null;
    }

    public String getPath() {
        if (type.equals(Type.ARCHIVE)) {
            return "archives" + File.separator + id;
        } else if (type.equals(Type.SCREENSHOT)){
            return "screenshots" + File.separator + id;
        } else {
            return null;
        }
    }

    public static File getDirectory(Context context, Type type) {
        String suffix;
        if (type.equals(Type.ARCHIVE)) {
            suffix = "archives";
        } else if (type.equals(Type.SCREENSHOT)) {
            suffix = "screenshots";
        } else {
            return null;
        }

        File file = new File(context.getCacheDir(), suffix);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    public Status getStatus() {
        return status;
    }
    public void setStatus(Status status) {
        this.status = status;
    }

    public String getId() { return id; }

    public Type getType() { return type; }

    public File getFile(Context context) {
        if (file == null) {
            file = new File(context.getCacheDir(), getPath());
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
        }
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public boolean deleteFile(Context context) {
        if (status.equals(Status.DOWNLOAD_REQUIRED)) {
            return false;
        }

        return getFile(context).delete();
    }

    public AmazonS3URI getAmazonS3URI(Context context, User user) {
        if (uri == null) {
            uri = StorageRepository.getPrivateUri(context, user, getPath());
        }
        return uri;
    }

    public URI getCanonicalURI(Context context, User user) {
        if (status.equals(Status.UPLOAD_REQUIRED)) {
            return getFile(context).toURI();
        } else {
            return getAmazonS3URI(context, user).getURI();
        }
    }

    public URI getCanonicalURI() {
        if (status.equals(Status.UPLOAD_REQUIRED) && file != null) {
            return file.toURI();
        } else if (uri != null) {
            return uri.getURI();
        }
        return null;
    }

    public void setContentType(Format contentType) {
        this.contentType = contentType;
    }

    public Format getContentType(Context context) {
        if (contentType == null) {
            if (type.equals(Type.ARCHIVE) && id.endsWith(".mhtml")) {
                contentType = Format.MULTIPART_RELATED;
            } else if (type.equals(Type.ARCHIVE)) {
                contentType = Format.TEXT_HTML;
            } if (type.equals(Type.SCREENSHOT)) {
                String extension = MimeTypeMap.getFileExtensionFromUrl(getFile(context).toURI().toString());
                String mimeType =  MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                if (contentType != null) {
                    contentType = Format.fromMimeType(mimeType);
                } else {
                    contentType = Format.APPLICATION_OCTET_STREAM;
                }
            }
        }
        return contentType;
    }

    public ObjectMetadata getObjectMetadata(Context context) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setCacheControl(CACHE_CONTROL);
        metadata.setContentType(getContentType(context).toMimeType());
        return metadata;
    }

    public CompletableFuture<AmazonS3URI> upload(Context context, User user, Function3<Integer, Long, Long, Void> onUploadProgress) {
        AmazonS3URI uri = getAmazonS3URI(context, user);
        CompletableFuture<AmazonS3URI> future = new CompletableFuture<>();
        StorageRepository.upload(
                context,
                uri.getKey(),
                getFile(context),
                getObjectMetadata(context),
                (e, uri1) -> {
                    if (e != null) {
                        future.completeExceptionally(e);
                    } else {
                        future.complete(uri1);
                    }
                    return null;
                },
                onUploadProgress
        );

        return future;
    }

    public TransferObserver upload(Context context, User user, Callback<Exception, AmazonS3URI> onUploadComplete, Function3<Integer, Long, Long, Void> onUploadProgress) {
        AmazonS3URI uri = getAmazonS3URI(context, user);
        return StorageRepository.upload(
                context,
                uri.getKey(),
                getFile(context),
                getObjectMetadata(context),
                (e, uri1) -> {
                    if (e == null) {
                        setStatus(Status.SYNCHRONIZED);
                    }
                    onUploadComplete.invoke(e, uri1);
                    return null;
                },
                onUploadProgress
        );
    }

    public CompletableFuture<Void> download(Context context, User user) {
        ensureDownloadRequired(context);
        if (status.equals(Status.UPLOAD_REQUIRED) || status.equals(Status.SYNCHRONIZED)) {
            return CompletableFuture.completedFuture(null);
        }

        if (status.equals(Status.MEMORY_ONLY)) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new Exception("Invalid state: unable to download StorageObject with status MEMORY_ONLY."));
            return future;
        }

        AmazonS3URI uri = getAmazonS3URI(context, user);
        return StorageRepository.download(context, uri.getKey(), getFile(context))
                .whenComplete((_void, e) -> {
                    if (e == null) {
                        setStatus(Status.SYNCHRONIZED);
                    }
                });
    }
}
