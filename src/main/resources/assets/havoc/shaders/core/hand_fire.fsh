#version 150

in vec2 texCoord;
in vec2 texelSize;

out vec4 fragColor;

uniform sampler2D SceneSampler;
uniform sampler2D BlurSampler;
uniform sampler2D MaskSampler;

layout(std140) uniform HandFireData {
    vec4 resolution;
    vec4 glowColor;
    vec4 settings;
    vec4 settings2;
};

float sampleMask(vec2 uv) {
    return texture(MaskSampler, clamp(uv, vec2(0.0), vec2(1.0))).r;
}

float edgeMask(vec2 uv) {
    float c = sampleMask(uv);
    float e = 0.0;
    e += abs(c - sampleMask(uv + vec2( texelSize.x, 0.0)));
    e += abs(c - sampleMask(uv + vec2(-texelSize.x, 0.0)));
    e += abs(c - sampleMask(uv + vec2(0.0,  texelSize.y)));
    e += abs(c - sampleMask(uv + vec2(0.0, -texelSize.y)));
    return clamp(e * 0.9, 0.0, 1.0);
}

void main() {
    vec4 scene = texture(SceneSampler, texCoord);
    vec4 smoke = texture(BlurSampler, texCoord);

    float intensity = clamp(resolution.w, 0.0, 1.5);
    float smokeSetting = clamp(settings2.y, 0.0, 0.8);
    float mask = sampleMask(texCoord);
    float edge = edgeMask(texCoord);

    float activity = clamp(settings2.z, 0.0, 1.0);
    float trail = clamp(smoke.a, 0.0, 1.0);
    float bloom = pow(trail, 0.62);
    float aura = smoothstep(0.012, 0.28, trail);
    float smokeAlpha = clamp(max(bloom * 0.62, aura * 0.38), 0.0, 0.76);
    smokeAlpha *= (0.92 + intensity * 0.48 + smokeSetting * 0.35 + activity * 0.12);
    smokeAlpha = clamp(smokeAlpha, 0.0, 0.86);
    smokeAlpha = smokeAlpha < 0.005 ? 0.0 : smokeAlpha;
    smokeAlpha *= 1.0 - mask * 0.18;

    vec3 outColor = scene.rgb;
    vec3 smokeColor = clamp(smoke.rgb * (1.18 + smokeSetting * 0.24), 0.0, 1.0);
    outColor += smokeColor * smokeAlpha;
    outColor += smokeColor * edge * bloom * (0.16 + intensity * 0.12);

    fragColor = vec4(clamp(outColor, 0.0, 1.0), scene.a);
}
