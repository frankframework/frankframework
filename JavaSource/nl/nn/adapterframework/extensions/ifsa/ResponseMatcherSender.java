/*
 * $Log: ResponseMatcherSender.java,v $
 * Revision 1.1  2004-11-08 08:31:44  L190409
 * first version
 *
 */
package nl.nn.adapterframework.extensions.ifsa;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;

/**
 * @author Gerrit van Brakel
 * @since  4.2d
 *
 */
public class ResponseMatcherSender implements ISender {
	
	private String name;
	private long timeOut=1000;
	private String matcherName="default";
	
	private ResponseMatcher matcher;

	public void configure() throws ConfigurationException {
		matcher = ResponseMatcher.getMatcher(getMatcher());
	}

	public void open() throws SenderException {
	}

	public void close() throws SenderException {
		matcher.clear();
	}

	public boolean isSynchronous() {
		return false;
	}

	public String sendMessage(String correlationID, String message) throws SenderException, TimeOutException {
		try {
			matcher.deliver(correlationID,message, getTimeOut());
		} catch (InterruptedException e) {
			throw new SenderException("Thread interrupted sending response for message with correlationId ["+correlationID+"]",e);
		}
		return correlationID;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMatcher() {
		return matcherName;
	}

	public void setMatcher(String matcher) {
		this.matcherName = matcher;
	}


	public long getTimeOut() {
		return timeOut;
	}

	public void setTimeOut(long timeout) {
		this.timeOut = timeout;
	}

}
