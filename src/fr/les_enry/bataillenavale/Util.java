package fr.les_enry.bataillenavale;

import android.view.View.MeasureSpec;

/**
 * Utility methods.
 */
class Util {

	/**
	 * Converts a MeasureSpec to human readable string.
	 * 
	 * @param measureSpec MeasureSpec to interpret.
	 * @return Human-readable version.
	 */
	static String measureSpecModeToString(int measureSpec) {
		switch (MeasureSpec.getMode(measureSpec)) {
		case MeasureSpec.UNSPECIFIED:
			return "UNSPECIFIED";
		case MeasureSpec.AT_MOST:
			return "AT_MOST";
		case MeasureSpec.EXACTLY:
			return "EXACTLY";
		default: 
			return "UNKNOWN";
		}
	}
}
