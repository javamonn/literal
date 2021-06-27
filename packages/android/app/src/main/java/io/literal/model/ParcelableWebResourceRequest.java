package io.literal.model;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.webkit.WebResourceRequest;

import java.util.HashMap;
import java.util.Map;

public class ParcelableWebResourceRequest implements Parcelable, WebResourceRequest {
    public static String KEY_METHOD = "METHOD";
    public static String KEY_REQUEST_HEADERS = "REQUEST_HEADERS";
    public static String KEY_URL = "URL";
    public static String KEY_HAS_GESTURE = "HAS_GESTURE";
    public static String KEY_IS_FOR_MAIN_FRAME = "IS_FOR_MAIN_FRAME";
    public static String KEY_IS_REDIRECT = "IS_REDIRECT";

    private final String method;
    private final HashMap<String, String> requestHeaders;
    private final Uri uri;
    private final boolean hasGesture;
    private final boolean isForMainFrame;
    private final boolean isRedirect;

    public ParcelableWebResourceRequest(WebResourceRequest webResourceRequest) {
        this.method = webResourceRequest.getMethod();
        this.requestHeaders = new HashMap<>(webResourceRequest.getRequestHeaders());
        this.uri = webResourceRequest.getUrl();
        this.hasGesture = webResourceRequest.hasGesture();
        this.isForMainFrame = webResourceRequest.isForMainFrame();
        this.isRedirect = webResourceRequest.isRedirect();
    }

    protected ParcelableWebResourceRequest(Parcel in) {
        Bundle input = new Bundle();
        input.readFromParcel(in);

        this.method = input.getString(KEY_METHOD);
        this.requestHeaders = (HashMap<String, String>) input.getSerializable(KEY_REQUEST_HEADERS);
        this.uri = input.getParcelable(KEY_URL);
        this.hasGesture = input.getBoolean(KEY_HAS_GESTURE);
        this.isForMainFrame = input.getBoolean(KEY_IS_FOR_MAIN_FRAME);
        this.isRedirect = input.getBoolean(KEY_IS_REDIRECT);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle output = new Bundle();
        output.putString(KEY_METHOD, this.method);
        output.putSerializable(KEY_REQUEST_HEADERS, this.requestHeaders);
        output.putParcelable(KEY_URL, this.uri);
        output.putBoolean(KEY_HAS_GESTURE, this.hasGesture);
        output.putBoolean(KEY_IS_FOR_MAIN_FRAME, this.isForMainFrame);
        output.putBoolean(KEY_IS_REDIRECT, this.isRedirect);
        output.writeToParcel(dest, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ParcelableWebResourceRequest> CREATOR = new Creator<ParcelableWebResourceRequest>() {
        @Override
        public ParcelableWebResourceRequest createFromParcel(Parcel in) {
            return new ParcelableWebResourceRequest(in);
        }

        @Override
        public ParcelableWebResourceRequest[] newArray(int size) {
            return new ParcelableWebResourceRequest[size];
        }
    };

    @Override
    public Uri getUrl() {
        return uri;
    }

    @Override
    public boolean isForMainFrame() {
        return isForMainFrame;
    }

    @Override
    public boolean isRedirect() {
        return isRedirect;
    }

    @Override
    public boolean hasGesture() {
        return hasGesture;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }
}
