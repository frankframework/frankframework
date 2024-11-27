/*
   Copyright 2022-2023 WeAreFrank!

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
package org.frankframework.receivers;

import java.util.Map;

import jakarta.annotation.Nonnull;

import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IListener;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;
import org.frankframework.util.LogUtil;

public abstract class SlowListenerBase implements IListener<jakarta.jms.Message> {
	protected Logger log = LogUtil.getLogger(this);
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;
	private @Getter @Setter String name;
	private @Setter int startupDelay = 10000;
	private @Setter int shutdownDelay = 0;
	private @Getter boolean closed = false;

	@Override
	public void configure() throws ConfigurationException {
		//Nothing to configure
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<jakarta.jms.Message> rawMessage, PipeLineSession pipeLineSession) {
		// No-op
	}

	@Override
	public Message extractMessage(@Nonnull RawMessageWrapper<jakarta.jms.Message> rawMessage, @Nonnull Map<String, Object> context) {
		return Message.asMessage(rawMessage.getRawMessage());
	}

	@Override
	public void start() {
		if (startupDelay > 0) {
			try {
				Thread.sleep(startupDelay);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	@Override
	public void stop() {
		if (shutdownDelay > 0) {
			try {
				Thread.sleep(shutdownDelay);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			log.debug("Closed after delay");
		}
		closed = true;
	}
}
