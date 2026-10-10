#version 150

in vec2 vPixel;
in vec2 vRectSize;
in vec4 vRadii;
in vec4 vColorA;
in vec4 vColorB;
in float vTileSize;
in float vGuiScale;

out vec4 fragColor;

float roundedBoxSDF(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.yz : r.xw;
    r.x = (p.y > 0.0) ? r.y : r.x;

    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

void main() {
    vec2 halfSize = vRectSize * 0.5;
    vec2 center = vPixel - halfSize;
    float maxRadius = min(halfSize.x, halfSize.y);
    vec4 radii = min(vRadii, vec4(maxRadius));

    float dist = roundedBoxSDF(center, halfSize, radii);
    float smoothing = max(fwidth(dist), 0.5 / max(vGuiScale, 0.001));
    float alpha = 1.0 - smoothstep(-smoothing, smoothing, dist);

    if (alpha < 0.01) {
        discard;
    }

    vec2 cell = floor(vPixel / max(vTileSize, 0.001));
    float parity = mod(cell.x + cell.y, 2.0);
    vec4 color = mix(vColorA, vColorB, parity);

    fragColor = vec4(color.rgb, color.a * alpha);
}
