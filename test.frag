#version 130

in vec2 texCoord;
in vec4 color;

uniform sampler2D uSampler;

void main() {
    vec4 texColor = texture2D(uSampler, texCoord);
    gl_FragColor = texColor * color;
}
