#version 140

const int MAX_LIGHTS = 10;
const float PI = 3.1415926;

in vec2 texCoord;
in vec3 eyeVec;

uniform int uNumLights;

uniform vec3 uLightPositions[MAX_LIGHTS];
uniform vec3 uLightIrradiance[MAX_LIGHTS];

uniform sampler2D uDiffuseSampler;
uniform sampler2D uNormalsDepthSampler;
uniform sampler2D uSpecularSampler;
uniform sampler2D uPreviousLightingSampler;
uniform sampler2D uAmbientOcclusionSampler;

uniform mat4 u3dProjectionMatrix;

uniform mat4 uModelViewMatrix;

uniform float uFarDistance;
uniform float uNearDistance;

vec3 positionFromDepth(float z) {
    vec4 clip = vec4(eyeVec.xyz, 1) * -z;
    return (inverse(u3dProjectionMatrix) * clip).xyz;
}

void main() {
    float ambientIntensity = 0.5;

    vec4 diffuse = texture2D(uDiffuseSampler, texCoord);
    vec4 specular = vec4(texture2D(uSpecularSampler, texCoord).xyz, 1);
    float uShininess = texture2D(uSpecularSampler, texCoord).a;

    vec4 final_color = vec4(0);

    // use this to mess around with att values: https://www.desmos.com/calculator/nmnaud1hrw
    float attConst = 1;
    float attLinear = 2;
    float attQuad = 1;

    float fractionalDepth = texture2D(uNormalsDepthSampler, texCoord).a;
    float depth = -(uNearDistance + fractionalDepth * (uFarDistance - uNearDistance));

    vec3 fragmentPosition = positionFromDepth(depth);
    vec3 eye = normalize(fragmentPosition);

    vec3 n = normalize(texture2D(uNormalsDepthSampler, texCoord).xyz);

    float lightsToRender = min(uNumLights, MAX_LIGHTS);
    for (int i = 0; i < lightsToRender; i++) {
        vec3 difference = uLightPositions[i] - fragmentPosition;
        float distanceToLight = length(difference);
        float att = 1.0 / (attConst+(attLinear*distanceToLight)+(attQuad*distanceToLight*distanceToLight));

        vec3 lDir = normalize(difference);
        vec3 h = normalize(eye - lDir);
        float cosTh = clamp(-dot(n, h), 0, 1);
        float cosTi = clamp(dot(n, lDir), 0, 1);
        float specularIntensity = pow(cosTh, uShininess) * (uShininess + 2.0) / 8.0;
        vec4 lightingColors = diffuse + specular * specularIntensity;
        final_color += lightingColors * vec4(uLightIrradiance[i], 1) * cosTi * att;

        float ambientOcclusionStrength = 8;
        final_color +=  diffuse * pow(texture2D(uAmbientOcclusionSampler, texCoord).r, ambientOcclusionStrength) * ambientIntensity * att * vec4(uLightIrradiance[i], 1);

        vec3 lightPosition = (uModelViewMatrix * vec4(uLightPositions[i], 1)).xyz;
        float lightDepth = lightPosition.z;

        if (lightDepth > depth) {
            float lightDirect = pow(clamp(dot(eye, normalize(lightPosition)), 0, 1), 10000);
            if (lightDirect > 0.1) {
                final_color += vec4(uLightIrradiance[i], 1);
            }
        }
    }

    final_color += texture2D(uPreviousLightingSampler, texCoord);

    gl_FragData[0] = final_color;
}
