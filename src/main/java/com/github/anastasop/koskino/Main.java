package com.github.anastasop.koskino;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

import com.github.anastasop.koskino.storage.StorageService;

public class Main implements Runnable {
	public static void main(String[] args) {
		new Main().run();
	}
	
	private Logger logger = Logger.getLogger(Main.class.getName());
	private StorageService storage = new StorageService();

	@Override
	public void run() {
		ServerSocket ear = null;
		try {
			ear = new ServerSocket(40000);
			ear.setReuseAddress(true);
		} catch (IOException e) {
			logger.severe("failed to create ServerSocket: " + e.getMessage());
			closeServerSocketAndExit(ear);
		}
		
		logger.info("koskino started");
		for (;;) {
			Socket peer = null;
			try {
				peer = ear.accept();
				logger.info("Accept connection from " + peer.getInetAddress());
			} catch (IOException e) {
				logger.severe("failed to accept a socket: " + e.getMessage());
				closeServerSocketAndExit(ear);
			}
			new Thread(new VtProcessor(peer, storage)).start();
		}
	}
	
	private void closeServerSocketAndExit(ServerSocket s) {
		try {
			if (s != null) {
				s.close();
			}
		} catch (IOException e) {
			logger.info("failed to shutdown server properly: " + e.getMessage());
		}
		System.exit(0);
	}
}
