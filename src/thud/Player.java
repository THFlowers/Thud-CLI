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
		this.moveLog = new ArrayList<>(other.moveLog);
	}

	// set board to initial game board (all pieces in default position)
	// returns playState for initial game state
	public PlayState initializeGame() {
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

		return new PlayState();
	}

	// replay the moves for a single round
	public PlayState replayMoveLog(List<String> moveLog) {
		board.initializeBoard();
		PlayState turn = initializeGame();
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

	public void calculateScores(int round) {
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

	// Assumes already checked for troll turn
	// Used to be a part of removePlay and was cleaner + more efficient,
	// but this test needed to be used in removePlay, main (used to use hack), and gui
	//
	// Also as implied in a comment in main, this makes more sense to be here than playState
	// as this is dependent on game rules, whereas PlayState should just hold state data
	public boolean mustRemove() {
		if (moveLog.size()==0)
			return false;

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
				mustCapture = false;
		}
		return mustCapture;
	}

	// Troll only special play, assumes this is already checked (as in play() above)
	private void removePlay(BoardPoint[] removePositions) {
		// retrieve anchorPos (old endPos)
		String oldMove = moveLog.get(moveLog.size()-1);
		String[] oldOrder = oldMove.split(" ");

		char oldCommand = oldOrder[0].charAt(0);
		if (!(oldCommand=='M' || oldCommand=='R' || oldCommand=='S'))
			throw new IllegalArgumentException("Previous move doesn't allow captures!");

		boolean mustCapture = mustRemove();

		BoardPoint anchorPos = new BoardPoint(oldOrder[2]);

		if (mustCapture && (removePositions.length == 0))
			throw new IllegalArgumentException("You must capture at least one dwarf");

		for (BoardPoint pos : removePositions) {
			if (!board.getAtPosition(pos).equals(BoardStates.DWARF))
				throw new IllegalArgumentException("Not a dwarf");
			if (abs(anchorPos.row - pos.row) > 1 || abs(anchorPos.col - pos.col) > 1)
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

				if (abs(startPos.row -endPos.row) > 1 || abs(startPos.col -endPos.col) > 1)
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
		if (startPos.row ==endPos.row)
			jStep = (startPos.col <endPos.col) ? 1 : -1;
		else if (startPos.col ==endPos.col)
			iStep = (startPos.row <endPos.row) ? 1 : -1;
		else if (Math.abs((startPos.col -endPos.col)/(startPos.row -endPos.row)) == 1) {
			iStep = (startPos.row <endPos.row) ? 1 : -1;
			jStep = (startPos.col <endPos.col) ? 1 : -1;
		}
		else return false;

		int numInLine = 0;
		do {
			curPos.row -= iStep;
			curPos.col -= jStep;
			numInLine++;
		} while(BoardPoint.isOnBoard(curPos.row, curPos.col) && board.getAtPosition(curPos).equals(turn));

		if ((numInLine == 1) && turn.equals(BoardStates.TROLL))
			throw new IllegalArgumentException("Shove must be at least 2 trolls");

		curPos = new BoardPoint(startPos);
		for (int c = 0; c < numInLine; c++) {
			curPos.row += iStep;
			curPos.col += jStep;
			if (curPos.equals(endPos))
				return true;
			if (!BoardPoint.isOnBoard(curPos.row, curPos.col) || !board.getAtPosition(curPos).equals(BoardStates.FREE))
				return false;
		}
		return false;
	}


	public PossiblePieceMoves getPossiblePieceMoves(PlayState turn, BoardPoint pos) {
		BoardStates side = board.getAtPosition(pos);

		switch (side) {
			case TROLL:
				return getPossibleTrollPieceMoves(turn, pos);
			case DWARF:
				return getPossibleDwarfPieceMoves(turn, pos);
			default:
				return new PossiblePieceMoves(pos, null, null, null, false);
		}
	}

	public PossiblePieceMoves getPossibleTrollPieceMoves(PlayState turn, BoardPoint pos) {
		ArrayList<BoardPoint> move = new ArrayList<>();
		ArrayList<BoardPoint> shove = new ArrayList<>();

		if (turn.isRemoveTurn()) {
			String oldMove = moveLog.get(moveLog.size() - 1);
			String[] oldOrder = oldMove.split(" ");
			BoardPoint oldPos = new BoardPoint(oldOrder[2]);
			if (!pos.equals(oldPos))
				return new PossiblePieceMoves(pos,null, null, null, false);
			else {
				List<BoardPoint> positions = kingMovesMatching(pos, BoardStates.DWARF);
				return new PossiblePieceMoves(pos, null, null, positions, mustRemove());
			}
		}

		// movement
		move.addAll(kingMoves(pos));

		// shove
		for (int i=-1; i<=1; i++) {
			for (int j=-1; j <= 1; j++) {
				if (i==0 && j==0)
					continue;

				BoardPoint temp = new BoardPoint(pos);
				int numTrolls;
				for (numTrolls=1; board.getAtPosition(temp).equals(BoardStates.TROLL); numTrolls++){

					temp.row -= i;
					temp.col -= j;
					if (!BoardPoint.isOnBoard(temp.row, temp.col))
						break;
				}

				if (numTrolls < 2)
					continue;

				temp = new BoardPoint(pos);
				if (!BoardPoint.isOnBoard(temp.row+2*i, temp.col+2*j))
					continue;

				temp.row += i;
				temp.col += j;
				for (int steps=1; board.getAtPosition(temp).equals(BoardStates.FREE) && steps<numTrolls; steps++) {
					if (steps>=2 && board.adjacentToAny(BoardStates.DWARF, temp))
						shove.add(new BoardPoint(temp));

					temp.row += i;
					temp.col += j;
					if (!BoardPoint.isOnBoard(temp.row,temp.col))
						break;
				}
			}
		}

		return new PossiblePieceMoves(pos, move, shove, null, false);
	}

	public PossiblePieceMoves getPossibleDwarfPieceMoves(PlayState turn, BoardPoint pos) {
		ArrayList<BoardPoint> move = new ArrayList<>();
		ArrayList<BoardPoint> hurl = new ArrayList<>();

		// movement
		for (int i=-1; i<=1; i++) {
			for (int j=-1; j <= 1; j++) {
				if (i==0 && j==0)
					continue;

				BoardPoint temp = new BoardPoint(pos);
				temp.row += i;
				temp.col += j;
				if (!BoardPoint.isOnBoard(temp.row,temp.col))
					continue;

				while (board.getAtPosition(temp).equals(BoardStates.FREE)) {
					move.add(new BoardPoint(temp));
					temp.row += i;
					temp.col += j;
					if (!BoardPoint.isOnBoard(temp.row,temp.col))
						break;
				}
			}
		}

		// hurl
		for (BoardPoint trollPos : board.getTrolls()) {
			if (distanceAttackCheck(BoardStates.DWARF, pos, trollPos))
				hurl.add(trollPos);
		}

		return new PossiblePieceMoves(pos, move, hurl, null, false);
	}

	private ArrayList<BoardPoint> kingMoves(BoardPoint pos) {
		return kingMovesMatching(pos, BoardStates.FREE);
	}
	private ArrayList<BoardPoint> kingMovesMatching(BoardPoint pos, BoardStates match) {
		ArrayList<BoardPoint> points = new ArrayList<>();
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				// ignore pos
				if (i == 0 && j == 0)
					continue;
				// don't allow pos[0]+i out of bounds
				if ((pos.row + i < 0) || (pos.row + i > 14))
					continue;
				// don't allow pos[1]+j out of bounds
				if ((pos.col + j < 0) || (pos.col + j > 14))
					continue;

				BoardPoint testPos = new BoardPoint(pos.row + i, pos.col + j);

				if (board.getAtPosition(testPos).equals(match))
					points.add(testPos);
			}
		}
		return points;
	}

	public List<String> getPossibleMoves(PlayState turn) {
		PossibleMoves allMoves = new PossibleMoves(turn);

		if (turn.isRemoveTurn()) {
			String oldMove = moveLog.get(moveLog.size()-1);
			String[] oldOrder = oldMove.split(" ");
			BoardPoint oldPos = new BoardPoint(oldOrder[2]);
			List<BoardPoint> positions = kingMovesMatching(oldPos, BoardStates.DWARF);
			PossiblePieceMoves removes = new PossiblePieceMoves(oldPos, null, null, positions, true);
			allMoves.addPieceMoves(removes);
			return allMoves.getEncodedMoves();
		}

		List<BoardPoint> pieces = (turn.isTurn(BoardStates.DWARF)) ? board.getDwarfs() : board.getTrolls();
		for (BoardPoint piece : pieces) {
			allMoves.addPieceMoves(getPossiblePieceMoves(turn, piece));
		}
		return allMoves.getEncodedMoves();
	}
}
