#ifdef normalFlag
out vec3 v_normal;
#endif //normalFlag

// texCoord unit mapping

#ifndef v_diffuseUV
#define v_diffuseUV v_texCoord0
#endif

#ifndef v_normalUV
#define v_normalUV v_texCoord0
#endif

#ifndef v_occlusionUV
#define v_occlusionUV v_texCoord0
#endif

#if defined(alpha1TextureFlag)
uniform sampler2D u_alpha1Texture;
#endif

#ifdef diffuseTextureFlag
in vec2 v_diffuseUV;
uniform sampler2D u_diffuseTexture;
#ifdef uv1ScaleFlag
uniform float u_uv1Scale;
#else
const float u_uv1Scale = 1.0;
#endif
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

#ifdef normalTextureFlag
uniform sampler2D u_normalTexture;
uniform float u_NormalScale;
#endif

#ifdef occlusionTextureFlag
uniform sampler2D u_OcclusionSampler;
uniform float u_OcclusionStrength;
#endif

#define u_ior 1.5

#ifdef specularFactorFlag
uniform float u_specularFactor;
#else
#define u_specularFactor 1.0
#endif

#ifdef specularColorFlag
uniform vec3 u_specularColorFactor;
#endif

#ifdef specularFactorTextureFlag
uniform sampler2D u_specularFactorSampler;
#endif

#ifdef specularColorTextureFlag
uniform sampler2D u_specularColorSampler;
#endif

uniform vec2 u_MetallicRoughnessValues;

// Encapsulate the various inputs used by the various functions in the shading equation
// We store values in structs to simplify the integration of alternative implementations
// PBRSurfaceInfo contains light independent information (surface/material only)
// PBRLightInfo contains light information (incident rays)
struct PBRSurfaceInfo
{
	vec3 n;						  // Normal vector at surface point
	vec3 v;						  // Vector from surface point to camera
	float NdotV;                  // cos angle between normal and view direction
	float perceptualRoughness;    // roughness value, as authored by the model creator (input to shader)
	float metalness;              // metallic value at the surface
	vec3 reflectance0;            // full reflectance color (normal incidence angle)
	vec3 reflectance90;           // reflectance color at grazing angle
	float alphaRoughness;         // roughness mapped to a more linear change in the roughness (proposed by [2])
	vec3 diffuseColor;            // color contribution from diffuse lighting
	vec3 specularColor;           // color contribution from specular lighting

	float thickness;           	  // volume thickness at surface point (used for refraction)

	float specularWeight;		  // Amount of specular for the material (default is 1.0)

};

vec4 getBaseColor()
{
    // The albedo may be defined from a base texture or a flat color

#ifdef diffuseTextureFlag
#if defined(alpha1TextureFlag)
	vec4 splat1 = texture2D(u_alpha1Texture, v_diffuseUV);
#endif
#if defined(diffuse2TextureFlag) && defined(diffuse3TextureFlag) && defined(diffuse4TextureFlag) && defined(alpha1TextureFlag)
	vec4 diffuse = texture2D(u_diffuseTexture, v_diffuseUV * u_uv1Scale) * splat1.a
	+ texture2D(u_diffuse2Texture, v_diffuseUV * u_uv2Scale) * splat1.b
	+ texture2D(u_diffuse3Texture, v_diffuseUV * u_uv3Scale) * splat1.g
	+ texture2D(u_diffuse4Texture, v_diffuseUV * u_uv4Scale) * splat1.r;
#elif defined(diffuse2TextureFlag) && defined(diffuse3TextureFlag) && defined(alpha1TextureFlag)
	vec4 diffuse = texture2D(u_diffuseTexture, v_diffuseUV * u_uv1Scale) * splat1.a
	+ texture2D(u_diffuse2Texture, v_diffuseUV * u_uv2Scale) * splat1.b
	+ texture2D(u_diffuse3Texture, v_diffuseUV * u_uv3Scale) * splat1.g;
#elif defined(diffuse2TextureFlag) && defined(alpha1TextureFlag)
	vec4 diffuse = texture2D(u_diffuseTexture, v_diffuseUV * u_uv1Scale) * splat1.a
	+ texture2D(u_diffuse2Texture, v_diffuseUV * u_uv2Scale) * splat1.b;
#else
	vec4 diffuse = texture2D(u_diffuseTexture, v_diffuseUV * u_uv1Scale);
#endif
	vec4 baseColor = SRGBtoLINEAR(diffuse);
#endif // diffuseTextureFlag

    return baseColor;
}
