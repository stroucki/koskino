package com.github.anastasop.koskino.storage;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import com.github.anastasop.koskino.Score;
import com.github.anastasop.koskino.io.RecordIOWriter;

public class FileStorageService implements StorageService {
	private OutputStream ost;
	private RecordIOWriter writer;
	private Map<String, Block> dataLog;
	
	public FileStorageService(Map<String, Block> dataLog, OutputStream ost) throws IOException {
		this.dataLog = dataLog;
		this.ost = ost;
		this.writer = new RecordIOWriter(ost);
	}
	
	@Override
	public synchronized Block get(Score score, byte type) {
		String key = score.toString();
		Block b = dataLog.get(key);
		if (b == null || b.getType() != type) {
			return null;
		}
		return b;
	}
	
	@Override
	public synchronized Block put(byte[] data, byte type) throws IOException {
		Block b = new Block(type, data);
		writer.writeBlock(type, data);
		dataLog.put(b.getScore().toString(), b);
		return b;
	}
	
	@Override
	public void sync() throws IOException {
		ost.flush();
	}
}
