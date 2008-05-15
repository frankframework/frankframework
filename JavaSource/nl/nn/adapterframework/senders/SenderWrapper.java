/*
 * $Log: SenderWrapper.java,v $
 * Revision 1.1  2008-05-15 15:08:26  europe\L190409
 * created senders package
 * moved some sender to senders package
 * created special senders
 *
 */
package nl.nn.adapterframework.senders;

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
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class SenderWrapper extends SenderWrapperBase {
	private ISender sender;
	
	protected boolean isSenderConfigured() {
		return getSender()!=null;
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
