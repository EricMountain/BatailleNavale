package fr.les_enry.bataillenavale;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import android.widget.Button;

import fr.les_enry.util.fsm.FSM;
import fr.les_enry.util.fsm.State;
import fr.les_enry.util.fsm.Event;
import fr.les_enry.util.fsm.Action;

/**
 * Represents current game state.
 */
public class GameState {
	/** Tag for logs. **/
	private static final String TAG = "GameState";

	/** Current BatailleNavale activity. */
	private BatailleNavale batailleNavale;

	/** Rows on the board. */
	static final int NB_ROWS = 10;

	/** Columns on the board. */
	static final int NB_COLS = 10;

	/** List of players in the game. */
	private List<Player> players = new ArrayList<Player>();

	/** Index of player that must place a ship or take a shot. */
	private int currentPlayer = 1;

	/** Game board. */
	private List<Cell> board = new ArrayList<Cell>();

	/** Reference to the "OK" button. */
	private Button actionButton = null;

	/** True if player can take a shot in this view, false o/w. */
	private boolean isPlayerViewShootable = false;

	/** Flying spaghetti monster. */
	private static FSM fsm = new FSM();

	private static final State INIT = fsm.state("Initial");
	private static final State BOAT_TO_PLACE = fsm.state("Boat to place");
	private static final State CHECK_BOAT_TO_PLACE = fsm
			.state("Check boat to place");
	private static final State SHOT_NEEDED = fsm.state("Shot needed");
	private static final State CHECK_WON = fsm.state("Check won");
	private static final State GAME_OVER = fsm.state("Game over");

	static final Event START = fsm.event("Start");
	private static final Event BOAT_PLACED = fsm.event("Boat placed");
	private static final Event MORE_BOATS = fsm.event("More boats to place");
	private static final Event NO_MORE_BOATS = fsm.event("No more boats to place");
	private static final Event SHOT_TAKEN = fsm.event("Shot taken");
	private static final Event NOT_WON = fsm.event("Game not won");
	private static final Event WON = fsm.event("Game won");
	private static final Event RESET = fsm.event("Reset game");

	/** Global game state. */
	private static final GameState gameState = new GameState();

	/**
	 * Gets the game state singleton.
	 * 
	 * @return singleton.
	 */
	static GameState getGameState() {
		if (gameState == null) {
			throw new RuntimeException(
					"GameState is null - did not expect that!");
		}
		return gameState;
	}

	/**
	 * Initialises game state with 2 players.
	 */
	GameState() {
		players.add(new Player("Player 1"));
		players.add(new Player("Player 2"));

		initFSM();
	}

	void setBatailleNavale(BatailleNavale batailleNavale) {
		this.batailleNavale = batailleNavale;
	}

	/**
	 * Sets up the FSM. Initial state is BOAT_TO_PLACE.
	 */
	private void initFSM() {
		fsm.rule().initial(INIT).event(START).ok(BOAT_TO_PLACE)
				.action(new Action() {
					public boolean act() {
						batailleNavale.handleButtonClick(null);
						return true;
					}
				});
		fsm.rule().initial(BOAT_TO_PLACE).event(BOAT_PLACED)
				.ok(CHECK_BOAT_TO_PLACE);
		fsm.rule().initial(CHECK_BOAT_TO_PLACE).event(MORE_BOATS)
				.ok(BOAT_TO_PLACE);
		fsm.rule().initial(CHECK_BOAT_TO_PLACE).event(NO_MORE_BOATS)
				.ok(SHOT_NEEDED);
		fsm.rule().initial(SHOT_NEEDED).event(SHOT_TAKEN).ok(CHECK_WON);
		fsm.rule().initial(CHECK_WON).event(NOT_WON).ok(SHOT_NEEDED);
		fsm.rule().initial(CHECK_WON).event(WON).ok(GAME_OVER);
		fsm.rule().initial(GAME_OVER).event(RESET).ok(BOAT_TO_PLACE);

		fsm.start(INIT);
	}

	void processEvent(Event event) {
		fsm.event(event);
	}

	void processSoftEvent(Event event) {
		try {
			processEvent(event);
		} catch (FSM.NoApplicableRuleException nowt) {
			// Do nothing
		}
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
	 * Clears the board of all existing cells.
	 */
	void clearBoard() {
		board.clear();
	}

	/**
	 * Gets the list of cells.
	 * 
	 * @return a reference to the board.
	 */
	List<Cell> getBoard() {
		return board;
	}

	/**
	 * Gets specified cell reference.
	 * 
	 * @param row
	 * @param column
	 * @return a cell
	 */
	Cell getCell(int row, int column) {
		return board.get(row * NB_COLS + column);
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
	 * @param cell
	 *            Cell to add.
	 */
	void addCell(Cell cell) {
		board.add(cell);
	}

	/**
	 * Clears all cells on the board to neutral state.
	 */
	private void resetCells() {
		for (Cell cell : board) {
			cell.reset();
		}
	}

	/**
	 * Draws a player's own boats view.
	 * 
	 * @param player
	 *            Player who's boats to display.
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
			board.get(cellOffset).setState(Cell.CellState.MISS);
		}

		// Show state of own ships
		for (Ship ship : allShips) {
			for (Cell shipCoordinates : ship.getCoordinates()) {
				int cellOffset = shipCoordinates.getRow() * NB_COLS
						+ shipCoordinates.getColumn();
				board.get(cellOffset).setState(Cell.CellState.SHIP);
			}

			boolean isSunk = ship.checkSunk();
			Cell.CellState state = isSunk ? Cell.CellState.SUNK
					: Cell.CellState.HIT;

			for (Shot shot : ship.getHits()) {
				int cellOffset = shot.getRow() * NB_COLS + shot.getColumn();
				board.get(cellOffset).setState(state);
			}
		}

		// Player can't shoot on this view
		this.isPlayerViewShootable = false;
	}

	/**
	 * Draws a player's shots taken view.
	 * 
	 * @param player
	 *            Player whose shots are to be displayed.
	 */
	void updateCellsWithPlayerShots(Player player) {

		Log.d(TAG, "Updating shots taken view for " + player);

		resetCells();

		for (Shot shot : player.getShotsFired()) {
			int cellOffset = shot.getRow() * NB_COLS + shot.getColumn();
			board.get(cellOffset).setState(Cell.CellState.MISS);
		}

		Player opponent = getOpponent();
		List<Ship> allShips = new ArrayList<Ship>();
		allShips.addAll(opponent.getShipsAfloat());
		allShips.addAll(opponent.getShipsSunk());
		for (Ship opponentShip : allShips) {
			boolean isSunk = opponentShip.checkSunk();
			Cell.CellState state = isSunk ? Cell.CellState.SUNK
					: Cell.CellState.HIT;

			for (Shot shot : opponentShip.getHits()) {
				int cellOffset = shot.getRow() * NB_COLS + shot.getColumn();
				board.get(cellOffset).setState(state);
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
	 * @param shot
	 *            Shot coordinates.
	 * @return ship that was hit, or null
	 * @throws AlreadyPlayedException
	 *             if same shot has been played before
	 */
	Ship fireShot(Shot shot) throws AlreadyPlayedException,
			CantShootHereException {
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
	 * Checks if the player can take a shot in the current view mode (own ships
	 * vs shots taken).
	 * 
	 * @return true if the player can shoot in this view, false otherwise.
	 */
	boolean isPlayerViewShootable() {
		return isPlayerViewShootable;
	}
}
