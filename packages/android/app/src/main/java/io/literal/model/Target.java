package io.literal.model;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import kotlin.annotation.AnnotationTarget;
import type.AnnotationTargetInput;

public class Target {
    public enum Type {
        TEXTUAL_TARGET,
        EXTERNAL_TARGET,
        SPECIFIC_TARGET
    }

    protected final Type type;

    public Target(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public JSONObject toJson() throws JSONException {
       throw new JSONException("Serialization not implemented.");
    }

    public AnnotationTargetInput toAnnotationTargetInput() {
        Log.d("Target", "Attempted to call toAnnotationTargetInput, but not implemented.");
        return null;
    }

    public static Target fromJson(JSONObject json) throws JSONException {
        if (json.has("value")) {
            return TextualTarget.fromJson(json);
        } else if (json.has("selector")) {
            return SpecificTarget.fromJson(json);
        } else {
            return ExternalTarget.fromJson(json);
        }
    }
}
