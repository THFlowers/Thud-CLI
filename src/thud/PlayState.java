package thud;

/**
 * Created by Thai Flowers on 6/9/2017.
 */
public class PlayState {
	BoardStates turn;
	boolean removeTurn;

	public PlayState() {
		turn = BoardStates.DWARF;
		removeTurn = false;
	}

	public PlayState(PlayState other) {
		this.set(other);
	}

	public PlayState(BoardStates turn, boolean removeTurn) {
		this.turn = turn;
		this.removeTurn = removeTurn;
	}

	public void set(PlayState other) {
		this.turn = other.turn;
		this.removeTurn = other.removeTurn;
	}

	public void alternateTurn() {
		if (!removeTurn) {
			turn = (turn.equals(BoardStates.DWARF)) ? BoardStates.TROLL : BoardStates.DWARF;
		}
	}

	public boolean isTurn(BoardStates side) {
		return turn.equals(side);
	}

	public BoardStates getTurn() {
		return turn;
	}

	public void setTurn(BoardStates turn) {
		this.turn = turn;
	}

	public boolean isRemoveTurn() {
		return removeTurn;
	}

	public void setRemoveTurn(boolean removeTurn) {
		this.removeTurn = removeTurn;
	}
}
