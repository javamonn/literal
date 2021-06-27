package io.literal.repository;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.UUID;

import io.literal.R;

public class BitmapRepository {

    public static Bitmap scaleAndAddBackground(Context context, Bitmap favicon) {
        if (favicon == null) {
            return null;
        }

        int faviconSize = context.getResources().getDimensionPixelSize(R.dimen.source_web_view_favicon_size);
        int padding = context.getResources().getDimensionPixelSize(R.dimen.source_web_view_favicon_padding);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(favicon, faviconSize, faviconSize, true);
        Bitmap outputBitmap = Bitmap.createBitmap(scaledBitmap.getWidth() + padding, scaledBitmap.getHeight() + padding, scaledBitmap.getConfig());
        Canvas canvas = new Canvas(outputBitmap);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(scaledBitmap, (float) padding / 2, (float) padding / 2, null);

        return outputBitmap;
    }

    public static Optional<File> toFile(Context context, Bitmap bitmap) {
        File outputDir = new File(context.getCacheDir(), "bitmaps");
        outputDir.mkdir();
        File outputFile = new File(outputDir, UUID.randomUUID().toString());
        try {
            OutputStream outputStream = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.WEBP, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            return Optional.of(outputFile);
        } catch (Exception e) {
            ErrorRepository.captureException(e);
            return Optional.empty();
        }
    }
}
