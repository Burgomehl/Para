package test;

import java.util.Random;

import Start.Start;
import process.ElectionNode;
import process.ElectionNode2;

public class Testing {
	static Random r = new Random();

	public static void main(String[] args) throws InterruptedException {
		Start.ellipse(4,(latch,i)-> new ElectionNode2("Node"+i,(i==0?true: r.nextBoolean()), latch));
	}

}
