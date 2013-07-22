package com.github.anastasop.koskino.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;

public class RecordIOReader {
	private Logger logger = LoggerFactory.getLogger(RecordIOReader.class);
	
	private InputStream ist;
	private byte[] buf;
	
	public RecordIOReader(InputStream ist) {
		this.ist = ist;
		this.buf = new byte[65536];
	}
	
	private long syncToNextValidRecord() throws IOException {
		int streamPos = 0;
		boolean syncingMode = false;
		scanForRecord: for (;;) {
			try {
				ByteStreams.readFully(ist, buf, 0, 34);
			} catch (EOFException e) {
			}

			if (buf[0] != RecordIOWriter.MAGIC[0]
					|| buf[1] != RecordIOWriter.MAGIC[1]
					|| buf[2] != RecordIOWriter.MAGIC[2]
					|| buf[3] != RecordIOWriter.MAGIC[3]) {
				if (!syncingMode) {
					logger.error("record does not start with MAGIC. Stream position {}", streamPos);
					syncingMode = true;
				}
				continue scanForRecord;
			}

			HashCode calculatedHash = Hashing.crc32().hashBytes(buf, 0, 30);
			byte[] hbytes = calculatedHash.asBytes();
			if (hbytes[0] != buf[30] || hbytes[1] != buf[31]
					|| hbytes[2] != buf[32] || hbytes[3] != buf[33]) {
				if (!syncingMode) {
					logger.error("record hash does not match computed hash. Stream position {}", streamPos);
					syncingMode = true;
				}
				continue scanForRecord;
			}
			
			if (buf[8] != RecordIOWriter.SHA1_HASH || buf[8] != RecordIOWriter.CRC32_HASH) {
				if (!syncingMode) {
					logger.error("data hash type {} is not supported. Stream position {}", buf[8], streamPos);
					syncingMode = true;
				}
				continue scanForRecord;
			}
			
			int dataLen = ((buf[0] << 8) & 0xFF) | (buf[1] & 0xFF);
			if (dataLen > 65536) {
				if (!syncingMode) {
					logger.error("data length of {} is not supported. Stream position {}", dataLen, streamPos);
					syncingMode = true;
				}
				continue scanForRecord;
			}
			
			byte dataType = buf[0];
			byte dataHash = 0;

			try {
				ByteStreams.readFully(ist, buf, 0, 34);
				streamPos += 34;
			} catch (EOFException e) {
			}
			
		}
	}
}
