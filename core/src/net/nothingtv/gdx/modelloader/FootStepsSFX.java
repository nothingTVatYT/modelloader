package net.nothingtv.gdx.modelloader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import java.util.Random;

public class FootStepsSFX implements Disposable {

    public Array<Sound> clips = new Array<>();
    public float stepIntervalWalking = 0.45f;
    public float stepIntervalRunning = 0.4f;
    public float volume;

    private final FirstPersonController playerController;
    private float lastStepTime;
    private float gameTime;
    private final Random rnd = new Random();
    private long lastClipId;
    private Sound lastSound;
    public float stepInterval;

    public FootStepsSFX(FirstPersonController controller) {
        this.playerController = controller;
        gameTime = 0;
    }

    public void initializeDefaults() {
        for (int i = 0; i < 8; i++) {
            clips.add(Gdx.audio.newSound(Gdx.files.internal("sounds/Grass1 " + (i+1) + "-Audio.wav")));
        }
        volume = 0.3f;
    }

    public void stopSounds() {
        if (lastSound != null)
            lastSound.stop(lastClipId);
    }

    private void playStep() {
        lastSound = clips.get(rnd.nextInt(clips.size));
        lastClipId = lastSound.play(volume);
        // sound.setPan(id, -1, 1);
    }

    public void update(float delta) {
        if (!playerController.isWalking()) {
            //stopSounds();
            return;
        }
        gameTime += delta;
        stepInterval = playerController.isRunning() ? stepIntervalRunning : stepIntervalWalking;

        if (playerController.isGrounded()) {
            if (gameTime >= lastStepTime + stepInterval) {
                playStep();
                lastStepTime = gameTime;
            }
        }
    }

    @Override
    public void dispose() {
        for (Sound sound : clips)
            sound.dispose();
        clips.clear();
    }
}
