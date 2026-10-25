/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.systemui.media.controls.ui;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import java.util.Locale;

/**
 * Reads LunarisBar music preferences from Settings.System.
 *
 * This helper does not create a status-bar chip. The future LunarisBar
 * controller can use these values when that component is implemented.
 */
public final class LunarisMusicSettings {
    private static final String PREFIX = "lunaris_bar_";

    private LunarisMusicSettings() {}

    public static boolean getBoolean(Context context, String key, boolean defaultValue) {
        int fallback = defaultValue ? 1 : 0;
        return Settings.System.getIntForUser(
                context.getContentResolver(),
                PREFIX + key,
                fallback,
                android.os.UserHandle.USER_CURRENT) != 0;
    }

    public static int getInt(Context context, String key, int defaultValue) {
        return Settings.System.getIntForUser(
                context.getContentResolver(),
                PREFIX + key,
                defaultValue,
                android.os.UserHandle.USER_CURRENT);
    }

    public static String getString(Context context, String key, String defaultValue) {
        String value = Settings.System.getStringForUser(
                context.getContentResolver(),
                PREFIX + key,
                android.os.UserHandle.USER_CURRENT);
        return value == null ? defaultValue : value;
    }

    public static boolean shouldShowMusicChip(Context context) {
        return getBoolean(context, "show_music_chip", true);
    }

    public static boolean shouldHidePausedPlayer(Context context) {
        return getBoolean(context, "hide_paused", false);
    }

    public static boolean shouldShowSeekBar(Context context) {
        return getBoolean(context, "show_seek_bar", true);
    }

    public static boolean shouldUseAlbumColors(Context context) {
        return getBoolean(context, "use_album_colors", false);
    }

    public static boolean shouldShowLyrics(Context context) {
        return getBoolean(context, "show_lyrics", false);
    }

    public static boolean shouldAnimateEqualizer(Context context) {
        return getBoolean(context, "animate_equalizer", true);
    }

    public static boolean shouldUseMusicHaptics(Context context) {
        return getBoolean(context, "music_haptics", true);
    }

    public static int getAlbumArtShape(Context context) {
        return getInt(context, "album_art_shape", 1);
    }

    public static boolean shouldUseCustomMusicColors(Context context) {
        return getBoolean(context, "customize_music_chip", false);
    }

    public static int getMusicBackgroundColor(Context context) {
        String value = getString(context, "music_background_color", "0xFF000000");
        try {
            return (int) Long.decode(value).longValue();
        } catch (NumberFormatException ignored) {
            try {
                return (int) Long.parseLong(
                        value.replaceFirst("^#", ""), 16);
            } catch (NumberFormatException ignoredAgain) {
                return 0xFF000000;
            }
        }
    }

    public static int getMusicTextColor(Context context) {
        String value = getString(context, "music_text_color", "0xFFFFFFFF");
        try {
            return (int) Long.decode(value).longValue();
        } catch (NumberFormatException ignored) {
            try {
                return (int) Long.parseLong(
                        value.replaceFirst("^#", ""), 16);
            } catch (NumberFormatException ignoredAgain) {
                return 0xFFFFFFFF;
            }
        }
    }
}
