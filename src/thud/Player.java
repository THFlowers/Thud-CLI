package thud;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;

/**
 * Created by Thai Flowers on 6/15/2017.
 */
public class Player {

    private Board board;
    private List<String> moveLog = new ArrayList<>();
    private int[] scores = new int[] {0,0}; // use mod 2 arithmetic to access index while scoring

    public Player(Board board) {
        this.board = board;
    }

    public Player(Player other) {
        this.board = new Board(other.board);
        this.moveLog = new ArrayList<>(moveLog);
    }

    // set board to initial game board (all pieces in default position)
    public void initializeGame() {
        board.initializeBoard();

        // Set thud stone in center surrounded by the Trolls
        board.setAtPosition(new BoardPoint(7, 7), BoardStates.STONE);
        for (int i=6; i<9; i++) {
            for (int j=6; j<9; j++) {
                if (i!=7 || j!=7)
                    board.addTroll(new BoardPoint(i, j));
            }
        }
        // Dwarf Diagonals
        int i=5, j=0;
        for (int count=0; count<6; count++) {
            board.addDwarf(new BoardPoint(i, j));
            board.addDwarf(new BoardPoint(i, 14-j));
            board.addDwarf(new BoardPoint(14-i, j));
            board.addDwarf(new BoardPoint(14-i, 14-j));

            i--;
            j++;
        }

        // Dwarf straights
        for (i=6; i<9; i++) {
            // Skip middles
            if (i==7)
                continue;

            board.addDwarf(new BoardPoint(i, 0));
            board.addDwarf(new BoardPoint(i, 14));
            board.addDwarf(new BoardPoint(0, i));
            board.addDwarf(new BoardPoint(14, i));
        }
    }

    // replay the moves for a single round
    public PlayState replayMoveLog(List<String> moveLog) {
        board.initializeBoard();
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

    void calculateScores(int round) {
        if (round<=0 || round>2)
            throw new IllegalArgumentException();

        scores[(round-1) % 2] = board.getNumDwarfs() * 10;
        scores[(round) % 2] = board.getNumTrolls() * 40;
    }

    public int[] getScores() { return scores; }

    public Board getBoard() { return board; }

    public void play(PlayState turn, String move) {

        move = move.toUpperCase(); // for uniformity and to ease comparisons
        String[] order = move.split(" ");

        if (turn.isRemoveTurn()) {
            if (!order[0].equals("R"))
                throw new IllegalArgumentException("Remove turn must be a remove command!");
            if (!turn.isTurn(BoardStates.TROLL))
                // shouldn't get here
                throw new IllegalArgumentException("Only Trolls can remove in standard Thud!");

            BoardPoint[] removePositions = new BoardPoint[Math.max(order.length-1,0)];
            for (int i=1; i<order.length; i++) {
                removePositions[i - 1] = new BoardPoint(order[i]);
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

            BoardPoint startPos = new BoardPoint(order[1]);
            BoardPoint endPos   = new BoardPoint(order[2]);

            if (startPos.equals(endPos))
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
            }
        }

        // necessary for multi-move turns (that is Troll captures)
        moveLog.add(move);
        turn.alternateTurn();
    }

    // Troll only special play, assumes this is already checked (as in play() above)
    private void removePlay(BoardPoint[] removePositions) {
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

        BoardPoint anchorPos = new BoardPoint(oldOrder[2]);

        if (mustCapture && (removePositions.length == 0))
            throw new IllegalArgumentException("You must capture at least one dwarf");

        for (BoardPoint pos : removePositions) {
            if (!board.getAtPosition(pos).equals(BoardStates.DWARF))
                throw new IllegalArgumentException("Not a dwarf");
            if (abs(anchorPos.x-pos.x) > 1 || abs(anchorPos.y-pos.y) > 1)
                throw new IllegalArgumentException("Dwarf is not adjacent to the troll");

            board.removePiece(pos);
        }
    }

    private boolean playDwarf(char command, BoardPoint startPos, BoardPoint endPos) {

        switch (command) {
            case 'M':
                if (!board.getAtPosition(startPos).equals(BoardStates.DWARF))
                    throw new IllegalArgumentException("Start piece is not a dwarf");
                if (!board.getAtPosition(endPos).equals(BoardStates.FREE))
                    throw new IllegalArgumentException("End position is not free (did you mean to 'H'url?)");
                if (!board.positionsAreLinear(startPos, endPos))
                    throw new IllegalArgumentException("Dwarf must move like chess queen");

                board.movePiece(startPos, endPos);

                break;
            case 'H':
                if (!board.getAtPosition(startPos).equals(BoardStates.DWARF))
                    throw new IllegalArgumentException("Start piece is not a dwarf");
                if (!board.getAtPosition(endPos).equals(BoardStates.TROLL))
                    throw new IllegalArgumentException("End position is not a troll");
                if (!distanceAttackCheck(BoardStates.DWARF, startPos, endPos))
                    throw new IllegalArgumentException("Hurl path is not clear or not edge troll");

                // remember a hurl is a capture
                board.removePiece(endPos);
                board.movePiece(startPos, endPos);

                break;
            default:
                throw new IllegalArgumentException("Dwarf Command must be 'M'ove or 'H'url");
        }
        // dwarf never has multi-part moves, but need to maintain simple + consistent interface
        return false;
    }

    // assumes play validates startPos and endPos
    private boolean playTroll(char command, BoardPoint startPos, BoardPoint endPos) {
        switch (command) {
            case 'M':
                if (!board.getAtPosition(startPos).equals(BoardStates.TROLL))
                    throw new IllegalArgumentException("Start piece is not a troll");
                if (!board.getAtPosition(endPos).equals(BoardStates.FREE))
                    throw new IllegalArgumentException("End position is not free");

                if (abs(startPos.x-endPos.x) > 1 || abs(startPos.y-endPos.y) > 1)
                    throw new IllegalArgumentException("Troll must move like chess king");

                board.movePiece(startPos, endPos);

                // if we end up next to dwarfs, allow captures
                return board.adjacentToAny(BoardStates.DWARF, endPos);
            case 'S':
                if (!board.getAtPosition(startPos).equals(BoardStates.TROLL))
                    throw new IllegalArgumentException("Start piece is not a troll");
                if (!board.getAtPosition(endPos).equals(BoardStates.FREE))
                    throw new IllegalArgumentException("End position is not free");
                if (!board.adjacentToAny(BoardStates.DWARF, endPos))
                    throw new IllegalArgumentException("End position is not adjacent to a dwarf");
                if (!distanceAttackCheck(BoardStates.TROLL, startPos, endPos))
                    throw new IllegalArgumentException("Shove path is not clear or not edge troll");

                // remember a shove isn't a capture, and remove happens on the next (special) turn
                board.movePiece(startPos, endPos);

                return true;
            default:
                throw new IllegalArgumentException("Troll Command must be 'M'ove or 'S'hove");
        }
    }

    // Here b/c it is game specific and depends on game play mechanics, ie low cohesion with board

    // assumes valid startPos and endPos (including size and positionOnBoard)
    // assumes rules for endPos are already enforced (Troll land adjacent to Dwarf, Dwarf land on Troll)
    private boolean distanceAttackCheck(BoardStates turn, BoardPoint startPos, BoardPoint endPos) {
        // figure out type of line (3 cases), and travel direction
        // then traverse backwards to get max shove/hurl distance
        // then proceed in proper direction to check that there is no blocking piece
        int iStep=0, jStep=0;
        BoardPoint curPos = new BoardPoint(startPos);
        if (startPos.x==endPos.x)
            jStep = (startPos.y<endPos.y) ? 1 : -1;
        else if (startPos.y==endPos.y)
            iStep = (startPos.x<endPos.x) ? 1 : -1;
        else if (Math.abs((startPos.y-endPos.y)/(startPos.x-endPos.x)) == 1) {
            iStep = (startPos.x<endPos.x) ? 1 : -1;
            jStep = (startPos.y<endPos.y) ? 1 : -1;
        }
        else return false;

        int numInLine = 0;
        do {
            curPos.x -= iStep;
            curPos.y -= jStep;
            numInLine++;
        } while(board.getAtPosition(curPos).equals(turn));

        if ((numInLine == 1) && turn.equals(BoardStates.TROLL))
            throw new IllegalArgumentException("Shove must be at least 2 trolls");

        curPos = new BoardPoint(startPos);
        for (int c = 0; c < numInLine; c++) {
            curPos.x += iStep;
            curPos.y += jStep;
            if (curPos.equals(endPos))
                return true;
            if (!board.getAtPosition(curPos).equals(BoardStates.FREE))
                return false;
        }
        return false;
    }


    public ArrayList<BoardPoint> getPossibleMoves(BoardPoint pos) {
        switch (board.getAtPosition(pos)) {
            case TROLL:
                return getPossibleTrollMoves(pos);
            case DWARF:
                return getPossibleDwarfMoves(pos);
            default:
                return new ArrayList<>();
        }
    }

    public ArrayList<BoardPoint> getPossibleTrollMoves(BoardPoint pos) {
        ArrayList<BoardPoint> points = new ArrayList<>();

        // movement
        points.addAll(kingMoves(pos));

        return points;
    }

    public ArrayList<BoardPoint> getPossibleDwarfMoves(BoardPoint pos) {
        ArrayList<BoardPoint> points = new ArrayList<>();

        // movement
        points.addAll(kingMoves(pos));

        return points;
    }

    private ArrayList<BoardPoint> kingMoves(BoardPoint pos) {
        ArrayList<BoardPoint> points = new ArrayList<>();
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                // ignore pos
                if (i == 0 && j == 0)
                    continue;
                // don't allow pos[0]+i out of bounds
                if ((pos.x + i < 0) || (pos.x + i > 14))
                    continue;
                // don't allow pos[1]+j out of bounds
                if ((pos.y + j < 0) || (pos.y + j > 14))
                    continue;

                BoardPoint testPos = new BoardPoint(pos.x + i, pos.y + j);

                if (board.getAtPosition(testPos).equals(BoardStates.FREE))
                    points.add(testPos);
            }
        }
        return points;
    }
}
