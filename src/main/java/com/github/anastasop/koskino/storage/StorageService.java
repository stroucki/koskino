package com.github.anastasop.koskino.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.github.anastasop.koskino.Score;

public class StorageService {
	private Logger logger = Logger.getLogger(StorageService.class.getName());
	
	private Map<String, Block> dataLog;
	private long nBytes;
	private long nBlocks;
	
	public StorageService() {
		this.dataLog = new HashMap<String, Block>();
	}
	
	public synchronized Block get(Score score, byte type) {
		String key = score.toString();
		Block b = dataLog.get(key);
		if (b == null || b.getType() != type) {
			return null;
		}
		return b;
	}
	
	public synchronized Block put(byte[] data, byte type) {
		Block b = new Block(type, data);
		dataLog.put(b.getScore().toString(), b);
		nBlocks++;
		nBytes += data.length;
		return b;
	}
	
	public synchronized boolean contains(Score score) {
		return dataLog.containsKey(score.toString());
	}
	
	public void logStatistics() {
		logger.info(String.format("blocks: %d bytes: %d", nBlocks, nBytes));
	}
}
