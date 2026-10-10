#version 150

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec2 UV1;
in vec2 RadiusXY;
in vec2 RadiusZW;
in vec2 ExtraData1;
in vec2 ExtraData2;

out vec2 FragCoord;
out vec4 FragColor;
out vec2 Size;
out float Progress;
out float Thickness;
out float Smoothness;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    FragCoord = UV0;
    FragColor = Color;
    Size = UV1;
    Progress = ExtraData1.x;
    Thickness = ExtraData1.y;
    Smoothness = ExtraData2.x;
}
