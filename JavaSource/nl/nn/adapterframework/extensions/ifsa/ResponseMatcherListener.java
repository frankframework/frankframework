/*
 * $Log: ResponseMatcherListener.java,v $
 * Revision 1.1  2004-11-08 08:31:44  L190409
 * first version
 *
 */
package nl.nn.adapterframework.extensions.ifsa;

import java.util.HashMap;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ICorrelatedPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.TimeOutException;

/**
 * @author Gerrit van Brakel
 * @version Id
 * @since  4.2d
 */
public class ResponseMatcherListener implements ICorrelatedPullingListener {

	private String name;
	private long timeOut=5000;
	private String matcherName="default";
	
	private ResponseMatcher matcher;

	public void configure() throws ConfigurationException {
		matcher = ResponseMatcher.getMatcher(getMatcher());
	}

	public void open() throws ListenerException {
	}

	public void close() throws ListenerException {
	}

	public Object getRawMessage(String correlationId, HashMap threadContext) throws ListenerException, TimeOutException {
		try {
			return matcher.receive(correlationId,getTimeOut());
		} catch (InterruptedException e) {
			throw new ListenerException("Thread interrupted waiting for response with correlationId ["+correlationId+"]",e);
		}
	}

	public HashMap openThread() throws ListenerException {
		return null;
	}

	public void closeThread(HashMap threadContext) throws ListenerException {
	}

	public Object getRawMessage(HashMap threadContext) throws ListenerException {
		return null;
	}


	public String getIdFromRawMessage(Object rawMessage, HashMap context) throws ListenerException {
		return null;
	}

	public String getStringFromRawMessage(Object rawMessage, HashMap context) throws ListenerException {
		return (String) rawMessage;
	}

	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessage, HashMap context) throws ListenerException {
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
