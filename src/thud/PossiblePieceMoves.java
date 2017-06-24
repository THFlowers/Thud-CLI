package thud;

import java.util.List;

/**
 * Created by Thai Flowers on 6/23/2017.
 */
public class PossiblePieceMoves {
    BoardPoint startPoint;
    List<BoardPoint> move;
    List<BoardPoint> special;
    // should only be used by Troll
    List<BoardPoint> remove;
    Boolean mustRemove;

    // not public, use externally but not make, as this is quick-and-dirty (but not over-engineered)
    PossiblePieceMoves(BoardPoint startPoint, List<BoardPoint> move, List<BoardPoint> special, List<BoardPoint> remove, Boolean mustRemove) {
        this.startPoint = startPoint;
        this.move = move;
        this.special = special;
        // list of all possible remove positions, done all or nothing for simplicity
        this.remove = remove;
        this.mustRemove = mustRemove;
    }

    // public should only use accessors
    public BoardPoint getStartPoint() {
        return startPoint;
    }

    public List<BoardPoint> getMove() {
        return move;
    }

    public List<BoardPoint> getSpecial() {
        return special;
    }

    public List<BoardPoint> getRemove() {
        return remove;
    }

    public Boolean mustRemove() {
        return mustRemove;
    }
}
