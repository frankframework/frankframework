/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
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
 * @version $Id$
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
