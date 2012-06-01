/*
 * $Log: ExecutePipe.java,v $
 * Revision 1.9  2012-06-01 10:52:49  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.8  2011/11/30 13:51:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.6  2011/01/26 14:44:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * simplified
 *
 * Revision 1.5  2011/01/26 14:32:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved splitting of command to ProcessUtil
 *
 * Revision 1.4  2011/01/26 11:03:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * adapted to new style procesUtil
 * deprecated
 *
 * Revision 1.3  2008/02/13 12:58:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipeLineSession;
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
 * @deprecated please use CommandSender
 */
public class ExecutePipe extends FixedForwardPipe {
	public static final String version = "$RCSfile: ExecutePipe.java,v $ $Revision: 1.9 $ $Date: 2012-06-01 10:52:49 $";
	
	private String command;
	private String commandSessionKey;
	
	public void configure() throws ConfigurationException {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = getLogPrefix(null)+"The class ["+getClass().getName()+"] has been deprecated. Please change to ["+CommandSender.class.getName()+"]";
		configWarnings.add(log, msg);
		super.configure();
	}

	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
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
