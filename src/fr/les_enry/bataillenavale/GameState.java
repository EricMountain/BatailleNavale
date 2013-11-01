package fr.les_enry.bataillenavale;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import fr.les_enry.bataillenavale.Cell.CellState;
import fr.les_enry.util.fsm.Action;
import fr.les_enry.util.fsm.Event;
import fr.les_enry.util.fsm.FSM;
import fr.les_enry.util.fsm.State;

/**
 * Represents current game state.
 */
class GameState {
	/** Tag for logs. **/
	private static final String TAG = "GameState";

	/** Rows on the board. */
	static final int NB_ROWS = 10;

	/** Columns on the board. */
	static final int NB_COLS = 10;

	/** Flying spaghetti monster. */
	private static FSM fsm = new FSM("GameState");

	private static final State INIT = fsm.state("Initial");
	private static final State BOAT_TO_PLACE = fsm.state("Boat to place");
	private static final State SHOT_NEEDED = fsm.state("Shot needed");
	private static final State GAME_OVER = fsm.state("Game over");

	static final Event START = fsm.event("Start");
	static final Event CELL_ACTIVATED = fsm.event("Cell activated");
	static final Event RESET = fsm.event("Reset game");

	/** Global game state. */
	private static final GameState gameState = new GameState();

	/** Current BatailleNavale activity. */
	private BatailleNavale batailleNavale;

	/** Board representation of the game. */
	private BoardState boardState = new BoardState(this);

	/** List of players in the game. */
	private List<Player> players = new ArrayList<Player>();

	/** Index of player that must place a ship or take a shot. */
	private int currentPlayer = 1;

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
		Log.d(TAG, "Constructing new game state");
		
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
		
		fsm.reset();
		
		fsm.rule().initial(INIT).event(START).ok(BOAT_TO_PLACE);
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
		fsm.softEvent(event);
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

	BoardState getBoardState() {
		return boardState;
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

		boardState.updateCells(false);

		batailleNavale.setClickableActionButton(false);

		return getCurrentPlayer();
	}

	/**
	 * Finds out which is the next player to place a ship.
	 * 
	 * @return next player to place a ship, or null if none.
	 */
	Player getPlayerToPlaceBoat() {
		Player player = testNextToPlace();

		if (player != null)
			boardState.updateCellsWithOwnBoats(player);

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
		if (!boardState.isPlayerViewShootable())
			throw new CantShootHereException("Can't take shot on this view.");

		Player opponent = getOpponent();

		Ship shipHit = getCurrentPlayer().fireShot(shot, opponent);

		// Only re-enable the action button if the shot was valid
		// i.e. no exception was thrown above
		batailleNavale.setClickableActionButton(true);

		return shipHit;
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

		Player player = getPlayerToPlaceBoat();

		boolean moreBoatsToPlace = true;
		if (player != null) {
			try {
				player.addShipToCell(new Cell(row, column));
				boardState.updateCellsWithOwnBoats(player);

				// More boats to place?
				Player player2 = testNextToPlace();
				if (player2 != null && player2 == player) {
					// More boats for same player
					batailleNavale.setActionText(player.getName() + " place "
							+ player.getShipToPlace());
				} else if (player2 != null && player2 != player) {
					// No more boats for current player
					batailleNavale
							.processEvent(batailleNavale.ALL_BOATS_PLACED);
				} else {
					// No more boats to place
					batailleNavale
							.processEvent(batailleNavale.ALL_BOATS_PLACED);

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
					boardState.setCellState(row, column, CellState.SUNK);
					boardState.updateCells(false);
					displayToast(shipHit + " sunk!");
				} else {
					boardState.setCellState(row, column, CellState.HIT);
					displayToast(shipHit + " hit!");
				}
			} else {
				boardState.setCellState(row, column, CellState.MISS);
				displayToast("Missed!");
			}


			if (opponent.checkLost()) {
				batailleNavale.processEvent(batailleNavale.WON, current.getName()
						+ " won!  Game over!");

				moreShotsNeeded = false;
			} else {
				batailleNavale.processEvent(batailleNavale.SHOT_FIRED,
						current.getName() + " turn complete ");
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
	 * Displays a fleeting message.
	 * 
	 * @param message
	 */
	private void displayToast(String message) {
		batailleNavale.displayToast(message);
	}
}
