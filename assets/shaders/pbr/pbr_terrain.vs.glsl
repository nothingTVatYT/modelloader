#line 1

in vec3 a_position;
out vec3 v_position;

uniform mat4 u_projViewTrans;

in vec3 a_normal;
out vec3 v_normal;

in vec2 a_texCoord0;
out vec2 v_texCoord0;

uniform mat4 u_worldTrans;

#ifdef shadowMapFlag
uniform mat4 u_shadowMapProjViewTrans;
out vec3 v_shadowMapUv;
#ifdef numCSM
uniform mat4 u_csmTransforms[numCSM];
out vec3 v_csmUVs[numCSM];
#endif
#endif //shadowMapFlag

void main() {

	v_texCoord0 = a_texCoord0;

	vec4 pos = u_worldTrans * vec4(a_position, 1.0);

	v_position = vec3(pos.xyz) / pos.w;
	gl_Position = u_projViewTrans * pos;

#ifdef shadowMapFlag
	vec4 spos = u_shadowMapProjViewTrans * pos;
	v_shadowMapUv.xyz = (spos.xyz / spos.w) * 0.5 + 0.5;
	v_shadowMapUv.z = min(v_shadowMapUv.z, 0.998);
#ifdef numCSM
	for(int i=0 ; i<numCSM ; i++){
		vec4 csmPos = u_csmTransforms[i] * pos;
		v_csmUVs[i].xyz = (csmPos.xyz / csmPos.w) * 0.5 + 0.5;
	}
#endif //numCSM
#endif //shadowMapFlag

	v_normal = a_normal;
}