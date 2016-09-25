package process;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import node.Node;
import node.NodeAbstract;

public class ElectionNode extends NodeAbstract {

	protected AtomicInteger countedEchos;
	protected Node wakeupNeighbour;
	protected Object data;

	private Integer strength;
	private AtomicBoolean restart = new AtomicBoolean(true);
	private boolean echo = false;

	public ElectionNode(String name, boolean initiator, CountDownLatch startLatch, int strength) {
		super(name, initiator, startLatch);
		if (initiator) {
			this.strength = strength;
			start();
		}
		countedEchos = new AtomicInteger(0);
	}

	@Override
	public synchronized void wakeup(Node neighbour, int strength) {
		if (this.strength == null || this.strength < strength) {
			this.strength = strength;
			this.data = null;
			countedEchos.set(0);
			wakeupNeighbour = neighbour;
			restart.set(true);
		}
		if (this.strength == strength) {
			System.out.println(this + " will increment counter in wakeup from " + countedEchos.get());
			countedEchos.incrementAndGet();
			System.out.println(this + " has incremented counter in wakeup to " + countedEchos.get());
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
		while (true) {
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
								node.wakeup(this, strength);
							}
						}
					}
					synchronized (this) {

						while (countedEchos.get() < neighbours.size() && !restart.get()) {
							try {
								wait();
								System.out.println(this + ":" + countedEchos.get() + "/" + neighbours.size()
										+ " Strength: " + this.strength);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				} while (restart.get() && countedEchos.get() < neighbours.size());

				synchronized (this) {
					if (initiator && wakeupNeighbour == null) {
						if (!echo){
							System.out.println("Fertig: " + this + " wurde gewählt");
							restart.set(echo = true);
							strength += 1;
							this.data = null;
						} else {
							System.out.println("Fertig: " + data);
							System.exit(0);
						}						
					} else {
						wakeupNeighbour.echo(this, wakeupNeighbour + "-" + this + (data != null ? "," + data : ""),
								strength);
					}

				
					if (countedEchos.get() >= neighbours.size()) {
						System.out.println(this + " reset counter (current value: " + countedEchos.get() + ")");
						countedEchos.set(0);
					} else {
						System.out.println(this + " has a problem");
					}
				}

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

	public synchronized void echo(Node neighbour, Object data, int strength) {
		if (this.strength == strength) {
			if (this.data == null) {
				this.data = data;
			} else {
				this.data = data + "," + this.data;
			}
			System.out.println("echo von " + neighbour + " an " + this);
			System.out.println(this + " will increment counter in echo from " + countedEchos.get());
			countedEchos.incrementAndGet();
			System.out.println(this + " has incremented counter in echo to " + countedEchos.get());
			notifyAll();
		} else {
			System.out.println(this + " echo ging daneben " + this.strength + "/" + strength);
		}
	}

	@Override
	public String toString() {
		return super.toString() + "(" + strength + ")";
	}
}
