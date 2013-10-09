#version 130

in vec2 texCoord;
in vec4 color;
in vec3 normal;

in vec3 lightDir;
in vec3 eyeVec;
in float distanceToLight;

uniform sampler2D uSampler;

void main() {
    float ambientIntensity = 0.05;

    vec4 final_color = vec4(0);

    // use this to mess around with att values: https://www.desmos.com/calculator/nmnaud1hrw
    float attConst = 1.0;
    float attLinear = 0.0;
    float attQuad = 0.05;

    float att = 1.0 / (attConst+(attLinear*distanceToLight)+(attQuad*distanceToLight*distanceToLight));

    vec3 n = normalize(normal);
    vec3 l = normalize(lightDir);
    float lambertTerm = dot(n, l) * att;

    lambertTerm = max(lambertTerm, ambientIntensity);

    final_color += texture2D(uSampler, texCoord) * color * lambertTerm;

    if (lambertTerm > ambientIntensity) {
        vec3 e = normalize(eyeVec);
        vec3 r = reflect(-l, n);
        float specular = pow(max(dot(r, e), 0.0), 20);
        final_color += 0.2 * vec4(specular) * att;
    }

    gl_FragColor = final_color;
}
