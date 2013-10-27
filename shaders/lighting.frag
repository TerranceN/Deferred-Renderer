#version 130

const int MAX_LIGHTS = 10;

in vec2 texCoord;
in vec3 eyeVec;

uniform int uNumLights;

uniform vec3 uLightPositions[MAX_LIGHTS];
uniform vec3 uLightIrradiance[MAX_LIGHTS];

uniform sampler2D uDiffuseSampler;
uniform sampler2D uNormalsDepthSampler;
uniform sampler2D uSpecularSampler;
uniform sampler2D uPreviousLightingSampler;

uniform float uFarDistance;
uniform float uNearDistance;

vec3 positionFromDepth(float z) {
    // Get x/w and y/w from the viewport position
    vec4 vProjectedPos = vec4(eyeVec.xy / uNearDistance, z, 1);
    // Transform by the inverse projection matrix
    vec4 vPositionVS = gl_ProjectionMatrixInverse * vProjectedPos;
    // Divide by w to get the view-space position
    return vPositionVS.xyz / vPositionVS.w;
}

void main() {
    float ambientIntensity = 0.2;

    vec4 diffuse = texture2D(uDiffuseSampler, texCoord);
    vec4 specular = vec4(texture2D(uSpecularSampler, texCoord).xyz, 1);
    float uShininess = texture2D(uSpecularSampler, texCoord).a;

    vec4 final_color = vec4(0);

    // use this to mess around with att values: https://www.desmos.com/calculator/nmnaud1hrw
    float attConst = 1.0;
    float attLinear = 0.0;
    float attQuad = 0.05;

    float depth = texture2D(uNormalsDepthSampler, texCoord).a * -uFarDistance;

    vec3 viewRay = vec3(eyeVec.xy, 1);
    vec3 fragmentPosition = positionFromDepth(depth);

    vec3 v = normalize(viewRay);
    vec3 n = normalize(texture2D(uNormalsDepthSampler, texCoord).xyz);

    float lightsToRender = min(uNumLights, MAX_LIGHTS);
    for (int i = 0; i < lightsToRender; i++) {
        vec3 difference = uLightPositions[i] - fragmentPosition;
        float distanceToLight = length(difference);
        float att = 1.0 / (attConst+(attLinear*distanceToLight)+(attQuad*distanceToLight*distanceToLight));

        vec3 lDir = normalize(difference);
        vec3 h = normalize(v + lDir);
        float cosTh = clamp(dot(n, h), 0, 1);
        float cosTi = clamp(dot(n, lDir), 0, 1);
        float specularIntensity = pow(cosTh, uShininess);
        vec4 lightingColors = diffuse;
        if (cosTh > 0) {
            lightingColors += specular * specularIntensity;
        }
        final_color += lightingColors * vec4(uLightIrradiance[i], 1) * cosTi * att;

        final_color += diffuse * ambientIntensity * att * vec4(uLightIrradiance[i], 1);
    }

    final_color += texture2D(uPreviousLightingSampler, texCoord);

    gl_FragData[0] = final_color;
}
