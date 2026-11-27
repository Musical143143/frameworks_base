/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.charging;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Charging decoration drawn around the battery percentage.
 *
 * <p>Supports the Origin, Energy, Feathers and Lightcloud styles. The owner lays out the
 * percentage text on top of this view.
 */
final class ChargingRingView extends View {
    static final int STYLE_ORIGIN = 0;
    static final int STYLE_ENERGY = 2;
    static final int STYLE_FEATHERS = 3;
    static final int STYLE_LIGHTCLOUD = 4;

    static final float RING_RADIUS_FRACTION = 0.19f;

    private static final long ENTRY_DURATION_MS = 600;
    private static final long SWEEP_DURATION_MS = 1600;
    private static final float ARC_SPAN_DEGREES = 96f;
    private static final int FILL_COLOR = 0x33000000;

    private static final int ORIGIN_RING_COLOR = 0xFFE8F1FF;
    private static final int ORIGIN_GLOW_COLOR = 0xFF9CC8FF;
    private static final int ORIGIN_ARC_COLOR = 0xFFFFFFFF;
    private static final int ENERGY_RING_COLOR = 0xFFFFB35A;
    private static final int ENERGY_GLOW_COLOR = 0xFFFF8A2B;
    private static final int FEATHERS_RING_COLOR = 0xFF8FC4FF;
    private static final int FEATHERS_GLOW_COLOR = 0xFF3D8BFF;
    private static final int LIGHTCLOUD_FILL_COLOR = 0xFFF2F5FA;
    private static final int LIGHTCLOUD_GLOW_COLOR = 0xFFFFFFFF;

    private final int mStyle;
    private final Paint mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mOval = new RectF();

    private float mEntryProgress;
    private float mSweepAngle;
    private ValueAnimator mEntryAnimator;
    private ValueAnimator mSweepAnimator;

    ChargingRingView(Context context, int style) {
        super(context);
        mStyle = style;

        mFillPaint.setStyle(Paint.Style.FILL);
        mFillPaint.setColor(FILL_COLOR);

        mRingPaint.setStyle(Paint.Style.STROKE);
        mRingPaint.setStrokeWidth(dp(context, 2f));

        mGlowPaint.setStyle(Paint.Style.STROKE);
        mGlowPaint.setStrokeWidth(dp(context, 10f));
        mGlowPaint.setMaskFilter(new BlurMaskFilter(dp(context, 8f), BlurMaskFilter.Blur.NORMAL));

        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeCap(Paint.Cap.ROUND);
        mArcPaint.setStrokeWidth(dp(context, 4f));
        mArcPaint.setColor(ORIGIN_ARC_COLOR);
        mArcPaint.setMaskFilter(new BlurMaskFilter(dp(context, 3f), BlurMaskFilter.Blur.NORMAL));

        setWillNotDraw(false);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimations();
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelAnimations();
        super.onDetachedFromWindow();
    }

    private void startAnimations() {
        if (mEntryAnimator == null) {
            mEntryAnimator = ValueAnimator.ofFloat(0f, 1f);
            mEntryAnimator.setDuration(ENTRY_DURATION_MS);
            mEntryAnimator.setInterpolator(new OvershootInterpolator(0.9f));
            mEntryAnimator.addUpdateListener(a -> {
                mEntryProgress = (float) a.getAnimatedValue();
                invalidate();
            });
        }
        if (mSweepAnimator == null) {
            mSweepAnimator = ValueAnimator.ofFloat(0f, 360f);
            mSweepAnimator.setDuration(SWEEP_DURATION_MS);
            mSweepAnimator.setInterpolator(new LinearInterpolator());
            mSweepAnimator.addUpdateListener(a -> {
                mSweepAngle = (float) a.getAnimatedValue();
                invalidate();
            });
        }
        mEntryAnimator.start();
        mSweepAnimator.start();
    }

    private void cancelAnimations() {
        if (mEntryAnimator != null) {
            mEntryAnimator.cancel();
        }
        if (mSweepAnimator != null) {
            mSweepAnimator.cancel();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final float cx = getWidth() * 0.5f;
        final float cy = getHeight() * 0.5f;
        final float baseRadius = Math.min(getWidth(), getHeight()) * RING_RADIUS_FRACTION;
        final float scale = Math.max(0f, mEntryProgress);
        final float radius = baseRadius * scale;
        if (radius <= 0f) {
            return;
        }

        switch (mStyle) {
            case STYLE_ENERGY:
                drawEnergy(canvas, cx, cy, radius, scale);
                break;
            case STYLE_FEATHERS:
                drawFeathers(canvas, cx, cy, radius, scale);
                break;
            case STYLE_LIGHTCLOUD:
                drawLightcloud(canvas, cx, cy, radius, scale);
                break;
            case STYLE_ORIGIN:
            default:
                drawOrigin(canvas, cx, cy, radius, scale);
                break;
        }
    }

    private void drawOrigin(Canvas canvas, float cx, float cy, float radius, float scale) {
        mOval.set(cx - radius, cy - radius, cx + radius, cy + radius);
        canvas.drawCircle(cx, cy, radius, mFillPaint);

        setStroke(mGlowPaint, ORIGIN_GLOW_COLOR, scale * 0.55f);
        canvas.drawOval(mOval, mGlowPaint);
        setStroke(mRingPaint, ORIGIN_RING_COLOR, scale);
        canvas.drawOval(mOval, mRingPaint);

        mArcPaint.setAlpha(alpha(scale));
        canvas.drawArc(mOval, mSweepAngle - 90f, ARC_SPAN_DEGREES, false, mArcPaint);
    }

    private void drawEnergy(Canvas canvas, float cx, float cy, float radius, float scale) {
        mOval.set(cx - radius * 1.45f, cy - radius * 0.9f, cx + radius * 1.45f,
                cy + radius * 0.9f);
        canvas.drawOval(mOval, mFillPaint);

        setStroke(mGlowPaint, ENERGY_GLOW_COLOR, scale * 0.6f);
        canvas.drawOval(mOval, mGlowPaint);
        setStroke(mRingPaint, ENERGY_RING_COLOR, scale);
        canvas.drawOval(mOval, mRingPaint);
    }

    private void drawFeathers(Canvas canvas, float cx, float cy, float radius, float scale) {
        mOval.set(cx - radius, cy - radius, cx + radius, cy + radius);
        setStroke(mGlowPaint, FEATHERS_GLOW_COLOR, scale * 0.5f);
        canvas.drawOval(mOval, mGlowPaint);
        setStroke(mRingPaint, FEATHERS_RING_COLOR, scale);
        canvas.drawOval(mOval, mRingPaint);
    }

    private void drawLightcloud(Canvas canvas, float cx, float cy, float radius, float scale) {
        final float halfHeight = radius * 0.5f;
        mOval.set(cx - radius * 1.3f, cy - halfHeight, cx + radius * 1.3f, cy + halfHeight);

        mGlowPaint.setColor(LIGHTCLOUD_GLOW_COLOR);
        mGlowPaint.setAlpha(alpha(scale * 0.5f));
        canvas.drawRoundRect(mOval, halfHeight, halfHeight, mGlowPaint);

        mFillPaint.setColor(LIGHTCLOUD_FILL_COLOR);
        mFillPaint.setAlpha(alpha(scale));
        canvas.drawRoundRect(mOval, halfHeight, halfHeight, mFillPaint);
        mFillPaint.setColor(FILL_COLOR);
        mFillPaint.setAlpha(255);
    }

    private static void setStroke(Paint paint, int color, float fraction) {
        paint.setColor(color);
        paint.setAlpha(alpha(fraction));
    }

    private static int alpha(float fraction) {
        return Math.round(Math.max(0f, Math.min(1f, fraction)) * 255f);
    }

    private static float dp(Context context, float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics());
    }
}
