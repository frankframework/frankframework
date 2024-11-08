/*
   Copyright 2021-2024 WeAreFrank!

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
package org.frankframework.scheduler.job;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.doc.Mandatory;
import org.frankframework.doc.Optional;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.receivers.FrankListener;
import org.frankframework.scheduler.AbstractJobDef;
import org.frankframework.senders.IbisLocalSender;
import org.frankframework.stream.Message;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.UUIDUtil;

/**
 * Scheduled job to send messages to a {@link FrankListener}.
 * Message may be {@literal null} (or empty).
 * 
 * {@inheritDoc}
 */
public class SendMessageJob extends AbstractJobDef {
	private @Setter IbisLocalSender localSender = null;
	private @Getter String javaListener;
	private @Getter String message = null;

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getJavaListener())) {
			throw new ConfigurationException("a javaListener must be specified");
		}

		super.configure();

		localSender = SpringUtils.createBean(getApplicationContext(), SendMessageJobSender.class);
		localSender.setJavaListener(getJavaListener());
		localSender.setIsolated(false);
		localSender.setName("Job " + getName());
		if (getInterval() == 0) {
			localSender.setDependencyTimeOut(-1);
		}
		localSender.configure();
	}

	@Override
	public void execute() throws JobExecutionException, TimeoutException {
		try (Message toSendMessage = getMessage() == null ? Message.nullMessage() : new Message(getMessage());
				PipeLineSession session = new PipeLineSession()) {
			// Set a messageId that will be forwarded by the localSender to the called adapter. Adapter and job will then share a Ladybug report.
			session.put(PipeLineSession.CORRELATION_ID_KEY, UUIDUtil.createSimpleUUID());

			localSender.start();
			localSender.sendMessageOrThrow(toSendMessage, session).close();
		} catch (LifecycleException | SenderException e) {
			throw new JobExecutionException("unable to send message to javaListener [" + javaListener + "]", e);
		} finally {
			try {
				localSender.stop();
			} catch (LifecycleException e) {
				log.warn("unable to close LocalSender", e);
			}
		}
	}

	/**
	 * JavaListener to send the message to
	 */
	@Mandatory
	public void setJavaListener(String javaListener) {
		this.javaListener = javaListener;
	}

	/** message to be sent into the pipeline */
	@Optional
	public void setMessage(String message) {
		if(StringUtils.isNotEmpty(message)) {
			this.message = message;
		}
	}

	/**
	 * The sole purpose of this calls is to prevent AOP wrapping around the sendMessage / sendMessageOrThrow methods.
	 * This pollutes the Ladybug with 'unwanted' reports about jobs being fired, without any useful information in the report.
	 * See org.frankframework.ibistesttool.IbisDebuggerAdvice for the exclusion
	 * 
	 * @author Niels Meijer
	 */
	public static class SendMessageJobSender extends IbisLocalSender {
		// empty
	}
}
