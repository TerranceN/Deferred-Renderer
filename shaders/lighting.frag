#version 130

const int MAX_LIGHTS = 10;
const float PI = 3.1415926;

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
    float hViewAngle = 90 * PI / 180;
    float aspect = 1.7777777;
    float f = 1 / tan(hViewAngle / 2);
    vec2 clip = eyeVec.xy * -z;
    return vec3(clip.x * aspect / f, clip.y / f, z);
    //// Get x/w and y/w from the viewport position
    //vec4 vProjectedPos = vec4(eyeVec.xy / uNearDistance, z, 1);
    //// Transform by the inverse projection matrix
    //vec4 vPositionVS = gl_ProjectionMatrixInverse * vProjectedPos;
    //// Divide by w to get the view-space position
    //return vPositionVS.xyz / vPositionVS.w;
}

void main() {
    float ambientIntensity = 0.2;

    vec4 diffuse = texture2D(uDiffuseSampler, texCoord);
    vec4 specular = vec4(texture2D(uSpecularSampler, texCoord).xyz, 1);
    float uShininess = texture2D(uSpecularSampler, texCoord).a;

    vec4 final_color = vec4(0);

    // use this to mess around with att values: https://www.desmos.com/calculator/nmnaud1hrw
    float attConst = 1;
    float attLinear = 2;
    float attQuad = 1;

    float fractionalDepth = texture2D(uNormalsDepthSampler, texCoord).a;
    float depth = -(uNearDistance + fractionalDepth * uFarDistance);

    vec3 viewRay = vec3(eyeVec.xyz);
    vec3 fragmentPosition = positionFromDepth(depth);
    vec3 eye = normalize(fragmentPosition);

    vec3 n = normalize(texture2D(uNormalsDepthSampler, texCoord).xyz);

    float lightsToRender = min(uNumLights, MAX_LIGHTS);
    for (int i = 0; i < lightsToRender; i++) {
        vec3 difference = uLightPositions[i] - fragmentPosition;
        float distanceToLight = length(difference);
        float att = 1.0 / (attConst+(attLinear*distanceToLight)+(attQuad*distanceToLight*distanceToLight));

        vec3 lDir = normalize(difference);
        vec3 h = normalize(eye - lDir);
        float cosTh = clamp(-dot(n, h), 0, 1);
        float cosTi = clamp(dot(n, lDir), 0, 1);
        float specularIntensity = pow(cosTh, uShininess) * (uShininess + 2.0) / 8.0;
        vec4 lightingColors = diffuse + specular * specularIntensity;
        final_color += lightingColors * vec4(uLightIrradiance[i], 1) * cosTi * att;

        final_color += diffuse * ambientIntensity * att * vec4(uLightIrradiance[i], 1);

        vec3 lightPosition = (gl_ModelViewMatrix * vec4(uLightPositions[i], 1)).xyz;
        float lightDepth = lightPosition.z;

        if (lightDepth > depth) {
            float lightDirect = pow(clamp(dot(eye, normalize(lightPosition)), 0, 1), 1000);
            if (lightDirect > 0.9) {
                final_color += vec4(normalize(uLightIrradiance[i]), 1);
            }
        }
    }

    final_color += texture2D(uPreviousLightingSampler, texCoord);

    //final_color = vec4(vec3(fractionalDepth), 1);
    if (fragmentPosition.y > 0) {
        //final_color = vec4(vec2(fragmentPosition), 0, 1);
    }

    gl_FragData[0] = final_color;
}
