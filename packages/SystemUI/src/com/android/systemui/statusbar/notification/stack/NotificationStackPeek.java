/*
 * Copyright (C) 2026 LunarisAOSP
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
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.View;

import com.android.systemui.res.R;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;

import java.util.List;

/**
 * "Stacked card" look for collapsed notification groups: up to two extra card layers peek out
 * from underneath the group summary, hinting that more notifications are hidden inside it.
 *
 * The peek layers are drawn by {@link ExpandableNotificationRow} into extra space that
 * {@link StackScrollAlgorithm} reserves below a collapsed group.
 *
 * Toggle: Settings.System "lunaris_notification_stack_peek" (0/1). The default comes from
 * R.bool.config_notificationStackPeekDefault.
 */
public final class NotificationStackPeek {

    public static final String SETTING_KEY = "lunaris_notification_stack_peek";

    private static final int MAX_LAYERS = 2;

    private static volatile boolean sEnabled = true;
    private static boolean sObserving;

    private NotificationStackPeek() {}

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
        final boolean def = context.getResources()
                .getBoolean(R.bool.config_notificationStackPeekDefault);
        sEnabled = Settings.System.getIntForUser(context.getContentResolver(), SETTING_KEY,
                def ? 1 : 0, UserHandle.USER_CURRENT) != 0;
    }

    /** Number of peek layers (0..2) to show below this view. */
    public static int getLayerCount(View view) {
        if (!sEnabled || !(view instanceof ExpandableNotificationRow)) {
            return 0;
        }
        final ExpandableNotificationRow row = (ExpandableNotificationRow) view;
        if (!row.isSummaryWithChildren() || row.isChildInGroup() || row.isBundle()
                || row.isGroupExpanded() || row.isRemoved()) {
            return 0;
        }
        final List<ExpandableNotificationRow> children = row.getAttachedChildren();
        if (children == null || children.isEmpty()) {
            return 0;
        }
        return Math.min(MAX_LAYERS, children.size());
    }

    /** Extra vertical space (px) that must be reserved below this view. */
    public static int getExtraHeight(View view) {
        final int layers = getLayerCount(view);
        if (layers == 0) {
            return 0;
        }
        return layers * view.getResources()
                .getDimensionPixelSize(R.dimen.notification_stack_peek_step);
    }

    /**
     * Draws the peek layers below {@code row}. Must be called before the row's own content is
     * drawn so the row covers the top of the layers.
     */
    public static void draw(ExpandableNotificationRow row, Canvas canvas, Paint paint,
            RectF tmp) {
        final int layers = getLayerCount(row);
        if (layers == 0 || row.getWidth() == 0) {
            return;
        }
        final int step = row.getResources().getDimensionPixelSize(
                R.dimen.notification_stack_peek_step);
        final int inset = row.getResources().getDimensionPixelSize(
                R.dimen.notification_stack_peek_inset);
        final float radius = row.getResources().getDimension(
                R.dimen.notification_corner_radius);
        final int baseColor = row.getBackgroundColorWithoutTint();
        final int bottom = row.getActualHeight() - row.getClipBottomAmount();

        for (int i = layers; i >= 1; i--) {
            // Further layers are slightly darker and more transparent.
            final float fade = 1f - 0.25f * i;
            final int a = Math.round(android.graphics.Color.alpha(baseColor) * fade);
            paint.setColor((baseColor & 0x00FFFFFF) | (a << 24));
            final float left = inset * i;
            final float right = row.getWidth() - inset * i;
            // Start well above the bottom edge so the rounded top is hidden behind the row.
            tmp.set(left, bottom - radius, right, bottom + step * i);
            canvas.drawRoundRect(tmp, radius, radius, paint);
        }
    }
}
