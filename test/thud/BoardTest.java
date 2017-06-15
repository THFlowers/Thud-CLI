package thud;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

class moveTestPair {
    public String move;
    public boolean errorExpected;
    moveTestPair(String move, boolean errorExpected) {
        this.move = move;
        this.errorExpected = errorExpected;
    }
}

/**
 * Created by Thai Flowers on 5/26/2017.
 */
class BoardTest {
    // plays part of a game, trying to hit all possible invalid move paths
    @org.junit.jupiter.api.Test
    void errorFilledPlay() {

        List<moveTestPair> errorGame = Arrays.asList(
                // utterly invalid
                new moveTestPair("Move B7 E7", true),
                new moveTestPair("M B7 E7 G7", true), // off edge of board / incorrect notation
                new moveTestPair("M B7 B0", true), // off edge of board / incorrect notation
                new moveTestPair("M Z7 Z0", true), // off edge of board / incorrect notation
                // general illegal movement
                new moveTestPair("M B7 B16", true), // off edge of board / incorrect notation
                new moveTestPair("M B7 B7", true), // in place move
                new moveTestPair("M B7 E7", true), // not our piece
                new moveTestPair("M A7 G7", true), // on another piece
                new moveTestPair("M A6 A5", true), // onto forbidden space
                // dwarf illegal movement
                new moveTestPair("S A7 B8", true), // not a valid dwarf command
                new moveTestPair("M A7 D8", true), // not linear
                // first legal dwarf move
                new moveTestPair("M A7 E7", false),
                // troll illegal movement
                new moveTestPair("H G7 G8", true), // not a valid troll command
                new moveTestPair("S G7 G8", true), // shove must be more than one troll
                new moveTestPair("M E7 B7", true), // not a troll
                new moveTestPair("M G7 G8", true), // not free
                new moveTestPair("M G7 G4", true), // more than 1 space away vertical
                new moveTestPair("M G7 I5", true), // more than 1 space away diagonally
                new moveTestPair("M I7 K7", true), // more than 1 space away horizontal
                // legal troll move
                new moveTestPair("M G7 F7", false),
                // bad remove
                new moveTestPair("R G8", true), // not a dwarf
                new moveTestPair("R F8 F7", true), // no pieces
                new moveTestPair("R E9", true), // not adjacent and not dwarf
                new moveTestPair("M E7", true), // not proper command
                new moveTestPair("M F7 G7", true), // second move must be remove
                // legal no remove
                new moveTestPair("R", false),
                // hurl of one
                new moveTestPair("H E7 F7", false),
                // test shove
                new moveTestPair("S H7 F7", true), // can't shove onto target
                new moveTestPair("S H7 G7", false), // shove of one allowed, b/c row is length 2 > 1
                // test remove with shove
                new moveTestPair("R", true), // must remove at-least one
                new moveTestPair("M H9 I10", true), // second move must be remove
                new moveTestPair("R F7", false),
                // position dwarfs
                new moveTestPair("M B11 D11", false),
                new moveTestPair("M H9 I10", false),
                new moveTestPair("M A6 E10", false),
                new moveTestPair("M G8 H7",  false),
                new moveTestPair("M K14 J14", false),
                new moveTestPair("M G9 H10",  false),
                // test longer (diagonal) hurl
                new moveTestPair("H D11 H7", true), // not end of line
                new moveTestPair("H D11 G8", true), // not on a troll
                new moveTestPair("H E10 H7", false),
                // test longer shove
                new moveTestPair("S I10 J13", true), // not a straight line
                new moveTestPair("S I10 I13", false)
        );

        Board board = new Board();
        Player player = new Player(board);
        player.initializeGame();

        PlayState turn = new PlayState();

        for (moveTestPair move : errorGame) {
            try {
                player.play(turn, move.move);
                if (move.errorExpected) {
                    System.out.println(board.toString());
                    fail("Illegal move allowed: " + move.move);
                }
            }
            catch (IllegalArgumentException ex) {
                if (!move.errorExpected) {
                   System.out.println(board.toString());
                   fail("Legal move forbidden: " + move.move + ex.getLocalizedMessage());
                }
            }
        }

        System.out.println(board.toString());
    }

    // tests the load/replay constructor
    @org.junit.jupiter.api.Test
    void errorFreeReplay() {

        List<String> moves = Arrays.asList(
                "M A7 E7", "M G7 F7", "R",
                "H E7 F7", "S H7 G7", "R F7",
                "M B11 D11", "M H9 I10", "M A6 E10",
                "M G8 H7", "M K14 J14", "M G9 H10",
                "H E10 H7", "S I10 I13"
        );

    Board board = new Board();
    Player player = new Player(board);
    player.replayMoveLog(moves);
    System.out.println(board.toString());

    }
}