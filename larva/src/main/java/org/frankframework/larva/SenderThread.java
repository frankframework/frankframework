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
package org.frankframework.larva;

import java.io.IOException;

import org.apache.logging.log4j.Logger;

import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.stream.Message;
import org.frankframework.util.LogUtil;
import org.frankframework.util.XmlEncodingUtils;

/**
 * @author Jaco de Groot
 */
public class SenderThread extends Thread {
	private static final Logger log = LogUtil.getLogger(SenderThread.class);

	private final String name;
	private final ISender sender;
	private PipeLineSession session;
	private final String request;
	private String response;
	private final String correlationId;
	private SenderException senderException;
	private IOException ioException;
	private TimeoutException timeoutException;
	private final boolean convertExceptionToMessage;

	public SenderThread(ISender sender, String request, PipeLineSession session, boolean convertExceptionToMessage, String correlationId) {
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
		session.put(PipeLineSession.CORRELATION_ID_KEY, correlationId);

		try (Message input = new Message(request); SenderResult result = sender.sendMessage(input, session)) {
			response = (Message.isNull(result.getResult())) ? "" : result.getResult().asString();
		} catch(SenderException e) {
			if (convertExceptionToMessage) {
				response = throwableToXml(e);
			} else {
				log.error("SenderException for ISender '{}'", name, e);
				senderException = e;
			}
		} catch(IOException e) {
			if (convertExceptionToMessage) {
				response = throwableToXml(e);
			} else {
				log.error("IOException for ISender '{}'", name, e);
				ioException = e;
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

    public String getResponse() {
		log.debug("Getting response for Sender: {}", name);
        while (this.isAlive()) {
            try {
                Thread.sleep(100);
            } catch(InterruptedException e) {
            }
        }
        return response;
    }

    public SenderException getSenderException() {
        while (this.isAlive()) {
            try {
                Thread.sleep(100);
            } catch(InterruptedException e) {
            }
        }
        return senderException;
    }

    public IOException getIOException() {
        while (this.isAlive()) {
            try {
                Thread.sleep(100);
            } catch(InterruptedException e) {
            }
        }
        return ioException;
    }

    public TimeoutException getTimeoutException() {
        while (this.isAlive()) {
            try {
                Thread.sleep(100);
            } catch(InterruptedException e) {
            }
        }
        return timeoutException;
    }

	static String throwableToXml(Throwable throwable) {
		StringBuilder xml = new StringBuilder("<throwable>");
		xml.append("<class>").append(throwable.getClass().getName()).append("</class>");
		xml.append("<message>").append(XmlEncodingUtils.encodeChars(XmlEncodingUtils.replaceNonValidXmlCharacters(throwable.getMessage()))).append("</message>");
		Throwable cause = throwable.getCause();
		if (cause != null) {
			xml = new StringBuilder(xml + "<cause>" + throwableToXml(cause) + "</cause>");
		}
		xml.append("</throwable>");
		return xml.toString();
	}

}
