#version 330

in vec3 worldPos;
in float time;
in vec3 themeColor;
in float shaderBrightness;
in float shaderAlpha;
in vec3 overlayBoundsMin;
in vec3 overlayBoundsSize;

out vec4 fragColor;

layout(std140) uniform BlockOverlayData {
    vec4 cameraAndTime;
    vec4 themeAndBrightness;
    vec4 shaderSettings;
    vec4 boundsMin;
    vec4 boundsSize;
};

float lumaWaveHash3(vec3 p) {
    p = fract(p * vec3(0.1031, 0.11369, 0.13787));
    p += dot(p, p.yxz + 19.19);
    return fract((p.x + p.y) * p.z);
}

float lumaWaveNoise3(vec3 x) {
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);

    return mix(
        mix(
            mix(lumaWaveHash3(p + vec3(0.0, 0.0, 0.0)), lumaWaveHash3(p + vec3(1.0, 0.0, 0.0)), f.x),
            mix(lumaWaveHash3(p + vec3(0.0, 1.0, 0.0)), lumaWaveHash3(p + vec3(1.0, 1.0, 0.0)), f.x),
            f.y),
        mix(
            mix(lumaWaveHash3(p + vec3(0.0, 0.0, 1.0)), lumaWaveHash3(p + vec3(1.0, 0.0, 1.0)), f.x),
            mix(lumaWaveHash3(p + vec3(0.0, 1.0, 1.0)), lumaWaveHash3(p + vec3(1.0, 1.0, 1.0)), f.x),
            f.y),
        f.z);
}

float lumaWaveFbm3(vec3 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += lumaWaveNoise3(p) * a;
        p = p * 2.04 + vec3(12.7, 5.1, 8.9);
        a *= 0.5;
    }
    return v;
}

vec3 normalizedAccent() {
    vec3 accent = max(themeColor, vec3(0.0));
    float maxChannel = max(max(accent.r, accent.g), accent.b);
    if (maxChannel < 0.15) {
        return vec3(0.22, 0.66, 1.00);
    }
    return accent / maxChannel;
}

void main() {
    vec3 local = clamp((worldPos - overlayBoundsMin) / overlayBoundsSize, vec3(0.0), vec3(1.0));
    vec3 centered = local - 0.5;

    vec3 normal = normalize(cross(dFdx(worldPos), dFdy(worldPos)));
    vec3 viewDir = normalize(cameraAndTime.xyz - worldPos);

    vec3 an = abs(normal);
    vec2 uv;
    if (an.x > an.y && an.x > an.z) {
        uv = local.zy;
    } else if (an.y > an.z) {
        uv = local.xz;
    } else {
        uv = local.xy;
    }

    float edgeDistance = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
    float edgeGlow = pow(1.0 - smoothstep(0.0, 0.18, edgeDistance), 1.7);
    float rim = pow(1.0 - clamp(abs(dot(normal, viewDir)), 0.0, 1.0), 2.0);

    float t = time;
    float turbulence = lumaWaveFbm3(vec3(uv * 3.6, centered.y * 2.0) + vec3(t * 0.18, t * 0.11, t * 0.23));
    vec2 flowUv = uv;
    flowUv += vec2(t * 0.055, -t * 0.038);
    flowUv += vec2(
        sin(t * 0.70 + uv.y * 6.2 + centered.z * 2.0),
        cos(t * 0.58 + uv.x * 5.6 + centered.x * 1.8)
    ) * 0.034;
    flowUv += (turbulence - 0.5) * 0.075;

    float broadA = sin(flowUv.x * 9.0 + flowUv.y * 4.2 + t * 2.35 + turbulence * 2.2);
    float broadB = sin(flowUv.x * -5.6 + flowUv.y * 12.0 - t * 2.05 + turbulence * 1.7);
    float broadC = sin((flowUv.x + flowUv.y) * 7.0 + t * 1.45);
    float broadWave = broadA * 0.48 + broadB * 0.34 + broadC * 0.18;
    float broad01 = broadWave * 0.5 + 0.5;
    float swell = smoothstep(0.18, 0.86, broad01);

    float bandA = 0.5 + 0.5 * sin(flowUv.y * 20.0 + flowUv.x * 7.5 + t * 4.15 + turbulence * 4.0);
    float bandB = 0.5 + 0.5 * sin(flowUv.x * 24.0 - flowUv.y * 8.0 - t * 3.65 + turbulence * 2.6);
    float bandC = 0.5 + 0.5 * sin((flowUv.x - flowUv.y) * 18.0 + t * 3.10 + turbulence * 3.2);
    float ribbons = pow(smoothstep(0.48, 0.98, bandA), 3.0) * 0.52;
    ribbons += pow(smoothstep(0.54, 0.97, bandB), 3.6) * 0.34;
    ribbons += pow(smoothstep(0.62, 0.98, bandC), 4.2) * 0.24;
    ribbons = clamp(ribbons, 0.0, 1.0);

    float causticA = abs(sin(flowUv.x * 31.0 + flowUv.y * 11.0 + t * 4.6 + turbulence * 5.0));
    float causticB = abs(sin(flowUv.x * -13.0 + flowUv.y * 28.0 - t * 4.0));
    float caustics = 1.0 - smoothstep(0.025, 0.18, min(causticA, causticB));
    caustics *= smoothstep(0.22, 0.92, broad01);

    float pulse = 0.92 + 0.08 * sin(t * 1.55 + centered.x * 3.0 + centered.z * 2.0);
    float centerLight = 1.0 - smoothstep(0.10, 0.80, length(centered));

    vec3 accent = normalizedAccent();
    vec3 waterAccent = min(mix(accent, vec3(0.03, 0.66, 1.00), 0.10) * 1.15, vec3(1.35));
    vec3 deep = vec3(0.008, 0.020, 0.032) + waterAccent * 0.36 + accent * 0.18;
    vec3 body = mix(deep, waterAccent * 1.08 + accent * 0.26, 0.30 + swell * 0.42 + centerLight * 0.12);
    vec3 light = mix(waterAccent, vec3(0.78, 0.95, 1.00), 0.30);

    vec3 color = body;
    color += light * ribbons * 0.44;
    color += light * caustics * 0.52;
    color += waterAccent * swell * 0.16;
    color += waterAccent * edgeGlow * 0.18;
    color += mix(vec3(0.72, 0.92, 1.05), waterAccent, 0.52) * rim * 0.18;
    color += accent * 0.055;
    color *= pulse * (1.04 + turbulence * 0.18);
    color *= clamp(shaderBrightness, 0.0, 3.0);

    float alpha = shaderAlpha * (0.56 + swell * 0.10 + ribbons * 0.18 + caustics * 0.16 + edgeGlow * 0.08 + rim * 0.07);
    fragColor = vec4(color, clamp(alpha, 0.0, 0.88));
}
