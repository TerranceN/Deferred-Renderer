#version 130

in vec3 normal;

void main() {
    gl_FragColor = vec4(normalize(normal), 0);
}
