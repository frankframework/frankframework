/*
 * $Log: CommandSender.java,v $
 * Revision 1.11  2012-04-19 16:15:58  m00f069
 * Added default value in docs for commandWithArguments
 *
 * Revision 1.10  2011/11/30 13:52:00  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.8  2011/01/26 14:32:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved splitting of command to ProcessUtil
 *
 * Revision 1.7  2011/01/26 11:05:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Added timeOut and commandWithArguments attributes
 *
 * Revision 1.6  2010/09/07 15:55:13  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 * Revision 1.5  2010/03/10 14:30:04  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * rolled back testtool adjustments (IbisDebuggerDummy)
 *
 * Revision 1.3  2009/12/04 18:23:34  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added ibisDebugger.senderAbort and ibisDebugger.pipeRollback
 *
 * Revision 1.2  2009/11/18 17:28:03  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added senders to IbisDebugger
 *
 * Revision 1.1  2008/08/06 16:36:39  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved from pipes to senders package
 *
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
 * @version Id
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
