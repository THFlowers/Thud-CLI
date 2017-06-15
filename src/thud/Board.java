package thud;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.abs;

/**
 * Created by Thai Flowers on 5/24/2017.
 */
public class Board {
    private BoardStates[][] board = new BoardStates[15][15];
    private int numTrolls=0, numDwarfs=0;
    private List<String> moveLog = new ArrayList<>();

    public Board() {
        // set board to empty state (no pieces)
        initializeBoard();
    }

    // replay the moves for a single round
    public PlayState replayMoveLog(List<String> moveLog) {
        initializeBoard();
        initializeGame();
        PlayState turn = new PlayState();
        for (String move : moveLog)
            play(turn, move);
        this.moveLog = moveLog;
        return turn;
    }

    public List<String> getMoveLog() { return moveLog; }

    public String getLastMove() {
        int size = moveLog.size();
        return (size == 0) ? " " : moveLog.get(size-1);
    }

    public int getNumTrolls() {
        return numTrolls;
    }

    public int getNumDwarfs() {
        return numDwarfs;
    }

    private void initializeBoard() {
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

    // set board to initial game state (all pieces in default position)
    public void initializeGame() {
        initializeBoard();

        numTrolls = 8;
        numDwarfs = 32;

        // Set thud stone in center surrounded by the Trolls
        board[7][7] = BoardStates.STONE;
        for (int i=6; i<9; i++) {
            for (int j=6; j<9; j++) {
                if (i!=7 || j!=7)
                    board[i][j] = BoardStates.TROLL;
            }
        }
        // Dwarf Diagonals
        int i=5, j=0;
        for (int count=0; count<6; count++) {
            board[i][j] = BoardStates.DWARF;
            board[i][14-j] = BoardStates.DWARF;
            board[14-i][j] = BoardStates.DWARF;
            board[14-i][14-j] = BoardStates.DWARF;

            i--;
            j++;
        }

        // Dwarf straights
        for (i=6; i<9; i++) {
            // Skip middles
            if (i==7)
                continue;

            board[i][0] = BoardStates.DWARF;
            board[i][14] = BoardStates.DWARF;
            board[0][i] = BoardStates.DWARF;
            board[14][i] = BoardStates.DWARF;
        }
    }

    public BoardStates getAtPosition(int[] pos) {
        if ((pos.length != 2) || !positionOnBoard(pos))
            throw new IllegalArgumentException();

        return board[pos[0]][pos[1]];
    }

    private void setAtPosition(int[] pos, BoardStates state) {
        if ((pos.length != 2) || !positionOnBoard(pos))
            throw new IllegalArgumentException();

        board[pos[0]][pos[1]] = state;
    }

    // TODO: refactor parsing/validation outside this class, pass in only encoded movement order
    public void play(PlayState turn, String move) {

        move = move.toUpperCase(); // for uniformity and to ease comparisons
        String[] order = move.split(" ");

        if (turn.isRemoveTurn()) {
             if (!order[0].equals("R"))
                 throw new IllegalArgumentException("Remove turn must be a remove command!");
             if (!turn.isTurn(BoardStates.TROLL))
                 // shouldn't get here
                 throw new IllegalArgumentException("Only Trolls can remove in standard Thud!");

            int[][] removePositions = new int[Math.max(order.length-1,0)][];
            for (int i=1; i<order.length; i++) {
                removePositions[i - 1] = notationToPosition(order[i]);
            }

            removePlay(removePositions);
            turn.setRemoveTurn(false);
        }
        else {

            if (order.length != 3)
                throw new IllegalArgumentException("Move must be of form Command StartPos [EndPos]");
            if (order[0].length() != 1)
                throw new IllegalArgumentException("Command must be a single letter");

            char ch = order[0].charAt(0);

            int[] startPos = notationToPosition(order[1]);
            int[] endPos = notationToPosition(order[2]);

            /* redundant as notationToPosition should generate valid positions, kept in case refactor introduces need
            if (!positionOnBoard(startPos) || !positionOnBoard(endPos))
                throw new IllegalArgumentException("Start and end positions must be at valid board positions");
            */

            if (Arrays.equals(startPos, endPos))
                throw new IllegalArgumentException("Movement can't be to the same square");

            boolean removeTurn;
            switch (turn.getTurn()) {
                case DWARF:
                    removeTurn = playDwarf(ch, startPos, endPos);
                    turn.setRemoveTurn(removeTurn);
                    break;
                case TROLL:
                    removeTurn = playTroll(ch, startPos, endPos);
                    turn.setRemoveTurn(removeTurn);
                    break;
                // TODO: May become redundant after PlayState guards refactor or new enum
                default:
                    throw new IllegalArgumentException("Turn must be either Troll or Dwarf");
            }
        }

        // necessary for multi-move turns (that is Troll captures)
        moveLog.add(move);
        turn.alternateTurn();
    }

    // Troll only special play, assumes this is already checked (as in play() above)
    private void removePlay(int[][] removePositions) {
        // use log to retrieve last move endPos and command
        // if old command is 'M' or 'R' option capture (empty line after 'R')
        // if old command is 'S' MUST capture at least one dwarf
        String prevMove = moveLog.get(moveLog.size()-1);
        String[] prevOrder = prevMove.split(" ");

        boolean mustCapture;
        switch (prevOrder[0].charAt(0)) {
            case 'M':
            case 'R':
                mustCapture = false;
                break;
            case 'S':
                mustCapture = true;
                break;
            default:
                throw new IllegalArgumentException("Previous move doesn't allow captures!");
        }

        // retrieve anchorPos (old endPos)
        String oldMove = moveLog.get(moveLog.size()-1);
        String[] oldOrder = oldMove.split(" ");

        int[] anchorPos = notationToPosition(oldOrder[2]);

        if (mustCapture && (removePositions.length == 0))
            throw new IllegalArgumentException("You must capture at least one dwarf");

        for (int[] pos : removePositions) {
            if (!getAtPosition(pos).equals(BoardStates.DWARF))
                throw new IllegalArgumentException("Not a dwarf");
            if (abs(anchorPos[0]-pos[0]) > 1 || abs(anchorPos[1]-pos[1]) > 1)
                throw new IllegalArgumentException("Dwarf is not adjacent to the troll");

            setAtPosition(pos, BoardStates.FREE);
            numDwarfs--;
        }
    }

    // TODO: refactor parsing/validation outside this class, pass in only encoded movement order
    private boolean playDwarf(char command, int[] startPos, int[] endPos) {

        switch (command) {
            case 'M':
                if (!getAtPosition(startPos).equals(BoardStates.DWARF))
                    throw new IllegalArgumentException("Start piece is not a dwarf");
                if (!getAtPosition(endPos).equals(BoardStates.FREE))
                    throw new IllegalArgumentException("End position is not free (did you mean to 'H'url?)");
                if (!positionsAreLinear(startPos, endPos))
                    throw new IllegalArgumentException("Dwarf must move like chess queen");

                 setAtPosition(startPos, BoardStates.FREE);
                 setAtPosition(endPos, BoardStates.DWARF);

                 break;
            case 'H':
                if (!getAtPosition(startPos).equals(BoardStates.DWARF))
                    throw new IllegalArgumentException("Start piece is not a dwarf");
                if (!getAtPosition(endPos).equals(BoardStates.TROLL))
                    throw new IllegalArgumentException("End position is not a troll");
                if (!distanceAttackCheck(BoardStates.DWARF, startPos, endPos))
                    throw new IllegalArgumentException("Hurl path is not clear or not edge troll");

                setAtPosition(startPos, BoardStates.FREE);
                setAtPosition(endPos, BoardStates.DWARF);
                numTrolls--;

                break;
            default:
                throw new IllegalArgumentException("Dwarf Command must be 'M'ove or 'H'url");
        }
        // dwarf never has multi-part moves, but need to maintain simple + consistent interface
        return false;
    }

    // TODO: refactor parsing/validation outside this class, pass in only encoded movement order
    // assumes play validates startPos and endPos
    private boolean playTroll(char command, int[] startPos, int[] endPos) {
        switch (command) {
            case 'M':
                if (!getAtPosition(startPos).equals(BoardStates.TROLL))
                    throw new IllegalArgumentException("Start piece is not a troll");
                if (!getAtPosition(endPos).equals(BoardStates.FREE))
                    throw new IllegalArgumentException("End position is not free");

                if (abs(startPos[0]-endPos[0]) > 1 || abs(startPos[1]-endPos[1]) > 1)
                    throw new IllegalArgumentException("Troll must move like chess king");

                setAtPosition(startPos, BoardStates.FREE);
                setAtPosition(endPos, BoardStates.TROLL);

                // if we end up next to dwarfs, allow captures
                return adjacentToAny(BoardStates.DWARF, endPos);
            case 'S':
                if (!getAtPosition(startPos).equals(BoardStates.TROLL))
                    throw new IllegalArgumentException("Start piece is not a troll");
                if (!getAtPosition(endPos).equals(BoardStates.FREE))
                    throw new IllegalArgumentException("End position is not free");
                if (!adjacentToAny(BoardStates.DWARF, endPos))
                    throw new IllegalArgumentException("End position is not adjacent to a dwarf");
                if (!distanceAttackCheck(BoardStates.TROLL, startPos, endPos))
                    throw new IllegalArgumentException("Shove path is not clear or not edge troll");

                setAtPosition(startPos, BoardStates.FREE);
                setAtPosition(endPos, BoardStates.TROLL);

                return true;
            default:
                throw new IllegalArgumentException("Troll Command must be 'M'ove or 'S'hove");
        }
    }

    // assumes valid startPos and endPos (including size and positionOnBoard)
    // assumes rules for endPos are already enforced (Troll land adjacent to Dwarf, Dwarf land on Troll)
    private boolean distanceAttackCheck(BoardStates turn, int[] startPos, int[] endPos) {
        // figure out type of line (3 cases), and travel direction
        // then traverse backwards to get max shove/hurl distance
        // then proceed in proper direction to check that there is no blocking piece
        int iStep=0, jStep=0;
        int[] curPos = Arrays.copyOf(startPos,2);
        if (startPos[0]==endPos[0])
            jStep = (startPos[1]<endPos[1]) ? 1 : -1;
        else if (startPos[1]==endPos[1])
            iStep = (startPos[0]<endPos[0]) ? 1 : -1;
        else if (Math.abs((startPos[1]-endPos[1])/(startPos[0]-endPos[0])) == 1) {
            iStep = (startPos[0]<endPos[0]) ? 1 : -1;
            jStep = (startPos[1]<endPos[1]) ? 1 : -1;
        }
        else return false;

        int numInLine = 0;
        do {
            curPos[0] -= iStep;
            curPos[1] -= jStep;
            numInLine++;
        } while(getAtPosition(curPos).equals(turn));

        if ((numInLine == 1) && turn.equals(BoardStates.TROLL))
                throw new IllegalArgumentException("Shove must be at least 2 trolls");

        curPos = Arrays.copyOf(startPos,2);
        for (int c = 0; c < numInLine; c++) {
            curPos[0] += iStep;
            curPos[1] += jStep;
            if (Arrays.equals(curPos, endPos))
                return true;
            if (!getAtPosition(curPos).equals(BoardStates.FREE))
                return false;
        }
        return false;
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
    private boolean positionOnBoard(int[] pos) {
        if ((pos[0] < 0) || (pos[0] > 14))
            return false;
        if ((pos[1] < 0) || (pos[1] > 14))
            return false;
        return true;
    }

    // assumes valid startPos and endPos (including size and positionOnBoard)
    private boolean positionsAreLinear(int[] startPos, int[] endPos) {
        /*
        if (!(startPos.length==2) || !(endPos.length==2))
            throw new IllegalArgumentException();
        */

        return (startPos[0]==endPos[0]) ||
               (startPos[1]==endPos[1]) ||
               (Math.abs((startPos[1]-endPos[1])/(startPos[0]-endPos[0])) == 1);
    }

    // TODO: refactor parsing/validation outside this class, pass in only encoded movement order
    int[] notationToPosition(String s) {
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
}
