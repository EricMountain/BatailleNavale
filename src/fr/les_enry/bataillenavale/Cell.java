package fr.les_enry.bataillenavale;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

//import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;

/**
 * Simple cell.
 */
class Cell implements Serializable {
	
	/**
	 * Serialisation version.
	 */
	private static final long serialVersionUID = -2002875558670694473L;

	/** Maps a state to the colour used to represent it. */
	private static final Map<CellState, Integer> state2Colour;

	enum CellState {
		NEUTRAL, HIT, MISS, SHIP, SUNK
	};

	/**
	 * Current cell state (neutral, hit, miss...).
	 */
	private CellState state = CellState.NEUTRAL;

	static {
		Map<CellState, Integer> tmp = new HashMap<CellState, Integer>();
		tmp.put(CellState.NEUTRAL, Color.GRAY);
		tmp.put(CellState.HIT, Color.RED);
		tmp.put(CellState.MISS, Color.BLUE);
		tmp.put(CellState.SHIP, Color.GREEN);
		tmp.put(CellState.SUNK, Color.MAGENTA);
		state2Colour = Collections.unmodifiableMap(tmp);
	}

	/** Row coordinate. */
	private final int row;

	/** Colmun coordinate. */
	private final int column;

	/**
	 * Constuctor.
	 * 
	 * @param row
	 * @param column
	 */
	Cell(int row, int column) {
		this.row = row;
		this.column = column;
	}

	/**
	 * Get row.
	 * 
	 * @return row
	 */
	public int getRow() {
		return row;
	}

	/**
	 * Get column.
	 * 
	 * @return column.
	 */
	public int getColumn() {
		return column;
	}

	@Override
	public boolean equals(Object otherObj) {
		Cell other = (Cell) otherObj;
		return other.row == this.row && other.column == this.column;
	}

	@Override
	public String toString() {
		return "(" + row + "," + column + ")";
	}

	/**
	 * Gets the colour to paint the rectangle depending on state.
	 * 
	 * @return Colour.
	 */
	int getColour() {
		return state2Colour.get(state);
	}

	/**
	 * Resets the cell's state to neutral.
	 */
	void reset() {
		setState(CellState.NEUTRAL);
	}

	/**
	 * Sets the cell's state.
	 * 
	 * @param newState
	 *            New state.
	 */
	void setState(CellState newState) {
		state = newState;
	}

	void draw(Canvas canvas, int x1, int y1, int x2, int y2) {
		Paint paint = new Paint();
		paint.setColor(getColour());
		paint.setStyle(Style.FILL);
		canvas.drawRect(new Rect(x1 + 1, y1 + 1, x2 - 1, y2 - 1), paint);
	}
}
