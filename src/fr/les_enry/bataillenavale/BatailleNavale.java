package fr.les_enry.bataillenavale;



import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

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

	private GameState gameState = GameState.getGameState();
	
	TextView actionTextView = null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        setContentView(R.layout.activity_bataille_navale);

        TableLayout tableLayout = (TableLayout) this.findViewById(R.id.TableLayout);
        tableLayout.setStretchAllColumns(true);  
        tableLayout.setShrinkAllColumns(true);  
        for (int row = 0; row < GameState.NB_ROWS; row++) {
        	TableRow tableRow = new TableRow(this);
        	for (int column = 0; column < GameState.NB_COLS; column++) {
        		// i is row, j is col
        		CellDrawableView cell = new CellDrawableView(this, row, column);
        		tableRow.addView(cell);
        		gameState.addCell(cell);
        	}
        	tableLayout.addView(tableRow);
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
        
        // TODO Refactor
        // Start ship placement sequence
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



//SVG svg = SVGParser.getSVGFromResource(getResources(), R.raw.drawing);
// Get a drawable from the parsed SVG and set it as the drawable for the ImageView
//ImageView imageView = (ImageView) findViewById(R.id.imageView1);
//imageView.setImageDrawable(svg.createPictureDrawable());

/*
RelativeLayout layout = new RelativeLayout(this);
layout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
		RelativeLayout.LayoutParams.MATCH_PARENT));

TextView textView = new TextView(this);
textView.setText("Player 1");
textView.setLayoutParams(new LayoutParams(
		LayoutParams.MATCH_PARENT,
		LayoutParams.WRAP_CONTENT));
layout.addView(textView);
*/
