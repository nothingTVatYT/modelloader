package net.nothingtv.gdx.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolsTest {

    @Test
    void smoothStep() {
        assertEquals(0f, Tools.smoothStep(1f, 2f, 3f));
        assertEquals(0.5f, Tools.smoothStep(2.5f, 2f, 3f));
        assertEquals(1f, Tools.smoothStep(3f, 2f, 3f));
    }

    @Test
    void smoothInRange() {
        assertEquals(0f, Tools.smoothInRange(0.1f, 0.2f, 0.6f, 0.01f, 0.01f));
        assertEquals(1f, Tools.smoothInRange(0.4f, 0.2f, 0.6f, 0.01f, 0.01f));
        assertEquals(0f, Tools.smoothInRange(0.8f, 0.2f, 0.6f, 0.01f, 0.01f));
    }

    @Test
    void clamp01() {
        assertEquals(1f, Tools.clamp01(1.2f));
        assertEquals(0f, Tools.clamp01(-0.1f));
        assertEquals(0.3f, Tools.clamp01(0.3f));
    }
}