#version 130

const int MAX_LIGHTS = 10;

in vec3 aCoord;
in vec3 aNormal;
in vec2 aTexCoord;

uniform int uNumLights;
uniform vec3 uLightPositions[MAX_LIGHTS];

out vec2 texCoord;
out vec3 normal;

out vec3 eyeVec;

out vec3 lightDir[MAX_LIGHTS];
out float distanceToLight[MAX_LIGHTS];

void main() {
    vec4 transformedCoord = gl_ModelViewMatrix * vec4(aCoord, 1.0);
    vec4 finishedCoord = gl_ProjectionMatrix * transformedCoord;

    texCoord = aTexCoord;
    normal = gl_NormalMatrix * aNormal;
    eyeVec = finishedCoord.xyz;

    float lightsToRender = min(uNumLights, MAX_LIGHTS);
    for (int i = 0; i < lightsToRender; i++) {
        vec3 difference = uLightPositions[i] - transformedCoord.xyz;
        lightDir[i] = difference;
        distanceToLight[i] = length(difference);
    }

    gl_Position = finishedCoord;
}
