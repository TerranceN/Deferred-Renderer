#version 130

in vec2 texCoord;
in vec3 normal;
in float depth;

uniform sampler2D uDiffuseSampler;
uniform vec4 uDiffuseColor;
uniform float uDiffuseTexture;

uniform sampler2D uSpecularSampler;
uniform vec4 uSpecularColor;
uniform float uSpecularTexture;

uniform float uShininess;

uniform float uFarDistance;

void main() {
    vec4 diffuse = vec4(1.0);
    if (uDiffuseTexture > 0.5) {
        diffuse = texture2D(uDiffuseSampler, texCoord);
    } else {
        diffuse = uDiffuseColor;
    }

    vec4 specular = vec4(1.0);
    if (uSpecularTexture > 0.5) {
        specular = texture2D(uSpecularSampler, texCoord);
    } else {
        specular = uSpecularColor;
    }

    gl_FragData[0] = diffuse;
    gl_FragData[1] = vec4(normal, depth);
    gl_FragData[2] = vec4(specular.xyz, uShininess);
}
