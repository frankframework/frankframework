/*
 * $Log: SenderWrapper.java,v $
 * Revision 1.3  2008-07-17 16:18:46  europe\L190409
 * call sender.configure in configure
 *
 * Revision 1.2  2008/05/21 10:41:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added documentation
 *
 * Revision 1.1  2008/05/15 15:08:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * created senders package
 * moved some sender to senders package
 * created special senders
 *
 */
package nl.nn.adapterframework.senders;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.HasStatistics;
import nl.nn.adapterframework.util.StatisticsKeeperIterationHandler;

/**
 * Wrapper for senders, that allows to get input from a session variable, and to store output in a session variable.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.senders.SenderWrapper</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setGetInputFromFixedValue(String) getInputFromFixedValue}</td><td>when set, this fixed value is taken as input, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPreserveInput(boolean) preserveInput}</td><td>when set <code>true</code>, the input of a pipe is restored before processing the next one</td><td>false</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ISender sender}</td><td>specification of sender to send messages with</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the senderwrapper will be handed to the sender, if this is a {@link IParameterizedSender}</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class SenderWrapper extends SenderWrapperBase {
	private ISender sender;
	
	protected boolean isSenderConfigured() {
		return getSender()!=null;
	}

	public void configure() throws ConfigurationException {
		super.configure();
		getSender().configure();
	}
	public void open() throws SenderException {
		getSender().open();
	}
	public void close() throws SenderException {
		getSender().close();
	}

	protected String doSendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		String result;
		if (sender instanceof ISenderWithParameters) {
			result = ((ISenderWithParameters)sender).sendMessage(correlationID,message,prc);
		} else {
			result = sender.sendMessage(correlationID,message);
		}
		return result;
	}

	public void iterateOverStatistics(StatisticsKeeperIterationHandler hski, Object data) {
		if (getSender() instanceof HasStatistics) {
			((HasStatistics)getSender()).iterateOverStatistics(hski,data);
		}
	}

	public boolean isSynchronous() {
		return getSender().isSynchronous();
	}

	public void setSender(ISender sender) {
		this.sender=sender;
	}
	protected ISender getSender() {
		return sender;
	}

}
