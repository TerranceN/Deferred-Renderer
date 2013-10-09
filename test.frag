#version 130

in vec2 texCoord;
in vec4 color;
in vec3 normal;

in vec3 lightDir;
in vec3 eyeVec;

uniform sampler2D uSampler;

void main() {
    float ambientIntensity = 0.05;

    vec4 final_color = vec4(0);

    vec3 n = normalize(normal);
    vec3 l = normalize(lightDir);
    float lambertTerm = dot(n, l);

    lambertTerm = max(lambertTerm, ambientIntensity);

    final_color += texture2D(uSampler, texCoord) * color * lambertTerm;

    if (lambertTerm > ambientIntensity) {
        vec3 e = normalize(eyeVec);
        vec3 r = reflect(-l, n);
        float specular = pow(max(dot(r, e), 0.0), 100);
        final_color += 0.5 * vec4(specular);
    }

    gl_FragColor = final_color;
}
