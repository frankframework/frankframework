/*
 * $Log: CommandSender.java,v $
 * Revision 1.1  2008-02-13 12:56:20  europe\L190409
 * first version
 *
 */
package nl.nn.adapterframework.pipes;

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
	
	private String command;
	private boolean synchronous=true;

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
	
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
		return ProcessUtil.executeCommand(commandline);
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

}
