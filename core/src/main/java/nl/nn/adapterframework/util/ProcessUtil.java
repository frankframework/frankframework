/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.task.TimeoutGuard;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;

/**
 * Process execution utilities.
 * 
 * @author  Gerrit van Brakel
 * @since   4.8
 */
public class ProcessUtil {
	private static Logger log = LogUtil.getLogger(ProcessUtil.class);

	private static String readStream(InputStream stream) throws IOException {
		StringBuffer result = new StringBuffer();

		BufferedReader bufferedReader = new BufferedReader(StreamUtil.getCharsetDetectingInputStreamReader(stream));
		String line = null;
		// read() instead of readLine() results in JVM core dumps (this
		// also happens when using InputStream or BufferedInputStream)
		// using WebSphere Studio Application Developer (Windows) 
		// Version: 5.1.2, Build id: 20040506_1735
		while ((line = bufferedReader.readLine()) != null) {
			result.append(line + "\n");
		}
		return result.toString();
	}

	protected static String getCommandLine(List command) {
		if (command==null || command.isEmpty()) {
			return "";
		}
		String result=(String)command.get(0);
		for (int i=1;i<command.size();i++) {
			result+=" "+command.get(i);
		}
		return result;
	}

	public static List splitUpCommandString(String command) {
		List list = new ArrayList();
		StringTokenizer stringTokenizer = new StringTokenizer(command);
		while (stringTokenizer.hasMoreElements()) {
			list.add(stringTokenizer.nextToken());
		}
		return list;
	}


	public static String executeCommand(String command) throws SenderException {
		try {
			return executeCommand(splitUpCommandString(command),0);
		} catch (TimeOutException e) {
			throw new SenderException(e);
		}
	}
	public static String executeCommand(String command, int timeout) throws TimeOutException, SenderException {
		return executeCommand(splitUpCommandString(command),timeout);
	}
	/**
	 * Execute a command as a process in the operating system. 
	 * Timeout is passed in seconds, or 0 to wait indefinitely until the process ends
	 */
	public static String executeCommand(List command, int timeout) throws TimeOutException, SenderException {
		String output;
		String errors;

		final Process process;
		try {
			process = Runtime.getRuntime().exec((String[])command.toArray(new String[0]));
		} catch (Throwable t) {
			throw new SenderException("Could not execute command ["+getCommandLine(command)+"]",t);
		}
		TimeoutGuard tg = new TimeoutGuard("ProcessUtil") {

			@Override
			protected void kill() {
				process.destroy();
			}
			
		};
		tg.activateGuard(timeout) ;
		try {
			// Wait until the process is completely finished, or timeout is expired
			process.waitFor();
		} catch(InterruptedException e) {
			if (tg.threadKilled()) {
				throw new TimeOutException("command ["+getCommandLine(command)+"] timed out",e);
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
			log.warn("command ["+getCommandLine(command)+"] had error output [" + errors + "]");
		}
		return output;
	}

}
