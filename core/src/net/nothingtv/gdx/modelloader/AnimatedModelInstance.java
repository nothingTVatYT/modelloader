package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodeAnimation;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import net.nothingtv.gdx.tools.Debug;

public class AnimatedModelInstance extends ModelInstance {
    protected final AnimationController animationController;
    protected Node rootMotionNode;
    private final Vector3 previousTranslation = new Vector3();
    private final Vector3 rootNodeLocalTranslation = new Vector3(0, 0, 0);
    private final Vector3 tmp1 = new Vector3();
    private boolean inAnimationUpdate;
    private float timeBeforeUpdate;
    protected Matrix4 rootTransform;

    public AnimatedModelInstance(Model model, String rootMotionNodeId) {
        super(model);
        animationController = new AnimationController(this);
        for (Animation animation : animations) {
            for (NodeAnimation na : animation.nodeAnimations) {
                if (!na.translation.isEmpty() && (rootMotionNodeId.isEmpty() || na.node.id.equals(rootMotionNodeId))) {
                    rootMotionNode = na.node;
                    System.out.printf("found a root motion animation node: %s%n", na.node.id);
                    break;
                }
            }
            if (rootMotionNode != null)
                break;
        }
        rootTransform = transform;
    }

    public void update(float deltaTime) {
        if (animationController.current != null) {
            inAnimationUpdate = true;
            timeBeforeUpdate = animationController.current.time;
            animationController.update(deltaTime);
            inAnimationUpdate = false;
        }
    }

    public void setRootTransform(Matrix4 rootTransform) {
        this.rootTransform = rootTransform;
    }

    @Override
    public void calculateTransforms() {
        if (inAnimationUpdate) {
            rootMotionNode.localTransform.getTranslation(tmp1);
            //System.out.printf("tmp1 is %s%n", tmp1);
            if (animationController.current.time > timeBeforeUpdate)
                tmp1.sub(previousTranslation);
            //tmp1.rot(transform);
            rootTransform.translate(tmp1);
            Debug.instance.drawTransform("root", rootTransform);
            Debug.instance.drawTransform("model", transform, 2);
            rootMotionNode.localTransform.getTranslation(previousTranslation);
            rootMotionNode.localTransform.setTranslation(rootNodeLocalTranslation);
        }
        super.calculateTransforms();
    }
}
