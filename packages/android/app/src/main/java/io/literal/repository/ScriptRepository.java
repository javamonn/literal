package io.literal.repository;

import android.content.res.AssetManager;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import io.literal.lib.JsonArrayUtil;
import io.literal.model.Annotation;

public class ScriptRepository {
    private static final String GET_ANNOTATION_SCRIPT_NAME = "SourceWebViewGetAnnotation.js";
    private static final String ANNOTATION_RENDERER_SCRIPT_NAME = "SourceWebViewAnnotationRenderer.js";
    private static final String GET_ANNOTATION_BOUNDING_BOX_SCRIPT_NAME = "SourceWebViewGetAnnotationBoundingBox.js";
    private static final String GET_SCRIPTS_SCRIPT_NAME = "SourceWebViewGetScripts.js";

    private static Optional<String> getAnnotationScript = Optional.empty();
    private static Optional<String> annotationRendererScript = Optional.empty();
    private static Optional<String> getAnnotationBoundingBoxScript = Optional.empty();
    private static Optional<String> getScriptsScript = Optional.empty();

    public static String getGetAnnotationScript(AssetManager assetManager) {
        String output = getAnnotationScript.orElseGet(() -> {
            try {
                return IOUtils.toString(assetManager.open(GET_ANNOTATION_SCRIPT_NAME), StandardCharsets.UTF_8);
            } catch (IOException e) {
                ErrorRepository.captureException(e);
                return null;
            }
        });
        getAnnotationScript = Optional.ofNullable(output);
        return output;
    }

    public static String getGetScriptsScript(AssetManager assetManager) {
        String output = getScriptsScript.orElseGet(() -> {
            try {
                return IOUtils.toString(assetManager.open(GET_SCRIPTS_SCRIPT_NAME), StandardCharsets.UTF_8);
            } catch (IOException e) {
                ErrorRepository.captureException(e);
                return null;
            }
        });

        getScriptsScript = Optional.ofNullable(output);
        return output;
    }

    public static String getAnnotationRendererScript(AssetManager assetManager, Annotation[] annotations, String focusedAnnotationId) {
        String scriptOutput = annotationRendererScript.orElseGet(() -> {
            try {
                return IOUtils.toString(assetManager.open(ANNOTATION_RENDERER_SCRIPT_NAME), StandardCharsets.UTF_8);
            } catch (IOException e) {
                ErrorRepository.captureException(e);
                return null;
            }
        });

        annotationRendererScript = Optional.ofNullable(scriptOutput);
        try {
            String paramAnnotations = JSONObject.quote(JsonArrayUtil.stringifyObjectArray(annotations, Annotation::toJson).toString());
            String paramFocusedAnnotationId = Optional.ofNullable(focusedAnnotationId).orElse("");

            return scriptOutput
                    .replaceAll(
                            "process\\.env\\.PARAM_ANNOTATIONS",
                            paramAnnotations.substring(1, paramAnnotations.length() - 1)
                    )
                    .replaceAll(
                            "process\\.env\\.PARAM_FOCUSED_ANNOTATION_ID",
                            JSONObject.quote(paramFocusedAnnotationId)
                    );
        } catch (JSONException e) {
            ErrorRepository.captureException(e);
            return null;
        }


    }

    public static String getGetAnnotationBoundingBoxScript(AssetManager assetManager, JSONObject paramAnnotation) {
        String scriptOutput = getAnnotationBoundingBoxScript.orElseGet(() -> {
            try {
                return IOUtils.toString(assetManager.open(GET_ANNOTATION_BOUNDING_BOX_SCRIPT_NAME), StandardCharsets.UTF_8);
            } catch (IOException e) {
                ErrorRepository.captureException(e);
                return null;
            }
        });

        getAnnotationBoundingBoxScript = Optional.ofNullable(scriptOutput);

        String stringifiedParamAnnotation = JSONObject.quote(paramAnnotation.toString());
        return scriptOutput
                .replaceAll(
                        "process\\.env\\.PARAM_ANNOTATION",
                        stringifiedParamAnnotation.substring(1, stringifiedParamAnnotation.length() - 1)
                );
    }
}
