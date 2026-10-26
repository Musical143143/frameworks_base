/*
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.axdynamicbar.shared

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GenericShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.android.systemui.axdynamicbar.domain.LunarisBarConfig
import com.android.systemui.axdynamicbar.model.IslandEvent
import kotlin.math.cos
import kotlin.math.sin

// Helpers that turn LunarisBar options into shapes, colors and durations. Every helper returns
// today's value for the default option, so a default config changes nothing.

private val SurfaceLightColor = Color(0xFFF2F2F7)
private val SurfaceDarkColor = Color(0xFF1C1C1E)
private val BlackColor = Color(0xFF000000)
private val OnLightColor = Color(0xFF1B1B1B)
private val OnDarkColor = Color(0xFFFFFFFF)

/** Shape of an expanded card. Default (rounded, not custom) is the existing 28dp card corner. */
fun LunarisBarConfig.cardCornerShape(): Shape = when (cardShape) {
    0 -> RoundedCornerShape(percent = 50)
    2 -> RectangleShape
    else -> RoundedCornerShape(if (customCornerRadius) 16.dp else 28.dp)
}

/** Shape of the buttons inside expanded cards. Default is the existing pill. */
fun LunarisBarConfig.cardButtonCornerShape(): Shape = when (cardButtonShape) {
    1 -> RoundedCornerShape(if (customCornerRadius) 8.dp else 12.dp)
    2 -> CircleShape
    else -> RoundedCornerShape(percent = 50)
}

/** Shape of the expanded panel background. Default is the existing 28dp corner. */
fun LunarisBarConfig.expandedBackgroundCornerShape(): Shape = when (expandedBackgroundShape) {
    0 -> RoundedCornerShape(percent = 50)
    2 -> RectangleShape
    else -> RoundedCornerShape(if (customCornerRadius) 16.dp else 28.dp)
}

/** Shape of the album art in the music card. Default is the existing 24dp corner. */
fun LunarisBarConfig.albumArtCornerShape(): Shape = when (albumArtShape) {
    0 -> CircleShape
    2 -> RectangleShape
    else -> RoundedCornerShape(if (customCornerRadius) 16.dp else 24.dp)
}

/**
 * Index of the compact chip shape: 0 circle, 1 rounded square, 2 pill, 3 hexagon.
 * When the option is unset, the Dynamic Bar chip style decides (legacyCircle = style 1).
 */
fun LunarisBarConfig.resolvedCompactShape(legacyCircle: Boolean): Int =
    compactShape ?: if (legacyCircle) 0 else 2

/** Shape of the status bar chip for the resolved compact shape. Circle maps to a circle. */
fun LunarisBarConfig.compactChipShape(legacyCircle: Boolean): Shape =
    when (resolvedCompactShape(legacyCircle)) {
        0 -> CircleShape
        1 -> RoundedCornerShape(if (customCornerRadius) 4.dp else 8.dp)
        3 -> polygonShape(regularPolygon(6, -90.0))
        else -> RoundedCornerShape(percent = 50)
    }

/**
 * Shape of the play and pause button. Null means no background at all.
 * The index order matches lunaris_bar_playback_button_shape_entries.
 */
fun LunarisBarConfig.playbackButtonCornerShape(): Shape? = when (playbackButtonShape) {
    0 -> null
    4 -> RoundedCornerShape(16.dp)
    5 -> RectangleShape
    6 -> polygonShape(regularPolygon(6, -90.0))
    7 -> CutCornerShape(percent = 30)
    8 -> polygonShape(listOf(0.5f to 0f, 1f to 0.5f, 0.5f to 1f, 0f to 0.5f))
    9 -> polygonShape(listOf(0.5f to 0f, 1f to 1f, 0f to 1f))
    10 -> polygonShape(regularPolygon(3, -90.0))
    11 -> RoundedCornerShape(4.dp)
    12 -> polygonShape(
        listOf(
            0.5f to 0f,
            1f to 0.5f,
            0.75f to 0.5f,
            0.75f to 1f,
            0.25f to 1f,
            0.25f to 0.5f,
            0f to 0.5f,
        ),
    )
    3 -> polygonShape(starPoints(5, 0.5f, 0.22f))
    // 1 (System) and 2 (Circle)
    else -> CircleShape
}

/** Background of the status bar chip. accent is today's event color. */
fun LunarisBarConfig.compactChipBackground(accent: Color): Color {
    val base = when (compactPalette) {
        0 -> SurfaceLightColor
        1 -> SurfaceDarkColor
        2 -> BlackColor
        4 -> Color(compactBackground)
        else -> accent
    }
    return base.copy(alpha = base.alpha * compactOpacityPct.coerceIn(0, 100) / 100f)
}

/** Text and icon color on the status bar chip. defaultContent is today's contrast color. */
fun LunarisBarConfig.compactChipContent(defaultContent: Color): Color = when {
    compactPalette == 4 -> Color(compactForeground)
    theme == 1 -> OnLightColor
    theme == 2 -> OnDarkColor
    else -> defaultContent
}

/** Background of the expanded cards. defaultColor is the existing card background. */
fun LunarisBarConfig.expandedCardBackground(defaultColor: Color): Color {
    val base = when (expandedPalette) {
        1 -> BlackColor
        2 -> SurfaceLightColor
        3 -> SurfaceDarkColor
        4 -> Color(expandedBackground)
        else -> defaultColor
    }
    return base.copy(alpha = base.alpha * expandedOpacityPct.coerceIn(0, 100) / 100f)
}

/** Text color in the expanded cards. defaultContent is today's color. */
fun LunarisBarConfig.expandedCardContent(defaultContent: Color): Color = when {
    expandedPalette == 4 -> Color(expandedForeground)
    expandedPalette == 2 -> OnLightColor
    expandedPalette == 1 || expandedPalette == 3 -> OnDarkColor
    theme == 1 -> OnLightColor
    theme == 2 -> OnDarkColor
    else -> defaultContent
}

/** Background of the music card. defaultColor is the existing media card background. */
fun LunarisBarConfig.musicCardBackground(defaultColor: Color): Color = when (musicPalette) {
    1 -> SurfaceLightColor
    2 -> SurfaceDarkColor
    3 -> BlackColor
    4 -> Color(musicBackgroundColor)
    else -> defaultColor
}

/** Text color in the music card. defaultContent is today's color. */
fun LunarisBarConfig.musicCardContent(defaultContent: Color): Color = when (musicPalette) {
    1 -> OnLightColor
    2, 3 -> OnDarkColor
    4 -> Color(musicTextColor)
    else -> defaultContent
}

/** Outline color for chips and cards. */
fun LunarisBarConfig.outlineColor(): Color =
    if (outlineLightMode) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.4f)

/** Shadow color for chips and cards. */
fun LunarisBarConfig.shadowColor(): Color =
    if (shadowLightMode) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.6f)

/** Duration after the reduce-animations and speed options. Zero means no animation. */
fun LunarisBarConfig.scaledDurationMillis(millis: Int): Int {
    if (reduceAnimations) return 0
    return millis * 100 / animationSpeedPct.coerceIn(50, 200)
}

/** Spring damping ratio for the bounce option, or null to keep the existing motion scheme. */
fun LunarisBarConfig.bounceDampingRatio(): Float? = when (bounceIntensity) {
    0 -> 1f
    1 -> 0.9f
    3 -> 0.5f
    else -> null
}

/**
 * Whether an expanded card for this event is shown. The chip is never filtered here.
 * Mapping: shortcut = app switch and recent app; timer = timer; clock = alarm and stopwatch;
 * widget = sports and promoted ongoing; utility = torch, hotspot, VPN, Bluetooth, ringer, clipboard.
 */
fun LunarisBarConfig.allowsExpandedCard(event: IslandEvent): Boolean = when (event) {
    is IslandEvent.AppSwitch -> cardShortcut
    is IslandEvent.Timer -> cardTimer
    is IslandEvent.Alarm, is IslandEvent.Stopwatch -> cardClock
    is IslandEvent.Sports, is IslandEvent.PromotedOngoing -> cardWidget
    is IslandEvent.Torch, is IslandEvent.Hotspot, is IslandEvent.Vpn,
    is IslandEvent.Bluetooth, is IslandEvent.RingerMode, is IslandEvent.Clipboard -> cardUtility
    else -> true
}

private fun polygonShape(points: List<Pair<Float, Float>>): Shape = GenericShape { size, _ ->
    points.forEachIndexed { index, point ->
        val x = point.first * size.width
        val y = point.second * size.height
        if (index == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** Vertices of a regular polygon inside the unit square, first vertex at startDeg. */
private fun regularPolygon(sides: Int, startDeg: Double): List<Pair<Float, Float>> =
    (0 until sides).map { i ->
        val angle = Math.toRadians(startDeg + 360.0 * i / sides)
        (0.5f + 0.5f * cos(angle).toFloat()) to (0.5f + 0.5f * sin(angle).toFloat())
    }

/** Vertices of a star with the given number of points, alternating outer and inner radius. */
private fun starPoints(points: Int, outer: Float, inner: Float): List<Pair<Float, Float>> =
    (0 until points * 2).map { i ->
        val radius = if (i % 2 == 0) outer else inner
        val angle = Math.toRadians(-90.0 + 180.0 * i / points)
        (0.5f + radius * cos(angle).toFloat()) to (0.5f + radius * sin(angle).toFloat())
    }
