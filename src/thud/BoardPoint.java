package thud;

/**
 * Created by Thai Flowers on 6/15/2017.
 *
 * Represents a position on the game board. Formerly called a position so much of the code takes args named *pos.
 * Many methods take a BoardPoint as argument as a way of making sure input is valid (if constructor works then valid pos)
 */
public class BoardPoint {

	int row;
	int col;

	public static boolean isOnBoard(int x, int y) {
		return (!(x<0 || x>14) && !(y<0 || y>14));
	}

	public BoardPoint(BoardPoint bp) {
		this.row = bp.row;
		this.col = bp.col;
	}
	public BoardPoint(int row, int col) {
		if (!isOnBoard(row, col))
			throw new IllegalArgumentException();
		this.row = row;
		this.col = col;
	}
	public BoardPoint(String s) {
		char col = Character.toUpperCase(s.charAt(0));
		if (!('A' <= col && col <= 'P'))
			throw new IllegalArgumentException("Column must be between A and P");

		int row = Integer.parseInt(s.substring(1));
		if (!(1 <= row && row <= 15))
			throw new IllegalArgumentException("Row must be between 1 and 15");

		this.row = row - 1;
		this.col = col - 'A';
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append((char)(col+'A'));
		sb.append(row+1);
		return sb.toString();
	}

	public int getRow() {
		return row;
	}

	public void setRow(int row) {
		if (!isOnBoard(row, col))
			throw new IllegalArgumentException();
		this.row = row;
	}

	public int getCol() {
		return col;
	}

	public void setCol(int col) {
		if (!isOnBoard(row, col))
			throw new IllegalArgumentException();
		this.col = col;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		BoardPoint that = (BoardPoint) o;

		if (row != that.row) return false;
		return col == that.col;
	}
}
