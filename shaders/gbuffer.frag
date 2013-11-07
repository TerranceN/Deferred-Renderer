#version 130

in vec2 texCoord;
in vec3 normal;
in float depth;

uniform sampler2D uDiffuseSampler;
uniform int uDiffuseNumComponents;
uniform vec4 uDiffuseColor;
uniform float uDiffuseTexture;

uniform sampler2D uSpecularSampler;
uniform int uSpecularNumComponents;
uniform vec4 uSpecularColor;
uniform float uSpecularTexture;

uniform float uShininess;

uniform float uFarDistance;

void main() {
    vec4 diffuse = vec4(1.0);
    if (uDiffuseTexture > 0.5) {
        diffuse = texture2D(uDiffuseSampler, texCoord);
        if (uDiffuseNumComponents == 3) {
            diffuse = vec4(diffuse.xyz, 1);
        }
        if (uDiffuseNumComponents < 3) {
            diffuse = vec4(vec3(diffuse.x), 1);
        }
    } else {
        diffuse = uDiffuseColor;
    }

    vec4 specular = vec4(1.0);
    if (uSpecularTexture > 0.5) {
        specular = texture2D(uSpecularSampler, texCoord);
        if (uSpecularNumComponents == 3) {
            specular = vec4(specular.xyz, 1);
        }
        if (uSpecularNumComponents < 3) {
            specular = vec4(vec3(specular.x), 1);
        }
    } else {
        specular = uSpecularColor;
    }

    gl_FragData[0] = diffuse;
    gl_FragData[1] = vec4(normal, depth);
    gl_FragData[2] = vec4(vec3(specular.xyz), uShininess);
}
