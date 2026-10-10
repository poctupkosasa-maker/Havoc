#version 150

layout(std140) uniform SpaceData {
    vec4 rect;          
    vec4 screen;        
    vec4 timeAndAlpha;  
};

out vec2 fragCoord;     
out vec2 pixelCoord;    
out vec2 rectSize;
out float time;
out float globalAlpha;
out vec2 parallaxUV;

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

    fragCoord = pos;
    pixelCoord = pos * rect.zw;
    rectSize = rect.zw;
    time = timeAndAlpha.x;
    globalAlpha = timeAndAlpha.y;
    parallaxUV = timeAndAlpha.zw;
}
