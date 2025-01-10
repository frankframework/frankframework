/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.senders;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.cache.ICache;
import org.frankframework.cache.ICacheEnabled;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.processors.SenderWrapperProcessor;
import org.frankframework.statistics.MetricsInitializer;
import org.frankframework.stream.Message;

/**
 * Baseclass for Wrappers for senders, that allows to get input from a session variable, and to store output in a session variable.
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public abstract class AbstractSenderWrapper extends AbstractSenderWithParameters implements ICacheEnabled<String,String> {

	private @Getter String getInputFromSessionKey;
	private @Getter String getInputFromFixedValue=null;
	private @Getter String storeResultInSessionKey;
	private @Getter String storeInputInSessionKey;
	private @Getter boolean preserveInput=false;

	protected @Setter MetricsInitializer configurationMetrics;

	protected @Setter SenderWrapperProcessor senderWrapperProcessor;
	private @Getter @Setter ICache<String,String> cache=null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (!isSenderConfigured()) {
			throw new ConfigurationException("must have at least a sender configured");
		}
		if (StringUtils.isNotEmpty(getGetInputFromSessionKey()) && StringUtils.isNotEmpty(getGetInputFromFixedValue())) {
			throw new ConfigurationException("cannot have both attributes inputFromSessionKey and inputFromFixedValue configured");
		}
		if (cache!=null) {
			cache.configure(getName());
		}
	}

	@Override
	public void start() {
		if (cache!=null) {
			cache.open();
		}
		super.start();
	}

	@Override
	public void stop() {
		try {
			super.stop();
		} finally {
			if (cache!=null) {
				cache.close();
			}
		}
	}

	protected abstract boolean isSenderConfigured();

	public abstract SenderResult doSendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException;

	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		if (senderWrapperProcessor!=null) {
			return senderWrapperProcessor.sendMessage(this, message, session);
		}
		return doSendMessage(message, session);
	}

	/** If set, input is taken from this session key, instead of regular input */
	public void setGetInputFromSessionKey(String string) {
		getInputFromSessionKey = string;
	}

	/** If set, this fixed value is taken as input, instead of regular input */
	public void setGetInputFromFixedValue(String string) {
		getInputFromFixedValue = string;
	}

	/**
	 * If set <code>true</code>, the input of a pipe is restored before processing the next one
	 * @ff.default false
	 */
	public void setPreserveInput(boolean preserveInput) {
		this.preserveInput = preserveInput;
	}

	/** If set, the result is stored under this session key */
	public void setStoreResultInSessionKey(String string) {
		storeResultInSessionKey = string;
	}

	/** If set, the input is stored under this session key */
	public void setStoreInputInSessionKey(String string) {
		storeInputInSessionKey = string;
	}

}
