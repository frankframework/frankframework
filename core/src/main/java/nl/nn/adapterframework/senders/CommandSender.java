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
package nl.nn.adapterframework.senders;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.ProcessUtil;

import org.apache.commons.lang.StringUtils;

/**
 * Sender that executes either its input or a fixed line, with all parametervalues appended, as a command.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.CommandSender</td><td>&nbsp;</td></tr>
 * <tr><td>command</td><td>the command to execute. When not specified the input message is supposed to be the command</td><td>&nbsp;</td></tr>
 * <tr><td>commandWithArguments</td><td>whether the command is supposed to contain arguments or not. When the command contains arguments but is executed as a command without arguments you probably get an error=123</td><td>false</td></tr>
 * <tr><td>timeOut</td><td>timeout in seconds. To disable the timeout and keep waiting until the process in completely finished, set this value to 0</td><td>0</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>&nbsp;</td><td>the values of all parameters present are appended as arguments to the command</td></tr>
 * </table>
 * </p>
 * 
 * @version $Id$
 * @since   4.8
 * @author  Gerrit van Brakel
 */
public class CommandSender extends SenderWithParametersBase {
	
	private String command;
	private int timeOut = 0;
	private boolean commandWithArguments = false;
	private boolean synchronous=true;

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		List commandline;
		if (StringUtils.isNotEmpty(getCommand())) {
			commandline = commandToList(getCommand());
		} else {
			commandline = commandToList(message);
		}
		if (paramList!=null) {
			ParameterValueList pvl;
			try {
				pvl = prc.getValues(paramList);
			} catch (ParameterException e) {
				throw new SenderException("Could not extract parametervalues",e);
			}
			for (int i=0; i<pvl.size(); i++) {
				commandline.add(pvl.getParameterValue(i).getValue());
			}
		}
		return ProcessUtil.executeCommand(commandline, timeOut);
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

	public boolean isSynchronous() {
		return synchronous;
	}

	public void setCommand(String string) {
		command = string;
	}
	public String getCommand() {
		return command;
	}

	public void setTimeOut(int timeOut) {
		this.timeOut = timeOut;
	}
	public int getTimeOut() {
		return timeOut;
	}

	public void setCommandWithArguments(boolean commandWithArguments) {
		this.commandWithArguments = commandWithArguments;
	}
	public boolean getCommandWithArguments() {
		return commandWithArguments;
	}

}
