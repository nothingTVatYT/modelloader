#line 0
precision mediump float;

in vec4 v_color;
#if defined(normalFlag)
in vec4 normal;
#endif

#ifdef diffuseTextureFlag
in vec2 v_diffuseUV;
uniform sampler2D u_diffuseTexture;
#endif

#ifdef emissiveColorFlag
in vec4 emissiveColor;
in vec4 worldPos;
#endif

#if defined(ambientLightColorFlag) || defined(ambientCubemapFlag)
#define ambientLightFlag
in float ambientLight;
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
#if defined(diffuseTextureFlag)
    vec4 diffuse = texture2D(u_diffuseTexture, v_diffuseUV * u_uv1Scale) * light;
#else
    vec4 diffuse = v_color * light;
#endif // diffuseTextureFlag
    color = diffuse;
#endif // emissiveColorFlag
}
