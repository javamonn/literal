package io.literal.ui.fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.amazonaws.services.cognitoidentityprovider.model.NotAuthorizedException;
import com.amazonaws.services.cognitoidentityprovider.model.UserNotFoundException;
import com.amazonaws.services.cognitoidentityprovider.model.UsernameExistsException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.UUID;

import io.literal.BuildConfig;
import io.literal.R;
import io.literal.lib.ContentResolverLib;
import io.literal.lib.FileActivityResultCallback;
import io.literal.lib.WebEvent;
import io.literal.model.StorageObject;
import io.literal.model.User;
import io.literal.repository.AnalyticsRepository;
import io.literal.repository.ErrorRepository;
import io.literal.repository.SharedPreferencesRepository;
import io.literal.ui.view.MessagingWebView;
import io.literal.viewmodel.AppWebViewViewModel;
import io.literal.viewmodel.AuthenticationViewModel;

public class AppWebView extends Fragment {
    public static final String PARAM_INITIAL_URL = "PARAM_INITIAL_URL";
    public static final String PARAM_APP_WEB_VIEW_MODEL_KEY = "PARAM_APP_WEB_VIEW_MODEL_KEY";

    private final FileActivityResultCallback fileActivityResultCallback = new FileActivityResultCallback();

    private String paramInitialUrl;
    private String paramAppWebViewModelKey;

    private ActivityResultLauncher<String> getFileContent;
    private AppWebViewViewModel appWebViewViewModel;
    private AuthenticationViewModel authenticationViewModel;
    private io.literal.ui.view.AppWebView appWebView;

    public AppWebView() {
    }

    public static AppWebView newInstance(String initialUrl, String appWebViewModelKey) {
        AppWebView fragment = new AppWebView();
        Bundle args = new Bundle();
        args.putString(PARAM_INITIAL_URL, initialUrl);
        args.putString(PARAM_APP_WEB_VIEW_MODEL_KEY, appWebViewModelKey);
        fragment.setArguments(args);
        return fragment;
    }

    private void handleSignUp(MessagingWebView view, String email, String password) {
        authenticationViewModel.signUp(email, password, (e, user) -> {
            if (e != null) {
                ErrorRepository.captureException(e);
            }

            try {
                JSONObject result = new JSONObject();
                if (e == null && !user.isSignedOut()) {
                    result.put("user", user.toJSON());
                    result.put("shouldMergeUserIdentities", !SharedPreferencesRepository.getIsSignedOut(getContext()));
                } else {
                    String errorCode;
                    if (e instanceof UsernameExistsException) {
                        errorCode = "SIGN_UP_FAILED_USER_EXISTS";
                    } else {
                        errorCode = "SIGN_UP_FAILED";
                    }
                    result.put("error", errorCode);
                }

                getActivity().runOnUiThread(() -> {
                    view.postWebEvent(
                            new WebEvent(WebEvent.TYPE_AUTH_SIGN_UP_RESULT, UUID.randomUUID().toString(), result)
                    );
                });
            } catch (JSONException ex) {
                ErrorRepository.captureException(ex);
            }

            if (e == null) {
                SharedPreferencesRepository.setIsSignedOut(getContext(), false);
            }
        });
    }

    private void handleSignInGoogle(MessagingWebView view) {
        authenticationViewModel.signInGoogle(getActivity(), (e, user) -> {
            if (e != null) {
                ErrorRepository.captureException(e);
            }

            try {
                JSONObject result = new JSONObject();
                if (e == null && !user.isSignedOut()) {
                    result.put("user", user.toJSON());
                    result.put("shouldMergeUserIdentities", !SharedPreferencesRepository.getIsSignedOut(getContext()));
                } else {
                    String errorCode = "SIGN_IN_FAILED";
                    result.put("error", errorCode);
                }

                getActivity().runOnUiThread(() -> view.postWebEvent(
                        new WebEvent(WebEvent.TYPE_AUTH_SIGN_IN_GOOGLE_RESULT, UUID.randomUUID().toString(), result)
                ));
            } catch (JSONException ex) {
                ErrorRepository.captureException(ex);
            }

            if (e == null) {
                SharedPreferencesRepository.setIsSignedOut(getContext(), false);
            }
        });
    }

    private void handleSignIn(MessagingWebView view, String email, String password) {
        authenticationViewModel.signIn(email, password, (e, user) -> {
            if (e != null) {
                ErrorRepository.captureException(e);
            }

            try {
                JSONObject result = new JSONObject();
                if (e == null && !user.isSignedOut()) {
                    result.put("user", user.toJSON());
                    result.put("shouldMergeUserIdentities", !SharedPreferencesRepository.getIsSignedOut(getContext()));
                } else {
                    String errorCode;
                    if (e instanceof UserNotFoundException) {
                        errorCode = "SIGN_IN_FAILED_USER_NOT_FOUND";
                    } else if (e instanceof NotAuthorizedException) {
                        errorCode = "SIGN_IN_FAILED_NOT_AUTHORIZED";
                    } else {
                        errorCode = "SIGN_IN_FAILED";
                    }
                    result.put("error", errorCode);
                }

                getActivity().runOnUiThread(() -> {
                    view.postWebEvent(
                            new WebEvent(WebEvent.TYPE_AUTH_SIGN_IN_RESULT, UUID.randomUUID().toString(), result)
                    );
                });
            } catch (JSONException ex) {
                ErrorRepository.captureException(ex);
            }

            if (e == null) {
                SharedPreferencesRepository.setIsSignedOut(getContext(), false);
            }
        });
    }

    private void handleSignOut(MessagingWebView view) {
        authenticationViewModel.signOut()
                .whenComplete((e, _void) -> {
                    try {
                        JSONObject result = new JSONObject();
                        if (e != null) {
                            result.put("error", "SIGN_OUT_FAILED");
                        }
                        getActivity().runOnUiThread(() -> {
                            view.postWebEvent(
                                    new WebEvent(WebEvent.TYPE_AUTH_SIGN_OUT_RESULT, UUID.randomUUID().toString(), result)
                            );
                        });
                    } catch (JSONException e1) {
                        ErrorRepository.captureException(e1);
                    }

                    if (e == null) {
                        SharedPreferencesRepository.setIsSignedOut(getContext(), true);
                    }
                });
    }

    private void handleGetUser(MessagingWebView view) {
        User user = authenticationViewModel.getUser().getValue();
        view.postWebEvent(
                new WebEvent(WebEvent.TYPE_AUTH_GET_USER_RESULT, UUID.randomUUID().toString(), user.toJSON())
        );
    }

    private void handleActionViewURI(MessagingWebView view, String uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            ErrorRepository.captureException(e);
        }
    }

    private void handleGetAppVersion(MessagingWebView view) {
        try {
            JSONObject result = new JSONObject();
            result.put("versionName", BuildConfig.VERSION_NAME);
            view.postWebEvent(new WebEvent(WebEvent.TYPE_GET_APP_VERSION_RESULT, UUID.randomUUID().toString(), result));
        } catch (JSONException e) {
            ErrorRepository.captureException(e);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            paramInitialUrl = getArguments().getString(PARAM_INITIAL_URL);
            paramAppWebViewModelKey = getArguments().getString(PARAM_APP_WEB_VIEW_MODEL_KEY);
        }
        getFileContent = registerForActivityResult(new ActivityResultContracts.GetContent(), fileActivityResultCallback);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_app_web_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        appWebViewViewModel =
                paramAppWebViewModelKey != null
                        ? new ViewModelProvider(requireActivity()).get(paramAppWebViewModelKey, AppWebViewViewModel.class)
                        : new ViewModelProvider(requireActivity()).get(AppWebViewViewModel.class);
        authenticationViewModel = new ViewModelProvider(requireActivity()).get(AuthenticationViewModel.class);

        appWebView = view.findViewById(R.id.app_web_view);

        appWebView.initialize();
        appWebView.setExternalWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(android.webkit.WebView view, String url) {
                if (!appWebViewViewModel.getHasFinishedInitializing().getValue()) {
                    appWebViewViewModel.setHasFinishedInitializing(true);
                }
            }
        });
        appWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(android.webkit.WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                fileActivityResultCallback.setFilePathCallback(new ValueCallback<Uri[]>() {
                    @Override
                    public void onReceiveValue(Uri[] value) {
                        Uri[] absoluteUrls = new Uri[value.length];
                        for (int idx = 0; idx < value.length; idx++) {
                            File file = value[idx] != null ? ContentResolverLib.toFile(
                                    getActivity(),
                                    StorageObject.getDirectory(getContext(), StorageObject.Type.SCREENSHOT),
                                    value[idx],
                                    UUID.randomUUID().toString()
                            ) : null;
                            absoluteUrls[idx] = Uri.fromFile(file);
                        }
                        filePathCallback.onReceiveValue(absoluteUrls);
                    }
                });
                getFileContent.launch("image/*");
                return true;
            }
        });
        appWebView.setWebEventCallback((webView, event) -> {
            appWebViewViewModel.dispatchReceivedWebEvent(event);
            switch (event.getType()) {
                case WebEvent.TYPE_AUTH_SIGN_UP:
                    try {
                        String email = event.getData().getString("email");
                        String password = event.getData().getString("password");
                        this.handleSignUp(webView, email, password);
                    } catch (JSONException e) {
                        ErrorRepository.captureException(e);
                    }
                    return null;
                case WebEvent.TYPE_AUTH_SIGN_IN:
                    try {
                        String email = event.getData().getString("email");
                        String password = event.getData().getString("password");
                        this.handleSignIn(webView, email, password);
                    } catch (JSONException e) {
                        ErrorRepository.captureException(e);
                    }
                    return null;
                case WebEvent.TYPE_AUTH_SIGN_IN_GOOGLE:
                    this.handleSignInGoogle(webView);
                    return null;
                case WebEvent.TYPE_AUTH_GET_USER:
                    this.handleGetUser(webView);
                    return null;
                case WebEvent.TYPE_AUTH_SIGN_OUT:
                    this.handleSignOut(webView);
                    return null;
                case WebEvent.TYPE_ANALYTICS_LOG_EVENT:
                    try {
                        String eventType = event.getData().getString("type");
                        JSONObject eventProperties = event.getData().getJSONObject("properties");
                        AnalyticsRepository.logEvent(eventType, eventProperties);
                    } catch (JSONException e) {
                        ErrorRepository.captureException(e);
                    }
                    return null;
                case WebEvent.TYPE_ANALYTICS_SET_USER_ID:
                    try {
                        String userId = event.getData().getString("userId");
                        AnalyticsRepository.setUserId(userId);
                    } catch (JSONException e) {
                        ErrorRepository.captureException(e);
                    }
                    return null;
                case WebEvent.TYPE_ACTION_VIEW_URI:
                    try {
                        String uri = event.getData().getString("uri");
                        this.handleActionViewURI(webView, uri);
                    } catch (JSONException e) {
                        ErrorRepository.captureException(e);
                    }
                    return null;
                case WebEvent.TYPE_GET_APP_VERSION:
                    this.handleGetAppVersion(webView);
                    return null;
            }
            return null;
        });

        appWebViewViewModel.getWebEvents().observe(requireActivity(), (webEvents) -> {
            if (webEvents == null) {
                return;
            }

            webEvents.iterator().forEachRemaining((webEvent) -> {
                appWebView.postWebEvent(webEvent);
            });

            appWebViewViewModel.clearWebEvents();
        });

        if (savedInstanceState != null) {
            appWebView.restoreState(savedInstanceState);
        }

        appWebView.loadUrl(paramInitialUrl);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.appWebView != null) {
            appWebView.saveState(outState);
        }
    }


    public io.literal.ui.view.AppWebView getWebView() {
        return this.appWebView;
    }

    public void postWebEvent(WebEvent webEvent) {
        this.appWebView.postWebEvent(webEvent);
    }
}