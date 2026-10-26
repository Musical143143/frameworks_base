/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.axdynamicbar.domain

import android.content.ContentResolver
import android.os.UserHandle
import android.provider.Settings
import androidx.compose.runtime.compositionLocalOf

/**
 * Options written by the Settings "LunarisBar" page into System settings.
 *
 * Every default reproduces the look and behaviour the Dynamic Bar already had, so nothing
 * changes for a user until one of these options is set. Event visibility is not part of this
 * class: it lives in the existing Secure key `ax_dynamic_bar_events` (see AxDynamicBarSettings).
 */
data class LunarisBarConfig(
    // Layout. Ignored (defaults used) unless separateLayout is true.
    val separateLayout: Boolean = true,
    val chipScalePct: Int = 100,
    val chipWidthPct: Int = 100,
    val chipHeightPct: Int = 100,
    val horizontalPositionDp: Int = 0,
    val verticalPositionDp: Int = 0,
    val cameraGapDp: Int = 0,
    val restingHeightDp: Int = 24,
    // Appearance. compactShape is null when unset, which means "follow the Dynamic Bar chip
    // style": 1 selects the circle style, anything else the pill.
    val compactShape: Int? = null,
    val theme: Int = 0,
    val blurChip: Boolean = false,
    val showPeekChip: Boolean = true,
    val keepCapsuleVisible: Boolean = false,
    val cardShape: Int = 1,
    val cardButtonShape: Int = 0,
    val expansionPlacement: Int = 0,
    val expandedWidth: Int = 0,
    val expandedBackgroundShape: Int = 1,
    val blurExpanded: Boolean = false,
    val customCornerRadius: Boolean = false,
    val outlineWidthDp: Int = 0,
    val shadowElevationDp: Int = 0,
    // Colors. Palette 0 (expanded and music) and 3 (compact) keep today's colors.
    val compactPalette: Int = 3,
    val compactOpacityPct: Int = 100,
    val compactBackground: Int = ARGB_BLACK,
    val compactForeground: Int = ARGB_WHITE,
    val expandedPalette: Int = 0,
    val expandedOpacityPct: Int = 100,
    val expandedBackground: Int = ARGB_BLACK,
    val expandedForeground: Int = ARGB_WHITE,
    val outlineLightMode: Boolean = false,
    val shadowLightMode: Boolean = true,
    // Motion
    val reduceAnimations: Boolean = false,
    val hapticFeedback: Boolean = false,
    val animationSpeedPct: Int = 100,
    val expansionStyle: Int = 1,
    val bounceIntensity: Int = 2,
    val fadeChipAppear: Boolean = true,
    val originalPopupAnimation: Boolean = true,
    val originalMusicPopup: Boolean = true,
    val originalFlashlightPopup: Boolean = true,
    // Music
    val rotateAlbumArt: Boolean = false,
    val thinAlbumBorder: Boolean = false,
    val albumArtShape: Int = 1,
    val musicBackground: Int = 1,
    val useAlbumColors: Boolean = true,
    val showMusicChip: Boolean = true,
    val customizeMusicChip: Boolean = false,
    val trailingContent: Int = 2,
    val hidePaused: Boolean = false,
    val showSeekBar: Boolean = true,
    val animateEqualizer: Boolean = false,
    val musicHaptics: Boolean = false,
    val playbackButtonShape: Int = 2,
    val playerIdentification: Int = 0,
    val musicPalette: Int = 0,
    val musicBackgroundColor: Int = ARGB_BLACK,
    val musicTextColor: Int = ARGB_WHITE,
    // Gestures on the status bar chip: 0 none, 1 expand, 2 open app, 3 play or pause, 4 next
    val gestureTap: Int = 1,
    val gestureDoubleTap: Int = 0,
    val gestureLongPress: Int = 0,
    // Expanded card types
    val cardShortcut: Boolean = true,
    val cardWidget: Boolean = true,
    val cardClock: Boolean = true,
    val cardTimer: Boolean = true,
    val cardUtility: Boolean = true,
) {
    companion object {
        const val ARGB_BLACK: Int = -16777216
        const val ARGB_WHITE: Int = -1
    }
}

/** Provided at the root of the bar and the expanded overlay. Falls back to today's defaults. */
val LocalLunarisBarConfig = compositionLocalOf { LunarisBarConfig() }

/** System setting keys. Each one is an int (colors are ARGB ints). */
internal object LunarisBarKeys {
    const val SEPARATE_LAYOUT = "lunaris_bar_separate_layout"
    const val CHIP_SCALE = "lunaris_bar_chip_scale"
    const val CHIP_WIDTH = "lunaris_bar_chip_width"
    const val CHIP_HEIGHT = "lunaris_bar_chip_height"
    const val HORIZONTAL_POSITION = "lunaris_bar_horizontal_position"
    const val VERTICAL_POSITION = "lunaris_bar_vertical_position"
    const val CAMERA_GAP_WIDTH = "lunaris_bar_camera_gap_width"
    const val RESTING_HEIGHT = "lunaris_bar_resting_height"
    const val COMPACT_SHAPE = "lunaris_bar_compact_shape"
    const val THEME = "lunaris_bar_theme"
    const val BLUR_CHIP = "lunaris_bar_blur_chip"
    const val SHOW_PEEK_CHIP = "lunaris_bar_show_peek_chip"
    const val KEEP_CAPSULE_VISIBLE = "lunaris_bar_keep_capsule_visible"
    const val CARD_SHAPE = "lunaris_bar_card_shape"
    const val CARD_BUTTON_SHAPE = "lunaris_bar_card_button_shape"
    const val EXPANSION_PLACEMENT = "lunaris_bar_expansion_placement"
    const val EXPANDED_WIDTH = "lunaris_bar_expanded_width"
    const val EXPANDED_BACKGROUND_SHAPE = "lunaris_bar_expanded_background_shape"
    const val BLUR_EXPANDED = "lunaris_bar_blur_expanded"
    const val CUSTOM_CORNER_RADIUS = "lunaris_bar_custom_corner_radius"
    const val OUTLINE_WIDTH = "lunaris_bar_outline_width"
    const val SHADOW_ELEVATION = "lunaris_bar_shadow_elevation"
    const val COMPACT_PALETTE = "lunaris_bar_compact_palette"
    const val COMPACT_OPACITY = "lunaris_bar_compact_opacity"
    const val COMPACT_BACKGROUND = "lunaris_bar_compact_background"
    const val COMPACT_FOREGROUND = "lunaris_bar_compact_foreground"
    const val EXPANDED_PALETTE = "lunaris_bar_expanded_palette"
    const val EXPANDED_OPACITY = "lunaris_bar_expanded_opacity"
    const val EXPANDED_BACKGROUND = "lunaris_bar_expanded_background"
    const val EXPANDED_FOREGROUND = "lunaris_bar_expanded_foreground"
    const val OUTLINE_LIGHT_MODE = "lunaris_bar_outline_light_mode"
    const val SHADOW_LIGHT_MODE = "lunaris_bar_shadow_light_mode"
    const val REDUCE_ANIMATIONS = "lunaris_bar_reduce_animations"
    const val HAPTIC_FEEDBACK = "lunaris_bar_haptic_feedback"
    const val ANIMATION_SPEED = "lunaris_bar_animation_speed"
    const val EXPANSION_STYLE = "lunaris_bar_expansion_style"
    const val BOUNCE_INTENSITY = "lunaris_bar_bounce_intensity"
    const val FADE_CHIP_APPEAR = "lunaris_bar_fade_chip_appear"
    const val ORIGINAL_POPUP_ANIMATION = "lunaris_bar_original_popup_animation"
    const val ORIGINAL_MUSIC_POPUP = "lunaris_bar_original_music_popup"
    const val ORIGINAL_FLASHLIGHT_POPUP = "lunaris_bar_original_flashlight_popup"
    const val ROTATE_ALBUM_ART = "lunaris_bar_rotate_album_art"
    const val THIN_ALBUM_BORDER = "lunaris_bar_thin_album_border"
    const val ALBUM_ART_SHAPE = "lunaris_bar_album_art_shape"
    const val MUSIC_BACKGROUND = "lunaris_bar_music_background"
    const val USE_ALBUM_COLORS = "lunaris_bar_use_album_colors"
    const val SHOW_MUSIC_CHIP = "lunaris_bar_show_music_chip"
    const val CUSTOMIZE_MUSIC_CHIP = "lunaris_bar_customize_music_chip"
    const val TRAILING_CONTENT = "lunaris_bar_trailing_content"
    const val HIDE_PAUSED = "lunaris_bar_hide_paused"
    const val SHOW_SEEK_BAR = "lunaris_bar_show_seek_bar"
    const val ANIMATE_EQUALIZER = "lunaris_bar_animate_equalizer"
    const val MUSIC_HAPTICS = "lunaris_bar_music_haptics"
    const val PLAYBACK_BUTTON_SHAPE = "lunaris_bar_playback_button_shape"
    const val PLAYER_IDENTIFICATION = "lunaris_bar_player_identification"
    const val MUSIC_PALETTE = "lunaris_bar_music_palette"
    const val MUSIC_BACKGROUND_COLOR = "lunaris_bar_music_background_color"
    const val MUSIC_TEXT_COLOR = "lunaris_bar_music_text_color"
    const val GESTURE_TAP = "lunaris_bar_gesture_tap"
    const val GESTURE_DOUBLE_TAP = "lunaris_bar_gesture_double_tap"
    const val GESTURE_LONG_PRESS = "lunaris_bar_gesture_long_press"
    const val CARD_SHORTCUT = "lunaris_bar_card_shortcut"
    const val CARD_WIDGET = "lunaris_bar_card_widget"
    const val CARD_CLOCK = "lunaris_bar_card_clock"
    const val CARD_TIMER = "lunaris_bar_card_timer"
    const val CARD_UTILITY = "lunaris_bar_card_utility"

    /** Every key above. AxDynamicBarSettings observes all of them. */
    val ALL: List<String> = listOf(
        SEPARATE_LAYOUT, CHIP_SCALE, CHIP_WIDTH, CHIP_HEIGHT, HORIZONTAL_POSITION,
        VERTICAL_POSITION, CAMERA_GAP_WIDTH, RESTING_HEIGHT, COMPACT_SHAPE, THEME,
        BLUR_CHIP, SHOW_PEEK_CHIP, KEEP_CAPSULE_VISIBLE, CARD_SHAPE, CARD_BUTTON_SHAPE,
        EXPANSION_PLACEMENT, EXPANDED_WIDTH, EXPANDED_BACKGROUND_SHAPE, BLUR_EXPANDED,
        CUSTOM_CORNER_RADIUS, OUTLINE_WIDTH, SHADOW_ELEVATION, COMPACT_PALETTE,
        COMPACT_OPACITY, COMPACT_BACKGROUND, COMPACT_FOREGROUND, EXPANDED_PALETTE,
        EXPANDED_OPACITY, EXPANDED_BACKGROUND, EXPANDED_FOREGROUND, OUTLINE_LIGHT_MODE,
        SHADOW_LIGHT_MODE, REDUCE_ANIMATIONS, HAPTIC_FEEDBACK, ANIMATION_SPEED,
        EXPANSION_STYLE, BOUNCE_INTENSITY, FADE_CHIP_APPEAR, ORIGINAL_POPUP_ANIMATION,
        ORIGINAL_MUSIC_POPUP, ORIGINAL_FLASHLIGHT_POPUP, ROTATE_ALBUM_ART, THIN_ALBUM_BORDER,
        ALBUM_ART_SHAPE, MUSIC_BACKGROUND, USE_ALBUM_COLORS, SHOW_MUSIC_CHIP,
        CUSTOMIZE_MUSIC_CHIP, TRAILING_CONTENT, HIDE_PAUSED, SHOW_SEEK_BAR, ANIMATE_EQUALIZER,
        MUSIC_HAPTICS, PLAYBACK_BUTTON_SHAPE, PLAYER_IDENTIFICATION, MUSIC_PALETTE,
        MUSIC_BACKGROUND_COLOR, MUSIC_TEXT_COLOR, GESTURE_TAP, GESTURE_DOUBLE_TAP,
        GESTURE_LONG_PRESS, CARD_SHORTCUT, CARD_WIDGET, CARD_CLOCK, CARD_TIMER, CARD_UTILITY,
    )
}

/** Reads every LunarisBar option for the current user. Missing values fall back to defaults. */
internal fun readLunarisBarConfig(resolver: ContentResolver): LunarisBarConfig {
    val d = LunarisBarConfig()

    fun int(key: String, fallback: Int): Int =
        Settings.System.getIntForUser(resolver, key, fallback, UserHandle.USER_CURRENT)

    fun flag(key: String, fallback: Boolean): Boolean =
        int(key, if (fallback) 1 else 0) != 0

    val separate = flag(LunarisBarKeys.SEPARATE_LAYOUT, d.separateLayout)
    // Placement and size options only apply while "separate layout" is on.
    fun layoutInt(key: String, fallback: Int): Int = if (separate) int(key, fallback) else fallback

    // -1 is the "unset" sentinel: the Dynamic Bar chip style decides in that case.
    val compactShapeRaw = int(LunarisBarKeys.COMPACT_SHAPE, -1)

    return LunarisBarConfig(
        separateLayout = separate,
        chipScalePct = layoutInt(LunarisBarKeys.CHIP_SCALE, d.chipScalePct),
        chipWidthPct = layoutInt(LunarisBarKeys.CHIP_WIDTH, d.chipWidthPct),
        chipHeightPct = layoutInt(LunarisBarKeys.CHIP_HEIGHT, d.chipHeightPct),
        horizontalPositionDp = layoutInt(LunarisBarKeys.HORIZONTAL_POSITION, d.horizontalPositionDp),
        verticalPositionDp = layoutInt(LunarisBarKeys.VERTICAL_POSITION, d.verticalPositionDp),
        cameraGapDp = layoutInt(LunarisBarKeys.CAMERA_GAP_WIDTH, d.cameraGapDp),
        restingHeightDp = layoutInt(LunarisBarKeys.RESTING_HEIGHT, d.restingHeightDp),
        compactShape = if (compactShapeRaw < 0) null else compactShapeRaw,
        theme = int(LunarisBarKeys.THEME, d.theme),
        blurChip = flag(LunarisBarKeys.BLUR_CHIP, d.blurChip),
        showPeekChip = flag(LunarisBarKeys.SHOW_PEEK_CHIP, d.showPeekChip),
        keepCapsuleVisible = flag(LunarisBarKeys.KEEP_CAPSULE_VISIBLE, d.keepCapsuleVisible),
        cardShape = int(LunarisBarKeys.CARD_SHAPE, d.cardShape),
        cardButtonShape = int(LunarisBarKeys.CARD_BUTTON_SHAPE, d.cardButtonShape),
        expansionPlacement = int(LunarisBarKeys.EXPANSION_PLACEMENT, d.expansionPlacement),
        expandedWidth = int(LunarisBarKeys.EXPANDED_WIDTH, d.expandedWidth),
        expandedBackgroundShape =
            int(LunarisBarKeys.EXPANDED_BACKGROUND_SHAPE, d.expandedBackgroundShape),
        blurExpanded = flag(LunarisBarKeys.BLUR_EXPANDED, d.blurExpanded),
        customCornerRadius = flag(LunarisBarKeys.CUSTOM_CORNER_RADIUS, d.customCornerRadius),
        outlineWidthDp = int(LunarisBarKeys.OUTLINE_WIDTH, d.outlineWidthDp),
        shadowElevationDp = int(LunarisBarKeys.SHADOW_ELEVATION, d.shadowElevationDp),
        compactPalette = int(LunarisBarKeys.COMPACT_PALETTE, d.compactPalette),
        compactOpacityPct = int(LunarisBarKeys.COMPACT_OPACITY, d.compactOpacityPct),
        compactBackground = int(LunarisBarKeys.COMPACT_BACKGROUND, d.compactBackground),
        compactForeground = int(LunarisBarKeys.COMPACT_FOREGROUND, d.compactForeground),
        expandedPalette = int(LunarisBarKeys.EXPANDED_PALETTE, d.expandedPalette),
        expandedOpacityPct = int(LunarisBarKeys.EXPANDED_OPACITY, d.expandedOpacityPct),
        expandedBackground = int(LunarisBarKeys.EXPANDED_BACKGROUND, d.expandedBackground),
        expandedForeground = int(LunarisBarKeys.EXPANDED_FOREGROUND, d.expandedForeground),
        outlineLightMode = flag(LunarisBarKeys.OUTLINE_LIGHT_MODE, d.outlineLightMode),
        shadowLightMode = flag(LunarisBarKeys.SHADOW_LIGHT_MODE, d.shadowLightMode),
        reduceAnimations = flag(LunarisBarKeys.REDUCE_ANIMATIONS, d.reduceAnimations),
        hapticFeedback = flag(LunarisBarKeys.HAPTIC_FEEDBACK, d.hapticFeedback),
        animationSpeedPct = int(LunarisBarKeys.ANIMATION_SPEED, d.animationSpeedPct),
        expansionStyle = int(LunarisBarKeys.EXPANSION_STYLE, d.expansionStyle),
        bounceIntensity = int(LunarisBarKeys.BOUNCE_INTENSITY, d.bounceIntensity),
        fadeChipAppear = flag(LunarisBarKeys.FADE_CHIP_APPEAR, d.fadeChipAppear),
        originalPopupAnimation =
            flag(LunarisBarKeys.ORIGINAL_POPUP_ANIMATION, d.originalPopupAnimation),
        originalMusicPopup = flag(LunarisBarKeys.ORIGINAL_MUSIC_POPUP, d.originalMusicPopup),
        originalFlashlightPopup =
            flag(LunarisBarKeys.ORIGINAL_FLASHLIGHT_POPUP, d.originalFlashlightPopup),
        rotateAlbumArt = flag(LunarisBarKeys.ROTATE_ALBUM_ART, d.rotateAlbumArt),
        thinAlbumBorder = flag(LunarisBarKeys.THIN_ALBUM_BORDER, d.thinAlbumBorder),
        albumArtShape = int(LunarisBarKeys.ALBUM_ART_SHAPE, d.albumArtShape),
        musicBackground = int(LunarisBarKeys.MUSIC_BACKGROUND, d.musicBackground),
        useAlbumColors = flag(LunarisBarKeys.USE_ALBUM_COLORS, d.useAlbumColors),
        showMusicChip = flag(LunarisBarKeys.SHOW_MUSIC_CHIP, d.showMusicChip),
        customizeMusicChip = flag(LunarisBarKeys.CUSTOMIZE_MUSIC_CHIP, d.customizeMusicChip),
        trailingContent = int(LunarisBarKeys.TRAILING_CONTENT, d.trailingContent),
        hidePaused = flag(LunarisBarKeys.HIDE_PAUSED, d.hidePaused),
        showSeekBar = flag(LunarisBarKeys.SHOW_SEEK_BAR, d.showSeekBar),
        animateEqualizer = flag(LunarisBarKeys.ANIMATE_EQUALIZER, d.animateEqualizer),
        musicHaptics = flag(LunarisBarKeys.MUSIC_HAPTICS, d.musicHaptics),
        playbackButtonShape = int(LunarisBarKeys.PLAYBACK_BUTTON_SHAPE, d.playbackButtonShape),
        playerIdentification = int(LunarisBarKeys.PLAYER_IDENTIFICATION, d.playerIdentification),
        musicPalette = int(LunarisBarKeys.MUSIC_PALETTE, d.musicPalette),
        musicBackgroundColor = int(LunarisBarKeys.MUSIC_BACKGROUND_COLOR, d.musicBackgroundColor),
        musicTextColor = int(LunarisBarKeys.MUSIC_TEXT_COLOR, d.musicTextColor),
        gestureTap = int(LunarisBarKeys.GESTURE_TAP, d.gestureTap),
        gestureDoubleTap = int(LunarisBarKeys.GESTURE_DOUBLE_TAP, d.gestureDoubleTap),
        gestureLongPress = int(LunarisBarKeys.GESTURE_LONG_PRESS, d.gestureLongPress),
        cardShortcut = flag(LunarisBarKeys.CARD_SHORTCUT, d.cardShortcut),
        cardWidget = flag(LunarisBarKeys.CARD_WIDGET, d.cardWidget),
        cardClock = flag(LunarisBarKeys.CARD_CLOCK, d.cardClock),
        cardTimer = flag(LunarisBarKeys.CARD_TIMER, d.cardTimer),
        cardUtility = flag(LunarisBarKeys.CARD_UTILITY, d.cardUtility),
    )
}
