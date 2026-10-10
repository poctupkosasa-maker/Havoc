#version 150

layout(std140) uniform LumaSkyData {
    vec4 cameraAndTime;  
    vec4 themeColor;     
    vec4 skySettings;    
};

out vec3 worldDir;       
out float time;
out vec3 vThemeColor;    
out float skyBrightness;
out float skyBlur;

void main() {
    
    vec2 positions[6] = vec2[](
        vec2(-1.0, -1.0),
        vec2( 1.0, -1.0),
        vec2( 1.0,  1.0),
        vec2(-1.0, -1.0),
        vec2( 1.0,  1.0),
        vec2(-1.0,  1.0)
    );
    vec2 ndc = positions[gl_VertexID];
    gl_Position = vec4(ndc, 0.0, 1.0);

    



    float aspect = cameraAndTime.w;
    float yaw = cameraAndTime.x;
    float pitch = cameraAndTime.y;
    float cy = cos(yaw);
    float sy = sin(yaw);
    float cp = cos(pitch);
    float sp = sin(pitch);

    vec3 forward = normalize(vec3(sy * cp, -sp, cy * cp));
    vec3 right = normalize(vec3(-cy, 0.0, sy));
    vec3 up = normalize(cross(right, forward));
    worldDir = normalize(forward + right * (ndc.x * aspect) + up * ndc.y);

    time = cameraAndTime.z;
    vThemeColor = themeColor.rgb;
    skyBrightness = skySettings.x;
    skyBlur = skySettings.y;
}
