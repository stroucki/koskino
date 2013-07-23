package com.github.anastasop.koskino;

import java.io.File;

import org.kohsuke.args4j.Option;

public class MainOptions {
	@Option(name = "-p", aliases = {"--port"}, usage = "server port. Default 40000", required = false)
	public Integer port = 40000;

	@Option(name = "-d", aliases = {"--arenas"}, usage = "directory to store arenas", required = true)
	public File arenaDir;

	@Option(name = "-u", aliases = {"--use"}, usage = "arena name to use", required = true)
	public String arenaName;
}
