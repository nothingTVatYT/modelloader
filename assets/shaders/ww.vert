in vec3 a_position;
uniform mat4 u_projViewTrans;

out vec4 v_color;

#if defined(colorFlag)
in vec4 a_color;
#endif // colorFlag

#if defined(normalFlag)
in vec4 a_normal;
out vec4 normal;
#endif

//#if defined(cameraPositionFlag)
uniform vec4 u_cameraPosition;
//#endif

#if defined(emissiveColorFlag)
in vec4 u_emissiveColor;
out vec4 emissiveColor;
#endif

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
    normal = a_normal;
#endif
    vec4 pos = u_worldTrans * vec4(a_position, 1.0);
    worldPos = pos;
    gl_Position = u_projViewTrans * pos;
}
