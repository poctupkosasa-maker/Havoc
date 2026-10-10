#version 150

#moj_import <minecraft:dynamictransforms.glsl>

in vec2 FragCoord;
in vec4 FragColor;
in vec2 Size;
in vec4 Radius;
in float Smoothness;
in float Spread;

out vec4 fragColor;

float sdRoundBox(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x = (p.y > 0.0) ? r.x : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

void main() {
    vec2 pos = FragCoord * Size;
    vec2 center = Size * 0.5;
    vec2 innerSize = max(Size - vec2(Spread * 2.0), vec2(0.0));
    vec2 innerHalf = max(innerSize * 0.5 - 1.0, vec2(0.0));

    float dist = sdRoundBox(center - pos, innerHalf, Radius);
    float safeSmoothness = max(Smoothness, 0.0001);
    float safeSpread = max(Spread, 0.0001);
    float outsideDist = max(dist, 0.0);

    // Keep a tiny dead zone near the panel edge so large spread doesn't visually merge with blur.
    float startOffset = min(safeSpread * 0.12, 1.25);
    float shadowDist = max(outsideDist - startOffset, 0.0);
    float shadowRange = max(safeSpread - startOffset, 0.0001);

    float edgeMask = smoothstep(0.0, safeSmoothness, shadowDist);
    float normalized = clamp(shadowDist / shadowRange, 0.0, 1.0);
    float decay = pow(1.0 - normalized, 1.75);
    float alpha = edgeMask * decay;

    vec4 color = ColorModulator * vec4(FragColor.rgb, FragColor.a * alpha);
    if (color.a <= 0.0) discard;

    fragColor = color;
}
