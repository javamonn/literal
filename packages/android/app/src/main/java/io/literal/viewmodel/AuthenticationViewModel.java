package io.literal.viewmodel;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserState;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.client.UserStateListener;
import com.amazonaws.mobile.client.results.Tokens;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;

import io.literal.factory.AWSMobileClientFactory;
import io.literal.repository.AuthenticationRepository;
import io.literal.repository.ErrorRepository;

public class AuthenticationViewModel extends ViewModel {


    private final MutableLiveData<UserStateDetails> userStateDetails = new MutableLiveData<>(null);
    private final MutableLiveData<Tokens> tokens = new MutableLiveData<>(null);
    private final MutableLiveData<String> username = new MutableLiveData<>(null);
    private final MutableLiveData<String> identityId = new MutableLiveData<>(null);
    private final MutableLiveData<Map<String, String>> userAttributes = new MutableLiveData<>(null);
    private final CountDownLatch hasInitializedLatch = new CountDownLatch(1);

    public MutableLiveData<UserStateDetails> getUserStateDetails() {
        return userStateDetails;
    }

    public MutableLiveData<Tokens> getTokens() {
        if (this.isSignedOut()) {
            tokens.postValue(null);
        } if (tokens.getValue() == null) {
            try {
                tokens.postValue(AuthenticationRepository.getTokens());
            } catch (Exception e) {
                ErrorRepository.captureException(e);
            }
        }
        return tokens;
    }

    public MutableLiveData<Map<String, String>> getUserAttributes() {
        if (this.isSignedOut()) {
            userAttributes.postValue(null);
        } else if (userAttributes.getValue() == null) {
            try {
                userAttributes.postValue(AuthenticationRepository.getUserAttributes());
            } catch (Exception e) {
                ErrorRepository.captureException(e);
            }
        }

        return userAttributes;
    }

    public MutableLiveData<String> getUsername() {
        if (this.isSignedOut()) {
            username.postValue(null);
        } else if (username.getValue() == null) {
            try {
                username.postValue(AuthenticationRepository.getUsername());
            } catch (Exception e) {
                ErrorRepository.captureException(e);
            }
        }
        return username;
    }

    public MutableLiveData<String> getIdentityId() {
        if (this.isSignedOut()) {
            identityId.postValue(null);
        } else if (identityId.getValue() == null) {
            identityId.postValue(AuthenticationRepository.getIdentityId());
        }
        return identityId;
    }

    public void initialize(ThreadPoolExecutor executor, Activity activity) {
        executor.execute(() -> AWSMobileClientFactory.initializeClient(activity.getApplicationContext(), new Callback<UserStateDetails>() {
            @Override
            public void onResult(UserStateDetails result) {
                userStateDetails.postValue(result);
                if (result.getUserState().equals(UserState.SIGNED_IN)) {
                    initializeValues();
                }
                hasInitializedLatch.countDown();
            }

            @Override
            public void onError(Exception e) {
                ErrorRepository.captureException(e);
            }
        }));

        AWSMobileClient.getInstance().addUserStateListener(userStateDetails::postValue);
    }

    public boolean isSignedOut() {
        if (userStateDetails.getValue() == null) {
            return true;
        }
        return userStateDetails.getValue().getUserState() != UserState.SIGNED_IN;
    }

    private void asyncInitializeValues() {
        tokens.postValue(null);
        AuthenticationRepository.getTokens((e, tokensResult) -> {
            tokens.postValue(tokensResult);
        });
        userAttributes.postValue(null);
        AuthenticationRepository.getUserAttributes((e, userInfoResult) -> {
            userAttributes.postValue(userInfoResult);
        });
        identityId.postValue(AuthenticationRepository.getIdentityId());
        AuthenticationRepository.getUsername((e, usernameResult) -> {
            username.postValue(usernameResult);
        });
    }

    private void initializeValues() {
        try {
            this.tokens.postValue(AuthenticationRepository.getTokens());
            this.userAttributes.postValue(AuthenticationRepository.getUserAttributes());
            this.identityId.postValue(AuthenticationRepository.getIdentityId());
            this.username.postValue(AuthenticationRepository.getUsername());
        } catch (Exception e) {
            ErrorRepository.captureException(e);
        }
    }

    public void awaitInitialization() {
        try {
            hasInitializedLatch.await();
        } catch (InterruptedException e) {
            ErrorRepository.captureException(e);
        }
    }

    public void awaitInitialization(ThreadPoolExecutor executor, io.literal.lib.Callback<InterruptedException, Void> callback) {
        executor.execute(() -> {
            try {
                hasInitializedLatch.await();
                callback.invoke(null, null);
            } catch (InterruptedException e) {
                callback.invoke(e, null);
            }
        });
    }

    public void signInGoogle(Activity activity, AuthenticationRepository.Callback<Void> callback) {
        AuthenticationRepository.signInGoogle(activity, (e, userStateDetails) -> {
            try {
                if (e != null) {
                    callback.invoke(e, null);
                    return;
                }

                this.userStateDetails.postValue(userStateDetails);
                initializeValues();

                callback.invoke(null, null);
            } catch (Exception getTokensException) {
                callback.invoke(getTokensException, null);
            }
        });
    }

    public void signUp(String email, String password, AuthenticationRepository.Callback<Void> callback) {
        AuthenticationRepository.signUp(email, password, (e, userStateDetails) -> {
            try {
                if (e != null) {
                    callback.invoke(e, null);
                    return;
                }

                this.userStateDetails.postValue(userStateDetails);
                initializeValues();

                callback.invoke(null, null);
            } catch (Exception getTokensException) {
                callback.invoke(getTokensException, null);
            }
        });
    }

    public void signIn(String email, String password, io.literal.lib.Callback<Exception, Void> callback) {
        AuthenticationRepository.signIn(email, password, (e, userStateDetails) -> {
            if (e != null) {
                callback.invoke(e, null);
                return;
            }

            this.userStateDetails.postValue(userStateDetails);
            initializeValues();

            callback.invoke(null, null);
        });
    }
}
