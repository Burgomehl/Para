package node;

public interface IElectionNode extends Node{
	public void wakeup(Node neighbour, int strenght);
}
