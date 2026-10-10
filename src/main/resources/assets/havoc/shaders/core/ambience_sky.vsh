#version 150

layout(std140) uniform AmbienceSkyData {
    vec4 cameraAndTime;
    mat4 inverseViewProjection;
    vec4 themeColor;
    vec4 themeColor2;
    vec4 settings1;
    vec4 settings2;
    vec4 settings3;
    vec4 settings4;
};

out vec3 worldDir;
out float shaderTime;
out vec3 colorA;
out vec3 colorB;
out vec4 skySettings1;
out vec4 skySettings2;
out vec4 skySettings3;
out vec2 screenUV;
out vec4 weatherSettings;

void main() {
    vec2 positions[6] = vec2[](
        vec2(-1.0, -1.0), vec2(1.0, -1.0), vec2(1.0, 1.0),
        vec2(-1.0, -1.0), vec2(1.0, 1.0), vec2(-1.0, 1.0)
    );
    vec2 ndc = positions[gl_VertexID];
    gl_Position = vec4(ndc, 0.0, 1.0);

    vec4 nearPoint = inverseViewProjection * vec4(ndc, -1.0, 1.0);
    vec4 farPoint = inverseViewProjection * vec4(ndc, 1.0, 1.0);
    float nearW = abs(nearPoint.w) < 0.00001 ? (nearPoint.w < 0.0 ? -0.00001 : 0.00001) : nearPoint.w;
    float farW = abs(farPoint.w) < 0.00001 ? (farPoint.w < 0.0 ? -0.00001 : 0.00001) : farPoint.w;
    nearPoint /= nearW;
    farPoint /= farW;
    worldDir = normalize(farPoint.xyz - nearPoint.xyz);
    shaderTime = cameraAndTime.x;
    colorA = themeColor.rgb;
    colorB = themeColor2.rgb;
    skySettings1 = settings1;
    skySettings2 = settings2;
    skySettings3 = settings3;
    screenUV = ndc * 0.5 + 0.5;
    weatherSettings = settings4;
}
