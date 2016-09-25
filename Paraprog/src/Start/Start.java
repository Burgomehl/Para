package Start;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiFunction;

import node.Node;
import node.NodeAbstract;
import process.EchoNode;
import process.ElectionNode;

public class Start {
	private static Random r = new Random();

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("You have to set some Parameters");
			System.out.println("first off all you have to decide between 'echo' or 'election' at the first paramter");
			System.out.println("ellipse 2 paramters: e and a number with min 3");
			System.out.println("nodeLoop 1 parameter: nL");
			System.out.println("tree 2 parameters: t and a numer with min 1");
			System.out.println("graph with a loop 2 parameters: lt and a number with min 3");
			System.out.println("full graph with two paramters: fg and a number with min 1");
		} else {
			if (!args[0].equals("echo") && !args[0].equals("election")) {
				System.out.println("You have to decide between 'echo' and 'elction' as first paramter ");
			} else {
				int nodes = 1;
				BiFunction<CountDownLatch, Integer, NodeAbstract> function = (latch,
						i) -> new EchoNode((i == 0 ? "Initiator" : "Node") + i, (i == 0 ? true : false), latch);
				if (args[0].equals("election")) {
					function = (latch, i) -> new ElectionNode("Node" + i, (i==0?true:r.nextBoolean()), latch, i);
				}
				if (args.length == 3) {
					nodes = Integer.parseInt(args[2]);
				}
				switch (args[1]) {
				case "e":
					ellipse(nodes, function);
					break;
				case "nl":
					nodeLoop(function);
					break;
				case "t":
					tree(nodes, function);
					break;
				case "lt":
					loopTree(nodes, function);
					break;
				case "fg":
					fullGraph(nodes, function);
					break;
				case "fgwl":
					fullGraphWithLoops(nodes, function);
					break;
				default:
					System.out.println("Missing Parameters for further information start application without params");
					break;
				}
			}
		}
	}

	public static void fullGraph(int nodesToCreate, BiFunction<CountDownLatch, Integer, NodeAbstract> function) {
		System.out.println("Anzahl der Nodes " + nodesToCreate);
		if (nodesToCreate < 1) {
			throw new IllegalArgumentException("There must be at least 1 Node for a loop");
		}
		CountDownLatch latch = new CountDownLatch(nodesToCreate);
		NodeAbstract init = function.apply(latch, 0);

		Set<NodeAbstract> nodes = new HashSet<>();
		nodes.add(init);

		for (int i = 1; i < nodesToCreate; i++) {
			nodes.add(function.apply(latch, i));
		}
		for (NodeAbstract node : nodes) {
			Set<NodeAbstract> copyNode = new HashSet<>(nodes);
			copyNode.remove(node);
			node.setupNeighbours(copyNode.toArray(new NodeAbstract[copyNode.size()]));
		}
	}
	
	public static void fullGraphWithLoops(int nodesToCreate, BiFunction<CountDownLatch, Integer, NodeAbstract> function) {
		System.out.println("Anzahl der Nodes " + nodesToCreate);
		if (nodesToCreate < 1) {
			throw new IllegalArgumentException("There must be at least 1 Node for a loop");
		}
		CountDownLatch latch = new CountDownLatch(nodesToCreate);
		NodeAbstract init = function.apply(latch, 0);

		Set<NodeAbstract> nodes = new HashSet<>();
		nodes.add(init);

		for (int i = 1; i < nodesToCreate; i++) {
			nodes.add(function.apply(latch, i));
		}
		for (NodeAbstract node : nodes) {
			node.setupNeighbours(nodes.toArray(new NodeAbstract[nodes.size()]));
		}
	}

	public static void tree(int nodesToCreate, BiFunction<CountDownLatch, Integer, NodeAbstract> function) {
		System.out.println("Anzahl der Nodes " + nodesToCreate);
		if (nodesToCreate < 1) {
			throw new IllegalArgumentException("There must be at least 1 Node for a loop");
		}
		CountDownLatch latch = new CountDownLatch(nodesToCreate);
		NodeAbstract init = function.apply(latch, 0);

		Set<NodeAbstract> nodes = new HashSet<>();
		nodes.add(init);

		Random r = new Random();
		for (int i = 1; i < nodesToCreate; i++) {
			NodeAbstract newNode = function.apply(latch, i);
			List<NodeAbstract> possibleNeighbours = new ArrayList<>(nodes);
			newNode.setupNeighbours(possibleNeighbours.get(r.nextInt(possibleNeighbours.size())));
			nodes.add(newNode);
		}
		init.setupNeighbours();
		System.out.println("init fertig");
	}

	public static void ellipse(int nodesToCreate, BiFunction<CountDownLatch, Integer, NodeAbstract> function) {
		System.out.println("Anzahl der Nodes " + nodesToCreate);
		if (nodesToCreate < 3) {
			throw new IllegalArgumentException("There must be at least 3 Nodes for a loop");
		}
		CountDownLatch latch = new CountDownLatch(nodesToCreate);
		NodeAbstract init = function.apply(latch, 0);

		NodeAbstract temp = init;
		for (int i = 1; i < nodesToCreate; i++) {
			NodeAbstract newNode = function.apply(latch, i);
			temp.setupNeighbours(newNode);
			temp = newNode;
		}
		temp.setupNeighbours(init);
	}

	public static void nodeLoop(BiFunction<CountDownLatch, Integer, NodeAbstract> function) {
		System.out.println("Anzahl der Nodes " + 1);
		CountDownLatch latch = new CountDownLatch(1);
		NodeAbstract init = function.apply(latch, 0);

		Set<NodeAbstract> nodes = new HashSet<>();
		nodes.add(init);

		init.setupNeighbours(init);
	}

	public static void loopTree(int nodesToCreate, BiFunction<CountDownLatch, Integer, NodeAbstract> function)
			throws IllegalArgumentException {
		System.out.println("Anzahl der Nodes " + nodesToCreate);
		if (nodesToCreate < 3) {
			throw new IllegalArgumentException("There must be at least 3 Nodes for a loop");
		}
		CountDownLatch latch = new CountDownLatch(nodesToCreate);
		NodeAbstract init = function.apply(latch, 0);
		NodeAbstract node1 = function.apply(latch, 1);
		NodeAbstract node2 = function.apply(latch, 2);

		Set<NodeAbstract> nodes = new HashSet<>();
		nodes.add(init);
		nodes.add(node1);
		nodes.add(node2);

		Random r = new Random();
		for (int i = 3; i < nodesToCreate; i++) {
			NodeAbstract newNode = function.apply(latch, i);
			int neighboursCount = r.nextInt(nodes.size()) + 1;
			List<NodeAbstract> possibleNeighbours = new ArrayList<>(nodes);
			Set<NodeAbstract> neighbours = new HashSet<>();
			for (int j = 0; j < neighboursCount; j++) {
				NodeAbstract newNeighbour = possibleNeighbours.get(r.nextInt(possibleNeighbours.size()));
				possibleNeighbours.remove(newNeighbour);
				neighbours.add(newNeighbour);
			}
			nodes.add(newNode);
			newNode.setupNeighbours(neighbours.toArray(new Node[neighbours.size()]));
			for (NodeAbstract nodes2 : neighbours) {
				System.out.println(nodes2);
			}
		}

		init.setupNeighbours(node1, node2);
		node1.setupNeighbours(init, node2);
		node2.setupNeighbours(node1, init);

	}

}
