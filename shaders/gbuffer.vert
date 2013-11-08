#version 130

in vec3 aCoord;
in vec3 aNormal;
in vec2 aTexCoord;

out vec2 texCoord;
out vec3 normal;
out float depth;
out vec3 eyeVec;

uniform float uFarDistance;

void main() {
    vec4 transformedCoord = gl_ModelViewMatrix * vec4(aCoord, 1.0);
    vec4 finishedCoord = gl_ProjectionMatrix * transformedCoord;

    texCoord = aTexCoord;

    normal = gl_NormalMatrix * aNormal;

    eyeVec = finishedCoord;

    depth = -transformedCoord.z / uFarDistance;

    gl_Position = finishedCoord;
}
