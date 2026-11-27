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
import android.view.animation.OvershootInterpolator;
import android.view.animation.LinearInterpolator;

/**
 * Glowing charging ring drawn around the battery percentage.
 *
 * <p>The ring scales in with a small overshoot, then a bright highlight arc travels once around
 * it while a soft outer glow pulses. The percentage text is expected to be laid out on top of
 * this view by the owner, so the ring only draws the decoration.
 */
final class ChargingRingView extends View {
    private static final long ENTRY_DURATION_MS = 600;
    private static final long SWEEP_DURATION_MS = 1600;
    private static final float RING_RADIUS_FRACTION = 0.19f;
    private static final float ARC_SPAN_DEGREES = 96f;
    private static final int RING_COLOR = 0xFFE8F1FF;
    private static final int GLOW_COLOR = 0xFF9CC8FF;
    private static final int ARC_COLOR = 0xFFFFFFFF;
    private static final int FILL_COLOR = 0x33000000;

    private final Paint mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mOval = new RectF();

    private final float mRingStroke;
    private final float mArcStroke;

    private float mEntryProgress;
    private float mSweepAngle;
    private ValueAnimator mEntryAnimator;
    private ValueAnimator mSweepAnimator;

    ChargingRingView(Context context) {
        super(context);
        mRingStroke = dp(context, 2f);
        mArcStroke = dp(context, 4f);

        mFillPaint.setStyle(Paint.Style.FILL);
        mFillPaint.setColor(FILL_COLOR);

        mRingPaint.setStyle(Paint.Style.STROKE);
        mRingPaint.setStrokeWidth(mRingStroke);
        mRingPaint.setColor(RING_COLOR);

        mGlowPaint.setStyle(Paint.Style.STROKE);
        mGlowPaint.setStrokeWidth(dp(context, 10f));
        mGlowPaint.setColor(GLOW_COLOR);
        mGlowPaint.setMaskFilter(new BlurMaskFilter(dp(context, 8f), BlurMaskFilter.Blur.NORMAL));

        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeCap(Paint.Cap.ROUND);
        mArcPaint.setStrokeWidth(mArcStroke);
        mArcPaint.setColor(ARC_COLOR);
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

        mOval.set(cx - radius, cy - radius, cx + radius, cy + radius);

        // Frosted interior so the wallpaper reads through the ring.
        canvas.drawCircle(cx, cy, radius, mFillPaint);

        // Outer glow, then crisp ring on top.
        mGlowPaint.setAlpha(clampAlpha(scale * 0.55f));
        canvas.drawOval(mOval, mGlowPaint);
        mRingPaint.setAlpha(clampAlpha(scale));
        canvas.drawOval(mOval, mRingPaint);

        // Highlight arc travelling once around the ring.
        mArcPaint.setAlpha(clampAlpha(scale));
        canvas.drawArc(mOval, mSweepAngle - 90f, ARC_SPAN_DEGREES, false, mArcPaint);
    }

    private static int clampAlpha(float fraction) {
        return Math.round(Math.max(0f, Math.min(1f, fraction)) * 255f);
    }

    private static float dp(Context context, float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics());
    }
}
