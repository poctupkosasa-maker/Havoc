#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;

layout(std140) uniform BlockOverlayData {
    vec4 cameraAndTime;
    vec4 themeAndBrightness;
    vec4 shaderSettings;
    vec4 boundsMin;
    vec4 boundsSize;
};

out vec3 worldPos;
out float time;
out vec3 themeColor;
out float shaderBrightness;
out float shaderAlpha;
out vec3 overlayBoundsMin;
out vec3 overlayBoundsSize;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    worldPos = Position + cameraAndTime.xyz;
    time = cameraAndTime.w;
    themeColor = themeAndBrightness.rgb;
    shaderBrightness = themeAndBrightness.w;
    shaderAlpha = shaderSettings.x;
    overlayBoundsMin = boundsMin.xyz;
    overlayBoundsSize = max(boundsSize.xyz, vec3(0.001));
}
