#version 150

in vec2 texCoord;

out vec4 fragColor;

uniform sampler2D SceneSampler;

layout(std140) uniform EffectData {
    vec4 settings;
};

void main() {
    vec4 color = texture(SceneSampler, texCoord);
    float saturation = max(settings.x, 0.0);
    float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    vec3 saturated = mix(vec3(gray), color.rgb, saturation);
    fragColor = vec4(clamp(saturated, 0.0, 1.0), color.a);
}
