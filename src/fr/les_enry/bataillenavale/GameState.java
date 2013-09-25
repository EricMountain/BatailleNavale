package fr.les_enry.bataillenavale;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import android.widget.Button;

/**
 * Represents current game state.
 */
public class GameState {
	/** Tag for logs. **/
	private static final String TAG = "GameState";
	
	/** List of players in the game. */
	private List<Player> players = new ArrayList<Player>();

	/** Index of player that must place a ship or take a shot. */
	private int currentPlayer = 1;
	
	//TODO Get rid of this singleton
	/** Global game state. */
	private static final GameState gameState = new GameState();
	
	/** Game board. */
	private List<CellDrawableView> board = new ArrayList<CellDrawableView>();
	
	/** Reference to the "OK" button. */
	private Button actionButton = null;
	
	/** True if player can take a shot in this view, false o/w. */
	private boolean isPlayerViewShootable = false;
	
	/** Rows on the board. */
	static final int NB_ROWS = 10;
	/**Columns on the board. */
	static final int NB_COLS = 10;
	
	/**
	 * Initialises game state with 2 players.
	 */
	GameState() {
		players.add(new Player("Player 1"));
		players.add(new Player("Player 2"));
	}
	
	/**
	 * Gets the game state singleton.
	 * 
	 * @return singleton.
	 */
	static GameState getGameState() {
		if (gameState == null) {
			throw new RuntimeException("GameState is null - did not expect that!"); 
		}
		return gameState;
	}
	
	/**
	 * Starts a new game.
	 */
	void resetGame() {
		for (Player player : players) {
			player.resetPlayer();
		}
		currentPlayer = 1;
	}
	
	/**
	 * Adds a player.
	 * 
	 * @param player
	 */
	//TODO not used?
	void addPlayer(Player player) {
		players.add(player);
	}
	
	/**
	 * Gets the player that must take a shot or place a ship.
	 * 
	 * @return current player.
	 */
	Player getCurrentPlayer() {
		return players.get(currentPlayer);
	}
	
	/**
	 * Gets the current player's opponent.
	 * 
	 * @return opponent.
	 */
	Player getOpponent() {
		return players.get(1 - currentPlayer);
	}

	/**
	 * Starts next turn.
	 * 
	 * @return the next player to shoot.
	 */
	Player nextPlayer() {
		currentPlayer = 1 - currentPlayer;

		Player player = getCurrentPlayer();
		
		player.allowTurn();
		
		updateCellsWithPlayerShots(getCurrentPlayer());
		
		actionButton.setClickable(false);
		
		// Default view presented to user is one they can shoot on
		//TODO Redundant with updateCellsWithPlayerShots above?
		this.isPlayerViewShootable = true;
		
		return getCurrentPlayer();
	}
	
	/**
	 * Finds out which is the next player to place a ship.
	 * 
	 * @return next player to place a ship, or null if none.
	 */
	Player getNextToPlace() {
		Player player = testNextToPlace();
		
		if (player != null)
			updateCells(player);
		
		return player;
	}
	
	/**
	 * Checks to see which is the next player to place a ship.
	 * 
	 * @return player that needs to place a ship, or null if nothing to place.
	 */
	Player testNextToPlace() {
		for (Player player : players) {
			if (player.hasShipsToPlace()) {
				return player;
			}
		}
		
		return null;
	}
	
	/**
	 * Adds a cell to the board.
	 * 
	 * @param cell Cell to add.
	 */
	void addCell(CellDrawableView cell) {
		board.add(cell);
	}
	
	/**
	 * Clears all cells on the board to neutral state.
	 */
	private void resetCells() {
		for (CellDrawableView cell : board) {
			cell.reset();
		}
	}
	
	/**
	 * Draws a player's own boats view.
	 * 
	 * @param player Player who's boats to display.
	 */
	void updateCells(Player player) {
		List<Ship> allShips = new ArrayList<Ship>();
		allShips.addAll(player.getShipsAfloat());
		allShips.addAll(player.getShipsToPlace());
		allShips.addAll(player.getShipsSunk());
		
		Log.d(TAG, "Updating boat placement view for " + player);
		
		resetCells();

		// Show shots taken by opponent
		Player opponent = this.getOpponent();
		for (Shot shot : opponent.getShotsFired()) {
			int cellOffset = shot.getRow() * NB_COLS + shot.getColumn();
			board.get(cellOffset).setState(CellDrawableView.MISS);			
		}
		
		// Show state of own ships
		for (Ship ship : allShips) {
			for (Cell shipCoordinates : ship.getCoordinates()) {
				int cellOffset = shipCoordinates.getRow() * NB_COLS + shipCoordinates.getColumn();
				board.get(cellOffset).setState(CellDrawableView.SHIP);
			}

			boolean isSunk = ship.checkSunk();
			int color = isSunk ? CellDrawableView.SUNK : CellDrawableView.HIT;

			for (Shot shot : ship.getHits()) {
				int cellOffset = shot.getRow() * NB_COLS + shot.getColumn();
				board.get(cellOffset).setState(color);
			}						
		}
		
		// Player can't shoot on this view
		this.isPlayerViewShootable = false;
	}


	/**
	 * Draws a player's shots taken view.
	 * 
	 * @param player Player whose shots are to be displayed.
	 */
	void updateCellsWithPlayerShots(Player player) {
		
		Log.d(TAG, "Updating shots taken view for " + player);
		
		resetCells();

		for (Shot shot : player.getShotsFired()) {
			int cellOffset = shot.getRow() * NB_COLS + shot.getColumn();
			board.get(cellOffset).setState(CellDrawableView.MISS);			
		}
		
		Player opponent = getOpponent();
		List<Ship> allShips = new ArrayList<Ship>();
		allShips.addAll(opponent.getShipsAfloat());
		allShips.addAll(opponent.getShipsSunk());
		for (Ship opponentShip : allShips) {
			boolean isSunk = opponentShip.checkSunk();
			int color = isSunk ? CellDrawableView.SUNK : CellDrawableView.HIT;
			
			for (Shot shot : opponentShip.getHits()) {
				int cellOffset = shot.getRow() * NB_COLS + shot.getColumn();
				board.get(cellOffset).setState(color);			
			}
		}
		
		// Player can shoot on this view
		this.isPlayerViewShootable = true;
		
	}

	/**
	 * Stores a reference to the action button on the GUI.
	 * 
	 * @param actionButton
	 */
	void setActionButton(Button actionButton) {
		this.actionButton = actionButton;
	}

	/**
	 * Takes a shot on behalf of the current player.
	 * 
	 * @param shot Shot coordinates.
	 * @return ship that was hit, or null
	 * @throws AlreadyPlayedException if same shot has been played before
	 */
	Ship fireShot(Shot shot) throws AlreadyPlayedException, CantShootHereException {
		if (!this.isPlayerViewShootable)
			throw new CantShootHereException("Can't take shot on this view.");
		
		Player opponent = getOpponent();
		
		Ship shipHit = getCurrentPlayer().fireShot(shot, opponent);
		
		// Only re-enable the action button if the shot was valid
		// i.e. no exception was thrown above
		actionButton.setClickable(true);
		
		return shipHit;
	}

	/**
	 * Checks if the player can take a shot in the current view mode (own ships vs shots taken).
	 * 
	 * @return true if the player can shoot in this view, false otherwise.
	 */
	boolean isPlayerViewShootable() {
		return isPlayerViewShootable;
	}
}
