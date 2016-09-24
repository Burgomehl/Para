package process;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import node.Node;

public class ElectionNode extends Nodes implements node.IElectionNode {
	private Integer strength;
	private AtomicBoolean restart = new AtomicBoolean(true);

	public ElectionNode(String name, boolean initiator, CountDownLatch startLatch, int strength) {
		super(name, initiator, startLatch);
		if (initiator) {
			this.strength = strength;
		}
	}

	@Override
	public synchronized void wakeup(Node neighbour, int strength) {
		if (this.strength == null || this.strength < strength) {
			this.strength = strength;
			countedEchos.set(0);
			wakeupNeighbour = neighbour;
			restart.set(true);
		}
		if (this.strength == strength) {
			countedEchos.incrementAndGet();
		}
		System.out.println(this + " received wakeup from " + neighbour + " counter: " + countedEchos.get() + "|"
				+ neighbours.size() + " neustart: " + restart.get());
		if (State.NEW == this.getState()) {
			start();
		}
		notifyAll();
	}

	@Override
	public void run() {
		while (true){
			try {
				startLatch.await();
				do {
					if (restart.getAndSet(false)) {
						System.out.println(this + " start/restart");
						for (Node node : neighbours) {
							if (restart.get()) {
								break;
							}
							System.out.println(" ");
							if (node != wakeupNeighbour) {
								((ElectionNode) node).wakeup(this, strength);
							}
						}
					}
					synchronized (this) {

						while (countedEchos.get() < neighbours.size() && !restart.get()) {
							try {
								wait();
								System.out.println(this + ":" + countedEchos.get() + "/" + neighbours.size() + " Strength: "
										+ this.strength);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				} while (restart.get());

				if (initiator && wakeupNeighbour == null) {
					System.out.println("Fertig: " + data);
					System.exit(0);
				} else {
					((ElectionNode)wakeupNeighbour).echo(this, wakeupNeighbour + "-" + this + (data != null ? "," + data : ""), strength);
				}
				countedEchos.set(0);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	public void setupNeighbours(ElectionNode... neighbours) {
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

	public synchronized void echo(Node neighbour, Object data, Integer strength) {
		if (this.strength == strength) {
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

	@Override
	public String toString() {
		return super.toString() + "(" + strength + ")";
	}
}
