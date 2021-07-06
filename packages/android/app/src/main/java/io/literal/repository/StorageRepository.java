package io.literal.repository;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

import org.json.JSONObject;

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import io.literal.factory.AWSMobileClientFactory;
import io.literal.factory.AmazonS3ClientFactory;
import io.literal.factory.AppSyncClientFactory;
import io.literal.lib.AWSConfigurationLib;
import io.literal.lib.Callback;
import io.literal.lib.Callback3;
import io.literal.model.User;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;

public class StorageRepository {

    private static String bucketName;
    private static String bucketRegion;
    private static String storageHost;

    public static AmazonS3URI getPrivateUri(Context context, User user, String path) {
        return new AmazonS3URI("s3://" + getBucketName(context) + "/private/" + user.getIdentityId() + "/" + path);
    }

    public static String getBucketName(Context context) {
        if (bucketName == null) {
            JSONObject s3TransferUtilityJson = AWSConfigurationLib
                    .getConfiguration(context)
                    .optJsonObject("S3TransferUtility");
            bucketName = s3TransferUtilityJson.optString("Bucket");
        }
        return bucketName;
    }

    public static String getBucketRegion(Context context) {
        if (bucketRegion == null) {
            JSONObject s3TransferUtilityJson = AWSConfigurationLib
                    .getConfiguration(context)
                    .optJsonObject("S3TransferUtility");
            bucketRegion = s3TransferUtilityJson.optString("Region");
        }
        return bucketRegion;
    }

    public static boolean isStorageUrl(Context context, URL url) {
        if (storageHost == null) {
            storageHost = getBucketName(context) + ".s3." + getBucketRegion(context) + ".amazonaws.com";
        }

        return url.getHost().equals(storageHost);
    }

    public static ObjectMetadata getObject(Context context, AmazonS3URI uri, File destinationFile) {
        return AmazonS3ClientFactory.getInstance(context).getObject(new GetObjectRequest(uri.getBucket(), uri.getKey()), destinationFile);
    }

    public static TransferObserver upload(
            Context context,
            String key,
            File inputFile,
            ObjectMetadata metadata,
            Function2<Exception, AmazonS3URI, Void> onUploadComplete,
            Function3<Integer, Long, Long, Void> onUploadProgress
    ) {
        TransferUtility transferUtility = AWSMobileClientFactory.getTransferUtility(context);

        TransferObserver transferObserver = transferUtility.upload(key, inputFile, metadata);
        transferObserver.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    AmazonS3URI s3URI = new AmazonS3URI("s3://" + transferObserver.getBucket() + "/" + transferObserver.getKey());
                    onUploadComplete.invoke(null, s3URI);
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                if (onUploadProgress != null) {
                    onUploadProgress.invoke(id, bytesCurrent, bytesTotal);
                }
            }

            @Override
            public void onError(int id, Exception e) {
                onUploadComplete.invoke(e, null);
            }
        });

        return transferObserver;
    }

    public static CompletableFuture<Void> download(Context context, String key, File outputFile) {
        TransferObserver transferObserver = AWSMobileClientFactory.getTransferUtility(context).download(key, outputFile);
        CompletableFuture<Void> future = new CompletableFuture<>();
        transferObserver.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED != state) {
                    return;
                }
                future.complete(null);
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                /** noop **/
            }

            @Override
            public void onError(int id, Exception ex) {
                future.completeExceptionally(ex);
            }
        });
        return future;
    }
}
