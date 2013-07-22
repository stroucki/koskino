package com.github.anastasop.koskino.io;

import java.io.IOException;
import java.io.OutputStream;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

public class RecordIOWriter implements AutoCloseable {
	public static final byte SHA1_HASH = 0;
	public static final int HEADER_LENGTH = 34;
	public static final byte[] MAGIC = new byte[]{0, 0, 0, 0};
	
	private OutputStream ost;
	
	public RecordIOWriter(OutputStream ost) {
		this.ost = ost;
	}
	
	public synchronized void writeBlock(byte type, byte[] data) throws IOException {
		// [4]magic [4]data_size [1]hash_func [1]block_type [20]data_hash [4]header_hash
		// little endian order
		byte[] header = new byte[HEADER_LENGTH];
		System.arraycopy(MAGIC, 0, header, 0, 4);
		int l = data.length;
		header[4] = (byte)(l &0xFF);
		header[5] = (byte)((l >>  8) & 0xFF);
		header[6] = (byte)((l >> 16) & 0xFF);
		header[7] = (byte)((l >> 24) & 0xFF);
		header[8] = SHA1_HASH;
		header[9] = type;
		HashCode dataHash = Hashing.sha1().hashBytes(data);
		dataHash.writeBytesTo(header, 10, 20);
		HashCode headerHash = Hashing.crc32().hashBytes(header, 0, 30);
		headerHash.writeBytesTo(header, 30, 4);
		ost.write(header);
		ost.write(data);
	}

	@Override
	public void close() throws IOException {
		ost.close();
	}
}
