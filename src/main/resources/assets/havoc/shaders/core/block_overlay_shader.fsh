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
            mix(lumaHash3(p + vec3(0.0, 0.0, 0.0)), lumaHash3(p + vec3(1.0, 0.0, 0.0)), f.x),
            mix(lumaHash3(p + vec3(0.0, 1.0, 0.0)), lumaHash3(p + vec3(1.0, 1.0, 0.0)), f.x),
            f.y),
        mix(
            mix(lumaHash3(p + vec3(0.0, 0.0, 1.0)), lumaHash3(p + vec3(1.0, 0.0, 1.0)), f.x),
            mix(lumaHash3(p + vec3(0.0, 1.0, 1.0)), lumaHash3(p + vec3(1.0, 1.0, 1.0)), f.x),
            f.y),
        f.z);
}

float lumaFbm3(vec3 p) {
    float value = 0.0;
    float amp = 0.52;
    for (int i = 0; i < 5; i++) {
        value += amp * lumaNoise3(p);
        p = p * 2.04 + vec3(11.7, 6.3, 17.9);
        amp *= 0.5;
    }
    return value;
}

float softCracks(vec3 p, vec3 n) {
    vec3 q = p * 3.2 + vec3(time * 0.045, time * 0.018, time * 0.034);
    float slowPulse = 0.70 + 0.30 * sin(time * 1.20 + dot(p, vec3(3.0, -2.0, 2.5)));
    float sheetA = abs(sin(dot(q, normalize(vec3(0.9, 0.25, 0.55))) * 5.2 + lumaFbm3(q * 1.8) * 2.4 + slowPulse));
    float sheetB = abs(sin(dot(q, normalize(vec3(-0.35, 0.8, 0.45))) * 4.5 + lumaFbm3(q * 2.3 + 9.0) * 2.0 - slowPulse));
    float cracks = (1.0 - smoothstep(0.035, 0.16, min(sheetA, sheetB))) * 0.55;
    float faceFade = 0.45 + 0.55 * (1.0 - abs(n.y) * 0.25);
    return cracks * faceFade;
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

    float edgeDistance = min(min(faceUv.x, 1.0 - faceUv.x), min(faceUv.y, 1.0 - faceUv.y));
    float edgeBand = 1.0 - smoothstep(0.015, 0.30, edgeDistance);
    float edgeBreath = 0.82 + 0.18 * sin(time * 1.45 + dot(local, vec3(5.0, 3.0, 4.0)));
    float softEdgeFrost = pow(edgeBand, 1.35) * edgeBreath;
    float rim = pow(1.0 - clamp(abs(dot(normal, viewDir)), 0.0, 1.0), 2.8);

    vec3 drift = vec3(time * 0.052, time * 0.021, time * 0.039);
    vec3 swirl = vec3(
        sin(time * 0.75 + centered.y * 5.0),
        cos(time * 0.62 + centered.z * 4.5),
        sin(time * 0.68 + centered.x * 5.5)
    ) * 0.055;
    float cloudyIce = lumaFbm3(centered * 3.25 + drift + swirl);
    float fineFrost = lumaFbm3(centered * 9.4 - drift * 2.1 + swirl * 2.5 + vec3(4.0, 11.0, 2.0));
    float flow = 0.5 + 0.5 * sin((centered.x * 2.4 + centered.z * 2.9 - centered.y * 1.7) * 4.0 + time * 2.15 + fineFrost * 2.2);
    float frost = smoothstep(0.42, 0.86, cloudyIce * 0.56 + fineFrost * 0.30 + flow * 0.14);
    float cracks = softCracks(centered + swirl * 0.75, normal) * smoothstep(0.18, 0.95, fineFrost);
    float shimmer = pow(smoothstep(0.68, 1.0, flow) * (0.55 + 0.45 * fineFrost), 2.2);

    float topLight = smoothstep(-0.55, 0.55, normal.y) * 0.18;
    float innerDepth = smoothstep(0.78, 0.10, length(centered));

    vec3 clientColor = max(themeColor, vec3(0.0));
    float clientMax = max(max(clientColor.r, clientColor.g), clientColor.b);
    vec3 clientAccent = clientMax < 0.15 ? vec3(0.22, 0.66, 1.00) : clientColor / clientMax;
    vec3 coolAccent = min(mix(clientAccent, vec3(0.48, 0.86, 1.00), 0.14) * 1.16, vec3(1.35));

    vec3 clearIce = mix(vec3(0.032, 0.080, 0.130), coolAccent * 0.46, 0.72);
    vec3 blueIce = mix(coolAccent * 0.98, vec3(0.28, 0.62, 0.92), 0.14);
    vec3 whiteFrost = mix(vec3(0.72, 0.91, 1.00), coolAccent, 0.28);
    vec3 deepCore = mix(vec3(0.015, 0.035, 0.065), clientAccent * 0.24, 0.68);

    vec3 color = mix(deepCore, clearIce, 0.62 + innerDepth * 0.18);
    color = mix(color, blueIce, frost * 0.42);
    color = mix(color, whiteFrost, cracks * 0.34 + softEdgeFrost * 0.52 + shimmer * 0.08);
    color += coolAccent * rim * 0.24;
    color += mix(coolAccent, vec3(0.76, 0.94, 1.10), 0.38) * topLight;
    color += mix(whiteFrost, coolAccent, 0.22) * softEdgeFrost * 0.20;
    color += coolAccent * shimmer * 0.10;
    color += clientAccent * 0.045;
    color *= 0.98 + cloudyIce * 0.18 + flow * 0.06;
    color *= clamp(shaderBrightness, 0.0, 3.0);

    float alpha = shaderAlpha * (0.32 + frost * 0.13 + cracks * 0.10 + softEdgeFrost * 0.16 + rim * 0.09 + shimmer * 0.05);
    fragColor = vec4(color, clamp(alpha, 0.0, 0.68));
}
