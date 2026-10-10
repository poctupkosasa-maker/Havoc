#version 150

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <havoc:noise.glsl>

in vec2 FragCoord;
in vec4 FragColor;

out vec4 fragColor;

// Цель: энергетическая частица TargetESP.
// Ядро + ободок + живая зернистость, мягкое затухание к краю квада,
// отсечение по альфе. Цвет приходит из вершин (палитра клиента и её альфа).

void main() {
    vec2 uv = FragCoord;
    vec2 p = uv * 2.0 - 1.0;
    float r = length(p);

    // ядро частицы
    float core = 1.0 - smoothstep(0.0, 0.95, r);
    core = pow(core, 1.7);

    // энергетический ободок на краю
    float rim = smoothstep(0.55, 0.80, r) * (1.0 - smoothstep(0.86, 1.05, r));
    rim *= 0.85;

    // живая зернистость: частица не выглядит пластиковой
    float grain = fbm(uv * 6.0 + r * 4.0);
    float detail = mix(0.85, 1.15, grain);

    float alpha = clamp((core + rim) * detail, 0.0, 1.0);
    if (alpha <= 0.004) {
        discard;
    }

    vec4 color = ColorModulator * vec4(FragColor.rgb, FragColor.a * alpha);
    if (color.a <= 0.0) {
        discard;
    }
    fragColor = color;
}
