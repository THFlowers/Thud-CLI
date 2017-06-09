package thud;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Main {

	// TODO : Refactor play and replay return values to be new playState class which holds BoardStates turn and Boolean multiTurn
	// current engine disallows 2nd non Remove play for Trolls, but only if it gets the correct multiTurn value, which the replay mechanism fails to return

	private static Board board = new Board();
	private static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	private static List<List<String>> moveLogs = new ArrayList<>();
	private static int[] playerScores = new int[2]; // use mod 2 arithmetic to access index while scoring
	private static BoardStates turn;

    public static void main(String[] args) throws IOException {

    	boolean resumeRound = false;
    	int startRound;
    	if (args.length == 1) {
    	    System.out.println("Loading save file, if the game is complete it will be re-scored, if it is incomplete it will resume");
    	    try {
				resumeRound = loadLogsFromFile(args[0]);
			}
			catch (FileNotFoundException ex) {
    	    	System.out.printf("File %s not found!", args[0]);
    	    	System.exit(0);
			}

    	    // stopped in middle of first round
    	    if (resumeRound && moveLogs.size() == 1) {
				turn = board.replayMoveLog(moveLogs.get(0));
			}
			// stopped at beginning of second round
            else if (!resumeRound && moveLogs.size() == 1) {
				board.replayMoveLog(moveLogs.get(0));
				turn = BoardStates.DWARF;
			}
			// stopped in middle of second round
			else if (resumeRound && moveLogs.size() == 2) {
				board.replayMoveLog(moveLogs.get(0));
				turn = board.replayMoveLog(moveLogs.get(1));
			}
			// full game recovered
			else if (!resumeRound && moveLogs.size() == 2) {
    	        System.out.println("Full game recovered\n");
				board.replayMoveLog(moveLogs.get(0));
				calculateScores(1);
				board.replayMoveLog(moveLogs.get(1));
				calculateScores(2);
			}
			else {
				System.out.println("Shut er down Johnny, she's a pumpin mud!");
				System.out.println("moveLogs.size() " + moveLogs.size());
				System.out.println("resumeRound " + resumeRound);
				System.exit(-999);
			}

			startRound = moveLogs.size()-1;
		}
		else {
			startRound = 0;
		}

		for (int round=startRound; round < 2; round++) {

    	    // don't initialize a new round if we loaded from a file
    		if (!resumeRound) {
				board.initializeGame();
				//moveLogs.add(board.getMoveLog());
				turn = BoardStates.DWARF;

				System.out.printf("Starting round %d\n", round+1);
                System.out.println("Dwarfs move first");
			}
			else {
				System.out.printf("Resuming round %d\n", round + 1);
				System.out.printf("Current turn: %s\n",
					(turn == BoardStates.DWARF) ? "Dwarfs" : "Trolls");
			}
			resumeRound = false;

			boolean playing = true;
			while (playing) {
				System.out.print(board);

				turn = playNext(turn);
				switch (turn) {
					case FORBIDDEN:
						// don't allow saving if first round and empty moveLog
					    if ( !(moveLogs.size() == 0 && round == 0) ) {
							savePrompt();
						}
						System.exit(0);
						break;
					case STONE:
                        if ( (moveLogs.size() == 0 && round == 0) ) {
                           System.out.println("Nothing to save!");
                        }
                        else {
							System.out.print("Filename: ");
							String input = in.readLine();
							saveFile(input);
						}
						break;
					case FREE:
						playing = false;
						break;
				}
			}

			// store scores for the round
            calculateScores(round);
		}

		// determine final score and winner
		System.out.println();
		for (int i=0; i<2; i++) {
			System.out.printf("Player %d: %d\n", i+1, playerScores[i]);
		}

		if (playerScores[0] != playerScores[1]) {
			System.out.printf("\nPlayer %d Wins\n", (playerScores[0] > playerScores[1]) ? 1 : 2);
			System.out.printf("By a margin of %d points\n", Math.abs(playerScores[0] - playerScores[1]));
		}
		else
			System.out.println("Game was a draw");

		savePrompt();
	}

	// the boolean refers to whether we are in the middle of a round or not
	private static boolean loadLogsFromFile(String fileName) throws IOException {
        // initialize or clear current moveLogs
        moveLogs = new ArrayList<>();

		boolean middleOfRound = false;
		List<String> roundMoveLog;

    	try (BufferedReader input = new BufferedReader(new FileReader(fileName))) {

    		for (int round=0; round<2; round++) {
				roundMoveLog = new ArrayList<>();
				String currentLine;

				// do-while b/c we need to check at least one command in current round
                // if first round ended due to null, this will be null and we will return the proper middleOfRound status
				currentLine = input.readLine();
				if (currentLine==null) {
					break;
				}
				do {
					// we have found a blank line, move on to next round
					if (currentLine.length()==0) {
						middleOfRound = false;
						break;
					}
					else {
						roundMoveLog.add(currentLine);
						middleOfRound = true;
					}
				} while ((currentLine = input.readLine()) != null);

				if (roundMoveLog.size() != 0)
					moveLogs.add(roundMoveLog);
			}
		}
		return middleOfRound;
	}

	private static void calculateScores(int round) {
		playerScores[round % 2] = board.getNumDwarfs() * 10;
		playerScores[(round+1) % 2] = board.getNumTrolls() * 40;
	}

	private static BoardStates playNext(BoardStates turn) throws IOException {
		boolean multiTurn = false;
		boolean validMove = false;
		while (!validMove) {
			if (board.getNumDwarfs() == 0 || board.getNumTrolls() == 0)
				return BoardStates.FREE;

			System.out.print((turn == BoardStates.DWARF) ? "Dwarfs: " : "Trolls: ");
			String move = in.readLine();

			// 'H'url by troll must be followed by an 'R'emove of 1 dwarf or more
            // don't allow interface commands to run in this case
            // (only Troll can 'H' so this test is safe)
			if (board.getLastMove().charAt(0) != 'H') {
				if (move.equalsIgnoreCase("exit"))
					return BoardStates.FORBIDDEN;
				if (move.equalsIgnoreCase("save")) {
					return BoardStates.STONE;
				}
				if (move.equalsIgnoreCase("forfeit")) {
					char lastCmd = board.getLastMove().charAt(0);
					if (lastCmd == 'S') {
						throw new IllegalArgumentException("Can't forfeit mid shove.");
                    }
                    // check if last move allows implicit remove of nothing, if so add it as explicit command and forfeit
					if (lastCmd == 'M' && turn == BoardStates.TROLL) {
						int[] oldEndPos = board.notationToPosition(board.getLastMove().substring(5));
						if (board.adjacentToAny(BoardStates.DWARF, oldEndPos)) {
							board.play(turn, "R", true);
						}
					}
                    return BoardStates.FREE;
				}
			}

			try {
				multiTurn = board.play(turn, move, multiTurn);
				validMove = true;
			} catch (IllegalArgumentException ex) {
				System.out.println(ex.getMessage());
			}
		}
		if (!multiTurn)
			return (turn == BoardStates.DWARF) ? BoardStates.TROLL : BoardStates.DWARF;
		else
			return turn;
	}

	// file format is round1_moves empty_line round2_moves
	private static void saveFile(String fileName) throws IOException {
		try (BufferedWriter out = new BufferedWriter(new FileWriter(fileName))) {
			for (List<String> log : moveLogs) {
				for (String move : log) {
					out.write(move);
					out.newLine();
				}
			}
			// empty line between rounds
			out.newLine();
		}
	}

	private static void savePrompt() throws IOException {
		boolean validInput = false;
		while (!validInput) {
			System.out.print("Save? [Y/N] ");
			String input = in.readLine();
			if (input.equalsIgnoreCase("Y")) {
				System.out.print("Filename: ");
				input = in.readLine();
				saveFile(input);
			} else if (input.equalsIgnoreCase("N")) {
				validInput = true;
			}
		}
	}
}
