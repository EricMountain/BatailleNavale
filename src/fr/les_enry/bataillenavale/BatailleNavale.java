package fr.les_enry.bataillenavale;



import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

//TODO Full i18n
//TODO Fix the way the dialog is created to be in line with modern way
//TODO "New game"/Reset should move to menu
//TODO Exit activity, save state when leaving activity, and restore
//TODO Refactor all logic including driving of the HCI into GameState
//TODO Timeout if no activity and let the screen turn off + save state and be able to restore it

//TODO Reimplement the board as a single View to avoid the whole issue with making a square layout 
//TODO Ship placement like Blokish?  Drawing with your finger is nice too though...  Better event handling needed anyway.
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
	 * Logging tag.
	 */
	private static final String TAG = "BatailleNavale";

	/** Used for generateId(). */
	private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);
	
	private GameState gameState = GameState.getGameState();
	
	TextView actionTextView = null;
	
	private static final int RESET_DIALOG = 1;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
        	                 WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_bataille_navale);

        gameState.clearBoard();
                
        RelativeLayout relativeLayout = (RelativeLayout) this.findViewById(R.id.RelativeLayoutCells);
        
        // Make a square table layout
        TableLayout tableLayout = new TableLayout(this) {
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
        };
        
        RelativeLayout.LayoutParams relTableLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        relTableLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        relTableLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        relTableLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        relTableLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        
        relativeLayout.addView(tableLayout, relTableLayoutParams);
        
        tableLayout.setWeightSum(1f);
        tableLayout.setStretchAllColumns(true);  
        tableLayout.setShrinkAllColumns(true);  

        for (int row = 0; row < GameState.NB_ROWS; row++) {
        	TableRow tableRow = new TableRow(this);
        	tableLayout.addView(tableRow);
        	((LinearLayout.LayoutParams) tableRow.getLayoutParams()).weight = .1f;
        	for (int column = 0; column < GameState.NB_COLS; column++) {
        		CellDrawableView cell = new CellDrawableView(this, row, column, GameState.NB_ROWS, GameState.NB_COLS);
        		tableRow.addView(cell);
        		((TableRow.LayoutParams) cell.getLayoutParams()).weight = .1f;
        		gameState.addCell(cell);
        	}
        }
               
        Button button = (Button) findViewById(R.id.actionButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handleButtonClick(v);
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
        //TODO To be handled by GameState
        viewOwnCheckBox.setClickable(false);
        viewOwnCheckBox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					// Display own board
					gameState.updateCells(gameState.getCurrentPlayer());
				} else {
					// Display shots taken
					gameState.updateCellsWithPlayerShots(gameState.getCurrentPlayer());
				}
			}
        });
        
        // Start ship placement sequence unless game is already in progress
        // e.g. screen rotated
        if (button.isClickable())
        	handleButtonClick(button);
    }

    // TODO Refactor
    void handleButtonClick(View view) {
    	CheckBox viewOwnCheckBox = (CheckBox) findViewById(R.id.ViewOwnCheckBox);
        viewOwnCheckBox.setChecked(false);
        
    	actionTextView = (TextView) findViewById(R.id.actionTextView);
    	    	
    	Player player2 = gameState.getNextToPlace();
    	if (player2 != null) {
    		actionTextView.setText(player2.getName() + " place " + player2.getShipToPlace());
    	} else {
    		// TODO If opponent has lost, then this is a request to start a new game
    		if (gameState.getOpponent().checkLost()) {
    			// Game is finished, create a new one and start ship placement sequence
    			//TODO refactor - c.f RESET_DIALOG
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

		switch(id) {
		case RESET_DIALOG:
	        AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        builder.setMessage(R.string.dialog_reset_game)
	               .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                	   	gameState.resetGame();
	           				handleButtonClick(null);
	                   }
	               })
	               .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                       // Do nowt	                	   
	                   }
	               });

	        return builder.create();
	    	
    	default:
	    	return super.onCreateDialog(id);
		}
	}
    
	/**
	 * Generate a value suitable for use in {@link #setId(int)}.
	 * This value will not collide with ID values generated at build time by aapt for R.id.
	 * 
	 * From http://stackoverflow.com/questions/1714297/android-view-setidint-id-programmatically-how-to-avoid-id-conflicts
	 *
	 * TODO Remove once we target minimum API level 17.
	 *
	 * @return a generated ID value
	 */
	public static int generateViewId() {
	    for (;;) {
	        final int result = sNextGeneratedId.get();
	        // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
	        int newValue = result + 1;
	        if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
	        if (sNextGeneratedId.compareAndSet(result, newValue)) {
	            return result;
	        }
	    }
	}
}
