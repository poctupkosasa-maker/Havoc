








vec4 kawaseDownsample(sampler2D src, vec2 uv, vec2 texel, float offset) {
    vec2 hp = texel * 0.5 * offset;
    vec4 sum = texture(src, uv) * 4.0;
    sum += texture(src, uv - hp);
    sum += texture(src, uv + hp);
    sum += texture(src, uv + vec2(hp.x, -hp.y));
    sum += texture(src, uv - vec2(hp.x, -hp.y));
    return sum / 8.0;
}

vec4 kawaseUpsample(sampler2D src, vec2 uv, vec2 texel, float offset) {
    return kawaseDownsample(src, uv, texel, offset); 
}





vec4 gaussianBlur9(sampler2D src, vec2 uv, vec2 direction) {
    const float w0 = 0.227027;
    const float w1 = 0.1945946;
    const float w2 = 0.1216216;
    const float w3 = 0.054054;
    const float w4 = 0.016216;

    vec4 c = texture(src, uv) * w0;
    c += texture(src, uv + direction * 1.0) * w1;
    c += texture(src, uv - direction * 1.0) * w1;
    c += texture(src, uv + direction * 2.0) * w2;
    c += texture(src, uv - direction * 2.0) * w2;
    c += texture(src, uv + direction * 3.0) * w3;
    c += texture(src, uv - direction * 3.0) * w3;
    c += texture(src, uv + direction * 4.0) * w4;
    c += texture(src, uv - direction * 4.0) * w4;
    return c;
}


vec4 boxBlur3(sampler2D src, vec2 uv, vec2 texel) {
    vec4 c = vec4(0.0);
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            c += texture(src, uv + vec2(float(x), float(y)) * texel);
        }
    }
    return c / 9.0;
}



vec4 radialBlur(sampler2D src, vec2 uv, vec2 origin, float strength) {
    vec2 d = (origin - uv) * (strength / 16.0);
    vec4 c = vec4(0.0);
    vec2 sample_uv = uv;
    for (int i = 0; i < 16; i++) {
        c += texture(src, sample_uv);
        sample_uv += d;
    }
    return c / 16.0;
}



vec4 brightPass(vec4 c, float threshold, float knee) {
    float l = max(c.r, max(c.g, c.b));
    float soft = clamp(l - threshold + knee, 0.0, 2.0 * knee);
    soft = soft * soft / max(4.0 * knee, 1e-4);
    float weight = max(soft, max(l - threshold, 0.0)) / max(l, 1e-4);
    return c * weight;
}




