package io.literal.model;

import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.literal.lib.AnnotationLib;
import io.literal.lib.AnnotationTargetLib;
import io.literal.lib.Constants;
import io.literal.lib.Crypto;
import io.literal.lib.JsonArrayUtil;
import io.literal.lib.WebRoutes;
import io.literal.repository.ErrorRepository;
import kotlin.jvm.functions.Function1;
import type.AnnotationType;
import type.CreateAnnotationInput;

public class Annotation {
    private final Body[] body;
    private final Target[] target;
    private final Motivation[] motivation;
    private final String created;
    private final String modified;
    private final String id;

    public Annotation(Body[] body, @NotNull Target[] target, Motivation[] motivation, String created, String modified, String id) {
        this.body = body;
        this.target = target;
        this.motivation = motivation;
        this.id = id;
        this.created = created;
        this.modified = modified;
    }

    public static Annotation fromJson(JSONObject json) throws JSONException {
        return new Annotation(
                !json.isNull("body")
                        ? JsonArrayUtil.parseJsonObjectArray(json.getJSONArray("body"), new Body[0], Body::fromJson)
                        : null,
                JsonArrayUtil.parseJsonObjectArray(json.getJSONArray("target"), new Target[0], Target::fromJson),
                !json.isNull("motivation")
                        ? Arrays.stream(JsonArrayUtil.parseJsonStringArray(json.getJSONArray("motivation")))
                        .map(Motivation::valueOf).toArray(Motivation[]::new)
                        : null,
                json.optString("created", null),
                json.optString("modified", null),
                json.optString("id", null)
        );
    }

    public String getId() {
        return id;
    }
    public Target[] getTarget() {
        return target;
    }
    public Body[] getBody() {
        return body;
    }
    public Motivation[] getMotivation() { return motivation; }
    public String getCreated() { return created; }
    public String getModified() { return modified; }

    public JSONObject toJson() throws JSONException {
        JSONObject output = new JSONObject();
        output.put("id", this.id);
        output.put("created", this.created);
        output.put("modified", this.modified);
        output.put("body", this.body != null ? JsonArrayUtil.stringifyObjectArray(this.body, Body::toJson) : null);
        output.put("target", JsonArrayUtil.stringifyObjectArray(this.target, Target::toJson));
        output.put("motivation", this.motivation != null ? new JSONArray(Arrays.stream(this.motivation).map(Motivation::name).collect(Collectors.toList())) : null);

        return output;
    }

    public CreateAnnotationInput toCreateAnnotationInput() {
        return CreateAnnotationInput.builder()
                .context(Collections.singletonList("http://www.w3.org/ns/anno.jsonld"))
                .type(Collections.singletonList(AnnotationType.ANNOTATION))
                .motivation(
                        this.motivation != null
                                ? Arrays.stream(this.motivation).map(Motivation::toGraphQL).collect(Collectors.toList())
                                : null
                )
                .id(this.id)
                .creatorUsername(AnnotationLib.creatorUsernameFromId(this.id))
                .target(
                        Stream.of(this.target).map(Target::toAnnotationTargetInput).collect(Collectors.toList())
                )
                .body(
                        this.body != null
                                ? Stream.of(this.body).map(Body::toAnnotationBodyInput).collect(Collectors.toList())
                                : null
                ).build();
    }

    public static Annotation fromText(String text, String creatorUsername) {
        try {
            String valueHash = Crypto.sha256Hex(text);
            String annotationId = WebRoutes.creatorsIdAnnotationId(
                    WebRoutes.getAPIHost(),
                    creatorUsername,
                    valueHash
            );

            Body[] bodies = { TextualBody.createTag(Constants.RECENT_ANNOTATION_COLLECTION_LABEL, creatorUsername) };
            Target[] targets = { new TextualTarget(AnnotationTargetLib.makeId(annotationId), text) };
            return new Annotation(
                    bodies,
                    targets,
                    new Motivation[] { Motivation.HIGHLIGHTING },
                    null,
                    null,
                    annotationId
            );
        } catch (NoSuchAlgorithmException ex) {
            ErrorRepository.captureException(ex);
            return null;
        }
    }

    public static Annotation fromScreenshot(StorageObject screenshot, String creatorUsername) {
        String annotationId = WebRoutes.creatorsIdAnnotationId(
                WebRoutes.getAPIHost(),
                creatorUsername,
                UUID.randomUUID().toString()
        );
        Body[] bodies = { TextualBody.createTag(Constants.RECENT_ANNOTATION_COLLECTION_LABEL, creatorUsername) };
        Target[] targets = { ExternalTarget.create(screenshot) };

        return new Annotation(
                bodies,
                targets,
                new Motivation[] { Motivation.HIGHLIGHTING },
                null,
                null,
                annotationId
        );
    }

    public Annotation updateTarget(Target updatedTarget) {
        Function1<Target, String> getIDForTarget = (target) -> {
            if (target instanceof ExternalTarget) {
                return ((ExternalTarget) target).getId().toString();
            } else if (target instanceof SpecificTarget) {
                return ((SpecificTarget) target).getId();
            } else {
                return ((TextualTarget) target).getId();
            }
        };

        Target[] newTarget = getTarget().clone();
        for (int i = 0; i < getTarget().length; i++) {
            if (getIDForTarget.invoke(newTarget[i]).equals(getIDForTarget.invoke(updatedTarget))) {
                newTarget[i] = updatedTarget;
                break;
            }
        }

        return new Annotation.Builder(this).setTarget(newTarget).build();
    }

    public static class Builder {
        private Body[] body;
        private Target[] target;
        private Motivation[] motivation;
        private String created;
        private String modified;
        private String id;

        public Builder(Annotation base) {
            this.body = base.getBody().clone();
            this.target = base.getTarget().clone();
            this.motivation = base.getMotivation();
            this.created = base.getCreated();
            this.id = base.getId();
            this.modified = base.getModified();
        }

        public Builder setBody(Body[] body) {
            this.body = body;
            return this;
        }

        public Builder setTarget(Target[] target) {
            this.target = target;
            return this;
        }

        public Builder setMotivation(Motivation[] motivation) {
            this.motivation = motivation;
            return this;
        }

        public Builder setCreated(String created) {
            this.created = created;
            return this;
        }

        public Builder setModified(String modified) {
            this.modified = modified;
            return this;
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Annotation build() {
            return new Annotation(
                    body,
                    target,
                    motivation,
                    created,
                    modified,
                    id
            );
        }
    }
}
