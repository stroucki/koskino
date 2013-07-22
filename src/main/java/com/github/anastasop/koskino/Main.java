package com.github.anastasop.koskino;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.anastasop.koskino.storage.FileStorageService;
import com.github.anastasop.koskino.storage.StorageService;

public class Main implements Runnable {
	public static void main(String[] args) throws IOException {
		MainOptions options = new MainOptions();
		CmdLineParser parser = new CmdLineParser(options);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.printf("Usage:%n");
			parser.printUsage(System.err);
			System.exit(2);
		}
		options.storageFile.createNewFile();
		Main m = new Main();
		m.port = options.port;
		m.storage = FileStorageService.forFile(options.storageFile);
		m.run();
	}
	
	private Logger logger = LoggerFactory.getLogger(Main.class);
	private StorageService storage;
	private int port;
	
	@Override
	public void run() {
		ServerSocket ear = null;
		try {
			ear = new ServerSocket(port);
			ear.setReuseAddress(true);
		} catch (IOException e) {
			logger.error("failed to create ServerSocket: {}", e.getMessage());
			closeServerSocketAndExit(ear);
		}
		
		logger.info("koskino listens at {}", ear.getLocalSocketAddress());
		for (;;) {
			Socket peer = null;
			try {
				peer = ear.accept();
				logger.info("Accept connection from " + peer.getInetAddress());
			} catch (IOException e) {
				logger.error("failed to accept a socket: {}", e.getMessage());
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
			storage.close();
		} catch (Exception e) {
			logger.info("failed to shutdown server properly: " + e.getMessage());
		}
		System.exit(0);
	}
}
