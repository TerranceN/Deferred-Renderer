#version 130

const int MAX_LIGHTS = 10;

in vec2 texCoord;
in vec3 normal;

in vec3 eyeVec;

in vec3 lightDir[MAX_LIGHTS];
in float distanceToLight[MAX_LIGHTS];

uniform int uNumLights;
uniform vec3 uLightIrradiance[MAX_LIGHTS];

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

    vec3 v = normalize(eyeVec);
    vec3 n = normalize(normal);

    float lightsToRender = min(uNumLights, MAX_LIGHTS);
    for (int i = 0; i < lightsToRender; i++) {
        float att = 1.0 / (attConst+(attLinear*distanceToLight[i])+(attQuad*distanceToLight[i]*distanceToLight[i]));

        vec3 lDir = normalize(lightDir[i]);
        vec3 h = normalize(v + lDir);
        float cosTh = clamp(dot(n, h), 0, 1);
        float cosTi = clamp(dot(n, lDir), 0, 1);
        final_color += (diffuse + specular * pow(cosTh, uShininess)) * vec4(uLightIrradiance[i], 1) * cosTi * att;

        final_color += diffuse * ambientIntensity * att * vec4(uLightIrradiance[i], 1);
    }

    gl_FragColor = final_color;
}
