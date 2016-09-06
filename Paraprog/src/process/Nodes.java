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
		countedEchos.incrementAndGet();
		System.out.println("wakeup von " + neighbour + " an " + this + ", anzahl Nachrichten: " + countedEchos.get()
				+ "/" + neighbours.size());
		if (wakeupNeighbour == null && !initiator) {
			wakeupNeighbour = neighbour;
			start();
		}
		yield();
		testFinish();		
	}

	@Override
	public void run() {
		runHelper();

	}

	private void runHelper() {
		try {
			startLatch.await();
			for (Node node : neighbours) {
				System.out.println(" ");
				if (node != wakeupNeighbour) {
					node.wakeup(this);
				}
			}
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
	
	private void testFinish(){
		System.out.println(this + ": " + countedEchos.get() + "/" + neighbours.size());
		if (countedEchos.get() >= neighbours.size()) {
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
		testFinish();
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
