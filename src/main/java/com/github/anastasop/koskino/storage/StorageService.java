package com.github.anastasop.koskino.storage;

import java.io.IOException;

import com.github.anastasop.koskino.Score;

public interface StorageService extends AutoCloseable {
	Block get(Score score, byte type) throws IOException;
	
	Block put(byte[] data, byte type) throws IOException;
	
	void sync() throws IOException;
}
