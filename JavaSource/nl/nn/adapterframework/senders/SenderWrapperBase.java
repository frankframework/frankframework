/*
 * $Log: SenderWrapperBase.java,v $
 * Revision 1.12  2011-11-30 13:52:00  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.10  2010/09/13 14:09:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed processor
 * added cache facility
 * implemented open() and close()
 *
 * Revision 1.9  2010/09/07 15:55:13  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 * Revision 1.8  2010/03/10 14:30:05  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * rolled back testtool adjustments (IbisDebuggerDummy)
 *
 * Revision 1.6  2009/12/29 14:37:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified imports to reflect move of statistics classes to separate package
 *
 * Revision 1.5  2009/12/04 18:23:34  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added ibisDebugger.senderAbort and ibisDebugger.pipeRollback
 *
 * Revision 1.4  2009/11/18 17:28:03  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added senders to IbisDebugger
 *
 * Revision 1.3  2008/06/03 15:51:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed superfluous code
 *
 * Revision 1.2  2008/05/21 10:42:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * same attributenames as Pipes
 *
 * Revision 1.1  2008/05/15 15:08:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * created senders package
 * moved some sender to senders package
 * created special senders
 *
 */
package nl.nn.adapterframework.senders;

import nl.nn.adapterframework.cache.ICacheAdapter;
import nl.nn.adapterframework.cache.ICacheEnabled;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.processors.SenderWrapperProcessor;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.util.ClassUtils;

import org.apache.commons.lang.StringUtils;

/**
 * Baseclasse for Wrappers for senders, that allows to get input from a session variable, and to store output in a session variable.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.senders.SenderWrapperBase</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setGetInputFromFixedValue(String) getInputFromFixedValue}</td><td>when set, this fixed value is taken as input, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPreserveInput(boolean) preserveInput}</td><td>when set <code>true</code>, the input of a pipe is restored before processing the next one</td><td>false</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>&lt;cache ... /&gt;</td><td>optional {@link nl.nn.adapterframework.cache.EhCache cache} definition</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public abstract class SenderWrapperBase extends SenderWithParametersBase implements HasStatistics, ICacheEnabled {

	private String getInputFromSessionKey; 
	private String getInputFromFixedValue=null;
	private String storeResultInSessionKey; 
	private boolean preserveInput=false; 
	protected SenderWrapperProcessor senderWrapperProcessor;
	
	private ICacheAdapter cache=null;

	
	public void configure() throws ConfigurationException {
		super.configure();
		if (!isSenderConfigured()) {
			throw new ConfigurationException(getLogPrefix()+"must have at least a sender configured");
		}
		if (StringUtils.isNotEmpty(getGetInputFromSessionKey()) && StringUtils.isNotEmpty(getGetInputFromFixedValue())) {
			throw new ConfigurationException(getLogPrefix()+"cannot have both attributes inputFromSessionKey and inputFromFixedValue configured");
		}
		if (cache!=null) {
			cache.configure(getName());
		}
	}

	public void open() throws SenderException {
		if (cache!=null) {
			cache.open();
		}
		super.open();
	}

	public void close() throws SenderException {
		try {
			super.close();
		} finally {
			if (cache!=null) {
				cache.close();
			}
		}
	}

	protected abstract boolean isSenderConfigured();

	public abstract String doSendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException; 

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		return senderWrapperProcessor.sendMessage(this, correlationID, message, prc);
	}

	public String getLogPrefix() {
		return ClassUtils.nameOf(this)+" ["+getName()+"] ";
	}

	public void registerCache(ICacheAdapter cache) {
		this.cache=cache;
	}
	public ICacheAdapter getCache() {
		return cache;
	}

	
	public abstract boolean isSynchronous() ;

	public abstract void setSender(ISender sender);
	
	public void setGetInputFromSessionKey(String string) {
		getInputFromSessionKey = string;
	}
	public String getGetInputFromSessionKey() {
		return getInputFromSessionKey;
	}

	public void setGetInputFromFixedValue(String string) {
		getInputFromFixedValue = string;
	}
	public String getGetInputFromFixedValue() {
		return getInputFromFixedValue;
	}

	public void setStoreResultInSessionKey(String string) {
		storeResultInSessionKey = string;
	}
	public String getStoreResultInSessionKey() {
		return storeResultInSessionKey;
	}

	public void setPreserveInput(boolean preserveInput) {
		this.preserveInput = preserveInput;
	}
	public boolean isPreserveInput() {
		return preserveInput;
	}

	public void setSenderWrapperProcessor(SenderWrapperProcessor senderWrapperProcessor) {
		this.senderWrapperProcessor = senderWrapperProcessor;
	}

}
