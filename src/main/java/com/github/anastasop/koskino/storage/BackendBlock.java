package com.github.anastasop.koskino.storage;

import com.github.anastasop.koskino.Score;

public class BackendBlock {
	private final Score score;
	private final byte type;
	private final byte[] data;
	
	public BackendBlock(byte type, Score score, byte[] data) {
		this.type = type;
		this.data = data;
		this.score = score;
	}

	public Score getScore() {
		return score;
	}

	public byte getType() {
		return type;
	}

	public byte[] getData() {
		return data;
	}
}
