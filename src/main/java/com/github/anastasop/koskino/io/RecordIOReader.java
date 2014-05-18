package com.github.anastasop.koskino.io;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.anastasop.koskino.storage.Block;

public class RecordIOReader implements AutoCloseable {
	private Logger logger = LoggerFactory.getLogger(RecordIOReader.class);
	
	private DataInputStream ist;
	int streamPos;
	private byte[] buf;
	private byte[] scoreBytes;
	
	public RecordIOReader(DataInputStream ist) {
		this.ist = ist;
		this.streamPos = 0;
		this.buf = new byte[RecordIOWriter.HEADER_LENGTH];
		this.scoreBytes = new byte[20];
	}
	
	public synchronized Block readBlock() throws IOException {
		boolean syncingMode = false;
		scanForValidHeader: for (;;) {
			try {
				ist.readFully(buf, 0, 34);
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

			CRC32 calculatedHash = new CRC32(); 
			calculatedHash.update(buf, 0, 30);
			// XXX endianness?
			long hashValue = calculatedHash.getValue();
			byte[] hbytes = ByteBuffer.allocate(4).putLong(hashValue).array(); 

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
			byte[] dataHashInRecord = scoreBytes;

			byte[] data = new byte[dataLen];
			try {
				ist.readFully(data, 0, dataLen);
			} catch (EOFException e) {
				logger.error("unexpected error while reading data. Stream position {}", dataLen, streamPos);
				return null;
			}
			
			MessageDigest md;
      try {
        md = MessageDigest.getInstance("SHA-1");
      } catch (NoSuchAlgorithmException e) {
        // XXX see note in Score
        e.printStackTrace();
        throw new InternalError(e);
      }
      
			md.update(data, 0, dataLen);
			byte[] dataHashRecomputed = md.digest();
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
