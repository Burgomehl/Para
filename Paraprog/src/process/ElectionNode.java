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
			synchronized (election) {
				if (election.getInitiatorName() == null || initiatorName.compareTo(election.getInitiatorName()) > 0) {
					election.getRestart().set(true);
					election.setInitiatorName(initiatorName);
					election.setData(null);
					election.getCountedEchos().set(0);
					election.setWakeupNeighbour(neighbour);
				}

				if (initiatorName.equals(election.getInitiatorName())) {
					System.out
							.println(this + " will increment counter in wakeup from " + election.getCountedEchos().get());
					election.getCountedEchos().incrementAndGet();
					System.out
							.println(this + " has incremented counter in wakeup to " + election.getCountedEchos().get());
				}
				System.out.println(this + ": " + initiatorName.equals(election.getInitiatorName()) + " passedName:"
						+ initiatorName + "/ currentName:" + election.getInitiatorName());
				
				System.out.println(this + " received wakeup from " + neighbour + "counter: " + election.getCountedEchos().get()
						+ "|" + neighbours.size() + " neustart: " + election.getRestart().get());
			}
		} else {
			System.out.println(this + " echo wakeup");
			if (!echo.containsKey(initiatorName)) {
				System.out.println(this + " ich bin drin");
				Attributes att = new Attributes(false);
				att.setInitiatorName(initiatorName);
				att.getRestart().set(true);
				att.setWakeupNeighbour(neighbour);
				echo.put(initiatorName, att);
			}
			System.out.println(this + " goon echo");
			Attributes actual = echo.get(initiatorName);
			actual.getCountedEchos().incrementAndGet();
			
			System.out.println(this + " received wakeup from " + neighbour + "counter: " + actual.getCountedEchos().get()
					+ "|" + neighbours.size() + " neustart: " + actual.getRestart().get());
		}
//		System.out.println(this + ": Wakeup actual: " + actual);

		
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
									election.getCountedEchos().set(0);
									election.setWakeupNeighbour(null);
									election.setData(null);
									election.getRestart().set(true);
									election.setInitiatorName(name);
								} else if (initiator) {
									System.out.println("name compare:" + !name.equals(election.getInitiatorName()) + "\n" +
														"strength compare:" + (election.getInitiatorName() == null
														|| name.compareTo(election.getInitiatorName()) >= 0));
								}

//								System.out.println(this + ":" + actual.getCountedEchos().get() + "/" + neighbours.size()
//										+ " Initiator: " + actual.getInitiatorName());
//								System.out.println(
//										this + " -> set actual to " + actual + " with a echo size  of " + echo.size());
								echo.forEach((key, att) -> System.out.println(this + "echo content " + att));
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						actual = null;
						System.out.println(
								this + ": --counted-->" + (election.getCountedEchos().get() < neighbours.size()));
						System.out.println(this + ": --restart-->" + (!election.getRestart().get()));
						echo.values().stream().filter(att -> !(att.getCountedEchos().get() < neighbours.size()))
								.findFirst().ifPresent(att -> actual = att);
//						System.out
//								.println(this + " -> set actual to " + actual + " with a echo size  of " + echo.size());
						echo.forEach((key, att) -> System.out.println(this + "echo content " + att));
						System.out.println((election.getRestart().get() + " || "
								+ (election.getCountedEchos().get() < neighbours.size())) + " || " + (echo.isEmpty())
								+ " || " + (echo.values().stream().anyMatch(att -> att.getRestart().get())));
					}
				} while ((election.getRestart().get() || election.getCountedEchos().get() < neighbours.size())
						&& !(echo.isEmpty() || !echo.values().stream().anyMatch(att -> att.getRestart().get())));
				synchronized (this) {
					boolean isElection = false;
					if (actual == null) {
						actual = election;
						isElection = true;
					}
					System.out.println("after while -> " + actual);
					if (initiator && actual.getWakeupNeighbour() == null) {
						if (isElection) {
							System.out.println("Fertig: " + this + " wurde gew�hlt " + election.getCountedEchos().get());
							System.out.println(echo.values().stream()
									.anyMatch(att -> att.getCountedEchos().get() < neighbours.size()));
							election.getRestart().set(false);
							Attributes attributes = new Attributes(false);
							attributes.getRestart().set(true);
							attributes.setInitiatorName(election.getInitiatorName());
							echo.put(election.getInitiatorName(), attributes);
							election.getCountedEchos().set(0);
							election.setData(null);
							
							election.setInitiatorName(null);
						} else {
							System.out.println("Fertig: " + actual.getData());
							if (actual.getData() != null) {
								System.out.println(
										"Ausgabenmenge: " + (((String) (actual.getData())).split(",").length + 1));
							}
							echo.remove(actual.getInitiatorName());
						}
					} else {
						System.out.println("Echo starting: " + this + " wurde gew�hlt " + actual.getCountedEchos().get()
								+ "/" + neighbours.size());
						System.out.println(this + " going to echo:  " + echo.values().stream()
								.anyMatch(att -> att.getCountedEchos().get() < neighbours.size()));
						System.out.println(this + " " + actual.getWakeupNeighbour());
						actual.getWakeupNeighbour().echo(this,
								actual.getWakeupNeighbour() + "-" + this
										+ (actual.getData() != null ? "," + actual.getData() : ""),
								actual.getInitiatorName(), isElection);
						actual.getCountedEchos().set(0);
						actual.setData("");
						actual.getRestart().set(false);
						actual.setWakeupNeighbour(null);
						actual.setInitiatorName(null);
					}

					// if (actual.getCountedEchos().get() >= neighbours.size())
					// {
					// System.out.println(
					// this + " reset counter (current value: " +
					// actual.getCountedEchos().get() + ")");
					// actual.getCountedEchos().set(0);
					// } else {
					// System.out.println(this + " has a problem");
					// }
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
				String initiatorName;
				synchronized (election) {
					if (att.getRestart().get()) {
						break;
					}
					initiatorName = new String(att.getInitiatorName());
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
		Attributes actual = isElection ? election : echo.get(initiatorName);
		System.out.println(actual);
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
