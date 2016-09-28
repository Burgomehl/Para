package node;


import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * Abstract implementation of the Node interface.
 */
public abstract class NodeAbstract extends Thread implements Node {

	/** Name of this node */
	protected final String name;

	/** Is this node the initiator of the echo algorithm? */
	protected final boolean initiator;

	/**
	 * Collection of known neighbours of this node; only the methods of the
	 * neighbours in this collection can be called.
	 */
	protected final Set<Node> neighbours = new HashSet<Node>();

	/** CountDownLatch to synchronize threads after calls of hello method */
	protected final CountDownLatch startLatch;

	/** Abstract constructor of a node */
	public NodeAbstract(String name, boolean initiator,
			CountDownLatch startLatch) {
		super(name);
		this.name = name;
		this.initiator = initiator;
		this.startLatch = startLatch;
	}

	/**
	 * Method to setup the list of initially known neighbours; the setup must be
	 * complete in all nodes before any echo thread is started!
	 * 
	 * Be aware that the neighbour relationship is symmetric (if node "a" has
	 * node "b" as its neighbour, also node "b" must have node "a" as its
	 * neighbour)!
	 */
	public void setupNeighbours(Node... neighbours) {
		if (neighbours != null) {
			for (Node node : neighbours) {
				this.neighbours.add(node);
				node.hello(this);
			}
		}
//		System.out.println(this + ": setupneighbours finished with " + (neighbours != null ? neighbours.length : "0")
//				+ " neighbours");
		startLatch.countDown();
	}

	/** Utility method to print this node in a readable way */
	@Override
	public String toString() {
		return name;
	}
	
	@Override
	public void hello(Node neighbour) {
//		System.out.println(neighbour+" sayed hello to "+this);
		neighbours.add(neighbour);
	}
}
