package oheap;

import java.security.SecureRandom;

import org.junit.Test;

import util.Utils;
import flexsc.CompEnv;
import flexsc.Mode;
import flexsc.PMCompEnv;
import flexsc.Party;


public class CountCircuitOHeap {
	int N;
	int logN;
	final int capacity = 3;
	int writecount = 1;
	int dataSize;
	
	public static void main(String[] args) throws Exception {
		new CountCircuitOHeap().runThreads();
	}

	public CountCircuitOHeap() {
	}

	SecureRandom rng = new SecureRandom();
	boolean breaksignal = false;

	class GenRunnable extends network.Server implements Runnable {
		int port;

		GenRunnable(int port) {
			this.port = port;
		}


		public void run() {
			try {
				listen(port);

				@SuppressWarnings("unchecked")
				CompEnv<Boolean> env = CompEnv.getEnv(Mode.COUNT, Party.Alice, this);
				CircuitOHeap<Boolean> client = new CircuitOHeap<Boolean>(env, N,
						dataSize, capacity, 80, false);
				System.out.println("logN:" + client.logN + ", N:" + client.N);

				for (int i = 0; i < writecount; ++i) {
					Boolean[] scData = client.env.inputOfAlice(Utils.fromInt(
							1, client.lengthOfData));
					PMCompEnv pm = (PMCompEnv) env;
					pm.statistic.flush();
					client.insert(client.lib.toSignals(1, 64), scData);

					System.out.println(pm.statistic.andGate);
					os.flush();
					
				}

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


		EvaRunnable(String host, int port) {
			this.host = host;
			this.port = port;
		}

		public void run() {
			try {
				connect(host, port);

				@SuppressWarnings("unchecked")
				CompEnv<Boolean> env = CompEnv.getEnv(Mode.COUNT, Party.Bob, this);
				CircuitOHeap<Boolean> server = new CircuitOHeap<Boolean>(env, N,
						dataSize, capacity, 80, false);

				for (int i = 0; i < writecount; ++i) {
					Boolean[] scData = server.env
							.inputOfAlice(new boolean[server.lengthOfData]);

					server.insert(server.lib.toSignals(1, 64), scData);
				}


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
		this.logN = 17;
		this.N = 1<<logN;
		this.dataSize = 64*8;//i+i+i+i+32+32;
		Thread tGen = new Thread(gen);
		Thread tEva = new Thread(eva);
		tGen.start();
		Thread.sleep(10);
		tEva.start();
		tGen.join();
//		System.out.println();
	}
}