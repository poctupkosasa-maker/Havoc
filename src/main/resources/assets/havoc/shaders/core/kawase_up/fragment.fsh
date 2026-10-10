#version 150

in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;
uniform vec2 Resolution;
uniform float Offset;

out vec4 OutColor;

void main() {
    vec2 texel = Resolution;
    float off = Offset + 0.5;

    vec2 d = texel * off * 1.5;
    vec2 o1 = vec2(d.x, d.y);
    vec2 o2 = vec2(-d.x, d.y);
    vec2 o3 = vec2(d.x, -d.y);
    vec2 o4 = vec2(-d.x, -d.y);

    vec3 sum = texture(Sampler0, TexCoord + o1).rgb;
    sum += texture(Sampler0, TexCoord + o2).rgb;
    sum += texture(Sampler0, TexCoord + o3).rgb;
    sum += texture(Sampler0, TexCoord + o4).rgb;

    OutColor = vec4(sum * 0.25, 1.0) * FragColor;
}
