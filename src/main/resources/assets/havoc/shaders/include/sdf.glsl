





float sdCircle(vec2 p, float r) {
    return length(p) - r;
}

float sdRect(vec2 p, vec2 halfSize) {
    vec2 d = abs(p) - halfSize;
    return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0);
}


float sdRoundRect(vec2 p, vec2 halfSize, vec4 r) {
    r.xy = (p.x > 0.0) ? r.yz : r.xw;
    r.x  = (p.y > 0.0) ? r.y : r.x;
    vec2 q = abs(p) - halfSize + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

float sdSegment(vec2 p, vec2 a, vec2 b, float thickness) {
    vec2 ab = b - a;
    vec2 ap = p - a;
    float h = clamp(dot(ap, ab) / dot(ab, ab), 0.0, 1.0);
    return length(ap - ab * h) - thickness;
}

float sdEquilateralTriangle(vec2 p, float r) {
    const float k = sqrt(3.0);
    p.x = abs(p.x) - r;
    p.y = p.y + r / k;
    if (p.x + k * p.y > 0.0) {
        p = vec2(p.x - k * p.y, -k * p.x - p.y) * 0.5;
    }
    p.x -= clamp(p.x, -2.0 * r, 0.0);
    return -length(p) * sign(p.y);
}



float sdAlpha(float dist, float pixelWidth, float softness) {
    float w = max(pixelWidth, 0.001) * softness;
    return 1.0 - smoothstep(-w, w, dist);
}


float sdOutlineAlpha(float dist, float halfThickness, float pixelWidth, float softness) {
    float w = max(pixelWidth, 0.001) * softness;
    return (1.0 - smoothstep(-w, w, dist - halfThickness))
         * smoothstep(-w, w, dist + halfThickness);
}




