package com.github.anastasop.koskino;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.anastasop.koskino.storage.Block;
import com.github.anastasop.koskino.storage.StorageService;

public class VtProcessor implements Runnable {
	private Logger logger = LoggerFactory.getLogger(VtProcessor.class);
	
	private Socket socket;
	private BufferedInputStream ist;
	private BufferedOutputStream ost;
	private StorageService storage;
	
	public VtProcessor(Socket socket, StorageService storage) {
		this.socket = socket;
		this.storage = storage;
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
				logger.info("Request: {}", req.toString());
				if (req.msgType == VtMessage.VtThello) {
					msgReader.setProtocolVersion(req.version);
					serializer.setProtocolVersion(req.version);
				}
			} catch (ProtocolException e) {
				logger.error("protocol expression: {}", e.getMessage());
				return;
			}
			
			switch (req.msgType) {
			case VtMessage.VtTping:
				resp = new VtMessage(VtMessage.VtRping, req.tag);
				break;
			case VtMessage.VtThello:
				resp = new VtMessage(VtMessage.VtRhello, req.tag);
				resp.sid = "Spy";
				resp.codec = "None";
				resp.crypto = "None";
				break;
			case VtMessage.VtTgoodbye:
				storage.sync();
				// no response for VtTgoodbye, server closes connection
				return;
			case VtMessage.VtTread:
				Block rblock = storage.get(req.score, req.type);
				if (rblock != null) {
					resp = new VtMessage(VtMessage.VtRread, req.tag);
					resp.data = rblock.getData();
				} else {
					resp = new VtMessage(VtMessage.VtRerror, req.tag);
					resp.error = String.format("no block with score %s/%d exists", req.score.toString(), req.type);
				}
				break;
			case VtMessage.VtTwrite:
				Block b = storage.put(req.data, req.type);
				resp = new VtMessage(VtMessage.VtRwrite, req.tag);
				resp.score = b.getScore();
				break;
			case VtMessage.VtTsync:
				storage.sync();
				resp = new VtMessage(VtMessage.VtRsync, req.tag);
				break;
			default:
				break;
			}
			if (resp != null) {
				logger.info("Response: {}", resp.toString());
				serializer.writeMessage(resp, ost);
				ost.flush();
			}
		}
	}
}
