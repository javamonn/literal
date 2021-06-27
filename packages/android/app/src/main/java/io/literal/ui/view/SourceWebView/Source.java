package io.literal.ui.view.SourceWebView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.webkit.WebResourceRequest;

import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

import io.literal.model.Annotation;
import io.literal.model.ExternalTarget;
import io.literal.model.SpecificTarget;
import io.literal.model.State;
import io.literal.model.StorageObject;
import io.literal.model.Target;
import io.literal.model.TextualTarget;
import io.literal.model.TimeState;
import io.literal.model.WebArchive;
import io.literal.repository.ErrorRepository;
import kotlin.jvm.functions.Function1;

public class Source {
    private final Type type;

    // WebArchive content
    private final Optional<WebArchive> webArchive;
    private final Optional<URI> displayURI;

    // External source content
    private final Optional<URI> uri;

    // Metadata
    private Optional<Bitmap> favicon;
    private final ArrayList<WebResourceRequest> pageWebResourceRequests;

    public Source(@NotNull WebArchive webArchive, Optional<URI> displayURI) {
        this.webArchive = Optional.of(webArchive);
        this.displayURI = displayURI;
        this.uri = Optional.empty();
        this.type = Type.WEB_ARCHIVE;
        this.pageWebResourceRequests = new ArrayList<>();
    }

    public Source(@NotNull URI uri) {
        this.uri = Optional.of(uri);
        this.webArchive = Optional.empty();
        this.displayURI = Optional.empty();
        this.type = Type.EXTERNAL_SOURCE;
        this.pageWebResourceRequests = new ArrayList<>();
    }

    public URI getDisplayURI() {
        return displayURI.orElse(uri.orElse(null));
    }
    public Optional<URI> getURI() { return uri; }

    public static Optional<Source> createFromAnnotation(Context context, Annotation annotation, String targetId) {
        return Arrays.stream(annotation.getTarget())
                .filter((t) -> {
                    if (t.getType().equals(Target.Type.EXTERNAL_TARGET)) {
                        return Optional.ofNullable(((ExternalTarget) t).getId())
                                .map((id) -> id.toString().equals(targetId))
                                .orElse(false);
                    } else if (t.getType().equals(Target.Type.SPECIFIC_TARGET)) {
                        return ((SpecificTarget) t).getId().equals(targetId);
                    } else if (t.getType().equals(Target.Type.TEXTUAL_TARGET)) {
                        return ((TextualTarget) t).getId().equals(targetId);
                    }
                    return false;
                })
                .findFirst()
                .flatMap((t) -> {
                    if (t.getType() == Target.Type.SPECIFIC_TARGET) {
                        Optional<URI> externalTargetURI = Optional.of(((SpecificTarget) t).getSource())
                                .flatMap((source) -> {
                                    if (source.getType().equals(Target.Type.EXTERNAL_TARGET)) {
                                        URI uri = null;
                                        try {
                                            uri = new URI(((ExternalTarget) source).getId().toString());
                                        } catch (URISyntaxException e) {
                                            ErrorRepository.captureException(e);
                                        }

                                        return Optional.ofNullable(uri);
                                    }
                                    return Optional.empty();
                                });

                        return Arrays.stream(Optional.ofNullable(((SpecificTarget) t).getState()).orElse(new State[0]))
                                .filter((s) -> s.getType().equals(State.Type.TIME_STATE))
                                .findFirst()
                                .flatMap((s) ->
                                        Arrays.stream(((TimeState) s).getCached())
                                                .filter(cached -> cached.getScheme().equals("https") || cached.getScheme().equals("s3"))
                                                .min(Comparator.comparingInt((cached) -> cached.toString().endsWith(".mhtml") ? 0 : 1))
                                )
                                .map((cached) -> {
                                    if (cached.toString().endsWith(".mhtml")) {
                                        StorageObject storageObject = StorageObject.create(context, cached);
                                        if (storageObject != null) {
                                            return new Source(new WebArchive(storageObject), externalTargetURI);
                                        }
                                    }
                                    return new Source(cached);
                                });
                    } else if (t.getType() == Target.Type.EXTERNAL_TARGET) {
                        URI uri = null;
                        try {
                            uri = new URI(((ExternalTarget) t).getId().toString());
                        } catch (URISyntaxException e) {
                            ErrorRepository.captureException(e);
                        }
                        return Optional.ofNullable(uri).map(Source::new);
                    }

                    return Optional.empty();
                });
    }

    public Type getType() {
        return type;
    }

    public Optional<WebArchive> getWebArchive() {
        return webArchive;
    }

    public ArrayList<WebResourceRequest> getPageWebResourceRequests() {
        return pageWebResourceRequests;
    }

    public Optional<Bitmap> getFavicon() {
        return favicon;
    }

    public void setFavicon(Optional<Bitmap> favicon) {
        this.favicon = favicon;
    }

    public enum Type {
        WEB_ARCHIVE,
        EXTERNAL_SOURCE
    }
}
