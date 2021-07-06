package io.literal.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.AttributeSet;
import android.webkit.WebViewClient;

import androidx.webkit.WebMessageCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.UUID;

import io.literal.lib.WebEvent;
import io.literal.lib.WebRoutes;

public class AppWebView extends MessagingWebView {
    private WebViewClient externalWebViewClient;
    private final WebViewClient webViewClient = new WebViewClient() {
        @Override
        public void onPageFinished(android.webkit.WebView webview, String url) {
            if (url == null) {
                return;
            }

            if (webview.getProgress() == 100) {
                if (AppWebView.this.externalWebViewClient != null) {
                    AppWebView.this.externalWebViewClient.onPageFinished(webview, url);
                }
            }
        }

        @Override
        public void onPageStarted(android.webkit.WebView webview, String url, Bitmap favicon) {
            if (url == null) {
                return;
            }

            if (AppWebView.this.externalWebViewClient != null) {
                AppWebView.this.externalWebViewClient.onPageStarted(webview, url, favicon);
            }
        }
    };
    private ArrayDeque<String> baseHistory;

    public AppWebView(Context context) {
        super(context);
        setNestedScrollingEnabled(true);
    }

    public AppWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setNestedScrollingEnabled(true);
    }

    public void initialize() {
        super.initialize(Uri.parse(WebRoutes.getWebHost()));
        this.setWebViewClient(this.webViewClient);
    }

    public void setExternalWebViewClient(WebViewClient externalWebViewClient) {
        this.externalWebViewClient = externalWebViewClient;
    }

    public boolean handleBackPressed() {
        if (this.canGoBack()) {
            this.goBack();
            return true;
        }

        if (baseHistory != null && baseHistory.peek() != null) {
            String prev = baseHistory.poll();
            try {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.POST_WEB_MESSAGE)) {
                    JSONObject data = new JSONObject();
                    data.put("url", prev);
                    WebViewCompat.postWebMessage(
                            this,
                            new WebMessageCompat(
                                    new WebEvent(WebEvent.TYPE_ROUTER_REPLACE, UUID.randomUUID().toString(), data)
                                            .toJSON()
                                            .toString()
                            ),
                            Uri.parse(WebRoutes.getWebHost())
                    );
                } else {
                    this.loadUrl(prev);
                }
            } catch (JSONException ex) {
                this.loadUrl(prev);
            }
            return true;
        }
        return false;
    }
}
