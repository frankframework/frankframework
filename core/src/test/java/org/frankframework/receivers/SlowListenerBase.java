/*
   Copyright 2022-2025 WeAreFrank!

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

import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IListener;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;

@Log4j2
public abstract class SlowListenerBase implements IListener<String> {
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
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<String> rawMessage, PipeLineSession pipeLineSession) {
		// No-op
	}

	@Override
	public Message extractMessage(@Nonnull RawMessageWrapper<String> rawMessage, @Nonnull Map<String, Object> context) {
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
