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
     * Check value is in the given range and return a smoothed value of 1 if inside, 0 if outside.
     * @param value the value to check against the range
     * @param begin the range min value
     * @param end the range max value
     * @param smoothBegin a relative value (0-1) for the smoothing at the min value
     * @param smoothEnd a relative value (0-1) for the smoothing at the max value
     * @return the smoothed value between 0 and 1
     */
    public static float smoothInRange(float value, float begin, float end, float smoothBegin, float smoothEnd) {
        float midPoint = (end - begin) / 2 + begin;
        if (value < midPoint)
            return smoothStep(value, begin - smoothBegin, begin + smoothBegin);
        return 1f - smoothStep(value, end - smoothEnd, end + smoothEnd);
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
}
