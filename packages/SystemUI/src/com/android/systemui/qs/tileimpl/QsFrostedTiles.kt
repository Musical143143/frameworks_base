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

package com.android.systemui.qs.tileimpl

import android.content.Context
import android.provider.Settings

/**
 * Frosted glass fill for Quick Settings tiles. The Settings app's Appearance and motion screen
 * writes [SETTING]; tiles read it when they apply their fill colour.
 */
object QsFrostedTiles {
    /** Secure setting key shared with the Settings app's glass toggle. */
    const val SETTING = "qs_frosted_tiles"

    /** Alpha for a frosted tile fill, matching the glass palette in the Settings app (0xB8). */
    const val FROSTED_ALPHA = 0xB8

    @JvmStatic
    fun isEnabled(context: Context): Boolean =
        Settings.Secure.getInt(context.contentResolver, SETTING, 0) != 0

    /** Returns [color] with frosted alpha applied when frosted tiles are on, else [color]. */
    @JvmStatic
    fun applyTo(context: Context, color: Int): Int =
        if (isEnabled(context)) {
            (color and 0x00FFFFFF) or (FROSTED_ALPHA shl 24)
        } else {
            color
        }
}
