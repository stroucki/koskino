package com.github.anastasop.koskino.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import kanzi.io.CompressedOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.anastasop.koskino.storage.BackendBlock;
import com.github.anastasop.koskino.storage.Block;

// big endian
// storage block version 0
// 0: version (0)
// 1-8: magic (deaddada)
// 9-12: data size (32 bit)
// 13-15: Compression (SNA)

public class BackendBlockWriter {
	private Logger logger = LoggerFactory.getLogger(BackendBlockWriter.class);
	
	int streamPos;
	byte type;
	
	
	public static BackendBlock fromBlock(Block block) throws IOException {
	  Logger logger = LoggerFactory.getLogger(BackendBlockWriter.class);
	  int dataLength = block.getData().length;
	  if (dataLength > 65536) {
	    logger.error("Block size limited to 65536");
throw new InternalError("block size incorrect");
}

	  byte[] header = new byte[16];
	  header[0] = 0;
	  
	  byte[] myMagic = "deaddada".getBytes();
	  System.arraycopy(myMagic, 0, header, 1, myMagic.length);

	  byte[] sizeBytes = new byte[4];
	  int foo = dataLength;
	  sizeBytes[3] = (byte) (foo & 0xff);
	  foo >>= 8;
	  sizeBytes[2] = (byte) (foo & 0xff);
	  foo >>= 8;
	  sizeBytes[1] = (byte) (foo & 0xff);
	  foo >>= 8;
	  sizeBytes[0] = (byte) (foo & 0xff);
	  System.arraycopy(sizeBytes, 0, header, 9, sizeBytes.length);

	  header[13] = 'S';
	  header[14] = 'N';
	  header[15] = 'A';

ByteArrayOutputStream ost = new ByteArrayOutputStream();
	  
	  ost.write(header);

	  OutputStream cost = new CompressedOutputStream("None", "Snappy", ost);
	  cost.write(block.getData());

	  cost.close();

	  
	  byte[] newData = ost.toByteArray();

	  return new BackendBlock(block.getType(), block.getScore(), newData);
	}

}
