package io.literal.viewmodel;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

import io.literal.lib.AnnotationCollectionLib;
import io.literal.lib.AnnotationLib;
import io.literal.lib.ArrayUtil;
import io.literal.lib.Constants;
import io.literal.lib.Crypto;
import io.literal.lib.WebEvent;
import io.literal.lib.WebRoutes;
import io.literal.model.Annotation;
import io.literal.model.Body;
import io.literal.model.Format;
import io.literal.model.Language;
import io.literal.model.Motivation;
import io.literal.model.Target;
import io.literal.model.TextDirection;
import io.literal.model.TextualBody;
import io.literal.model.TextualTarget;
import io.literal.model.WebArchive;
import io.literal.repository.ErrorRepository;
import io.literal.ui.view.SourceWebView.Source;

public class SourceWebViewViewModel extends ViewModel {
    private final MutableLiveData<ArrayList<Annotation>> annotations = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> focusedAnnotationId = new MutableLiveData<>(null);
    private final MutableLiveData<ArrayDeque<WebEvent>> webEvents = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> sourceHasFinishedInitializing = new MutableLiveData<>(false);
    private final ArrayList<String> newAnnotationIds = new ArrayList<>();
    private final HashMap<String, WebArchive> webArchives = new HashMap<>();

    public MutableLiveData<ArrayList<Annotation>> getAnnotations() {
        return annotations;
    }
    public void setAnnotations(ArrayList<Annotation> annotations) {
        this.annotations.setValue(annotations);
    }

    public MutableLiveData<String> getFocusedAnnotationId() {
        return focusedAnnotationId;
    }

    public Optional<Annotation> getFocusedAnnotation() {
        if (focusedAnnotationId.getValue() == null) {
            return Optional.empty();
        }

        return annotations.getValue().stream()
                .filter(a -> a.getId().equals(focusedAnnotationId.getValue()))
                .findFirst();
    }

    public void setFocusedAnnotationId(String annotationId) {
        focusedAnnotationId.setValue(annotationId);
    }

    public ArrayList<String> getNewAnnotationIds() {
        return newAnnotationIds;
    }

    public void addWebArchive(String annotationId, WebArchive webArchive) {
        this.webArchives.put(annotationId, webArchive);
    }

    public HashMap<String, WebArchive> getWebArchives() {
        return webArchives;
    }

    public MutableLiveData<Boolean> getSourceHasFinishedInitializing() {
        return sourceHasFinishedInitializing;
    }

    public void setSourceHasFinishedInitializing(boolean sourceHasFinishedInitializing) {
        Log.d("setSourceHasFinishedInitializing", "sourceHasFinishedInitializing: " + sourceHasFinishedInitializing, new Exception("trace"));
        this.sourceHasFinishedInitializing.setValue(sourceHasFinishedInitializing);
    }

    public void reset() {
        this.annotations.setValue(new ArrayList<>());
        this.focusedAnnotationId.setValue(null);
        this.webEvents.setValue(new ArrayDeque<>());
        this.sourceHasFinishedInitializing.setValue(false);
        this.newAnnotationIds.clear();
        this.webArchives.clear();
    }

    public Annotation createAnnotation(String json, String creatorUsername) {
        try {
            ArrayList<Annotation> newAnnotations = (ArrayList<Annotation>) annotations.getValue().clone();
            Annotation annotation = Annotation.fromJson(new JSONObject(json));

            boolean needsRecentTag =
                    Arrays.stream(annotation.getBody() == null ? new Body[0] : annotation.getBody())
                            .noneMatch((body) -> {
                                if (body.getType() == Body.Type.TEXTUAL_BODY) {
                                    TextualBody textualBody = (TextualBody) body;
                                    return AnnotationLib.idComponentFromId(textualBody.getId()).equals(Constants.RECENT_ANNOTATION_COLLECTION_ID_COMPONENT);
                                }
                                return false;
                            });
            if (needsRecentTag) {
                Body recentTag = new TextualBody(
                        AnnotationCollectionLib.makeId(
                                creatorUsername,
                                Constants.RECENT_ANNOTATION_COLLECTION_LABEL
                        ),
                        Format.TEXT_PLAIN,
                        Language.EN_US,
                        Language.EN_US,
                        TextDirection.LTR,
                        null,
                        null,
                        new Motivation[]{Motivation.TAGGING},
                        Constants.RECENT_ANNOTATION_COLLECTION_LABEL
                );

                annotation = new Annotation(
                        ArrayUtil.add(
                                annotation.getBody() != null ? annotation.getBody() : new Body[0],
                                recentTag
                        ),
                        annotation.getTarget(),
                        annotation.getMotivation(),
                        annotation.getCreated(),
                        annotation.getModified(),
                        annotation.getId()
                );
            }

            if (annotation.getId() == null) {
                TextualTarget textualTarget = (TextualTarget)
                        Arrays.stream(annotation.getTarget()).filter(target -> target.getType() == Target.Type.TEXTUAL_TARGET)
                                .findFirst()
                                .get();
                String valueHash = Crypto.sha256Hex(textualTarget.getValue());
                String annotationId = WebRoutes.creatorsIdAnnotationId(
                        WebRoutes.getAPIHost(),
                        creatorUsername,
                        valueHash
                );

                annotation = new Annotation(
                        annotation.getBody(),
                        annotation.getTarget(),
                        annotation.getMotivation(),
                        annotation.getCreated(),
                        annotation.getModified(),
                        annotationId
                );

            }

            newAnnotations.add(annotation);
            annotations.setValue(newAnnotations);
            newAnnotationIds.add(annotation.getId());

            return annotation;
        } catch (Exception e) {
            ErrorRepository.captureException(e);
            return null;
        }
    }

    public void createAnnotation(Annotation annotation) {
        ArrayList<Annotation> newAnnotations = (ArrayList<Annotation>) annotations.getValue().clone();
        newAnnotations.add(annotation);
        annotations.setValue(newAnnotations);
        newAnnotationIds.add(annotation.getId());
    }

    public void addAnnotation(Annotation annotation) {
        ArrayList<Annotation> newAnnotations = (ArrayList<Annotation>) annotations.getValue().clone();
        newAnnotations.add(annotation);
        annotations.setValue(newAnnotations);
    }

    public boolean updateAnnotation(Annotation annotation) {
        if (annotation.getId() != null) {
            ArrayList<Annotation> newAnnotations = (ArrayList<Annotation>) annotations.getValue().clone();
            int idx = -1;
            for (int i = 0; i < newAnnotations.size(); i++) {
                if (newAnnotations.get(i).getId().equals(annotation.getId())) {
                    idx = i;
                    break;
                }
            }

            if (idx != -1) {
                newAnnotations.set(idx, annotation);
                annotations.setValue(newAnnotations);
                return true;
            }
        }
        return false;
    }

    public boolean removeAnnotation(String annotationId) {
        ArrayList<Annotation> updatedAnnotations = (ArrayList<Annotation>) annotations.getValue().clone();
        boolean updated = updatedAnnotations.removeIf((annotation) -> annotation.getId().equals(annotationId));
        if (updated) {
            annotations.setValue(updatedAnnotations);
        }
        return updated;
    }

    public void dispatchWebEvent(WebEvent webEvent) {
        ArrayDeque<WebEvent> newWebEvents;
        if (webEvents.getValue() == null) {
            newWebEvents = new ArrayDeque<>();
        } else {
            newWebEvents = webEvents.getValue().clone();
        }
        newWebEvents.add(webEvent);
        webEvents.setValue(newWebEvents);
    }

    public void clearWebEvents() {
        webEvents.setValue(null);
    }

    public MutableLiveData<ArrayDeque<WebEvent>> getWebEvents() {
        return webEvents;
    }
}
