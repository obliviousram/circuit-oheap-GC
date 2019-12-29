// Copyright (C) 2014 by Xiao Shaun Wang <wangxiao@cs.umd.edu>
package oheap;

import java.util.Arrays;

public class Block<T> {

	public T[] iden;
	public T[] pos;
	public T[] data;
	public T isDummy;
	
	public T[] key;
	public static final int LENGTH_OF_KEY = 64;  // number of bits in long

	public Block(T[] iden, T[] pos, T[] key, T[] data, T isDummy) {
		this.iden = iden;
		this.pos = pos;
		this.data = data;
		this.isDummy = isDummy;
		this.key = key;
	}

	public Block(T[] Tarray, int lengthOfIden, int lengthOfPos, int lengthOfData) {
		iden = Arrays.copyOfRange(Tarray, 0, lengthOfIden);
		pos = Arrays.copyOfRange(Tarray, lengthOfIden, lengthOfIden
				+ lengthOfPos);
		key = Arrays.copyOfRange(Tarray, lengthOfIden+lengthOfPos, lengthOfIden+lengthOfPos+LENGTH_OF_KEY);
		data = Arrays.copyOfRange(Tarray, lengthOfIden+lengthOfPos+LENGTH_OF_KEY,
				lengthOfIden+lengthOfPos+LENGTH_OF_KEY+lengthOfData);
		isDummy = Tarray[Tarray.length - 1];
	}

}
