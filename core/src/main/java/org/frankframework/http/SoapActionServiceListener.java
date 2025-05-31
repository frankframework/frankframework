/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.http;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.receivers.Receiver;
import org.frankframework.receivers.ServiceDispatcher;
import org.frankframework.stream.Message;
import org.frankframework.util.StringUtil;
import org.frankframework.util.XmlBuilder;

/**
 * Listener that allows a {@link Receiver} to receive messages as a SOAP webservice.
 * Messages are routed based on their SOAP Action header or namespace.
 *
 * <br/>For each request:<ul>
 * <li>MIME headers are described in a 'mimeHeaders'-XML stored under session key 'mimeHeaders'</li>
 * <li>Attachments present in the request are described by an 'attachments'-XML stored under session key 'attachments'</li>
 * <li>SOAP protocol is stored under a session key 'soapProtocol'</li>
 * <li>SOAP action is stored under a session key 'SOAPAction'</li>
 * </ul>
 * and for each response a multipart message is constructed if a 'multipart'-XML is provided in sessionKey specified by multipartXmlSessionKey.
 *
 * @author Niels Meijer
 */
public class SoapActionServiceListener extends PushingListenerAdapter implements HasPhysicalDestination {

	private final @Getter String domain = "Http";
	private @Getter String soapAction;

	/* Attachments */
	private @Getter String attachmentSessionKeys = "";
	private @Getter String multipartXmlSessionKey = "multipartXml";
	private final List<String> attachmentSessionKeysList = new ArrayList<>();

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (StringUtils.isNotEmpty(getAttachmentSessionKeys())) {
			attachmentSessionKeysList.addAll(StringUtil.split(getAttachmentSessionKeys(), " ,;"));
		}
	}

	@Override
	public void start() {
		log.debug("registering listener [{}] with ServiceDispatcher by soapAction [{}]", this::getName, this::getSoapAction);
		ServiceDispatcher.getInstance().registerServiceClient(getSoapAction(), this);

		super.start();
	}

	@Override
	public void stop() {
		super.stop();

		log.debug("unregistering listener [{}] from ServiceDispatcher by soapAction [{}]", this::getName, this::getSoapAction);
		ServiceDispatcher.getInstance().unregisterServiceClient(getSoapAction());
	}

	@Override
	public Message processRequest(Message message, PipeLineSession session) throws ListenerException {
		if (!attachmentSessionKeysList.isEmpty()) {
			XmlBuilder xmlMultipart = new XmlBuilder("parts");
			for(String attachmentSessionKey: attachmentSessionKeysList) {
				// Using the following format: <parts><part type=\"file\" name=\"document.pdf\" sessionKey=\"part_file\" size=\"12345\" mimeType=\"application/octet-stream\"/></parts>
				XmlBuilder part = new XmlBuilder("part");
				part.addAttribute("name", attachmentSessionKey);
				part.addAttribute("sessionKey", attachmentSessionKey);
				part.addAttribute("mimeType", "application/octet-stream");
				xmlMultipart.addSubElement(part);
			}
			session.put(getMultipartXmlSessionKey(), xmlMultipart.asXmlString());
		}

		return super.processRequest(message, session);
	}

	@Override
	public String getPhysicalDestinationName() {
		return "soapAction ["+getSoapAction()+"]";
	}

	/**
	 * SOAP Action to listen to. Requests sent to `/servlet/rpcrouter` which matches the soapAction will be processed by this listener.
	 * This is slightly different from the WebServiceListener, which also handles SOAP requests but routes messages based on the first namespaceURI's.
	 */
	public void setSoapAction(String string) {
		soapAction = string;
	}

	/** Comma separated list of session keys to hold contents of attachments of the request */
	public void setAttachmentSessionKeys(String attachmentSessionKeys) {
		this.attachmentSessionKeys = attachmentSessionKeys;
	}

	/**
	 * Key of session variable that holds the description (name, sessionKey, mimeType) of the parts present in the request. Only used if attachmentSessionKeys are specified
	 * @ff.default multipartXml
	 */
	public void setMultipartXmlSessionKey(String multipartXmlSessionKey) {
		this.multipartXmlSessionKey = multipartXmlSessionKey;
	}

}
