package com.github.anastasop.koskino;

import java.io.File;

import org.kohsuke.args4j.Option;

public class MainOptions {
	@Option(name = "-p", aliases = {"--port"}, usage = "server port. Default 40000", required = false)
	public Integer port = 40000;

	@Option(name = "-s", aliases = {"--storage"}, usage = "storage file to use", required = true)
	public File storageFile;
}
