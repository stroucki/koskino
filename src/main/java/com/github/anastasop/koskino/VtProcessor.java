package com.github.anastasop.koskino;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ascient.threading.FCFSQueue;
import ascient.threading.RunnableTask;

import com.github.anastasop.koskino.storage.Block;
import com.github.anastasop.koskino.storage.StorageService;

public class VtProcessor implements Runnable {
  private Logger logger = LoggerFactory.getLogger(VtProcessor.class);

  private Socket socket;
  private BufferedInputStream ist;
  private BufferedOutputStream ost;
  private StorageService storage;
  private FCFSQueue<VtMessage> incomingQueue;

  public VtProcessor(Socket socket, StorageService storage) {
    this.socket = socket;
    this.storage = storage;
    this.incomingQueue = new FCFSQueue<>("incoming");
  }

  private void prepareConnection() throws IOException {
    this.ist = new BufferedInputStream(socket.getInputStream());
    this.ost = new BufferedOutputStream(socket.getOutputStream());
    ost.write("venti-02:04-koskino\n".getBytes());
    ost.flush();
    byte[] buf = new byte[64];
    int pos = 0;
    for (;;) {
      int r = ist.read();
      if (r == -1) {
        throw new IOException("prepareConnection: EOF before read version line");
      }
      buf[pos++] = (byte)r;
      if (r == '\n') {
        pos--; // do not include LF
        logger.info("Version line: {}", new String(buf, 0, pos));
        break;
      }
    }
  }

  @Override
  public void run() {
    try {
      prepareConnection();
      logger.info("connection processor started");
      runLoop();
    } catch (IOException e) {
      logger.info("IOException: Closing Connection", e);
    } finally {
      // release connection
      try {
        if (ist != null) {
          ist.close();
        }
      } catch (IOException e) {
        //
      }
      try {
        if (ost != null) {
          ost.close();
        }
      } catch (IOException e) {
        //
      }
    }
  }

  private void runLoop() throws IOException {
    VtMessageReader msgReader = new VtMessageReader(ist);
    for (;;) {
      VtMessageSerializer serializer = new VtMessageSerializer();
      VtMessage req = null;
      VtMessage resp = null;

      try {
        req = msgReader.read();
        if (req == null) {
          break;
        }
        //logger.info("Request: {}", req.toString());
        if (req.msgType == VtMessage.VtThello) {
          msgReader.setProtocolVersion(req.version);
          serializer.setProtocolVersion(req.version);
        }
      } catch (ProtocolException e) {
        logger.error("protocol expression: {}", e.getMessage());
        return;
      }

      final VtMessage taskReq = req;

      Callable<VtMessage> callable = new Callable<VtMessage>() {
     @Override
        public VtMessage call() throws IOException {
          VtMessage resp = null;
          switch (taskReq.msgType) {
            case VtMessage.VtTping:
              resp = new VtMessage(VtMessage.VtRping, taskReq.tag);
              break;
            case VtMessage.VtThello:
              resp = new VtMessage(VtMessage.VtRhello, taskReq.tag);
              resp.sid = "Spy";
              resp.codec = "None";
              resp.crypto = "None";
              break;
            case VtMessage.VtTgoodbye:
              try {
                storage.sync();
              } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }
              // no response for VtTgoodbye, server closes connection
              return null;
            case VtMessage.VtTread:
              Block rblock;
              try {
                rblock = storage.get(taskReq.score, taskReq.type);
              } catch (IOException e) {
                // TODO Auto-generated catch block
                throw new InternalError(e);
              }
              if (rblock != null) {
                resp = new VtMessage(VtMessage.VtRread, taskReq.tag);
                resp.data = rblock.getData();
              } else {
                resp = new VtMessage(VtMessage.VtRerror, taskReq.tag);
                resp.error = String.format("no block with score %s/%d exists", taskReq.score.toString(), taskReq.type);
              }
              break;
            case VtMessage.VtTwrite:
              Block b;
              try {
                b = storage.put(taskReq.data, taskReq.type);
              } catch (IOException e) {
                // TODO Auto-generated catch block
                throw new InternalError(e);
              }
              resp = new VtMessage(VtMessage.VtRwrite, taskReq.tag);
              resp.score = b.getScore();
              break;
            case VtMessage.VtTsync:
              storage.sync();
              resp = new VtMessage(VtMessage.VtRsync, taskReq.tag);
              break;
            default:
              break;
          }
          return resp;

        }
      };
      
      
      RunnableTask<VtMessage> task = new RunnableTask<VtMessage>(callable);
      incomingQueue.put(task);

      try {
        resp = task.returnValue();
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (ExecutionException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      if (resp != null) {
        //logger.info("Response: {}", resp.toString());
        serializer.writeMessage(resp, ost);
        ost.flush();
      } else {
        logger.error("shit");
      }
    }
  }
}
