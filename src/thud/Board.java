package thud;

import java.util.Arrays;

/**
 * Created by Thai Flowers on 5/24/2017.
 */
public class Board {
    private BoardStates[][] board = new BoardStates[15][15];
    private int numTrolls=0, numDwarfs=0;

    public Board() {
        // set board to empty state (no pieces)
        initializeBoard();
    }

    public int getNumTrolls() {
        return numTrolls;
    }
    // TODO: remove this and replace with list of each piece and remove/replace routines
    public void setNumTrolls(int numTrolls) { this.numTrolls = numTrolls; }

    public int getNumDwarfs() {
        return numDwarfs;
    }
    // TODO: remove this and replace with list of each piece and remove/replace routines
    public void setNumDwarfs(int numDwarfs) { this.numDwarfs = numDwarfs; }

    // Set board to empty state, that is set valid and forbidden cells only
    // This is correct for Koom Valley Thud and regular Thud, thus here and not in Player
    void initializeBoard() {
        for (int i=0; i<15; i++) {
            Arrays.fill(board[i], BoardStates.FREE);
        }
        for (int i=0; i<5; i++) {
            for (int j=0; j<(5-i); j++) {
                board[i][j] = BoardStates.FORBIDDEN;
                board[i][14 - j] = BoardStates.FORBIDDEN;
                board[14-i][j] = BoardStates.FORBIDDEN;
                board[14-i][14-j] = BoardStates.FORBIDDEN;
            }
        }
    }

    @Override
    public String toString() { return printBoard(true); }
    public String printBoard(boolean decorate) {
        StringBuilder sb = new StringBuilder(256);
        for (int i=-1; i<15; i++) {
            if (decorate) {
                // Decorate edge of board with row number
                if (i != -1) { // But not the decorator row
                    sb.append(String.format("%2d ", i+1));
                } else {
                    sb.append("   ");
                }
            }

            for (int j=0; j<15; j++) {

                if (i == -1) {
                    // Decorate edge of board with column letter
                    if (decorate) {
                        sb.append((char) ('A' + j));
                        sb.append(' ');
                    }
                }
                else {

                    char c = getSymbol(board[i][j]);
                    sb.append(c);
                    sb.append(' ');
                }
            }
            if ( !((i==-1 && !decorate) || i==14 ) )
                sb.append('\n');
        }
        return sb.toString();
    }

    private char getSymbol(BoardStates boardStates) {
        char c = ' ';
        switch (boardStates) {
            case FORBIDDEN:
                c = '#';
                break;
            case DWARF:
                c = 'D';
                break;
            case TROLL:
                c = 'T';
                break;
            case STONE:
                c = 'S';
                break;
            case FREE:
                c = ' ';
                break;
        }
        return c;
    }


    public BoardStates getAtPosition(int[] pos) {
        if ((pos.length != 2) || !positionOnBoard(pos))
            throw new IllegalArgumentException();

        return board[pos[0]][pos[1]];
    }

    void setAtPosition(int[] pos, BoardStates state) {
        if ((pos.length != 2) || !positionOnBoard(pos))
            throw new IllegalArgumentException();

        board[pos[0]][pos[1]] = state;
    }

    // assumes valid pos (including size and positionOnBoard)
    public boolean adjacentToAny(BoardStates state, int[] pos) {
        /*
        if (pos.length != 2)
            throw new IllegalArgumentException();
        */

        int[] testPos = new int[2];
        for (int i=-1; i<=1; i++) {
            for (int j=-1; j<=1; j++) {
                // ignore pos
                if (i==0 && j==0)
                    continue;
                // don't allow pos[0]+i out of bounds
                if ((pos[0]+i < 0) || (pos[0]+i > 14))
                    continue;
                // don't allow pos[1]+j out of bounds
                if ((pos[1]+j < 0) || (pos[1]+j > 14))
                    continue;

                testPos[0] = pos[0]+i;
                testPos[1] = pos[1]+j;

                if (getAtPosition(testPos).equals(state))
                    return true;
            }
        }
        return false;
    }

    // assumes pos is correct size
    public boolean positionOnBoard(int[] pos) {
        if ((pos[0] < 0) || (pos[0] > 14))
            return false;
        if ((pos[1] < 0) || (pos[1] > 14))
            return false;
        return true;
    }

    // assumes valid startPos and endPos (including size and positionOnBoard)
    public boolean positionsAreLinear(int[] startPos, int[] endPos) {
        /*
        if (!(startPos.length==2) || !(endPos.length==2))
            throw new IllegalArgumentException();
        */

        return (startPos[0]==endPos[0]) ||
               (startPos[1]==endPos[1]) ||
               (Math.abs((startPos[1]-endPos[1])/(startPos[0]-endPos[0])) == 1);
    }

    public int[] notationToPosition(String s) {
        int[] pos = new int[2];

        char col = Character.toUpperCase(s.charAt(0));
        if (!('A' <= col && col <= 'P'))
            throw new IllegalArgumentException("Column must be between A and P");

        int row = Integer.parseInt(s.substring(1));
        if (!(1 <= row && row <= 15))
            throw new IllegalArgumentException("Row must be between 1 and 15");

        pos[0] = row-1;
        pos[1] = col-'A';
        return pos;
    }

    public void movePiece(int[] startPos, int[] endPos) {
        BoardStates bs = getAtPosition(startPos);
        setAtPosition(startPos, BoardStates.FREE);
        setAtPosition(endPos, bs);
    }

    public void removePiece(int[] pos) {
        BoardStates bs = getAtPosition(pos);
        switch (bs) {
            case TROLL:
                numTrolls--;
                break;
            case DWARF:
                numDwarfs--;
                break;
            default:
                throw new IllegalArgumentException();
        }
        setAtPosition(pos, BoardStates.FREE);
    }
}
