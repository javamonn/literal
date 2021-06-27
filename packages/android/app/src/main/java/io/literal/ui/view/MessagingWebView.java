package io.literal.ui.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.webkit.WebMessageCompat;
import androidx.webkit.WebMessagePortCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import org.json.JSONException;
import org.json.JSONObject;

import io.literal.BuildConfig;
import io.literal.lib.WebEvent;
import io.literal.repository.AnalyticsRepository;
import io.literal.repository.ErrorRepository;
import io.literal.ui.view.SourceWebView.SourceWebView;
import kotlin.jvm.functions.Function2;

public class MessagingWebView extends WebView {

    private Uri baseURI;
    private Function2<MessagingWebView, WebEvent, Void> webEventCallback;
    private WebMessagePortCompat[] webMessageChannel;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public MessagingWebView(Context context) {
        this(context, null);
    }

    public MessagingWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void initialize(Uri baseURI) {
        SourceWebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        WebSettings webSettings = this.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setAllowFileAccess(true);
        this.baseURI = baseURI;
        this.addJavascriptInterface(new JavascriptInterface(), "literalWebview");
    }

    public void postWebEvent(WebEvent webEvent) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.POST_WEB_MESSAGE)) {
            WebViewCompat.postWebMessage(
                    this,
                    new WebMessageCompat(webEvent
                            .toJSON()
                            .toString()
                    ),
                    Uri.parse("*")
            );
            try {
                JSONObject properties = new JSONObject();
                properties.put("target", "SourceWebView");
                properties.put("event", webEvent.toJSON());
                AnalyticsRepository.logEvent(AnalyticsRepository.TYPE_DISPATCHED_WEB_EVENT, properties);
            } catch (JSONException e) {
                ErrorRepository.captureException(e);
            }
        }
    }

    private void initializeWebMessageChannel() {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.CREATE_WEB_MESSAGE_CHANNEL) &&
                WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK) &&
                WebViewFeature.isFeatureSupported(WebViewFeature.POST_WEB_MESSAGE)) {
            webMessageChannel = WebViewCompat.createWebMessageChannel(this);
            webMessageChannel[0].setWebMessageCallback(mainHandler, new WebMessagePortCompat.WebMessageCallbackCompat() {
                @SuppressLint("RequiresFeature")
                @Override
                public void onMessage(@NonNull WebMessagePortCompat port, @Nullable WebMessageCompat message) {
                    super.onMessage(port, message);
                    String data = message.getData();
                    try {
                        JSONObject json = new JSONObject(data);
                        WebEvent webEvent = new WebEvent(json);
                        if (webEventCallback != null) {
                            webEventCallback.invoke(MessagingWebView.this, webEvent);
                        }

                        JSONObject loggedEvent = webEvent.toJSON(!webEvent.getType().startsWith("AUTH"));
                        if (!webEvent.getType().equals(WebEvent.TYPE_ANALYTICS_LOG_EVENT) && !webEvent.getType().equals(WebEvent.TYPE_ANALYTICS_SET_USER_ID)) {
                            JSONObject properties = new JSONObject();
                            properties.put("target", "AppWebView");
                            properties.put("event", loggedEvent);
                            AnalyticsRepository.logEvent(AnalyticsRepository.TYPE_RECEIVED_WEB_EVENT, properties);
                        }
                    } catch (JSONException ex) {
                        ErrorRepository.captureException(ex);
                    }
                }
            });

            // Initial handshake - deliver WebMessageChannel port to JS.
            WebViewCompat.postWebMessage(
                    this,
                    new WebMessageCompat("", new WebMessagePortCompat[]{webMessageChannel[1]}),
                    baseURI
            );
        }
    }

    public void setWebEventCallback(Function2<MessagingWebView, WebEvent, Void> webEventCallback) {
        this.webEventCallback = webEventCallback;
    }

    private class JavascriptInterface {
        @android.webkit.JavascriptInterface
        public boolean isWebview() {
            return true;
        }

        @android.webkit.JavascriptInterface
        public void sendMessagePort() {
            mainHandler.post(() -> initializeWebMessageChannel());
        }
    }

}
