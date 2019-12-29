// Copyright (C) 2014 by Xiao Shaun Wang <wangxiao@cs.umd.edu>
package oheap;

import java.util.Arrays;

import flexsc.CompEnv;
import flexsc.Party;
import util.Utils;

public class CircuitOHeap<T> extends TreeBasedOHeapParty<T> {
	public CircuitOHeapLib<T> lib;
	Block<T>[] scQueue;
	public PlainBlock[] queue;
	public int queueCapacity;
	
	T[] timestamp;  // number of ops performed so far
	boolean typeHidingSecurity;
	
	boolean[] posToPath(int pos) {
		boolean[] res = new boolean[logN];
		for (int i = res.length - 1; i >= 0; --i) {
			res[i] = (pos & 1) == 1;
			pos >>= 1;
		}
		return res;
	}
	
	boolean[] randomPath() {
		return Utils.tobooleanArray((Boolean[])lib.randBools(logN));
	}

	public CircuitOHeap(CompEnv<T> env, int N, int dataSize, int cap, int sp, boolean typeHidingSecurity) {
		super(env, N, dataSize, cap);
		lib = new CircuitOHeapLib<T>(lengthOfIden, lengthOfPos, lengthOfData,
				logN, capacity, env);
		queueCapacity = 30;
		queue = new PlainBlock[queueCapacity];

		for (int i = 0; i < queue.length; ++i)
			queue[i] = getDummyBlock(p == Party.Alice);

		scQueue = prepareBlocks(queue, queue);
		
		timestamp = lib.zeros(lengthOfIden);
		
		this.typeHidingSecurity = typeHidingSecurity;
	}

	public CircuitOHeap(CompEnv<T> env, int N, int dataSize) {
		super(env, N, dataSize, 3);
		lib = new CircuitOHeapLib<T>(lengthOfIden, lengthOfPos, lengthOfData,
				logN, capacity, env);
		queueCapacity = 30;
		queue = new PlainBlock[queueCapacity];

		for (int i = 0; i < queue.length; ++i)
			queue[i] = getDummyBlock(p == Party.Alice);

		scQueue = prepareBlocks(queue, queue);

		timestamp = lib.zeros(lengthOfIden);
	}

	protected void ControlEviction() {
		boolean[] randomPathOne = randomPath();
		boolean[] randomPathTwo = randomPath();
		// force the paths to be non-overlapping
		randomPathOne[logN - 1] = true;
		randomPathTwo[logN - 1] = false;
		
		flushOneTime(randomPathOne);
		flushOneTime(randomPathTwo);
	}
	
	public void flushOneTime(boolean[] pos) {
		PlainBlock[][] blocks = getPath(pos);
		Block<T>[][] scPath = preparePath(blocks, blocks);

		lib.flush(scPath, pos, scQueue);

		blocks = preparePlainPath(scPath);
		putPath(blocks, pos);
		
		updateSubtreeMins(pos, scPath);
	}

	int initalValue = 0;
	public void setInitialValue(int intial) {
		initalValue = intial;
	}
	
	private Block<T> dummyBlock() {
		return new Block<T>(lib.toSignals(initalValue, lengthOfIden),
			lib.toSignals(initalValue, lengthOfPos),
			lib.toSignals(initalValue, Block.LENGTH_OF_KEY),
			lib.toSignals(initalValue, lengthOfData), lib.SIGNAL_ONE);
	}
	
	public Block<T> readAndRemove(T[] scIden, boolean[] pos,
			boolean RandomWhenNotFound) {
		PlainBlock[][] blocks = getPath(pos);
		Block<T>[][] scPath = preparePath(blocks, blocks);

		Block<T> res = lib.readAndRemove(scPath, scIden);
		Block<T> res2 = lib.readAndRemove(scQueue, scIden);
		res = lib.mux(res, res2, res.isDummy);

		blocks = preparePlainPath(scPath);
		putPath(blocks, pos);

		if (RandomWhenNotFound) {
			PlainBlock b = randomBlock();
			Block<T> scb = inputBlockOfClient(b);
			Block<T> finalRes = lib.mux(res, scb, res.isDummy);

			return finalRes;
		} else {
			return lib.mux(res, dummyBlock(), res.isDummy);
		}
	}

	public Block<T> putBack(T[] scIden, T[] scNewPos, T[] scKey, T[] scData) {
		Block<T> b = new Block<T>(scIden, scNewPos, scKey, scData, lib.SIGNAL_ZERO);
		lib.add(scQueue, b);

		env.flush();
		ControlEviction();
		
		return b;
	}
	
	public Block<T> findMin() {
		Block<T> ret = onlyFindMin();
		
		if (typeHidingSecurity) {
			onlyDelete(lib.toSignals(initalValue, lengthOfIden),
					lib.toSignals(initalValue, lengthOfPos), lib.SIGNAL_ONE);
			onlyInsert(lib.toSignals(initalValue, Block.LENGTH_OF_KEY),
					lib.toSignals(initalValue, lengthOfData), lib.SIGNAL_ONE);
		}
		
		return ret;
	}
	
	public Block<T> delete(T[] scIden, T[] pos) {
		if (typeHidingSecurity) {
			onlyFindMin();
		}
		
		Block<T> ret = onlyDelete(scIden, pos, lib.SIGNAL_ZERO);
		
		if (typeHidingSecurity) {
			onlyInsert(lib.toSignals(initalValue, Block.LENGTH_OF_KEY),
					lib.toSignals(initalValue, lengthOfData), lib.SIGNAL_ONE);
		}
		
		timestamp = lib.incrementByOne(timestamp);
		return ret;
	}
	
	/** Returns a copy of the inserted block (so caller will know its pos and iden) */
	public Block<T> insert(T[] scKey, T[] scData) {
		if (typeHidingSecurity) {
			onlyFindMin();
			onlyDelete(lib.toSignals(initalValue, lengthOfIden),
					lib.toSignals(initalValue, lengthOfPos), lib.SIGNAL_ONE);
		}
		
		Block<T> ret = onlyInsert(scKey, scData, lib.SIGNAL_ZERO);
		
		timestamp = lib.incrementByOne(timestamp);
		return ret;
	}
	
	public Block<T> extractMin() {
		Block<T> minBlock = onlyFindMin();
		Block<T> ret = onlyDelete(minBlock.iden, minBlock.pos, lib.SIGNAL_ZERO);
		
		if (typeHidingSecurity) {
			onlyInsert(lib.toSignals(initalValue, Block.LENGTH_OF_KEY),
					lib.toSignals(initalValue, lengthOfData), lib.SIGNAL_ONE);
		}
		
		timestamp = lib.incrementByOne(timestamp);
		return ret;
	}
	
	public Block<T> decreaseKey(T[] scIden, T[] pos) {
		if (typeHidingSecurity) {
			onlyFindMin();
		}
		
		Block<T> oldBlock = onlyDelete(scIden, pos, lib.SIGNAL_ZERO);
		Block<T> ret = onlyInsert(lib.decrementByOne(oldBlock.key), oldBlock.data, lib.SIGNAL_ZERO);
		
		timestamp = lib.incrementByOne(timestamp);
		return ret;
	}
	
	public Block<T> increaseKey(T[] scIden, T[] pos) {
		if (typeHidingSecurity) {
			onlyFindMin();
		}
		
		Block<T> oldBlock = delete(scIden, pos);
		Block<T> ret = insert(lib.incrementByOne(oldBlock.key), oldBlock.data);
		
		timestamp = lib.incrementByOne(timestamp);
		return ret;
	}
	
	private Block<T> onlyFindMin() {
		Block<T> min = subtree_mins[1];
		for (Block<T> b : scQueue) {
			min = blockMin(min, b);
		}
		return min;
	}
	
	private Block<T> onlyDelete(T[] scIden, T[] pos, T dummy) {
		Block<T> ret = conditionalReadAndRemove(scIden, pos, lib.not(dummy));
		flushOneTime(lib.declassifyToBoth(pos));
		return ret;
	}
	
	private Block<T> onlyInsert(T[] scKey, T[] scData, T dummy) {
		T[] randLeaf = lib.randBools(lengthOfPos);
		Block<T> ret = conditionalPutBack(timestamp, randLeaf, scKey, scData, lib.not(dummy));
		
		return ret;
	}

	public Block<T> conditionalReadAndRemove(T[] scIden, T[] pos, T condition) {
		// Utils.print(env, "rar: iden:", scIden, pos, condition);
		scIden = Arrays.copyOf(scIden, lengthOfIden);
		T[] scPos = Arrays.copyOf(pos, lengthOfPos);
		T[] randbools = lib.randBools(scPos.length);
		T[] posToUse = lib.mux(randbools, scPos, condition);

		boolean[] path = lib.declassifyToBoth(posToUse);

		PlainBlock[][] blocks = getPath(path);
		Block<T>[][] scPath = preparePath(blocks, blocks);

		Block<T> res = lib.conditionalReadAndRemove(scPath, scIden, condition);
		Block<T> res2 = lib
				.conditionalReadAndRemove(scQueue, scIden, condition);
		res = lib.mux(res, res2, res.isDummy);

		blocks = preparePlainPath(scPath);
		putPath(blocks, path);
		env.flush();
		return lib.mux(res, dummyBlock(), res.isDummy);
	}

	public Block<T> conditionalPutBack(T[] scIden, T[] scNewPos, T[] scKey, T[] scData,
			T condition) {
		env.flush();
		scIden = Arrays.copyOf(scIden, lengthOfIden);

		Block<T> b = new Block<T>(scIden, scNewPos, scKey, scData, lib.SIGNAL_ZERO);
		lib.conditionalAdd(scQueue, b, condition);
		env.flush();
		ControlEviction();
		
		return b;
	}
}
