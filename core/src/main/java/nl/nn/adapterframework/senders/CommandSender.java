/*
   Copyright 2013 Nationale-Nederlanden, 2021, 2022 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ProcessUtil;

/**
 * Sender that executes either its input or a fixed line, with all parametervalues appended, as a command.
 *
 * @ff.parameters All parameters present are appended as arguments to the command.
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
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
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
			for(ParameterValue pv : pvl) {
				commandline.add(pv.getValue());
			}
		}
		return new SenderResult(ProcessUtil.executeCommand(commandline, timeOut));
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

	@IbisDoc({"The command to be executed. Note: Executing a command in WAS requires &lt;&lt;ALL FILES&gt;&gt; execute permission to avoid that provide the absolute path of the command. Absolute path can be found with the following command 'which -a {commandName}'", "" })
	public void setCommand(String string) {
		command = string;
	}
	public String getCommand() {
		return command;
	}

	@IbisDoc({"The number of seconds to execute a command. If the limit is exceeded, a TimeoutException is thrown. A value of 0 means execution time is not limited", "0" })
	public void setTimeOut(int timeOut) {
		this.timeOut = timeOut;
	}
	public int getTimeOut() {
		return timeOut;
	}

	@IbisDoc({"In case the command that will be executed contains arguments then this flag should be set to true", "false" })
	public void setCommandWithArguments(boolean commandWithArguments) {
		this.commandWithArguments = commandWithArguments;
	}
	public boolean getCommandWithArguments() {
		return commandWithArguments;
	}

}
