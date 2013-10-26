package fr.les_enry.bataillenavale;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import fr.les_enry.bataillenavale.Cell.CellState;
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
	private static final State SHOT_NEEDED = fsm.state("Shot needed");
	private static final State GAME_OVER = fsm.state("Game over");

	static final Event START = fsm.event("Start");
	static final Event CELL_ACTIVATED = fsm.event("Cell activated");
	static final Event RESET = fsm.event("Reset game");

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
						batailleNavale.handleActionButtonClick(null);
						return true;
					}
				});
		fsm.rule().initial(BOAT_TO_PLACE).event(CELL_ACTIVATED)
				.ok(BOAT_TO_PLACE).fail(SHOT_NEEDED).action(new Action() {
					public boolean act(Object... rowColumn) {
						return handleCellActivationForBoatPlacement(rowColumn);
					}
				});
		fsm.rule().initial(SHOT_NEEDED).event(CELL_ACTIVATED).ok(SHOT_NEEDED)
				.fail(GAME_OVER).action(new Action() {
					public boolean act(Object... rowColumn) {
						return handleCellActivationForShot(rowColumn);
					}
				});

		fsm.rule().initial(GAME_OVER).event(RESET).ok(INIT)
				.action(new Action() {
					public boolean act() {
						return resetGame();
					}
				});
		fsm.rule().initial(BOAT_TO_PLACE).event(RESET).ok(INIT)
				.action(new Action() {
					public boolean act() {
						return resetGame();
					}
				});
		fsm.rule().initial(SHOT_NEEDED).event(RESET).ok(INIT)
				.action(new Action() {
					public boolean act() {
						return resetGame();
					}
				});

		// Ignore cell activation if game is over
		fsm.rule().initial(GAME_OVER).event(CELL_ACTIVATED).ok(GAME_OVER);

		fsm.start(INIT);
	}

	void processEvent(Event event, Object... args) {
		fsm.event(event, args);
	}

	void processEvent(Event event) {
		processEvent(event, (Object) null);
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
	boolean resetGame() {
		for (Player player : players) {
			player.resetPlayer();
		}
		currentPlayer = 1;

		return true;
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
	 * Updates the board cells as requested.
	 * 
	 * @param isOwnBoatsView
	 *            If true, show own boats, o/w show sthots taken.
	 */
	void updateCells(boolean isOwnBoatsView) {
		if (isOwnBoatsView)
			updateCells(getCurrentPlayer());
		else
			updateCellsWithPlayerShots(getCurrentPlayer());
	}

	/**
	 * Draws a player's own boats view.
	 * 
	 * @param player
	 *            Player whose boats to display.
	 */
	private void updateCells(Player player) {
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
	private void updateCellsWithPlayerShots(Player player) {

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

	/**
	 * Places part of a boat on the cell that was activated.
	 * 
	 * @param rowColumn
	 *            cell row, cell column
	 * @return true if placement OK, false o/w.
	 */
	boolean handleCellActivationForBoatPlacement(Object... rowColumn) {
		int row = (Integer) rowColumn[0];
		int column = (Integer) rowColumn[1];

		Cell cell = getCell(row, column);

		Player player = getNextToPlace();

		boolean moreBoatsToPlace = true;
		if (player != null) {
			try {
				player.addShipToCell(cell);
				cell.setState(CellState.SHIP);

				// More boats to place?
				Player player2 = testNextToPlace();
				if (player2 != null && player2 == player) {
					// More boats for same player
					player2 = getNextToPlace();
					batailleNavale.setActionText(player2.getName() + " place "
							+ player2.getShipToPlace());
				} else if (player2 != null && player2 != player) {
					// No more boats for current player
					displayToast("Press OK when ready.");
					batailleNavale.setActionText(player.getName()
							+ " ships placed.  Press OK.");
				} else {
					// No more boats to place
					displayToast("Press OK when ready.");
					batailleNavale.setActionText(player.getName()
							+ " ships placed.  Press OK.");

					moreBoatsToPlace = false;
				}

			} catch (BadPlacementException e) {
				displayToast("Can't place the ship there.");
			}
		} else
			throw new RuntimeException(
					"Placing boats, but no player has any left to place.");

		return moreBoatsToPlace;
	}

	/**
	 * Takes shot on cell activation.
	 * 
	 * @param rowColumn
	 *            cell row, cell column
	 * @return true if shot OK, false o/w.
	 */
	boolean handleCellActivationForShot(Object... rowColumn) {
		int row = (Integer) rowColumn[0];
		int column = (Integer) rowColumn[1];

		Cell cell = getCell(row, column);

		Shot shot = new Shot(row, column);

		Player current = getCurrentPlayer();
		Player opponent = getOpponent();

		boolean moreShotsNeeded = true;
		try {
			Ship shipHit = fireShot(shot);
			Log.d(TAG, (shipHit == null ? "Shot was a miss" : "Shot hit ship "
					+ shipHit));

			if (shipHit != null) {
				if (shipHit.checkSunk()) {
					cell.setState(CellState.SUNK);
					updateCellsWithPlayerShots(getCurrentPlayer());
					displayToast(shipHit + " sunk!");
				} else {
					cell.setState(CellState.HIT);
					displayToast(shipHit + " hit!");
				}
			} else {
				cell.setState(CellState.MISS);
				displayToast("Missed!");
			}

			if (opponent.checkLost()) {
				Log.d(TAG, opponent + " has lost");

				displayToast(current.getName() + " has won!");
				batailleNavale.setActionText(current.getName()
						+ " won.  Game over.");

				moreShotsNeeded = false;
			} else {
				batailleNavale.setActionText(current.getName()
						+ " turn complete ");
			}
		} catch (AlreadyPlayedShotException e) {
			displayToast("Shot already played.");
		} catch (AlreadyPlayedException e) {
			displayToast("Already played, it's " + gameState.getOpponent()
					+ "'s turn.");
		} catch (CantShootHereException e) {
			displayToast("You can't shoot yourself!");
		}

		return moreShotsNeeded;
	}

	/**
	 * Display a toast.
	 * 
	 * @param text
	 *            Text to display.
	 */
	private void displayToast(CharSequence text) {
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(batailleNavale, text, duration);
		toast.show();
	}

}
