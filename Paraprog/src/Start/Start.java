package Start;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

import node.Node;
import node.NodeAbstract;
import process.ElectionNode;
import process.EchoNode;

public class Start {
	private static Random r = new Random();

	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("You have to set some Parameters");
			System.out.println("ellipse 2 paramters: e and a number with min 3");
			System.out.println("nodeLoop 1 parameter: nL");
			System.out.println("tree 2 parameters: t and a numer with min 1");
			System.out.println("graph with a loop 2 parameters: lt and a number with min 3");
			System.out.println("full graph with two paramters: fg and a number with min 1");
		} else {
			int nodes = 1;
			if(args.length==2){
				nodes = Integer.parseInt(args[1]);
			}
			switch (args[0]) {
			case "e":
				 ellipse(nodes);
				break;
			case "nl":
				 nodeLoop();
				break;
			case "t":
				 tree(nodes);
				break;
			case "lt":
				loopTree(nodes);
				break;
			case "fg":
				fullGraph(nodes);
				break;
			case "election":
				election(3);
				break;
			}
		}
	}
	
	public static void election(int nodesToCreate){
		System.out.println("Anzahl der ElectionNode " + nodesToCreate);
		if (nodesToCreate < 3) {
			throw new IllegalArgumentException("There must be at least 3 ElectionNode for a loop");
		}
		CountDownLatch latch = new CountDownLatch(nodesToCreate+1);
		ElectionNode init = new ElectionNode("Initiator", true, latch,0);

		ElectionNode temp = init;
		for (int i = 1; i < nodesToCreate; i++) {
			ElectionNode newNode = new ElectionNode("Node" + i, r.nextBoolean(), latch,i);
			temp.setupNeighbours(newNode);
			temp = newNode;
		}
		ElectionNode newNode = new ElectionNode("StrongInitiator", true, latch,nodesToCreate+1);
		temp.setupNeighbours(newNode);
		newNode.setupNeighbours(init);
	}

	public static void fullGraph(int nodesToCreate) {
		System.out.println("Anzahl der Nodes " + nodesToCreate);
		if (nodesToCreate < 1) {
			throw new IllegalArgumentException("There must be at least 1 Node for a loop");
		}
		CountDownLatch latch = new CountDownLatch(nodesToCreate);
		Set<NodeAbstract> nodes = new HashSet<>();
		IntStream.range(0, nodesToCreate).forEach( i -> nodes.add(new EchoNode(i==0?"Initiator":"Node" + i, i==0?true:false, latch)));
		nodes.forEach(node -> node.setupNeighbours(nodes.toArray(new EchoNode[nodes.size()])));
	}
	
	public static void fullGraphElection(int nodesToCreate) {
		System.out.println("Anzahl der Nodes " + nodesToCreate);
		if (nodesToCreate < 1) {
			throw new IllegalArgumentException("There must be at least 1 Node for a loop");
		}
		CountDownLatch latch = new CountDownLatch(nodesToCreate);
		NodeAbstract init = new ElectionNode("Initiator", true, latch , 0);

		Set<NodeAbstract> nodes = new HashSet<>();
		nodes.add(init);

		for (int i = 1; i < nodesToCreate; i++) {
			nodes.add(new ElectionNode("Node" + i, r.nextBoolean(), latch, i));
		}
		for (NodeAbstract node : nodes) {
			node.setupNeighbours(nodes.toArray(new ElectionNode[nodes.size()]));
		}
	}

	public static void tree(int nodesToCreate) {
		System.out.println("Anzahl der Nodes " + nodesToCreate);
		if (nodesToCreate < 1) {
			throw new IllegalArgumentException("There must be at least 1 Node for a loop");
		}
		CountDownLatch latch = new CountDownLatch(nodesToCreate);
		EchoNode init = new EchoNode("Initiator", true, latch);

		Set<EchoNode> nodes = new HashSet<>();
		nodes.add(init);

		Random r = new Random();
		for (int i = 1; i < nodesToCreate; i++) {
			EchoNode newNode = new EchoNode("Node" + i, false, latch);
			List<EchoNode> possibleNeighbours = new ArrayList<>(nodes);
			newNode.setupNeighbours(possibleNeighbours.get(r.nextInt(possibleNeighbours.size())));
			nodes.add(newNode);
		}
		init.setupNeighbours();
		System.out.println("init fertig");
	}

	public static void ellipse(int nodesToCreate) {
		System.out.println("Anzahl der Nodes " + nodesToCreate);
		if (nodesToCreate < 3) {
			throw new IllegalArgumentException("There must be at least 3 Nodes for a loop");
		}
		CountDownLatch latch = new CountDownLatch(nodesToCreate);
		EchoNode init = new EchoNode("Initiator", true, latch);

		EchoNode temp = init;
		for (int i = 1; i < nodesToCreate; i++) {
			EchoNode newNode = new EchoNode("Node" + i, false, latch);
			temp.setupNeighbours(newNode);
			temp = newNode;
		}
		temp.setupNeighbours(init);
	}

	public static void nodeLoop() {
		System.out.println("Anzahl der Nodes " + 1);
		CountDownLatch latch = new CountDownLatch(1);
		EchoNode init = new EchoNode("Initiator", true, latch);

		Set<EchoNode> nodes = new HashSet<>();
		nodes.add(init);

		init.setupNeighbours(init);
	}

	public static void loopTree(int nodesToCreate) throws IllegalArgumentException {
		System.out.println("Anzahl der Nodes " + nodesToCreate);
		if (nodesToCreate < 3) {
			throw new IllegalArgumentException("There must be at least 3 Nodes for a loop");
		}
		CountDownLatch latch = new CountDownLatch(nodesToCreate);
		EchoNode init = new EchoNode("Initiator", true, latch);
		EchoNode node1 = new EchoNode("Node1", false, latch);
		EchoNode node2 = new EchoNode("Node2", false, latch);

		Set<EchoNode> nodes = new HashSet<>();
		nodes.add(init);
		nodes.add(node1);
		nodes.add(node2);

		Random r = new Random();
		for (int i = 3; i < nodesToCreate; i++) {
			EchoNode newNode = new EchoNode("Node" + i, false, latch);
			int neighboursCount = r.nextInt(nodes.size()) + 1;
			List<EchoNode> possibleNeighbours = new ArrayList<>(nodes);
			Set<EchoNode> neighbours = new HashSet<>();
			for (int j = 0; j < neighboursCount; j++) {
				EchoNode newNeighbour = possibleNeighbours.get(r.nextInt(possibleNeighbours.size()));
				possibleNeighbours.remove(newNeighbour);
				neighbours.add(newNeighbour);
			}
			nodes.add(newNode);
			newNode.setupNeighbours(neighbours.toArray(new Node[neighbours.size()]));
			for (EchoNode nodes2 : neighbours) {
				System.out.println(nodes2);
			}
		}

		init.setupNeighbours(node1, node2);
		node1.setupNeighbours(init, node2);
		node2.setupNeighbours(node1, init);

	}

}
