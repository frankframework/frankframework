/*
 * $Log: FxfListener.java,v $
 * Revision 1.1  2008-02-13 12:53:53  europe\L190409
 * introduction of FxF components
 *
 */
package nl.nn.adapterframework.extensions.fxf;

import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jms.JmsListener;
import nl.nn.adapterframework.util.ProcessUtil;
import nl.nn.adapterframework.util.TransformerPool;

/**
 * Listener for files transferred using the FxF protocol. Message handed to the pipeline is the local filename.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.extensions.fxf.FxfListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setScript(String) script}</td><td>full pathname to the FXF script to be executed to transfer the file</td><td>/usr/local/bin/FXF_get</td></tr>
 * <tr><td>{@link #setDestinationName(String) destinationName}</td><td>name of the JMS destination (queue or topic) to use</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.8
 * @version Id
 */
public class FxfListener extends JmsListener {

	public static final String EXTRACT_TRANSFERNAME_DXPATH="FXF/Transfer_name";
	public static final String EXTRACT_LOCALNAME_DXPATH="FXF/Local_File";
	
	private String script="/usr/local/bin/FXF_get";

	private TransformerPool extractTransfername;
	private TransformerPool extractLocalname;
	
	public void configure() throws ConfigurationException {
		super.configure();
		extractTransfername=TransformerPool.configureTransformer(getLogPrefix(),EXTRACT_TRANSFERNAME_DXPATH,null,"text",false,null);
		extractLocalname=TransformerPool.configureTransformer(getLogPrefix(),EXTRACT_LOCALNAME_DXPATH,null,"text",false,null);
	}
	
	public String getStringFromRawMessage(Object rawMessage, Map threadContext) throws ListenerException {
		String message=super.getStringFromRawMessage(rawMessage, threadContext);
		String transfername;
		String localname;
		try {
			transfername=extractTransfername.transform(message,null);
			localname=extractLocalname.transform(message,null);
		} catch (Exception e) {
			throw new ListenerException("could not extract name from message ["+message+"]");
		}
		String command = getScript()+" "+transfername;
		log.debug(getLogPrefix()+"retrieving local file ["+localname+"] by executing command ["+command+"]");
		try {
			String execResult=ProcessUtil.executeCommand(command);
			log.debug(getLogPrefix()+"output of command ["+execResult+"]");
		} catch (SenderException e1) {
			throw new ListenerException(e1);
		}
		return localname;
	}


	public void setScript(String string) {
		script = string;
	}
	public String getScript() {
		return script;
	}

}
