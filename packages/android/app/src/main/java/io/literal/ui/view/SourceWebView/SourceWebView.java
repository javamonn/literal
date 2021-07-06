package io.literal.ui.view.SourceWebView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.MotionEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.core.view.MotionEventCompat;
import androidx.core.view.NestedScrollingChild;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.ViewCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.Optional;

import io.literal.lib.Callback;
import io.literal.lib.ResultCallback;
import io.literal.repository.ErrorRepository;
import io.literal.ui.view.MessagingWebView;
import kotlin.jvm.functions.Function1;

public class SourceWebView extends MessagingWebView implements NestedScrollingChild {

    private final CreateAnnotationActionModeCallback.Builder createAnnotationActionModeCallbackBuilder;

    private ResultCallback<Integer, Void> onGetTextSelectionMenu;

    private Optional<Client.Builder> clientBuilder;
    private Optional<Source> source;
    private Optional<Function1<Source, Void>> onSourceChanged;
    private Optional<Function1<Bitmap, Void>> onReceivedIcon;

    // NestedScrollingChild
    private int mLastY;
    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private int mNestedOffsetY;
    private NestedScrollingChildHelper mChildHelper;

    private WebChromeClient webChromeClient = new WebChromeClient() {
        @Override
        public void onReceivedIcon(WebView view, Bitmap icon) {
            super.onReceivedIcon(view, icon);
            source.ifPresent(s -> {
                s.setFavicon(Optional.ofNullable(icon));
                onReceivedIcon.ifPresent(cb -> cb.invoke(icon));
            });
        }
    };

    public SourceWebView(Context context) {
        this(context, null);
    }

    public SourceWebView(Context context, AttributeSet attrs) {
        super(context, attrs);

        source = Optional.empty();
        onSourceChanged = Optional.empty();
        createAnnotationActionModeCallbackBuilder = new CreateAnnotationActionModeCallback.Builder();

        mChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);

        super.initialize(Uri.parse("*"));
        this.setWebChromeClient(webChromeClient);
    }

    public void setSource(@NonNull Source source) {
        this.source = Optional.of(source);

        if (!clientBuilder.isPresent()) {
            ErrorRepository.captureException(new Exception("Expected clientBuilder to not be empty."));
            return;
        }

        Client client = clientBuilder.get()
                .setSource(source)
                .setOnReceivedIcon((icon) -> {
                    webChromeClient.onReceivedIcon(this, icon);
                    return null;
                })
                .setOnWebResourceRequest((webResourceRequest) -> {
                    source.getPageWebResourceRequests().add(webResourceRequest);
                    return null;
                })
                .setOnSourceChanged((newSource) -> {
                    setSource(newSource);
                    onSourceChanged.ifPresent(cb -> cb.invoke(newSource));
                    return null;
                })
                .build();

        this.setWebViewClient(client);

        Optional<URI> sourceURI = client.getSourceURI();
        if (!sourceURI.isPresent()) {
            ErrorRepository.captureException(new Exception("Invalid Source: getSourceURI returned empty."));
            return;
        }

        if (!Optional.ofNullable(getUrl()).map(u -> u.equals(sourceURI.get().toString())).orElse(false)) {
            this.loadUrl(sourceURI.get().toString());
        }
    }


    public Optional<Source> getSource() {
        return source;
    }

    public void setOnSourceChanged(Function1<Source, Void> onSourceChanged) {
        this.onSourceChanged = Optional.of(onSourceChanged);
    }

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback) {
        if (onGetTextSelectionMenu != null) {
            ActionMode.Callback2 cb = createAnnotationActionModeCallbackBuilder
                    .setOriginalCallback((ActionMode.Callback2) callback)
                    .setMenu(onGetTextSelectionMenu.invoke(null, null))
                    .build();
            return super.startActionMode(cb);
        }
        return super.startActionMode(callback);
    }

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback, int type) {
        if (callback instanceof EditAnnotationActionModeCallback) {
            ActionMode actionMode = super.startActionMode(callback, type);
            return actionMode;
        } else if (onGetTextSelectionMenu != null) {
            // Default Chrome text selection action mode, which we intercept to provide different menu options.
            ActionMode.Callback2 cb = createAnnotationActionModeCallbackBuilder
                    .setOriginalCallback((ActionMode.Callback2) callback)
                    .setMenu(onGetTextSelectionMenu.invoke(null, null))
                    .build();
            return super.startActionMode(cb, type);
        }
        return super.startActionMode(callback, type);
    }

    public ActionMode startEditAnnotationActionMode(
            String getAnnotationBoundingBoxScript,
            Rect initialAnnotationBoundingBox,
            Callback<Void, Void> onEditAnnotation,
            Callback<Void, Void> onDeleteAnnotation
    ) {
        EditAnnotationActionModeCallback actionModeCallback = new EditAnnotationActionModeCallback(initialAnnotationBoundingBox, onEditAnnotation, onDeleteAnnotation);
        ActionMode actionMode = super.startActionMode(actionModeCallback, ActionMode.TYPE_FLOATING);

        setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> evaluateJavascript(getAnnotationBoundingBoxScript, value -> {
            try {
                JSONObject boundingBox = new JSONObject(value);
                int left = boundingBox.getInt("left");
                int top = boundingBox.getInt("top");
                int right = boundingBox.getInt("right");
                int bottom = boundingBox.getInt("bottom");

                actionModeCallback.setAnnotationBoundingBox(new Rect(left, top, right, bottom));
                actionMode.invalidateContentRect();
            } catch (JSONException ex) {
                ErrorRepository.captureException(ex);
            }
        }));

        return actionMode;
    }

    public void finishEditAnnotationActionMode(ActionMode actionMode) {
        setOnScrollChangeListener(null);
        actionMode.finish();
    }

    public void setOnAnnotationCreated(Function1<ActionMode, Void> onAnnotationCreated) {
        this.createAnnotationActionModeCallbackBuilder.setOnAnnotationCreated(onAnnotationCreated);
    }

    public void setOnAnnotationCommitEdit(Function1<ActionMode, Void> onAnnotationCommitEdit) {
        this.createAnnotationActionModeCallbackBuilder.setOnAnnotationCommitEdit(onAnnotationCommitEdit);
    }

    public void setOnAnnotationCancelEdit(Function1<ActionMode, Void> onAnnotationCancelEdit) {
        this.createAnnotationActionModeCallbackBuilder.setOnAnnotationCancelEdit(onAnnotationCancelEdit);
    }

    public void setOnGetTextSelectionMenu(ResultCallback<Integer, Void> onGetTextSelectionMenu) {
        this.onGetTextSelectionMenu = onGetTextSelectionMenu;
    }

    public boolean handleBackPressed() {
        if (this.canGoBack()) {
            this.goBack();
            return true;
        }

        return false;
    }

    public void setClientBuilder(@NonNull Client.Builder clientBuilder) {
        this.clientBuilder = Optional.of(clientBuilder);
    }

    public Optional<Client.Builder> getClientBuilder() {
        return this.clientBuilder;
    }

    public void setOnReceivedIcon(Function1<Bitmap, Void> onReceivedIcon) {
        this.onReceivedIcon = Optional.of(onReceivedIcon);
    }

    //
    // Begin NestedScrollingChild implements
    //
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean returnValue = false;

        MotionEvent event = MotionEvent.obtain(ev);
        final int action = MotionEventCompat.getActionMasked(event);
        if (action == MotionEvent.ACTION_DOWN) {
            mNestedOffsetY = 0;
        }
        int eventY = (int) event.getY();
        event.offsetLocation(0, mNestedOffsetY);
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                int deltaY = mLastY - eventY;
                // NestedPreScroll
                if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset)) {
                    deltaY -= mScrollConsumed[1];
                    mLastY = eventY - mScrollOffset[1];
                    event.offsetLocation(0, -mScrollOffset[1]);
                    mNestedOffsetY += mScrollOffset[1];
                }
                returnValue = super.onTouchEvent(event);

                // NestedScroll
                if (dispatchNestedScroll(0, mScrollOffset[1], 0, deltaY, mScrollOffset)) {
                    event.offsetLocation(0, mScrollOffset[1]);
                    mNestedOffsetY += mScrollOffset[1];
                    mLastY -= mScrollOffset[1];
                }
                break;
            case MotionEvent.ACTION_DOWN:
                returnValue = super.onTouchEvent(event);
                mLastY = eventY;
                // start NestedScroll
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                break;
            default:
                returnValue = super.onTouchEvent(event);
                // end NestedScroll
                stopNestedScroll();
                break;
        }
        return returnValue;
    }

    // Nested Scroll implements
    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed,
                                        int[] offsetInWindow) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    //
    // End NestedScrollingChild implements
    //
}
