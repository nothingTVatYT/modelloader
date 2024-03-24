#line 0
precision mediump float;

in vec4 v_color;
#if defined(normalFlag)
in vec4 normal;
#endif

#if defined(alpha1TextureFlag)
uniform sampler2D u_alpha1Texture;
#endif

#ifdef diffuseTextureFlag
in vec2 v_diffuseUV;
uniform sampler2D u_diffuseTexture;
#endif

#ifdef diffuse2TextureFlag
uniform sampler2D u_diffuse2Texture;
#ifdef uv2ScaleFlag
uniform float u_uv2Scale;
#else
const float u_uv2Scale = 1.0;
#endif
#endif

#ifdef diffuse3TextureFlag
uniform sampler2D u_diffuse3Texture;
#ifdef uv3ScaleFlag
uniform float u_uv3Scale;
#else
const float u_uv3Scale = 1.0;
#endif
#endif

#ifdef diffuse4TextureFlag
uniform sampler2D u_diffuse4Texture;
#ifdef uv4ScaleFlag
uniform float u_uv4Scale;
#else
const float u_uv4Scale = 1.0;
#endif
#endif

#ifdef emissiveColorFlag
in vec4 emissiveColor;
in vec4 worldPos;
#endif

#if defined(ambientLightColorFlag) || defined(ambientCubemapFlag)
#define ambientLightFlag
in float ambientLight;
#endif

#if numDirectionalLights > 0
in vec3 v_lightDiffuse;
#endif

#ifdef uv1ScaleFlag
uniform float u_uv1Scale;
#else
const float u_uv1Scale = 1.0;
#endif
uniform vec4 u_cameraPosition;
uniform mat4 u_worldTrans;

in vec3 a_position;
out vec4 color;

void main() {
#if defined(lightingFlag)
#if defined(ambientLightFlag)
    float light = ambientLight;
#else
    float light = 0.05;
#endif // ambientLightFlag
#else
    float light = 1;
#endif // lightingFlag

#if defined(emissiveColorFlag)
    // darken faces not facing the camera
    color = emissiveColor * dot(normalize(u_cameraPosition.xyz - worldPos.xyz), (normal * inverse(u_worldTrans)).xyz);
    // brighten faces facing the camera
    // color = emissiveColor + (vec4(1) * pow(dot(normalize(u_cameraPosition.xyz - worldPos.xyz), (normal * inverse(u_worldTrans)).xyz), 10));
#else
#if defined(alpha1TextureFlag)
    vec4 splat1 = texture2D(u_alpha1Texture, v_diffuseUV);
#endif
#if defined(diffuseTextureFlag) && defined(diffuse2TextureFlag) && defined(diffuse3TextureFlag) && defined(diffuse4TextureFlag) && defined(alpha1TextureFlag)
    vec4 diffuse = (texture2D(u_diffuseTexture, v_diffuseUV * u_uv1Scale) * splat1.a
    + texture2D(u_diffuse2Texture, v_diffuseUV * u_uv2Scale) * splat1.b
    + texture2D(u_diffuse3Texture, v_diffuseUV * u_uv3Scale) * splat1.g
    + texture2D(u_diffuse4Texture, v_diffuseUV * u_uv4Scale) * splat1.r) * light;
#elif defined(diffuseTextureFlag) && defined(diffuse2TextureFlag) && defined(diffuse3TextureFlag) && defined(alpha1TextureFlag)
    vec4 diffuse = (texture2D(u_diffuseTexture, v_diffuseUV) * splat1.a
    + texture2D(u_diffuse2Texture, v_diffuseUV) * splat1.b
    + texture2D(u_diffuse3Texture, v_diffuseUV) * splat1.g) * light;
#elif defined(diffuseTextureFlag) && defined(diffuse2TextureFlag) && defined(alpha1TextureFlag)
    vec4 diffuse = (texture2D(u_diffuseTexture, v_diffuseUV) * splat1.a
    + texture2D(u_diffuse2Texture, v_diffuseUV) * splat1.b) * light;
#elif defined(diffuseTextureFlag)
    vec4 diffuse = texture2D(u_diffuseTexture, v_diffuseUV * u_uv1Scale) * light;
#else
    vec4 diffuse = v_color * light;
#if numDirectionalLights > 0
    diffuse += vec4(v_lightDiffuse, 0);
#endif
#endif // diffuseTextureFlag
    color = diffuse + normal * 0.1;
#endif // emissiveColorFlag
}
