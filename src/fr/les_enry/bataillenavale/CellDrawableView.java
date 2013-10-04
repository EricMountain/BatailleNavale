package fr.les_enry.bataillenavale;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

// TODO implement missing constructor(s)
@SuppressLint("ViewConstructor")
public class CellDrawableView extends View {

	/**
	 * Logging tag.
	 */
	private static final String TAG = "CellDrawableView";

	/**
	 * Padding to apply around the rectangular area that will be coloured.
	 */
	private static final int PADDING = 1;
	
	/** Maps a state to the colour used to represent it. */ 
	private static final Map<CellState,Integer> state2Colour;
	
	/**
	 * Area we draw into.
	 */
	private ShapeDrawable drawableArea;

	/** Constant for width/height not set. */
	private static final int NOT_SET = 10;
	
	/** Drawable area width. */
	private int width = NOT_SET;
	
	/** Drawable area height. */
	private int height = NOT_SET;
	
	/**
	 * Cell on the game board.
	 */
	private final Cell cell;
	
	enum CellState {NEUTRAL, HIT, MISS, SHIP, SUNK};
	
	/**
	 * Current cell state (neutral, hit, miss...).
	 */
	private CellState state = CellState.NEUTRAL;
	
	static {
		Map<CellState,Integer> tmp = new HashMap<CellState,Integer>();
		tmp.put(CellState.NEUTRAL, Color.GRAY);
		tmp.put(CellState.HIT, Color.RED);
		tmp.put(CellState.MISS, Color.BLUE);
		tmp.put(CellState.SHIP, Color.GREEN);
		tmp.put(CellState.SUNK, Color.MAGENTA);
		state2Colour = Collections.unmodifiableMap(tmp);
	}
	
	
	/**
	 * Constructor.
	 * 
	 * @param context Process context.
	 * @param row Row index of this cell in the board's matrix.
	 * @param col Column index of this cell in the board's matrix.
	 */
	public CellDrawableView(Context context, int row, int col) {
		super(context);

		this.cell = new Cell(row, col);
		
		drawableArea = new ShapeDrawable(new RectShape());
		drawableArea.getPaint().setColor(getColour());
	}

	/**
	 * Gets the colour to paint the rectangle depending on state.
	 * 
	 * @return Colour.
	 */
	private int getColour() {
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
	 * @param newState New state.
	 */
	void setState(CellState newState) {
		state = newState;
		drawableArea.getPaint().setColor(getColour());
		invalidate();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		drawableArea.draw(canvas);
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		
		drawableArea.setBounds(PADDING, PADDING, getWidth() - PADDING, getHeight() - PADDING);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int newWidth = computeSize(widthMeasureSpec, true);
		int newHeight = computeSize(heightMeasureSpec, false);

		width = height = Math.min(newWidth, newHeight);
		
		//Log.d(TAG, "New size: " + width + ", " + height + " - " + cell);
				
		int newWidthMeasureSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getMode(widthMeasureSpec), width);
		int newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getMode(heightMeasureSpec), height);

		super.onMeasure(newWidthMeasureSpec, newHeightMeasureSpec);
	}

	/**
	 * Computes height/width of a cell based on input measure spec.
	 * 
	 * @param measureSpec
	 * @param isWidth True if computing size of width, false if height.
	 */
	private int computeSize(int measureSpec, boolean isWidth) {
		int size = Integer.MIN_VALUE;
		
		switch (MeasureSpec.getMode(measureSpec)) {
		case MeasureSpec.UNSPECIFIED:
			size = (isWidth ? this.getSuggestedMinimumWidth() : this.getSuggestedMinimumHeight());
			break;
		case MeasureSpec.AT_MOST:
		case MeasureSpec.EXACTLY:
			size = MeasureSpec.getSize(measureSpec);
			break;
		default:
			Log.e(TAG, "Unknown MeasureSpec mode: " + MeasureSpec.getMode(measureSpec));
		}
		
		return size;
	}

	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		//TODO fix event handling
		// For the time being, only deal with "up" events
		int action = event.getActionMasked();
//		Log.d(TAG, "MotionEvent: " + event);
		if (action != MotionEvent.ACTION_UP
				&& action != MotionEvent.ACTION_POINTER_UP) {
			return true;
		}
		
		GameState gameState = GameState.getGameState();
    	
		//TODO Refactor and simplify all this
    	Player player = gameState.getNextToPlace();
    	if (player != null) {
    		try {
    			player.addShipToCell(cell);
    			setState(CellState.SHIP);

    			// Automatically update boat to place
    			Player player2 = gameState.testNextToPlace();
    			if (player2 != null && player2 == player) {
    				player2 = gameState.getNextToPlace();
    				((BatailleNavale) getContext()).actionTextView.setText(player2.getName() + " place " + player2.getShipToPlace());	
    			} else {
    				displayToast("Press OK when ready.");
    				((BatailleNavale) getContext()).actionTextView.setText(player.getName() + " ships placed.  Press OK.");
    			}
    		} catch (BadPlacementException e) {
    			displayToast("Can't place the ship there.");
    		}
    	} else {
    		Shot shot = new Shot(cell.getRow(), cell.getColumn());

    		Player current = gameState.getCurrentPlayer();
    		Player opponent = gameState.getOpponent();
    		try {
    			Ship shipHit = gameState.fireShot(shot);
    			Log.d(TAG, (shipHit == null ? "Shot was a miss" : "Shot hit ship " + shipHit));

    			if (shipHit != null) {
    				if (shipHit.checkSunk()) {
    					setState(CellState.SUNK);
    					gameState.updateCellsWithPlayerShots(gameState.getCurrentPlayer());
    					displayToast(shipHit + " sunk!");
    				} else {
    					setState(CellState.HIT);
    					displayToast(shipHit + " hit!");
    				}
    			} else {
    				setState(CellState.MISS);
    				displayToast("Missed!");
    			}

    			if (opponent.checkLost()) {
    				Log.d(TAG, opponent + " has lost");

    				displayToast(current.getName() + " has won!");
    				((BatailleNavale) getContext()).actionTextView.setText(current.getName() + " won.  Game over.");
    			} else {
    				((BatailleNavale) getContext()).actionTextView.setText(current.getName() + " turn complete ");
    			}
    		} catch (AlreadyPlayedShotException e) {
    			displayToast("Shot already played.");
    		} catch (AlreadyPlayedException e) {
    			displayToast("Already played, it's " + gameState.getOpponent() + "'s turn.");
    		} catch (CantShootHereException e) {
    			displayToast("You can't shoot yourself!");
			}
    	}
		
		return true;
	}

	/**
	 * Display a toast.
	 * 
	 * @param text Text to display.
	 */
	private void displayToast(CharSequence text) {
		Context context = getContext().getApplicationContext();
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}

	/**
	 * Gets the cell this represents on the game board.
	 * 
	 * @return Coordinates.
	 */
	Cell getCell() {
		return cell;
	}

}
