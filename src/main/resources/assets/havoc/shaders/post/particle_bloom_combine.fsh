#version 330

uniform sampler2D MainSampler;
uniform sampler2D BloomSampler;

layout(std140) uniform BloomConfig {
    vec4 Params;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec3 mainColor = texture(MainSampler, texCoord).rgb;
    vec3 bloom = texture(BloomSampler, texCoord).rgb;
    float intensity = Params.x;
    fragColor = vec4(mainColor + bloom * intensity, 1.0);
}
