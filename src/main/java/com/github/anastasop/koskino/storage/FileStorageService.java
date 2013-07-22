package com.github.anastasop.koskino.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.anastasop.koskino.Score;
import com.github.anastasop.koskino.io.RecordIOReader;
import com.github.anastasop.koskino.io.RecordIOWriter;

public class FileStorageService implements StorageService {
	private Logger logger = LoggerFactory.getLogger(FileStorageService.class);
	private OutputStream ost;
	private RecordIOWriter writer;
	private Map<String, Block> dataLog;
	
	private FileStorageService(Map<String, Block> dataLog, OutputStream ost) throws IOException {
		this.dataLog = dataLog;
		this.ost = ost;
		this.writer = new RecordIOWriter(ost);
	}
	
	public static FileStorageService forFile(File f) throws IOException {
		Map<String, Block> dataLog = new HashMap<String, Block>();
		RecordIOReader r = new RecordIOReader(new FileInputStream(f));
		try  {
			Block b = null;
			while ((b = r.readBlock()) != null) {
				dataLog.put(b.getScore().toString(), b);
			}
		} finally {
			r.close();
		}
		
		final FileStorageService storage = new FileStorageService(dataLog, new FileOutputStream(f, true));
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
		return storage;
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
		Block scratch = new Block(type, data);
		Block inMem = get(scratch.getScore(), type);
		if (inMem == null) {
			writer.writeBlock(type, data);
			dataLog.put(scratch.getScore().toString(), scratch);
			inMem = scratch;
		} else {
			logger.info("block {} coalesced as already exists", inMem.getScore().toString());
		}
		return inMem;
	}
	
	@Override
	public void sync() throws IOException {
		ost.flush();
	}

	@Override
	public void close() throws Exception {
		ost.flush();
		ost.close();
	}
}
