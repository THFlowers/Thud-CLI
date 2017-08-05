package thud;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Thai Flowers on 6/23/2017.
 *
 * not public, use externally but do not make/instantiate, as this is quick-and-dirty (but not over-engineered)
 *
 * A helper data class for the possibleMoves class and (transitively) the MonteCarloPlay ai class,
 * this merely stores a starting position and lists of endpoints separated by move type.
 *
 * PossibleMoves takes this input and uses the move type separation, startpoint, and endPoint to generate a flat list
 * of strings storing all possible moves for this piece (minus a few remove variations).  This is then aggregated with
 * the possiblePieceMoves for all the current player's other pieces into one massive list.
 *
 * It is incredibly quick and dirty and is here to simplify code so that only a small number of
 * arguments are passed to aforementioned classes.
 *
 * Has no move generation code of it's own, purely a data class.
 * For that look in Player (toward the bottom).
 */
public class PossiblePieceMoves {
	BoardPoint startPoint;
	List<BoardPoint> move;
	List<BoardPoint> special;
	// should only be used by Troll
	List<BoardPoint> remove;
	Boolean mustRemove;

	PossiblePieceMoves(BoardPoint startPoint, List<BoardPoint> move, List<BoardPoint> special, List<BoardPoint> remove, Boolean mustRemove) {
		this.startPoint = startPoint;
		this.move = move;
		this.special = special;
		// list of all possible remove positions, done all or nothing for simplicity
		this.remove = remove;
		this.mustRemove = mustRemove;

		if (this.move == null)
			this.move = new ArrayList<>();
		if (this.special == null)
			this.special = new ArrayList<>();
		if (this.remove == null)
			this.remove = new ArrayList<>();
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
