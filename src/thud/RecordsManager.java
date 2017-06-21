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

    public List<List<String>> getMoveLogs() {
        return moveLogs;
    }

    public boolean resumeRound() {
        return resumeRound;
    }

    // file format is round1_moves empty_line round2_moves
    public void saveFile(String fileName) throws IOException {
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

    public void loadFile(String fileName) throws IOException {
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
            resumeRound = true;
        else if (roundMoveLog.isEmpty())
            resumeRound = false;
        else
            resumeRound = !hitEndBlank;
    }

    public int replayRecords(Player player, PlayState turn) {
        int startRound = 0;
        // stopped in middle of first round
        if (resumeRound && moveLogs.size() == 1) {
            turn.set(player.replayMoveLog(moveLogs.get(0)));

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
            turn.set(player.replayMoveLog(moveLogs.get(1)));

            startRound = 2;
        }
        // full game recovered
        else if (!resumeRound && moveLogs.size() == 2) {
            player.replayMoveLog(moveLogs.get(0));
            player.calculateScores(1);
            player.replayMoveLog(moveLogs.get(1));
            player.calculateScores(2);

            startRound = 3;
        }
        return startRound;
    }
}
