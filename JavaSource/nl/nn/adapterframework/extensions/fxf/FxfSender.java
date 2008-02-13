/*
 * $Log: FxfSender.java,v $
 * Revision 1.1  2008-02-13 12:53:53  europe\L190409
 * introduction of FxF components
 *
 */
package nl.nn.adapterframework.extensions.fxf;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.ProcessUtil;

/**
 * Sender for transferring files using the FxF protocol. Assumes pipe input is local name
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.extensions.fxf.FxfListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setScript(String) script}</td><td>full pathname to the FXF script to be executed to transfer the file</td><td>/usr/local/bin/FXF_put</td></tr>
 * <tr><td>{@link #setDestinationName(String) destinationName}</td><td>name of the JMS destination (queue or topic) to use</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.8
 * @version Id
 */
public class FxfSender extends JmsSender {

	private String script="/usr/local/bin/FXF_init";
	private String transfername;

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException {
//		return super.sendMessage(correlationID, message, prc);
		String command = getScript()+" "+getTransfername() +" "+message;
		return ProcessUtil.executeCommand(command);
	}

	public void setScript(String string) {
		script = string;
	}
	public String getScript() {
		return script;
	}

	public void setTransfername(String string) {
		transfername = string;
	}
	public String getTransfername() {
		return transfername;
	}

}
