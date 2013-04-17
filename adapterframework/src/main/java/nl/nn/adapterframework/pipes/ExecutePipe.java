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
 * @version $Id$
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
