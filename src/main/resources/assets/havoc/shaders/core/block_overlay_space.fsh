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

float lumaSpaceHash31(vec3 p) {
    p = fract(p * vec3(0.1031, 0.11369, 0.13787));
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float lumaSpaceNoise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float n000 = lumaSpaceHash31(i + vec3(0.0, 0.0, 0.0));
    float n100 = lumaSpaceHash31(i + vec3(1.0, 0.0, 0.0));
    float n010 = lumaSpaceHash31(i + vec3(0.0, 1.0, 0.0));
    float n110 = lumaSpaceHash31(i + vec3(1.0, 1.0, 0.0));
    float n001 = lumaSpaceHash31(i + vec3(0.0, 0.0, 1.0));
    float n101 = lumaSpaceHash31(i + vec3(1.0, 0.0, 1.0));
    float n011 = lumaSpaceHash31(i + vec3(0.0, 1.0, 1.0));
    float n111 = lumaSpaceHash31(i + vec3(1.0, 1.0, 1.0));

    float nx00 = mix(n000, n100, f.x);
    float nx10 = mix(n010, n110, f.x);
    float nx01 = mix(n001, n101, f.x);
    float nx11 = mix(n011, n111, f.x);
    float nxy0 = mix(nx00, nx10, f.y);
    float nxy1 = mix(nx01, nx11, f.y);
    return mix(nxy0, nxy1, f.z);
}

float lumaSpaceFbm3(vec3 p) {
    float value = 0.0;
    float amp = 0.52;

    for (int i = 0; i < 5; i++) {
        value += lumaSpaceNoise3(p) * amp;
        p = p * 2.03 + vec3(7.1, 3.8, 5.4);
        p = vec3(p.y + p.z * 0.31, p.z - p.x * 0.27, p.x + p.y * 0.23);
        amp *= 0.50;
    }

    return value;
}

vec3 lumaSpaceSaturate(vec3 color, float amount) {
    float luma = dot(color, vec3(0.299, 0.587, 0.114));
    return max(mix(vec3(luma), color, amount), vec3(0.0));
}

void main() {
    vec3 local = clamp((worldPos - overlayBoundsMin) / overlayBoundsSize, vec3(0.0), vec3(1.0));
    vec3 centered = local - 0.5;

    vec3 normal = normalize(cross(dFdx(worldPos), dFdy(worldPos)));
    vec3 viewDir = normalize(cameraAndTime.xyz - worldPos);

    vec3 an = abs(normal);
    vec2 faceUv;
    if (an.x > an.y && an.x > an.z) {
        faceUv = local.zy;
    } else if (an.y > an.z) {
        faceUv = local.xz;
    } else {
        faceUv = local.xy;
    }

    float t = time;
    vec3 volumePos = centered * 2.05;
    vec3 drift = vec3(t * 0.048, -t * 0.026, t * 0.038);
    vec3 swirl = vec3(
        sin(t * 0.62 + volumePos.y * 2.6 + volumePos.z * 0.8),
        cos(t * 0.54 + volumePos.z * 2.3 - volumePos.x * 0.7),
        sin(t * 0.58 + volumePos.x * 2.4 + volumePos.y * 0.6)
    ) * 0.075;
    vec3 spacePos = volumePos + drift + swirl;
    vec3 waveDir = normalize(vec3(0.43, 0.72, 0.54));
    vec3 waveCross = normalize(vec3(-0.62, 0.18, 0.76));
    vec3 waveThird = normalize(vec3(-0.22, 0.66, -0.72));
    float spaceWave = sin(dot(spacePos, waveDir) * 4.65 + t * 1.45) * 0.48
        + sin(dot(spacePos, waveCross) * 5.25 - t * 1.18) * 0.34
        + sin(dot(spacePos, waveThird) * 3.70 + t * 0.82 + sin(t * 0.31) * 0.35) * 0.18;
    float wave01 = spaceWave * 0.5 + 0.5;
    float waveGlow = pow(smoothstep(0.44, 0.94, wave01), 1.85);
    spacePos += waveDir * (wave01 - 0.5) * 0.115
        + vec3(
            sin(t * 0.72 + volumePos.y * 2.8),
            cos(t * 0.65 + volumePos.z * 2.5),
            sin(t * 0.69 + volumePos.x * 2.7)
        ) * 0.028;

    float dust = lumaSpaceFbm3(spacePos * 1.48);
    float fineDust = lumaSpaceFbm3(spacePos * 4.25 + vec3(-t * 0.038, t * 0.030, t * 0.024));

    float bandCurve = dot(spacePos, waveDir) + sin(dot(spacePos, waveCross) * 2.15 + t * 0.18) * 0.125;
    float galaxyBand = exp(-bandCurve * bandCurve * 4.35);
    float nebula = smoothstep(0.26, 0.82, dust * 0.60 + fineDust * 0.28 + galaxyBand * 0.40 + waveGlow * 0.08);
    float darkLanes = smoothstep(0.48, 0.92, lumaSpaceFbm3(spacePos * 2.70 + vec3(9.0, -3.0, 5.0))) * galaxyBand;
    float softMist = smoothstep(0.28, 0.86, lumaSpaceFbm3(spacePos * 0.92 + vec3(t * 0.022, -t * 0.018, t * 0.014)) + galaxyBand * 0.24);
    float veil = smoothstep(0.40, 0.92, lumaSpaceFbm3(spacePos * 2.05 + vec3(-t * 0.032, t * 0.026, -t * 0.020)) + waveGlow * 0.14);

    float edgeDistance = min(min(faceUv.x, 1.0 - faceUv.x), min(faceUv.y, 1.0 - faceUv.y));
    float edgeGlow = pow(1.0 - smoothstep(0.0, 0.28, edgeDistance), 1.55);
    float rim = pow(1.0 - clamp(abs(dot(normal, viewDir)), 0.0, 1.0), 2.35);
    float centerDepth = 1.0 - smoothstep(0.12, 0.86, length(centered));
    float innerGlow = smoothstep(0.10, 0.78, centerDepth + nebula * 0.25 + softMist * 0.18);

    vec3 clientColor = max(themeColor, vec3(0.0));
    float clientMax = max(max(clientColor.r, clientColor.g), clientColor.b);
    vec3 clientAccent = clientMax < 0.15 ? vec3(0.38, 0.52, 1.00) : clientColor / clientMax;
    vec3 accent = lumaSpaceSaturate(clientAccent, 1.18);
    vec3 deepSpace = vec3(0.004, 0.005, 0.014) + accent * 0.035;
    vec3 themeDark = accent * 0.22;
    vec3 themeMid = accent * 0.62;
    vec3 themeBright = accent * 1.08;
    vec3 themeMist = accent * 0.42;
    vec3 themeGlow = accent * 1.22;

    vec3 color = deepSpace;
    color = mix(color, themeMid, nebula * 0.72 + softMist * 0.22 + galaxyBand * 0.18);
    color += themeBright * nebula * 0.28;
    color += themeDark * galaxyBand * dust * 0.14;
    color += themeMist * softMist * 0.36;
    color += themeMid * veil * 0.18;
    color *= 1.0 - darkLanes * 0.22;
    color += themeBright * waveGlow * 0.14;
    color += themeGlow * edgeGlow * 0.13;
    color += themeGlow * rim * 0.12;
    color += themeBright * innerGlow * 0.090;
    color *= 0.90 + 0.10 * sin(t * 1.35 + dust * 4.2 + softMist * 2.4 + wave01 * 1.8);
    color = lumaSpaceSaturate(color, 1.10);
    color *= clamp(shaderBrightness, 0.0, 3.0);

    float alpha = shaderAlpha * (0.58 + nebula * 0.14 + softMist * 0.11 + veil * 0.06 + waveGlow * 0.05 + edgeGlow * 0.10 + rim * 0.07);
    fragColor = vec4(color, clamp(alpha, 0.0, 0.88));
}
