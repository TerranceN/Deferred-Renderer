#version 130

in vec2 texCoord;
in vec3 normal;

in vec3 lightDir;
in vec3 eyeVec;
in float distanceToLight;

uniform sampler2D uDiffuseSampler;
uniform vec4 uDiffuseColor;
uniform float uDiffuseTexture;

uniform sampler2D uSpecularSampler;
uniform vec4 uSpecularColor;
uniform float uSpecularTexture;

uniform float uShininess;

void main() {
    float ambientIntensity = 0.1;

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

    vec4 final_color = vec4(0);

    // use this to mess around with att values: https://www.desmos.com/calculator/nmnaud1hrw
    float attConst = 1.0;
    float attLinear = 0.0;
    float attQuad = 0.05;

    float att = 1.0 / (attConst+(attLinear*distanceToLight)+(attQuad*distanceToLight*distanceToLight));

    vec3 v = normalize(eyeVec);
    vec3 n = normalize(normal);
    vec3 lDir = normalize(lightDir);

    // for each light source
    vec3 h = normalize(v + lDir);
    float cosTh = clamp(dot(n, h), 0, 1);
    float cosTi = clamp(dot(n, lDir), 0, 1);
    final_color += (diffuse + specular * pow(cosTh, uShininess)) * 0.5 * cosTi;

    gl_FragColor = final_color;
}
