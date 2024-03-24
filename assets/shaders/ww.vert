in vec3 a_position;
uniform mat4 u_projViewTrans;

out vec4 v_color;

#if defined(colorFlag)
in vec4 a_color;
#endif // colorFlag

#if defined(normalFlag)
in vec4 a_normal;
out vec3 normal;
#endif

//#if defined(cameraPositionFlag)
uniform vec4 u_cameraPosition;
//#endif

#if defined(emissiveColorFlag)
in vec4 u_emissiveColor;
out vec4 emissiveColor;
#endif

#if numDirectionalLights > 0
struct DirectionalLight
{
    vec3 color;
    vec3 direction;
};
uniform DirectionalLight u_dirLights[numDirectionalLights];
out vec3 v_lightDiffuse;
#endif // numDirectionalLights

out vec4 worldPos;

#ifdef diffuseTextureFlag
in vec2 a_texCoord0;
#endif // textureFlag

#ifdef diffuseTextureFlag
uniform vec4 u_diffuseUVTransform;
out vec2 v_diffuseUV;
#endif

#if defined(ambientLightColorFlag) || defined(ambientCubemapFlag)
#define ambientLightFlag
out float ambientLight;
#endif

#ifdef ambientCubemapFlag
uniform vec3 u_ambientCubemap[6];
#endif // ambientCubemapFlag

#ifdef diffuseColorFlag
uniform vec4 u_diffuseColor;
#endif

#ifdef baseColorFactorFlag
uniform vec4 u_BaseColorFactor;
#endif

uniform mat4 u_worldTrans;

void main() {
#if defined(baseColorFactorFlag)
    v_color = u_BaseColorFactor;
#elif defined(diffuseColorFlag)
    v_color = u_diffuseColor;
#elif defined(colorFlag)
    v_color = a_color;
#elif defined(emissiveColorFlag)
    v_color = u_emissiveColor;
#else
    v_color = vec4(1);
#endif

#if defined(ambientLightFlag)
#if defined(ambientCubemapFlag)
    ambientLight = u_ambientCubemap[0].x;
#else
    ambientLight = u_ambientLightColor.x;
#endif
#endif

#ifdef diffuseTextureFlag
    v_diffuseUV = u_diffuseUVTransform.xy + a_texCoord0 * u_diffuseUVTransform.zw;
#endif
#if defined(emissiveColorFlag)
    emissiveColor = u_emissiveColor;
#endif
#if defined(normalFlag)
    normal = vec3(a_normal);
#endif

#if (numDirectionalLights > 0) && defined(normalFlag)
    v_lightDiffuse = vec3(0);
    for (int i = 0; i < numDirectionalLights; i++) {
        vec3 lightDir = -u_dirLights[i].direction;
        float NdotL = clamp(dot(normal, lightDir), 0.0, 1.0);
        vec3 value = u_dirLights[i].color * NdotL;
        v_lightDiffuse += value;
    }
#endif // numDirectionalLights

    vec4 pos = u_worldTrans * vec4(a_position, 1.0);
    worldPos = pos;
    gl_Position = u_projViewTrans * pos;
}
