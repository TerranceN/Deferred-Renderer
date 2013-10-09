#version 130

in vec3 aCoord;
in vec4 aColor;
in vec3 aNormal;
in vec2 aTexCoord;

out vec2 texCoord;
out vec4 color;
out vec3 normal;

out vec3 lightDir;
out vec3 eyeVec;

uniform vec2 uMousePos;

void main() {
    vec3 lightPosition = vec3((uMousePos / vec2(1280, 720) * 2 - vec2(1)) * 10, 11.5);
    vec4 finishedCoord = gl_ModelViewProjectionMatrix * vec4(aCoord, 1.0);

    color = aColor;
    texCoord = aTexCoord;
    normal = gl_NormalMatrix * aNormal;
    eyeVec = finishedCoord.xyz;
    lightDir = lightPosition - finishedCoord.xyz;

    gl_Position = finishedCoord;
}
