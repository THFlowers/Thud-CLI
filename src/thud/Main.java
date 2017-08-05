package thud;

import java.io.*;

public class Main {

	// encodes players actions above valid game moves (those are NORMAL actions)
	// each input loop produces one of these values, allowing main loop to handle plays and special actions
	private enum SpecialActions {
		NORMAL, SAVE, QUIT, FORFEIT
	}

	private static Player player = new Player(new Board());
	private static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	private static PlayState turn = new PlayState();
	private static RecordsManager recordsManager = new RecordsManager();
	private static SpecialActions specialAction = SpecialActions.NORMAL;

	public static void main(String[] args) throws IOException {

		// NOTE: in current form only ai or save file can be loaded NOT both!

		// rounds are 1 indexed for clarity
		int startRound = 1;
		boolean player2ai = false;  // set to true if second player is played by ai
		MonteCarloPlay ai = null;

        if (args.length > 1) {
        	printUsageAndExit(1);
		}
        else if (args.length == 1) {
			if (args[0].charAt(0) == '-') {
				if (args[0].equals("-a")) {
					player2ai = true;
				} else {
					printUsageAndExit(2);
				}

			} else {
				System.out.println("Loading save file, if the game is complete it will be re-scored, if it is incomplete it will resume");
				try {
					recordsManager.loadFile(args[0]);
				} catch (FileNotFoundException ex) {
					System.out.printf("File %s not found!", args[0]);
					System.exit(3);
				} catch (IOException ex) {
					System.out.printf("IO error loading %s!", args[0]);
					System.exit(3);
				}

				recordsManager.replayRecords(player, turn);
				startRound = recordsManager.getCurrentRound();
				if (startRound <= 0) {
					System.out.println("Shut er down Johnny, she's a pumpin' mud!");
					System.out.println("moveLogs.size() " + recordsManager.getMoveLogs().size());
					System.out.println("resumeRound " + recordsManager.resumeRound);
					System.exit(-999);
				} else if (startRound == 3) {
					System.out.println("Full game recovered\n");
				}
			}
		}

		for (int round=startRound; round <= 2; round++) {
			// don't initialize a new round if we loaded from a file in middle of a round
			// if we didn't load from a file resumeRound() defaults to false
			if (!recordsManager.resumeRound()) {
				player = new Player(new Board());
				turn = player.initializeGame();
				recordsManager.addRound(player);

				// With human players we don't have to alternate starting round player side, they can figure it out
				// thus we can use the default game initializer (initializeGame above) and default message
				System.out.printf("Starting round %d\n", round);
				System.out.println("Dwarfs move first");

				// However with ai, we do have to alternate starting round player side, by giving the ai a first move
				// before the main loop starts when it gets first move, then it plays after human in the main loop
				if (player2ai) {
					ai = new MonteCarloPlay((round==1) ? BoardStates.TROLL : BoardStates.DWARF);

					// if second round, then do an initial turn for the ai
					if (round == 2) {
						player.play(turn, ai.selectPlay()); // first move never has remove, so don't worry handling it
						System.out.println("\nAI plays: " + player.getLastMove());
					}
				}
			}
			else {
				System.out.printf("Resuming round %d\n", round);
				System.out.printf("Current turn: %s\n",
					(turn.isTurn(BoardStates.DWARF)) ? "Dwarfs" : "Trolls");
				recordsManager.setResumeRound(false);
			}

			boolean playing = true;
			while (playing) { // while playing the current round

				System.out.print(player.getBoard());
				switch (specialAction = playNext(turn)) {
					case NORMAL:
						break;
					case QUIT:
						// don't allow saving if first round and empty moveLog (for this round)
						if ( !(recordsManager.getMoveLogs().size() == 0 && round == 0) ) {
							savePrompt();
						}
						System.exit(0);
						break;
					case SAVE:
						if ( (recordsManager.getMoveLogs().size() == 0 && round == 0) ) {
						   System.out.println("Nothing to save!");
						}
						else {
							System.out.print("Filename: ");
							String input = in.readLine();
							recordsManager.saveFile(input);
						}
						break;
					case FORFEIT:
						playing = false;
						break;
				}

				if (playing && specialAction==SpecialActions.NORMAL && player2ai) {
					ai.opponentPlay(player.getLastMove());

					// skip ai play if human has remove turn next
					if (!turn.isRemoveTurn()) {
						player.play(turn, ai.selectPlay());
						System.out.print("\nAI plays: " + player.getLastMove());

						// if ai move has remove turn then handle it now, so that it is player turn on next iteration
						if (turn.isRemoveTurn()) {
							player.play(turn, ai.selectPlay());
							System.out.print("\nAI plays: " + player.getLastMove());
						}
						System.out.println();
					}
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

	private static void printUsageAndExit(int error) {
		System.err.println("Proper Usage: thud -a | file.txt where -a enables ai opponent or restore human v human file");
		System.exit(error);
	}

	private static SpecialActions playNext(PlayState turn) throws IOException {
		Board board = player.getBoard();
		boolean validMove = false;
		while (!validMove) {

			// force a forfeit move if further play is pointless
			if (board.getNumDwarfs() == 0 || board.getNumTrolls() == 0)
				return SpecialActions.FORFEIT;

			System.out.println();
			System.out.print((turn.isTurn(BoardStates.DWARF)) ? "Dwarfs: " : "Trolls: ");
			String move = in.readLine();

			// don't allow interface commands to run if a mandatory remove is in progress (could forfeit)
			// no need to check for troll as only they can remove (at least under default rules),
			// if using other rules then this should still be valid
			if (!player.mustRemove()) {
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

	private static void savePrompt() throws IOException {
		boolean validInput = false;
		while (!validInput) {
			System.out.print("Save? [Y/N] ");
			String input = in.readLine();
			if (input.equalsIgnoreCase("Y")) {
				validInput = true;
				System.out.print("Filename: ");
				input = in.readLine();
				recordsManager.saveFile(input);
			} else if (input.equalsIgnoreCase("N")) {
				validInput = true;
			}
		}
	}
}
