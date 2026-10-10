#version 150

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <havoc:noise.glsl>

in vec2 FragCoord;
in vec4 FragColor;

out vec4 fragColor;

// Цель: энергетическое поле TargetESP для полос/колец/квадов.
// u — вдоль полосы, v — поперёк толщины. Мягкие края по толщине,
// бегущие полосы, лёгкая ячеистая модуляция и зерно.

void main() {
    float u = FragCoord.x;
    float v = FragCoord.y;

    // мягкие края поперёк толщины
    float edge = smoothstep(0.0, 0.30, v) * (1.0 - smoothstep(0.70, 1.0, v));

    // бегущие полосы вдоль (фаза приходит из CPU-запечённого u)
    float bands = 0.62 + 0.38 * sin(u * 39.4784176);   // 6 полос на круг

    // лёгкая ячеистая модуляция: поле не однородное
    float energy = 0.90 + 0.10 * sin((u * 11.0 + v * 7.0) * 3.14159265);

    // зерно, чтобы убрать «пластиковую» плоскость
    float grain = 0.92 + 0.08 * hash12(vec2(u * 64.0, v * 64.0));

    float alpha = edge * bands * energy * grain;
    if (alpha <= 0.004) {
        discard;
    }

    vec4 color = ColorModulator * vec4(FragColor.rgb, FragColor.a * alpha);
    if (color.a <= 0.0) {
        discard;
    }
    fragColor = color;
}
