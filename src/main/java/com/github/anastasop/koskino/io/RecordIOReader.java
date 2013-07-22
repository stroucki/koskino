package com.github.anastasop.koskino.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.anastasop.koskino.storage.Block;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashCodes;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;

public class RecordIOReader implements AutoCloseable {
	private Logger logger = LoggerFactory.getLogger(RecordIOReader.class);
	
	private InputStream ist;
	int streamPos;
	private byte[] buf;
	private byte[] scoreBytes;
	
	public RecordIOReader(InputStream ist) {
		this.ist = ist;
		this.streamPos = 0;
		this.buf = new byte[RecordIOWriter.HEADER_LENGTH];
		this.scoreBytes = new byte[20];
	}
	
	public synchronized Block readBlock() throws IOException {
		boolean syncingMode = false;
		scanForValidHeader: for (;;) {
			try {
				ByteStreams.readFully(ist, buf, 0, 34);
			} catch (EOFException e) {
				return null;
			}

			if (buf[0] != RecordIOWriter.MAGIC[0]
					|| buf[1] != RecordIOWriter.MAGIC[1]
					|| buf[2] != RecordIOWriter.MAGIC[2]
					|| buf[3] != RecordIOWriter.MAGIC[3]) {
				if (!syncingMode) {
					logger.error("record does not start with MAGIC. Stream position {}", streamPos);
					syncingMode = true;
				}
				streamPos += 34;
				continue scanForValidHeader;
			}

			HashCode calculatedHash = Hashing.crc32().hashBytes(buf, 0, 30);
			byte[] hbytes = calculatedHash.asBytes();
			if (hbytes[0] != buf[30] || hbytes[1] != buf[31]
					|| hbytes[2] != buf[32] || hbytes[3] != buf[33]) {
				if (!syncingMode) {
					logger.error("record hash does not match computed hash. Stream position {}", streamPos);
					syncingMode = true;
				}
				streamPos += 34;
				continue scanForValidHeader;
			}
			
			if (buf[8] != RecordIOWriter.SHA1_HASH) {
				if (!syncingMode) {
					logger.error("data hash type {} is not supported. Stream position {}", buf[8], streamPos);
					syncingMode = true;
				}
				streamPos += 34;
				continue scanForValidHeader;
			}
			
			int dataLen = (buf[4] & 0xFF) | ((buf[5] & 0xFF) << 8);
			if (dataLen > 65536) {
				if (!syncingMode) {
					logger.error("data length of {} is not supported. Stream position {}", dataLen, streamPos);
					syncingMode = true;
				}
				streamPos += 34;
				continue scanForValidHeader;
			}
			
			byte dataType = buf[9];
			System.arraycopy(buf, 10, scoreBytes, 0, 20);
			HashCode dataHashInRecord = HashCodes.fromBytes(scoreBytes);

			byte[] data = new byte[dataLen];
			try {
				ByteStreams.readFully(ist, data, 0, dataLen);
			} catch (EOFException e) {
				logger.error("unexpected error while reading data. Stream position {}", dataLen, streamPos);
				return null;
			}
			
			HashCode dataHashRecomputed = Hashing.sha1().hashBytes(data, 0, dataLen);
			if (!dataHashInRecord.equals(dataHashRecomputed)) {
				logger.error("data hash in header does not match computed data hash. Stream position {}", streamPos);
				syncingMode = true;
				streamPos += dataLen;
				continue scanForValidHeader;
			}
			
			// TODO: recomputes SHA1
			return new Block(dataType, data);
		}
	}
	
	@Override
	public void close() throws IOException {
		ist.close();
	}
}
