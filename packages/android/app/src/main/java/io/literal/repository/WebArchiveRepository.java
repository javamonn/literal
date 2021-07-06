package io.literal.repository;

import android.content.Context;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.BodyPartBuilder;
import org.apache.james.mime4j.message.SingleBodyBuilder;
import org.apache.james.mime4j.stream.RawField;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.literal.model.HTMLScriptElement;
import io.literal.model.ParcelableWebResourceRequest;
import io.literal.model.StorageObject;
import io.literal.model.WebArchive;
import io.literal.ui.view.SourceWebView.SourceWebView;
import io.literal.ui.view.SourceWebView.Client;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class WebArchiveRepository {

    private static final OkHttpClient client = new OkHttpClient();

    public static CompletableFuture<WebArchive> capture(Context context, SourceWebView webView, ArrayList<HTMLScriptElement> scriptElements) {
        StorageObject archive = WebArchiveRepository.createArchiveStorageObject();
        ArrayList<ParcelableWebResourceRequest> webResourceRequests = new ArrayList<>(
                webView
                        .getSource()
                        .map(s -> s.getPageWebResourceRequests().stream().map(ParcelableWebResourceRequest::new)
                        .collect(Collectors.toList()))
                        .orElse(new ArrayList<>())
        );

        return WebViewRepository.saveWebArchive(webView, archive.getFile(context).toURI().getPath(), false)
                .thenApply(_archive -> {
                    archive.setStatus(StorageObject.Status.UPLOAD_REQUIRED);
                    return new WebArchive(archive, webResourceRequests, scriptElements);
                });
    }

    public static StorageObject createArchiveStorageObject() {
        String key = UUID.randomUUID().toString() + "/archive.mhtml";
        return new StorageObject(StorageObject.Type.ARCHIVE, key, StorageObject.Status.MEMORY_ONLY, null, null);
    }

    public static Optional<BodyPart> createBodyPart(WebResourceRequest webResourceRequest, ResponseBody webResourceResponse) {
        try {
            Optional<MediaType> mediaType = Optional.ofNullable(webResourceResponse.contentType());
            Charset charset = mediaType.flatMap(m -> Optional.ofNullable(m.charset(null))).orElse(StandardCharsets.UTF_8);
            String contentType = mediaType.map(m -> m.type() + "/" + m.subtype()).orElse("text/html");
            String contentTransferEncoding = mediaType.map(m -> {
                if (
                        m.type().equals("text") ||
                                (m.type().equals("application") && m.subtype().equals("javascript")) ||
                                (m.type().equals("image") && m.subtype().startsWith("svg"))
                ) {
                    return "quoted-printable";
                }
                return "base64";
            }).orElse("quoted-printable");

            return Optional.of(
                    BodyPartBuilder.create()
                            .setBody(
                                    SingleBodyBuilder.create()
                                            .readFrom(webResourceResponse.byteStream())
                                            .setCharset(charset)
                                            .build()
                            )
                            .setContentType(contentType)
                            .setContentTransferEncoding(contentTransferEncoding)
                            .setField(new RawField("Content-Location", webResourceRequest.getUrl().toString()))
                            .build()
            );
        } catch (Exception e) {
            ErrorRepository.captureException(e);
            return Optional.empty();
        }
    }

    public static CompletableFuture<ResponseBody> executeWebResourceRequest(WebResourceRequest webResourceRequest) {
        Request request = new Request.Builder()
                .url(webResourceRequest.getUrl().toString())
                .headers(Headers.of(webResourceRequest.getRequestHeaders()))
                .build();
        CompletableFuture<ResponseBody> future = new CompletableFuture<>();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful())
                        throw new IOException("Unexpected code: " + response);

                    future.complete(responseBody);
                }
            }
        });

        return future;
    }
}
