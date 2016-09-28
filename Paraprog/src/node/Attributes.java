package node;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Attributes {
	protected AtomicInteger countedEchos = new AtomicInteger(0);
	protected Node wakeupNeighbour;
	protected Object data;

	private String initiatorName;
	private AtomicBoolean restart = new AtomicBoolean(false);
	private boolean isElection;
	
	public Attributes(boolean isElection) {
		this.isElection= isElection;
	}

	public AtomicInteger getCountedEchos() {
		return countedEchos;
	}

	public void setCountedEchos(AtomicInteger countedEchos) {
		this.countedEchos = countedEchos;
	}

	public Node getWakeupNeighbour() {
		System.out.println("wakeupneighbour "+wakeupNeighbour+" get echo --> initiatorname "+initiatorName +" last "+ this.initiatorName);
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

	public synchronized String getInitiatorName() {
//		System.out.println("get initiatorname "+initiatorName);
		return initiatorName;
	}

	public synchronized void setInitiatorName(String initiatorName) {
		System.out.println("set initiatorname "+initiatorName +" last "+ this.initiatorName);
		this.initiatorName = initiatorName;
	}
	
	@Override
	public String toString() {
		return (isElection?"ElectionAttribute: ":"EchoAttribute: ") + initiatorName+" counted "+ countedEchos;
	}

	public boolean isElection() {
		return isElection;
	}
}