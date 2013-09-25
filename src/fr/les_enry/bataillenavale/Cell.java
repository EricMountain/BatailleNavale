package fr.les_enry.bataillenavale;

public class Cell {
	private final int row;
	private final int column;
	
	Cell(int row, int column) {
		this.row = row;
		this.column = column;
	}
	
	public int getRow() {
		return row;
	}
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
