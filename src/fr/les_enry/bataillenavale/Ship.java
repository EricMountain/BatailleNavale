package fr.les_enry.bataillenavale;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.util.Log;

class Ship {
	private static final String TAG = "Ship";

	private List<Cell> coordinates = new ArrayList<Cell>();
	private List<Shot> hits = new ArrayList<Shot>();

	private String type;
	private int size;
	
	protected Ship(String type, int size) {
		this.type = type;
		this.size = size;
	}

	
	
	@Override
	public String toString() {
		return type + "[" + size + "]";
	}



	List<Shot> getHits() {
		return hits;
	}
	
	/**
	 * Adds a coordinate to the list of coordinates for this ship.
	 * 
	 * @return true if no more coordinates needed, false o/w.
	 */
	boolean addToCell(Cell newCoordinates, List<Ship> allShips) throws BadPlacementException {
		Log.d(TAG, "Adding placement " + newCoordinates);
		
		// Check new coordinate is valid
		// If empty, OK
		// If same col or same row as existing, OK
		// Must occupy contiguous cells
		boolean isNewCoordinatesOK = false;
		if (!coordinates.isEmpty()) {
			Log.d(TAG, "Current coordinates not empty");
			
			// There are already some coordinates, so must check consistency
			boolean isSameRow = checkSameRow(newCoordinates);
			boolean isSameColumn = checkSameColumn(newCoordinates);
			
			Log.d(TAG, "New coordinates - same row = " + isSameRow + ", same col = " + isSameColumn);
			
			if (isSameRow || isSameColumn) {
				isNewCoordinatesOK = checkRangeIsContinuous(newCoordinates, isSameRow);
				Log.d(TAG, "Is range continuous = " + isNewCoordinatesOK);
			}
		} else {
			Log.d(TAG, "Current coordinates empty => OK");
			isNewCoordinatesOK = true;
		}
		
		// Check if cell already occupied
		for (Ship ship : allShips) {
			for (Cell coordinate : ship.getCoordinates()) {
				if (newCoordinates.equals(coordinate)) {
					Log.d(TAG, "New coordinates already occupied.");
					isNewCoordinatesOK = false;
				}
			}
		}
		
		if (!isNewCoordinatesOK) {
			throw new BadPlacementException("Can't put ship there.");
		}
		
		coordinates.add(newCoordinates);
		
		boolean isFullyPlaced = (coordinates.size() == size);
		
		Log.d(TAG, "Adding coord " + newCoordinates + " to " + this + " => "
				+ (isFullyPlaced ? "fully placed" : "not fully placed"));
		
		return isFullyPlaced;
	}
	
	boolean checkRangeIsContinuous(Cell newCoordinates, boolean isSameRow) {
		List<Integer> integers = new ArrayList<Integer>();
		
		for (Cell coordinate : coordinates) {
			if (isSameRow) {
				integers.add(coordinate.getColumn());
			} else {
				integers.add(coordinate.getRow());
			}
		}
		
		if (isSameRow) {
			integers.add(newCoordinates.getColumn());
		} else {
			integers.add(newCoordinates.getRow());
		}
		
		Collections.sort(integers);
		int lastInt = -1;
		for (Integer integer : integers) {
			Log.d(TAG, "Checking continuity: " + integer);
			if (lastInt == -1) {
				lastInt = integer;
			} else if (integer != lastInt + 1) {
				Log.d(TAG, "Continuity failure.");
				return false;
			} else {
				lastInt = integer;
			}
		}
		
		Log.d(TAG, "Continuity OK.");
		
		return true;
	}
	
	boolean checkSameRow(Cell newCoordinates) {
		for (Cell coordinate : coordinates) {
			if (newCoordinates.getRow() != coordinate.getRow())
				return false;
		}
		
		return true;
	}

	boolean checkSameColumn(Cell newCoordinates) {
		for (Cell coordinate : coordinates) {
			if (newCoordinates.getColumn() != coordinate.getColumn())
				return false;
		}
		
		return true;
	}
	
	boolean checkHit(Shot shot) {
		Log.d(TAG, this + "has already taken hits: " + hits.size());
		
		for (Shot hit : hits) {
			if (hit.equals(shot)) {
				throw new RuntimeException("Already hit here: " + shot);
			}
		}
		
		// TODO Think equals is supposed to be true if the objects are one and the same?  OR is it 
		// just some kind of equivalence?  Shouldn't we rather implement Comparable?
		for (Cell coordinate : coordinates) {
			if (coordinate.equals(shot)) {
				Log.d(TAG, this + " has been hit by this shot");
				hits.add(shot);
				checkSunk();
				return true;
			}
		}
		
		Log.d(TAG, this + " was not hit by this shot");
		
		return false;
	}

	boolean checkSunk() {
		boolean isSunk = (size == hits.size());
		Log.d(TAG, this + " is " + (isSunk ? "sunk" : "afloat"));
		return isSunk;		
	}

	String getType() {
		return type;
	}
	
	List<Cell> getCoordinates() {
		return coordinates;
	}
}

class AircraftCarrier extends Ship {
	
	protected AircraftCarrier() {
		super("Porte-avions", 5);
	}
}

class Cruiser extends Ship {
	
	protected Cruiser() {
		super("Croiseur", 4);
	}
}

class Submarine extends Ship {
	
	protected Submarine() {
		super("Sous-marin", 3);
	}
}

class CounterTorpedo extends Ship {
	
	protected CounterTorpedo() {
		super("Contre-torpilleur", 3);
	}
}

class Torpedo extends Ship {
	
	protected Torpedo() {
		super("Torpilleur", 2);
	}
}
