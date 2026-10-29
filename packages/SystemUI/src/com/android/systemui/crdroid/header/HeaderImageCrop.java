/*
 * Copyright (C) 2026 The LunarisAOSP Project
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

package com.android.systemui.crdroid.header;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.provider.Settings;
import android.widget.ImageView;

/**
 * Applies the user's header image zoom and focal point. The image is scaled to cover the header
 * (the same as centerCrop) and then zoomed; the focal point decides which part of the overflow
 * stays visible. Default values (zoom 100, focus 50/50) match the old centerCrop look.
 */
public final class HeaderImageCrop {

    public static final int ZOOM_MIN = 100;
    public static final int ZOOM_MAX = 300;
    public static final int POS_DEFAULT = 50;

    private HeaderImageCrop() {}

    /** Applies the saved crop to {@code view}. Safe to call from layout listeners. */
    public static void apply(ImageView view, Context context) {
        if (view == null || context == null) {
            return;
        }
        int zoom = clamp(readInt(context, Settings.System.STATUS_BAR_CUSTOM_HEADER_ZOOM, 100),
                ZOOM_MIN, ZOOM_MAX);
        int posX = clamp(readInt(context, Settings.System.STATUS_BAR_CUSTOM_HEADER_POS_X,
                POS_DEFAULT), 0, 100);
        int posY = clamp(readInt(context, Settings.System.STATUS_BAR_CUSTOM_HEADER_POS_Y,
                POS_DEFAULT), 0, 100);
        view.setScaleType(ImageView.ScaleType.MATRIX);
        view.setImageMatrix(matrixFor(view, view.getDrawable(), zoom, posX, posY));
    }

    static Matrix matrixFor(ImageView view, Drawable drawable, int zoom, int posX, int posY) {
        Matrix m = new Matrix();
        float vw = view.getWidth();
        float vh = view.getHeight();
        if (drawable == null || vw <= 0 || vh <= 0) {
            return m;
        }
        float dw = drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : vw;
        float dh = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : vh;
        float cover = Math.max(vw / dw, vh / dh);
        float scale = cover * zoom / 100f;
        float overflowX = dw * scale - vw;
        float overflowY = dh * scale - vh;
        m.setScale(scale, scale);
        m.postTranslate(-overflowX * posX / 100f, -overflowY * posY / 100f);
        return m;
    }

    private static int readInt(Context context, String key, int def) {
        return Settings.System.getIntForUser(context.getContentResolver(), key, def,
                UserHandle.USER_CURRENT);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
