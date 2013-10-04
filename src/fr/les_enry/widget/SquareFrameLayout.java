package fr.les_enry.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View.MeasureSpec;
import android.widget.FrameLayout;

/**
 * Makes a square FrameLayout.
 */
public class SquareFrameLayout extends FrameLayout {

	/** Log identifier. */
	private static final String TAG = "SquareFrameLayout";

	public SquareFrameLayout(Context context) {
		super(context);
	}

	public SquareFrameLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SquareFrameLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final int size;
		final int mode = MeasureSpec.getMode(widthMeasureSpec); // Assume both modes identical
		switch (mode) {
		case MeasureSpec.UNSPECIFIED:
			size = Math.min(this.getSuggestedMinimumWidth(), this.getSuggestedMinimumHeight());
			break;
		case MeasureSpec.AT_MOST:
		case MeasureSpec.EXACTLY:
			size = Math.min(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
			break;
		default:
			Log.e(TAG, "Unknown MeasureSpec mode: " + MeasureSpec.getMode(widthMeasureSpec));
			size = Math.min(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
		}

		final int squareSpec = MeasureSpec.makeMeasureSpec(mode, size);
		super.onMeasure(squareSpec, squareSpec);
	}

}
