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

package com.android.systemui.biometrics

import android.graphics.RuntimeShader

/**
 * Clear-water ripple for the fingerprint unlock animation.
 *
 * This is an analytic wave simulation evaluated per pixel, not a pre-rendered or tinted ring:
 *  - A circular wave packet travels outwards from the sensor at [speed] px/s. Its height field is
 *    `A * gauss(r - c*t) * cos(k*(r - c*t)) / sqrt(1 + r/r0)` (2D spreading loss).
 *  - A trailing capillary packet (shorter waves, slower front) follows the main ring, as real
 *    water does after a drop.
 *  - Amplitude decays over time ([decay]: energy loss in the water), and the ring is slightly uneven in
 *    angle, because a finger press is never a perfect circle.
 *  - A brief bright splash at the touch point (impact bloom) fades out in about half a second.
 *  - The surface normal is the analytic gradient of the height field. The pixel is shaded from
 *    that normal: specular glint, Fresnel sky reflection on steep slopes, and signed
 *    light/dark transport. Where the surface is flat the output is exactly transparent, so the
 *    water is clear and there is no haze or visible edge.
 *  - With [fullscreen] the screen edges act as walls. Reflections use the method of images
 *    (mirrored sources, the 8 neighbours, each bounce keeping 82% of the amplitude).
 *
 * Time is passed in seconds from the animator's play time, so the speed does not depend on the
 * frame rate.
 */
class WaterRippleShader internal constructor() : RuntimeShader(SHADER) {

    companion object {
        private const val SHADER =
            """
            uniform float2 in_origin;
            uniform float2 in_size;
            uniform float in_time;
            uniform float in_speed;
            uniform float in_wavelength;
            uniform float in_fade;
            uniform float in_fullscreen;
            uniform float in_decay;
            uniform float3 in_tint;

            const float TAU = 6.2831853;
            const float BOUNCE_KEEP = 0.82;
            const float CAPILLARY_RATIO = 0.42;  // trailing packet wavelength vs main
            const float CAPILLARY_SPEED = 0.55;  // trailing packet front vs main
            const float CAPILLARY_AMP = 0.35;
            const float SPLASH_DECAY = 11.0;     // 1/s
            const float SPLASH_GAIN = 0.55;

            float2 imageSource(int kx, int ky) {
                float x = in_origin.x;
                float y = in_origin.y;
                if (kx < 0) { x = -in_origin.x; }
                if (kx > 0) { x = 2.0 * in_size.x - in_origin.x; }
                if (ky < 0) { y = -in_origin.y; }
                if (ky > 0) { y = 2.0 * in_size.y - in_origin.y; }
                return float2(x, y);
            }

            // Height gradient of one travelling packet at offset v (r = |v|).
            float2 packetGrad(float2 v, float r, float lambda, float front, float amp) {
                float k = TAU / lambda;
                float w = lambda * 1.7;
                float d = r - front;
                if (abs(d) > 4.5 * w) { return float2(0.0); }
                float env = exp(-(d * d) / (2.0 * w * w));
                float spread = 1.0 / sqrt(1.0 + r / (lambda * 2.0));
                float ph = k * d;
                float dh = amp * env * spread * (-d / (w * w) * cos(ph) - k * sin(ph));
                return dh * (v / r);
            }

            half4 main(float2 p) {
                float lambda = in_wavelength;
                float t = in_time;
                float decay = exp(-in_decay * t);
                float2 grad = float2(0.0);

                for (int ix = -1; ix <= 1; ix++) {
                    for (int iy = -1; iy <= 1; iy++) {
                        bool isDirect = (ix == 0 && iy == 0);
                        if (isDirect || in_fullscreen > 0.5) {
                            float2 v = p - imageSource(ix, iy);
                            float r = length(v) + 0.001;
                            float keep = 1.0;
                            if (ix != 0) { keep *= BOUNCE_KEEP; }
                            if (iy != 0) { keep *= BOUNCE_KEEP; }
                            float ang = atan(v.y, v.x);
                            float uneven = 1.0 + 0.10 * sin(3.0 * ang + 1.3)
                                               + 0.06 * sin(5.0 * ang + 0.4);
                            float amp = keep * uneven * decay;
                            grad += packetGrad(v, r, lambda, in_speed * t, amp);
                            grad += packetGrad(v, r,
                                lambda * CAPILLARY_RATIO,
                                in_speed * CAPILLARY_SPEED * t,
                                amp * CAPILLARY_AMP);
                        }
                    }
                }

                // Height amplitude is ~7% of a wavelength, faded in/out by in_fade so the water
                // calms down smoothly instead of switching off.
                float2 g = grad * (0.07 * lambda) * in_fade;

                float3 n = normalize(float3(-g.x, -g.y, 1.0));
                float3 L = normalize(float3(-0.45, -0.55, 0.70));
                float3 V = float3(0.0, 0.0, 1.0);
                float3 H = normalize(L + V);

                // Specular glint, minus what a flat surface would give, so flat water is clear.
                float specFlat = pow(H.z, 40.0);
                float spec = max(pow(max(dot(n, H), 0.0), 40.0) - specFlat, 0.0);

                // Sky reflection grows on steep slopes (Fresnel-like).
                float fres = pow(1.0 - n.z, 2.0);

                // Signed light transport: slopes facing the light brighten, others darken.
                float diff = dot(n, L) / L.z - 1.0;
                float bright = clamp(diff * 0.55, 0.0, 1.0) * 0.30 + spec * 0.45 + fres * 0.20;
                float dark = clamp(-diff * 0.55, 0.0, 1.0) * 0.24;

                // Impact splash at the touch point. Fades in over 20 ms so it never pops.
                float2 q = p - in_origin;
                float sigma = lambda * 0.2;
                float splash = exp(-dot(q, q) / (2.0 * sigma * sigma))
                             * exp(-SPLASH_DECAY * t)
                             * smoothstep(0.0, 0.02, t);
                bright += splash * SPLASH_GAIN;

                bright = clamp(bright, 0.0, 1.0);
                float alpha = bright + dark * (1.0 - bright);
                return half4(half3(in_tint * bright), half(alpha));
            }
            """
    }

    /** Ripple centre, in px. */
    fun setOrigin(x: Float, y: Float) {
        setFloatUniform("in_origin", x, y)
    }

    /** Size of the surface the waves live on (the screen), in px. */
    fun setSize(width: Float, height: Float) {
        setFloatUniform("in_size", width, height)
    }

    /** Seconds since the touch. */
    var time: Float = 0f
        set(value) {
            field = value
            setFloatUniform("in_time", value)
        }

    /** Wave-front speed in px/s. */
    var speed: Float = 0f
        set(value) {
            field = value
            setFloatUniform("in_speed", value)
        }

    /** Distance between crests in px. */
    var wavelength: Float = 100f
        set(value) {
            field = value
            setFloatUniform("in_wavelength", value)
        }

    /** 0..1 global wave amplitude envelope. */
    var fade: Float = 0f
        set(value) {
            field = value
            setFloatUniform("in_fade", value)
        }

    /** Whether the screen edges reflect the waves. */
    var fullscreen: Boolean = false
        set(value) {
            field = value
            setFloatUniform("in_fullscreen", if (value) 1f else 0f)
        }

    /** Amplitude loss rate over time, 1/s. Lower keeps the waves alive longer. */
    var decay: Float = 1.0f
        set(value) {
            field = value
            setFloatUniform("in_decay", value)
        }

    /** Colour of the highlights (linear-ish sRGB, 0..1). */
    fun setTint(r: Float, g: Float, b: Float) {
        setFloatUniform("in_tint", r, g, b)
    }

    init {
        setOrigin(0f, 0f)
        setSize(1f, 1f)
        time = 0f
        speed = 0f
        wavelength = 100f
        fade = 0f
        fullscreen = false
        decay = 1.0f
        setTint(0.93f, 0.97f, 1.0f)
    }
}
