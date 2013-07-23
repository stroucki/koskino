package com.github.anastasop.koskino.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.anastasop.koskino.Score;
import com.github.anastasop.koskino.io.RecordIOReader;
import com.github.anastasop.koskino.io.RecordIOWriter;
import com.google.common.hash.HashCodes;

public class FileStorageService implements StorageService {
	private static class BlockDescr {
		long offset;
		int length;
		byte type;
		byte[] score;
		
		private BlockDescr(long offset, int length, byte type, byte[] score) {
			this.offset = offset;
			this.length = length;
			this.type = type;
			this.score = score;
		}
		
		byte[] toByteArray() {
			byte[] data = new byte[33];
			data[0] = (byte)((offset >>  0) & 0xFF);
			data[1] = (byte)((offset >>  8) & 0xFF);
			data[2] = (byte)((offset >> 16) & 0xFF);
			data[3] = (byte)((offset >> 24) & 0xFF);
			data[4] = (byte)((offset >> 32) & 0xFF);
			data[5] = (byte)((offset >> 40) & 0xFF);
			data[6] = (byte)((offset >> 48) & 0xFF);
			data[7] = (byte)((offset >> 56) & 0xFF);
			data[8] = (byte)((length >>  0) & 0xFF);
			data[9] = (byte)((length >>  8) & 0xFF);
			data[10] = (byte)((length >> 16) & 0xFF);
			data[11] = (byte)((length >> 24) & 0xFF);
			data[12] = type;
			System.arraycopy(score, 0, data, 13, 20);
			return data;
		}
		
		static BlockDescr fromByteArray(byte[] data) {
			long offset = (data[0] & 0xFF) | (data[1] & 0xFF) << 8 | (data[2] & 0xFF) << 16 | (data[3] & 0xFF) << 24 |
					 (data[4] & 0xFF) << 32 | (data[5] & 0xFF) << 40 | (data[6] & 0xFF) << 48 | (data[7] & 0xFF) << 56;
			
			int length = (data[8] & 0xFF) | (data[9] & 0xFF) << 8 | (data[10] & 0xFF) << 16 | (data[11] & 0xFF) << 24;
			
			byte[] score = new byte[20];
			System.arraycopy(data, 13, score, 0, 20);
			
			return new BlockDescr(offset, length, data[12], score);
		}
	}
	
	private Logger logger = LoggerFactory.getLogger(FileStorageService.class);
	private OutputStream logStream;
	private OutputStream indexStream;
	private RecordIOWriter indexWriter;
	private RecordIOWriter logWriter;
	private Map<String, BlockDescr> blockIndex;
	private RandomAccessFile logFile;
	private String arenaName;
	private long nBytesWrittenToArenaLog;
	
	public static FileStorageService forName(File arenaDir, String arenaName) throws IOException {
		Logger logger = LoggerFactory.getLogger(FileStorageService.class);
		File arenaLog = new File(arenaDir, arenaName + ".log");
		arenaLog.createNewFile();
		File arenaIndex = new File(arenaDir, arenaName + ".idx");
		arenaIndex.createNewFile();
		
		Map<String, BlockDescr> blockIndex = new HashMap<String, BlockDescr>();
		RecordIOReader r = new RecordIOReader(new FileInputStream(arenaIndex));
		try  {
			Block b = null;
			while ((b = r.readBlock()) != null) {
				BlockDescr descr = BlockDescr.fromByteArray(b.getData());
				String key = HashCodes.fromBytes(descr.score).toString();
				blockIndex.put(key, descr);
				logger.debug("index entry for {}", key);
			}
		} finally {
			r.close();
		}
		
		final FileStorageService storage = new FileStorageService();
		storage.logStream = new FileOutputStream(arenaLog, true);
		storage.logWriter = new RecordIOWriter(storage.logStream);
		storage.indexStream = new FileOutputStream(arenaIndex, true);
		storage.indexWriter = new RecordIOWriter(storage.indexStream);
		storage.logFile = new RandomAccessFile(arenaLog, "r");
		storage.arenaName = arenaName;
		storage.nBytesWrittenToArenaLog = 0L;
		storage.blockIndex = blockIndex;
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					storage.close();
				} catch (Exception e) {
					// TODO: do sth here
				}
			}
		});
		logger.info("index contains {} blocks", blockIndex.size());
		return storage;
	}
	
	@Override
	public synchronized Block get(Score score, byte type) throws IOException {
		String key = score.toString();
		BlockDescr descr = blockIndex.get(key);
		if (descr == null || descr.type != type) {
			return null;
		}
		logFile.seek(descr.offset);
		byte[] data = new byte[descr.length];
		logFile.readFully(data);
		
		Block b = new Block(type, data);
		String reComputedKey = b.getScore().toString();
		if (!reComputedKey.equals(key)) {
			logger.error("GET: corrupted block, key mismatch, with key {}/{} at arena {}/{}", key, reComputedKey, arenaName, descr.offset);
			return null;
		}
		return b;
	}
	
	@Override
	public synchronized Block put(byte[] data, byte type) throws IOException {
		Block scratch = new Block(type, data);
		String key = scratch.getScore().toString();
		BlockDescr descr = blockIndex.get(key);
		if (descr != null) {
			if (descr.type == type) {
				logger.info("block {} coalesced as already exists", key);
			} else {
				logger.error("corrupted block, type mismatch, with key {} at arena {}/{}", key, arenaName, descr.offset);
			}
			return scratch;
		}
		
		long pos = nBytesWrittenToArenaLog;
		logWriter.writeBlock(type, data);
		nBytesWrittenToArenaLog += RecordIOWriter.HEADER_LENGTH + data.length;
		descr = new BlockDescr(pos + RecordIOWriter.HEADER_LENGTH, data.length, type, scratch.getScore().getBytes());
		indexWriter.writeBlock(type, descr.toByteArray());
		blockIndex.put(key, descr);
		return scratch;
	}
	
	@Override
	public void sync() throws IOException {
		indexStream.flush();
		logStream.flush();
	}

	@Override
	public void close() throws Exception {
		indexStream.flush();
		indexStream.close();
		logStream.flush();
		logStream.close();
	}
}
