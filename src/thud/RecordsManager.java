package thud;

import java.io.*;
import java.util.*;

/**
 * Created by Thai Flowers on 6/21/2017.
 *
 * A manager class that handles save file loading and saving.
 * You must call replayRecords after loading to make game/board state match the loaded data.
 *
 * It also can be used to hold the moveLogs for each round.
 * Since player is a primary class and this is a helper class we inject player's moveLog into this class
 * This is done via the addRound call, which is supposed to be done when the log is empty (right after creation/reset of player)
 *
 * Injection in one direction or the other was necessary because player modifies the moveLog during each move
 * and we want the records stored here to update when that occurs, so we use a reference in both objects.
 *
 * As a bonus, when player is replaced or reset then this prevents garbage collection of the old moveLogs
 * meaning we keep the old without any special effort or space being used to duplicate it.
 *
 * When loading a file this class assumes a valid save file and performs no checking and throws no special exceptions.
 * Only unchecked exceptions may be thrown.  If only generated save files are used then this is reliable.
 *
 * resumeRound is set to true when loading a file and finds the round is still in progress,
 * the method call should be though of as a question.
 * It must be explicitly set to false via the setter setResumeRound as playing a move is performed by the player class.
 * Besides another application (say a log viewer/editor) may not want to set it to false.
 */
public class RecordsManager {
	List<List<String>> moveLogs = new ArrayList<>();
	boolean resumeRound = false;
	int currentRound = 0;

	public List<List<String>> getMoveLogs() {
		return moveLogs;
	}

	public boolean resumeRound() {
		return resumeRound;
	}

	public void setResumeRound(boolean resumeRound) {
		this.resumeRound = resumeRound;
	}

	public void addRound(Player player) {
		moveLogs.add(player.getMoveLog());
		currentRound++;
	}

	public int getCurrentRound() {
		return currentRound;
	}

	// file format is round1_moves empty_line round2_moves
	public void saveFile(String fileName) throws IOException {
		try (BufferedWriter out = new BufferedWriter(new FileWriter(fileName))) {
			if (currentRound < 1)
				return;
			for (String move : moveLogs.get(0)) {
				out.write(move);
				out.newLine();
			}
			if (currentRound < 2)
				return;
			out.newLine();
			for (String move : moveLogs.get(1)) {
				out.write(move);
				out.newLine();
			}
			if (currentRound < 3)
				return;
			out.newLine();
		}
	}

	public void loadFile(String fileName) throws IOException {
		// initialize or clear moveLogs
		moveLogs = new ArrayList<>();

		List<String> roundMoveLog;

		boolean hitMidBlank = false;
		boolean hitEndBlank = false;
		try (Scanner input = new Scanner(new FileReader(fileName))) {

			roundMoveLog = new ArrayList<>();
			currentRound = 0;

			while (input.hasNextLine()) {
				String currentLine = input.nextLine();

				// if any input, then round is 1 not 0, if statement to prevent multi-counting
				if (currentRound==0)
					currentRound=1;

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

		if (!hitMidBlank) {
			resumeRound = true;
		}
		else if (roundMoveLog.isEmpty()) {
			currentRound=2;
			resumeRound = false;
		}
		else {
			resumeRound = !hitEndBlank;
			if (!hitEndBlank)
				currentRound=2;
			else
				currentRound=3;
		}
	}

	public void replayRecords(Player player, PlayState turn) {

		// stopped in middle of first round
		if (resumeRound && currentRound == 1) {
			turn.set(player.replayMoveLog(moveLogs.get(0)));
		}
		// stopped at beginning of second round
		else if (!resumeRound && currentRound == 2) {
			player.replayMoveLog(moveLogs.get(0));
			turn.setTurn(BoardStates.DWARF);
		}
		// stopped in middle of second round
		else if (resumeRound && currentRound == 2) {
			player.replayMoveLog(moveLogs.get(0));
			turn.set(player.replayMoveLog(moveLogs.get(1)));
		}
		// full game recovered
		else if (!resumeRound && currentRound == 3) {
			player.replayMoveLog(moveLogs.get(0));
			player.calculateScores(1);
			player.replayMoveLog(moveLogs.get(1));
			player.calculateScores(2);
		}
	}
}
