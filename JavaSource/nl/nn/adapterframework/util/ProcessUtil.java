/*
 * $Log: ProcessUtil.java,v $
 * Revision 1.7  2011-12-08 09:28:25  europe\m168309
 * fixed javadoc
 *
 * Revision 1.6  2011/11/30 13:51:48  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2011/01/26 14:46:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * restored old style methods
 *
 * Revision 1.3  2011/01/26 13:42:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Added timeOut and list style passing of command
 *
 * Revision 1.2  2009/05/06 11:43:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * show error output when available
 *
 * Revision 1.1  2008/02/13 12:57:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.task.TimeoutGuard;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Process execution utilities.
 * 
 * @author  Gerrit van Brakel
 * @since   4.8
 * @version Id
 */
public class ProcessUtil {
	private static Logger log = LogUtil.getLogger(ProcessUtil.class);

	private static String readStream(InputStream stream) throws IOException {
		StringBuffer result = new StringBuffer();

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
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
	 *  
	 * @param timeout: timeout in milliseconds, or 0 to wait indefinetely until the process ends
	 * @param command 
	 * @return
	 * @throws TimeOutException
	 * @throws SenderException
	 */
	public static String executeCommand(List command, int timeout) throws TimeOutException, SenderException {
		String output;
		String errors;

		Process process;
		try {
			process = Runtime.getRuntime().exec((String[])command.toArray(new String[0]));
		} catch (IOException e) {
			throw new SenderException("Could not execute command ["+getCommandLine(command)+"]",e);
		}
		TimeoutGuard tg = new TimeoutGuard("ProcessUtil ");
		tg.activateGuard(timeout);
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
