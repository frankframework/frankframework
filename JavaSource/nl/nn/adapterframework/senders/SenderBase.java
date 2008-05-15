/*
 * $Log: SenderBase.java,v $
 * Revision 1.1  2008-05-15 15:08:26  europe\L190409
 * created senders package
 * moved some sender to senders package
 * created special senders
 *
 */
package nl.nn.adapterframework.senders;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * Baseclass for senders.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Sender</td><td>&nbsp;</td></tr>
 * </table>
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public abstract class SenderBase implements ISender {
	protected Logger log = LogUtil.getLogger(this);

	private String name;

	public void configure() throws ConfigurationException {
	}

	public void open() throws SenderException {
	}

	public void close() throws SenderException {
	}

	public abstract String sendMessage(String correlationID, String message) throws SenderException, TimeOutException;

	public boolean isSynchronous() {
		return true;
	}

	protected String getLogPrefix(){
		return "Sender ["+getName()+"] ";
	}

	public void setName(String name) {
		this.name=name;
	}
	public String getName() {
		return name;
	}

}
