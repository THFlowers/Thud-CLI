package thud;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

	private enum SpecialActions {
	    NORMAL, SAVE, QUIT, FORFEIT
	}

	private static Player player = new Player(new Board());
	private static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	private static List<List<String>> moveLogs = new ArrayList<>();
	private static PlayState turn = new PlayState();
	private static SpecialActions specialAction;

    public static void main(String[] args) throws IOException {

    	boolean resumeRound = false;
    	int startRound;
    	if (args.length == 1) {
    	    System.out.println("Loading save file, if the game is complete it will be re-scored, if it is incomplete it will resume");
    	    try {
				resumeRound = loadFile(args[0]);
			}
			catch (FileNotFoundException ex) {
    	    	System.out.printf("File %s not found!", args[0]);
    	    	System.exit(0);
			}

			startRound = -1;

    	    // stopped in middle of first round
    	    if (resumeRound && moveLogs.size() == 1) {
				turn = player.replayMoveLog(moveLogs.get(0));

				startRound = 1;
			}
			// stopped at beginning of second round
            else if (!resumeRound && moveLogs.size() == 1) {
				player.replayMoveLog(moveLogs.get(0));
				turn.setTurn(BoardStates.DWARF);

				startRound = 2;
			}
			// stopped in middle of second round
			else if (resumeRound && moveLogs.size() == 2) {
				player.replayMoveLog(moveLogs.get(0));
				turn = player.replayMoveLog(moveLogs.get(1));

				startRound = 2;
			}
			// full game recovered
			else if (!resumeRound && moveLogs.size() == 2) {
    	        System.out.println("Full game recovered\n");
				player.replayMoveLog(moveLogs.get(0));
				player.calculateScores(1);
				player.replayMoveLog(moveLogs.get(1));
				player.calculateScores(2);

				startRound = 3;
			}
			else {
				System.out.println("Shut er down Johnny, she's a pumpin mud!");
				System.out.println("moveLogs.size() " + moveLogs.size());
				System.out.println("resumeRound " + resumeRound);
				System.exit(-999);
			}
		}
		else {
			startRound = 1;
		}

		for (int round=startRound; round <= 2; round++) {

    	    // don't initialize a new round if we loaded from a file
    		if (!resumeRound) {
				player.initializeGame();

				System.out.printf("Starting round %d\n", round);
                System.out.println("Dwarfs move first");
			}
			else {
				System.out.printf("Resuming round %d\n", round);
				System.out.printf("Current turn: %s\n",
					(turn.isTurn(BoardStates.DWARF)) ? "Dwarfs" : "Trolls");
			}
			resumeRound = false;

			boolean playing = true;
			while (playing) {
				System.out.print(player.getBoard());

				specialAction = playNext(turn);
				switch (specialAction) {
					case NORMAL:
						break;
					case QUIT:
						// don't allow saving if first round and empty moveLog
					    if ( !(moveLogs.size() == 0 && round == 0) ) {
							savePrompt();
						}
						System.exit(0);
						break;
					case SAVE:
                        if ( (moveLogs.size() == 0 && round == 0) ) {
                           System.out.println("Nothing to save!");
                        }
                        else {
							System.out.print("Filename: ");
							String input = in.readLine();
							saveFile(input);
						}
						break;
					case FORFEIT:
						playing = false;
						break;
				}
			}

			// store scores for the round
            player.calculateScores(round);
		}

		// determine final score and winner
        int[] playerScores = player.getScores();
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

		if (startRound!=3)
            savePrompt();
	}

	// the boolean refers to whether we are in the middle of a round or not
	private static boolean loadFile(String fileName) throws IOException {
        // initialize or clear current moveLogs
        moveLogs = new ArrayList<>();

		List<String> roundMoveLog;

		boolean hitMidBlank = false;
		boolean hitEndBlank = false;
    	try (Scanner input = new Scanner(new FileReader(fileName))) {

			roundMoveLog = new ArrayList<>();

		 	while (input.hasNextLine()) {
		 	    String currentLine = input.nextLine();

		 		// if we have hit the 2nd blank but not end of input, then format is wrong
		 	    if (hitEndBlank)
		 	    	throw new IOException("File is more than 2 rounds");

		 	    // if 1st blank hit, move to next round, if 2nd mark it for purposes of determining round status
				if (currentLine.length() == 0) {
					if (!hitMidBlank) {
						hitMidBlank = true;
						moveLogs.add(roundMoveLog);
						roundMoveLog = new ArrayList<>();
					} else {
						hitEndBlank = true;
                    }
				} else {
					roundMoveLog.add(currentLine);
				}
			}

			if (!roundMoveLog.isEmpty())
				moveLogs.add(roundMoveLog);
		}

		if (!hitMidBlank)
			return true;
    	else if (roundMoveLog.isEmpty())
    		return false;
    	else
            return !hitEndBlank;
	}

	private static SpecialActions playNext(PlayState turn) throws IOException {
        Board board = player.getBoard();
		boolean validMove = false;
		while (!validMove) {
			if (board.getNumDwarfs() == 0 || board.getNumTrolls() == 0)
				return SpecialActions.FORFEIT;

			System.out.println();
			System.out.print((turn.isTurn(BoardStates.DWARF)) ? "Dwarfs: " : "Trolls: ");
			String move = in.readLine();

			// 'H'url by troll must be followed by an 'R'emove of 1 dwarf or more
            // don't allow interface commands to run in this case
            // (only Troll can 'H' so this test is safe)
			if (player.getLastMove().charAt(0) != 'H') {
				if (move.equalsIgnoreCase("exit"))
					return SpecialActions.QUIT;
				if (move.equalsIgnoreCase("save")) {
					return SpecialActions.SAVE;
				}
				if (move.equalsIgnoreCase("forfeit")) {
					char lastCmd = player.getLastMove().charAt(0);
					if (lastCmd == 'S') {
						throw new IllegalArgumentException("Can't forfeit mid shove.");
                    }
                    // check if last move allows implicit remove of nothing, if so add it as explicit command and forfeit
					if (lastCmd == 'M' && turn.isTurn(BoardStates.TROLL)) {
						BoardPoint oldEndPos = new BoardPoint(player.getLastMove().substring(5));
						if (board.adjacentToAny(BoardStates.DWARF, oldEndPos)) {
							player.play(turn, "R");
						}
					}
					return SpecialActions.FORFEIT;
				}
			}

			try {
				player.play(turn, move);
				validMove = true;
			} catch (IllegalArgumentException ex) {
				System.out.println(ex.getMessage());
			}
		}

		return SpecialActions.NORMAL;
	}

	// file format is round1_moves empty_line round2_moves
	private static void saveFile(String fileName) throws IOException {
		try (BufferedWriter out = new BufferedWriter(new FileWriter(fileName))) {
			for (List<String> log : moveLogs) {
				for (String move : log) {
					out.write(move);
					out.newLine();
				}
				out.newLine();
			}
		}
	}

	private static void savePrompt() throws IOException {
		boolean validInput = false;
		while (!validInput) {
			System.out.print("Save? [Y/N] ");
			String input = in.readLine();
			if (input.equalsIgnoreCase("Y")) {
				validInput = true;
				System.out.print("Filename: ");
				input = in.readLine();
				saveFile(input);
			} else if (input.equalsIgnoreCase("N")) {
				validInput = true;
			}
		}
	}
}
