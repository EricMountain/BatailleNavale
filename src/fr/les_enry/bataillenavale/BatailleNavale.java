package fr.les_enry.bataillenavale;



import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

//TODO Option to end game
//TODO i18n

//TODO Timeout if no activity and let the screen turn off + save state and be able to restore it
//TODO Bluetooth game
//TODO see how to grey disabled elements
//TODO Nicer buttons
//TODO Game icon
//TODO Animations
//TODO Setup (sound on/off, animations on/off)
//TODO Ship placement like Blokish?  Drawing with your finger is nice too though...  Better event handling needed anyway.
//TODO High scores
//TODO Exit activity, save state when leaving activity, and restore
//TODO Handle back button properly
//TODO Refactor all logic including driving of the HCI into GameState
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

	private GameState gameState = GameState.getGameState();
	
	TextView actionTextView = null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
        	                 WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_bataille_navale);

        gameState.clearBoard();
        
        FrameLayout squareFrameLayout = new FrameLayout(this) {
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
        
        FrameLayout frameLayout = (FrameLayout) this.findViewById(R.id.frameLayout);
        ViewGroup parent = (ViewGroup) frameLayout.getParent();
        int frameLayoutIndex = parent.indexOfChild(frameLayout);
        parent.removeView(frameLayout);
        parent.addView(squareFrameLayout, frameLayoutIndex);
 
//        squareFrameLayout.setBackgroundColor(android.graphics.Color.BLUE);
        
        TableLayout tableLayout = new TableLayout(this);
        squareFrameLayout.addView(tableLayout);
        tableLayout.setWeightSum(1f);
        tableLayout.setStretchAllColumns(true);  
        tableLayout.setShrinkAllColumns(true);  

        for (int row = 0; row < GameState.NB_ROWS; row++) {
        	TableRow tableRow = new TableRow(this);
        	tableLayout.addView(tableRow);
        	((LinearLayout.LayoutParams) tableRow.getLayoutParams()).weight = .1f;
        	for (int column = 0; column < GameState.NB_COLS; column++) {
        		CellDrawableView cell = new CellDrawableView(this, row, column);
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
        
        CheckBox viewOwnCheckBox = (CheckBox) findViewById(R.id.ViewOwnCheckBox);
        //TODO To be handled by GameState
        viewOwnCheckBox.setClickable(false);
        viewOwnCheckBox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					// Display own
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
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.bataille_navale, menu);
        return true;
    }
    
}
