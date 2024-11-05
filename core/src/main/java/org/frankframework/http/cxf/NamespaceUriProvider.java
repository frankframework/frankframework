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
package org.frankframework.http.cxf;

import java.util.Iterator;

import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.BindingType;
import jakarta.xml.ws.ServiceMode;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;

import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.receivers.ServiceDispatcher;
import org.frankframework.stream.Message;

/**
 * Soap Provider that accepts any message and routes it to a listener with a corresponding TargetObjectNamespacURI.
 *
 * @author Niels Meijer
 */

@ServiceMode(value=jakarta.xml.ws.Service.Mode.MESSAGE)
@BindingType(jakarta.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
public class NamespaceUriProvider extends AbstractSOAPProvider {

	private ServiceDispatcher sd = ServiceDispatcher.getInstance();

	private SOAPMessage soapMessage;

	public NamespaceUriProvider() {
		log.debug("initiating NamespaceUriProvider");
	}

	@Override
	public SOAPMessage invoke(SOAPMessage request) {
		this.soapMessage = request;
		return super.invoke(request);
	}

	@Override
	Message processRequest(Message message, PipeLineSession pipelineSession) throws ListenerException {
		String serviceName = findNamespaceUri();
		log.debug("found namespace[{}]", serviceName);
		return sd.dispatchRequest(serviceName, message, pipelineSession);
	}

	public String findNamespaceUri() throws ListenerException {
		log.debug("trying to find serviceName from soapMessage[{}]", soapMessage);

		try {
			SOAPBody body = soapMessage.getSOAPBody();
			if(body.hasChildNodes()) {
				Iterator<?> it = body.getChildElements();
				while (it.hasNext()) {
					Node node = (Node) it.next();

					// Found first namespaceURI
					if(StringUtils.isNotEmpty(node.getNamespaceURI()))
						return node.getNamespaceURI();
				}
			}
		}
		catch (SOAPException e) {
			throw new ListenerException(e);
		}

		throw new ListenerException("unable to determine serviceName from NamespaceURI");
	}
}
