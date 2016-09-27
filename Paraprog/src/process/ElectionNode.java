package process;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import node.Node;
import node.NodeAbstract;

public class ElectionNode extends NodeAbstract {

	class Attributes {
		protected AtomicInteger countedEchos;
		protected Node wakeupNeighbour;
		protected Object data;

		private String initiatorName;
		private AtomicBoolean restart = new AtomicBoolean(false);
		private boolean echo = false;

		public AtomicInteger getCountedEchos() {
			return countedEchos;
		}

		public void setCountedEchos(AtomicInteger countedEchos) {
			this.countedEchos = countedEchos;
		}

		public Node getWakeupNeighbour() {
			return wakeupNeighbour;
		}

		public void setWakeupNeighbour(Node wakeupNeighbour) {
			this.wakeupNeighbour = wakeupNeighbour;
		}

		public Object getData() {
			return data;
		}

		public void setData(Object data) {
			this.data = data;
		}

		public AtomicBoolean getRestart() {
			return restart;
		}

		public void setRestart(AtomicBoolean restart) {
			this.restart = restart;
		}

		public boolean isEcho() {
			return echo;
		}

		public void setEcho(boolean echo) {
			this.echo = echo;
		}

		public synchronized String getInitiatorName() {
			return initiatorName;
		}

		public synchronized void setInitiatorName(String initiatorName) {
			this.initiatorName = initiatorName;
		}
	}

	private Attributes actual;
	private Attributes echo = new Attributes();
	private Attributes election = new Attributes();

	public ElectionNode(String name, boolean initiator, CountDownLatch startLatch) {
		super(name, initiator, startLatch);
		if (initiator) {
			election.setInitiatorName(name);
			echo.setInitiatorName(name);
			election.getRestart().set(true);
			start();
		}
		echo.setCountedEchos(new AtomicInteger(0));
		election.setCountedEchos(new AtomicInteger(0));
	}

	@Override
	public synchronized void wakeup(Node neighbour, String initiatorName, boolean isElection) {
		if (isElection) {
			actual = election;
			if (actual.getInitiatorName() == null || initiatorName.compareTo(actual.getInitiatorName()) > 0) {
				actual.setInitiatorName(initiatorName);
				actual.setData(null);
				actual.getCountedEchos().set(0);
				actual.setWakeupNeighbour(neighbour);
				actual.getRestart().set(true);
			}
			
			if (initiatorName.equals(actual.getInitiatorName())) {
				System.out.println(this + " will increment counter in wakeup from " + actual.getCountedEchos().get());
				actual.getCountedEchos().incrementAndGet();
				System.out.println(this + " has incremented counter in wakeup to " + actual.getCountedEchos().get());
			}
		} else {
			actual = echo;
			actual.setWakeupNeighbour(neighbour);
			actual.getCountedEchos().incrementAndGet();			
		}
		
		System.out.println(this + " received wakeup from " + neighbour + "counter: " + actual.getCountedEchos().get()
				+ "|" + neighbours.size() + " neustart: " + actual.getRestart().get());
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

					restart(election, true);
					restart(echo, false);
					synchronized (this) {

						while ((election.getCountedEchos().get() < neighbours.size() && !election.getRestart().get())
								&& (echo.getCountedEchos().get() < neighbours.size() && !echo.getRestart().get())) {
							try {
								wait();
								System.out.println(this + ":" + actual.getCountedEchos().get() + "/" + neighbours.size()
										+ " Initiator: " + actual.getInitiatorName());
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				} while (election.getRestart().get() && election.getCountedEchos().get() < neighbours.size());
				yield();
				synchronized (this) {
					boolean isElection;
					if (election.getCountedEchos().get() < neighbours.size()) {
						actual = echo;
						isElection = false;
					} else {
						actual = election;
						isElection = true;
					}
					if (initiator && actual.getWakeupNeighbour() == null) {
						if (isElection) {
							System.out.println("Fertig: " + this + " wurde gew�hlt");
							actual.getRestart().set(false);
							echo.getRestart().set(true);
							actual.getCountedEchos().set(0);
							actual.setData(null);
							echo.setWakeupNeighbour(null);
						} else {
							System.out.println("Fertig: " + actual.getData());
							if (actual.getData() != null) {
								System.out.println(
										"Ausgabenmenge: " + (((String) (actual.getData())).split(",").length + 1));
							}
						}
					} else {
						actual.getWakeupNeighbour().echo(this,
								actual.getWakeupNeighbour() + "-" + this
										+ (actual.getData() != null ? "," + actual.getData() : ""),
								actual.getInitiatorName(), isElection);
					}

//					if (actual.getCountedEchos().get() >= neighbours.size()) {
//						System.out.println(
//								this + " reset counter (current value: " + actual.getCountedEchos().get() + ")");
//						actual.getCountedEchos().set(0);
//					} else {
//						System.out.println(this + " has a problem");
//					}
				}

			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	private void restart(Attributes att, boolean isElection) {
		if (att.getRestart().getAndSet(false)) {
			if (initiator) {
				System.out.println(this + " started " + (isElection ? "Election" : "Echo"));
			}
			System.out.println(this + " start/restart");
			for (Node node : neighbours) {
				String initiatorName = att.getInitiatorName();
				if (att.getRestart().get()) {
					break;
				}
				if (node != att.getWakeupNeighbour()) {
					node.wakeup(this, initiatorName, isElection);
				}
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

	public synchronized void echo(Node neighbour, Object data, String initiatorName, boolean isElection) {
		if (!isElection || actual.getInitiatorName().equals(initiatorName)) {
			if (actual.getData() == null) {
				actual.setData(data);
			} else {
				actual.setData(data + "," + actual.getData());
			}
			System.out.println("echo von " + neighbour + " an " + this);
			System.out.println(this + " will increment counter in echo from " + actual.getCountedEchos().get());
			actual.getCountedEchos().incrementAndGet();
			System.out.println(this + " has incremented counter in echo to " + actual.getCountedEchos().get());
			notifyAll();
		} else {
			System.out.println(this + " echo ging daneben " + actual.getInitiatorName() + "/" + initiatorName);
		}
	}

	@Override
	public String toString() {
		return super.toString() + "(" + election.getInitiatorName() + ")";
	}
}
