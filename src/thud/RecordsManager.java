package thud;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by Thai Flowers on 6/21/2017.
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
		// initialize or clear root moveLogs
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
