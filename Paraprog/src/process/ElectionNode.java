package process;

import java.util.concurrent.CountDownLatch;

import node.Node;

public class ElectionNode extends Nodes implements node.IElectionNode {
	private Integer strength;
	private boolean neustart = true;

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
			neustart = true;
		}
		if (this.strength == strength) {
			countedEchos.incrementAndGet();
		}
		System.out.println(this + " received wakeup from " + neighbour + " counter: " + countedEchos.get() + "|" + neighbours.size());
		if (!isAlive()) {
			start();
		}
		notifyAll();
	}

	@Override
	public void run() {
		try {
			startLatch.await();
			do {
				if (neustart) {
					System.out.println(this + " start/restart");
					for (Node node : neighbours) {
						System.out.println(" ");
						if (node != wakeupNeighbour) {
							((ElectionNode) node).wakeup(this, strength);
						}
					}
					neustart = false;
				}
				synchronized (this) {
					while (countedEchos.get() < neighbours.size()) {
						try {
							wait();
							System.out.println(this + ":" + countedEchos.get() + "/" + neighbours.size() + " Strength: "
									+ this.strength);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			} while (neustart);

			if (initiator && wakeupNeighbour == null) {
				System.out.println("Fertig: " + data);
			} else {
				wakeupNeighbour.echo(this, wakeupNeighbour + "-" + this + (data != null ? "," + data : ""));
			}
		} catch (InterruptedException e1) {
			e1.printStackTrace();
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

	@Override
	public String toString() {
		return super.toString() + "(" + strength +  ")";
	}
}
