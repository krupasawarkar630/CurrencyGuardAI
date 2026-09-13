package com.example.currencyguard.ml;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.example.currencyguard.model.AppConfig;

/**
 * Image preprocessing utilities for neural network inputs and CV pipelines.
 */
public class ImagePreprocessor {

    public static Bitmap prepareForModel(Bitmap source) {
        if (source == null) return null;
        return Bitmap.createScaledBitmap(source, AppConfig.MODEL_INPUT_SIZE, AppConfig.MODEL_INPUT_SIZE, true);
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        if (source == null || angle == 0) return source;
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static Bitmap cropToCenter(Bitmap source, float widthFraction, float heightFraction) {
        if (source == null) return null;
        int targetW = (int) (source.getWidth() * widthFraction);
        int targetH = (int) (source.getHeight() * heightFraction);
        int startX = Math.max(0, (source.getWidth() - targetW) / 2);
        int startY = Math.max(0, (source.getHeight() - targetH) / 2);
        return Bitmap.createBitmap(source, startX, startY, targetW, targetH);
    }
}
