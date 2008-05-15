/*
 * $Log: SenderWrapperBase.java,v $
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
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.HasStatistics;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Baseclasse for Wrappers for senders, that allows to get input from a session variable, and to store output in a session variable.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public abstract class SenderWrapperBase extends SenderWithParametersBase implements HasStatistics {
	protected Logger log = LogUtil.getLogger(this);

	private String getFromSession; 
	private String putInSession; 
	private boolean inputToOutput=false; 


	public void configure() throws ConfigurationException {
		super.configure();
		if (!isSenderConfigured()) {
			throw new ConfigurationException(getLogPrefix()+"must have at least a sender configured");
		}
	}

	protected abstract boolean isSenderConfigured();

	protected abstract String doSendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException; 

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		String senderInput=message;
		if (StringUtils.isNotEmpty(getGetFromSession())) {
			senderInput=(String)prc.getSession().get(getGetFromSession());
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"set contents of session variable ["+getGetFromSession()+"] as input ["+senderInput+"]");
		}
		String result = doSendMessage(correlationID, senderInput, prc);
		if (StringUtils.isNotEmpty(getPutInSession())) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"storing results in session variable ["+getPutInSession()+"]");
			prc.getSession().put(getPutInSession(),result);
		}
		return isInputToOutput()?message:result;
	}

	protected String getLogPrefix() {
		return ClassUtils.nameOf(this)+" ["+getName()+"] ";
	}

	public abstract boolean isSynchronous() ;

	public abstract void setSender(ISender sender);
	
	public void setGetFromSession(String string) {
		getFromSession = string;
	}
	public String getGetFromSession() {
		return getFromSession;
	}

	public void setPutInSession(String string) {
		putInSession = string;
	}
	public String getPutInSession() {
		return putInSession;
	}

	public void setInputToOutput(boolean b) {
		inputToOutput = b;
	}
	public boolean isInputToOutput() {
		return inputToOutput;
	}

}
