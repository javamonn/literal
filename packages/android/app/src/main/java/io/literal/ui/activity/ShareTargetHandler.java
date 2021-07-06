package io.literal.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.lifecycle.ViewModelProvider;

import com.amazonaws.amplify.generated.graphql.CreateAnnotationMutation;
import com.amazonaws.amplify.generated.graphql.GetAnnotationQuery;
import com.amazonaws.mobile.client.UserState;
import com.amazonaws.mobileconnectors.appsync.ClearCacheException;
import com.amazonaws.mobileconnectors.appsync.ClearCacheOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.UUID;

import io.literal.R;
import io.literal.factory.AppSyncClientFactory;
import io.literal.lib.AnnotationLib;
import io.literal.lib.Constants;
import io.literal.lib.ContentResolverLib;
import io.literal.lib.DomainMetadata;
import io.literal.lib.JsonArrayUtil;
import io.literal.lib.WebEvent;
import io.literal.lib.WebRoutes;
import io.literal.model.Annotation;
import io.literal.model.StorageObject;
import io.literal.model.User;
import io.literal.repository.AnalyticsRepository;
import io.literal.repository.AnnotationRepository;
import io.literal.repository.ErrorRepository;
import io.literal.repository.NotificationRepository;
import io.literal.repository.ToastRepository;
import io.literal.service.AnnotationService;
import io.literal.service.CreateAnnotationIntent;
import io.literal.ui.fragment.AppWebView;
import io.literal.ui.fragment.AppWebViewBottomSheetAnimator;
import io.literal.ui.fragment.SourceWebView;
import io.literal.ui.view.SourceWebView.Source;
import io.literal.viewmodel.AppWebViewViewModel;
import io.literal.viewmodel.AuthenticationViewModel;
import io.literal.viewmodel.SourceWebViewViewModel;
import io.sentry.SentryLevel;

public class ShareTargetHandler extends InstrumentedActivity {
    public static final String RESULT_EXTRA_ANNOTATIONS = "RESULT_EXTRA_ANNOTATIONS";
    private static final String APP_WEB_VIEW_FRAGMENT_NAME = "APP_WEB_VIEW_FRAGMENT";
    private static final String SOURCE_WEB_VIEW_FRAGMENT_NAME = "SOURCE_WEB_VIEW_FRAGMENT";
    private AppWebViewViewModel appWebViewViewModel;
    private SourceWebViewViewModel sourceWebViewViewModel;
    private AuthenticationViewModel authenticationViewModel;
    private AppWebView appWebViewFragment;
    private SourceWebView sourceWebViewFragment;
    private FragmentContainerView bottomSheetFragmentContainer;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_target_handler);

        Intent intent = getIntent();

        authenticationViewModel = new ViewModelProvider(this).get(AuthenticationViewModel.class);

        NotificationRepository.createNewAnnotationNotificationChannel(this);
        NotificationRepository.createNewExternalTargetNotificationChannel(this);

        authenticationViewModel.initialize(this).whenComplete((user, e) -> runOnUiThread(() -> {
            if (savedInstanceState == null) {
                this.handleIntent(intent, user);
            } else {
                Fragment fragment;
                fragment = getSupportFragmentManager().getFragment(savedInstanceState, APP_WEB_VIEW_FRAGMENT_NAME);
                if (fragment == null) {
                    fragment = getSupportFragmentManager().getFragment(savedInstanceState, SOURCE_WEB_VIEW_FRAGMENT_NAME);
                }
                if (fragment != null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .setReorderingAllowed(true)
                            .add(R.id.fragment_container, fragment)
                            .commit();
                }
            }
        }));
    }

    private void handleIntent(Intent intent, User user) {
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null && type.startsWith("image/")) {
            handleCreateFromImage(intent, user);
        } else if (Intent.ACTION_SEND.equals(action) && type != null && type.equals("text/plain")) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (Patterns.WEB_URL.matcher(text).matches()) {
                handleCreateFromSource(intent, user);
            } else {
                handleCreateFromText(intent, user);
            }
        } else {
            handleSendNotSupported();
        }

        try {
            JSONObject properties = new JSONObject();
            properties.put("name", "ShareTargetHandler");
            properties.put("action", action);
            properties.put("type", type);
            properties.put("extra", intent.getStringExtra(Intent.EXTRA_TEXT));
            AnalyticsRepository.logEvent(AnalyticsRepository.TYPE_ACTIVITY_START, properties);
        } catch (JSONException e) {
            ErrorRepository.captureException(e);
        }
    }


    private void installSourceWebView(String sourceWebViewUri, String appWebViewUri) {
        sourceWebViewViewModel = new ViewModelProvider(this).get(SourceWebViewViewModel.class);

        sourceWebViewViewModel.getSourceHasFinishedInitializing().observe(this, hasFinishedInitializing -> {
            ViewGroup splash = findViewById(R.id.share_target_handler_splash);
            Log.i("installSourceWebview", "hasFinishedInitializing: " + hasFinishedInitializing);
            if (hasFinishedInitializing && splash.getVisibility() == View.VISIBLE) {
                splash.setVisibility(View.INVISIBLE);
                ToastRepository.show(this, R.string.toast_create_from_source);
            }
        });


        bottomSheetFragmentContainer = findViewById(R.id.bottom_sheet_fragment_container);

        // initialize view model for managing app web view bottom sheet
        appWebViewViewModel = new ViewModelProvider(this).get(AppWebViewViewModel.class);
        appWebViewViewModel.getBottomSheetState().observe(this, bottomSheetState -> {
            AppWebViewBottomSheetAnimator.handleBottomSheetStateChange(
                    bottomSheetFragmentContainer,
                    sourceWebViewViewModel.getFocusedAnnotation().orElse(null),
                    getResources(),
                    bottomSheetState,
                    (_e, webEvent) -> appWebViewFragment.postWebEvent(webEvent)
            );
        });

        appWebViewViewModel.setBottomSheetState(BottomSheetBehavior.STATE_HIDDEN);

        sourceWebViewFragment = SourceWebView.newInstance(
                sourceWebViewUri,
                null,
                null,
                R.drawable.done_white
        );
        sourceWebViewFragment.setOnToolbarPrimaryActionCallback((annotations, source) -> {
            this.handleCreateFromSourceDone(annotations);
            return null;
        });

        appWebViewFragment = AppWebView.newInstance(appWebViewUri, null);
        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragment_container, sourceWebViewFragment)
                .add(R.id.bottom_sheet_fragment_container, appWebViewFragment)
                .commit();
    }

    private void installAppWebView(String appWebViewUri) {
        // Remove unused bottom sheet fragment container
        ViewGroup layout = findViewById(R.id.layout);
        View bottomSheetContainer = findViewById(R.id.bottom_sheet_fragment_container);
        if (layout != null && bottomSheetContainer != null) {
            layout.removeView(bottomSheetContainer);
        }

        appWebViewViewModel = new ViewModelProvider(this).get(AppWebViewViewModel.class);
        appWebViewViewModel.getHasFinishedInitializing().observe(this, hasFinishedInitializing -> {
            ViewGroup splash = findViewById(R.id.share_target_handler_splash);
            splash.setVisibility(hasFinishedInitializing ? View.INVISIBLE : View.VISIBLE);
        });
        appWebViewFragment = AppWebView.newInstance(appWebViewUri, null);
        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragment_container, appWebViewFragment)
                .commit();
    }

    private void handleCreateFromSource(Intent intent, User user) {
        if (!user.getState().equals(UserState.SIGNED_IN) && !user.getState().equals(UserState.GUEST)) {
            handleSignedOut();
            return;
        }
        String sourceWebViewUri = intent.getStringExtra(Intent.EXTRA_TEXT);
        String appWebViewUri = WebRoutes.creatorsIdWebview(user.getEncodedIdentityId());
        installSourceWebView(sourceWebViewUri, appWebViewUri);
    }

    private void handleCreateFromSourceDone(Annotation[] annotations) {
        if (annotations != null && annotations.length > 0) {
            String resultAnnotationsJson = "";
            try {
                resultAnnotationsJson = JsonArrayUtil.stringifyObjectArray(annotations, Annotation::toJson).toString();
            } catch (JSONException e) {
                ErrorRepository.captureException(e);
            }
            Intent intent = new Intent();
            intent.putExtra(RESULT_EXTRA_ANNOTATIONS, resultAnnotationsJson);
            setResult(RESULT_OK, intent);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    private void handleCreateFromText(Intent intent, User user) {
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (!user.getState().equals(UserState.SIGNED_IN) && !user.getState().equals(UserState.GUEST)) {
                handleSignedOut();
            return;
        }
        Annotation annotation = Annotation.fromText(text, user.getAppSyncIdentity());
        String uri = WebRoutes.creatorsIdAnnotationsNewAnnotationId(
                user.getEncodedIdentityId(),
                AnnotationLib.idComponentFromId(annotation.getId())
        );
        installAppWebView(uri);
        appWebViewViewModel.getReceivedWebEvents().observe(ShareTargetHandler.this, (webEvents) -> {
            if (webEvents == null) {
                return;
            }

            webEvents.iterator().forEachRemaining(webEvent -> {
                if (webEvent.getType().equals(WebEvent.TYPE_ACTIVITY_FINISH)) {
                    displayAnnotationCreatedNotification(user, annotation);
                    finish();
                }
            });

            appWebViewViewModel.clearReceivedWebEvents();
        });

        (new CreateAnnotationIntent.Builder())
                .setAnnotations(new Annotation[]{annotation})
                .setId(UUID.randomUUID().toString())
                .build()
                .toIntent()
                .ifPresent(this::startService);

    }

    private void handleCreateFromImage(Intent intent, User user) {
        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (!user.getState().equals(UserState.SIGNED_IN) && !user.getState().equals(UserState.GUEST)) {
            handleSignedOut();
            return;
        }

        StorageObject storageObject = StorageObject.createFromContentResolverURI(this, StorageObject.Type.SCREENSHOT, imageUri);
        Annotation annotation = Annotation.fromScreenshot(storageObject, user.getAppSyncIdentity());
        String uri = WebRoutes.creatorsIdAnnotationsNewAnnotationId(
                user.getEncodedIdentityId(),
                AnnotationLib.idComponentFromId(annotation.getId())
        );
        installAppWebView(uri);
        appWebViewViewModel.getReceivedWebEvents().observe(ShareTargetHandler.this, (webEvents) -> {
            if (webEvents == null) {
                return;
            }

            webEvents.iterator().forEachRemaining(webEvent -> {
                if (webEvent.getType().equals(WebEvent.TYPE_ACTIVITY_FINISH)) {
                    displayAnnotationCreatedNotification(user, annotation);
                    finish();
                }
            });

            appWebViewViewModel.clearReceivedWebEvents();
        });

        (new CreateAnnotationIntent.Builder())
                .setAnnotations(new Annotation[]{annotation})
                .setId(UUID.randomUUID().toString())
                .build()
                .toIntent()
                .ifPresent(this::startService);
    }

    private void displayAnnotationCreatedNotification(User user, Annotation annotation) {
        // Try to refetch the annotation, as it may have changed within the WebView.
        try {
            AppSyncClientFactory.getInstanceForUser(this, user).clearCaches(ClearCacheOptions.builder().clearQueries().build());
        } catch (ClearCacheException e) {
            ErrorRepository.captureException(e);
        }

        AnnotationRepository.getAnnotationQuery(
                AppSyncClientFactory.getInstanceForUser(this, user),
                GetAnnotationQuery.builder().creatorUsername(user.getAppSyncIdentity()).id(annotation.getId()).build(),
                (e, updatedAnnotation) -> {
                    if (e != null) {
                        ErrorRepository.captureException(e);
                        return;
                    }
                    // FIXME: Need to broadcast the updated annotation.
                    GetAnnotationQuery.GetAnnotation updatedAnnotationData = updatedAnnotation.getAnnotation();
                    if (updatedAnnotationData != null) {
                        NotificationRepository.annotationCreatedNotification(this, user, updatedAnnotationData);
                    }
                }
        );
    }

    private void handleSendNotSupported() {
        // TODO: implement fallback handling, e.g. display a "This does not look like a screenshot" UI
    }

    private void handleSignedOut() {
        ErrorRepository.captureBreadcrumb(ErrorRepository.CATEGORY_AUTHENTICATION, "User is signed out, prompting Sign in.", SentryLevel.INFO);
        Intent intent = new Intent(this, MainActivity.class);
        intent.setData(Uri.parse(WebRoutes.authenticate() + "?forResult=true"));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_TASK_ON_HOME);

        // We must set FLAG_ACTIVITY_NEW_TASK for MainActivity to spawn Amplify hosted authentication UI correctly. With the flag
        // set, startActivityForResult no longer works, so we shuttle along an intent to fire when MainActivity is finished.
        Intent manualResultIntent = new Intent(getIntent());
        manualResultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_TASK_ON_HOME | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        manualResultIntent.setClass(this, ShareTargetHandler.class);
        intent.putExtra(Constants.INTENT_MANUAL_FOR_RESULT_DATA, manualResultIntent);

        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (sourceWebViewFragment != null && sourceWebViewFragment.getWebView().handleBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (appWebViewFragment != null) {
            getSupportFragmentManager().putFragment(outState, APP_WEB_VIEW_FRAGMENT_NAME, appWebViewFragment);
        }
        if (sourceWebViewFragment != null) {
            getSupportFragmentManager().putFragment(outState, APP_WEB_VIEW_FRAGMENT_NAME, sourceWebViewFragment);
        }
    }
}
