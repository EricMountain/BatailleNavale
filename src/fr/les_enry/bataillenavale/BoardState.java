package fr.les_enry.bataillenavale;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import fr.les_enry.bataillenavale.Cell.CellState;

class BoardState {
	/** Tag for logs. **/
	private static final String TAG = "BoardState";

	/** Game board. */
	private List<Cell> board = new ArrayList<Cell>();

	/** Reference to game state. */
	private final GameState gameState;
	
	/** Number of columns on the board. */
	private final int columns;

	/** True if player can take a shot in this view, false o/w. */
	private boolean isPlayerViewShootable = false;

	/**
	 * Constructor.
	 * 
	 * @param gameState
	 */
	BoardState(GameState gameState) {
		this.gameState = gameState;
		this.columns = GameState.NB_COLS;

		for (int row = 0; row < GameState.NB_ROWS; row++) {
			for (int column = 0; column < columns; column++) {
				Cell cell = new Cell(row, column);
				board.add(cell);
			}
		}

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
		return board.get(row * columns + column);
	}
	
	/**
	 * Sets state of the specified cell.
	 * 
	 * @param row
	 * @param column
	 * 
	 */
	void setCellState(int row, int column, CellState newState) {
		getCell(row, column).setState(newState);
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
			updateCellsWithOwnBoats(gameState.getCurrentPlayer());
		else
			updateCellsWithPlayerShots(gameState.getCurrentPlayer());
	}

	/**
	 * Draws a player's own boats view.
	 * 
	 * @param player
	 *            Player whose boats to display.
	 */
	void updateCellsWithOwnBoats(Player player) {
		List<Ship> allShips = new ArrayList<Ship>();
		allShips.addAll(player.getShipsAfloat());
		allShips.addAll(player.getShipsToPlace());
		allShips.addAll(player.getShipsSunk());

		Log.d(TAG, "Updating boat placement view for " + player);

		resetCells();

		// Show shots taken by opponent
		Player opponent = gameState.getOpponent();
		for (Shot shot : opponent.getShotsFired()) {
			int cellOffset = shot.getRow() * columns + shot.getColumn();
			board.get(cellOffset).setState(Cell.CellState.MISS);
		}

		// Show state of own ships
		for (Ship ship : allShips) {
			for (Cell shipCoordinates : ship.getCoordinates()) {
				int cellOffset = shipCoordinates.getRow() * columns
						+ shipCoordinates.getColumn();
				board.get(cellOffset).setState(Cell.CellState.SHIP);
			}

			boolean isSunk = ship.checkSunk();
			Cell.CellState state = isSunk ? Cell.CellState.SUNK
					: Cell.CellState.HIT;

			for (Shot shot : ship.getHits()) {
				int cellOffset = shot.getRow() * columns + shot.getColumn();
				board.get(cellOffset).setState(state);
			}
		}

		// Player can't shoot on this view
		isPlayerViewShootable = false;
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
			int cellOffset = shot.getRow() * columns + shot.getColumn();
			board.get(cellOffset).setState(Cell.CellState.MISS);
		}

		Player opponent = gameState.getOpponent();
		List<Ship> allShips = new ArrayList<Ship>();
		allShips.addAll(opponent.getShipsAfloat());
		allShips.addAll(opponent.getShipsSunk());
		for (Ship opponentShip : allShips) {
			boolean isSunk = opponentShip.checkSunk();
			Cell.CellState state = isSunk ? Cell.CellState.SUNK
					: Cell.CellState.HIT;

			for (Shot shot : opponentShip.getHits()) {
				int cellOffset = shot.getRow() * columns + shot.getColumn();
				board.get(cellOffset).setState(state);
			}
		}

		// Player can shoot on this view
		isPlayerViewShootable = true;
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
