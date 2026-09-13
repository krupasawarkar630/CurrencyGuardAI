package com.example.currencyguard.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Bitmap and file utility methods for image storage and orientation adjustment.
 */
public class ImageUtils {

    public static File createTempImageFile(Context context) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(context.getFilesDir(), "scans");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    public static Bitmap loadAndCorrectOrientation(String path) {
        if (path == null) return null;
        try {
            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, boundsOptions);

            int sampleSize = 1;
            int maxDim = Math.max(boundsOptions.outWidth, boundsOptions.outHeight);
            int targetDim = 1920;
            while (maxDim / sampleSize > targetDim) {
                sampleSize *= 2;
            }

            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = Math.max(1, sampleSize);
            decodeOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;

            Bitmap bitmap = BitmapFactory.decodeFile(path, decodeOptions);
            if (bitmap == null) return null;

            try {
                ExifInterface exif = new ExifInterface(path);
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                int rotation = 0;
                if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotation = 90;
                else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotation = 180;
                else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotation = 270;

                if (rotation != 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotation);
                    Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    if (rotated != bitmap && !bitmap.isRecycled()) {
                        bitmap.recycle();
                    }
                    return rotated;
                }
            } catch (Throwable ignored) {}

            return bitmap;
        } catch (Throwable t) {
            return null;
        }
    }

    public static Bitmap loadBitmapFromUri(Context context, Uri uri) {
        if (context == null || uri == null) return null;
        try {
            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                if (is != null) {
                    BitmapFactory.decodeStream(is, null, boundsOptions);
                }
            }

            int sampleSize = 1;
            int maxDim = Math.max(boundsOptions.outWidth, boundsOptions.outHeight);
            int targetDim = 1920;
            while (maxDim / sampleSize > targetDim) {
                sampleSize *= 2;
            }

            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = Math.max(1, sampleSize);
            decodeOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;

            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(is, null, decodeOptions);
            }
        } catch (Throwable e) {
            return null;
        }
    }

    public static String saveBitmap(Context context, Bitmap bitmap, String prefix) {
        try {
            File dir = new File(context.getFilesDir(), "scans");
            if (!dir.exists()) dir.mkdirs();

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File file = new File(dir, prefix + "_" + timeStamp + ".jpg");

            try (FileOutputStream fos = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            }
            return file.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }
}
