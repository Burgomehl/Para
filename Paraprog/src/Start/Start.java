package Start;

import java.util.concurrent.CountDownLatch;

import process.Nodes;

public class Start {

	public static void main(String[] args) {
		int nodes = 2;
		CountDownLatch latch = new CountDownLatch(nodes);
		Nodes init = new Nodes("Initiator", true, latch);
		Nodes node1 = new Nodes("Node1", false, latch);
		Nodes node2 = new Nodes("Node2",false,latch);
		Nodes node3 = new Nodes("Node3",false,latch);
		Nodes node4 = new Nodes("Node4",false,latch);
		
		init.setupNeighbours(node1,node4);
		node1.setupNeighbours(init,node2);
		node2.setupNeighbours(node1);
		node3.setupNeighbours(node1,node2);
		node4.setupNeighbours(init);
	}

}
