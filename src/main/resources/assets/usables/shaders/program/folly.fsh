#version 150

in vec2 texCoord;
uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform vec2 OutSize;
out vec4 fragColor;

const vec3 TINT_COLOR = vec3(1.0, 0.0, 0.31);
const float TINT_INTENSITY = 0.45;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    vec3 tinted = mix(color.rgb, color.rgb * TINT_COLOR, TINT_INTENSITY);
    fragColor = vec4(tinted, color.a);
}