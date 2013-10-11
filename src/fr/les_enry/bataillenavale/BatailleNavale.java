package fr.les_enry.bataillenavale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.TextView;

//TODO Must not handle touch event after player 1 has placed all ships
//TODO Full i18n
//TODO Fix the way the dialog is created to be in line with modern way
//TODO "New game"/Reset should move to menu
//TODO Exit activity, save state when leaving activity, and restore
//TODO Refactor all logic including driving of the HCI into GameState
//TODO Timeout if no activity and let the screen turn off + save state and be able to restore it

//TODO Ship placement like Blokish?  Drawing with your finger is nice too though...  Better event handling needed anyway.
//TODO Automated tests
//TODO Bluetooth game
//TODO see how to grey disabled elements
//TODO Nicer buttons
//TODO Game icon
//TODO Animations
//TODO Setup (sound on/off, animations on/off)
//TODO High scores
//TODO Handle back button properly
//TODO Sounds
//TODO Setup theme colours properly, not using hardcodes on layout and TextView colours

/**
 * Entry point activity.
 * 
 */
public class BatailleNavale extends Activity {
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

			for (Cell c : GameState.getGameState().getBoard()) {
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
			// Log.d(TAG, "MotionEvent: " + event);

			if (action == MotionEvent.ACTION_UP
					|| action == MotionEvent.ACTION_POINTER_UP) {
				float x = event.getX();
				float y = event.getY();
				int row = (int) Math.floor(y / rowStep);
				int col = (int) Math.floor(x / colStep);
				Log.d(TAG, "Touch at: " + x + "," + y + " cell: " + row + ","
						+ col + " steps: " + rowStep + "," + colStep);
				Cell cell = GameState.getGameState().getCell(row, col);
				cell.handleTouchEvent(this);
				this.invalidate();
			}

			return true;
		}
	}

	/**
	 * Logging tag.
	 */
	private static final String TAG = "BatailleNavale";

	private GameState gameState = GameState.getGameState();

	TextView actionTextView = null;
	FrameLayout squareLayout = null;

	private static final int RESET_DIALOG = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
						| WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_bataille_navale);

		gameState.clearBoard();

		FrameLayout frameLayout = (FrameLayout) this
				.findViewById(R.id.FrameLayout);

		// Make a square table layout
		squareLayout = new SquareLayout(this);

		FrameLayout.LayoutParams squareLayoutParams = new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

		frameLayout.addView(squareLayout, squareLayoutParams);

		// frameLayout.setBackgroundColor(Color.WHITE);
		//
		// squareLayout.setBackgroundColor(Color.YELLOW);
		squareLayout.setBackgroundColor(Color.BLACK); // TODO Why is this nec.?
														// Why doesn't
														// invalidate() do the
														// job?
		// squareLayout.invalidate();

		for (int row = 0; row < GameState.NB_ROWS; row++) {
			for (int column = 0; column < GameState.NB_COLS; column++) {
				Cell cell = new Cell(row, column);
				gameState.addCell(cell);
			}
		}

		Button button = (Button) findViewById(R.id.actionButton);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				handleButtonClick(v);
				squareLayout.invalidate();
			}
		});
		gameState.setActionButton(button);

		Button resetButton = (Button) findViewById(R.id.resetButton);
		resetButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				handleResetButtonClick(v);
			}
		});

		CheckBox viewOwnCheckBox = (CheckBox) findViewById(R.id.ViewOwnCheckBox);
		// TODO To be handled by GameState
		viewOwnCheckBox.setClickable(false);
		viewOwnCheckBox
				.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked) {
							// Display own board
							gameState.updateCells(gameState.getCurrentPlayer());
						} else {
							// Display shots taken
							gameState.updateCellsWithPlayerShots(gameState
									.getCurrentPlayer());
						}
						squareLayout.invalidate();
					}
				});

		// Start ship placement sequence unless game is already in progress
		// e.g. screen rotated
		if (button.isClickable()) {
			handleButtonClick(button);
			squareLayout.invalidate();
		}
	}

	// TODO Refactor
	void handleButtonClick(View view) {
		CheckBox viewOwnCheckBox = (CheckBox) findViewById(R.id.ViewOwnCheckBox);
		viewOwnCheckBox.setChecked(false);

		actionTextView = (TextView) findViewById(R.id.actionTextView);

		Player player2 = gameState.getNextToPlace();
		if (player2 != null) {
			actionTextView.setText(player2.getName() + " place "
					+ player2.getShipToPlace());
		} else {
			if (gameState.getOpponent().checkLost()) {
				// Game is finished, create a new one and start ship placement
				// sequence
				// TODO refactor - c.f RESET_DIALOG
				gameState.resetGame();
				handleButtonClick(view);
			} else {
				// This starts and plays the game
				viewOwnCheckBox.setClickable(true);

				Player nextPlayer = gameState.nextPlayer();
				actionTextView.setText(nextPlayer.getName() + " doit tirer");
			}
		}
	}

	void handleResetButtonClick(View view) {
		showDialog(RESET_DIALOG);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.bataille_navale, menu);
		return true;
	}

	@Override
	protected Dialog onCreateDialog(int id) {

		switch (id) {
		case RESET_DIALOG:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.dialog_reset_game)
					.setPositiveButton(android.R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									gameState.resetGame();
									((CheckBox) findViewById(R.id.ViewOwnCheckBox))
											.setClickable(false);
									handleButtonClick(null);
									squareLayout.invalidate();
								}
							})
					.setNegativeButton(android.R.string.no,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									// Do nowt
								}
							});

			return builder.create();

		default:
			return super.onCreateDialog(id);
		}
	}
}
