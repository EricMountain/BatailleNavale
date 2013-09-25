package fr.les_enry.bataillenavale;

import java.util.List;

public class Shot extends Cell {

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
