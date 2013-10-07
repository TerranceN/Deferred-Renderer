in vec3 aCoord;
in vec4 aColor;

out vec4 fragmentColor;

void main() {
    fragmentColor = aColor;
    gl_Position = gl_ModelViewProjectionMatrix * vec4(aCoord, 1.0);
}
