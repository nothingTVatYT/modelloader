package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.graphics.g3d.model.NodeAnimation;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Vector3;

public class AnimatedModelInstance extends ModelInstance {
    protected final AnimationController animationController;
    protected NodeAnimation rootNodeAnimation;
    private final Vector3 previousTranslation = new Vector3();
    private final Vector3 rootNodeLocalTranslation = new Vector3(0, 0, 0);
    private final Vector3 tmp1 = new Vector3();
    private boolean inAnimationUpdate;
    private float timeBeforeUpdate;

    public AnimatedModelInstance(Model model) {
        super(model);
        animationController = new AnimationController(this);
        rootNodeAnimation = null;
        for (Animation animation : animations) {
            for (NodeAnimation na : animation.nodeAnimations) {
                if (!na.translation.isEmpty()) {
                    rootNodeAnimation = na;
                    break;
                }
            }
            if (rootNodeAnimation != null)
                break;
        }
    }

    public void update(float deltaTime) {
        inAnimationUpdate = true;
        timeBeforeUpdate = animationController.current.time;
        animationController.update(deltaTime);
        inAnimationUpdate = false;
    }

    @Override
    public void calculateTransforms() {
        if (inAnimationUpdate) {
            rootNodeAnimation.node.localTransform.getTranslation(tmp1);
            if (animationController.current.time > timeBeforeUpdate)
                tmp1.sub(previousTranslation);
            transform.translate(tmp1);
            rootNodeAnimation.node.localTransform.getTranslation(previousTranslation);
            rootNodeAnimation.node.localTransform.setTranslation(rootNodeLocalTranslation);
        }
        super.calculateTransforms();
    }
}
