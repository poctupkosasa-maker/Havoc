#version 150

in vec2 texCoord;
in vec2 texelSize;

out vec4 fragColor;

uniform sampler2D SceneSampler;
uniform sampler2D BlurSampler;
uniform sampler2D MaskSampler;

layout(std140) uniform GlassData {
    vec4 resolution;
    vec4 tintColor;
    vec4 settings;
};

vec3 adjustSaturation(vec3 color, float saturation) {
    float gray = dot(color, vec3(0.299, 0.587, 0.114));
    return mix(vec3(gray), color, saturation);
}

float getEdge(vec2 uv) {
    float center = texture(MaskSampler, uv).r;
    float edge = 0.0;

    edge += abs(center - texture(MaskSampler, uv + vec2(texelSize.x, 0.0)).r);
    edge += abs(center - texture(MaskSampler, uv - vec2(texelSize.x, 0.0)).r);
    edge += abs(center - texture(MaskSampler, uv + vec2(0.0, texelSize.y)).r);
    edge += abs(center - texture(MaskSampler, uv - vec2(0.0, texelSize.y)).r);

    return clamp(edge * 2.0, 0.0, 1.0);
}

void main() {
    vec4 scene = texture(SceneSampler, texCoord);
    float maskValue = texture(MaskSampler, texCoord).r;

    if (maskValue < 0.5) {
        fragColor = scene;
        return;
    }

    float saturation = resolution.z;
    float doReflect = resolution.w;
    float tintIntensity = settings.x;
    float edgeGlowIntensity = settings.y;
    float liquidGlass = settings.z;

    vec2 blurUV = texCoord;
    if (doReflect > 0.5) {
        vec2 center = vec2(0.5, 0.5);
        vec2 offset = texCoord - center;
        blurUV = center - offset * 0.3 + offset;
    }

    vec4 blur = texture(BlurSampler, blurUV);

    float edge = getEdge(texCoord);

    vec3 glassColor = blur.rgb;

    if (liquidGlass > 0.5) {
        vec2 center = vec2(0.5, 0.5);
        vec2 lensDir = normalize(texCoord - center + vec2(0.0001));
        float depthStrength = pow(edge, 1.45) * 0.018;
        vec2 depthOffset = lensDir * depthStrength;
        vec2 refractUV = clamp(blurUV + depthOffset, vec2(0.001), vec2(0.999));

        vec3 refracted = texture(BlurSampler, refractUV).rgb;
        float chroma = pow(edge, 1.35) * 0.008;
        float cr = texture(BlurSampler, clamp(refractUV + lensDir * chroma, vec2(0.001), vec2(0.999))).r;
        float cg = texture(BlurSampler, refractUV).g;
        float cb = texture(BlurSampler, clamp(refractUV - lensDir * chroma, vec2(0.001), vec2(0.999))).b;

        glassColor = mix(blur.rgb, refracted, 0.65);
        glassColor = mix(glassColor, vec3(cr, cg, cb), pow(edge, 1.7) * 0.75);
        glassColor = adjustSaturation(glassColor, saturation);
        glassColor = mix(glassColor, tintColor.rgb, tintIntensity * tintColor.a);
        glassColor += vec3(0.88, 0.94, 1.0) * pow(edge, 2.2) * (0.16 + edgeGlowIntensity);
        glassColor = pow(clamp(glassColor, vec3(0.0), vec3(1.0)), vec3(0.94));
        fragColor = vec4(glassColor, 1.0);
        return;
    }

    glassColor = adjustSaturation(glassColor, saturation);

    if (tintIntensity > 0.001) {
        glassColor = mix(glassColor, tintColor.rgb, tintIntensity);
    }

    if (edgeGlowIntensity > 0.001) {
        vec3 glowColor = tintColor.rgb;
        if (tintColor.a < 0.01) {
            glowColor = vec3(1.0);
        }
        glassColor += edge * glowColor * edgeGlowIntensity;
    }

    float fresnel = pow(edge, 2.0) * 0.3;
    glassColor += fresnel * 0.1;

    glassColor = clamp(glassColor, vec3(0.0), vec3(1.0));

    fragColor = vec4(glassColor, 1.0);
}
