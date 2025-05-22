/*
   Copyright 2018 Nationale-Nederlanden, 2025 WeAreFrank!

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
package org.frankframework.http.cxf;

import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.BindingType;
import jakarta.xml.ws.ServiceMode;

import org.apache.logging.log4j.CloseableThreadContext;

import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.http.PushingListenerAdapter;
import org.frankframework.stream.Message;
import org.frankframework.util.LogUtil;

/**
 * A JAX-WS wired message provider for handling soap messages
 *
 * @author Niels Meijer
 *
 */

@ServiceMode(value=jakarta.xml.ws.Service.Mode.MESSAGE)
@BindingType(jakarta.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
public class MessageProvider extends AbstractSOAPProvider {

	private final PushingListenerAdapter listener;

	public MessageProvider(PushingListenerAdapter listener, String multipartXmlSessionKey) {
		super();
		this.listener = listener;
		setAttachmentXmlSessionKey(multipartXmlSessionKey);
	}

	@Override
	Message processRequest(SOAPMessage soapMessage, PipeLineSession pipelineSession) throws ListenerException {
		try (final CloseableThreadContext.Instance ctc = CloseableThreadContext.put(LogUtil.MDC_LISTENER_KEY, listener.getName())) {
			Message message = parseSOAPMessage(soapMessage);
			return listener.processRequest(message, pipelineSession);
		}
	}
}
