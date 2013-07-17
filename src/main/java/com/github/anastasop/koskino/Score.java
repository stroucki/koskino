package com.github.anastasop.koskino;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashCodes;
import com.google.common.hash.Hashing;

public class Score {
	private HashCode scoreHashCode;
	
	private Score(HashCode scoreHashCode) {
		this.scoreHashCode = scoreHashCode;
	}
	
	public static Score forBlock(byte[] b) {
		return new Score(Hashing.sha1().hashBytes(b));
	}
	
	public static Score fromBytes(byte[] b) {
		return new Score(HashCodes.fromBytes(b));
	}
	
	public byte[] getBytes() {
		return scoreHashCode.asBytes();
	}
	
	@Override
	public String toString() {
		return scoreHashCode.toString();
	}
}
