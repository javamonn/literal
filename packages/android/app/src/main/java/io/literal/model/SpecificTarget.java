package io.literal.model;

import com.amazonaws.services.s3.model.ObjectMetadata;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.literal.lib.JsonArrayUtil;
import type.AnnotationAddOperationInput;
import type.AnnotationSetOperationInput;
import type.AnnotationTargetInput;
import type.AnnotationWhereInput;
import type.PatchAnnotationOperationInput;
import type.SpecificResourceType;
import type.SpecificTargetInput;

public class SpecificTarget extends Target {

    private final String id;
    private final Target source;
    private final Selector[] selector;
    private final State[] state;

    public SpecificTarget(String id, @NotNull Target source, Selector[] selector, State[] state) {
        super(Type.SPECIFIC_TARGET);

        this.id = id == null || id.equals("") ? UUID.randomUUID().toString() : id;
        this.source = source;
        this.selector = selector;
        this.state = state;
    }

    public static SpecificTarget fromJson(JSONObject json) throws JSONException {
        return new SpecificTarget(
                json.optString("id"),
                Target.fromJson(json.getJSONObject("source")),
                !json.isNull("selector")
                        ? JsonArrayUtil.parseJsonObjectArray(
                        json.getJSONArray("selector"),
                        new Selector[0],
                        Selector::fromJson
                )
                        : null,
                !json.isNull("state")
                        ? JsonArrayUtil.parseJsonObjectArray(
                        json.getJSONArray("state"),
                        new State[0],
                        State::fromJson
                )
                        : null
        );
    }

    ;

    public String getId() {
        return id;
    }

    public Target getSource() {
        return source;
    }

    public Selector[] getSelector() {
        return selector;
    }

    public State[] getState() {
        return state;
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject output = new JSONObject();
        output.put("id", this.id);
        output.put("source", this.source.toJson());
        output.put("selector",
                this.selector != null
                        ? JsonArrayUtil.stringifyObjectArray(
                        this.selector,
                        Selector::toJson
                )
                        : null
        );
        output.put("state", this.state != null
                        ? JsonArrayUtil.stringifyObjectArray(
                this.state,
                State::toJson
                )
                        : null
        );
        output.put("type", SpecificResourceType.SPECIFIC_RESOURCE.name());

        return output;
    }

    @Override
    public AnnotationTargetInput toAnnotationTargetInput() {
        return AnnotationTargetInput.builder().specificTarget(
                SpecificTargetInput.builder()
                        .id(this.id)
                        .source(this.source.toAnnotationTargetInput())
                        .selector(
                                this.selector != null
                                        ? Stream.of(this.selector).map(Selector::toSelectorInput).collect(Collectors.toList())
                                        : null
                        )
                        .state(
                                this.state != null
                                        ? Stream.of(this.state).map(State::toStateInput).collect(Collectors.toList())
                                        : null
                        )
                        .type(SpecificResourceType.SPECIFIC_RESOURCE)
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
                                                .id(this.id)
                                                .build()
                                )
                                .target(this.toAnnotationTargetInput())
                                .build()
                )
                .build();
    }

    public static class Builder {
        private String id;
        private Target source;
        private Selector[] selector;
        private State[] state;

        public Builder() {}
        public Builder(SpecificTarget base) {
            this.id = base.getId();
            this.source = base.getSource();
            this.selector = base.getSelector();
            this.state = base.getState();
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setSource(Target source) {
            this.source = source;
            return this;
        }

        public Builder setSelector(Selector[] selector) {
            this.selector = selector;
            return this;
        }

        public Builder setState(State[] state) {
            this.state = state;
            return this;
        }

        public SpecificTarget build() {
            Objects.requireNonNull(source);
            return new SpecificTarget(id, source, selector, state);
        }
    }
}
