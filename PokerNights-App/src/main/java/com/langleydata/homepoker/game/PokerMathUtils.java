package com.langleydata.homepoker.game;

public final class PokerMathUtils {
	static final float COMPARED_THRESHOLD = 0.01f;
	
	/** Is the value within 0.001 of being zero?
	 * 
	 * @param a
	 * @return
	 */
	public static boolean floatEqualsZero(float a) {
		return floatEquals(a, 0f, 0.001f);
	}
	/** Compare two floats to within a threshold value of 0.01
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean floatEquals(double a, double b) {
		return floatEquals(a, b, COMPARED_THRESHOLD);
	}
	/** Compare two floats to within a threshold value of 0.01
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean floatEquals(float a, float b) {
		return floatEquals(a, b, COMPARED_THRESHOLD);
	}
	/** Compare two floats to within a threshold value of 0.01
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean floatEquals(double a, double b, float threshold) {
		return Math.abs(a-b) < threshold;
	}
	/** Compare two floats to within a threshold value of 0.01
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean floatEquals(float a, float b, float threshold) {
		return Math.abs(a-b) < threshold;
	}
	
	/** Used for float comparisons - round to 2 decimal places
	 * 
	 * @param value
	 * @return
	 */
	public static float rd(final float value) {
		return Math.round(value * 100) / 100f;
	}
	/** Used for float comparisons - round to 2 decimal places
	 * 
	 * @param value
	 * @return
	 */
	public static float rd(final double value) {
		return Math.round(value * 100) / 100f;
	}
}
