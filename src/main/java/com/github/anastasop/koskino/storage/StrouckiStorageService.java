package com.github.anastasop.koskino.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ascient.threading.FCFSQueue;
import ascient.threading.RunnableTask;

import com.github.anastasop.koskino.Score;
import com.github.anastasop.koskino.io.BackendBlockWriter;
import com.github.anastasop.koskino.io.StrouckiRecordIOReader;

public class StrouckiStorageService implements StorageService {

  Object writeLock = new Object();
  private Logger logger = LoggerFactory.getLogger(StrouckiStorageService.class);
  private FCFSQueue<Block> blockQueue = new FCFSQueue<>("blockqueue");
  private FCFSQueue<Void> saveQueue = new FCFSQueue<>("savequeue");
  private FCFSQueue<Void> testQueue = new FCFSQueue<>("testqueue");
  
  public StrouckiStorageService() {
    Runnable loader = new Runnable() {

      @Override
      public void run() {
       while (true) {
         logger.error("1");
         Callable<Void> callable = new Callable<Void>() {

          @Override
          public Void call() throws Exception {
           Thread.sleep(1000);
           return null;
          }
        };
        logger.error("2");
  
        RunnableTask<Void> task = new RunnableTask<Void>(callable);
        testQueue.put(task);
        logger.error("3");

        
        new BackgroundTask(task).start();
        logger.error("4");
     
       }
        
      }
      
    };
    
    //new Thread(loader).start();
  }


  private static String getDirName(String fullName) {
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
  
  private class BackgroundTask extends Thread {
    RunnableTask<?> task;
    
    private BackgroundTask(RunnableTask<?> x) {
      task = x;
    }
    public void run() {
      try {
        //logger.error("waiting");
        task.returnValue();
        //logger.error("done");
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (ExecutionException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  @Override
  public void close() throws Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public Block get(final Score score, final byte type) throws IOException {
    String scoreString = score.toString();
    String dirName = getDirName(scoreString);
    final File dataFile = new File(dirName+scoreString+"-"+(long)type);
    Block block = null;
    if (dataFile.exists()) {
      long length = dataFile.length();
      if (length > 70000) {
        logger.error("Block size limited to 70000");;
        return null;
      }

    Callable<Block> callable = new Callable<Block>() {

      @Override
      public Block call() throws Exception {
        FileInputStream is = new FileInputStream(dataFile);
        StrouckiRecordIOReader reader = new StrouckiRecordIOReader(is, type);
        Block out = reader.readBlock();
        is.close();
        return out;
      }

    };

    RunnableTask<Block> task = new RunnableTask<Block>(callable);
    blockQueue.put(task);

    try {
      block = task.returnValue();
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ExecutionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    }

    if (block == null) {
      logger.debug("Block {} not found", scoreString);
      return null;
    }

    return block;
  }

  @Override
  public Block put(final byte[] data, final byte type) throws IOException {
    if (data.length > 65536) {
      logger.error("Block size limited to 65536");
      return null;
    }
    final Block block = new Block(type, data);
    final String scoreString = block.getScore().toString();
    final String dirName = getDirName(scoreString);
    final String fileName = dirName+scoreString+"-"+(long)type;
    final File dataFile = new File(fileName);
    
    Callable<Void> callable = new Callable<Void>() {

      @Override
      public Void call() {
        if (dataFile.exists()) {
          logger.debug("Block {} already exists", scoreString);
          return null;
        }
        
        BackendBlock backendBlock;
        try {
          backendBlock = BackendBlockWriter.fromBlock(block);
        } catch (IOException e1) {
          e1.printStackTrace();
          throw new InternalError("failed to create backend block");
        }
        
        synchronized (fileName) {
          if (!dataFile.exists()) {
            File dataDir = new File(dirName);
            dataDir.mkdirs();

            FileOutputStream os;
            try {
              os = new FileOutputStream(dataFile);
            os.write(backendBlock.getData());
           os.close();
            } catch (IOException e) {
              logger.error("Failed to write block: {}", e);
              dataFile.delete();
throw new InternalError("failed to write block");
}
  
          } else {
            // already exists
            logger.debug("Block {} already exists", scoreString);
          }
        }
 return null;
      }
    };
    
    RunnableTask<Void> task = new RunnableTask<Void>(callable);
    saveQueue.put(task);
    
    new BackgroundTask(task).start();
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
