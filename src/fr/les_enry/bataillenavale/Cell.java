package fr.les_enry.bataillenavale;

/**
 * Simple cell.
 */
class Cell {
	/** Row coordinate. */
	private final int row;
	
	/** Colmun coordinate. */
	private final int column;
	
	/**
	 * Constuctor.
	 * 
	 * @param row
	 * @param column
	 */
	Cell(int row, int column) {
		this.row = row;
		this.column = column;
	}
	
	/**
	 * Get row.
	 * 
	 * @return row
	 */
	public int getRow() {
		return row;
	}
	
	/**
	 * Get column.
	 * 
	 * @return column.
	 */
	public int getColumn() {
		return column;
	}

	@Override
	public boolean equals(Object otherObj) {
		Cell other = (Cell) otherObj;
		return other.row == this.row && other.column == this.column;
	}

	@Override
	public String toString() {
		return "(" + row + "," + column +")";
	}
}
