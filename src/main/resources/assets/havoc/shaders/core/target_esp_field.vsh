#version 150

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 FragCoord;
out vec4 FragColor;

void main() {
    // UV0.x — вдоль полосы/кольца (на CPU в него запекается фаза анимации),
    // UV0.y — поперёк толщины. Так полосы «бегут» без юниформов времени.
    FragCoord = UV0;
    FragColor = Color;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
