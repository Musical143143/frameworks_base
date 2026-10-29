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

package com.android.systemui.qs;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;

/**
 * Gate for the One UI-style Quick Settings panel (Settings &gt; Display &gt; One UI-style Quick
 * Settings). Off by default, so the current Quick Settings panel is unchanged until it is turned on.
 * The panel and its split-shade layout are not built yet.
 */
public final class QsOneUiStyle {

    public static final String SETTING_KEY = "lunaris_oneui_qs";

    private static volatile boolean sEnabled;
    private static boolean sObserving;

    private QsOneUiStyle() {}

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

    /** Whether the One UI-style Quick Settings is turned on. False until the user enables it. */
    public static boolean isEnabled() {
        return sEnabled;
    }
}
