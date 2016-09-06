package process;

import java.util.concurrent.CountDownLatch;

import node.Node;
import node.NodeAbstract;

public class Nodes extends NodeAbstract implements Runnable {
	private int countedEchos = 0;

	public Nodes(String name, boolean initiator, CountDownLatch startLatch) {
		super(name, initiator, startLatch);
	}

	@Override
	public void hello(Node neighbour) {
		if (!neighbours.contains(neighbour)) {
			neighbours.add(neighbour);
		}
	}

	@Override
	public synchronized void wakeup(Node neighbour) {
		++countedEchos;
		if (neighbours.size() <= 1) {
			neighbour.echo(this, null);
		} else {
			// wait for echo of all neighbours
			while (true) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (countedEchos >= neighbours.size()) {
					neighbour.echo(this, null);
					continue;
				}
			}
		}
	}

	@Override
	public void echo(Node neighbour, Object data) {
		// wait for all neightbours sending an echo
		++countedEchos;
		notify();
	}

	@Override
	public void setupNeighbours(Node... neighbours) {
		for (Node node : neighbours) {
			this.neighbours.add(node);
			node.hello(this);
		}
	}

}
