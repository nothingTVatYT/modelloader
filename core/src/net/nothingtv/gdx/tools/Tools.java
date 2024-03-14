package net.nothingtv.gdx.tools;

public class Tools {

    /**
     * Smooth value between edge1 and edge2 using the 3x²-2x³ formula
     * @param value the x value to be fed into the smooth function
     * @param edge1 the lower limit where smoothing should begin (below the result is 0)
     * @param edge2 the upper limit where smoothing should end (above the result is 1)
     * @return a smoothed value between 0 and 1
     */
    public static float smoothStep(float value, float edge1, float edge2) {
        float result = clamp01((value - edge1) / (edge2 - edge1));
        return result * result * (3 - 2 * result);
    }

    /**
     * Clamp the value between 0 and 1
     * @param x the value to be clamped
     * @return 0 if x<=0, 1 if x>=1, x otherwise
     */
    public static float clamp01(float x) {
        if (x < 0) return 0;
        return Math.min(x, 1);
    }

    public static void main(String[] args) {
        for (int x = 0; x < 10; x++) {
            System.out.printf("smoothstep(%f, 0.4, 0.6) = %f%n", (float)x, smoothStep((float)x, 3f, 7f));
        }
    }
}
