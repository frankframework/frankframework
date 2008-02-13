/*
 * $Log: ProcessUtil.java,v $
 * Revision 1.1  2008-02-13 12:57:43  europe\L190409
 * first version
 *
 */
package nl.nn.adapterframework.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import nl.nn.adapterframework.core.SenderException;

/**
 * Process execution utilities.
 * 
 * @author  Gerrit van Brakel
 * @since   4.8
 * @version Id
 */
public class ProcessUtil {

	public static String executeCommand(String commandline) throws SenderException {
		StringBuffer result = new StringBuffer();

		Process process;
		try {
			process = Runtime.getRuntime().exec(commandline);
		} catch (IOException e) {
			throw new SenderException("Could not execute command ["+commandline+"]",e);
		}
		// Read the output of the process
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line = null;
		// read() instead of readLine() results in JVM core dumps (this
		// also happens when using InputStream or BufferedInputStream)
		// using WebSphere Studio Application Developer (Windows) 
		// Version: 5.1.2, Build id: 20040506_1735
		try {
			while ((line = bufferedReader.readLine()) != null) {
				result.append(line + "\n");
			}
		} catch (IOException e) {
			throw new SenderException("Could not read output of command ["+commandline+"]",e);
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
			throw new SenderException("Nonzero exit value [" + exitValue + "] for command [" + commandline + "], process output was [" + result.toString()+"]");
		}
		return result.toString();
	}

}
