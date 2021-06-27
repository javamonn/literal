package io.literal.model;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import io.literal.lib.Crypto;
import io.literal.lib.JsonArrayUtil;
import io.literal.repository.ErrorRepository;
import type.AnnotationAddOperationInput;
import type.AnnotationSetOperationInput;
import type.AnnotationTargetInput;
import type.AnnotationWhereInput;
import type.ExternalTargetInput;
import type.PatchAnnotationOperationInput;

public class ExternalTarget extends Target {

    private final Id id;
    private final Format format;
    private final Language language;
    private final Language processingLanguage;
    private final TextDirection textDirection;
    private final ResourceType resourceType;
    private final String[] accessibility;
    private final String[] rights;

    public static class Id {
        public enum Type {
            IRI,
            STORAGE_OBJECT;
        }

        private final Type type;
        private final String iri;
        private final StorageObject storageObject;

        public Id(String iri) {
            this.type = Type.IRI;
            this.iri = iri;
            storageObject = null;
        }

        public Id(StorageObject storageObject) {
            this.type = Type.STORAGE_OBJECT;
            this.storageObject = storageObject;
            this.iri = null;
        }

        public static Id fromString(String json) {
            try {
                URI uri = new URI(json);
                StorageObject storageObject = StorageObject.create(uri);
                if (storageObject != null) {
                    return new Id(storageObject);
                }
                return new Id(json);
            } catch (URISyntaxException e) {
                return new Id(json);
            }
        }

        @NotNull
        public String toString() {
            if (type.equals(Type.IRI) && iri != null) {
                return iri;
            }
            return storageObject.getCanonicalURI().toString();
        }

        public Type getType() {
            return type;
        }

        public StorageObject getStorageObject() {
            return storageObject;
        }

        public String getIri() {
            return iri;
        }
    }

    public ExternalTarget(@NotNull Id id, Format format, Language language, Language processingLanguage, TextDirection textDirection, String[] accessibility, String[] rights, ResourceType resourceType) {
        super(Type.EXTERNAL_TARGET);
        this.id = id;
        this.format = format;
        this.language = language;
        this.processingLanguage = processingLanguage;
        this.textDirection = textDirection;
        this.accessibility = accessibility;
        this.rights = rights;
        this.resourceType = resourceType;
    }

    public static ExternalTarget create(@NotNull StorageObject storageObject) {
        if (storageObject.getType().equals(StorageObject.Type.SCREENSHOT)) {
            return new ExternalTarget(
                    new Id(storageObject),
                    Format.IMAGE_PNG,
                    Language.EN_US,
                    Language.EN_US,
                    TextDirection.LTR,
                    null,
                    null,
                    ResourceType.IMAGE
            );
        }
        return null;
    }

    public Id getId() {
        return id;
    }
    public Format getFormat() { return format; }
    public Language getLanguage() { return language; }
    public Language getProcessingLanguage() { return processingLanguage; }
    public ResourceType getResourceType() { return resourceType; }
    public String[] getAccessibility() { return accessibility; }
    public String[] getRights() { return rights; }
    public TextDirection getTextDirection() { return textDirection; }

    public static ExternalTarget fromJson(JSONObject json) throws JSONException {
        return new ExternalTarget(
                Id.fromString(json.getString("id")),
                !json.isNull("format") ? Format.valueOf(json.getString("format")) : null,
                !json.isNull("language") ? Language.valueOf(json.getString("language")) : null,
                !json.isNull("processingLanguage") ? Language.valueOf(json.getString("processingLanguage")) : null,
                !json.isNull("textDirection") ? TextDirection.valueOf(json.getString("textDirection")) : null,
                !json.isNull("accessibility") ? JsonArrayUtil.parseJsonStringArray(json.getJSONArray("accessibility")) : null,
                !json.isNull("rights") ? JsonArrayUtil.parseJsonStringArray(json.getJSONArray("rights")) : null,
                !json.isNull("type") ? ResourceType.valueOf(json.getString("type")) : null
        );
    }

    public JSONObject toJson() throws JSONException {
        JSONObject output = new JSONObject();
        output.put("id", this.id.toString());
        output.put("format", this.format != null ? this.format.name() : null);
        output.put("language", this.language != null ? this.language.name() : null);
        output.put("processingLanguage", this.processingLanguage != null ? this.processingLanguage.name() : null);
        output.put("textDirection", this.textDirection != null ? this.textDirection.name() : null);
        output.put("accessibility", this.accessibility != null ? new JSONArray(this.accessibility) : null);
        output.put("rights", this.rights != null ? new JSONArray(this.rights) : null);
        output.put("type", this.resourceType);

        return output;
    }

    @Override
    public AnnotationTargetInput toAnnotationTargetInput() {
        String hashId = null;
        try {
            hashId = Crypto.sha256Hex(this.id.toString());
        } catch (NoSuchAlgorithmException e) {
            ErrorRepository.captureException(e);
        }

        return AnnotationTargetInput.builder().externalTarget(
                ExternalTargetInput.builder()
                        .id(this.id.toString())
                        .format(this.format != null ? this.format.toGraphQL() : null)
                        .language(this.language != null ? this.language.toGraphQL() : null)
                        .processingLanguage(this.processingLanguage != null ? this.processingLanguage.toGraphQL() : null)
                        .textDirection(this.textDirection != null ? this.textDirection.toGraphQL() : null)
                        .accessibility(
                                this.accessibility != null
                                        ? Arrays.asList(this.accessibility)
                                        : null
                        )
                        .rights(
                                this.rights != null
                                        ? Arrays.asList(this.rights)
                                        : null
                        )
                        .hashId(hashId)
                        .type(
                                this.resourceType != null ? this.resourceType.toGraphQL() : null
                        )
                        .build()
        ).build();
    }

    @Override
    public PatchAnnotationOperationInput toPatchAnnotationOperationInputAdd() {
        return PatchAnnotationOperationInput.builder()
                .add(
                        AnnotationAddOperationInput
                                .builder()
                                .target(
                                        this.toAnnotationTargetInput()
                                )
                                .build()
                )
                .build();
    }

    @Override
    public PatchAnnotationOperationInput toPatchAnnotationOperationInputSet() {
        return PatchAnnotationOperationInput.builder()
                .set(
                        AnnotationSetOperationInput.builder()
                                .where(
                                        AnnotationWhereInput.builder()
                                                .id(this.id.toString())
                                                .build()
                                )
                                .target(this.toAnnotationTargetInput())
                                .build()
                )
                .build();
    }
}
