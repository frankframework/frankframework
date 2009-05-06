/*
 * $Log: ProcessUtil.java,v $
 * Revision 1.2  2009-05-06 11:43:53  L190409
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

import nl.nn.adapterframework.core.SenderException;

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

	public static String executeCommand(String commandline) throws SenderException {
		String output;
		String errors;

		Process process;
		try {
			process = Runtime.getRuntime().exec(commandline);
		} catch (IOException e) {
			throw new SenderException("Could not execute command ["+commandline+"]",e);
		}
		// Read the output of the process
		try {
			output=readStream(process.getInputStream());
		} catch (IOException e) {
			throw new SenderException("Could not read output of command ["+commandline+"]",e);
		}
		// Read the errors of the process
		try {
			errors=readStream(process.getErrorStream());
		} catch (IOException e) {
			throw new SenderException("Could not read errors of command ["+commandline+"]",e);
		}

		// Wait until the process is completely finished
		try {
			process.waitFor();
		} catch(InterruptedException e) {
			throw new SenderException("Interrupted while waiting for process",e);
		}
		// Throw an exception if the command returns an error exit value
		int exitValue = process.exitValue();
		if (exitValue != 0) {
			throw new SenderException("Nonzero exit value [" + exitValue + "] for command [" + commandline + "], process output was [" + output + "], error output was [" + errors + "]");
		}
		if (StringUtils.isNotEmpty(errors)) {
			log.warn("command [" + commandline + "] had error output [" + errors + "]");
		}
		return output;
	}

}
