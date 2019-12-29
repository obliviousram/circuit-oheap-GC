// Copyright (C) 2014 by Xiao Shaun Wang <wangxiao@cs.umd.edu>
package oheap;

import flexsc.CompEnv;
import flexsc.Mode;
import flexsc.Party;

public abstract class TreeBasedOHeapParty<T> extends OHeapParty<T> {
	public PlainBlock[][] tree;
	protected int capacity;
	public Block<T>[] subtree_mins;

	public TreeBasedOHeapParty(CompEnv<T> env, int N, int dataSize, int capacity) {
		super(env, N, dataSize);
		this.capacity = capacity;

		if (env.mode != Mode.COUNT) {
			tree = new PlainBlock[this.N][capacity];

			PlainBlock b = getDummyBlock(p == Party.Alice);

			for (int i = 0; i < this.N; ++i)
				for (int j = 0; j < capacity; ++j)
					tree[i][j] = b;
		}
		
		PlainBlock[] temp_plain_subtree_mins = new PlainBlock[this.N];
		for (int i = 0; i < this.N; i++) {
			temp_plain_subtree_mins[i] = getDummyBlock(p == Party.Alice);
		}
		subtree_mins = prepareBlocks(temp_plain_subtree_mins, temp_plain_subtree_mins);
	}
	
	protected Block<T> blockMin(Block<T> first, Block<T> second) {
		T firstKeyLess = lib.leq(first.key, second.key);
		T keysTiedFirstTimestampLess = lib.and(lib.eq(first.key, second.key), lib.leq(first.iden, second.iden));
		T firstLeqSecond = lib.or(firstKeyLess, keysTiedFirstTimestampLess);
		T pickFirst = lib.and(lib.or(firstLeqSecond, second.isDummy), lib.not(first.isDummy));
		
		return lib.mux(second, first, pickFirst);
	}
	
	/**
	 * Given a path specified by `path` and a desired level, return the
	 * corresponding index for `this.tree`.
	 */
	private int P(boolean[] path, int level) {
		if (level == 0) return 1;
		
		int ret = 1;
		for (int i = 1; i <= level; i++) {
			ret *= 2;
			if (path[lengthOfPos - i]) {
				ret++;
			}
		}
		return ret;
	}
	
	protected void updateSubtreeMins(boolean[] path, Block<T>[][] scPath) {
		for (int l = logN - 1; l >= 0; l--) {
			Block<T> min = scPath[l][0];
			for (int i = 1; i < capacity; i++) {
				min = blockMin(min, scPath[l][i]);
			}
			
			int p = P(path, l);
			if (l != logN - 1) {
				min = blockMin(min, subtree_mins[2*p]);
				min = blockMin(min, subtree_mins[2*p + 1]);
			}
			
			subtree_mins[p] = min;
		}
	}
	
	protected PlainBlock[][] getPath(boolean[] path) {
		PlainBlock[][] result = new PlainBlock[logN][];
		if (env.mode == Mode.COUNT) {
			for (int i = 0; i < logN; ++i) {
				result[i] = new PlainBlock[capacity];
				for (int j = 0; j < capacity; ++j)
					result[i][j] = getDummyBlock(true);
			}
			return result;
		}
		int index = 1;
		result[0] = tree[index];
		for (int i = 1; i < logN; ++i) {
			index *= 2;
			if (path[lengthOfPos - i])
				++index;
			result[i] = tree[index];
		}
		return result;
	}

	protected void putPath(PlainBlock[][] blocks, boolean[] path) {
		if (env.mode == Mode.COUNT)
			return;
		int index = 1;
		tree[index] = blocks[0];
		for (int i = 1; i < logN; ++i) {
			index *= 2;
			if (path[lengthOfPos - i])
				++index;
			tree[index] = blocks[i];
		}
	}

	public Block<T>[][] preparePath(PlainBlock[][] clientBlock,
			PlainBlock[][] serverBlock) {
		Block<T>[][] s = inputPathOfServer(serverBlock);
		Block<T>[][] c = inputPathOfClient(clientBlock);
		return lib.xor(s, c);
	}

	public Block<T>[][] inputPathOfClient(PlainBlock[][] b) {
		int length = 0;
		for (int i = 0; i < b.length; ++i)
			length += b[i].length;

		PlainBlock[] tmp = new PlainBlock[length];
		int cnt = 0;
		for (int i = 0; i < b.length; ++i)
			for (int j = 0; j < b[i].length; ++j)
				tmp[cnt++] = b[i][j];

		Block<T>[] tmpResult = inputBucketOfClient(tmp);
		cnt = 0;
		Block<T>[][] result = lib.newBlockMatrix(b.length);
		for (int i = 0; i < b.length; ++i) {
			result[i] = lib.newBlockArray(b[i].length);
			for (int j = 0; j < b[i].length; ++j)
				result[i][j] = tmpResult[cnt++];
		}
		return result;
	}

	public Block<T>[][] inputPathOfServer(PlainBlock[][] b) {
		int length = 0;
		for (int i = 0; i < b.length; ++i)
			length += b[i].length;

		PlainBlock[] tmp = new PlainBlock[length];
		int cnt = 0;
		for (int i = 0; i < b.length; ++i)
			for (int j = 0; j < b[i].length; ++j)
				tmp[cnt++] = b[i][j];
		Block<T>[] tmpResult = inputBucketOfServer(tmp);

		cnt = 0;
		Block<T>[][] result = lib.newBlockMatrix(b.length);
		for (int i = 0; i < b.length; ++i) {
			result[i] = lib.newBlockArray(b[i].length);
			for (int j = 0; j < b[i].length; ++j)
				result[i][j] = tmpResult[cnt++];
		}
		return result;
	}

	public PlainBlock[][] preparePlainPath(Block<T>[][] blocks) {
		Block<T>[][] randomSCPath = lib.newBlockMatrix(blocks.length);

		PlainBlock[][] randomPath = new PlainBlock[blocks.length][];
		for (int i = 0; i < randomPath.length; ++i) {
			randomPath[i] = randomBucket(blocks[i].length);
		}
		env.flush();
		
		randomSCPath = inputPathOfServer(randomPath);

		PlainBlock[][] result = outputBuckets(lib.xor(blocks, randomSCPath));

		if (p == Party.Alice)
			return result;
		else
			return randomPath;
	}

	public PlainBlock[][] randomPath(PlainBlock[][] path) {
		PlainBlock[][] result = new PlainBlock[path.length][];
		for (int i = 0; i < path.length; ++i)
			result[i] = randomBucket(path[i].length);
		return result;
	}
}
