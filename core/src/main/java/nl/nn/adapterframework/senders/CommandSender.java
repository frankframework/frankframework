/*
   Copyright 2013 Nationale-Nederlanden, 2021 WeAreFrank!

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
package nl.nn.adapterframework.senders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ProcessUtil;

/**
 * Sender that executes either its input or a fixed line, with all parametervalues appended, as a command.
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>&nbsp;</td><td>the values of all parameters present are appended as arguments to the command</td></tr>
 * </table>
 * </p>
 *
 * @since   4.8
 * @author  Gerrit van Brakel
 */
public class CommandSender extends SenderWithParametersBase {

	private String command;
	private int timeOut = 0;
	private boolean commandWithArguments = false;
	private boolean synchronous=true;

	@Override
	public Message sendMessage(Message message, IPipeLineSession session) throws SenderException, TimeOutException {
		List commandline;
		if (StringUtils.isNotEmpty(getCommand())) {
			commandline = commandToList(getCommand());
		} else {
			try {
				commandline = commandToList(message.asString());
			} catch (IOException e) {
				throw new SenderException(getLogPrefix(),e);
			}
		}
		if (paramList!=null) {
			ParameterValueList pvl;
			try {
				pvl = paramList.getValues(message, session);
			} catch (ParameterException e) {
				throw new SenderException("Could not extract parametervalues",e);
			}
			for (int i=0; i<pvl.size(); i++) {
				commandline.add(pvl.getParameterValue(i).getValue());
			}
		}
		return new Message(ProcessUtil.executeCommand(commandline, timeOut));
	}

	private List commandToList(String command) {
		List list;
		if (commandWithArguments) {
			list=ProcessUtil.splitUpCommandString(command);
		} else {
			list = new ArrayList();
			list.add(command);
		}
		return list;
	}

	@Override
	public boolean isSynchronous() {
		return synchronous;
	}

	@IbisDoc({ "1", "The command to be executed. Note: Executing a command in WAS requires <<ALL FILES>> execute permission to avoid that provide the absolute path of the command. Absolute path can be found with the following command 'which -a {commandName}'", "" })
	public void setCommand(String string) {
		command = string;
	}
	public String getCommand() {
		return command;
	}

	@IbisDoc({ "2", "The number of seconds to execute a command. If the limit is exceeded, a TimeoutException is thrown. A value of 0 means execution time is not limited", "0" })
	public void setTimeOut(int timeOut) {
		this.timeOut = timeOut;
	}
	public int getTimeOut() {
		return timeOut;
	}

	@IbisDoc({ "3", "In case the command that will be executed contains arguments then this flag should be set to true", "false" })
	public void setCommandWithArguments(boolean commandWithArguments) {
		this.commandWithArguments = commandWithArguments;
	}
	public boolean getCommandWithArguments() {
		return commandWithArguments;
	}

}
