package Start;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import node.Node;
import process.Nodes;

public class Start {

	public static void main(String[] args) {
		int nodes = 9;
		loopTree(nodes);
	}

	private static void loopTree(int nodesToCreate) throws IllegalArgumentException{
		System.out.println("Anzahl der Nodes "+nodesToCreate);
		if(nodesToCreate <3){
			throw new IllegalArgumentException("There must be at least 3 Nodes for a loop");
		}
		CountDownLatch latch = new CountDownLatch(nodesToCreate);
		Nodes init = new Nodes("Initiator", true, latch);
		Nodes node1 = new Nodes("Node1", false, latch);
		Nodes node2 = new Nodes("Node2",false,latch);
		
		Set<Nodes> nodes = new HashSet<>();
		nodes.add(init);
		nodes.add(node1);
		nodes.add(node2);
		
		Random r = new Random();
		for (int i = 3; i < nodesToCreate; i++) {
			Nodes newNode = new Nodes("Node"+i, false, latch);
			int neighboursCount = r.nextInt(nodes.size())+1;
			List<Nodes> possibleNeighbours = new ArrayList<>(nodes);
			Set<Nodes> neighbours = new HashSet<>();
			for (int j = 0; j < neighboursCount; j++) {
				Nodes newNeighbour = possibleNeighbours.get(r.nextInt(possibleNeighbours.size()));
				possibleNeighbours.remove(newNeighbour);
				neighbours.add(newNeighbour);
			}
			nodes.add(newNode);
			newNode.setupNeighbours(neighbours.toArray(new Node[neighbours.size()]));
			for (Nodes nodes2 : neighbours) {
				System.out.println(nodes2);
			}
		}
		
		init.setupNeighbours(node1,node2);
		node1.setupNeighbours(init,node2);
		node2.setupNeighbours(node1,init);

	}

}
