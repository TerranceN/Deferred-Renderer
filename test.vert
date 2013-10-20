#version 130

in vec3 aCoord;
in vec3 aNormal;
in vec2 aTexCoord;

out vec2 texCoord;
out vec3 normal;

out vec3 lightDir;
out vec3 eyeVec;
out float distanceToLight;

uniform vec2 uMousePos;

void main() {
    float PI = 3.14159265358979323846264;
    // idealy these shouldn't be hard coded, but for now it is
    float lightDistance = 5;
    float hAngle = 90;
    float vAngle = 59;
    float x = tan((hAngle * PI / 180) / 2) * 2 * lightDistance;
    float y = tan((vAngle * PI / 180) / 2) * 2 * lightDistance;
    vec2 mouseGradient = uMousePos / vec2(1280, 720) * 2 - vec2(1);
    vec3 lightPosition = vec3(mouseGradient * vec2(x, y), -lightDistance);

    vec4 transformedCoord = gl_ModelViewMatrix * vec4(aCoord, 1.0);
    vec4 finishedCoord = gl_ProjectionMatrix * transformedCoord;

    distanceToLight = length(lightPosition - transformedCoord.xyz);
    texCoord = aTexCoord;
    normal = gl_NormalMatrix * aNormal;
    eyeVec = finishedCoord.xyz;
    lightDir = lightPosition - transformedCoord.xyz;

    gl_Position = finishedCoord;
}
