package io.literal.model;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.webkit.WebResourceRequest;

import androidx.annotation.NonNull;

import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.SingleBody;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.BodyPartBuilder;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.message.DefaultMessageWriter;
import org.apache.james.mime4j.message.SingleBodyBuilder;
import org.apache.james.mime4j.stream.Field;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.literal.repository.ErrorRepository;
import io.literal.repository.WebArchiveRepository;

public class WebArchive implements Parcelable {
    public static String KEY_WEB_REQUESTS = "WEB_REQUESTS";
    public static String KEY_SCRIPT_ELEMENTS = "SCRIPT_ELEMENTS";
    public static String KEY_STORAGE_OBJECT = "STORAGE_OBJECT";
    public static String KEY_ID = "ID";

    private final StorageObject storageObject;
    private final ArrayList<ParcelableWebResourceRequest> webRequests;
    private final ArrayList<HTMLScriptElement> scriptElements;
    private String id;
    private Message mimeMessage;
    private HashMap<String, BodyPart> bodyPartByContentLocation;

    public WebArchive(
            @NotNull StorageObject storageObject,
            @NotNull ArrayList<ParcelableWebResourceRequest> webRequests,
            @NotNull ArrayList<HTMLScriptElement> scriptElements
    ) {
        this.storageObject = storageObject;
        this.webRequests = webRequests;
        this.scriptElements = scriptElements;
        this.mimeMessage = null;
    }

    public WebArchive(@NotNull StorageObject storageObject) {
        this.storageObject = storageObject;
        this.webRequests = new ArrayList<>();
        this.scriptElements = new ArrayList<>();
        this.mimeMessage = null;
    }

    protected WebArchive(Parcel in) {
        Bundle input = new Bundle();
        input.setClassLoader(getClass().getClassLoader());
        input.readFromParcel(in);

        this.scriptElements = input.getParcelableArrayList(KEY_SCRIPT_ELEMENTS);
        this.webRequests = input.getParcelableArrayList(KEY_WEB_REQUESTS);
        this.storageObject = input.getParcelable(KEY_STORAGE_OBJECT);
        this.id = input.getString(KEY_ID);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle output = new Bundle();
        output.putParcelableArrayList(KEY_WEB_REQUESTS, webRequests);
        output.putParcelableArrayList(KEY_SCRIPT_ELEMENTS, scriptElements);
        output.putParcelable(KEY_STORAGE_OBJECT, storageObject);
        if (id != null) {
            output.putString(KEY_ID, id);
        }
        output.writeToParcel(dest, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<WebArchive> CREATOR = new Creator<WebArchive>() {
        @Override
        public WebArchive createFromParcel(Parcel in) {
            return new WebArchive(in);
        }

        @Override
        public WebArchive[] newArray(int size) {
            return new WebArchive[size];
        }
    };

    public StorageObject getStorageObject() {
        return storageObject;
    }
    public List<ParcelableWebResourceRequest> getWebRequests() {
        return webRequests;
    }
    public List<HTMLScriptElement> getScriptElements() {
        return scriptElements;
    }

    public CompletableFuture<Void> open(Context context, User user) {
        return storageObject.download(context, user)
                .thenCompose((_void) -> {
                    CompletableFuture<Void> future = new CompletableFuture<>();
                    FileInputStream fileInputStream = null;
                    try {
                        fileInputStream = new FileInputStream(storageObject.getFile(context));
                        DefaultMessageBuilder messageBuilder = new DefaultMessageBuilder();
                        mimeMessage = messageBuilder.parseMessage(fileInputStream);
                        buildMimeBodyPartIndex(context, user);
                    } catch (Exception innerException) {
                        future.completeExceptionally(innerException);
                        return future;
                    } finally {
                       if (fileInputStream != null) {
                           try {
                               fileInputStream.close();
                           } catch (IOException ioException) {
                               ErrorRepository.captureException(ioException);
                           }
                       }
                    }
                    future.complete(null);
                    return future;
                });
    }

    private void buildMimeBodyPartIndex(Context context, User user) {
        if (mimeMessage == null) {
            ErrorRepository.captureException(new Exception("Invalid state: attempted to build mime body part index, but mimeMessage is null."));
            return;
        }

        Multipart multipart = (Multipart) mimeMessage.getBody();
        List<Entity> bodyParts = multipart.getBodyParts();
        bodyPartByContentLocation = bodyParts.stream().collect(
                HashMap::new,
                (agg, bodyPart) -> {
                    Field contentLocation = bodyPart.getHeader().getField("Content-Location");
                    if (contentLocation == null) {
                        ErrorRepository.captureWarning(new Exception("Invalid state: unable to find content-location header in body part."));
                        return;
                    }
                    agg.put(contentLocation.getBody(), (BodyPart) bodyPart);
                },
                HashMap::putAll
        );

        // Alias the canonical archive URI with the primary index document.
        bodyPartByContentLocation.put(
                storageObject.getAmazonS3URI(context, user).toString(),
                (BodyPart) bodyParts.get(0)
        );
    }
    public CompletableFuture<WebArchive> compile(Context context, User user) {
        if (this.getScriptElements().size() == 0 && this.getWebRequests().size() == 0) {
            return CompletableFuture.completedFuture(null);
        }

        return this.open(context, user)
                .thenCompose(_file -> {
                    // For each web request not currently within the archive, execute it and build a BodyPart

                    HashMap<String, BodyPart> bodyPartByContentLocation = getBodyPartByContentLocation();
                    List<CompletableFuture<Optional<BodyPart>>> webRequestBodyPartFutures = getWebRequests()
                            .stream()
                            .filter(webResourceRequest -> !bodyPartByContentLocation.containsKey(webResourceRequest.getUrl().toString()))
                            .map(webResourceRequest ->
                                    WebArchiveRepository.executeWebResourceRequest(webResourceRequest)
                                            .thenApply((responseBody) -> WebArchiveRepository.createBodyPart(webResourceRequest, responseBody))
                            )
                            .collect(Collectors.toList());

                    return CompletableFuture.allOf(webRequestBodyPartFutures.toArray(new CompletableFuture[0]))
                            .thenApply(_void -> {;
                                return webRequestBodyPartFutures.stream()
                                        .map((f) -> f.getNow(Optional.empty()).orElse(null))
                                        .filter(f -> !Objects.isNull(f))
                                        .collect(Collectors.toList());
                            });
                })
                .thenApply((bodyParts) -> {
                    // Add the constructed web request body parts into the mime message, and update the primary
                    // index body part with built script elements
                    Multipart multipart = (Multipart) mimeMessage.getBody();
                    bodyParts.forEach(multipart::addBodyPart);
                    BodyPart updatedIndexBodyPart = addScriptsToBodyPart((BodyPart) multipart.getBodyParts().get(0), getScriptElements());
                    multipart.replaceBodyPart(updatedIndexBodyPart, 0);

                    // Write the updated mime message to disk
                    StorageObject updatedWebArchive = WebArchiveRepository.createArchiveStorageObject();
                    FileOutputStream updatedWebArchiveOutputStream = null;
                    try {
                        updatedWebArchiveOutputStream = new FileOutputStream(updatedWebArchive.getFile(context));
                        DefaultMessageWriter writer = new DefaultMessageWriter();
                        writer.writeMessage(mimeMessage, updatedWebArchiveOutputStream);
                        storageObject.setStatus(StorageObject.Status.UPLOAD_REQUIRED);

                        return new WebArchive(updatedWebArchive);
                    } catch (Exception e) {
                        ErrorRepository.captureException(e);
                    } finally {
                        if (updatedWebArchiveOutputStream != null) {
                            try {
                                updatedWebArchiveOutputStream.close();
                            } catch (Exception e1) {
                                ErrorRepository.captureException(e1);
                            }
                        }
                    }
                    return null;
                });
    }

    private BodyPart addScriptsToBodyPart(BodyPart bodyPart, List<HTMLScriptElement> scriptElements) {
        try {
            SingleBody body = (SingleBody) bodyPart.getBody();
            String html = IOUtils.toString(body.getInputStream(), StandardCharsets.UTF_8);
            int closeHeadElemIdx = html.indexOf("</body>");
            if (closeHeadElemIdx == -1) {
                ErrorRepository.captureException(new Exception("Unable to locate index of head element within archive."));
                return bodyPart;
            }

            int stringBuilderInitialCapacity = scriptElements.stream().map(s -> s.getText().length()).reduce(0, Integer::sum) + (scriptElements.size() * 24);
            StringBuilder scriptsStringBuilder = new StringBuilder(stringBuilderInitialCapacity);
            scriptElements.forEach((scriptElement -> {
                scriptElement.appendToStringBuilder(scriptsStringBuilder);
                scriptsStringBuilder.append("\n");
            }));
            StringBuilder htmlStringBuilder = new StringBuilder(html);
            htmlStringBuilder.insert(closeHeadElemIdx, scriptsStringBuilder.toString());

            BodyPartBuilder bodyPartBuilder = BodyPartBuilder.create()
                    .setBody(SingleBodyBuilder.createCopy(body).setText(htmlStringBuilder.toString()).build())
                    .setContentDisposition(bodyPart.getDispositionType())
                    .setContentTransferEncoding(bodyPart.getContentTransferEncoding())
                    .setContentType(bodyPart.getMimeType());
            bodyPart.getHeader().getFields().forEach(bodyPartBuilder::setField);

            return bodyPartBuilder.build();
        } catch (Exception e) {
            ErrorRepository.captureException(e);
            return bodyPart;
        }
    }

    public static WebArchive fromURI(URI uri) {
        StorageObject storageObject = StorageObject.create(uri);
        if (storageObject == null) {
            return null;
        }

        return new WebArchive(storageObject);
    }

    public HashMap<String, BodyPart> getBodyPartByContentLocation() {
        return bodyPartByContentLocation;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
