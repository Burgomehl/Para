import java.util.concurrent.CountDownLatch;

public class Nodes extends NodeAbstract implements Runnable{
	private int countedEchos = 0;

	public Nodes(String name, boolean initiator, CountDownLatch startLatch) {
		super(name, initiator, startLatch);
	}

	@Override
	public void hello(Node neighbour) {
		if(!neighbours.contains(neighbour)){
			neighbours.add(neighbour);
		}
	}

	@Override
	public void wakeup(Node neighbour) {
		if(neighbours.size()<=1){
			neighbour.echo(this, null);
		}else{
			//wait for echo of all neighbours
			if(countedEchos >= neighbours.size()){
				neighbour.echo(this, null);
			}
		}
	}

	@Override
	public void echo(Node neighbour, Object data) {
		//wait for all neightbours sending an echo
		++countedEchos;
	}

	@Override
	public void setupNeighbours(Node... neighbours) {
		for (Node node : neighbours) {
			node.hello(this);
		}
	}

}
