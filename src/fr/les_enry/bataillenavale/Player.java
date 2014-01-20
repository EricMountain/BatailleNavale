package fr.les_enry.bataillenavale;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds ship state, shots fired etc.
 */
public class Player implements Serializable {
	
	private static final long serialVersionUID = -5674373709357780733L;

	private String name;
	
	private List<Shot> shotsFired = new ArrayList<Shot>();
	
	private List<Ship> shipsToPlace = new ArrayList<Ship>();
	private List<Ship> shipsAfloat = new ArrayList<Ship>();
	private List<Ship> shipsSunk = new ArrayList<Ship>();
	
	private boolean isTurn = false;
	
	/** Player's colour.  Opaque black by default. */
	private int colour = 0xFF000000;
	
	Player(String name, int colour, Ship... ships) {
		this.name = name;
		this.colour = colour;
		
		addShipsToPlace(ships);
	}
	
	void addShipsToPlace(Ship... ships) {
		for (Ship ship : ships) {
			shipsToPlace.add(ship);
		}
	}
	
	String getName() {
		return name;
	}
	
	int getColour() {
		return colour;
	}

	void resetPlayer() {
		shipsToPlace.addAll(shipsAfloat);
		shipsToPlace.addAll(shipsSunk);
		
		for (Ship ship : shipsToPlace)
			ship.reset();
		
		shipsAfloat.clear();
		shipsSunk.clear();
		
		shotsFired.clear();
		
//		addShipsToPlace();
	}
	
	boolean hasShipsToPlace() {
		return !getShipsToPlace().isEmpty();
	}
	
	Ship getShipToPlace() {
		return getShipsToPlace().isEmpty() ? null : getShipsToPlace().get(0);
	}
	
	boolean addShipToCell(Cell newCell) throws BadPlacementException {
		List<Ship> allShips = new ArrayList<Ship>();
		allShips.addAll(shipsAfloat);
		allShips.addAll(shipsToPlace);
		
		boolean isFullyPlaced = getShipToPlace().addToCell(newCell, allShips);
		
		if (isFullyPlaced) {
			getShipsAfloat().add(getShipsToPlace().remove(0));
		}
		
		return isFullyPlaced;
	}
	
	Ship fireShot(Shot newShot, Player opponent) throws AlreadyPlayedException {
		// Check if it is this player's turn
		if (!isTurn) {
			throw new AlreadyPlayedException("Already played.");
		}
		
		// Check if this shot has already been played before
		for(Shot shot : getShotsFired()) {
			if (shot.equals(newShot)) {
				throw new AlreadyPlayedShotException("Already played shot: " + shot);
			}
		}
		
		// Check if a ship is hit by the shot
		Ship shipHit = opponent.receiveShot(newShot);
		
		getShotsFired().add(newShot);
		
		isTurn = false;
		
		return shipHit;
	}
	
	Ship receiveShot(Shot newShot) {
		Ship ship = newShot.applyShot(getShipsAfloat());
		
		if (ship != null && ship.checkSunk()) {
			getShipsAfloat().remove(ship);
			getShipsSunk().add(ship);
		}
		
		return ship;
	}
	
	boolean checkLost() {
		return getShipsAfloat().isEmpty();
	}

	@Override
	public boolean equals(Object o) {
		return name.equals(((Player) o).name);
	}

	@Override
	public String toString() {
		return name;
	}

	List<Shot> getShotsFired() {
		return shotsFired;
	}

	void setShotsFired(List<Shot> shotsFired) {
		this.shotsFired = shotsFired;
	}

	List<Ship> getShipsToPlace() {
		return shipsToPlace;
	}

	void setShipsToPlace(List<Ship> shipsToPlace) {
		this.shipsToPlace = shipsToPlace;
	}

	List<Ship> getShipsAfloat() {
		return shipsAfloat;
	}

	void setShipsAfloat(List<Ship> shipsAfloat) {
		this.shipsAfloat = shipsAfloat;
	}

	List<Ship> getShipsSunk() {
		return shipsSunk;
	}

	void setShipsSunk(List<Ship> shipsSunk) {
		this.shipsSunk = shipsSunk;
	}

	boolean isTurn() {
		return isTurn;
	}

	void allowTurn() {
		this.isTurn = true;
	}
}
