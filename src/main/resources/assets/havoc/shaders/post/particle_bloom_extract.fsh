#version 330

uniform sampler2D MainSampler;

layout(std140) uniform BloomFilter {
    vec4 MaskColor;
    vec4 BloomColor;
};

in vec2 texCoord;

out vec4 fragColor;

const float COLOR_HARD = 0.09;
const float COLOR_SOFT = 0.6;
const float EDGE_MIN = 0.015;
const float EDGE_MAX = 0.08;

void main() {
    vec4 color = texture(MainSampler, texCoord);
    float dist = distance(color.rgb, MaskColor.rgb);
    float colorMask = 1.0 - smoothstep(COLOR_HARD, COLOR_SOFT, dist);

    vec2 texel = 1.0 / vec2(textureSize(MainSampler, 0));
    vec3 left = texture(MainSampler, texCoord + vec2(-texel.x, 0.0)).rgb;
    vec3 right = texture(MainSampler, texCoord + vec2(texel.x, 0.0)).rgb;
    vec3 up = texture(MainSampler, texCoord + vec2(0.0, texel.y)).rgb;
    vec3 down = texture(MainSampler, texCoord + vec2(0.0, -texel.y)).rgb;

    float edge = length(right - left) + length(up - down);
    float edgeMask = smoothstep(EDGE_MIN, EDGE_MAX, edge);

    float mask = colorMask * edgeMask;
    vec3 bloom = BloomColor.rgb * mask;
    fragColor = vec4(bloom, mask);
}
