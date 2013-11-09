#version 130

in vec3 aCoord;
in vec2 aTexCoord;

out vec2 texCoord;
out vec3 eyeVec;

uniform mat4 uProjectionMatrix;
uniform mat4 uModelViewMatrix;

uniform float uNearDistance;

void main() {
    vec4 transformedCoord = uModelViewMatrix * vec4(aCoord, 1.0);
    vec4 finishedCoord = uProjectionMatrix * transformedCoord;

    eyeVec = finishedCoord.xyz;

    texCoord = aTexCoord;
    gl_Position = finishedCoord;
}
