package io.literal.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Pair;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.UserStateListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.services.s3.AmazonS3URI;

import org.json.JSONException;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.literal.factory.AWSMobileClientFactory;
import io.literal.factory.AppSyncClientFactory;
import io.literal.lib.Callback;
import io.literal.lib.Constants;
import io.literal.lib.DateUtil;
import io.literal.lib.JsonArrayUtil;
import io.literal.model.Annotation;
import io.literal.model.SpecificTarget;
import io.literal.model.State;
import io.literal.model.StorageObject;
import io.literal.model.TimeState;
import io.literal.model.User;
import io.literal.model.WebArchive;
import io.literal.repository.AnnotationRepository;
import io.literal.repository.ErrorRepository;
import io.literal.repository.NotificationRepository;
import io.literal.ui.MainApplication;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function3;

public class AnnotationService extends Service {
    public static String ACTION_BROADCAST_CREATED_ANNOTATIONS = Constants.NAMESPACE + "ACTION_BROADCAST_CREATED_ANNOTATIONS";
    public static String ACTION_BROADCAST_UPDATED_ANNOTATION = Constants.NAMESPACE + "ACTION_BROADCAST_UPDATED_ANNOTATION";

    public static String EXTRA_ID = Constants.NAMESPACE + "EXTRA_ID";
    public static String EXTRA_ANNOTATIONS = Constants.NAMESPACE + "EXTRA_ANNOTATIONS";
    public static String EXTRA_ANNOTATION = Constants.NAMESPACE + "EXTRA_ANNOTATION";

    private User user;
    private UserStateListener userStateListener;

    public AnnotationService() {
    }

    public static void broadcastCreatedAnnotations(Context context, String intentId, Annotation[] annotations) {
        try {
            Intent intent = new Intent();
            intent.setAction(ACTION_BROADCAST_CREATED_ANNOTATIONS);
            intent.putExtra(
                    EXTRA_ANNOTATIONS,
                    JsonArrayUtil.stringifyObjectArray(annotations, Annotation::toJson).toString()
            );
            intent.putExtra(EXTRA_ID, intentId);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        } catch (JSONException ex) {
            ErrorRepository.captureException(ex);
        }
    }

    public static void broadcastUpdatedAnnotation(Context context, String intentId, Annotation annotation) {
        try {
            Intent intent = new Intent();
            intent.setAction(ACTION_BROADCAST_UPDATED_ANNOTATION);
            intent.putExtra(
                    EXTRA_ANNOTATION,
                    annotation.toJson().toString()
            );
            intent.putExtra(EXTRA_ID, intentId);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        } catch (JSONException ex) {
            ErrorRepository.captureException(ex);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initialize(getBaseContext());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (userStateListener != null) {
            AWSMobileClient.getInstance().removeUserStateListener(userStateListener);
            userStateListener = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Optional<CreateAnnotationIntent> createAnnotationIntentOpt = CreateAnnotationIntent.fromIntent(getBaseContext(), intent);
        if (createAnnotationIntentOpt.isPresent()) {
            ((MainApplication) getApplication()).getThreadPoolExecutor().execute(() -> initialize(getBaseContext())
                    .thenCompose((user) ->
                            handleCreateAnnotation(
                                    getBaseContext(),
                                    user,
                                    createAnnotationIntentOpt.get()
                            )
                    ).whenComplete((_void, ex) -> {
                        if (ex != null) {
                            future.completeExceptionally(ex);
                        } else {
                            future.complete(null);
                        }
                    }));
        } else {
            future.complete(null);
        }

        future.whenComplete((_void, e) -> {
            if (e != null) {
                ErrorRepository.captureException(e);
            }
            stopSelfResult(startId);
        });

        return START_REDELIVER_INTENT;
    }

    private CompletableFuture<User> initialize(Context context) {
        if (user != null) {
            return CompletableFuture.completedFuture(user);
        }

        CompletableFuture<User> userFuture = AWSMobileClientFactory.initializeClient(getApplicationContext()).thenCompose(User::getInstance);

        userFuture.whenComplete((instance, error) -> {
            userStateListener = User.subscribe((e1, newUser) -> {
                user = newUser;
                return null;
            });

            if (error != null) {
                ErrorRepository.captureException(error);
                return;
            }

            user = instance;
        });

        return userFuture;
    }

    private CompletableFuture<HashMap<String, AmazonS3URI>> uploadWebArchives(
            Context context,
            User user,
            CreateAnnotationIntent intent,
            Function1<Integer, Void> onUploadProgress
    ) {

        ArrayList<Function1<Callback<Exception, StorageObject>, TransferObserver>> uploadThunks = new ArrayList<>();
        HashMap<Integer, Pair<Long, Long>> uploadProgressById = new HashMap<>();
        Function3<Integer, Long, Long, Void> handleUploadProgress = (id, bytesCurrent, bytesTotal) -> {
            uploadProgressById.put(id, new Pair<>(bytesCurrent, bytesTotal));
            Pair<Long, Long> progress = uploadProgressById.values().stream()
                    .reduce(new Pair<>(0L, 0L), (acc, item) -> new Pair<>(acc.first + item.first, acc.second + item.second));
            int progressOutOf100 = progress.second.compareTo(0L) == 0 || progress.first.compareTo(0L) == 0
                    ? 0
                    : (int) (((double) progress.first / progress.second) * 100);
            onUploadProgress.invoke(progressOutOf100);
            return null;
        };

        List<CompletableFuture<Pair<String, AmazonS3URI>>> storageObjectUploadFutures = intent.getWebArchives().orElse(new HashMap<>()).entrySet().stream().map(entry -> {
            String annotationId = entry.getKey();
            WebArchive webArchive = entry.getValue();

            CompletableFuture<AmazonS3URI> future;
            if (!webArchive.getStorageObject().getStatus().equals(StorageObject.Status.UPLOAD_REQUIRED)) {
                future = CompletableFuture.completedFuture(webArchive.getStorageObject().getAmazonS3URI(context, user));
            } else {
                future = webArchive.compile(context, user)
                        .thenCompose(_result -> webArchive.getStorageObject().upload(context, user, handleUploadProgress));
            }

            return future.thenApply((storageObjectURI) -> new Pair<>(annotationId, storageObjectURI));

        }).collect(Collectors.toList());
        onUploadProgress.invoke(0);

        return CompletableFuture.allOf(storageObjectUploadFutures.toArray(new CompletableFuture[0])).thenApply((_void) -> storageObjectUploadFutures.stream().map((f) -> f.getNow(null)).collect(
                HashMap::new,
                (agg, pair) -> agg.put(pair.first, pair.second),
                HashMap::putAll
        ));
    }

    private CompletableFuture<Void> handleCreateAnnotation(Context context, User user, CreateAnnotationIntent createAnnotationIntent) {
        Function1<Integer, Void> onDisplayNotificationProgress = (Integer uploadProgress) -> {
            if (!createAnnotationIntent.getFaviconBitmap().isPresent() || !createAnnotationIntent.getDisplayURI().isPresent()) {
                return null;
            }

            NotificationRepository.sourceCreatedNotificationStart(
                    context,
                    user.getAppSyncIdentity(),
                    createAnnotationIntent.getDisplayURI().get(),
                    createAnnotationIntent.getFaviconBitmap().get(),
                    new Pair<>(100, Math.max(uploadProgress - 5, 0)) // subtract 5 to fake mutation progress
            );

            return null;
        };

        return uploadWebArchives(context, user, createAnnotationIntent, onDisplayNotificationProgress)
                .thenCompose((uploadedWebArchives) -> {
                    List<Annotation> annotationsWithCached = Arrays.stream(createAnnotationIntent.getAnnotations())
                            .map((annotation) -> Optional.ofNullable(uploadedWebArchives.getOrDefault(annotation.getId(), null))
                                    .flatMap((cachedWebArchiveURI) ->
                                            Arrays.stream(annotation.getTarget())
                                                    .filter(t -> t instanceof SpecificTarget)
                                                    .findFirst()
                                                    .map((specificTarget) -> {
                                                        SpecificTarget updatedSpecifcTarget = new SpecificTarget.Builder((SpecificTarget) specificTarget)
                                                                .setState(new State[]{
                                                                        new TimeState(
                                                                                new URI[]{cachedWebArchiveURI.getURI()},
                                                                                new String[]{DateUtil.toISO8601UTC(new Date())}
                                                                        )
                                                                })
                                                                .build();

                                                        return annotation.updateTarget(updatedSpecifcTarget);
                                                    })

                                    )
                                    .orElse(annotation))
                            .collect(Collectors.toList());

                    return AnnotationRepository.createAnnotations(
                            AppSyncClientFactory.getInstanceForUser(context, user),
                            annotationsWithCached.toArray(new Annotation[0])
                    );
                })
                .thenApply(_results -> (Void) null)
                .whenComplete((_void, e) -> {
                    if (e != null) {
                        NotificationRepository.sourceCreatedNotificationError(
                                context,
                                user.getAppSyncIdentity(),
                                createAnnotationIntent.getDisplayURI()
                        );
                    } else {
                        broadcastCreatedAnnotations(context, createAnnotationIntent.getId(), createAnnotationIntent.getAnnotations());
                        NotificationRepository.sourceCreatedNotificationComplete(
                                context,
                                user.getAppSyncIdentity(),
                                createAnnotationIntent.getDisplayURI(),
                                createAnnotationIntent.getFaviconBitmap()
                        );
                    }
                });
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}