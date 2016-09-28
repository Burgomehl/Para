package process;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import node.Attributes;
import node.Node;
import node.NodeAbstract;

public class ElectionNode2 extends NodeAbstract {

	private Queue<Attributes> allAttributes = new LinkedBlockingQueue<>();
	private Map<String, Attributes> echo = Collections.synchronizedMap(new HashMap<>());
	private Attributes election = null;
	private AtomicBoolean isElectStart = new AtomicBoolean(false);
	private AtomicBoolean isEchoStart = new AtomicBoolean(false);

	public ElectionNode2(String name, boolean initiator, CountDownLatch startLatch) {
		super(name, initiator, startLatch);
		if (initiator) {
			start();
			Attributes ell = new Attributes(true);
			ell.setInitiatorName(name);
			allAttributes.add(ell);
//			isElectStart.set(true);
		}
	}

	@Override
	public synchronized void wakeup(Node neighbour, String initiatorName, boolean isElection) {
		Attributes att = new Attributes(isElection);
		att.setInitiatorName(initiatorName);
		att.setWakeupNeighbour(neighbour);
		synchronized (allAttributes) {
			allAttributes.add(att);
		}
		if (State.NEW == this.getState()) {
			start();
		}
		notifyAll();
	}

	@Override
	public synchronized void echo(Node neighbour, Object data, String initiatorName, boolean isElection) {
		Attributes att = new Attributes(isElection);
		att.setInitiatorName(initiatorName);
		att.setWakeupNeighbour(neighbour);
		att.setData(data);
		synchronized (allAttributes) {
			allAttributes.add(att);
		}
		notifyAll();
	}

	@Override
	public void run() {
		Attributes restartEcho = null;
		while (true) {
			try {
				startLatch.await();
				if (isElectStart.get()) {
					System.out.println(this+" Election WakeupToAllNeighbours");
					synchronized (neighbours) {
						for (Node node : neighbours) {
							node.wakeup(this, election.getInitiatorName(), true);
						}
					}
					isElectStart.set(false);
				} else if (isEchoStart.get() && restartEcho != null) {
					System.out.println(this+" Echo WakeupToAllNeighbours");
					synchronized (neighbours) {
						for (Node node : neighbours) {
							node.wakeup(this, restartEcho.getInitiatorName(), false);
						}
					}
					isEchoStart.set(false);
				}

//				synchronized (allAttributes) {
					boolean restart = false;
					while (!restart) {
						if (allAttributes.isEmpty()) {
							synchronized (this) {
								wait();
							}
							
						}
						Attributes poll = allAttributes.poll();
						if (poll.isElection()) {
							if (election == null) {
								election = poll;
								restart= true;
								isElectStart.set(true);
								isEchoStart.set(false);
							} else {
								if (election.getInitiatorName().compareTo(poll.getInitiatorName()) == 0) {
									System.out.println(this+" election  is like "+poll.getWakeupNeighbour());
									election.getCountedEchos().incrementAndGet();
									if (poll.getData() != null) {
										System.out.println(this+" election war echo");
										election.setData(election.getData() + "," + poll.getData());
									} else {
										election.setData(this + "-" + poll.getWakeupNeighbour());
									}
								} else if (poll.getInitiatorName().compareTo(election.getInitiatorName()) > 0) {
									System.out.println(this+" Election restart node with new father "+poll.getWakeupNeighbour());
									election.setWakeupNeighbour(poll.getWakeupNeighbour());
									election.setInitiatorName(poll.getInitiatorName());
									election.getCountedEchos().set(1);
									restart = true;
									isElectStart.set(true);
									isEchoStart.set(false);
								}
							}
							if (!(election.getCountedEchos().get() < neighbours.size())) {
								if (election.getWakeupNeighbour() != null) {
									System.out.println(this + " will call echo from " + election.getWakeupNeighbour());
									election.getWakeupNeighbour().echo(this, election.getData(),
											election.getInitiatorName(), true);
								} else {
									System.out.println(this + ": Finished Election");
									this.wakeup(null, name, false);
								}
							}
						} else {
							if (!echo.containsKey(poll.getInitiatorName())) {
								echo.put(poll.getInitiatorName(), poll);
							} else {
								Attributes attributes = echo.get(poll.getInitiatorName());
								if (poll.getInitiatorName().compareTo(attributes.getInitiatorName()) == 0) {
									System.out.println(this+" echo updates already known neighbour "+ poll.getWakeupNeighbour());
									attributes.getCountedEchos().incrementAndGet();
									if (poll.getData() != null) {
										System.out.println(this+" echo is an echo of echo "+ poll.getWakeupNeighbour());
										attributes.setData(attributes.getData() + "," + poll.getData());
									} else {
										attributes.setData(this + "-" + poll.getWakeupNeighbour());
									}
								} else if (poll.getInitiatorName().compareTo(attributes.getInitiatorName()) > 0) {
									System.out.println(this+" nobody cares about the strength at echo");
//									attributes.setWakeupNeighbour(poll.getWakeupNeighbour());
//									attributes.setInitiatorName(poll.getInitiatorName());
//									attributes.getCountedEchos().set(1);
//									restart = true;
//									isElectStart.set(false);
//									isEchoStart.set(true);
								}
							}
							Attributes singleEcho = echo.get(poll.getInitiatorName());
							if (!(singleEcho.getCountedEchos().get() < neighbours.size())) {
								if (singleEcho.getWakeupNeighbour() != null) {
									System.out.println(this+" send echo in echo to "+ singleEcho.getWakeupNeighbour());
									singleEcho.getWakeupNeighbour().echo(this,
											(singleEcho.getData() + "," + singleEcho.getWakeupNeighbour() + "-" + this),
											singleEcho.getInitiatorName(), false);
								} else {
									System.out.println(this + ": Finished Echo: " + singleEcho.getData());
									System.out.println(
											"Knoten: " + (singleEcho.getData().toString().split(",").length + 1));
								}
							}
						}

					}
//				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}

		}
	}

}
