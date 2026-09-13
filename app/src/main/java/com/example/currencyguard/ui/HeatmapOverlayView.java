package com.example.currencyguard.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Custom View for rendering the Grad-CAM Attention Heatmap overlay
 * with 50% alpha transparency over the currency note image.
 */
public class HeatmapOverlayView extends View {

    private Bitmap heatmapBitmap;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Rect destRect = new Rect();

    public HeatmapOverlayView(Context context) {
        super(context);
        init();
    }

    public HeatmapOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HeatmapOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setAlpha(128); // 50% transparency as specified in Section 11
    }

    public void setHeatmapBitmap(Bitmap bitmap) {
        this.heatmapBitmap = bitmap;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (heatmapBitmap != null && !heatmapBitmap.isRecycled()) {
            destRect.set(0, 0, getWidth(), getHeight());
            canvas.drawBitmap(heatmapBitmap, null, destRect, paint);
        }
    }
}
