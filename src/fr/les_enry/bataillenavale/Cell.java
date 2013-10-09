package fr.les_enry.bataillenavale;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

/**
 * Simple cell.
 */
class Cell {
	/** Tag for logging. */
	private static final String TAG = "Cell";

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

	void handleTouchEvent(View view) {
		Log.d(TAG, "touched " + this);
		if (!placeShipOnThisCell(view))
			takeShot(view);
	}

	/**
	 * Takes a shot on this cell.
	 */
	private void takeShot(View view) {
		GameState gameState = GameState.getGameState();

		Shot shot = new Shot(getRow(), getColumn());

		Player current = gameState.getCurrentPlayer();
		Player opponent = gameState.getOpponent();
		try {
			Ship shipHit = gameState.fireShot(shot);
			Log.d(TAG, (shipHit == null ? "Shot was a miss" : "Shot hit ship "
					+ shipHit));

			if (shipHit != null) {
				if (shipHit.checkSunk()) {
					setState(CellState.SUNK);
					gameState.updateCellsWithPlayerShots(gameState
							.getCurrentPlayer());
					displayToast(view, shipHit + " sunk!");
				} else {
					setState(CellState.HIT);
					displayToast(view, shipHit + " hit!");
				}
			} else {
				setState(CellState.MISS);
				displayToast(view, "Missed!");
			}

			if (opponent.checkLost()) {
				Log.d(TAG, opponent + " has lost");

				displayToast(view, current.getName() + " has won!");
				((BatailleNavale) view.getContext()).actionTextView
						.setText(current.getName() + " won.  Game over.");
			} else {
				((BatailleNavale) view.getContext()).actionTextView
						.setText(current.getName() + " turn complete ");
			}
		} catch (AlreadyPlayedShotException e) {
			 displayToast(view, "Shot already played.");
		} catch (AlreadyPlayedException e) {
			 displayToast(view, "Already played, it's " +
			 gameState.getOpponent()
			 + "'s turn.");
		} catch (CantShootHereException e) {
			 displayToast(view, "You can't shoot yourself!");
		}
	}

	/**
	 * Checks if we are placing ships, and places part of a boat of this cell if
	 * it is the case.
	 * 
	 * @return true if we're placing ships.
	 */
	private boolean placeShipOnThisCell(View view) {
		GameState gameState = GameState.getGameState();
		Player player = gameState.getNextToPlace();

		if (player != null) {
			try {
				player.addShipToCell(this);
				setState(CellState.SHIP);

				// Automatically update boat to place
				Player player2 = gameState.testNextToPlace();
				if (player2 != null && player2 == player) {
					player2 = gameState.getNextToPlace();
					((BatailleNavale) view.getContext()).actionTextView
							.setText(player2.getName() + " place "
									+ player2.getShipToPlace());
				} else {
					displayToast(view, "Press OK when ready.");
					((BatailleNavale) view.getContext()).actionTextView
							.setText(player.getName()
									+ " ships placed.  Press OK.");
				}
			} catch (BadPlacementException e) {
				displayToast(view, "Can't place the ship there.");
			}

			return true;
		} else
			return false;
	}

	/**
	 * Display a toast.
	 * 
	 * @param text
	 *            Text to display.
	 */
	private void displayToast(View view, CharSequence text) {
		Context context = view.getContext().getApplicationContext();
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}

	void draw(Canvas canvas, int x1, int y1, int x2, int y2) {
		Paint paint = new Paint();
		paint.setColor(getColour());
		paint.setStyle(Style.FILL);
		canvas.drawRect(new Rect(x1 + 1, y1 + 1, x2 - 1, y2 - 1), paint);
	}
}
