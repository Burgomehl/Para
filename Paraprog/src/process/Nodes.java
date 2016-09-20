package process;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import node.Node;
import node.NodeAbstract;

public class Nodes extends NodeAbstract {
	protected AtomicInteger countedEchos;
	protected Node wakeupNeighbour;
	protected Object data;

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
		countedEchos.incrementAndGet();
		System.out.println("wakeup von " + neighbour + " an " + this + ", anzahl Nachrichten: " + countedEchos.get()
				+ "/" + neighbours.size());
		if (wakeupNeighbour == null && !initiator) {
			wakeupNeighbour = neighbour;
			start();
		}
		notifyAll();
	}

	@Override
	public void run() {
		try {
			startLatch.await();
			for (Node node : neighbours) {
				System.out.println(" ");
				if (node != wakeupNeighbour) {
					node.wakeup(this);
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		synchronized (this) {
			while (countedEchos.get() < neighbours.size()) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (initiator) {
				System.out.println("Fertig: " + data);
			} else {
				wakeupNeighbour.echo(this, wakeupNeighbour + "-" + this + (data != null ? "," + data : ""));
			}
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
		System.out.println(this + ": setupneighbours finished with " + (neighbours != null ? neighbours.length : "0")
				+ " neighbours");
		startLatch.countDown();
	}

}
