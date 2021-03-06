package fr.les_enry.bataillenavale;

import java.io.Serializable;
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
class GameState implements Serializable {

	private static final long serialVersionUID = 322809421085556089L;

	/** Tag for logs. **/
	private static final String TAG = "GameState";

	/** Rows on the board. */
	static final int NB_ROWS = 10;

	/** Columns on the board. */
	static final int NB_COLS = 10;

	/** Flying spaghetti monster. */
	private FSM fsm = new FSM("GameState");

	private final State INIT = fsm.state("Initial");
	private final State BOAT_TO_PLACE = fsm.state("Boat to place");
	private final State SHOT_NEEDED = fsm.state("Shot needed");
	private final State GAME_OVER = fsm.state("Game over");

	final Event START = fsm.event("Start");
	final Event CELL_ACTIVATED = fsm.event("Cell activated");
	final Event RESET = fsm.event("Reset game");

	/** Current BatailleNavale activity. */
	private transient BatailleNavale batailleNavale;

	/** Board representation of the game. */
	private BoardState boardState = new BoardState(this);

	/** List of players in the game. */
	private List<Player> players = new ArrayList<Player>();

	/** Index of player that must place a ship or take a shot. */
	private int currentPlayer = 1;

	/**
	 * Initialises game state with 2 players.
	 */
	GameState(BatailleNavale batailleNavale) {
		Log.d(TAG, "Constructing new game state");

		this.batailleNavale = batailleNavale;

		players.add(new Player(
				batailleNavale.getString(R.string.player) + " 1",
				0xff3a0000,
				new AircraftCarrier(batailleNavale
						.getString(R.string.aircraft_carrier)),
				new Cruiser(batailleNavale.getString(R.string.cruiser)),
				new Submarine(batailleNavale.getString(R.string.submarine)),
				new CounterTorpedo(batailleNavale.getString(R.string.destroyer)),
				new Torpedo(batailleNavale.getString(R.string.torpedo_boat))));
		players.add(new Player(
				batailleNavale.getString(R.string.player) + " 2",
				0xff060542,
				new AircraftCarrier(batailleNavale
						.getString(R.string.aircraft_carrier)),
				new Cruiser(batailleNavale.getString(R.string.cruiser)),
				new Submarine(batailleNavale.getString(R.string.submarine)),
				new CounterTorpedo(batailleNavale.getString(R.string.destroyer)),
				new Torpedo(batailleNavale.getString(R.string.torpedo_boat))));

		initFSM();
	}

	void setBatailleNavale(BatailleNavale batailleNavale) {
		this.batailleNavale = batailleNavale;
	}

	/**
	 * Sets up the FSM. Initial state is BOAT_TO_PLACE.
	 */
	@SuppressWarnings("serial")
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
		
		Log.d(TAG, fsm.toDag());
	}

	// TODO Remove, only for debugging
	State getFSMState() {
		return fsm.getState();
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

		batailleNavale.actionButtonSetClickable(false);

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
		batailleNavale.actionButtonSetClickable(true);

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
					batailleNavale.setActionText(player.getName() + ", "
							+ getString(R.string.place) + " "
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
				displayToast(getString(R.string.cannot_place_here));
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
					displayToast(shipHit + " " + getString(R.string.sunk) + "!");
				} else {
					boardState.setCellState(row, column, CellState.HIT);
					displayToast(shipHit + " " + getString(R.string.hit) + "!");
				}
			} else {
				boardState.setCellState(row, column, CellState.MISS);
				displayToast(getString(R.string.missed) + "!");
			}

			if (opponent.checkLost()) {
				batailleNavale.processEvent(batailleNavale.WON,
						current.getName() + " " + getString(R.string.won)
								+ "! " + getString(R.string.game_over));

				moreShotsNeeded = false;
			} else {
				batailleNavale.processEvent(batailleNavale.SHOT_FIRED,
						current.getName() + " "
								+ getString(R.string.turn_finished));
			}
		} catch (AlreadyPlayedShotException e) {
			displayToast(getString(R.string.already_shot));
		} catch (AlreadyPlayedException e) {
			displayToast(getString(R.string.already_played));
		} catch (CantShootHereException e) {
			displayToast(getString(R.string.cannot_shoot_self));
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

	private String getString(int resId) {
		return batailleNavale.getString(resId);
	}
}
