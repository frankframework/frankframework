/*
   Copyright 2021, 2022 WeAreFrank!

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
package nl.nn.adapterframework.scheduler.job;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.scheduler.JobDef;
import nl.nn.adapterframework.senders.IbisLocalSender;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.SpringUtils;
import nl.nn.adapterframework.util.UUIDUtil;

public class SendMessageJob extends JobDef {
	private @Setter IbisLocalSender localSender = null;
	private @Getter String javaListener;
	private @Getter String message = null;

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getJavaListener())) {
			throw new ConfigurationException("a javaListener must be specified");
		}

		super.configure();

		localSender = SpringUtils.createBean(getApplicationContext(), IbisLocalSender.class);
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
		try (Message toSendMessage = new Message((getMessage() == null) ? "" : getMessage());
				PipeLineSession session = new PipeLineSession()) {
			//Set a messageId that will be forwarded by the localSender to the called adapter. Adapter and job will then share a Ladybug report.
			session.put(PipeLineSession.CORRELATION_ID_KEY, UUIDUtil.createSimpleUUID());

			localSender.open();
			localSender.sendMessageOrThrow(toSendMessage, session).close();
		} catch (SenderException | IOException e) {
			throw new JobExecutionException("unable to send message to javaListener [" + javaListener + "]", e);
		} finally {
			try {
				localSender.close();
			} catch (SenderException e) {
				log.warn("unable to close LocalSender", e);
			}
		}
	}

	/**
	 * JavaListener to send the message to
	 * @ff.mandatory
	 */
	public void setJavaListener(String javaListener) {
		this.javaListener = javaListener;
	}

	@Deprecated
	@ConfigurationWarning("Please use attribute javaListener instead")
	public void setReceiverName(String receiverName) {
		setJavaListener(receiverName); //For backwards compatibility
	}

	/** message to be sent into the pipeline */
	public void setMessage(String message) {
		if(StringUtils.isNotEmpty(message)) {
			this.message = message;
		}
	}
}
