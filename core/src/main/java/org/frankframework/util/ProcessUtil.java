/*
   Copyright 2013 Nationale-Nederlanden, 2023 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.task.TimeoutGuard;

/**
 * Process execution utilities.
 *
 * @author  Gerrit van Brakel
 * @since   4.8
 */
public class ProcessUtil {
	private static final Logger log = LogUtil.getLogger(ProcessUtil.class);

	private static String readStream(InputStream stream) throws IOException {
		StringBuilder result = new StringBuilder();

		BufferedReader bufferedReader = new BufferedReader(StreamUtil.getCharsetDetectingInputStreamReader(stream));
		String line = null;
		// read() instead of readLine() results in JVM core dumps (this
		// also happens when using InputStream or BufferedInputStream)
		// using WebSphere Studio Application Developer (Windows)
		// Version: 5.1.2, Build id: 20040506_1735
		while ((line = bufferedReader.readLine()) != null) {
			result.append(line).append("\n");
		}
		return result.toString();
	}

	protected static String getCommandLine(List<String> command) {
		if (command==null || command.isEmpty()) {
			return "";
		}
		StringBuilder result = new StringBuilder(command.get(0));
		for (int i = 1; i < command.size(); i++) {
			result.append(" ").append(command.get(i));
		}
		return result.toString();
	}

	public static List<String> splitUpCommandString(String command) {
		return Arrays.asList(command.split("(\\s|\f)+"));
	}

	public static String executeCommand(String command) throws SenderException {
		try {
			return executeCommand(splitUpCommandString(command),0);
		} catch (TimeoutException e) {
			throw new SenderException(e);
		}
	}

	/**
	 * Execute a command as a process in the operating system.
	 * Timeout is passed in seconds, or 0 to wait indefinitely until the process ends
	 */
	public static String executeCommand(List<String> command, int timeout) throws TimeoutException, SenderException {
		String output;
		String errors;

		final Process process;
		try {
			process = Runtime.getRuntime().exec(command.toArray(new String[0]));
		} catch (Throwable t) {
			throw new SenderException("Could not execute command [" + getCommandLine(command) + "]", t);
		}
		TimeoutGuard tg = new TimeoutGuard("ProcessUtil") {

			@Override
			protected void abort() {
				process.destroy();
			}

		};
		tg.activateGuard(timeout) ;
		try {
			// Wait until the process is completely finished, or timeout is expired
			process.waitFor();
		} catch(InterruptedException e) {
			if (tg.threadKilled()) {
				throw new TimeoutException("command ["+getCommandLine(command)+"] timed out",e);
			} else {
				throw new SenderException("command ["+getCommandLine(command)+"] interrupted while waiting for process",e);
			}
		} finally {
			tg.cancel();
		}
		// Read the output of the process
		try {
			output=readStream(process.getInputStream());
		} catch (IOException e) {
			throw new SenderException("Could not read output of command ["+getCommandLine(command)+"]",e);
		}
		// Read the errors of the process
		try {
			errors=readStream(process.getErrorStream());
		} catch (IOException e) {
			throw new SenderException("Could not read errors of command ["+getCommandLine(command)+"]",e);
		}
		// Throw an exception if the command returns an error exit value
		int exitValue = process.exitValue();
		if (exitValue != 0) {
			throw new SenderException("Nonzero exit value [" + exitValue + "] for command  ["+getCommandLine(command)+"], process output was [" + output + "], error output was [" + errors + "]");
		}
		if (StringUtils.isNotEmpty(errors)) {
			log.warn("command [{}] had error output [{}]", getCommandLine(command), errors);
		}
		return output;
	}

}
