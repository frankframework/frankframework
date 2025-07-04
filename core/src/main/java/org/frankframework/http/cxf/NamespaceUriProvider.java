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

import java.util.Iterator;

import jakarta.annotation.Nullable;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.BindingType;
import jakarta.xml.ws.ServiceMode;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.w3c.dom.Node;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.http.WebServiceListener;
import org.frankframework.receivers.ServiceClient;
import org.frankframework.receivers.ServiceDispatcher;
import org.frankframework.stream.Message;

/**
 * Soap Provider that accepts any message and routes it to a listener with a corresponding TargetObjectNamespacURI.
 *
 * @author Niels Meijer
 */
@Log4j2
@ServiceMode(value=jakarta.xml.ws.Service.Mode.MESSAGE)
@BindingType(jakarta.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
public class NamespaceUriProvider extends AbstractSOAPProvider {

	private final ServiceDispatcher sd = ServiceDispatcher.getInstance();

	public NamespaceUriProvider() {
		log.debug("initiating NamespaceUriProvider");
	}

	@Override
	protected Message processRequest(SOAPMessage request, PipeLineSession pipelineSession) throws ListenerException {
		String serviceName = pipelineSession.getString(SoapBindingConstants.SOAP_ACTION);
		ServiceClient service = findService(serviceName);

		if (service instanceof WebServiceListener) {
			// Found listener with corresponding soap action
			return processRequest(service, request, pipelineSession);
		} else {
			// Try again, this time to find the namespaceURI
			serviceName = findNamespaceUri(request);
			service = findService(serviceName);
		}

		if (!(service instanceof WebServiceListener)) {
			throw new ListenerException("service ["+ serviceName +"] is not registered or not of required type");
		}

		return processRequest(service, request, pipelineSession);
	}

	private Message processRequest(ServiceClient service, SOAPMessage request, PipeLineSession session) throws ListenerException {
		Message message = parseSOAPMessage(request);
		log.info("processing message [{}] on service [{}]", message.getObjectId(), service);
		return service.processRequest(message, session);
	}

	@Nullable
	private ServiceClient findService(String serviceName) {
		if (StringUtils.isBlank(serviceName)) {
			return null;
		}
		log.debug("trying to find serviceName from soapMessage [{}]", serviceName);

		return sd.getListener(serviceName);
	}

	public static String findNamespaceUri(SOAPMessage soapMessage) throws ListenerException {
		log.debug("trying to find serviceName from soapMessage [{}]", soapMessage);

		try {
			SOAPBody body = soapMessage.getSOAPBody();
			if(body.hasChildNodes()) {
				Iterator<?> it = body.getChildElements();
				while (it.hasNext()) {
					Node node = (Node) it.next();

					// Found first namespaceURI
					String namespace = node.getNamespaceURI();
					if(StringUtils.isNotEmpty(namespace)) {
						log.debug("found namespace[{}]", namespace);
						return namespace;
					}
				}
			}
		}
		catch (SOAPException e) {
			throw new ListenerException("unable to read soap message", e);
		}

		throw new ListenerException("unable to determine serviceName from namespaceURI");
	}
}
