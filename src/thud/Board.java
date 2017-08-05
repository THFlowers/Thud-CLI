package thud;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Thai Flowers on 5/24/2017.
 *
 * Represents the game board, keeps track of current game pieces and positions, allows modification of board
 * Also holds rule and game state agnostic board/piece queries such as is there a clear path between two pieces
 */
public class Board {

	private BoardStates[][] board = new BoardStates[15][15];
	private List<BoardPoint> dwarfs = new LinkedList<>();
	private List<BoardPoint> trolls = new LinkedList<>();

	public Board() {
		// set board to empty board (no pieces)
		initializeBoard();
	}

	public Board(Board other) {
		initializeBoard();
		for (int i=0; i<15; i++)
			this.board[i] = Arrays.copyOf(other.board[i], 15);
		this.dwarfs = new LinkedList<>(other.dwarfs);
		this.trolls = new LinkedList<>(other.trolls);
	}

	public List<BoardPoint> getTrolls() { return trolls; }
	public int getNumTrolls() {
		return trolls.size();
	}
	public void addTroll(BoardPoint pos) {
		trolls.add(pos);
		setAtPosition(pos, BoardStates.TROLL);
	}

	public List<BoardPoint> getDwarfs() { return dwarfs; }
	public int getNumDwarfs() {
		return dwarfs.size();
	}
	public void addDwarf(BoardPoint pos) {
		dwarfs.add(pos);
		setAtPosition(pos, BoardStates.DWARF);
	}

	// Set board to empty board, that is set valid and forbidden cells only
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

	public BoardStates getAtPosition(int x, int y) {
		return getAtPosition(new BoardPoint(x,y));
	}
	public BoardStates getAtPosition(BoardPoint pos) {
		return board[pos.row][pos.col];
	}

	void setAtPosition(int x, int y, BoardStates state) {
		setAtPosition(new BoardPoint(x,y), state);
	}
	void setAtPosition(BoardPoint pos, BoardStates state) {
		board[pos.row][pos.col] = state;
	}

	// assumes valid pos (including size and positionOnBoard)
	public boolean adjacentToAny(BoardStates state, BoardPoint pos) {

		for (int i=-1; i<=1; i++) {
			for (int j=-1; j<=1; j++) {
				// ignore pos
				if (i==0 && j==0)
					continue;
				// don't allow pos[0]+i out of bounds
				if ((pos.row +i < 0) || (pos.row +i > 14))
					continue;
				// don't allow pos[1]+j out of bounds
				if ((pos.col +j < 0) || (pos.col +j > 14))
					continue;

				BoardPoint testPos = new BoardPoint(pos.row +i, pos.col +j);

				if (getAtPosition(testPos).equals(state))
					return true;
			}
		}
		return false;
	}

	// assumes valid startPos and endPos (including size and positionOnBoard)
	public boolean positionsAreLinear(BoardPoint startPos, BoardPoint endPos) {
		return (startPos.row ==endPos.row) ||
			   (startPos.col ==endPos.col) ||
			   ( ((double)Math.abs(startPos.col-endPos.col)) / ((double)Math.abs(startPos.row-endPos.row)) == 1);
	}

	public boolean clearPathBetween(BoardPoint startPos, BoardPoint endPos) {
	    BoardPoint curPos = new BoardPoint(startPos);

	    int i, j;
	    // note that the vector components are in terms of startPos -> endPos, thus endPos - startPos is our formula
	    i = endPos.row - startPos.row;
	    j = endPos.col - startPos.col;

	    // divided each direction by the length along that direction
		// getting direction alone, allowing for incremental walking
	    if (i!=0)
	    	i = i/Math.abs(i);
		if (j!=0)
			j = j/Math.abs(j);

		do {
		    curPos.row += i;
		    curPos.col += j;
			if (!getAtPosition(curPos).equals(BoardStates.FREE))
				break;
		} while (!curPos.equals(endPos));

		return (curPos.equals(endPos));
	}

	public void movePiece(BoardPoint startPos, BoardPoint endPos) {
		BoardStates bs = getAtPosition(startPos);
		setAtPosition(startPos, BoardStates.FREE);
		setAtPosition(endPos, bs);

		switch (bs) {
			case TROLL:
				trolls.remove(startPos);
				trolls.add(endPos);
				break;
			case DWARF:
				dwarfs.remove(startPos);
				dwarfs.add(endPos);
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	public void removePiece(BoardPoint pos) {
		BoardStates bs = getAtPosition(pos);
		switch (bs) {
			case TROLL:
				trolls.remove(pos);
				break;
			case DWARF:
				dwarfs.remove(pos);
				break;
			default:
				throw new IllegalArgumentException();
		}
		setAtPosition(pos, BoardStates.FREE);
	}
}
