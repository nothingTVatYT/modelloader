#line 1

#define textureCubeLodEXT textureLod
#define texture2DLodEXT textureLod

#define varying in
out vec4 out_FragColor;
#define textureCube texture
#define texture2D texture

#include <functions.glsl>
#include <splat_material.glsl>

#ifdef fogFlag
uniform vec4 u_fogColor;
#ifdef fogEquationFlag
uniform vec3 u_fogEquation;
#endif
#endif // fogFlag

#ifdef ambientLightFlag
uniform vec3 u_ambientLight;
#endif // ambientLightFlag

uniform vec4 u_cameraPosition;
uniform mat4 u_worldTrans;
in vec3 v_position;

uniform mat4 u_projViewTrans;

//-----------
#include <terrain_lights.glsl>

#include <terrain_shadows.glsl>
#ifdef USE_IBL
#include <ibl.glsl>
#endif

#define heightBegin2 0.1
#define heightEnd2 0.7
#define heightBegin3 0.7
#define heightEnd3 1.0
#define slopeBegin3 0.21
#define slopeEnd3 1.0

void main() {
	
    // Metallic and Roughness material properties are packed together
    // In glTF, these factors can be specified by fixed scalar values
    // or from a metallic-roughness map
    float perceptualRoughness = u_MetallicRoughnessValues.y;
    float metallic = u_MetallicRoughnessValues.x;
    perceptualRoughness = clamp(perceptualRoughness, c_MinRoughness, 1.0);
    metallic = clamp(metallic, 0.0, 1.0);
    // Roughness is authored as perceptual roughness; as is convention,
    // convert to material roughness by squaring the perceptual roughness [2].
    float alphaRoughness = perceptualRoughness * perceptualRoughness;

// terrain parameters:
// {resolution:1024,layers:[{},{heightBegin:0.3,heightEnd:0.5},{elevationWeight:1,heightBegin:0.1,heightEnd:0.7},{elevationWeight:1,heightBegin:0.7,heightEnd:1,slopeWeight:1,slopeBegin:0.21,slopeEnd:1}]}

    float relHeight = (v_position.y + 5.0) / 50.0;
    float weight2 = max(0.0, ((heightEnd2 - heightBegin2) - abs(relHeight - heightEnd2)) / (heightEnd2 - heightBegin2));
    float weight3h = max(0.0, ((heightEnd3 - heightBegin3) - abs(relHeight - heightEnd3)) / (heightEnd3 - heightBegin3));
    float weight3s = max(0.0, ((slopeEnd3 - slopeBegin3) - abs(v_normal.y - slopeEnd3)) / (slopeEnd3 - slopeBegin3));
    float weight3 = max(weight3h, weight3s);
    float weight0 = max(0.0, 1.0 - weight2 - weight3);

    vec4 diffuse = weight0 * texture2D(u_diffuse4Texture, v_diffuseUV * u_uv4Scale) +
        weight2 * texture2D(u_diffuse2Texture, v_diffuseUV * u_uv2Scale) +
        weight3 * texture2D(u_diffuseTexture, v_diffuseUV * u_uv1Scale);

    vec4 baseColor = SRGBtoLINEAR(diffuse);
    //vec4 baseColor = vec4(weight0, weight2, weight3, 1.0);
    //vec4 baseColor = getBaseColor();

    vec3 f0 = vec3(0.04); // from ior 1.5 value

    // Specular
    float specularWeight = 1.0;
    vec3 diffuseColor = baseColor.rgb * (vec3(1.0) - f0);
    diffuseColor *= 1.0 - metallic;
    vec3 specularColor = mix(f0, baseColor.rgb, metallic);


    // Compute reflectance.
    float reflectance = max(max(specularColor.r, specularColor.g), specularColor.b);

    // For typical incident reflectance range (between 4% to 100%) set the grazing reflectance to 100% for typical fresnel effect.
    // For very low reflectance range on highly diffuse objects (below 4%), incrementally reduce grazing reflecance to 0%.
    float reflectance90 = clamp(reflectance * 25.0, 0.0, 1.0);
    vec3 specularEnvironmentR0 = specularColor.rgb;
    vec3 specularEnvironmentR90 = vec3(1.0, 1.0, 1.0) * reflectance90;

    vec3 surfaceToCamera = u_cameraPosition.xyz - v_position;
    float eyeDistance = length(surfaceToCamera);

    vec3 n = normalize(v_normal);                             // normal at surface point
    vec3 v = surfaceToCamera / eyeDistance;        // Vector from surface point to camera
    vec3 reflection = -normalize(reflect(v, n));

    float NdotV = clamp(abs(dot(n, v)), 0.001, 1.0);

    PBRSurfaceInfo pbrSurface = PBRSurfaceInfo(
    	n,
		v,
		NdotV,
		perceptualRoughness,
		metallic,
		specularEnvironmentR0,
		specularEnvironmentR90,
		alphaRoughness,
		diffuseColor,
		specularColor,
		0.0,
		specularWeight
    );

    vec3 f_diffuse = vec3(0.0);
    vec3 f_specular = vec3(0.0);

    // Calculate lighting contribution from image based lighting source (IBL)

#if defined(USE_IBL) && defined(ambientLightFlag)
    PBRLightContribs contribIBL = getIBLContribution(pbrSurface, n, reflection);
    f_diffuse += contribIBL.diffuse * u_ambientLight;
    f_specular += contribIBL.specular * u_ambientLight;
    vec3 ambientColor = vec3(0.0, 0.0, 0.0);
#elif defined(USE_IBL)
    PBRLightContribs contribIBL = getIBLContribution(pbrSurface, n, reflection);
    f_diffuse += contribIBL.diffuse;
    f_specular += contribIBL.specular;
    vec3 ambientColor = vec3(0.0, 0.0, 0.0);
#elif defined(ambientLightFlag)
    vec3 ambientColor = u_ambientLight;
#else
    vec3 ambientColor = vec3(0.0, 0.0, 0.0);
#endif

#if (numDirectionalLights > 0)
    // Directional lights calculation
    PBRLightContribs contrib0 = getDirectionalLightContribution(pbrSurface, u_dirLights[0]);
#ifdef shadowMapFlag
    float shadows = getShadow();
    f_diffuse += contrib0.diffuse * shadows;
    f_specular += contrib0.specular * shadows;
#else
    f_diffuse += contrib0.diffuse;
    f_specular += contrib0.specular;
#endif

    for(int i=1 ; i<numDirectionalLights ; i++){
    	PBRLightContribs contrib = getDirectionalLightContribution(pbrSurface, u_dirLights[i]);
        f_diffuse += contrib.diffuse;
        f_specular += contrib.specular;
    }
#endif

#if (numPointLights > 0)
    // Point lights calculation
    for(int i=0 ; i<numPointLights ; i++){
    	PBRLightContribs contrib = getPointLightContribution(pbrSurface, u_pointLights[i]);
    	f_diffuse += contrib.diffuse;
    	f_specular += contrib.specular;
    }
#endif // numPointLights

#if (numSpotLights > 0)
    // Spot lights calculation
    for(int i=0 ; i<numSpotLights ; i++){
    	PBRLightContribs contrib = getSpotLightContribution(pbrSurface, u_spotLights[i]);
    	f_diffuse += contrib.diffuse;
    	f_specular += contrib.specular;
    }
#endif // numSpotLights

    vec3 color = ambientColor + f_diffuse + f_specular;

    // final frag color
#ifdef GAMMA_CORRECTION
    out_FragColor = vec4(pow(color,vec3(1.0/GAMMA_CORRECTION)), baseColor.a);
#else
    out_FragColor = vec4(color, baseColor.a);
#endif

#ifdef fogFlag
#ifdef fogEquationFlag
    float fog = (eyeDistance - u_fogEquation.x) / (u_fogEquation.y - u_fogEquation.x);
    fog = clamp(fog, 0.0, 1.0);
    fog = pow(fog, u_fogEquation.z);
#else
	float fog = min(1.0, eyeDistance * eyeDistance * u_cameraPosition.w);
#endif
	out_FragColor.rgb = mix(out_FragColor.rgb, u_fogColor.rgb, fog * u_fogColor.a);
#endif

	out_FragColor.a = 1.0;
}
