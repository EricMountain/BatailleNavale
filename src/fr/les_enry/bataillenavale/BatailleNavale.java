package fr.les_enry.bataillenavale;

import org.acra.ACRA;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import fr.les_enry.util.fsm.Action;
import fr.les_enry.util.fsm.Event;
import fr.les_enry.util.fsm.FSM;
import fr.les_enry.util.fsm.State;

//TODO Bluetooth game
//TODO Make menu work on devices that don't have a menu button.
//TODO Automated tests
//TODO Serialise to persistent storage?
//TODO see how to grey disabled elements
//TODO Nicer buttons
//TODO Ship placement like Blokish?  Drawing with your finger is nice too though...  Better event handling needed anyway.
//TODO Animations
//TODO Setup (sound on/off, animations on/off)
//TODO High scores
//TODO Sounds

/**
 * Entry point activity.
 * 
 */
public class BatailleNavale extends FragmentActivity implements
		ResetDialogFragment.ResetDialogListener {

	private static final String ACTION_TEXT = "ActionText";
	private static final String VIEW_OWN_CHECKED = "ViewOwnChecked";
	private static final String VIEW_OWN_CLICKABLE = "ViewOwnClickable";
	private static final String GRID_CLICKABLE = "GridClickable";
	private static final String ACTION_BUTTON_CLICKABLE = "ActionButtonClickable";
	private static final String FSM_STATE = "FSMState";
	private static final String GAME_STATE = "GameState";
	private static final String CURRENT_BG_COLOUR = "CurrentBgColour";

	/**
	 * Implements a square layout.
	 */
	private final class SquareLayout extends FrameLayout {
		/** Number of pixels between rows. */
		private int rowStep = 0;

		/** Number of pixels between columns. */
		private int colStep = 0;

		/** Pre-allocated painter. */
		private final Paint boardPaint;

		/**
		 * Pre-allocated object for calls to canvas.getClipBounds(Rect).
		 */
		private Rect viewPort = new Rect();

		/**
		 * Constuctor.
		 * 
		 * @param context
		 */
		private SquareLayout(Context context) {
			super(context);

			boardPaint = new Paint();
			boardPaint.setColor(Color.DKGRAY);
			boardPaint.setStrokeWidth(1.3f);
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			final int size;
			final int mode = MeasureSpec.getMode(widthMeasureSpec); // Assume
																	// both
																	// modes
																	// identical
			switch (mode) {
			case MeasureSpec.UNSPECIFIED:
				size = Math.min(this.getSuggestedMinimumWidth(),
						this.getSuggestedMinimumHeight());
				break;
			case MeasureSpec.AT_MOST:
			case MeasureSpec.EXACTLY:
				size = Math.min(MeasureSpec.getSize(widthMeasureSpec),
						MeasureSpec.getSize(heightMeasureSpec));
				break;
			default:
				Log.e(TAG,
						"Unknown MeasureSpec mode: "
								+ MeasureSpec.getMode(widthMeasureSpec));
				size = Math.min(MeasureSpec.getSize(widthMeasureSpec),
						MeasureSpec.getSize(heightMeasureSpec));
			}

			final int squareSpec = MeasureSpec.makeMeasureSpec(mode, size);

			super.onMeasure(squareSpec, squareSpec);
		}

		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);

			canvas.getClipBounds(viewPort);
			Log.d(TAG, canvas.getWidth() + "," + canvas.getHeight() + " - "
					+ viewPort);

			// Draw grid
			int right = viewPort.right - viewPort.right % GameState.NB_COLS;
			int bottom = viewPort.bottom - viewPort.bottom % GameState.NB_ROWS;
			rowStep = viewPort.bottom / GameState.NB_ROWS;
			colStep = viewPort.right / GameState.NB_COLS;

			// Draw horizontal lines
			for (int y = 0; y <= bottom; y += rowStep) {
				canvas.drawLine(0, y, right, y, boardPaint);
			}

			// Draw vertical lines
			for (int x = 0; x <= right; x += colStep) {
				canvas.drawLine(x, 0, x, bottom, boardPaint);
			}

			for (Cell c : gameState.getBoardState().getBoard()) {
				c.draw(canvas, c.getColumn() * colStep, c.getRow() * rowStep,
						(c.getColumn() + 1) * colStep, (c.getRow() + 1)
								* rowStep);
			}
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			// TODO fix event handling to handle "clicks"

			// For the time being, only deal with "up" events
			int action = event.getActionMasked();
			Log.d(TAG, "MotionEvent: " + event);

			if (action == MotionEvent.ACTION_UP
					|| action == MotionEvent.ACTION_POINTER_UP) {

				if (!this.isClickable()) {
					Log.d(TAG, this + " is not clickable");
					return true;
				} else {
					Log.d(TAG, this + " is clickable");
				}

				float x = event.getX();
				float y = event.getY();
				int row = (int) Math.floor(y / rowStep);
				int col = (int) Math.floor(x / colStep);
				Log.d(TAG, "Touch at: " + x + "," + y + " cell: " + row + ","
						+ col + " steps: " + rowStep + "," + colStep);

				if (row >= GameState.NB_ROWS || col >= GameState.NB_COLS)
					Log.d(TAG, "Touch is out of bounds, ignoring");
				else
					gameState.processEvent(gameState.CELL_ACTIVATED, row, col);

				this.invalidate();
			}

			return true;
		}
	}

	/**
	 * Logging tag.
	 */
	private static final String TAG = "BatailleNavale";

	private final FSM fsm = new FSM("BatailleNavale");
	private final State INIT = fsm.state("Initial");
	private final State P1_PLACE_BOAT = fsm.state("Player 1 placing boat");
	private final State P2_PLACE_BOAT = fsm.state("Player 2 placing boat");
	private final State P1_BOATS_PLACED = fsm.state("Player 1 boats placed");
	private final State P2_BOATS_PLACED = fsm.state("Player 2 boats placed");
	private final State P1_TAKE_SHOT = fsm.state("Player 1 taking shot");
	private final State P2_TAKE_SHOT = fsm.state("Player 2 taking shot");
	private final State P1_SHOT_TAKEN = fsm.state("Player 1 shot taken");
	private final State P2_SHOT_TAKEN = fsm.state("Player 2 shot taken");
	private final State P1_OWN_BOATS = fsm.state("Player 1 own boats");
	private final State P2_OWN_BOATS = fsm.state("Player 2 own boats");
	private final State P1_OWN_BOATS_ST = fsm
			.state("Player 1 own boats (shot taken)");
	private final State P2_OWN_BOATS_ST = fsm
			.state("Player 2 own boats (shot taken)");
	private final State GAME_OVER = fsm.state("Game over");
	private final State OWN_BOATS_GO = fsm.state("Own boats (game over)");

	final Event START = fsm.event("Start");
	final Event ALL_BOATS_PLACED = fsm.event("All boats placed");
	final Event P2_START_PLACING = fsm.event("P2 to start placing boats");
	final Event P1_TO_FIRE = fsm.event("P1 to take shot");
	final Event P2_TO_FIRE = fsm.event("P2 to take shot");
	final Event SHOT_FIRED = fsm.event("Shot fired");
	final Event WON = fsm.event("Player won!");
	final Event SEE_OWN_BOATS_TOGGLE = fsm.event("Toggle own boats view");
	final Event RESET = fsm.event("Reset game");

	/** Game state. */
	private GameState gameState = null;

	/** Square frame layout. */
	private FrameLayout squareLayout = null;

	/** Current background colour. */
	private int currentBackgroundColour = 0xff000000;

	private class ResetAction extends Action {

		private static final long serialVersionUID = 1292901524013404589L;

		public boolean act() {
			gameState.processEvent(gameState.RESET);
			return true;
		}
	}

	private class ToggleViewOwnBoatsAction extends Action {

		private static final long serialVersionUID = -5644547148071671339L;

		public boolean act(Object... isChecked) {
			gameState.getBoardState().updateCells((Boolean) isChecked[0]);
			squareLayout.invalidate();

			return true;
		}
	}

	private final ResetAction resetAction = new ResetAction();
	private final ToggleViewOwnBoatsAction toggleViewOwnBoatsAction = new ToggleViewOwnBoatsAction();

	@SuppressWarnings("serial")
	private void initFSM() {
		fsm.reset();

		fsm.rule().initial(INIT).event(START).ok(P1_PLACE_BOAT)
				.action(new Action() {
					public boolean act(Object... rowColumn) {
						// TODO Why soft event?
						gameState.processSoftEvent(gameState.START);
						return updateUiForBoatPlacement();
					}
				});
		fsm.rule().initial(P1_PLACE_BOAT).event(ALL_BOATS_PLACED)
				.ok(P1_BOATS_PLACED).action(new Action() {
					public boolean act() {
						displayToast("Press OK when ready.");
						setActionText("Ships placed.  Press OK.");
						squareLayout.setClickable(false);
						actionButtonSetClickable(true);
						return true;
					}
				});
		fsm.rule().initial(P1_BOATS_PLACED).event(P2_START_PLACING)
				.ok(P2_PLACE_BOAT).action(new Action() {
					public boolean act() {
						return updateUiForBoatPlacement();
					}
				});
		fsm.rule().initial(P2_PLACE_BOAT).event(ALL_BOATS_PLACED)
				.ok(P2_BOATS_PLACED).action(new Action() {
					public boolean act() {
						displayToast("Press OK when ready.");
						setActionText("Ships placed.  Press OK.");
						squareLayout.setClickable(false);
						actionButtonSetClickable(true);
						return true;
					}
				});
		fsm.rule().initial(P2_BOATS_PLACED).event(P1_TO_FIRE).ok(P1_TAKE_SHOT)
				.action(new Action() {
					public boolean act() {
						return updateUiForPlayerShot();
					}
				});
		fsm.rule().initial(P1_TAKE_SHOT).event(SHOT_FIRED).ok(P1_SHOT_TAKEN)
				.action(new Action() {
					public boolean act(Object... args) {
						setActionText((String) args[0]);
						squareLayout.setClickable(false);
						actionButtonSetClickable(true);
						return true;
					}
				});
		fsm.rule().initial(P1_TAKE_SHOT).event(WON).ok(GAME_OVER)
				.action(new Action() {
					public boolean act(Object... args) {
						setActionText((String) args[0]);
						displayToast((String) args[0]);
						squareLayout.setClickable(false);
						actionButtonSetClickable(true);
						return true;
					}
				});
		fsm.rule().initial(P1_SHOT_TAKEN).event(P2_TO_FIRE).ok(P2_TAKE_SHOT)
				.action(new Action() {
					public boolean act() {
						return updateUiForPlayerShot();
					}
				});

		fsm.rule().initial(P2_TAKE_SHOT).event(SHOT_FIRED).ok(P2_SHOT_TAKEN)
				.action(new Action() {
					public boolean act(Object... args) {
						setActionText((String) args[0]);
						squareLayout.setClickable(false);
						actionButtonSetClickable(true);
						return true;
					}
				});
		fsm.rule().initial(P2_TAKE_SHOT).event(WON).ok(GAME_OVER)
				.action(new Action() {
					public boolean act(Object... args) {
						setActionText((String) args[0]);
						displayToast((String) args[0]);
						squareLayout.setClickable(false);
						actionButtonSetClickable(true);
						return true;
					}
				});
		fsm.rule().initial(P2_SHOT_TAKEN).event(P1_TO_FIRE).ok(P1_TAKE_SHOT)
				.action(new Action() {
					public boolean act() {
						return updateUiForPlayerShot();
					}
				});

		fsm.rule().initial(P1_TAKE_SHOT).event(SEE_OWN_BOATS_TOGGLE)
				.ok(P1_OWN_BOATS).action(toggleViewOwnBoatsAction);
		fsm.rule().initial(P2_TAKE_SHOT).event(SEE_OWN_BOATS_TOGGLE)
				.ok(P2_OWN_BOATS).action(toggleViewOwnBoatsAction);
		fsm.rule().initial(P1_OWN_BOATS).event(SEE_OWN_BOATS_TOGGLE)
				.ok(P1_TAKE_SHOT).action(toggleViewOwnBoatsAction);
		fsm.rule().initial(P2_OWN_BOATS).event(SEE_OWN_BOATS_TOGGLE)
				.ok(P2_TAKE_SHOT).action(toggleViewOwnBoatsAction);

		fsm.rule().initial(P1_SHOT_TAKEN).event(SEE_OWN_BOATS_TOGGLE)
				.ok(P1_OWN_BOATS_ST).action(toggleViewOwnBoatsAction);
		fsm.rule().initial(P2_SHOT_TAKEN).event(SEE_OWN_BOATS_TOGGLE)
				.ok(P2_OWN_BOATS_ST).action(toggleViewOwnBoatsAction);
		fsm.rule().initial(P1_OWN_BOATS_ST).event(SEE_OWN_BOATS_TOGGLE)
				.ok(P1_SHOT_TAKEN).action(toggleViewOwnBoatsAction);
		fsm.rule().initial(P2_OWN_BOATS_ST).event(SEE_OWN_BOATS_TOGGLE)
				.ok(P2_SHOT_TAKEN).action(toggleViewOwnBoatsAction);

		fsm.rule().initial(OWN_BOATS_GO).event(SEE_OWN_BOATS_TOGGLE)
				.ok(GAME_OVER).action(toggleViewOwnBoatsAction);
		fsm.rule().initial(GAME_OVER).event(SEE_OWN_BOATS_TOGGLE)
				.ok(OWN_BOATS_GO).action(toggleViewOwnBoatsAction);

		fsm.rule().initial(INIT).event(SEE_OWN_BOATS_TOGGLE).ok(INIT)
				.action(toggleViewOwnBoatsAction);

		// Reset from any state to init
		fsm.rule().initial(INIT).event(RESET).ok(INIT);
		fsm.rule().initial(P1_PLACE_BOAT).event(RESET).ok(INIT)
				.action(resetAction);
		fsm.rule().initial(P2_PLACE_BOAT).event(RESET).ok(INIT)
				.action(resetAction);
		fsm.rule().initial(P1_TAKE_SHOT).event(RESET).ok(INIT)
				.action(resetAction);
		fsm.rule().initial(P2_TAKE_SHOT).event(RESET).ok(INIT)
				.action(resetAction);
		fsm.rule().initial(P1_OWN_BOATS).event(RESET).ok(INIT)
				.action(resetAction);
		fsm.rule().initial(P2_OWN_BOATS).event(RESET).ok(INIT)
				.action(resetAction);
		fsm.rule().initial(P1_BOATS_PLACED).event(RESET).ok(INIT)
				.action(resetAction);
		fsm.rule().initial(P2_BOATS_PLACED).event(RESET).ok(INIT)
				.action(resetAction);
		fsm.rule().initial(P1_SHOT_TAKEN).event(RESET).ok(INIT)
				.action(resetAction);
		fsm.rule().initial(P2_SHOT_TAKEN).event(RESET).ok(INIT)
				.action(resetAction);
		fsm.rule().initial(P1_OWN_BOATS_ST).event(RESET).ok(INIT)
				.action(resetAction);
		fsm.rule().initial(P2_OWN_BOATS_ST).event(RESET).ok(INIT)
				.action(resetAction);
		fsm.rule().initial(GAME_OVER).event(RESET).ok(INIT).action(resetAction);
		fsm.rule().initial(OWN_BOATS_GO).event(RESET).ok(INIT)
				.action(resetAction);

		fsm.start(INIT);

		Log.d(TAG, fsm.toDag());
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(TAG, "onCreate");

		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
						| WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_bataille_navale);

		initFSM();

		FrameLayout frameLayout = (FrameLayout) findViewById(R.id.FrameLayout);
		FrameLayout.LayoutParams squareLayoutParams = new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

		squareLayout = new SquareLayout(this);
		// TODO Why is this nec.? Why doesn't invalidate() do the job?
		squareLayout.setBackgroundColor(Color.BLACK);
		// squareLayout.invalidate();

		Log.d(TAG,
				"squareLayout is clickable after creation: "
						+ squareLayout.isClickable());

		frameLayout.addView(squareLayout, squareLayoutParams);

		Log.d(TAG,
				"onCreate, before setting onClickListener: action button clickable: "
						+ findViewById(R.id.actionButton).isClickable());
		Button actionButton = (Button) findViewById(R.id.actionButton);
		actionButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				handleActionButtonClick(v);
			}
		});

		Log.d(TAG,
				"onCreate, after setting onClickListener: action button clickable: "
						+ findViewById(R.id.actionButton).isClickable());

		boolean isRestoredGame = setupGameState(savedInstanceState);

		viewOwnBoatsCheckBoxSetOnCheckedListener();

		if (!isRestoredGame)
			fsm.event(RESET);

		Log.d(TAG, "FSM state on create: " + fsm.getState());

		// Start ship placement sequence unless game is already in progress
		if (fsm.isState(INIT))
			fsm.event(START);

		Log.d(TAG, "onCreate end");
	}

	/**
	 * Restores saved state if any. If there is no state to restore, or if the
	 * restore fails, creates a new GameState and onCreate will subsequently
	 * reset the state of the UI and FSMs.
	 * 
	 * @param savedInstanceState
	 * @return true if state was restored successfully.
	 */
	private boolean setupGameState(Bundle savedInstanceState) {
		boolean isRestoredGame = false;

		gameState = null;

		// Restore previous state if any. Needs to be done before installing
		// viewOwnBoatsCheck.onCheckedListener
		if (savedInstanceState != null) {
			Log.d(TAG, "savedInstanceState: " + savedInstanceState);

			try {
				// Restore UI state
				setActionText(savedInstanceState.getCharSequence(ACTION_TEXT));

				// Ensure the view-own-boats check-box has no listener at this
				// point
				getViewOwnBoatsCheckBox().setOnCheckedChangeListener(null);

				getViewOwnBoatsCheckBox().setChecked(
						savedInstanceState.getBoolean(VIEW_OWN_CHECKED));
				getViewOwnBoatsCheckBox().setClickable(
						savedInstanceState.getBoolean(VIEW_OWN_CLICKABLE));

				squareLayout.setClickable(savedInstanceState
						.getBoolean(GRID_CLICKABLE));

				actionButtonSetClickable(savedInstanceState
						.getBoolean(ACTION_BUTTON_CLICKABLE));

				fsm.start(fsm.findStateByName(savedInstanceState
						.getString(FSM_STATE)));

				setBackgroundColour(savedInstanceState.getInt(
						CURRENT_BG_COLOUR, 0xff000000));

				gameState = (GameState) savedInstanceState
						.getSerializable(GAME_STATE);

				isRestoredGame = true;
				Log.d(TAG,
						"GameState FSM state on restart: "
								+ gameState.getFSMState());

			} catch (Exception e) {
				Log.d(TAG, "Exception caught restoring from bundle", e);
				ACRA.getErrorReporter().handleSilentException(e);
			}
		} else {
			Log.d(TAG, "savedInstanceState is null");

		}

		if (!isRestoredGame)
			gameState = new GameState(this);
		else
			gameState.setBatailleNavale(this);

		return isRestoredGame;
	}

	@Override
	public void onBackPressed() {
		// super.onBackPressed();
		Log.d(TAG, "Back pressed. (disabled default action)");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy");
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "onPause");
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Log.d(TAG, "onSaveInstanceState");

		// Save UI state
		outState.putCharSequence(ACTION_TEXT, getActionText());

		outState.putBoolean(VIEW_OWN_CHECKED, getViewOwnBoatsCheckBox()
				.isChecked());
		outState.putBoolean(VIEW_OWN_CLICKABLE, getViewOwnBoatsCheckBox()
				.isClickable());

		outState.putBoolean(GRID_CLICKABLE, squareLayout.isClickable());

		outState.putBoolean(ACTION_BUTTON_CLICKABLE,
				findViewById(R.id.actionButton).isClickable());

		outState.putString(FSM_STATE, fsm.getState().getName());

		outState.putInt(CURRENT_BG_COLOUR, currentBackgroundColour);

		// Save full game state
		outState.putSerializable(GAME_STATE, gameState);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.d(TAG, "onRestart");
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "onStart");
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(TAG, "onStop");
	}

	/**
	 * Handles OK button logic.
	 * 
	 * @param view
	 */
	void handleActionButtonClick(View view) {

		if (fsm.isStateIn(P1_OWN_BOATS, P1_OWN_BOATS_ST, P2_OWN_BOATS,
				P2_OWN_BOATS_ST))
			viewOwnBoatsCheckBoxClear();

		if (fsm.isState(P1_BOATS_PLACED)) {
			fsm.event(P2_START_PLACING);
		} else if (fsm.isStateIn(P2_BOATS_PLACED, P2_SHOT_TAKEN)) {
			fsm.event(P1_TO_FIRE);
		} else if (fsm.isState(P1_SHOT_TAKEN)) {
			fsm.event(P2_TO_FIRE);
		} else if (fsm.isStateIn(GAME_OVER, OWN_BOATS_GO)) {
			onResetRequest();
		} else {
			throw new RuntimeException("Why are we here? FSM state = "
					+ fsm.getState());
		}

		squareLayout.invalidate();
	}

	void actionButtonSetClickable(boolean isClickable) {
		Button actionButton = (Button) findViewById(R.id.actionButton);
		actionButton.setClickable(isClickable);
		Log.d(TAG, "action button set clickable: " + isClickable);
	}

	/**
	 * Clears the "view own boats" check box.
	 * 
	 * @return
	 */
	private void viewOwnBoatsCheckBoxClear() {
		getViewOwnBoatsCheckBox().setChecked(false);
	}

	private CheckBox getViewOwnBoatsCheckBox() {
		return (CheckBox) findViewById(R.id.ViewOwnCheckBox);
	}

	/**
	 * Enables/disables the "view own boats" check box.
	 * 
	 * @return
	 */
	private void viewOwnBoatsCheckBoxSetClickable(boolean isClickable) {
		getViewOwnBoatsCheckBox().setClickable(isClickable);
	}

	private void viewOwnBoatsCheckBoxSetOnCheckedListener() {
		getViewOwnBoatsCheckBox().setOnCheckedChangeListener(
				new CheckBox.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						fsm.event(SEE_OWN_BOATS_TOGGLE, isChecked);
					}
				});
	}

	void handleResetButtonClick() {
		new ResetDialogFragment().show(this.getSupportFragmentManager(), null);
	}

	void setActionText(CharSequence charSequence) {
		TextView actionTextView = (TextView) findViewById(R.id.actionTextView);

		Log.d(TAG, "setting action text: " + charSequence + "; "
				+ actionTextView);
		actionTextView.setText(charSequence);
		Log.d(TAG, "action text set");
	}

	private CharSequence getActionText() {
		TextView actionTextView = (TextView) findViewById(R.id.actionTextView);

		return actionTextView.getText();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.bataille_navale, menu);
		
		MenuItem resetMenuItem = (MenuItem) menu.findItem(R.id.action_new_game);
		resetMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				handleResetButtonClick();
				return false;
			}
		});

		return true;
	}

	/**
	 * Implements the ResetDialogListener interface. Takes reset actions.
	 */
	public void onResetRequest() {
		fsm.event(RESET);
		fsm.event(START);
	}

	/**
	 * Process an FSM event.
	 * 
	 * @param event
	 * @param args
	 */
	void processEvent(Event event, Object... args) {
		fsm.event(event, args);
	}

	/**
	 * Process an FSM event.
	 * 
	 * @param event
	 */
	void processEvent(Event event) {
		processEvent(event, (Object) null);
	}

	/**
	 * Process an FSM event w/o throwing an exception if no rule matches.
	 * 
	 * @param event
	 */
	void processSoftEvent(Event event) {
		fsm.softEvent(event);
	}

	/**
	 * Display a toast.
	 * 
	 * @param text
	 *            Text to display.
	 */
	void displayToast(CharSequence text) {
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(this, text, duration);
		toast.show();
	}

	private void setBackgroundColour(Player player) {
		setBackgroundColour(player.getColour());
	}

	private void setBackgroundColour(int colour) {
		currentBackgroundColour = colour;

		View root = findViewById(R.id.root);
		root.setBackgroundColor(colour);

		View frame = findViewById(R.id.FrameLayout);
		frame.setBackgroundColor(colour);

		squareLayout.setBackgroundColor(colour);
	}

	/**
	 * Sets the UI up for player to place boats.
	 * 
	 * @return true
	 */
	private boolean updateUiForBoatPlacement() {
		Player player = gameState.getPlayerToPlaceBoat();

		Log.d(TAG, "updateUiForBoatPlacement");

		setActionText(player.getName() + ", " + getString(R.string.place) + " "
				+ player.getShipToPlace());

		setBackgroundColour(player);

		actionButtonSetClickable(false);
		viewOwnBoatsCheckBoxClear();
		viewOwnBoatsCheckBoxSetClickable(false);
		squareLayout.setClickable(true);

		squareLayout.invalidate();

		return true;
	}

	/**
	 * Sets the UI up for player to take a shot.
	 * 
	 * @return true
	 */
	private boolean updateUiForPlayerShot() {
		Player player = gameState.nextPlayer();

		viewOwnBoatsCheckBoxSetClickable(true);

		setActionText(player.getName() + " " + getString(R.string.must_shoot));

		setBackgroundColour(player);

		actionButtonSetClickable(false);
		squareLayout.setClickable(true);

		return true;
	}
}
