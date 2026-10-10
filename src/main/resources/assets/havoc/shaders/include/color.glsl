






vec3 srgbToLinear(vec3 srgb) { return pow(srgb, vec3(2.2)); }
vec3 linearToSrgb(vec3 lin)  { return pow(lin,  vec3(1.0 / 2.2)); }

vec4 srgbToLinear(vec4 c)    { return vec4(srgbToLinear(c.rgb), c.a); }
vec4 linearToSrgb(vec4 c)    { return vec4(linearToSrgb(c.rgb), c.a); }


float luma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

vec3 saturation(vec3 c, float s) {
    float l = luma(c);
    return mix(vec3(l), c, s);
}

vec3 contrast(vec3 c, float k) {
    return (c - 0.5) * k + 0.5;
}

vec3 brightness(vec3 c, float b) {
    return c + b;
}


vec3 hsvToRgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0/3.0, 1.0/3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

vec3 rgbToHsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0/3.0, 2.0/3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)),
                d / (q.x + e),
                q.x);
}



vec3 tonemapACES(vec3 x) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}


vec3 tonemapReinhard(vec3 x) { return x / (1.0 + x); }



float vignetteAlpha(vec2 uv, float strength, float smoothness) {
    vec2 d = uv - 0.5;
    float r = length(d) * 1.4142135;       
    return mix(1.0, 1.0 - r, clamp(strength, 0.0, 1.0))
         * smoothstep(0.0, smoothness, 1.0 - r);
}



float bayerDither(vec2 fragCoord) {
    return fract(sin(dot(fragCoord, vec2(12.9898, 78.233))) * 43758.5453) / 255.0;
}




