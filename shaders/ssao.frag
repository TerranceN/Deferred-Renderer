#version 140

const int MAX_KERN_POINTS = 50;

in vec2 texCoord;
in vec3 eyeVec;

uniform sampler2D uNormalsDepthSampler;

uniform sampler2D uSSAONoiseSampler;
uniform vec2 uNoiseScale;

uniform int uNumKernPoints;
uniform vec3 uKernPoints[MAX_KERN_POINTS];

uniform mat4 u3dProjectionMatrix;

uniform float uFarDistance;
uniform float uNearDistance;

uniform float uRadius;

vec3 positionFromEyeAndDepth(vec3 eye, float z) {
    vec4 clip = vec4(eye.xyz, 1) * -z;
    return (inverse(u3dProjectionMatrix) * clip).xyz;
}

vec3 positionFromDepth(float z) {
    return positionFromEyeAndDepth(eyeVec, z);
}

void main() {
    float fractionalDepth = texture2D(uNormalsDepthSampler, texCoord).a;

    float depth = -(uNearDistance + fractionalDepth * uFarDistance) - 0.1;

    vec3 fragmentPosition = positionFromDepth(depth);
    vec3 eye = normalize(fragmentPosition);

    vec3 n = normalize(texture2D(uNormalsDepthSampler, texCoord).xyz);

    vec3 rvec = texture(uSSAONoiseSampler, texCoord * uNoiseScale).xyz * 2.0 - 1.0;
    vec3 tangent = normalize(rvec - n * dot(rvec, n));
    vec3 bitangent = cross(n, tangent);
    mat3 tbn = mat3(tangent, bitangent, n);

    vec3 test = vec3(0);
    float occlusion = 0.0;
    float maxIndex = min(MAX_KERN_POINTS, uNumKernPoints);
    for (int i = 0; i < maxIndex; i++) {
        // get sample position:
        vec3 sample = tbn * uKernPoints[i];
        test += sample;

        sample = sample * uRadius + fragmentPosition;

        // project sample position:
        vec4 offset = vec4(sample, 1.0);
        offset = u3dProjectionMatrix * offset;
        offset.xyz /= offset.w;
        vec2 offsetTex = offset.xy * 0.5 + 0.5;

        float sampleLinearDepth = texture2D(uNormalsDepthSampler, offsetTex).a;
        float sampleDepth = -(uNearDistance + sampleLinearDepth * uFarDistance) - 0.1;
        vec3 samplePosition = positionFromEyeAndDepth(offset, sampleDepth);

        //occlusion += sampleLinearDepth;
        //if (abs(samplePosition.z - sample.z) > 0.01) {
        //if (length(sample - samplePosition) <= uRadius) {
        if (sample.z - samplePosition.z > 0.001) {
        //if (abs(dot(tangent, tangent)) > 0.1) {
            //occlusion += sampleLinearDepth;
        }

        // range check & accumulate:
        float rangeCheck= length(fragmentPosition - samplePosition) < uRadius ? 1.0 : 0.0;
        occlusion += (samplePosition.z > sample.z ? 1.0 : 0.0) * rangeCheck;
    }

    occlusion = 1.0 - (occlusion / maxIndex);
    test = normalize(test);

    gl_FragColor = vec4(vec3(occlusion), 1);
    //gl_FragColor = vec4(rvec, 1);
    //gl_FragColor = vec4(test, 1);
}
