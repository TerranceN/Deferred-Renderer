#version 130

in vec3 aCoord;
in vec2 aTexCoord;

out vec2 texCoord;
out vec3 eyeVec;

void main() {
    vec4 transformedCoord = gl_ModelViewMatrix * vec4(aCoord, 1.0);
    vec4 finishedCoord = gl_ProjectionMatrix * transformedCoord;

    eyeVec = finishedCoord.xyz;

    texCoord = aTexCoord;
    gl_Position = finishedCoord;
}
