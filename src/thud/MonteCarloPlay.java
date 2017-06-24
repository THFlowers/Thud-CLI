package thud;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by Thai Flowers on 6/16/2017.
 *
 * Based on:
 *     https://jeffbradberry.com/posts/2015/09/intro-to-monte-carlo-tree-search/
 *     https://en.wikipedia.org/wiki/Monte_Carlo_tree_search
 */
public class MonteCarloPlay {
	class MonteCarloNode {
		int wins;
		int visits;

		Player player;
		PlayState playState;
		MonteCarloNode parent;
		List<MonteCarloNode> children;
		List<String> possibleMoves;

		MonteCarloNode(String move, MonteCarloNode parent) {
			this.parent = parent;
			if (parent==null) {
				this.player = new Player(new Board());
				this.player.initializeGame();
				this.playState = new PlayState();
				this.possibleMoves = player.getPossibleMoves(this.playState);
			}
			else {
				this.player = new Player(parent.player);
				this.playState = new PlayState(parent.playState);
				this.player.play(playState, move);
				this.possibleMoves = player.getPossibleMoves(this.playState);
			}

			wins = visits = 0;
			children = null;
		}

		double score() {
				return (((double)wins) / ((double)visits)) + 2*Math.sqrt(2*Math.log(numPlayouts)/visits);
		}
	}

	Random rand = new Random();
	int numPlayouts = 0;
	MonteCarloNode root = new MonteCarloNode("", null);
	MonteCarloNode current = root;
	BoardStates side;

	MonteCarloPlay(BoardStates side){
		this.side = side;
		// since dwarfs go first, be sure to generate nodes
		// for player 1 move so we can move there for our turn
		if (this.side == BoardStates.TROLL) {
			for (int i = 0; i < 4000; i++) {
				playOut();
				if (i%300==0)
					System.out.printf("\r%d", i);
			}
			System.out.println();
		}
	}

	void opponentPlay(String move) {
		for (MonteCarloNode child : current.children)
			if (child.player.getLastMove().equals(move))
				current = child;
	}

	String selectPlay() {
		for (int i = 0; i < 1000; i++)
			playOut();

		MonteCarloNode bestChoice = null;
		for (MonteCarloNode child : current.children) {
			if (bestChoice == null)
				bestChoice = child;
			else if (child.score() > bestChoice.score())
				bestChoice = child;
		}

		current = bestChoice;
		return current.player.getLastMove();
	}

	void playOut() {
		numPlayouts++;
		MonteCarloNode temp = current;

		// Selection
		while (temp.children != null && temp.children.size() > 0 && temp.children.size() == temp.possibleMoves.size()) {
			MonteCarloNode bestChoice = temp.children.get(0);
			for (int i=1; i<temp.children.size(); i++) {
				MonteCarloNode curChild = temp.children.get(i);
				if (curChild.score() > bestChoice.score())
					bestChoice = curChild;
			}
			temp = bestChoice;
		}

		// Expansion
		// if current node is null then allot memory
		if (temp.children == null)
			temp.children = new LinkedList<>();

		// get all possible moves and remove already explored nodes
		List<String> moves = temp.player.getPossibleMoves(temp.playState);
		for (MonteCarloNode child : temp.children) {
			for (int i=0; i<moves.size(); i++)
				if (moves.get(i).equals(child.player.getLastMove()))
					moves.remove(i);
		}

		// choose a move at random from unexplored
		if (moves.size()==0) {
			return;
		}
		String move = moves.get(rand.nextInt(moves.size()));
		MonteCarloNode newNode = new MonteCarloNode(move, temp);
		temp.children.add(newNode);

		// simulation
		Player simulation  = new Player(newNode.player);
		PlayState simState = new PlayState(newNode.playState);
		while (true) {

			List<String> simMoves = simulation.getPossibleMoves(simState);
			move = simMoves.get(rand.nextInt(simMoves.size()));
			simulation.play(simState, move);

			if (simState.isTurn(BoardStates.DWARF)) {
				if (simulation.getBoard().getNumDwarfs() < 2)
					break;
			}
			else {
				if (simulation.getBoard().getNumTrolls() < 3)
					break;
			}
		}

		// backprop
		int winsInc;
		if (simState.isTurn(this.side))
			winsInc = 1;
		else
			winsInc = -1;

		temp = newNode;
		while (temp != root) {
			temp.wins += winsInc;
			temp.visits += 1;
			temp = temp.parent;
		}
	}
}
