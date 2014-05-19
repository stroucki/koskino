package com.github.anastasop.koskino.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.anastasop.koskino.Score;
import com.github.anastasop.koskino.io.StrouckiRecordIOReader;
import com.github.anastasop.koskino.io.StrouckiRecordIOWriter;

public class StrouckiStorageService implements StorageService {

  Object writeLock = new Object();
  private Logger logger = LoggerFactory.getLogger(StrouckiStorageService.class);

  
  private String getDirName(String fullName) {
    char[] chars = fullName.toCharArray();
    StringBuilder sb = new StringBuilder();
    char[][] segments = new char[4][2];
    System.arraycopy(chars, 0, segments[0], 0, 2);
    System.arraycopy(chars, 2, segments[1], 0, 2);
    System.arraycopy(chars, 4, segments[2], 0, 2);
    System.arraycopy(chars, 6, segments[3], 0, 2);

    sb.append(segments[0]).append('/');
    sb.append(segments[1]).append('/');
    sb.append(segments[2]).append('/');
    sb.append(segments[3]).append('/');

    return sb.toString();
  }
  
  @Override
  public void close() throws Exception {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Block get(Score score, byte type) throws IOException {
    String scoreString = score.toString();
    String dirName = getDirName(scoreString);
    File dataFile = new File(dirName+scoreString+"-"+(long)type);
    if (dataFile.exists()) {
      long length = dataFile.length();
      if (length > 70000) {
        logger.error("Block size limited to 70000");;
        return null;
      }
      
      FileInputStream is = new FileInputStream(dataFile);
      StrouckiRecordIOReader reader = new StrouckiRecordIOReader(is, type);
      Block out = reader.readBlock();
      is.close();
      return out;
    }
    
    logger.debug("Block {} not found", scoreString);
    return null;
  }

  @Override
  public Block put(byte[] data, byte type) throws IOException {
    if (data.length > 65536) {
      logger.error("Block size limited to 65536");
      return null;
    }
    Block block = new Block(type, data);
    String scoreString = block.getScore().toString();
    String dirName = getDirName(scoreString);
    File dataFile = new File(dirName+scoreString+"-"+(long)type);
    synchronized (writeLock) {
      if (!dataFile.exists()) {
        File dataDir = new File(dirName);
        dataDir.mkdirs();
        FileOutputStream os = new FileOutputStream(dataFile);
        StrouckiRecordIOWriter writer = new StrouckiRecordIOWriter(os, type);
        int bytesWritten = writer.writeBlock(block);
        os.close();

        if (bytesWritten == -1) {
          logger.error("Failed to write block");
          dataFile.delete();
        }

      } else {
        // already exists
        logger.debug("Block {} already exists", scoreString);
      }
    }
    
    return block;
  }

  @Override
  public void sync() throws IOException {
    // TODO Auto-generated method stub
    
  }

  public static StorageService forName(File arenaDir, String arenaName) {
    return new StrouckiStorageService();
  }

}
