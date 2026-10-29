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
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;

/**
 * Gate for the Oxygen-style per-app notification stacks (Settings &gt; Notifications &gt;
 * Oxygen-style stacks). Off by default, so the current shade is unchanged until it is turned on.
 * The card layout that this gate controls is not drawn yet.
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
}
