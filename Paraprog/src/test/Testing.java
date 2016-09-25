package test;

import java.util.Random;

import Start.Start;
import process.ElectionNode;

public class Testing {
	static Random r = new Random();

	public static void main(String[] args) throws InterruptedException {
		Start.tree(3000,(latch,i)-> new ElectionNode("Node"+i, r.nextBoolean(), latch, i));
	}

}
