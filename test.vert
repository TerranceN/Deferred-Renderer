#version 130

in vec3 aCoord;
in vec4 aColor;
in vec3 aNormal;
in vec2 aTexCoord;

out vec2 texCoord;
out vec4 color;

void main() {
    color = aColor;
    texCoord = aTexCoord;
    gl_Position = gl_ModelViewProjectionMatrix * vec4(aCoord, 1.0);
}
