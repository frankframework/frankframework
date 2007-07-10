/*
 * $Log: ExecutePipe.java,v $
 * Revision 1.2  2007-07-10 07:52:29  europe\L190409
 * cosmetic changes
 *
 * Revision 1.1  2006/08/22 12:56:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.pipes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

import org.apache.commons.lang.StringUtils;

/**
 * Executes a command.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setCommand(String) command}</td><td>The command to execute (if command and commandSessionKey are empty, the command is taken from the input of the pipe)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCommandSessionKey(String) commandSessionKey}</td><td>The session key that holds the command to execute</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @version Id
 * @author Jaco de Groot (***@dynasol.nl)
 */
public class ExecutePipe extends FixedForwardPipe {
	public static final String version = "$RCSfile: ExecutePipe.java,v $ $Revision: 1.2 $ $Date: 2007-07-10 07:52:29 $";
	
	private String command;
	private String commandSessionKey;

	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		StringBuffer result = new StringBuffer();
		String command;
		if (StringUtils.isNotEmpty(getCommand())) {
			command = getCommand();
		} else if (StringUtils.isNotEmpty(getCommandSessionKey())) {
			command = (String)session.get(getCommandSessionKey());
		} else {
			command = (String)input;
		}
		try {
			Process process = Runtime.getRuntime().exec(command);
			// Read the output of the process
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = null;
			// read() instead of readLine() results in JVM core dumps (this
			// also happens when using InputStream or BufferedInputStream)
			// using WebSphere Studio Application Developer (Windows) 
			// Version: 5.1.2, Build id: 20040506_1735
			while ((line = bufferedReader.readLine()) != null) {
				result.append(line + "\n");
			}
			// Wait until the process is completely finished
			try {
				process.waitFor();
			} catch(InterruptedException e) {
				throw new PipeRunException(this, "Interrupted while waiting for process",e);
			}
			// Throw an exception if the command returns an error exit value
			int exitValue = process.exitValue();
			if (exitValue != 0) {
				throw new PipeRunException(this, "Error exit value '" + exitValue + "' (not 0) for command '" + command + "', process output was: " + result.toString());
			}
		} catch(IOException e) {
			throw new PipeRunException(this, "Error executing command [" + command + "]", e);
		}
		// Return result
		return new PipeRunResult(getForward(), result.toString());
	}

	public void setCommand(String command) {
		this.command = command;
	}
	public String getCommand() {
		return command;
	}

	public void setCommandSessionKey(String commandSessionKey) {
		this.commandSessionKey = commandSessionKey;
	}
	public String getCommandSessionKey() {
		return commandSessionKey;
	}
}
