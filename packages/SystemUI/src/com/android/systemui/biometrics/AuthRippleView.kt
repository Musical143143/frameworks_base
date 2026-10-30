/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.systemui.biometrics

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.os.UserHandle
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.view.animation.PathInterpolator
import com.android.internal.graphics.ColorUtils
import com.android.app.animation.Interpolators
import com.android.settingslib.Utils
import com.android.systemui.surfaceeffects.ripple.RippleShader

private const val RIPPLE_SPARKLE_STRENGTH: Float = 0.7f

/**
 * Handles two ripple effects: dwell ripple and unlocked ripple
 * Dwell Ripple:
 *     - startDwellRipple: dwell ripple expands outwards around the biometric area
 *     - retractDwellRipple: retracts the dwell ripple to radius 0 to signal a failure
 *     - fadeDwellRipple: fades the dwell ripple away to alpha 0
 * Unlocked ripple:
 *     - startUnlockedRipple: ripple expands from biometric auth location to the edges of the screen
 */
class AuthRippleView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private val retractInterpolator = PathInterpolator(.05f, .93f, .1f, 1f)

    private val dwellPulseDuration = 100L
    private val dwellExpandDuration = 2000L - dwellPulseDuration

    private var drawDwell: Boolean = false
    private var drawRipple: Boolean = false
    private var drawWater: Boolean = false
    private var pitRadiusPx: Float = 0f
    private var drawStyle: Boolean = false

    private var lockScreenColorVal = Color.WHITE
    private val fadeDuration = 83L
    private val retractDuration = 400L
    private val dwellShader = DwellRippleShader()
    private val dwellPaint = Paint()
    private val rippleShader = RippleShader()
    private val ripplePaint = Paint()
    private val waterShader = WaterRippleShader()
    private val waterPaint = Paint()
    private val styleShader = FodStyleRippleShader()
    private val stylePaint = Paint()
    private var unlockedRippleAnimator: Animator? = null
    private var fadeDwellAnimator: Animator? = null
    private var retractDwellAnimator: Animator? = null
    private var dwellPulseOutAnimator: Animator? = null
    private var dwellRadius: Float = 0f
        set(value) {
            dwellShader.maxRadius = value
            field = value
        }
    private var dwellOrigin: Point = Point()
        set(value) {
            dwellShader.origin = value
            field = value
        }
    private var radius: Float = 0f
        set(value) {
            field = value * .9f
            rippleShader.rippleSize.setMaxSize(field * 2.2f, field * 2.2f)
        }
    private var origin: Point = Point()
        set(value) {
            rippleShader.setCenter(value.x.toFloat(), value.y.toFloat())
            field = value
        }

    init {
        rippleShader.rawProgress = 0f
        rippleShader.pixelDensity = resources.displayMetrics.density
        rippleShader.sparkleStrength = RIPPLE_SPARKLE_STRENGTH
        updateRippleFadeParams()
        ripplePaint.shader = rippleShader
        waterPaint.shader = waterShader
        waterPaint.isAntiAlias = false
        stylePaint.shader = styleShader
        setLockScreenColor(Utils.getColorAttr(context,
                android.R.attr.colorAccent).defaultColor) // default color

        dwellShader.color = 0xffffffff.toInt() // default color
        dwellShader.progress = 0f
        dwellShader.distortionStrength = .8f
        dwellPaint.shader = dwellShader
        visibility = GONE
    }

    fun setSensorLocation(location: Point) {
        origin = location
        radius = maxOf(location.x, location.y, width - location.x, height - location.y).toFloat()
    }

    fun setFingerprintSensorLocation(location: Point, sensorRadius: Float) {
        origin = location
        radius = maxOf(location.x, location.y, width - location.x, height - location.y).toFloat()
        dwellOrigin = location
        dwellRadius = sensorRadius * 1.7f
    }

    /**
     * Animate dwell ripple inwards back to radius 0
     */
    fun retractDwellRipple() {
        if (retractDwellAnimator?.isRunning == true || fadeDwellAnimator?.isRunning == true) {
            return // let the animation finish
        }

        if (dwellPulseOutAnimator?.isRunning == true) {
            val retractDwellRippleAnimator = ValueAnimator.ofFloat(dwellShader.progress, 0f)
                    .apply {
                interpolator = retractInterpolator
                duration = retractDuration
                addUpdateListener { animator ->
                    val now = animator.currentPlayTime
                    dwellShader.progress = animator.animatedValue as Float
                    dwellShader.time = now.toFloat()

                    invalidate()
                }
            }

            val retractAlphaAnimator = ValueAnimator.ofInt(255, 0).apply {
                interpolator = Interpolators.LINEAR
                duration = retractDuration
                addUpdateListener { animator ->
                    dwellShader.color = ColorUtils.setAlphaComponent(
                            dwellShader.color,
                            animator.animatedValue as Int
                    )
                    invalidate()
                }
            }

            retractDwellAnimator = AnimatorSet().apply {
                playTogether(retractDwellRippleAnimator, retractAlphaAnimator)
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        dwellPulseOutAnimator?.cancel()
                        drawDwell = true
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        drawDwell = false
                        resetDwellAlpha()
                        invalidate()
                    }
                })
                start()
            }
        }
    }

    /**
     * Animate ripple fade to alpha=0
     */
    fun fadeDwellRipple() {
        if (fadeDwellAnimator?.isRunning == true) {
            return // let the animation finish
        }

        if (dwellPulseOutAnimator?.isRunning == true || retractDwellAnimator?.isRunning == true) {
            fadeDwellAnimator = ValueAnimator.ofInt(Color.alpha(dwellShader.color), 0).apply {
                interpolator = Interpolators.LINEAR
                duration = fadeDuration
                addUpdateListener { animator ->
                    dwellShader.color = ColorUtils.setAlphaComponent(
                            dwellShader.color,
                            animator.animatedValue as Int
                    )
                    invalidate()
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        retractDwellAnimator?.cancel()
                        dwellPulseOutAnimator?.cancel()
                        drawDwell = true
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        drawDwell = false
                        resetDwellAlpha()
                        invalidate()
                    }
                })
                start()
            }
        }
    }

    /**
     * Plays a ripple animation that grows to the dwellRadius with distortion.
     */
    fun startDwellRipple(isDozing: Boolean) {
        if (unlockedRippleAnimator?.isRunning == true || dwellPulseOutAnimator?.isRunning == true) {
            return
        }

        updateDwellRippleColor(isDozing)

        val dwellPulseOutRippleAnimator = ValueAnimator.ofFloat(0f, .8f).apply {
            interpolator = Interpolators.LINEAR
            duration = dwellPulseDuration
            addUpdateListener { animator ->
                val now = animator.currentPlayTime
                dwellShader.progress = animator.animatedValue as Float
                dwellShader.time = now.toFloat()

                invalidate()
            }
        }

        // slowly animate outwards until we receive a call to retractRipple or startUnlockedRipple
        val expandDwellRippleAnimator = ValueAnimator.ofFloat(.8f, 1f).apply {
            interpolator = Interpolators.LINEAR_OUT_SLOW_IN
            duration = dwellExpandDuration
            addUpdateListener { animator ->
                val now = animator.currentPlayTime
                dwellShader.progress = animator.animatedValue as Float
                dwellShader.time = now.toFloat()

                invalidate()
            }
        }

        dwellPulseOutAnimator = AnimatorSet().apply {
            playSequentially(
                    dwellPulseOutRippleAnimator,
                    expandDwellRippleAnimator
            )
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    retractDwellAnimator?.cancel()
                    fadeDwellAnimator?.cancel()
                    visibility = VISIBLE
                    drawDwell = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    drawDwell = false
                    invalidate()
                }
            })
            start()
        }
    }

    /**
     * Ripple that bursts outwards from the position of the sensor to the edges of the screen
     */
    fun startUnlockedRipple(onAnimationEnd: Runnable?) {
        unlockedRippleAnimator?.cancel()
        // Stop the dwell pulse so its orange circle does not stay on screen over the unlock effect.
        dwellPulseOutAnimator?.cancel()
        retractDwellAnimator?.cancel()
        fadeDwellAnimator?.cancel()
        drawDwell = false

        val fodStyle = getFodAnimStyle()
        if (fodStyle >= FOD_ANIM_FIRST_CUSTOM && fodStyle <= FOD_ANIM_LAST_CUSTOM) {
            startStyleRipple(fodStyle, onAnimationEnd)
            return
        }
        if (isWaterRippleEnabled()) {
            startWaterRipple(onAnimationEnd)
            return
        }

        val rippleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = AuthRippleController.RIPPLE_ANIMATION_DURATION
            addUpdateListener { animator ->
                val now = animator.currentPlayTime
                rippleShader.rawProgress = animator.animatedValue as Float
                rippleShader.time = now.toFloat()

                invalidate()
            }
        }

        unlockedRippleAnimator = rippleAnimator.apply {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    drawRipple = true
                    visibility = VISIBLE
                }

                override fun onAnimationEnd(animation: Animator) {
                    onAnimationEnd?.run()
                    drawRipple = false
                    visibility = GONE
                    unlockedRippleAnimator = null
                }
            })
        }
        unlockedRippleAnimator?.start()
    }

    private fun getFodAnimStyle(): Int =
        Settings.System.getIntForUser(
            context.contentResolver, SETTING_FOD_UNLOCK_ANIM, 0, UserHandle.USER_CURRENT
        )

    /**
     * Radius of the press area around the sensor. The effects stay inside it, as in the reference
     * video, instead of spreading over the whole screen.
     */
    private fun pressRadius(w: Float): Float =
        maxOf(w * FOD_PRESS_RADIUS_FRACTION, FOD_PRESS_RADIUS_MIN_DP * resources.displayMetrics.density)

    /** Lunaris extra styles (2..6), drawn by [FodStyleRippleShader]. */
    private fun startStyleRipple(style: Int, onAnimationEnd: Runnable?) {
        val highFrameRate = readBool(SETTING_FOD_WATER_HFR, true)
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)
        val pit = pressRadius(w)
        // drawPressArea() clips to this radius, so it must be set for style ripples too.
        pitRadiusPx = pit
        val c = lockScreenColorVal

        styleShader.setOrigin(origin.x.toFloat(), origin.y.toFloat())
        styleShader.setSize(w, h)
        styleShader.maxRadius = pit
        styleShader.density = resources.displayMetrics.density
        styleShader.style = style.toFloat()
        styleShader.setTint(Color.red(c) / 255f, Color.green(c) / 255f, Color.blue(c) / 255f)
        styleShader.reach = WATER_REACH_FRACTION
        styleShader.progress = 0f
        styleShader.fade = 0f
        styleShader.setBackdrop(captureBackdrop(w.toInt(), h.toInt()))

        // Same timing as the water effect: duration, fade-in and reach are shared constants.
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = WATER_DURATION_MS
            interpolator = Interpolators.LINEAR
            addUpdateListener { a ->
                styleShader.progress = a.animatedValue as Float
                styleShader.fade = smooth(a.currentPlayTime.toFloat() / WATER_FADE_IN_MS)
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    drawStyle = true
                    visibility = VISIBLE
                    if (highFrameRate) {
                        setRequestedFrameRate(REQUESTED_FRAME_RATE_CATEGORY_HIGH)
                    }
                }

                override fun onAnimationEnd(animation: Animator) {
                    drawStyle = false
                    setRequestedFrameRate(REQUESTED_FRAME_RATE_CATEGORY_DEFAULT)
                    visibility = GONE
                    unlockedRippleAnimator = null
                    onAnimationEnd?.run()
                }
            })
        }
        unlockedRippleAnimator = animator
        animator.start()
    }

    private fun isWaterRippleEnabled(): Boolean =
        Settings.System.getIntForUser(
            context.contentResolver, SETTING_FOD_UNLOCK_ANIM, 0, UserHandle.USER_CURRENT
        ) == FOD_ANIM_WATER

    private fun readBool(key: String, def: Boolean): Boolean =
        Settings.System.getIntForUser(
            context.contentResolver, key, if (def) 1 else 0, UserHandle.USER_CURRENT
        ) != 0

    /**
     * Real water waves travelling out from the sensor (see [WaterRippleShader]).
     *
     * Smoothness notes: time comes from the animator play time (not a frame counter), the wave
     * amplitude fades in over [WATER_FADE_IN_MS] and out over the last part of the animation so the
     * surface is exactly flat (fully transparent) when the view is hidden, and the whole view is
     * drawn as one rect, so there is no mask edge that could pop.
     */
    private fun startWaterRipple(onAnimationEnd: Runnable?) {
        val fullscreen = readBool(SETTING_FOD_WATER_FULLSCREEN, false)
        val highFrameRate = readBool(SETTING_FOD_WATER_HFR, true)
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)
        val density = resources.displayMetrics.density
        val durationMs = if (fullscreen) WATER_DURATION_FULLSCREEN_MS else WATER_DURATION_MS
        // Press area: the wave stops at this radius (or the far corner in fullscreen mode).
        val reachRadius = if (fullscreen) {
            maxOf(
                Math.hypot(origin.x.toDouble(), origin.y.toDouble()),
                Math.hypot((w - origin.x).toDouble(), origin.y.toDouble()),
                Math.hypot(origin.x.toDouble(), (h - origin.y).toDouble()),
                Math.hypot((w - origin.x).toDouble(), (h - origin.y).toDouble())
            ).toFloat()
        } else {
            pressRadius(w)
        }
        pitRadiusPx = reachRadius

        waterShader.setOrigin(origin.x.toFloat(), origin.y.toFloat())
        waterShader.setSize(w, h)
        waterShader.wavelength = WATER_WAVELENGTH_DP * density
        waterShader.fullscreen = fullscreen
        // Fullscreen waves travel much further, so they lose energy more slowly.
        waterShader.decay = if (fullscreen) WATER_DECAY_FULLSCREEN else WATER_DECAY
        waterShader.speed = if (fullscreen) {
            Math.hypot(w.toDouble(), h.toDouble()).toFloat() * WATER_FULLSCREEN_SPEED_DIAGS_PER_S
        } else {
            reachRadius / (durationMs / 1000f * WATER_REACH_FRACTION)
        }
        waterShader.time = 0f
        waterShader.fade = 0f
        waterShader.lensRadius = 0f
        waterShader.lensStrength = 0f
        waterShader.setBackdrop(captureBackdrop(w.toInt(), h.toInt()))
        val lensCap = if (fullscreen) w * WATER_LENS_MAX_FRACTION else reachRadius * FOD_LENS_OF_PRESS

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = Interpolators.LINEAR
            addUpdateListener { a ->
                val tMs = a.currentPlayTime.toFloat()
                val fadeIn = smooth(tMs / WATER_FADE_IN_MS)
                val fadeOut = 1f - smooth((tMs - durationMs * WATER_FADE_OUT_START) /
                        (durationMs * (1f - WATER_FADE_OUT_START)))
                waterShader.time = tMs / 1000f
                waterShader.fade = fadeIn * fadeOut
                // The lens follows the wave front, grows to a fixed size and then holds.
                waterShader.lensRadius = minOf(waterShader.speed * tMs / 1000f, lensCap)
                waterShader.lensStrength = fadeIn * fadeOut
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    drawWater = true
                    visibility = VISIBLE
                    if (highFrameRate) {
                        setRequestedFrameRate(REQUESTED_FRAME_RATE_CATEGORY_HIGH)
                    }
                }

                override fun onAnimationEnd(animation: Animator) {
                    drawWater = false
                    setRequestedFrameRate(REQUESTED_FRAME_RATE_CATEGORY_DEFAULT)
                    visibility = GONE
                    unlockedRippleAnimator = null
                    onAnimationEnd?.run()
                }
            })
        }
        unlockedRippleAnimator = animator
        animator.start()
    }

    /**
     * Copy of the wallpaper the lens bends. Built once per ripple. Returns null when no wallpaper
     * can be read, which turns the lens off and leaves the plain water ring.
     */
    private fun captureBackdrop(w: Int, h: Int): Bitmap? {
        if (w <= 0 || h <= 0) return null
        return try {
            val drawable = WallpaperManager.getInstance(context).drawable ?: return null
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            // Centre-crop the wallpaper to the screen, the same as the lock screen background.
            val dw = drawable.intrinsicWidth.coerceAtLeast(1).toFloat()
            val dh = drawable.intrinsicHeight.coerceAtLeast(1).toFloat()
            val scale = maxOf(w / dw, h / dh)
            val ox = (w - dw * scale) / 2f
            val oy = (h - dh * scale) / 2f
            canvas.translate(ox, oy)
            canvas.scale(scale, scale)
            drawable.setBounds(0, 0, dw.toInt(), dh.toInt())
            drawable.draw(canvas)
            bmp
        } catch (e: Exception) {
            null
        }
    }

    /** Paints only the square around the press area, clipped to the view. */
    private fun drawPressArea(canvas: Canvas, paint: Paint) {
        val r = pitRadiusPx + FOD_PRESS_MARGIN_PX
        val left = (origin.x - r).coerceAtLeast(0f)
        val top = (origin.y - r).coerceAtLeast(0f)
        val right = (origin.x + r).coerceAtMost(width.toFloat())
        val bottom = (origin.y + r).coerceAtMost(height.toFloat())
        canvas.drawRect(left, top, right, bottom, paint)
    }

    /** Smoothstep on a 0..1 clamped input. */
    private fun smooth(x: Float): Float {
        val t = x.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    fun setLockScreenColor(color: Int) {
        lockScreenColorVal = color
        rippleShader.color = ColorUtils.setAlphaComponent(
                lockScreenColorVal,
                62
        )
    }

    fun updateDwellRippleColor(isDozing: Boolean) {
        if (isDozing) {
            dwellShader.color = Color.WHITE
        } else {
            dwellShader.color = lockScreenColorVal
        }
        resetDwellAlpha()
    }

    fun resetDwellAlpha() {
        dwellShader.color = ColorUtils.setAlphaComponent(
                dwellShader.color,
                255
        )
    }

    private fun updateRippleFadeParams() {
        with(rippleShader) {
            baseRingFadeParams.fadeInStart = 0f
            baseRingFadeParams.fadeInEnd = .2f
            baseRingFadeParams.fadeOutStart = .2f
            baseRingFadeParams.fadeOutEnd = 1f

            centerFillFadeParams.fadeInStart = 0f
            centerFillFadeParams.fadeInEnd = .15f
            centerFillFadeParams.fadeOutStart = .15f
            centerFillFadeParams.fadeOutEnd = .56f
        }
    }

    override fun onDraw(canvas: Canvas) {
        // To reduce overdraw, we mask the effect to a circle whose radius is big enough to cover
        // the active effect area. Values here should be kept in sync with the
        // animation implementation in the ripple shader. (Twice bigger)
        if (drawDwell) {
            val maskRadius = (1 - (1 - dwellShader.progress) * (1 - dwellShader.progress) *
                    (1 - dwellShader.progress)) * dwellRadius * 2f
            canvas?.drawCircle(dwellOrigin.x.toFloat(), dwellOrigin.y.toFloat(),
                    maskRadius, dwellPaint)
        }

        if (drawWater) {
            drawPressArea(canvas, waterPaint)
        }

        if (drawStyle) {
            drawPressArea(canvas, stylePaint)
        }

        if (drawRipple) {
            canvas?.drawCircle(origin.x.toFloat(), origin.y.toFloat(),
                    rippleShader.rippleSize.currentWidth, ripplePaint)
        }
    }
}

private const val SETTING_FOD_UNLOCK_ANIM = "lunaris_fod_unlock_anim" // 0 = default, 1 = water
private const val SETTING_FOD_WATER_HFR = "lunaris_fod_water_hfr" // default on
private const val SETTING_FOD_WATER_FULLSCREEN = "lunaris_fod_water_fullscreen" // default off
private const val FOD_ANIM_WATER = 1

private const val WATER_DURATION_MS = 2200L
private const val WATER_DURATION_FULLSCREEN_MS = 3600L
private const val WATER_FADE_IN_MS = 140f
private const val WATER_FADE_OUT_START = 0.62f
private const val WATER_WAVELENGTH_DP = 38f
// Press area: radius of the effect around the sensor, as a share of the screen width.
// The reference video stays within about 30% of the width.
private const val FOD_PRESS_RADIUS_FRACTION = 0.30f
private const val FOD_PRESS_RADIUS_MIN_DP = 96f
// Extra margin around the press area so the soft edges are not clipped.
private const val FOD_PRESS_MARGIN_PX = 8f
// Lens size inside the press area, as a share of the press radius.
private const val FOD_LENS_OF_PRESS = 0.7f
// Lens size at its largest, as a share of the screen width (fullscreen mode only).
private const val WATER_LENS_MAX_FRACTION = 0.42f
private const val WATER_DECAY = 1.0f
private const val WATER_DECAY_FULLSCREEN = 0.6f
// Share of the animation the wave front needs to reach the press-area edge.
private const val WATER_REACH_FRACTION = 0.72f
// Fullscreen mode: front speed in screen diagonals per second.
private const val WATER_FULLSCREEN_SPEED_DIAGS_PER_S = 0.42f

private const val FOD_ANIM_FIRST_CUSTOM = 2 // Pulse, Neon ring, Aurora, Sparkle, Shockwave
private const val FOD_ANIM_LAST_CUSTOM = 31
