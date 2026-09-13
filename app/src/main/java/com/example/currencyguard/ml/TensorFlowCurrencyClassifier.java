package com.example.currencyguard.ml;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.currencyguard.model.AppConfig;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Production TensorFlow Lite Classifier.
 * Runs on-device inference using the TFLite native interpreter.
 * Gracefully falls back to DemoCurrencyClassifier if the .tflite model file
 * is not placed in app/src/main/assets/.
 */
public class TensorFlowCurrencyClassifier implements CurrencyClassifier {

    private static final String TAG = "TFLiteClassifier";
    private Interpreter interpreter;
    private final List<String> labels = new ArrayList<>();
    private final DemoCurrencyClassifier fallbackDemoClassifier;
    private boolean isModelLoaded = false;

    public TensorFlowCurrencyClassifier(Context context) {
        this.fallbackDemoClassifier = new DemoCurrencyClassifier();
        try {
            loadLabels(context);
            ByteBuffer modelBuffer = loadModelFile(context, AppConfig.MODEL_ASSET_PATH);
            if (modelBuffer != null) {
                Interpreter.Options options = new Interpreter.Options();
                options.setNumThreads(4);
                this.interpreter = new Interpreter(modelBuffer, options);
                this.isModelLoaded = true;
                Log.i(TAG, "TensorFlow Lite model loaded successfully.");
            }
        } catch (Exception e) {
            Log.w(TAG, "Custom .tflite model not found or failed to initialize. Falling back to Demo mode: " + e.getMessage());
            this.isModelLoaded = false;
        }
    }

    private void loadLabels(Context context) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open(AppConfig.LABELS_ASSET_PATH)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    labels.add(line.trim());
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "Labels file not found, using defaults.");
            labels.clear();
            labels.add("Likely Genuine");
            labels.add("Suspicious");
            labels.add("Likely Fake");
            labels.add("Unable to Verify");
        }
    }

    private ByteBuffer loadModelFile(Context context, String modelPath) {
        try {
            AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public ClassificationResult classify(Bitmap bitmap) {
        if (!isModelLoaded || interpreter == null) {
            // Graceful fallback to Demo classifier
            return fallbackDemoClassifier.classify(bitmap);
        }

        try {
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, AppConfig.MODEL_INPUT_SIZE, AppConfig.MODEL_INPUT_SIZE, true);
            ByteBuffer inputBuffer = ByteBuffer.allocateDirect(4 * AppConfig.MODEL_INPUT_SIZE * AppConfig.MODEL_INPUT_SIZE * 3);
            inputBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[AppConfig.MODEL_INPUT_SIZE * AppConfig.MODEL_INPUT_SIZE];
            scaledBitmap.getPixels(intValues, 0, scaledBitmap.getWidth(), 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight());

            int pixel = 0;
            for (int i = 0; i < AppConfig.MODEL_INPUT_SIZE; ++i) {
                for (int j = 0; j < AppConfig.MODEL_INPUT_SIZE; ++j) {
                    final int val = intValues[pixel++];
                    // Normalize [0, 255] to [0.0, 1.0]
                    inputBuffer.putFloat(((val >> 16) & 0xFF) / 255.0f);
                    inputBuffer.putFloat(((val >> 8) & 0xFF) / 255.0f);
                    inputBuffer.putFloat((val & 0xFF) / 255.0f);
                }
            }

            int numClasses = labels.isEmpty() ? 4 : labels.size();
            float[][] output = new float[1][numClasses];
            interpreter.run(inputBuffer, output);

            int maxIndex = 0;
            float maxProb = -1.0f;
            for (int i = 0; i < numClasses; i++) {
                if (output[0][i] > maxProb) {
                    maxProb = output[0][i];
                    maxIndex = i;
                }
            }

            String predictedLabel = (maxIndex < labels.size()) ? labels.get(maxIndex) : "Likely Genuine";
            return new ClassificationResult(predictedLabel, maxProb * 100.0, output[0], false);

        } catch (Exception e) {
            Log.e(TAG, "TFLite inference failed, reverting to demo classifier: " + e.getMessage());
            return fallbackDemoClassifier.classify(bitmap);
        }
    }

    @Override
    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }

    public boolean isNativeModelLoaded() {
        return isModelLoaded;
    }
}
