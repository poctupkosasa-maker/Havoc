



































layout(std140) uniform PostFrame {
    vec4 PostResolution;   
    vec4 PostTime;         
    vec4 PostParams0;      
    vec4 PostParams1;      
    vec4 PostParams2;      
    vec4 PostParams3;      
};




const vec2 LUMA_QUAD_POS[6] = vec2[](
    vec2(-1.0, -1.0),
    vec2( 1.0, -1.0),
    vec2( 1.0,  1.0),
    vec2(-1.0, -1.0),
    vec2( 1.0,  1.0),
    vec2(-1.0,  1.0)
);
const vec2 LUMA_QUAD_UV[6] = vec2[](
    vec2(0.0, 0.0),
    vec2(1.0, 0.0),
    vec2(1.0, 1.0),
    vec2(0.0, 0.0),
    vec2(1.0, 1.0),
    vec2(0.0, 1.0)
);

vec4 postQuadPos() {
    return vec4(LUMA_QUAD_POS[gl_VertexID], 0.0, 1.0);
}

vec2 postQuadUv() {
    return LUMA_QUAD_UV[gl_VertexID];
}

vec2 postResolution() { return PostResolution.xy; }
vec2 postTexelSize()  { return PostResolution.zw; }
float postTime()      { return PostTime.x; }
float postFrame()     { return PostTime.y; }


vec2 postClampUv(vec2 uv) {
    return clamp(uv, vec2(0.0005), vec2(0.9995));
}




