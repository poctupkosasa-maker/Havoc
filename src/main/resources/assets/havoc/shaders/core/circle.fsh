#version 150

#moj_import <minecraft:dynamictransforms.glsl>

in vec2 FragCoord;
in vec4 FragColor;
in vec2 Size;
in float Smoothness;

out vec4 fragColor;

void main() {
    vec2 uv = FragCoord * 2.0 - 1.0;
    float r = length(uv);
    float aa = max(Smoothness / max(Size.x, 1.0), 0.0001);
    float alpha = 1.0 - smoothstep(1.0 - aa, 1.0 + aa, r);
    if (alpha <= 0.0) {
        discard;
    }

    vec4 color = ColorModulator * FragColor;
    color.a *= alpha;
    if (color.a <= 0.0) {
        discard;
    }
    fragColor = color;
}

