/*
 * $Log: ExecutePipe.java,v $
 * Revision 1.3  2008-02-13 12:58:41  europe\L190409
 * now uses ProcessUtils
 *
 * Revision 1.2  2007/07/10 07:52:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.1  2006/08/22 12:56:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.ProcessUtil;

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
	public static final String version = "$RCSfile: ExecutePipe.java,v $ $Revision: 1.3 $ $Date: 2008-02-13 12:58:41 $";
	
	private String command;
	private String commandSessionKey;

	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		String command;
		if (StringUtils.isNotEmpty(getCommand())) {
			command = getCommand();
		} else if (StringUtils.isNotEmpty(getCommandSessionKey())) {
			command = (String)session.get(getCommandSessionKey());
		} else {
			command = (String)input;
		}
		try {
			return new PipeRunResult(getForward(), ProcessUtil.executeCommand(command));
		} catch(SenderException e) {
			throw new PipeRunException(this, "Error executing command", e);
		}
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
