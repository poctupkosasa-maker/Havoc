#version 150

#moj_import <minecraft:dynamictransforms.glsl>

in vec2 FragCoord;
in vec4 FragColor;
in vec2 Size;
in float Progress;
in float Thickness;
in float Smoothness;

out vec4 fragColor;

const float PI = 3.14159265358979323846;
const float TWO_PI = 6.28318530717958647692;

void main() {
    vec2 p = FragCoord * 2.0 - 1.0;
    float r = length(p);

    float diameter = max(Size.x, 1.0);
    float aa = max(Smoothness / max(diameter, 1.0), 0.0001);
    float outerDisk = 1.0 - smoothstep(1.0 - aa, 1.0 + aa, r);
    float ringThickness = clamp(Thickness, 0.001, 0.999);
    float innerRadius = max(0.0, 1.0 - ringThickness);
    float innerDisk = 1.0 - smoothstep(innerRadius - aa, innerRadius + aa, r);
    float ring = clamp(outerDisk - innerDisk, 0.0, 1.0);
    if (ring <= 0.0) discard;

    float theta = atan(p.y, p.x);
    float t = mod((theta + PI * 0.5) / TWO_PI + 1.0, 1.0);
    float pClamped = clamp(Progress, 0.0, 1.0);

    float arc = step(t, pClamped);
    if (pClamped >= 0.9999) {
        arc = 1.0;
    }

    float alpha = ring * arc;
    if (alpha <= 0.0) discard;

    vec4 color = ColorModulator * FragColor;
    color.a *= alpha;
    if (color.a <= 0.0) discard;
    fragColor = color;
}
