package thud;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Thai Flowers on 6/23/2017.
 *
 * Generates a list of all moves possible from
 */
public class PossibleMoves {

	PlayState turn;
	List<String> encodedMoves = new ArrayList<>();

	public PossibleMoves(PlayState turn) {
		this.turn = turn;
	}

	public List<String> getEncodedMoves() { return encodedMoves; }

	public void addPieceMoves(PossiblePieceMoves moves) {
		String start = moves.startPoint.toString();
		if (!turn.isRemoveTurn()) {
			for (BoardPoint end : moves.move) {
				encodedMoves.add(String.format("M %s %s", start, end.toString()));
			}
			for (BoardPoint end : moves.special) {
				char specialMove = (turn.isTurn(BoardStates.DWARF)) ? 'H' : 'S';
				encodedMoves.add(String.format("%c %s %s", specialMove, start, end.toString()));
			}
		}
		else {
			StringBuilder allEnds = new StringBuilder();
			for (BoardPoint end : moves.remove) {
				allEnds.append(end.toString());
				allEnds.append(" ");
			}
			// All or nothing options to simplify AI and approximate user interface (zero, one, or all options)
			encodedMoves.add(String.format("R %s", allEnds.toString()));
			/*
			if (!moves.mustRemove())
				encodedMoves.add("R");
			*/
		}
	}
}
