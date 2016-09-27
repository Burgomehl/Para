package process;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import node.Node;
import node.NodeAbstract;

public class EchoNode extends NodeAbstract {
	protected AtomicInteger countedEchos;
	protected Node wakeupNeighbour;
	protected Object data;

	public EchoNode(String name, boolean initiator, CountDownLatch startLatch) {
		super(name, initiator, startLatch);
		if (initiator) {
			start();
		}
		countedEchos = new AtomicInteger(0);
	}

	@Override
	public synchronized void wakeup(Node neighbour, String initiatorName, boolean isElection) {
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
					node.wakeup(this, null, false);
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
				wakeupNeighbour.echo(this, wakeupNeighbour + "-" + this + (data != null ? "," + data : ""), null, false);
			}
		}

	}

	@Override
	public synchronized void echo(Node neighbour, Object data, String initiatorName, boolean isElection) {
		if (this.data == null) {
			this.data = data;
		} else {
			this.data = data + "," + this.data;
		}
		System.out.println("echo von " + neighbour + " an " + this);
		countedEchos.incrementAndGet();
		notifyAll();
	}

}
