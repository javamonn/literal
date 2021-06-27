package io.literal.ui.view.SourceWebView;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazonaws.services.s3.model.ObjectMetadata;

import org.apache.james.mime4j.dom.SingleBody;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.PKIXRevocationChecker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.literal.lib.DomainMetadata;
import io.literal.lib.JsonArrayUtil;
import io.literal.model.Annotation;
import io.literal.model.StorageObject;
import io.literal.model.User;
import io.literal.model.WebArchive;
import io.literal.repository.ErrorRepository;
import io.literal.repository.StorageRepository;
import io.literal.viewmodel.AuthenticationViewModel;
import io.literal.viewmodel.SourceWebViewViewModel;
import io.sentry.Sentry;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;

public class Client extends WebViewClient {
    private final Context context;
    private final User user;
    private final Source source;
    private final Function1<Source, Void> onSourceChanged;
    private final Function1 <WebView, CompletableFuture<Void>> onInjectAnnotationRendererScript;
    private final Function1<WebResourceRequest, Void> onWebResourceRequest;
    private final Function1<Bitmap, Void> onReceivedIcon;

    private boolean shouldClearHistoryOnPageFinished = false;
    private boolean hasInjectedAnnotationRendererScript = false;

    public Client(
            @NonNull Context context,
            @NonNull Source source, User user,
            @NonNull Function1<Source, Void> onSourceChanged,
            @NonNull Function1<WebResourceRequest, Void> onWebResourceRequest,
            @NonNull Function1<WebView, CompletableFuture<Void>> onInjectAnnotationRendererScript,
            @NonNull Function1<Bitmap, Void> onReceivedIcon
    ) {
        this.context = context;
        this.source = source;
        this.user = user;
        this.onSourceChanged = onSourceChanged;
        this.onWebResourceRequest = onWebResourceRequest;
        this.onInjectAnnotationRendererScript = onInjectAnnotationRendererScript;
        this.onReceivedIcon = onReceivedIcon;
    }

    @Nullable
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        if (request == null || !request.getMethod().equals("GET")) {
            return null;
        }

        Log.i("shouldInterceptRequest", "webResourceRequest: " + request.getUrl());

        onWebResourceRequest.invoke(request);

        if (source.getType().equals(Source.Type.EXTERNAL_SOURCE) || !source.getWebArchive().isPresent()) {
            return null;
        }

        Log.i("shouldInterceptRequest", "getting webarchive");
        WebArchive webArchive = source.getWebArchive().get();
        Log.i("shouldInterceptRequest", "got webarchive");
        try {
            webArchive.open(context, user).get();

            Optional<WebResourceResponse> response = Optional.ofNullable(webArchive.getBodyPartByContentLocation().get(request.getUrl().toString()))
                    .flatMap((bodyPart) -> {
                        try {
                            WebResourceResponse webResourceResponse = new WebResourceResponse(
                                    bodyPart.getMimeType(),
                                    bodyPart.getCharset(),
                                    ((SingleBody) bodyPart.getBody()).getInputStream()
                            );

                            Map<String, String> responseHeaders = new HashMap<>();
                            responseHeaders.put("Cache-Control", StorageObject.CACHE_CONTROL);
                            webResourceResponse.setResponseHeaders(responseHeaders);

                            return Optional.of(webResourceResponse);
                        } catch (IOException e) {
                            ErrorRepository.captureException(e);
                            return Optional.empty();
                        }
                    });

            if (!response.isPresent()) {
                Sentry.pushScope();
                Sentry.configureScope((scope) -> {
                    scope.setContexts("WebResourceRequest URL", request.getUrl());
                    scope.setContexts("WebArchive URI", webArchive.getStorageObject().getAmazonS3URI(context, user).toString());
                });
                ErrorRepository.captureException(new Exception("Unable to find content location in archive."));
                Sentry.popScope();
            }

            return response.orElse(null);
        } catch (Exception e) {
            ErrorRepository.captureException(e);
            return null;
        }
    }

    @Override
    public void onPageFinished(WebView webview, String url) {
        if (webview.getProgress() == 100) {
            if (!hasInjectedAnnotationRendererScript) {
                onInjectAnnotationRendererScript.invoke(webview).whenComplete((_void, error) -> {
                    hasInjectedAnnotationRendererScript = true;
                });
            }

            if (shouldClearHistoryOnPageFinished) {
                webview.clearHistory();
                shouldClearHistoryOnPageFinished = false;
            }
        }
    }

    public Optional<URI> getSourceURI() {
        if (source.getType().equals(Source.Type.EXTERNAL_SOURCE)) {
            return source.getURI();
        }

        if (source.getType().equals(Source.Type.WEB_ARCHIVE)) {
            return source.getWebArchive().map(w -> w.getStorageObject().getAmazonS3URI(context, user).getURI());
        }

        return Optional.empty();
    }

    public boolean sourceContainsURI(URI uri) {
        if (source.getType().equals(Source.Type.EXTERNAL_SOURCE)) {
            return source.getURI().map(u -> u.equals(uri)).orElse(false);
        }

        return source.getWebArchive().map(w -> {
            boolean containsContentLocation = w.getBodyPartByContentLocation().containsKey(uri.toString());
            boolean isPrimaryURI = getSourceURI().map(u -> u.equals(uri)).orElse(false);

            return containsContentLocation || isPrimaryURI;
        }).orElse(false);
    }


    @Override
    public void onPageStarted(WebView webview, String url, Bitmap favicon) {
        Optional<URI> newSourceURIOpt = Optional.empty();
        try {
            newSourceURIOpt = Optional.of(new URI(url));
        } catch (URISyntaxException e) {
            ErrorRepository.captureException(e);
        }

        Optional.ofNullable(favicon).ifPresent(onReceivedIcon::invoke);

        newSourceURIOpt.ifPresent((newSourceURI) -> {
            if (!sourceContainsURI(newSourceURI)) {
                onSourceChanged.invoke(new Source(newSourceURI));
            }
        });
    }

    public void setShouldClearHistoryOnPageFinished(boolean shouldClearHistoryOnPageFinished) {
        this.shouldClearHistoryOnPageFinished = shouldClearHistoryOnPageFinished;
    }

    public static class Builder {
        private Context context;
        private User user;
        private Source source;
        private Function1<Source, Void> onSourceChanged;
        private Function1<WebView, CompletableFuture<Void>> onInjectAnnotationRendererScript;
        private Function1<WebResourceRequest, Void> onWebResourceRequest;
        private Function1<Bitmap, Void> onReceivedIcon;

        public Builder() {}


        public Builder setContext(Context context) {
            this.context = context;
            return this;
        }

        public Builder setUser(User user) {
            this.user = user;
            return this;
        }

        public Builder setSource(Source source) {
            this.source = source;
            return this;
        }

        public Builder setOnSourceChanged(Function1<Source, Void> onSourceChanged) {
            this.onSourceChanged = onSourceChanged;
            return this;
        }

        public Builder setOnInjectAnnotationRendererScript(Function1<WebView, CompletableFuture<Void>> onInjectAnnotationRendererScript) {
            this.onInjectAnnotationRendererScript = onInjectAnnotationRendererScript;
            return this;
        }

        public Builder setOnWebResourceRequest(Function1<WebResourceRequest, Void> onWebResourceRequest) {
            this.onWebResourceRequest = onWebResourceRequest;
            return this;
        }

        public Builder setOnReceivedIcon(Function1<Bitmap, Void> onReceivedIcon) {
            this.onReceivedIcon = onReceivedIcon;
            return this;
        }


        public Client build() {
            Objects.requireNonNull(context);
            Objects.requireNonNull(source);
            Objects.requireNonNull(user);
            Objects.requireNonNull(onSourceChanged);
            Objects.requireNonNull(onWebResourceRequest);
            Objects.requireNonNull(onInjectAnnotationRendererScript);
            Objects.requireNonNull(onReceivedIcon);

            return new Client(
                    context,
                    source,
                    user,
                    onSourceChanged,
                    onWebResourceRequest,
                    onInjectAnnotationRendererScript,
                    onReceivedIcon
            );
        }

    }
}
