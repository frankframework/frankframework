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
package nl.nn.adapterframework.testtool;

import java.io.IOException;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;
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
	private TimeoutException timeOutException;
	private boolean convertExceptionToMessage = false;

	public SenderThread(ISender sender, String request, PipeLineSession session, boolean convertExceptionToMessage, String correlationId) {
		name = sender.getName();
		this.sender = sender;
		this.request = request;
		this.session = session;
		this.convertExceptionToMessage = convertExceptionToMessage;
		this.correlationId = correlationId;
		log.debug("Creating SenderThread for ISenderWithParameters '" + name + "'");
		log.debug("Request: " + request);
	}

	@Override
	public void run() {
		try {
			if (session==null) {
				session = new PipeLineSession();
			}
			session.put(PipeLineSession.CORRELATION_ID_KEY, correlationId);
			SenderResult result = sender.sendMessage(new Message(request), session);
			response = result.getResult().asString();
			result.getResult().close();
		} catch(SenderException e) {
			if (convertExceptionToMessage) {
				response = Util.throwableToXml(e);
			} else {
				log.error("SenderException for ISender '" + name + "'", e);
				senderException = e;
			}
		} catch(IOException e) {
			if (convertExceptionToMessage) {
				response = Util.throwableToXml(e);
			} else {
				log.error("IOException for ISender '" + name + "'", e);
				ioException = e;
			}
		} catch(TimeoutException e) {
			if (convertExceptionToMessage) {
				response = Util.throwableToXml(e);
			} else {
				log.error("timeOutException for ISender '" + name + "'", e);
				timeOutException = e;
			}
		}
	}

    public String getResponse() {
    	log.debug("Getting response for Sender: " + name);
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

    public TimeoutException getTimeOutException() {
        while (this.isAlive()) {
            try {
                Thread.sleep(100);
            } catch(InterruptedException e) {
            }
        }
        return timeOutException;
    }

}
