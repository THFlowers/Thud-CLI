package thud;

/**
 * Created by Thai Flowers on 6/15/2017.
 */
public class BoardPoint {
    int x;
    int y;

    public static boolean isOnBoard(int x, int y) {
        return (!(x<0 || x>14) && !(y<0 || y>14));
    }

    public BoardPoint(BoardPoint bp) {
        this.x = bp.x;
        this.y = bp.y;
    }
    public BoardPoint(int x, int y) {
        if (!isOnBoard(x,y))
            throw new IllegalArgumentException();
        this.x=x;
        this.y=y;
    }
    public BoardPoint(String s) {
        char col = Character.toUpperCase(s.charAt(0));
        if (!('A' <= col && col <= 'P'))
            throw new IllegalArgumentException("Column must be between A and P");

        int row = Integer.parseInt(s.substring(1));
        if (!(1 <= row && row <= 15))
            throw new IllegalArgumentException("Row must be between 1 and 15");

        this.x = row - 1;
        this.y = col - 'A';
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        if (!isOnBoard(x, y))
            throw new IllegalArgumentException();
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        if (!isOnBoard(x, y))
            throw new IllegalArgumentException();
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BoardPoint that = (BoardPoint) o;

        if (x != that.x) return false;
        return y == that.y;
    }
}
