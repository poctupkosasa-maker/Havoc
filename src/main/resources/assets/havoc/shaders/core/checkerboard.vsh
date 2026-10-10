#version 150

layout(std140) uniform CheckerboardData {
    vec4 rect;
    vec4 screen;
    vec4 radii;
    vec4 colorA;
    vec4 colorB;
    vec4 params;
};

out vec2 vPixel;
out vec2 vRectSize;
out vec4 vRadii;
out vec4 vColorA;
out vec4 vColorB;
out float vTileSize;
out float vGuiScale;

void main() {
    vec2 positions[6] = vec2[](
        vec2(0.0, 0.0),
        vec2(1.0, 0.0),
        vec2(1.0, 1.0),
        vec2(0.0, 0.0),
        vec2(1.0, 1.0),
        vec2(0.0, 1.0)
    );

    vec2 pos = positions[gl_VertexID];
    vec2 screenPos = rect.xy + pos * rect.zw;
    vec2 ndc = (screenPos / screen.xy) * 2.0 - 1.0;
    ndc.y = -ndc.y;

    gl_Position = vec4(ndc, 0.0, 1.0);

    vPixel = pos * rect.zw;
    vRectSize = rect.zw;
    vRadii = radii;
    vColorA = colorA;
    vColorB = colorB;
    vTileSize = params.x;
    vGuiScale = screen.z;
}
