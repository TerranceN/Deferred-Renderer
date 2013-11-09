#version 140

in vec3 aCoord;
in vec3 aNormal;
in vec2 aTexCoord;

out vec2 texCoord;
out vec3 normal;
out float depth;
out vec3 eyeVec;

uniform mat4 uProjectionMatrix;
uniform mat4 uModelViewMatrix;

uniform float uFarDistance;

void main() {
    vec4 transformedCoord = uModelViewMatrix * vec4(aCoord, 1.0);
    vec4 finishedCoord = uProjectionMatrix * transformedCoord;

    texCoord = aTexCoord;

    normal = (inverse(transpose(uModelViewMatrix)) * vec4(aNormal, 0)).xyz;

    eyeVec = finishedCoord;

    depth = -transformedCoord.z / uFarDistance;

    gl_Position = finishedCoord;
}
