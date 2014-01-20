package fr.les_enry.bataillenavale;

import java.io.Serializable;
import java.util.List;

/**
 * Holds the coordinates of a shot and checks if it hit a ship.
 */
public class Shot extends Cell implements Serializable {

	private static final long serialVersionUID = 151877224654937911L;

	Shot(int row, int column) {
		super(row, column);
	}

	Ship applyShot(List<Ship> shipsAfloat) {
		for(Ship ship : shipsAfloat) {
			if (ship.checkHit(this)) {
				return ship;
			}
		}
		
		return null;
	}
	
}
