/*
   Copyright 2021-2025 WeAreFrank!

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
package org.frankframework.larva;

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.SpringSecurityHandler;
import org.frankframework.core.TimeoutException;
import org.frankframework.stream.Message;
import org.frankframework.util.XmlEncodingUtils;

/**
 * @author Jaco de Groot
 */
@Log4j2
public class SenderThread extends Thread {
	private static final Authentication DEFAULT_TEST_AUTHENTICATION = new TestingAuthenticationToken("LarvaSender", "");

	private final String name;
	private final ISender sender;
	private final PipeLineSession session;
	private final Message request;
	private Message response;
	private final String correlationId;
	private SenderException senderException;
	private TimeoutException timeoutException;
	private final boolean convertExceptionToMessage;
	private @Setter SecurityContext securityContext;

	public SenderThread(ISender sender, Message request, PipeLineSession session, boolean convertExceptionToMessage, String correlationId) {
		name = sender.getName();
		this.sender = sender;
		this.request = request;
		this.session = session;
		this.convertExceptionToMessage = convertExceptionToMessage;
		this.correlationId = correlationId;
		log.debug("Creating SenderThread for ISenderWithParameters '{}'", name);
		log.debug("Request: {}", request);
	}

	@Override
	public void run() {
		if (securityContext == null) { // Ensure there is always a SecurityContext present, though this gets lost when using the IbisJavaSender
			securityContext = SecurityContextHolder.getContextHolderStrategy().createEmptyContext();
			securityContext.setAuthentication(DEFAULT_TEST_AUTHENTICATION);
		}

		SecurityContextHolder.getContextHolderStrategy().setContext(securityContext);
		session.put(PipeLineSession.CORRELATION_ID_KEY, correlationId);
		session.setSecurityHandler(new SpringSecurityHandler());

		try {
			SenderResult result = sender.sendMessage(request, session);
			response = (result.getResult() == null) ? Message.nullMessage() : result.getResult();
		} catch(SenderException e) {
			if (convertExceptionToMessage) {
				response = throwableToXml(e);
			} else {
				log.error("SenderException for ISender '{}'", name, e);
				senderException = e;
			}
		} catch(TimeoutException e) {
			if (convertExceptionToMessage) {
				response = throwableToXml(e);
			} else {
				log.error("timeoutException for ISender '{}'", name, e);
				timeoutException = e;
			}
		}
	}

	public Message getResponse() {
		log.debug("Getting response for Sender: {}", name);
		while (this.isAlive()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
		return response;
	}

	public SenderException getSenderException() {
		while (this.isAlive()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
		return senderException;
	}

	public TimeoutException getTimeoutException() {
		while (this.isAlive()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
		return timeoutException;
	}

	static Message throwableToXml(Throwable throwable) {
		return new Message(throwableToXmlString(throwable));
	}

	static String throwableToXmlString(Throwable throwable) {
		StringBuilder xml = new StringBuilder("<throwable>");
		xml.append("<class>").append(throwable.getClass().getName()).append("</class>");
		xml.append("<message>").append(XmlEncodingUtils.encodeChars(XmlEncodingUtils.replaceNonValidXmlCharacters(throwable.getMessage()))).append("</message>");
		Throwable cause = throwable.getCause();
		if (cause != null) {
			xml = new StringBuilder(xml + "<cause>" + throwableToXmlString(cause) + "</cause>");
		}
		xml.append("</throwable>");
		return xml.toString();
	}

}
