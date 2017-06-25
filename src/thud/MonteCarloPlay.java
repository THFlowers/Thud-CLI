package thud;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
/*
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
*/

/**
 * Created by Thai Flowers on 6/16/2017.
 *
 * Based on:
 *     https://jeffbradberry.com/posts/2015/09/intro-to-monte-carlo-tree-search/
 *     https://en.wikipedia.org/wiki/Monte_Carlo_tree_search
 *
 * Background playOut thread was blazing fast and no exceptions, but requires gigs of ram
 * Even with holding the root node and its children, and not the whole tree
 *
 * I could modify this to add nodes when a move is unsimulated with a playOut(move)
 * but as it stands, the delay caused by completing playOuts for each leaf of the root branch
 * isn't too long and makes it feel like the ai is thinking (though it is dumber than the threaded version)
 *
 */
public class MonteCarloPlay {
	static final int MAX_SIM_MOVES = 12;
	//final Lock lock = new ReentrantLock();
	//final Condition runningPlayout = lock.newCondition();

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
			int c;
			if (this.playState.isTurn(BoardStates.DWARF))
				c = 1;
			else
				c = 2000;

			return (((double)wins) / ((double)visits)) + c*Math.sqrt(Math.log((double)numPlayouts)/(double)visits);
		}
	}

	//boolean destroy = false;
	Random rand = new Random();
	int numPlayouts = 0;
	MonteCarloNode root = new MonteCarloNode("", null);
	BoardStates side;

	public MonteCarloPlay(BoardStates side){
		this.side = side;
		/*
		Runnable playoutLoop = () -> {
			for (long i=0; i<(long)Math.pow(10, 5); i++) {
				playOut();
				lock.lock();
				if (destroy) {
					lock.unlock();
					break;
				}
				lock.unlock();
			}
		};
		new Thread(playoutLoop).start();
		*/
	}

	/*
	public void destroy() {
		lock.lock();
		destroy = true;
		root = null;
		lock.unlock();
	}
	*/

	void opponentPlay(String move) {
		//lock.lock();
		//try {
		while (root.children == null || root.children.size() < root.possibleMoves.size())
			playOut();
			//runningPlayout.awaitUninterruptibly();
		for (MonteCarloNode child : root.children)
			if (child.player.getLastMove().equals(move))
				root = child;
		/*
		} finally {
			lock.unlock();
		}
		*/
	}

	String selectPlay() {
		/*
		String move;
		lock.lock();
		try {
		*/
		while (root.children == null || root.children.size() < root.possibleMoves.size()) {
			//runningPlayout.awaitUninterruptibly();
			playOut();
		}

		MonteCarloNode bestChoice = null;
		for (MonteCarloNode child : root.children) {
			if (bestChoice == null)
				bestChoice = child;
			else if (child.score() > bestChoice.score())
				bestChoice = child;
		}

		root = bestChoice;
		return root.player.getLastMove();
		/*
		} finally {
			lock.unlock();
		}

		return move;
		*/
	}

	void playOut() {
		/*
		lock.lock();
		try {
			if (destroy) {
				return;
			}
		*/

		numPlayouts++;
		MonteCarloNode current = root;

		// Selection
		while (current.children != null && current.children.size() == current.possibleMoves.size()) {
			MonteCarloNode bestChoice = current.children.get(0);
			for (int i = 1; i < current.children.size(); i++) {
				MonteCarloNode curChild = current.children.get(i);
				if (curChild.score() > bestChoice.score())
					bestChoice = curChild;
			}
			current = bestChoice;
		}

		// Expansion
		if (current.children == null)
			current.children = new LinkedList<>();

		// get all possible moves and remove already explored nodes
		List<String> moves = current.player.getPossibleMoves(current.playState);
		for (MonteCarloNode child : current.children) {
			for (int i = 0; i < moves.size(); i++)
				if (moves.get(i).equals(child.player.getLastMove()))
					moves.remove(i);
		}

		// choose a move at random from unexplored
		if (moves.size() == 0) {
			return;
		}
		String move = moves.get(rand.nextInt(moves.size()));
		MonteCarloNode newNode = new MonteCarloNode(move, current);
		current.children.add(newNode);

		// simulation
		Player simulation = new Player(newNode.player);
		PlayState simState = new PlayState(newNode.playState);
		for (int i = 0; i < MAX_SIM_MOVES; i++) {

			List<String> simMoves = simulation.getPossibleMoves(simState);
			move = simMoves.get(rand.nextInt(simMoves.size()));
			simulation.play(simState, move);

			if (simState.isTurn(BoardStates.DWARF)) {
				if (simulation.getBoard().getNumDwarfs() < 2)
					break;
			} else {
				if (simulation.getBoard().getNumTrolls() < 3)
					break;
			}
		}

		simulation.calculateScores(1);
		int[] scores = simulation.getScores();

		// backprop
		int winsInc = 0;
		if (side == BoardStates.DWARF)
			winsInc = (scores[0] > scores[1]) ? 1 : -1;
		else //if (side == BoardStates.TROLL)
			winsInc = (scores[0] < scores[1]) ? 1 : -1;

		current = newNode;
		while (current != root) {
			current.wins += winsInc;
			current.visits += 1;
			current = current.parent;
		}
		/*
		} finally {
			runningPlayout.signal();
			lock.unlock();
		}
		*/
	}
}
