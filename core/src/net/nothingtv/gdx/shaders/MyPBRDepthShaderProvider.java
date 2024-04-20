package net.nothingtv.gdx.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.DepthShader;
import net.mgsx.gltf.scene3d.shaders.PBRCommon;
import net.mgsx.gltf.scene3d.shaders.PBRDepthShader;
import net.mgsx.gltf.scene3d.shaders.PBRDepthShaderProvider;

public class MyPBRDepthShaderProvider extends PBRDepthShaderProvider {


    public MyPBRDepthShaderProvider(PBRDepthShader.Config config) {
        super(config);
    }

    @Override
    protected Shader createShader(Renderable renderable) {

        PBRCommon.checkVertexAttributes(renderable);

        String prefix = DepthShader.createPrefix(renderable, config) + morphTargetsPrefix(renderable);
        if( renderable.meshPart.mesh.isInstanced()) {
            prefix += "#define instanced\n";
        }
        config.vertexShader = Gdx.files.internal("shaders/depth.vs.glsl").readString();
        return new MyPBRDepthShader(renderable, config, prefix );
    }


}
