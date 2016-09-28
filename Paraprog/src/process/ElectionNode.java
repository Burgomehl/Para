package process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import node.Attributes;
import node.Node;
import node.NodeAbstract;

public class ElectionNode extends NodeAbstract {

	private Attributes actual;
	private Map<String, Attributes> echo = Collections.synchronizedMap(new HashMap<>());
	private Attributes election = new Attributes(true);

	public ElectionNode(String name, boolean initiator, CountDownLatch startLatch) {
		super(name, initiator, startLatch);
		if (initiator) {
			election.setInitiatorName(name);
			election.getRestart().set(true);
			start();
		}
		election.setCountedEchos(new AtomicInteger(0));
	}

	@Override
	public synchronized void wakeup(Node neighbour, String initiatorName, boolean isElection) {
		System.out.println(this + " test " + isElection);
		if (isElection) {
//			System.out.println(this + "election go");
			if (election.getInitiatorName() == null || initiatorName.compareTo(election.getInitiatorName()) > 0) {
//				System.out.println(this + "setnew data for election");
				election.getRestart().set(true);
				election.setInitiatorName(initiatorName);
				election.setData(null);
				election.getCountedEchos().set(0);
				election.setWakeupNeighbour(neighbour);
//				System.out.println(this + " have set data for election");
			}

			System.out.println(this+": "+initiatorName +"  "+ election.getInitiatorName());
			if (initiatorName.equals(election.getInitiatorName())) {
				election.getCountedEchos().incrementAndGet();
			}
			System.out.println(
					this + " received wakeup from " + neighbour + "counter: " + election.getCountedEchos().get() + "|"
							+ neighbours.size() + " neustart: " + election.getRestart().get());
		} else {
			if (!echo.containsKey(initiatorName)) {
				Attributes att = new Attributes(false);
				att.setInitiatorName(initiatorName);
				att.getRestart().set(true);
				att.setWakeupNeighbour(neighbour);
				echo.put(initiatorName, att);
			}
			Attributes actual = echo.get(initiatorName);
			actual.getCountedEchos().incrementAndGet();

			System.out
					.println(this + " received wakeup from " + neighbour + "counter: " + actual.getCountedEchos().get()
							+ "|" + neighbours.size() + " neustart: " + actual.getRestart().get());
		}

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
					for (Attributes att : new ArrayList<>(echo.values())) {
						restart(att, false);
					}
					synchronized (this) {
						while ((election.getCountedEchos().get() < neighbours.size() && !election.getRestart().get())
								&& (echo.isEmpty() || echo.values().stream().anyMatch(att -> (!att.getRestart().get()
										&& att.getCountedEchos().get() < neighbours.size())))) {
							try {
								wait((int) (1000 + Math.random() * 4000));
								if (initiator && !name.equals(election.getInitiatorName())
										&& (election.getInitiatorName() == null
												|| name.compareTo(election.getInitiatorName()) >= 0)) {
									System.out.println(this + " timed restart");
									resetAttribute();
								}
//								System.out.println(this + " innere schleife ");
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}

						actual = null;
						echo.values().stream().filter(att -> !(att.getCountedEchos().get() < neighbours.size()))
								.findFirst().ifPresent(att -> actual = att);
						// System.out.println(this + " vor restart");
					}
				} while ((election.getRestart().get() || election.getCountedEchos().get() < neighbours.size())
						&& !(echo.isEmpty() || !echo.values().stream().anyMatch(att -> att.getRestart().get())));
				synchronized (this) {
//					System.out.println(this + "nach while");
					boolean isElection = false;
					if (actual == null) {
						actual = election;
						isElection = true;
					}
					if (initiator && actual.getWakeupNeighbour() == null) {
						if (isElection) {
							System.out
									.println("Fertig: " + this + " wurde gewählt " + election.getCountedEchos().get());
							System.out.println(this+ ": "+election.getData());
							Attributes attributes = new Attributes(false);
							attributes.getRestart().set(true);
							attributes.setInitiatorName(election.getInitiatorName());
							echo.put(name, attributes);
							started.clear();
							election.getRestart().set(false);
							election.getCountedEchos().set(0);
							election.setData(null);
//							started = new HashMap<>();
							// election.setInitiatorName(null);
						} else {
							System.out.println("Fertig: " + actual.getData());
							if (actual.getData() != null) {
								System.out.println(
										"Ausgabenmenge: " + (((String) (actual.getData())).split(",").length + 1));
							}
							echo.remove(actual.getInitiatorName());
//							System.exit(0);
						}
					} else {
						System.out.println(this+" "+ actual);
						System.out.println(this + " " + actual.getWakeupNeighbour());
						actual.getWakeupNeighbour().echo(this,
								actual.getWakeupNeighbour() + "-" + this
										+ (actual.getData() != null ? "," + actual.getData() : ""),
								actual.getInitiatorName(), isElection);
						actual.getCountedEchos().set(0);
						actual.setData("");
						actual.getRestart().set(false);
//						actual.setWakeupNeighbour(null);
						// actual.setInitiatorName(null);
					}
				}

			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	public synchronized void echo(Node neighbour, Object data, String initiatorName, boolean isElection) {
		Attributes actual = isElection ? election : echo.get(initiatorName);
		System.out.println(actual + " " + isElection);
		if (!isElection || actual.getInitiatorName().equals(initiatorName)) {
			if (actual.getData() == null) {
				actual.setData(data);
			} else {
				actual.setData(data + "," + actual.getData());
			}
			System.out.println("echo von " + neighbour + " an " + this +" " + (actual.getCountedEchos().get()+1));
			actual.getCountedEchos().incrementAndGet();
			notifyAll();
		} else {
			System.out.println(this + " echo ging daneben " + actual.getInitiatorName() + "/" + initiatorName);
		}
	}

	Map<Node, String> started = new HashMap<>();
	
	private void restart(Attributes att, boolean isElection) {
		for (Node node : neighbours) {
			if (node != att.getWakeupNeighbour()) {
				if (!started.containsKey(node) || started.get(node).compareTo(att.getInitiatorName()) != 0) {
					att.getRestart().set(false);
//					System.out.println(this + "re-/start now oldvalue := " + started.get(node));
					node.wakeup(this, att.getInitiatorName(), isElection);
					started.put(node, att.getInitiatorName());
//					System.out.println(this + " re-/started node " + node);
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
	@Override
	public String toString() {
		return super.toString() + "(" + election.getInitiatorName() + ")";
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object arg0) {
		if (arg0 instanceof ElectionNode) {
			return this.hashCode() == arg0.hashCode();
		}
		return false;
	}
	private void resetAttribute() {
		election.getCountedEchos().set(0);
		election.setWakeupNeighbour(null);
		election.setData(null);
		election.getRestart().set(true);
		election.setInitiatorName(name);
	}
}
