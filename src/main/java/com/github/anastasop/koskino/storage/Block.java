package com.github.anastasop.koskino.storage;

import com.github.anastasop.koskino.Score;

public class Block {
	private final Score score;
	private final byte type;
	private final byte[] data;
	
	public Block(byte type, byte[] data) {
		this.type = type;
		this.data = data;
		this.score = Score.forBlock(data);
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
