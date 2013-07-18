package com.github.anastasop.koskino;

import org.kohsuke.args4j.Option;

public class MainOptions {
	@Option(name = "-p", aliases = {"--port"}, usage = "server port. Default 40000", required = false)
	public Integer port = 40000;
}
