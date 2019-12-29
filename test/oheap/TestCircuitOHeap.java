package oheap;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.PriorityQueue;

import org.junit.Assert;
import org.junit.Test;

import util.Utils;
import flexsc.CompEnv;
import flexsc.Mode;
import flexsc.Party;
import oheap.Block;
import oheap.CircuitOHeap;

//import gc.Boolean;

public class TestCircuitOHeap {
	final int N = 1 << 6;
	final int capacity = 3;
	int dataSize = 32;
	
	PriorityQueue<Long> regularHeap = new PriorityQueue<>();
	int[] ops = new int[1000];
	
	public static void main(String[] args) throws Exception {
		new TestCircuitOHeap().runThreads();
	}
	
	public TestCircuitOHeap() {
		SecureRandom rng = new SecureRandom();
		
		// run a large number of random operations on both the OHeap and a regular heap, comparing them
		// 0 = extractMin
		// 1,2 = insert
		// bias insert over extractMin so the heap eventually fills up
		// but never dequeue when empty or enqueue when full
		int imbalance = 0;
		for (int i = 0; i < ops.length; i++) {
			if (imbalance == 0) {
				ops[i] = 1;
				imbalance++;
			} else if (imbalance == N) {
				ops[i] = 0;
				imbalance--;
			} else {
				ops[i] = rng.nextInt(3);
				if (ops[i] == 0) { imbalance--; } else { imbalance++; }
			}
		}
	}

	SecureRandom rng = new SecureRandom();
	boolean breaksignal = false;
	
	class GenRunnable extends network.Server implements Runnable {
		int port;

		GenRunnable(int port) {
			this.port = port;
		}

		public int[][] idens;
		public boolean[][] du;
		public int[] stash;

		public void run() {
			try {
				listen(port);

				@SuppressWarnings("unchecked")
				CompEnv<Boolean> env = CompEnv.getEnv(Mode.VERIFY, Party.Alice, this);
				CircuitOHeap<Boolean> client = new CircuitOHeap<Boolean>(env, N,
						dataSize, capacity, 80, false);
				System.out.println("logN:" + client.logN + ", N:" + client.N);

				for (int i = 0; i < ops.length; ++i) {
					if (ops[i] != 0) {
						long key = rng.nextInt(1000000);
						Boolean[] scKey = client.env.inputOfAlice(Utils.fromLong(
								key, Block.LENGTH_OF_KEY));
						Boolean[] scData = client.env.inputOfAlice(Utils.fromLong(
								2*key + 1, client.lengthOfData));
						client.insert(scKey, scData);
						regularHeap.add(key);
	
						os.write(0);
						os.flush();
	
						//Runtime rt = Runtime.getRuntime();
						//double usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024.0 / 1024.0;
						//System.out.println("mem: " + usedMB);
						
						System.out.printf("t=%d: Inserted %d\n", i, key);
					} else {
						Block<Boolean> blk = client.extractMin();
					    long key = Utils.toLong(client.env.outputToAlice(blk.key));
						int data = Utils.toInt(client.env.outputToAlice(blk.data));
						long regularKey = regularHeap.poll();
						
						os.write(0);
						os.flush();
						
						if (key != regularKey) {
							System.out.printf("Error! Keys %d and %d don't match!\n", key, regularKey);
							Assert.fail();
						} else {
							System.out.printf("t=%d: %d and %d match\n", i, key, regularKey);
						}
						if (2*key + 1 != data) {
							System.out.printf("Error! Key %d doesn't match data %d\n", key, data);
							Assert.fail();
						}
					}
				}

				idens = new int[client.tree.length][];
				du = new boolean[client.tree.length][];

				for (int j = 1; j < client.tree.length; ++j) {
					idens[j] = new int[client.tree[j].length];
					for (int i = 0; i < client.tree[j].length; ++i)
						idens[j][i] = (int) client.tree[j][i].iden;
				}

				for (int j = 1; j < client.tree.length; ++j) {
					du[j] = new boolean[client.tree[j].length];
					for (int i = 0; i < client.tree[j].length; ++i)
						du[j][i] = client.tree[j][i].isDummy;
				}

				stash = new int[client.queue.length];
				for (int j = 0; j < client.queue.length; ++j)
					stash[j] = (int) client.queue[j].iden;

				os.flush();

				disconnect();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	class EvaRunnable extends network.Client implements Runnable {
		String host;
		int port;
		public int[][] idens;
		public boolean[][] du;
		public int[] stash;

		EvaRunnable(String host, int port) {
			this.host = host;
			this.port = port;
		}

		public void run() {
			try {
				connect(host, port);

				@SuppressWarnings("unchecked")
				CompEnv<Boolean> env = CompEnv.getEnv(Mode.VERIFY, Party.Bob, this);
				CircuitOHeap<Boolean> server = new CircuitOHeap<Boolean>(env, N,
						dataSize, capacity, 80, false);

				for (int i = 0; i < ops.length; ++i) {
					if (ops[i] != 0) {
						Boolean[] scKey = server.env
								.inputOfAlice(new boolean[Block.LENGTH_OF_KEY]);
						Boolean[] scData = server.env
								.inputOfAlice(new boolean[server.lengthOfData]);
						
						server.insert(scKey, scData);
					} else {
						Block<Boolean> blk = server.extractMin();
						server.env.outputToAlice(blk.key);
						server.env.outputToAlice(blk.data);
					}
					is.read();
				}

				idens = new int[server.tree.length][];
				du = new boolean[server.tree.length][];
				for (int j = 1; j < server.tree.length; ++j) {
					idens[j] = new int[server.tree[j].length];
					for (int i = 0; i < server.tree[j].length; ++i)
						idens[j][i] = (int) server.tree[j][i].iden;
				}

				for (int j = 1; j < server.tree.length; ++j) {
					du[j] = new boolean[server.tree[j].length];
					for (int i = 0; i < server.tree[j].length; ++i)
						du[j][i] = server.tree[j][i].isDummy;
				}

				stash = new int[server.queue.length];
				for (int j = 0; j < server.queue.length; ++j)
					stash[j] = (int) server.queue[j].iden;
				os.flush();

				disconnect();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	GenRunnable gen = new GenRunnable(1234);
	EvaRunnable eva = new EvaRunnable("localhost", 1234);

	@Test
	public void runThreads() throws Exception {
		Thread tGen = new Thread(gen);
		Thread tEva = new Thread(eva);
		tGen.start();
		Thread.sleep(10);
		tEva.start();
		tGen.join();
		printTree(gen, eva);
		System.out.println(Arrays.toString(xor(gen.stash, eva.stash)));
		System.out.print("\n");

		System.out.println();
	}

	public void printTree(GenRunnable gen, EvaRunnable eva) {
		int k = 1;
		int i = 1;
		for (int j = 1; j < gen.idens.length; ++j) {
			System.out.print("[");
			int[] a = xor(gen.idens[j], eva.idens[j]);
			boolean[] bb = xor(gen.du[j], eva.du[j]);
			for (int p = 0; p < eva.idens[j].length; ++p)
				if (bb[p])
					System.out.print("d,");
				else
					System.out.print(a[p] + ",");
			System.out.print("]");
			if (i == k) {
				k = k * 2;
				i = 0;
				System.out.print("\n");
			}
			++i;
		}
		System.out.print("\n");
	}

	public boolean[] xor(boolean[] a, boolean[] b) {
		boolean[] res = new boolean[a.length];
		for (int i = 0; i < res.length; ++i)
			res[i] = a[i] ^ b[i];
		return res;

	}

	public int[] xor(int[] a, int[] b) {
		int[] res = new int[a.length];
		for (int i = 0; i < res.length; ++i)
			res[i] = a[i] ^ b[i];
		return res;

	}

}
