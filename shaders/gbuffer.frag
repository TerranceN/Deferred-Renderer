#version 130

in vec2 texCoord;
in vec3 normal;
in float depth;
in vec3 eyeVec;

uniform sampler2D uDiffuseSampler;
uniform int uDiffuseNumComponents;
uniform vec4 uDiffuseColor;
uniform float uDiffuseTexture;

uniform sampler2D uSpecularSampler;
uniform int uSpecularNumComponents;
uniform vec4 uSpecularColor;
uniform float uSpecularTexture;

uniform float uShininess;

uniform sampler2D uNormalMapSampler;
uniform float uNormalMapTexture;

uniform float uFarDistance;

mat3 cotangent_frame( vec3 N, vec3 p, vec2 uv )
{
    // get edge vectors of the pixel triangle
    vec3 dp1 = dFdx( p );
    vec3 dp2 = dFdy( p );
    vec2 duv1 = dFdx( uv );
    vec2 duv2 = dFdy( uv );
 
    // solve the linear system
    vec3 dp2perp = cross( dp2, N );
    vec3 dp1perp = cross( N, dp1 );
    vec3 T = dp2perp * duv1.x + dp1perp * duv2.x;
    vec3 B = dp2perp * duv1.y + dp1perp * duv2.y;
 
    // construct a scale-invariant frame 
    float invmax = inversesqrt( max( dot(T,T), dot(B,B) ) );
    return mat3( T * invmax, B * invmax, N );
}

vec3 perturb_normal( vec3 N, vec3 V, vec2 texcoord )
{
    // assume N, the interpolated vertex normal and 
    // V, the view vector (vertex to eye)
    vec3 map = texture2D(uNormalMapSampler, texcoord).xyz;
    map = map * 255./127. - 128./127.;
    map.y = -map.y;
    mat3 TBN = cotangent_frame(N, -V, texcoord);
    return normalize( TBN * map );
}

void main() {
    vec4 diffuse = vec4(1.0);
    if (uDiffuseTexture > 0.5) {
        diffuse = texture2D(uDiffuseSampler, texCoord);
        if (uDiffuseNumComponents == 3) {
            diffuse = vec4(diffuse.xyz, 1);
        }
        if (uDiffuseNumComponents < 3) {
            diffuse = vec4(vec3(diffuse.x), 1);
        }
    } else {
        diffuse = uDiffuseColor;
    }

    vec4 specular = vec4(1.0);
    if (uSpecularTexture > 0.5) {
        specular = texture2D(uSpecularSampler, texCoord);
        if (uSpecularNumComponents == 3) {
            specular = vec4(specular.xyz, 1);
        }
        if (uSpecularNumComponents < 3) {
            specular = vec4(vec3(specular.x), 1);
        }
    } else {
        specular = uSpecularColor;
    }

    vec3 newNormal;
    if (uNormalMapTexture > 0.5) {
        newNormal = perturb_normal(normalize(normal), normalize(eyeVec), texCoord);
    } else {
        newNormal = normal;
    }

    gl_FragData[0] = diffuse;
    gl_FragData[1] = vec4(newNormal, depth);
    gl_FragData[2] = vec4(vec3(specular.xyz), uShininess);
}
