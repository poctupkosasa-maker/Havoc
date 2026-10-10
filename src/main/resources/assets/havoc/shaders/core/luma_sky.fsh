#version 150

in vec3 worldDir;
in float time;
in vec3 vThemeColor;
in float skyBrightness;
in float skyBlur;

out vec4 fragColor;









float lumaHash3(vec3 p) {
    p = fract(p * vec3(443.8975, 397.2973, 491.1871));
    p += dot(p, p.yxz + 19.19);
    return fract((p.x + p.y) * p.z);
}

float lumaNoise3(vec3 x) {
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);

    return mix(
        mix(
            mix(lumaHash3(p + vec3(0,0,0)), lumaHash3(p + vec3(1,0,0)), f.x),
            mix(lumaHash3(p + vec3(0,1,0)), lumaHash3(p + vec3(1,1,0)), f.x),
            f.y),
        mix(
            mix(lumaHash3(p + vec3(0,0,1)), lumaHash3(p + vec3(1,0,1)), f.x),
            mix(lumaHash3(p + vec3(0,1,1)), lumaHash3(p + vec3(1,1,1)), f.x),
            f.y),
        f.z);
}

float lumaFbm3(vec3 p) {
    float f = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 5; i++) {
        f += amp * lumaNoise3(p);
        p = p * 2.02 + vec3(time * 0.03, time * 0.02, time * 0.025);
        amp *= 0.5;
    }
    return f;
}

vec3 sampleSky(vec3 d) {
    d = normalize(d);
    






    vec3 drift = vec3(time * 0.012, time * 0.006, time * 0.009);

    




    float smoke = lumaFbm3(d * 2.0 + drift + vec3(time * 0.020, 0.0, time * 0.015));
    float wisps = lumaFbm3(d * 5.5 + drift * 1.4 + vec3(0.0, time * 0.045, time * 0.030));

    float density = smoke * 0.65 + wisps * 0.35;
    density = clamp(density * 1.6, 0.0, 1.0);

    







    vec3 accent = max(vThemeColor, vec3(0.0));
    vec3 base      = accent * 0.05 + vec3(0.015, 0.020, 0.030);
    vec3 deepNavy  = accent * 0.35 + vec3(0.010);
    vec3 steelBlue = accent * 0.85;
    vec3 cyanHi    = clamp(accent * 1.40 + vec3(0.05), 0.0, 1.5);

    vec3 col = base;
    col = mix(col, deepNavy,  density);
    col = mix(col, steelBlue, smoothstep(0.40, 0.85, density));
    col += cyanHi * pow(density, 2.2) * 0.45;

    
    float horizonGlow = exp(-abs(d.y) * 5.0) * 0.18;
    col += accent * horizonGlow * 1.6;

    




    vec3 sg = d * 50.0 + vec3(time * 0.004, time * 0.002, time * 0.003);
    vec3 sId = floor(sg);
    vec3 sGv = fract(sg) - 0.5;
    float sH = lumaHash3(sId);
    if (sH > 0.96 && d.y > 0.0) {
        vec3 off = vec3(
            (lumaHash3(sId + 1.7) - 0.5) * 0.6,
            (lumaHash3(sId + 3.1) - 0.5) * 0.6,
            (lumaHash3(sId + 5.9) - 0.5) * 0.6);
        float twinkle = 0.55 + 0.45 * sin(time * 1.7 + sH * 50.0);
        float dist = length(sGv - off);
        float core = smoothstep(0.05, 0.0, dist);
        float halo = smoothstep(0.16, 0.0, dist) * 0.30;
        vec3 tint = mix(
            vec3(0.78, 0.88, 1.00),
            vec3(1.00, 0.92, 0.78),
            step(0.55, lumaHash3(sId + 7.3)));
        col += tint * (core + halo) * twinkle * 0.65;
    }

    
    vec3 bg = d * 14.0 + vec3(time * 0.002, time * 0.0015, time * 0.0025);
    vec3 bId = floor(bg);
    vec3 bGv = fract(bg) - 0.5;
    float bH = lumaHash3(bId + 19.0);
    if (bH > 0.985 && d.y > 0.05) {
        vec3 bOff = vec3(
            (lumaHash3(bId + 23.0) - 0.5) * 0.5,
            (lumaHash3(bId + 29.0) - 0.5) * 0.5,
            (lumaHash3(bId + 31.0) - 0.5) * 0.5);
        float bDist = length(bGv - bOff);
        float bPulse = 0.7 + 0.3 * sin(time * 0.9 + bH * 100.0);
        float bCore = smoothstep(0.06, 0.0, bDist);
        float bHalo = smoothstep(0.20, 0.0, bDist) * 0.50;
        col += vec3(0.92, 1.00, 1.10) * (bCore + bHalo) * bPulse * 1.00;
    }

    return col;
}

void main() {
    vec3 d = normalize(worldDir);
    vec3 col = sampleSky(d);
    float blur = clamp(skyBlur, 0.0, 1.0);

    if (blur > 0.001) {
        vec3 axis = abs(d.y) < 0.95 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);
        vec3 tangentX = normalize(cross(axis, d));
        vec3 tangentY = normalize(cross(d, tangentX));
        float radius = blur * 0.08;

        vec3 blurred = col * 0.44;
        blurred += sampleSky(d + tangentX * radius) * 0.14;
        blurred += sampleSky(d - tangentX * radius) * 0.14;
        blurred += sampleSky(d + tangentY * radius) * 0.14;
        blurred += sampleSky(d - tangentY * radius) * 0.14;
        col = mix(col, blurred, blur);
    }

    fragColor = vec4(col * skyBrightness, 1.0);
}
