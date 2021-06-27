package io.literal.repository;

import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.literal.lib.JsonArrayUtil;
import io.literal.model.HTMLScriptElement;

public class WebViewRepository {
    public static CompletableFuture<String> evaluateJavascript(WebView webView, String script) {
        CompletableFuture<String> future = new CompletableFuture<>();
        webView.evaluateJavascript(script, future::complete);
        return future;
    }

    public static CompletableFuture<String> saveWebArchive(WebView webView, String basename, boolean autoname) {
        CompletableFuture<String> future = new CompletableFuture<>();
        webView.saveWebArchive(basename, autoname, future::complete);
        return future;
    }

    public static CompletableFuture<ArrayList<HTMLScriptElement>> getPageScriptElements(WebView webView, String getScriptsScript) {
        return WebViewRepository.evaluateJavascript(webView, getScriptsScript)
                .thenCompose((scriptsJSON) -> {
                    CompletableFuture<ArrayList<HTMLScriptElement>> future = new CompletableFuture<>();
                    try {
                        HTMLScriptElement[] scriptElements = JsonArrayUtil.parseJsonObjectArray(
                                new JSONArray(scriptsJSON),
                                new HTMLScriptElement[0],
                                HTMLScriptElement::fromJSON
                        );
                        future.complete(new ArrayList<>(Arrays.asList(scriptElements)));
                    } catch (JSONException e) {
                        future.completeExceptionally(e);
                    }
                    return future;
                });
    }
}
