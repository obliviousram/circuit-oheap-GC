// Copyright (C) 2014 by Xiao Shaun Wang <wangxiao@cs.umd.edu>
package oheap;

import util.Utils;

public class PlainBlock {
	public long iden;       // aka timestamp
	public long pos;        // aka leaf_id
	public boolean[] data;
	public boolean isDummy;

	public long key;

	public PlainBlock(long iden, long pos, long key, boolean[] data, boolean isDummy) {
		this.iden = iden;
		this.pos = pos;
		this.data = data;
		this.isDummy = isDummy;
		this.key = key;
	}

	public boolean[] toBooleanArray(int lengthOfIden, int lengthOfPos) {
		boolean[] result = new boolean[lengthOfIden + lengthOfPos + Block.LENGTH_OF_KEY + data.length
				+ 1];
		System.arraycopy(Utils.fromLong(iden, lengthOfIden), 0, result, 0, lengthOfIden);
		System.arraycopy(Utils.fromLong(pos, lengthOfIden), 0, result, lengthOfIden, lengthOfPos);
		System.arraycopy(Utils.fromLong(key, Block.LENGTH_OF_KEY), 0, result, lengthOfIden+lengthOfPos, Block.LENGTH_OF_KEY);
		System.arraycopy(data, 0, result, lengthOfPos+lengthOfIden+Block.LENGTH_OF_KEY, data.length);
		result[result.length - 1] = isDummy;
		return result;
	}

	static public boolean[] toBooleanArray(PlainBlock[] blocks, int lengthOfIden, int lengthOfPos) {
		int blockSize = (lengthOfIden + lengthOfPos + Block.LENGTH_OF_KEY + blocks[0].data.length + 1);
		boolean[] result = new boolean[blockSize * blocks.length];
		for (int i = 0; i < blocks.length; ++i) {
			boolean[] tmp = blocks[i].toBooleanArray(lengthOfIden, lengthOfPos);
			System.arraycopy(tmp, 0, result, i * blockSize, blockSize);
		}
		return result;
	}
}
