/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020, 2021 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.cache.ICache;
import nl.nn.adapterframework.cache.ICacheEnabled;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.processors.SenderWrapperProcessor;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * Baseclass for Wrappers for senders, that allows to get input from a session variable, and to store output in a session variable.
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>&lt;cache ... /&gt;</td><td>optional {@link nl.nn.adapterframework.cache.EhCache cache} definition</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public abstract class SenderWrapperBase extends SenderWithParametersBase implements HasStatistics, ICacheEnabled<String,String> {

	private @Getter String getInputFromSessionKey;
	private @Getter String getInputFromFixedValue=null;
	private @Getter String storeResultInSessionKey;
	private @Getter String storeInputInSessionKey;
	private @Getter boolean preserveInput=false;
	
	protected @Setter SenderWrapperProcessor senderWrapperProcessor;
	private @Getter @Setter ICache<String,String> cache=null;

	@Override
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

	@Override
	public void open() throws SenderException {
		if (cache!=null) {
			cache.open();
		}
		super.open();
	}

	@Override
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

	public abstract Message doSendMessage(Message message, PipeLineSession session) throws SenderException, TimeOutException; 

	@Override
	public Message sendMessage(Message message, PipeLineSession session) throws SenderException, TimeOutException {
		if (senderWrapperProcessor!=null) {
			return senderWrapperProcessor.sendMessage(this, message, session);
		}
		return doSendMessage(message, session);
	}

	@Override
	public String getLogPrefix() {
		return ClassUtils.nameOf(this)+" ["+getName()+"] ";
	}

	@Override
	public abstract boolean isSynchronous() ;
	public abstract void setSender(ISender sender);


	@IbisDoc({"1", "If set, input is taken from this session key, instead of regular input", ""})
	public void setGetInputFromSessionKey(String string) {
		getInputFromSessionKey = string;
	}

	@IbisDoc({"2", "If set, this fixed value is taken as input, instead of regular input", ""})
	public void setGetInputFromFixedValue(String string) {
		getInputFromFixedValue = string;
	}

	@IbisDoc({"3", "If set <code>true</code>, the input of a pipe is restored before processing the next one", "false"})
	public void setPreserveInput(boolean preserveInput) {
		this.preserveInput = preserveInput;
	}

	@IbisDoc({"4", "If set, the result is stored under this session key", ""})
	public void setStoreResultInSessionKey(String string) {
		storeResultInSessionKey = string;
	}

	@IbisDoc({"5", "If set, the input is stored under this session key", ""})
	public void setStoreInputInSessionKey(String string) {
		storeInputInSessionKey = string;
	}

}
