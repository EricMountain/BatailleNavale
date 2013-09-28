package fr.les_enry.bataillenavale;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
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
	
	/**
	 * Size to be requested initially for the drawable area.  Dummy value, since 
	 * the whole idea is that the UI will tell us what size we can be.
	 */
	private static final int DUMMY_INITIAL_SIZE = 10;
	
	// TODO Make enum
	static final int NEUTRAL = 0;
	static final int HIT = 1;
	static final int MISS = 2;
	static final int SHIP = 3;
	static final int SUNK = 4;

	private static final Map<Integer,Integer> state2Colour;
	
	/**
	 * Area we draw into.
	 */
	private ShapeDrawable drawableArea;

	private static int NOT_SET;
	private int width = NOT_SET;
	private int height = NOT_SET;
	
	/**
	 * Cell on the game board.
	 */
	private final Cell cell;
	
	/**
	 * Current cell state (neutral, hit, miss...).
	 */
	private int state = 0;
	
	static {
		Map<Integer,Integer> tmp = new HashMap<Integer,Integer>();
		tmp.put(NEUTRAL, Color.GRAY);
		tmp.put(HIT, Color.RED);
		tmp.put(MISS, Color.BLUE);
		tmp.put(SHIP, Color.GREEN);
		tmp.put(SUNK, Color.MAGENTA);
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
		
		//TODO Do this later in the game?  Is there a callback that can be used after
		// measurement takes place?
		drawableArea = new ShapeDrawable(new RectShape());
		drawableArea.getPaint().setColor(getColour());
		drawableArea.setBounds(PADDING, PADDING, width - PADDING, height - PADDING);
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
		setState(NEUTRAL);
	}
	
	/**
	 * Sets the cell's state.
	 * 
	 * @param newState New state.
	 */
	void setState(int newState) {
		state = newState;
		drawableArea.getPaint().setColor(getColour());
		invalidate();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		drawableArea.draw(canvas);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		
		/* OLD: Next 2 work. */
//		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//		setMeasuredDimension(x + width, y + height);

//		Log.d(TAG, "onMeasure: modeW: "
//				+ Util.measureSpecModeToString(widthMeasureSpec)
//				+ "/" + MeasureSpec.getSize(widthMeasureSpec)
//				+ ", modeH: "
//				+ Util.measureSpecModeToString(heightMeasureSpec)
//				+ "/" + MeasureSpec.getSize(heightMeasureSpec));
		
		width = computeSize(widthMeasureSpec);
		//height = computeSize(heightMeasureSpec);
		height = width;
		
		setMeasuredDimension(width, height);
		
		drawableArea.setBounds(PADDING, PADDING, width - PADDING, height - PADDING);
	}

	/**
	 * Computes height/width of a cell based on input measure spec.
	 * 
	 * @param measureSpec
	 */
	private int computeSize(int measureSpec) {
		int size = Integer.MIN_VALUE;
		
		// Figure out width
		switch (MeasureSpec.getMode(measureSpec)) {
		case MeasureSpec.UNSPECIFIED:
			size = DUMMY_INITIAL_SIZE; // TODO Fix this dummy value
			break;
		case MeasureSpec.AT_MOST:
		case MeasureSpec.EXACTLY:
			size = MeasureSpec.getSize(measureSpec);
		}
		
		return size;
	}

	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		//TODO fix event handling
		// For the time being, only deal with UP events
		int action = event.getActionMasked();
		Log.d(TAG, "MotionEvent: " + event);
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
    			setState(SHIP);

    			// Automatically update boat to place
    			Player player2 = gameState.testNextToPlace();
    			if (player2 != null) {
    				if (player2 == player) {
    					player2 = gameState.getNextToPlace();
    					((BatailleNavale) getContext()).actionTextView.setText(player2.getName() + " place " + player2.getShipToPlace());	
    				} else {
    					displayToast("Press OK so next player can place their ships.");
    					((BatailleNavale) getContext()).actionTextView.setText(player.getName() + " ships placed.  Press OK.");
    				}
    			} else {
    				displayToast("Press OK to start game.");
    			}
    		} catch (BadPlacementException e) {
    			displayToast("Can't place the ship there.");
    		}
    	} else {
    		Shot shot = new Shot(cell.getRow(), cell.getColumn());

    		Player current = gameState.getCurrentPlayer();
    		Player opponent = gameState.getOpponent();
    		//Shot shot = new Shot(coordinates.getRow(), coordinates.getColumn());
    		try {
    			//Ship shipHit = gameState.getCurrentPlayer().fireShot(shot, opponent);
    			Ship shipHit = gameState.fireShot(shot);
    			Log.d(TAG, (shipHit == null ? "Shot was a miss" : "Shot hit ship " + shipHit));

    			if (shipHit != null) {
    				if (shipHit.checkSunk()) {
    					setState(SUNK);
    					gameState.updateCellsWithPlayerShots(gameState.getCurrentPlayer());
    					displayToast(shipHit + " sunk!");
    				} else {
    					setState(HIT);
    					displayToast(shipHit + " hit!");
    				}
    			} else {
    				setState(MISS);
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
