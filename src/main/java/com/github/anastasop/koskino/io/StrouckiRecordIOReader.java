package com.github.anastasop.koskino.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import kanzi.io.CompressedInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.anastasop.koskino.storage.Block;

// big endian
// storage block version 0
// 0: version (0)
// 1-8: magic (deaddada)
// 9-12: data size (32 bit)
// 13-15: compression (SNA)

public class StrouckiRecordIOReader {
	private Logger logger = LoggerFactory.getLogger(StrouckiRecordIOReader.class);
	
	private InputStream ist;
	int streamPos;
	byte type;
	
	public StrouckiRecordIOReader(InputStream ist, byte type) {
		this.ist = ist;
		this.streamPos = 0;
		this.type = type;
	}
	
	public Block readBlock() throws IOException {
	  int firstByte = ist.read();
	  if (firstByte == -1) {
	    logger.error("Could not read block file");
	    return null;
	  }
	  
	  int version = firstByte;
	  if (version > 0) {
	    logger.error("Cannot handle version > 0");
	    return null;
	  }
	  
	  byte[] magic = new byte[8];
	  int bytesRead = ist.read(magic);
	  if (bytesRead != 8) {
	    logger.error("Could not read magic");
	    return null;
	  }
	  
	  byte[] myMagic = "deaddada".getBytes();
	  if (!Arrays.equals(myMagic, magic)) {
	    logger.error("Magic doesn't match");
	    return null;
	  }
	  
	  byte[] sizeBytes = new byte[4];
	  bytesRead = ist.read(sizeBytes);
	  if (bytesRead != 4) {
	    logger.error("Could not get block size");
	    return null;
	  }
	  
	  long size = 0;
	  size |= sizeBytes[0] & 0xff;
	  size <<= 8;
	  size |= sizeBytes[1] & 0xff;
	  size <<= 8;
	  size |= sizeBytes[2] & 0xff;
	  size <<= 8;
	  size |= sizeBytes[3] & 0xff;
	  	  
	  if (size > 65536) {
	    logger.error("Size is limited to 65536");
	    return null;
	  }
	  
	  byte[] compression = new byte[3];
	  bytesRead = ist.read(compression);
	  if (bytesRead != 3) {
	    logger.error("Could not read compression field");
	    return null;
	  }
	  
    byte[] data = new byte[(int)size];
	  if (compression[0]=='S' && compression[1]=='N' && compression[2]=='A') {
	    CompressedInputStream cist = new CompressedInputStream(ist);
	    bytesRead = cist.read(data);
	    cist.close();
	  }
	  else if ((compression[0] & compression[1] & compression[2]) == 0) {
	    bytesRead = ist.read(data);
	  }
	  else {
	    logger.error("Unknown compression format");
	    return null;
	  }
    if (bytesRead != size) {
      logger.error("Short read {} out of {}", bytesRead, size);
      return null;
    }
    
    Block out = new Block(type, data);
    return out;
	}

}
