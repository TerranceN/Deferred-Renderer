#version 130

in vec3 aCoord;
in vec3 aNormal;

out vec3 normal;

void main() {
    vec4 transformedCoord = gl_ModelViewMatrix * vec4(aCoord, 1.0);
    vec4 finishedCoord = gl_ProjectionMatrix * transformedCoord;

    normal = gl_NormalMatrix * aNormal;

    gl_Position = finishedCoord;
}
