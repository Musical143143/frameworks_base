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

/** Extra fingerprint unlock styles: 2 Pulse, 3 Neon ring, 4 Aurora, 5 Sparkle, 6 Shockwave. */
class FodStyleRippleShader internal constructor() : RuntimeShader(SHADER) {

    companion object {
        private const val SHADER =
            """
            uniform float2 in_origin;
            uniform float2 in_size;
            uniform float in_progress;
            uniform float in_fade;
            uniform float in_style;
            uniform float in_maxr;
            uniform float in_density;
            uniform float in_reach;
            uniform float3 in_tint;
            // Lens: the wallpaper behind the sensor is bent inside the expanding front,
            // the same as the water effect.
            uniform shader in_bg;
            uniform float in_lens_on;

            const float TAU = 6.2831853;
            const float PI = 3.14159265;

            float hash11(float n) { return fract(sin(n * 91.3458) * 47453.5453); }

            float band(float d, float w) {
                float x = d / w;
                return exp(-x * x);
            }

            half4 pack(float3 c, float a) {
                float aa = clamp(a, 0.0, 1.0);
                return half4(half3(c * aa), half(aa));
            }

            half4 main(float2 p) {
                float dp = in_density;
                float2 v = p - in_origin;
                float r = length(v);
                float ang = atan(v.y, v.x);
                float t = in_progress;
                // Same scale as the water effect: the front reaches the far corner at in_reach,
                // and the style fades out over the last part of the animation.
                float tf = clamp(t / in_reach, 0.0, 1.0);
                float e = 1.0 - pow(1.0 - tf, 3.0);
                float front = e * in_maxr;
                float life = in_fade * (1.0 - smoothstep(0.62, 1.0, t));
                float3 col = in_tint;
                float a = 0.0;

                if (in_style < 2.5) {
                    // Pulse: three soft rings leaving one after another
                    for (int i = 0; i < 3; i++) {
                        float fi = float(i);
                        float ti = clamp(t * 1.35 - fi * 0.2, 0.0, 1.0);
                        float ri = (1.0 - pow(1.0 - ti, 2.5)) * in_maxr * 0.85;
                        a += band(r - ri, 16.0 * dp) * (1.0 - ti) * 0.55;
                    }
                    a += band(r, 30.0 * dp) * (1.0 - smoothstep(0.0, 0.35, t)) * 0.6;
                    col = mix(in_tint, float3(1.0), 0.35);
                } else if (in_style < 3.5) {
                    // Neon ring: hot white core, tinted glow, thin echo ring
                    float core = band(r - front, 2.5 * dp);
                    float glow = band(r - front, 20.0 * dp);
                    float echo = band(r - front * 0.82, 2.0 * dp);
                    a = core + glow * 0.45 + echo * 0.35;
                    col = mix(in_tint, float3(1.0), clamp(core, 0.0, 1.0));
                } else if (in_style < 4.5) {
                    // Aurora: wavy band with colours drifting around the ring
                    float wob = 7.0 * dp * sin(ang * 6.0 + t * 14.0)
                              + 4.0 * dp * sin(ang * 11.0 - t * 9.0);
                    a = band(r - (front + wob), 34.0 * dp) * 0.8;
                    col = 0.5 + 0.5 * cos(TAU * (ang / TAU + t * 1.2 + float3(0.0, 0.33, 0.67)));
                } else if (in_style < 5.5) {
                    // Sparkle: 48 twinkling particles flying outwards
                    float cells = 48.0;
                    float idx = floor((ang + PI) / TAU * cells);
                    float h1 = hash11(idx + 1.0);
                    float h2 = hash11(idx + 17.0);
                    float start = 40.0 * dp;
                    float pr = start + (in_maxr - start) * e * (0.35 + 0.65 * h1);
                    float ca = (idx + 0.5) / cells * TAU - PI;
                    float2 pos = in_origin + float2(cos(ca), sin(ca)) * pr;
                    float size = (2.0 + 3.5 * h2) * dp;
                    float tw = 0.6 + 0.4 * sin(t * 40.0 + h2 * TAU);
                    a = band(length(p - pos), size) * tw + band(r - front, 14.0 * dp) * 0.15;
                    col = mix(in_tint, float3(1.0), 0.6);
                } else if (in_style < 6.5) {
                    // Shockwave: sharp leading edge, fading trail, flash at the sensor
                    float d = r - front;
                    float lead = band(d, 4.0 * dp);
                    float trail = 0.0;
                    if (d < 0.0) {
                        trail = exp(d / (70.0 * dp)) * 0.35 * smoothstep(0.0, 60.0 * dp, r);
                    }
                    float flash = band(r, 45.0 * dp) * (1.0 - smoothstep(0.0, 0.25, t));
                    a = lead * 0.95 + trail + flash * 0.7;
                    col = mix(in_tint, float3(1.0), 0.5);
                } else if (in_style < 7.5) {
                    // Starburst: spinning rays plus a bright edge ring
                    float rays = pow(abs(cos(ang * 8.0 + t * 3.0)), 12.0);
                    a = rays * band(r - front * 0.8, 70.0 * dp) * 0.9
                      + band(r - front, 3.0 * dp) * 0.6;
                    col = mix(in_tint, float3(1.0), 0.5);
                } else if (in_style < 8.5) {
                    // Radar: rotating sweep with a fading trail inside the ring
                    float sweep = t * TAU * 2.0;
                    float diff = mod(sweep - ang, TAU);
                    float trail = exp(-diff * 2.5);
                    float inside = 1.0 - smoothstep(front - 6.0 * dp, front, r);
                    a = trail * inside * 0.75 + band(r - front, 3.0 * dp) * 0.8;
                    col = mix(in_tint, float3(0.6, 1.0, 0.7), 0.5);
                } else if (in_style < 9.5) {
                    // Bloom: petal-shaped ring with an inner petal ring
                    float pr = front * (0.75 + 0.25 * cos(ang * 6.0 + t * 4.0));
                    a = band(r - pr, 14.0 * dp) * 0.9 + band(r - pr * 0.6, 8.0 * dp) * 0.4;
                    col = mix(in_tint, float3(1.0, 0.6, 0.8), 0.4);
                } else if (in_style < 10.5) {
                    // Glitch: RGB-split ring with jittering scanline offsets
                    float row = floor(p.y / (6.0 * dp));
                    float off = (hash11(row + floor(t * 20.0)) - 0.5) * 30.0 * dp * (1.0 - t);
                    float split = 6.0 * dp;
                    float cr = band(length(v + float2(off + split, 0.0)) - front, 3.0 * dp);
                    float cg = band(length(v + float2(off, 0.0)) - front, 3.0 * dp);
                    float cb = band(length(v + float2(off - split, 0.0)) - front, 3.0 * dp);
                    a = max(max(cr, cg), cb);
                    col = clamp(float3(cr, cg, cb) / max(a, 0.001), 0.0, 1.0);
                } else if (in_style < 11.5) {
                    // Orbit: three dots circling the expanding ring
                    float orad = front * 0.85;
                    for (int i = 0; i < 3; i++) {
                        float ao = t * TAU * 2.0 + float(i) * TAU / 3.0;
                        float2 pos = in_origin + float2(cos(ao), sin(ao)) * orad;
                        a += band(length(p - pos), 9.0 * dp);
                    }
                    a += band(r - orad, 2.0 * dp) * 0.3;
                    col = mix(in_tint, float3(1.0), 0.6);
                } else if (in_style < 12.5) {
                    // Lightning: jagged bolts shooting outwards, thin ring at the edge
                    float n = 10.0;
                    float cell = floor((ang + PI) / TAU * n);
                    float ca = (cell + 0.5) / n * TAU - PI;
                    float jag = (hash11(cell + floor(r / (14.0 * dp)) + floor(t * 18.0)) - 0.5) * 0.06;
                    float da = ang - ca + jag;
                    float inside = 1.0 - smoothstep(front - 4.0 * dp, front, r);
                    a = band(r * da, 2.5 * dp) * inside * 0.9 + band(r - front, 3.0 * dp) * 0.5;
                    col = mix(in_tint, float3(0.8, 0.9, 1.0), 0.7);
                } else if (in_style < 13.5) {
                    // Heartbeat: two quick thumps
                    float t1 = clamp(t * 2.2, 0.0, 1.0);
                    float t2 = clamp(t * 2.2 - 0.45, 0.0, 1.0);
                    float r1 = (1.0 - pow(1.0 - t1, 2.5)) * in_maxr * 0.7;
                    float r2 = (1.0 - pow(1.0 - t2, 2.5)) * in_maxr * 0.7;
                    a = band(r - r1, 14.0 * dp) * (1.0 - t1) * 0.8
                      + band(r - r2, 14.0 * dp) * (1.0 - t2) * 0.8
                      + band(r, 30.0 * dp) * exp(-t * 6.0) * 0.7;
                    col = mix(in_tint, float3(1.0, 0.3, 0.4), 0.55);
                } else if (in_style < 14.5) {
                    // Rainbow: wide ring whose colour shifts with radius and time
                    a = band(r - front, 22.0 * dp) * 0.9;
                    col = 0.5 + 0.5 * cos(TAU * (r / in_maxr * 1.5 + t + float3(0.0, 0.33, 0.67)));
                } else if (in_style < 15.5) {
                    // Spiral: three arms spinning outwards
                    float phase = ang * 3.0 - r / (30.0 * dp) * 2.0 + t * 8.0;
                    float arms = pow(0.5 + 0.5 * cos(phase), 6.0);
                    float inside = 1.0 - smoothstep(front - 10.0 * dp, front, r);
                    a = arms * inside * 0.85 + band(r - front, 3.0 * dp) * 0.4;
                    col = mix(in_tint, float3(1.0), 0.4);
                } else if (in_style < 16.5) {
                    // Hexagon: expanding hexagonal outline with a faint inner hexagon
                    float seg = mod(ang + PI, TAU / 6.0) - PI / 6.0;
                    float hd = r * cos(seg);
                    a = band(hd - front * 0.9, 3.0 * dp) * 0.9
                      + band(hd - front * 0.9, 16.0 * dp) * 0.3
                      + band(hd - front * 0.55, 2.5 * dp) * 0.4;
                    col = mix(in_tint, float3(1.0), 0.5);
                } else if (in_style < 17.5) {
                    // Liquid Gold: molten ring with wobbly edge and moving shimmer
                    float wob = 5.0 * dp * sin(ang * 5.0 + t * 10.0) + 3.0 * dp * sin(ang * 9.0 - t * 7.0);
                    float shimmer = 0.75 + 0.25 * sin(ang * 12.0 + t * 18.0);
                    a = band(r - (front + wob), 18.0 * dp) * 0.85 * shimmer
                      + band(r - front * 0.7, 6.0 * dp) * 0.3
                      + band(r - front, 2.5 * dp) * 0.5;
                    col = mix(float3(1.0, 0.78, 0.35), float3(1.0, 0.95, 0.75), shimmer - 0.5);
                } else if (in_style < 18.5) {
                    // Prism: ring splits into R, G, B like light through glass
                    float spread = 14.0 * dp * (0.3 + t);
                    float cr = band(r - (front + spread), 5.0 * dp);
                    float cg = band(r - front, 5.0 * dp);
                    float cb = band(r - (front - spread), 5.0 * dp);
                    a = max(max(cr, cg), cb) * 0.9;
                    col = clamp(float3(cr, cg, cb) / max(max(max(cr, cg), cb), 0.001), 0.0, 1.0);
                } else if (in_style < 19.5) {
                    // Nebula: soft swirling cloud inside the ring
                    float swirl = sin(ang * 3.0 + r / (40.0 * dp) - t * 6.0)
                                + 0.6 * sin(ang * 5.0 - r / (25.0 * dp) + t * 4.0);
                    float cloud = clamp(0.5 + 0.35 * swirl, 0.0, 1.0);
                    float inside = 1.0 - smoothstep(front - 30.0 * dp, front, r);
                    a = cloud * inside * 0.7 + band(r - front, 4.0 * dp) * 0.5;
                    col = 0.5 + 0.5 * cos(TAU * (ang / TAU * 0.5 + r / in_maxr + t * 0.6 + float3(0.55, 0.75, 0.95)));
                } else if (in_style < 20.5) {
                    // Echo: five thin glassy rings leaving one after another
                    for (int i = 0; i < 5; i++) {
                        float fi = float(i);
                        float ti = clamp(t * 1.5 - fi * 0.12, 0.0, 1.0);
                        float ri = (1.0 - pow(1.0 - ti, 3.0)) * in_maxr;
                        a += band(r - ri, 2.5 * dp) * (1.0 - ti) * (1.0 - fi * 0.15);
                    }
                    a += band(r, 22.0 * dp) * exp(-t * 7.0) * 0.5;
                    col = mix(in_tint, float3(1.0), 0.55);
                } else if (in_style < 21.5) {
                    // Supernova: white flash, then glowing ring with radial streaks
                    float flash = band(r, 60.0 * dp) * exp(-t * 7.0);
                    float streaks = pow(abs(cos(ang * 14.0)), 24.0) * band(r - front * 0.75, 80.0 * dp) * 0.6;
                    a = flash + streaks + band(r - front, 6.0 * dp) * 0.8 + band(r - front, 28.0 * dp) * 0.25;
                    col = mix(in_tint, float3(1.0), clamp(flash + 0.35, 0.0, 1.0));
                } else if (in_style < 22.5) {
                    // Comet: bright head spiralling outwards with a long glowing tail
                    float corad = front * 0.9;
                    float cha = t * TAU * 2.5;
                    float2 chp = in_origin + float2(cos(cha), sin(cha)) * corad;
                    float cdiff = mod(cha - ang, TAU);
                    float ctail = exp(-cdiff * 2.2);
                    a = band(r - corad, 7.0 * dp) * ctail * 0.85
                      + band(length(p - chp), 11.0 * dp) * 1.0;
                    col = mix(in_tint, float3(1.0), 0.55);
                } else if (in_style < 23.5) {
                    // Silk: soft flowing ribbons moving through the ring
                    float flow = 0.5 + 0.5 * sin(ang * 4.0 + r / (22.0 * dp) - t * 9.0);
                    float sinside = 1.0 - smoothstep(front - 20.0 * dp, front, r);
                    a = flow * band(r - front * 0.7, 55.0 * dp) * sinside * 0.7
                      + band(r - front, 3.0 * dp) * 0.5;
                    col = mix(float3(1.0, 0.7, 0.85), float3(0.75, 0.7, 1.0), flow);
                } else if (in_style < 24.5) {
                    // Crystal: sharp icy rays with a thin frosted ring
                    float crays = pow(abs(cos(ang * 3.0)), 30.0);
                    a = crays * band(r - front * 0.7, 90.0 * dp) * 0.85
                      + band(r - front, 2.0 * dp) * 0.7
                      + band(r - front, 14.0 * dp) * 0.2;
                    col = mix(float3(0.7, 0.9, 1.0), float3(1.0), 0.4);
                } else if (in_style < 25.5) {
                    // Ember: orange sparks flying outwards and flickering
                    float ecells = 36.0;
                    float eidx = floor((ang + PI) / TAU * ecells);
                    float eh1 = hash11(eidx + 3.0);
                    float eh2 = hash11(eidx + 29.0);
                    float estart = 30.0 * dp;
                    float epr = estart + (in_maxr - estart) * e * (0.3 + 0.7 * eh1);
                    float eca = (eidx + 0.5) / ecells * TAU - PI;
                    float2 epos = in_origin + float2(cos(eca), sin(eca)) * epr;
                    float esize = (2.0 + 3.0 * eh2) * dp;
                    float eflick = 0.5 + 0.5 * sin(t * 55.0 + eh2 * TAU);
                    float ed = length(p - epos);
                    a = band(ed, esize) * eflick
                      + band(ed, esize * 4.0) * 0.25 * eflick
                      + band(r, 28.0 * dp) * exp(-t * 8.0) * 0.6;
                    col = mix(float3(1.0, 0.45, 0.1), float3(1.0, 0.85, 0.4), eh2);
                } else if (in_style < 26.5) {
                    // Plasma: shifting colourful interference pattern inside the ring
                    float pv = sin(r / (18.0 * dp) - t * 12.0)
                             + sin(ang * 5.0 + t * 6.0)
                             + sin((p.x + p.y) / (40.0 * dp) + t * 5.0);
                    float pl = 0.5 + 0.5 * sin(pv * 1.5);
                    float pinside = 1.0 - smoothstep(front - 25.0 * dp, front, r);
                    a = pl * pinside * 0.7 + band(r - front, 4.0 * dp) * 0.5;
                    col = 0.5 + 0.5 * cos(TAU * (pv * 0.15 + t * 0.5 + float3(0.0, 0.33, 0.67)));
                } else if (in_style < 27.5) {
                    // Vortex: swirling two-tone bands spinning outwards
                    float vtw = ang * 2.0 + r / (26.0 * dp) - t * 10.0;
                    float vb = 0.5 + 0.5 * sin(vtw);
                    float vin = 1.0 - smoothstep(front - 12.0 * dp, front, r);
                    a = pow(vb, 3.0) * vin * 0.85 + band(r - front, 3.0 * dp) * 0.5;
                    col = mix(float3(0.4, 0.8, 1.0), float3(0.9, 0.5, 1.0), vb);
                } else if (in_style < 28.5) {
                    // Neon Grid: cyberpunk rings and spokes
                    float gr = band(mod(r, 28.0 * dp) - 14.0 * dp, 1.6 * dp);
                    float gsp = pow(abs(cos(ang * 6.0)), 40.0);
                    float gin = 1.0 - smoothstep(front - 6.0 * dp, front, r);
                    a = (gr * 0.7 + gsp * 0.5 * smoothstep(0.0, 40.0 * dp, r)) * gin
                      + band(r - front, 3.0 * dp);
                    col = mix(float3(1.0, 0.2, 0.8), float3(0.2, 0.9, 1.0), smoothstep(0.0, in_maxr, r));
                } else if (in_style < 29.5) {
                    // Fireflies: soft pulsing lights drifting outwards
                    float fcells = 28.0;
                    float fidx = floor((ang + PI) / TAU * fcells);
                    float fh1 = hash11(fidx + 5.0);
                    float fh2 = hash11(fidx + 41.0);
                    float fpr = 35.0 * dp + (in_maxr * 0.8 - 35.0 * dp) * e * (0.25 + 0.75 * fh1);
                    float fca = (fidx + 0.5) / fcells * TAU - PI + 0.15 * sin(t * 5.0 + fh2 * TAU);
                    float2 fpos = in_origin + float2(cos(fca), sin(fca)) * fpr;
                    float fd = length(p - fpos);
                    float fpulse = 0.5 + 0.5 * sin(t * 12.0 + fh2 * TAU);
                    a = band(fd, (3.0 + 3.0 * fh2) * dp) * fpulse + band(fd, 16.0 * dp) * 0.2 * fpulse;
                    col = mix(float3(1.0, 0.95, 0.4), float3(0.6, 1.0, 0.5), fh2);
                } else if (in_style < 30.5) {
                    // Ocean: layered wave rings with foam on the crests
                    float ow = 5.0 * dp * sin(ang * 7.0 + t * 9.0);
                    float o1 = band(r - (front + ow), 10.0 * dp);
                    float o2 = band(r - (front * 0.8 + ow * 0.7), 8.0 * dp);
                    float o3 = band(r - (front * 0.6 + ow * 0.5), 6.0 * dp);
                    float foam = pow(0.5 + 0.5 * sin(ang * 18.0 - t * 20.0), 6.0) * o1;
                    a = o1 * 0.7 + o2 * 0.45 + o3 * 0.3 + foam * 0.5;
                    col = mix(float3(0.1, 0.5, 0.9), float3(0.85, 0.97, 1.0), clamp(foam + o1 * 0.2, 0.0, 1.0));
                } else {
                    // Galaxy: spiral arms, twinkling stars and a glowing core
                    float garm = pow(0.5 + 0.5 * cos(ang * 2.0 - r / (35.0 * dp) * 1.6 + t * 5.0), 4.0);
                    float gin2 = 1.0 - smoothstep(front - 10.0 * dp, front, r);
                    float gcore = band(r, 40.0 * dp) * exp(-t * 3.0);
                    float gstar = pow(hash11(floor(p.x / (9.0 * dp)) * 13.0 + floor(p.y / (9.0 * dp)) * 7.0), 40.0)
                                * (0.5 + 0.5 * sin(t * 30.0 + p.x));
                    a = garm * gin2 * 0.6 * smoothstep(0.0, 50.0 * dp, r) + gcore * 0.8
                      + gstar * gin2 * 0.9 + band(r - front, 3.0 * dp) * 0.4;
                    col = mix(float3(0.6, 0.5, 1.0), float3(1.0, 0.8, 0.9), garm);
                }

                float ringA = clamp(a * life, 0.0, 1.0);
                float3 ringRgb = col * ringA;

                float lensA = 0.0;
                float3 lensRgb = float3(0.0);
                if (in_lens_on > 0.5 && life > 0.0 && front > 1.0) {
                    float lensR = min(front, in_maxr * 0.7);
                    float x = r / lensR;
                    lensA = (1.0 - smoothstep(0.94, 1.0, x)) * life;
                    float pull = -0.20 * (1.0 - x * x) * life;
                    half4 bgc = in_bg.eval(p + v * pull);
                    float rim = exp(-pow((x - 0.94) / 0.06, 2.0)) * life;
                    lensRgb = mix(float3(bgc.rgb), col, rim * 0.55);
                }
                float3 outRgb = lensRgb * lensA + ringRgb * (1.0 - lensA);
                float outA = lensA + ringA * (1.0 - lensA);
                return half4(half3(outRgb), half(clamp(outA, 0.0, 1.0)));
            }
            """
    }

    fun setOrigin(x: Float, y: Float) { setFloatUniform("in_origin", x, y) }

    fun setSize(width: Float, height: Float) { setFloatUniform("in_size", width, height) }

    fun setTint(r: Float, g: Float, b: Float) { setFloatUniform("in_tint", r, g, b) }

    var progress: Float = 0f
        set(value) { field = value; setFloatUniform("in_progress", value) }

    var fade: Float = 0f
        set(value) { field = value; setFloatUniform("in_fade", value) }

    var style: Float = 2f
        set(value) { field = value; setFloatUniform("in_style", value) }

    var maxRadius: Float = 1f
        set(value) { field = value; setFloatUniform("in_maxr", value) }

    var density: Float = 1f
        set(value) { field = value; setFloatUniform("in_density", value) }

    /** Share of the animation where the front reaches [maxRadius], as the water effect does. */
    var reach: Float = 0.72f
        set(value) { field = value; setFloatUniform("in_reach", value) }

    /** Wallpaper (or any backdrop) bent inside the lens. Null turns the lens off. */
    fun setBackdrop(bitmap: android.graphics.Bitmap?) {
        if (bitmap == null) {
            setFloatUniform("in_lens_on", 0f)
            return
        }
        setInputShader(
            "in_bg",
            android.graphics.BitmapShader(
                bitmap,
                android.graphics.Shader.TileMode.CLAMP,
                android.graphics.Shader.TileMode.CLAMP
            )
        )
        setFloatUniform("in_lens_on", 1f)
    }

    init {
        setOrigin(0f, 0f)
        setSize(1f, 1f)
        setTint(1f, 1f, 1f)
        progress = 0f
        fade = 0f
        style = 2f
        maxRadius = 1f
        density = 1f
        reach = 0.72f
        // A child shader must always be bound; a 1x1 transparent bitmap keeps the lens off.
        setInputShader(
            "in_bg",
            android.graphics.BitmapShader(
                android.graphics.Bitmap.createBitmap(1, 1,
                    android.graphics.Bitmap.Config.ARGB_8888),
                android.graphics.Shader.TileMode.CLAMP,
                android.graphics.Shader.TileMode.CLAMP
            )
        )
        setBackdrop(null)
    }
}
