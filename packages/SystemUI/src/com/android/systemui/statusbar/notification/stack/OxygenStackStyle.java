/*
 * Copyright (C) 2026 Lunaris AOSP
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

package com.android.systemui.statusbar.notification.stack;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;

/**
 * Gate for the Oxygen-style per-app notification stacks (Settings &gt; Notifications &gt;
 * Oxygen-style stacks). Off by default, so the current shade is unchanged until it is turned on.
 */
public final class OxygenStackStyle {

    public static final String SETTING_KEY = "lunaris_oxygen_stacks";

    private static volatile boolean sEnabled;
    private static boolean sObserving;

    private OxygenStackStyle() {}

    /** Reads the setting and keeps it cached; safe to call more than once. */
    public static synchronized void init(Context context) {
        final Context appContext = context.getApplicationContext() != null
                ? context.getApplicationContext() : context;
        refresh(appContext);
        if (sObserving) {
            return;
        }
        sObserving = true;
        appContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(SETTING_KEY), false,
                new ContentObserver(new Handler(Looper.getMainLooper())) {
                    @Override
                    public void onChange(boolean selfChange) {
                        refresh(appContext);
                    }
                }, UserHandle.USER_ALL);
    }

    private static void refresh(Context context) {
        sEnabled = Settings.System.getIntForUser(context.getContentResolver(), SETTING_KEY, 0,
                UserHandle.USER_CURRENT) == 1;
    }

    /** Whether the Oxygen-style stacks are turned on. Always false until the user enables it. */
    public static boolean isEnabled() {
        return sEnabled;
    }

    /**
     * Draws the stacked card layers under a collapsed app group: rounded glass cards with a
     * translucent gradient fill (lighter at the top) and a thin light border, each one a little
     * narrower and lower than the one above. Drawn only when {@link #isEnabled()} is true.
     */
    public static void drawLayers(Canvas canvas, Paint paint, RectF rect, float radius,
            float density, int baseColor, int layers, float inset, float step, float width,
            float bottom) {
        final int baseAlpha = Color.alpha(baseColor);
        final int r = Color.red(baseColor);
        final int g = Color.green(baseColor);
        final int b = Color.blue(baseColor);
        for (int i = layers; i >= 1; i--) {
            final float left = inset * i;
            final float right = width - inset * i;
            rect.set(left, bottom - radius, right, bottom + step * i);

            final float fade = 1f - 0.18f * i;
            final int top = Color.argb(Math.round(baseAlpha * fade), r, g, b);
            final int lower = Color.argb(Math.round(baseAlpha * fade * 0.7f), r, g, b);
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new LinearGradient(0, rect.top, 0, rect.bottom,
                    top, lower, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(rect, radius, radius, paint);
            paint.setShader(null);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1f, density));
            paint.setColor(Color.argb(Math.round(baseAlpha * fade * 0.35f), 255, 255, 255));
            canvas.drawRoundRect(rect, radius, radius, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }
}
