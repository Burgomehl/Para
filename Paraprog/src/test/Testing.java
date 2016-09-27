package test;

import java.util.Random;

import Start.Start;
import process.ElectionNode;

public class Testing {
	static Random r = new Random();

	public static void main(String[] args) throws InterruptedException {
		Start.ellipse(10,(latch,i)-> new ElectionNode("Node"+i,(i==0?true: r.nextBoolean()), latch, i));
	}

}
