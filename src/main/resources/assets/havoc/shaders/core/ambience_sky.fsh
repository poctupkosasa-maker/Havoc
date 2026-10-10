#version 150

#define TAU 6.28318530718
#define PI  3.14159265359

in vec3 worldDir;
in float shaderTime;
in vec3 colorA;
in vec3 colorB;
in vec4 skySettings1;
in vec4 skySettings2;
in vec4 skySettings3;
in vec2 screenUV;
uniform sampler2D DepthSampler;
in vec4 weatherSettings;
out vec4 fragColor;

float sk_hash(vec3 p) {
    return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453123);
}

float sk_noise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 w3 = f * f * (3.0 - 2.0 * f);
    float n000 = sk_hash(i);
    float n100 = sk_hash(i + vec3(1.0, 0.0, 0.0));
    float n010 = sk_hash(i + vec3(0.0, 1.0, 0.0));
    float n110 = sk_hash(i + vec3(1.0, 1.0, 0.0));
    float n001 = sk_hash(i + vec3(0.0, 0.0, 1.0));
    float n101 = sk_hash(i + vec3(1.0, 0.0, 1.0));
    float n011 = sk_hash(i + vec3(0.0, 1.0, 1.0));
    float n111 = sk_hash(i + vec3(1.0, 1.0, 1.0));
    float x00 = mix(n000, n100, w3.x);
    float x10 = mix(n010, n110, w3.x);
    float x01 = mix(n001, n101, w3.x);
    float x11 = mix(n011, n111, w3.x);
    float y0 = mix(x00, x10, w3.y);
    float y1 = mix(x01, x11, w3.y);
    return mix(y0, y1, w3.z);
}

float sk_fbm(vec3 p) {
    float s = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        s += a * sk_noise(p);
        p *= 2.07;
        a *= 0.5;
    }
    return s;
}

void main() {
    float sceneDepth = texture(DepthSampler, screenUV).r;
    float skyPixel = 1.0 - smoothstep(0.000002, 0.00008, sceneDepth);
    vec3 rd = normalize(worldDir);
    float y = rd.y;
    float t = shaderTime;
    float n = clamp(skySettings2.w, 0.0, 1.0);
    float u_Scale = skySettings1.z;
    float rain = clamp(weatherSettings.x, 0.0, 1.0);
    float dayAngle = (skySettings3.x - 0.25) * TAU;
    vec3 sunDir = normalize(vec3(-sin(dayAngle), cos(dayAngle), 0.22));

    float breathe = 0.93 + 0.07 * sin(t * 0.26);
    float pulseSat = 0.96 + 0.04 * sin(t * 0.19 + 1.7);
    float pulseNight = 0.88 + 0.12 * sin(t * 0.12 + 0.4);

    float hazeMask = smoothstep(0.2, -0.38, y);
    hazeMask *= hazeMask;
    vec3 hazeOff = vec3(
        sin(t * 2.15 + dot(rd, vec3(1.0, 0.55, 0.35)) * 12.0),
        cos(t * 1.95 + dot(rd, vec3(-0.45, 1.0, 0.5)) * 11.5),
        sin(t * 2.35 + dot(rd, vec3(0.25, -0.4, 1.0)) * 13.0)
    ) * (0.0055 * hazeMask * u_Scale * mix(1.0, 0.65, n));

    vec3 rsum = rd + hazeOff;
    float rlen = length(rsum);
    vec3 rs = (rlen > 1.0e-4) ? (rsum / rlen) : rd;

    float sc = u_Scale;
    vec3 drift = vec3(t * 0.042, t * 0.024, t * 0.033);

    vec3 zenithD = vec3(0.04, 0.26, 0.78) * pulseSat;
    vec3 zenithN = vec3(0.015, 0.04, 0.14) * pulseNight;
    vec3 zenith = mix(zenithD, zenithN, n);

    vec3 horizonLightD = vec3(0.62, 0.86, 1.0);
    vec3 horizonLightN = vec3(0.05, 0.07, 0.16);
    vec3 horizonLight = mix(horizonLightD, horizonLightN, n);

    vec3 grassHintD = vec3(0.1, 0.42, 0.24);
    vec3 grassHintN = vec3(0.02, 0.05, 0.09);
    vec3 grassHint = mix(grassHintD, grassHintN, n);

    float gradMix = smoothstep(-0.38, 0.68, y);
    vec3 skyBase = mix(horizonLight, zenith, gradMix);
    float nearGround = (1.0 - gradMix) * smoothstep(-0.55, 0.18, y);
    skyBase = mix(skyBase, mix(skyBase, grassHint, 0.4), nearGround * mix(0.55, 0.35, n));

    vec3 pC = rs * sc * 1.85 + drift;
    float boil = sk_fbm(pC * 2.35 + vec3(t * 0.11));
    float cLow = sk_fbm(pC + vec3(boil * 0.5, -boil * 0.38, boil * 0.22));
    float cHi = sk_fbm(pC * 1.35 - drift * 0.55 + vec3(17.3, 84.1, 31.7));
    float clouds = cLow * 0.58 + cHi * 0.42;
    clouds = pow(clamp(clouds, 0.0, 1.0), mix(0.82, 0.92, n));
    float puffCore = smoothstep(0.5, 0.84, clouds);
    float puffEdge = smoothstep(0.36, 0.9, clouds) * (1.0 - puffCore) * mix(0.45, 0.32, n);
    vec3 cloudTintD = vec3(1.0, 0.99, 0.97);
    vec3 cloudTintN = vec3(0.22, 0.24, 0.32);
    vec3 cloudTint = mix(cloudTintD, cloudTintN, n);
    vec3 col = mix(skyBase, cloudTint, puffCore * 0.92);
    col = mix(col, mix(skyBase, cloudTint, 0.94), puffEdge * 1.18);
    col += vec3(1.0, 0.92, 0.75) * puffEdge * mix(0.12, 0.02, n) * breathe;

    // AstraLex-style layered cloud volume: several height slices share wind but
    // use rotated offsets, producing illuminated bodies instead of a flat mask.
    float volume = 0.0;
    vec3 volumeColor = vec3(0.0);
    if (rd.y > 0.015) {
        for (int layer = 0; layer < 12; layer++) {
            float fi = float(layer);
            float heightSlice = 0.72 + fi * 0.055;
            vec2 plane = rd.xz / max(rd.y, 0.025) * heightSlice;
            float angle = fi * 2.391;
            plane += vec2(cos(angle), sin(angle)) * 0.055;
            vec3 samplePos = vec3(plane * (0.72 * u_Scale)
                    + vec2(t * 0.018, t * 0.004), fi * 0.19);
            float density = sk_fbm(samplePos);
            density += sk_fbm(samplePos * 2.17 + vec3(17.0, 3.0, 29.0)) * 0.34;
            density = smoothstep(0.60 - rain * 0.14, 0.83 - rain * 0.08, density);
            density *= 1.0 - volume;
            float sunLight = clamp(dot(sunDir, normalize(vec3(-plane.x, 1.8, -plane.y)))
                    * 0.5 + 0.5, 0.0, 1.0);
            vec3 litCloud = mix(vec3(0.34, 0.37, 0.45), vec3(1.0, 0.94, 0.84),
                    0.28 + sunLight * 0.72);
            litCloud = mix(litCloud, vec3(0.28, 0.30, 0.34), rain * 0.72);
            volumeColor += litCloud * density * (0.45 + sunLight * 0.55);
            volume += density * 0.72;
            if (volume > 0.97) break;
        }
        float horizonFade = smoothstep(0.012, 0.13, rd.y);
        volume = clamp(volume * horizonFade, 0.0, 0.96);
        volumeColor /= max(volume * 1.15, 0.16);
        col = mix(col, volumeColor, volume);
    }

    vec3 gBase = rs * sc * 1.25 + drift * 0.85 + vec3(0.0, sin(t * 0.28) * 0.12, 0.0);
    float g1 = sk_fbm(gBase + sk_fbm(gBase * 1.65 + vec3(t * 0.095)) * 0.55);
    float g2 = sk_fbm(gBase * 2.2 - vec3(t * 0.05, t * 0.03, t * 0.04));
    float goldMask = smoothstep(0.52, 0.8, g1 * 0.55 + g2 * 0.45);
    goldMask *= smoothstep(-0.15, 0.58, y);
    goldMask *= 0.55 + 0.45 * sin(dot(rs, vec3(1.15, 2.05, 0.75)) * 3.0 + t * 0.7);
    goldMask *= mix(1.0, 0.08, n);
    vec3 goldA = vec3(1.0, 0.843, 0.0);
    vec3 goldB = vec3(1.0, 0.58, 0.15);
    vec3 goldCol = mix(goldA, goldB, sin(t * 0.45 + dot(rs, vec3(0.55, 1.95, 1.05)) * 4.0) * 0.5 + 0.5);
    vec3 tintRgb = vec3(colorA.r, colorA.g, colorA.b);
    goldCol = mix(goldCol, clamp(tintRgb, 0.15, 1.0), 0.2);
    col += goldCol * goldMask * 0.5 * breathe;

    float airLight = pow(max(0.0, 1.0 - abs(y) - 0.04), 2.15);
    col = mix(col, col * 1.06 + vec3(0.06, 0.07, 0.03), airLight * mix(0.38, 0.12, n));

    vec3 sp = rs * 48.0 + vec3(t * 0.018, t * 0.011, t * 0.014);
    float st = sk_fbm(sp);
    float tw = sin(t * 2.4 + st * 40.0) * 0.5 + 0.5;
    float starField = smoothstep(0.72, 0.92, st) * smoothstep(0.15, 0.85, y) * n;
    starField *= (0.35 + 0.65 * tw);
    col += vec3(0.75, 0.82, 1.0) * starField * 0.55;

    float sunDot = max(dot(rd, sunDir), 0.0);
    float sunDisc = smoothstep(0.9994, 0.99986, sunDot);
    vec3 sunPlane = normalize(rd - sunDir * dot(rd, sunDir) + vec3(0.00001));
    float rayNoise = sk_fbm(sunPlane * (7.0 / max(skySettings3.w, 0.1))
            + vec3(t * 0.035, -t * 0.021, t * 0.014));
    float shafts = pow(sunDot, mix(7.0, 2.2, clamp(skySettings3.w / 2.5, 0.0, 1.0)));
    shafts *= smoothstep(0.38, 0.82, rayNoise) * skySettings3.y * skySettings3.z;
    shafts *= smoothstep(-0.12, 0.16, sunDir.y) * (1.0 - n * 0.8);
    vec3 sunlight = mix(vec3(1.0, 0.55, 0.24), vec3(1.0, 0.92, 0.72),
            smoothstep(0.08, 0.55, sunDir.y));
    col += sunlight * (sunDisc * 1.45 + shafts * 0.72);

    col = clamp(col * mix(1.01 + 0.035 * sin(t * 0.21), 0.92 + 0.04 * sin(t * 0.17), n), 0.0, 1.0);
    if (skyPixel > 0.5) {
        fragColor = vec4(col, 1.0);
    } else {
        float distanceFog = 1.0 - smoothstep(0.0015, 0.055, sceneDepth);
        distanceFog = pow(clamp(distanceFog, 0.0, 1.0), mix(1.35, 0.72, rain));
        vec3 horizonFog = mix(vec3(1.0, 0.72, 0.46), horizonLight, 0.58);
        horizonFog = mix(horizonFog, vec3(0.055, 0.07, 0.14), n);
        horizonFog = mix(horizonFog, vec3(0.42, 0.46, 0.52), rain * 0.75);
        float fogShafts = shafts * distanceFog * (1.0 - rain * 0.72);
        fogShafts *= 0.55 + 0.45 * smoothstep(0.0, 0.35, 1.0 - abs(rd.y));
        horizonFog += sunlight * fogShafts * 1.15;
        float fogAlpha = distanceFog * mix(0.72, 0.94, rain);
        fogAlpha = clamp(fogAlpha + fogShafts * 0.16, 0.0, 0.96);
        fragColor = vec4(horizonFog, fogAlpha);
    }
}
