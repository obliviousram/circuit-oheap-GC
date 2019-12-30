package oheap;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import util.Utils;
import flexsc.CompEnv;
import flexsc.Flag;
import flexsc.Mode;
import flexsc.Party;
import gc.GCSignal;

public class TestCircuitOHeapReal {

	public static void main(String args[]) throws Exception {
		new TestCircuitOHeapReal().runThreads();
	}

	@Test
	public void runThreads() throws Exception {
		Flag.sw.flush();
		GenRunnable gen = new GenRunnable(12345, 12, 3, 32);
		EvaRunnable eva = new EvaRunnable("localhost", 12345);
		Thread tGen = new Thread(gen);
		Thread tEva = new Thread(eva);
		tGen.start();
		Thread.sleep(10);
		tEva.start();
		tGen.join();
		Flag.sw.print();
		System.out.println();
		TimeUnit.SECONDS.sleep(1);
	}

	final static int writeCount = 1 << 7;
	final static int readCount = (1 << 7);

	public TestCircuitOHeapReal() {
	}

	public static class GenRunnable extends network.Server implements Runnable {
		int port;
		int logN;
		int N;
		int capacity;
		int dataSize;
		int logCutoff;

		GenRunnable(int port, int logN, int capacity, int dataSize) {
			this.port = port;
			this.logN = logN;
			this.N = 1 << logN;
			this.dataSize = dataSize;
			this.capacity = capacity;
		}

		public void run() {
			try {
				listen(port);

				os.write(logN);
				os.write(capacity);
				os.write(dataSize);
				os.flush();

				System.out.println("\nlogN capacity dataSize");
				System.out.println(logN + " " + capacity + " " + dataSize);

				@SuppressWarnings("unchecked")
				CompEnv<GCSignal> env = CompEnv.getEnv(Mode.REAL, Party.Alice, this);
				System.out.println("Server constructing CircuitOHeap instance...");
				CircuitOHeap<GCSignal> client = new CircuitOHeap<GCSignal>(
						env, N, dataSize, capacity, 80, false);
				System.out.println("Server finished constructing CircuitOHeap instance.");

				for (int i = 0; i < writeCount; ++i) {
					int element = i % N;

					Flag.sw.ands = 0;
					GCSignal[] scKey = client.lib.toSignals(element, 64);
					GCSignal[] scData = client.env.inputOfAlice(Utils.fromInt(element*2, dataSize));
					os.flush();
					Flag.sw.startTotal();
					client.insert(scKey, scData);
					double t = Flag.sw.stopTotal();
					System.out.println(i + " " + Flag.sw.ands + " " + t / 1000000000.0
							+ " " + Flag.sw.ands / t * 1000);
					if(i != 0) Flag.sw.addCounter();
					
					os.write(0);
					os.flush();

//					Runtime rt = Runtime.getRuntime();
//					double usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024.0 / 1024.0;
//					System.out.println("mem: " + usedMB);
				}

				for (int i = 0; i < readCount; ++i) {
					int element = i % N;
					Block<GCSignal> blk = client.extractMin();
					long key = Utils.toLong(client.env.outputToAlice(blk.key));
					int data = Utils.toInt(client.env.outputToAlice(blk.data));
					
					os.write(0);
					os.flush();
					
					if (key != element) {
						System.out.printf("Error! Keys %d and %d don't match!\n", key, element);
						Assert.fail();
					} else if (data != element*2) {
						System.out.printf("Error! Key %d doesn't match data %d\n", key, data);
						Assert.fail();
					}
					System.out.println(i);
				}

				os.flush();

				disconnect();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	public static class EvaRunnable extends network.Client implements Runnable {

		String host;
		int port;

		EvaRunnable(String host, int port) {
			this.host = host;
			this.port = port;
		}

		public void run() {
			try {
				connect(host, port);

				int logN = is.read();
				int capacity = is.read();
				int dataSize = is.read();

				int N = 1 << logN;
				System.out
						.println("\nlogN capacity dataSize");
				System.out.println(logN + " " + capacity + " " + dataSize);

				@SuppressWarnings("unchecked")
				CompEnv<GCSignal> env = CompEnv.getEnv(Mode.REAL, Party.Bob, this);
				System.out.println("Client constructing CircuitOHeap instance...");
				CircuitOHeap<GCSignal> server = new CircuitOHeap<GCSignal>(
						env, N, dataSize, capacity, 80, false);
				System.out.println("Client finished constructing CircuitOHeap instance.");
				for (int i = 0; i < writeCount; ++i) {
					int element = i % N;
					GCSignal[] scKey = server.lib.toSignals(element, 64);
					GCSignal[] scData = server.env.inputOfAlice(new boolean[dataSize]);
					Flag.sw.startTotal();
					server.insert(scKey, scData);
					Flag.sw.stopTotal();
					if(i != 0) Flag.sw.addCounter();
//					printStatistic();
					
					is.read();
				}

				for (int i = 0; i < readCount; ++i) {
					Block<GCSignal> blk = server.extractMin();
					server.env.outputToAlice(blk.key);
					server.env.outputToAlice(blk.data);
					
					is.read();
				}

				disconnect();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
}
