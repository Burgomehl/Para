package process;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import node.Node;
import node.NodeAbstract;

public class Nodes extends NodeAbstract {
	private AtomicInteger countedEchos;
	private Node wakeupNeighbour;
	private Object data;

	public Nodes(String name, boolean initiator, CountDownLatch startLatch) {
		super(name, initiator, startLatch);
		if (initiator) {
			start();
		}
		countedEchos = new AtomicInteger(0);
	}

	@Override
	public void hello(Node neighbour) {
		neighbours.add(neighbour);
	}

	@Override
	public synchronized void wakeup(Node neighbour) {
		System.out.println("wakeup von " + neighbour + " an " + this);
		countedEchos.incrementAndGet();
		if (wakeupNeighbour == null) {
			wakeupNeighbour = neighbour;
			start();
		} else {
			notifyAll();
		}
	}

	@Override
	public void run() {
		runHelper();

	}

	private synchronized void runHelper() {
		try {
			startLatch.await();
			for (Node node : neighbours) {
				if (node != wakeupNeighbour) {
					node.wakeup(this);
				}
			}
			while (countedEchos.get() < neighbours.size()) {
				wait();
			}
			if (initiator) {
				System.out.println("Fertig: " + data);
			} else {
				wakeupNeighbour.echo(this, wakeupNeighbour + "-" + this + (data != null?"," + data:""));
			}
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}

	@Override
	public synchronized void echo(Node neighbour, Object data) {
		if (this.data == null) {
			this.data = data;
		} else {
			this.data = data + "," + this.data;
		}
		System.out.println("echo von " + neighbour + " an " + this);
		countedEchos.incrementAndGet();
		notifyAll();
	}

	@Override
	public void setupNeighbours(Node... neighbours) {
		if (neighbours != null) {
			for (Node node : neighbours) {
				this.neighbours.add(node);
				node.hello(this);
			}
		}
		startLatch.countDown();
	}

}
