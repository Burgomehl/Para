package process;

import java.util.concurrent.CountDownLatch;

import node.Node;
import node.NodeAbstract;

public class Processor extends NodeAbstract implements Runnable, Node {

	public Processor(String name, boolean initiator, CountDownLatch startLatch) {
		super(name, initiator, startLatch);
	}

	@Override
	public void run() {

	}

	@Override
	public void hello(Node neighbour) {
		// TODO Auto-generated method stub

	}

	@Override
	public void wakeup(Node neighbour) {
		// TODO Auto-generated method stub

	}

	@Override
	public void echo(Node neighbour, Object data) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setupNeighbours(Node... neighbours) {
		// TODO Auto-generated method stub

	}

}
