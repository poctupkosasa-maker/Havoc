#version 150

in vec2 fragCoord;     
in vec2 pixelCoord;    
in vec2 rectSize;
in float time;
in float globalAlpha;
in vec2 parallaxUV;

out vec4 fragColor;






float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float noise(vec2 x) {
    vec2 p = floor(x);
    vec2 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(p + vec2(0.0, 0.0)), hash(p + vec2(1.0, 0.0)), f.x),
               mix(hash(p + vec2(0.0, 1.0)), hash(p + vec2(1.0, 1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    float f = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 5; i++) {
        f += amp * noise(p);
        p = p * 2.01 + time * 0.04;
        amp *= 0.5;
    }
    return f;
}

void main() {
    




    vec2 baseUv = fragCoord;
    float aspect = max(rectSize.x / max(rectSize.y, 1.0), 0.0001);
    baseUv.x *= aspect;
    vec2 uv = baseUv + parallaxUV;

    





    vec2 smokeUv = uv * 1.5;
    smokeUv += vec2(time * 0.012, time * 0.006);
    smokeUv.x += sin(time * 0.30 + uv.y * 2.0) * 0.05;
    float n = fbm(smokeUv);

    
    
    
    float density = n * n;

    




    vec3 base      = vec3(0.020, 0.030, 0.060);
    vec3 deepNavy  = vec3(0.030, 0.080, 0.180);
    vec3 steelBlue = vec3(0.080, 0.230, 0.420);
    vec3 cyanHi    = vec3(0.300, 0.520, 0.720);

    vec3 col = base;
    col = mix(col, deepNavy,  smoothstep(0.10, 0.50, density));
    col = mix(col, steelBlue, smoothstep(0.30, 0.85, density));
    col += cyanHi * pow(density, 3.5) * 0.55;

    




    vec2 starUv = (baseUv + parallaxUV * 2.7) * 80.0;
    starUv += vec2(time * 0.18, time * 0.075);
    vec2 starId = floor(starUv);
    vec2 starGv = fract(starUv) - 0.5;
    float starHash = hash(starId);
    if (starHash > 0.985) {
        
        
        vec2 offset = vec2(
            (hash(starId + 1.0) - 0.5) * 0.7,
            (hash(starId + 2.0) - 0.5) * 0.7);

        float twinkle = 0.55 + 0.45 * sin(time * 2.0 + starHash * 50.0);
        float d = length(starGv - offset);
        
        float core = smoothstep(0.04, 0.0, d);
        float halo = smoothstep(0.16, 0.0, d) * 0.30;
        
        col += vec3(0.70, 0.85, 1.00) * (core + halo) * twinkle * 0.50;
    }

    



    vec2 vUv = fragCoord - 0.5;
    float vignette = 1.0 - dot(vUv, vUv) * 0.95;
    col *= clamp(vignette, 0.55, 1.0);

    fragColor = vec4(col, globalAlpha);
}
