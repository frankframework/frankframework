/*
   Copyright 2018 Nationale-Nederlanden

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
package nl.nn.adapterframework.http.cxf;

import javax.xml.ws.BindingType;
import javax.xml.ws.ServiceMode;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.http.PushingListenerAdapter;

/**
 * A JAX-WS wired message provider for handling soap messages
 * 
 * @author Niels Meijer
 *
 */

@ServiceMode(value=javax.xml.ws.Service.Mode.MESSAGE)
@BindingType(javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
public class MessageProvider extends SOAPProviderBase {

	private PushingListenerAdapter<String> listener;

	public MessageProvider(PushingListenerAdapter<String> listener, String multipartXmlSessionKey) {
		super();
		this.listener = listener;
		setAttachmentXmlSessionKey(multipartXmlSessionKey);
	}

	@Override
	String processRequest(String correlationId, String message, IPipeLineSession pipelineSession) throws ListenerException {
		return listener.processRequest(correlationId, message, pipelineSession);
	}

	@Override
	protected String getLogPrefix(String correlationId) {
		return "Listener ["+listener.getName()+"] correlationId["+correlationId+"] ";
	}
}
