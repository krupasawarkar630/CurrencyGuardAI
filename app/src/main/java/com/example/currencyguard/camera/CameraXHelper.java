package com.example.currencyguard.camera;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CameraX lifecycle helper for Preview and high-resolution ImageCapture.
 */
public class CameraXHelper {

    private static final String TAG = "CameraXHelper";
    private ImageCapture imageCapture;
    private Camera camera;
    private boolean isTorchOn = false;
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();

    public interface CaptureCallback {
        void onImageCaptured(File photoFile);
        void onError(String error);
    }

    public void startCamera(Context context, LifecycleOwner lifecycleOwner, PreviewView previewView) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                );

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(context));
    }

    public void takePhoto(Context context, File outputFile, CaptureCallback callback) {
        if (imageCapture == null) {
            callback.onError("Camera is not ready yet.");
            return;
        }

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(outputFile).build();

        imageCapture.takePicture(
                outputOptions,
                cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        callback.onImageCaptured(outputFile);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        callback.onError(exception.getMessage());
                    }
                }
        );
    }

    public void toggleFlash() {
        if (camera != null) {
            CameraControl control = camera.getCameraControl();
            isTorchOn = !isTorchOn;
            control.enableTorch(isTorchOn);
        }
    }

    public void shutdown() {
        cameraExecutor.shutdown();
    }
}
