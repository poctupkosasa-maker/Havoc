#version 150

#moj_import <minecraft:dynamictransforms.glsl>

in vec2 FragCoord;
in vec4 FragColor;
in vec2 Size;
in vec4 Radius;
in float Smoothness;
in float Time;
out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}
float noise(vec2 p) {
    vec2 i=floor(p), f=fract(p); f=f*f*(3.0-2.0*f);
    return mix(mix(hash(i),hash(i+vec2(1,0)),f.x),
               mix(hash(i+vec2(0,1)),hash(i+vec2(1,1)),f.x),f.y);
}
float fbm(vec2 p) {
    float v=0.0,a=0.5;
    for(int i=0;i<5;i++){v+=noise(p)*a;p*=2.0;a*=0.5;}
    return v;
}
float sdRoundBox(vec2 p, vec2 b, vec4 r) {
    r.xy=(p.x>0.0)?r.xy:r.zw; r.x=(p.y>0.0)?r.x:r.y;
    vec2 q=abs(p)-b+r.x;
    return min(max(q.x,q.y),0.0)+length(max(q,0.0))-r.x;
}
void main() {
    vec2 p=FragCoord*3.0;
    float randomOffset=fbm(vec2(Time*.1,Time*.23))*3.0;
    float n=fbm(p+randomOffset+Time*.3);
    float t=smoothstep(0.0,1.0,Time);
    float threshold=mix(1.0,0.0,t);
    float mask=step(threshold,n);
    float edge=1.0-smoothstep(0.0,.05,abs(n-threshold));
    vec2 center=Size*.5;
    float shape=1.0-smoothstep(1.0-Smoothness,1.0,
        sdRoundBox(center-FragCoord*Size,center-1.0,Radius));
    vec3 outline=mix(FragColor.rgb,vec3(1.0),.28);
    vec3 rgb=FragColor.rgb*mask+outline*edge;
    float alpha=(FragColor.a*mask+edge*.92)*shape;
    if(alpha<=0.001)discard;
    fragColor=vec4(rgb,alpha)*ColorModulator;
}
