#version 130

in vec2 texCoord;

uniform sampler2D uDiffuseSampler;
uniform vec4 uDiffuseColor;
uniform float uDiffuseTexture;

void main() {
    vec4 diffuse = vec4(1.0);
    if (uDiffuseTexture > 0.5) {
        diffuse = texture2D(uDiffuseSampler, texCoord);
    } else {
        diffuse = uDiffuseColor;
    }

    gl_FragColor = diffuse;
}
