#version 130

in vec2 texCoord;

uniform sampler2D uSampler;

void main() {
    float gamma = 1.0 / 2.2;

    vec4 final_color = texture2D(uSampler, texCoord);

    gl_FragColor = vec4(
        pow(final_color.r, gamma),
        pow(final_color.g, gamma),
        pow(final_color.b, gamma), 1);
}
