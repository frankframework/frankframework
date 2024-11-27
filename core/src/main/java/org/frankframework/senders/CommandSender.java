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
package org.frankframework.senders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nonnull;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.util.ProcessUtil;

/**
 * Sender that executes either its input or a fixed line, with all parametervalues appended, as a command.
 *
 * @ff.parameters All parameters present are appended as arguments to the command.
 *
 * @since   4.8
 * @author  Gerrit van Brakel
 */
public class CommandSender extends AbstractSenderWithParameters {

	private String command;
	@Getter private int timeout = 0;
	private boolean commandWithArguments = false;
	private final boolean synchronous = true;

	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
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
		return new SenderResult(ProcessUtil.executeCommand(commandline, timeout));
	}

	private List<String> commandToList(String command) {
		List<String> list;
		if (commandWithArguments) {
			list=ProcessUtil.splitUpCommandString(command);
		} else {
			list = new ArrayList<>();
			list.add(command);
		}
		return list;
	}

	@Override
	public boolean isSynchronous() {
		return synchronous;
	}

	/** The command to be executed. Note: Executing a command in WAS requires &lt;&lt;ALL FILES&gt;&gt; execute permission to avoid that provide the absolute path of the command. Absolute path can be found with the following command 'which -a {commandName}' */
	public void setCommand(String string) {
		command = string;
	}
	public String getCommand() {
		return command;
	}

	/**
	 * The number of seconds to execute a command. If the limit is exceeded, a TimeoutException is thrown. A value of 0 means execution time is not limited
	 * @ff.default 0
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	/**
	 * In case the command that will be executed contains arguments then this flag should be set to true
	 * @ff.default false
	 */
	public void setCommandWithArguments(boolean commandWithArguments) {
		this.commandWithArguments = commandWithArguments;
	}
	public boolean getCommandWithArguments() {
		return commandWithArguments;
	}

}
