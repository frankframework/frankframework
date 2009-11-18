/*
 * $Log: CommandSender.java,v $
 * Revision 1.2  2009-11-18 17:28:03  m00f069
 * Added senders to IbisDebugger
 *
 * Revision 1.1  2008/08/06 16:36:39  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved from pipes to senders package
 *
 */
package nl.nn.adapterframework.senders;

import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.debug.IbisDebugger;
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
 * </table>
 * </p>
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>&nbsp;</td><td>the values of all parameters present are concatenated to the command line</td></tr>
 * </table>
 * </p>
 * 
 * @version Id
 * @since   4.8
 * @author  Gerrit van Brakel
 */
public class CommandSender extends SenderWithParametersBase {
	private IbisDebugger ibisDebugger;
	
	private String command;
	private boolean synchronous=true;

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		if (log.isDebugEnabled() && ibisDebugger!=null) {
			message = ibisDebugger.senderInput(this, correlationID, message);
		}
	
		String commandline;
		if (StringUtils.isNotEmpty(getCommand())) {
			commandline=getCommand();
		} else {
			commandline=message;
		}
		if (paramList!=null) {
			ParameterValueList pvl;
			try {
				pvl = prc.getValues(paramList);
			} catch (ParameterException e) {
				throw new SenderException("Could not extract parametervalues",e);
			}
			for (int i=0; i<pvl.size(); i++) {
				commandline += " "+pvl.getParameterValue(i);
			}
		}
		String result = ProcessUtil.executeCommand(commandline);
		if (log.isDebugEnabled() && ibisDebugger!=null) {
			result = ibisDebugger.senderOutput(this, correlationID, result);
		}
		return result;
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
	
	public void setIbisDebugger(IbisDebugger ibisDebugger) {
		this.ibisDebugger = ibisDebugger;
	}

}
