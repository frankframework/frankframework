/*
   Copyright 2018-2021 Nationale-Nederlanden, 2021-2024 WeAreFrank!

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

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jakarta.annotation.Resource;
import jakarta.xml.soap.AttachmentPart;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.MimeHeader;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPPart;
import jakarta.xml.ws.BindingType;
import jakarta.xml.ws.Provider;
import jakarta.xml.ws.ServiceMode;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.WebServiceProvider;
import jakarta.xml.ws.handler.MessageContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Logger;
import org.springframework.util.MimeType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SpringSecurityHandler;
import org.frankframework.http.mime.MultipartUtils;
import org.frankframework.http.mime.MultipartUtils.MultipartMessages;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.DomBuilderException;
import org.frankframework.util.LogUtil;
import org.frankframework.util.MessageDataSource;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.XmlBuilder;
import org.frankframework.util.XmlUtils;

/**
 * Base class for handling JAX-WS SOAP messages
 *
 * @author Jaco de Groot
 * @author Niels Meijer
 *
 */
@WebServiceProvider
@ServiceMode(value=jakarta.xml.ws.Service.Mode.MESSAGE)
@BindingType(jakarta.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
public abstract class AbstractSOAPProvider implements Provider<SOAPMessage> {
	protected Logger log = LogUtil.getLogger(this);

	private String attachmentXmlSessionKey = null;
	private Map<String, MessageFactory> factory = new HashMap<>();

	private static final String SOAP_PROTOCOL_KEY = "soapProtocol";

	protected boolean multipartBackwardsCompatibilityMode = AppConstants.getInstance().getBoolean("WebServiceListener.backwardsCompatibleMultipartNotation", false);

	// WebServiceProviders must have a default public constructor
	public AbstractSOAPProvider() {
		log.debug("initiating SOAP Service Provider");
	}

	/*
	 * autowired via spring bean, see springContext.xml
	 */
	@Resource
	WebServiceContext webServiceContext;

	private Message createSoapFault() {
		String faultcode = "soap:Server";
		String faultstring = "SOAPMessage is null";
		String httpRequestMethod = (String) webServiceContext.getMessageContext().get(MessageContext.HTTP_REQUEST_METHOD);
		if (!"POST".equals(httpRequestMethod)) {
			faultcode = "soap:Client";
			faultstring = "Request was send using '" + httpRequestMethod + "' instead of 'POST'";
		}
		return new Message(
				"<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
				"<soap:Body><soap:Fault>" +
				"<faultcode>" + faultcode + "</faultcode>" +
				"<faultstring>" + faultstring + "</faultstring>" +
				"</soap:Fault></soap:Body></soap:Envelope>");
	}

	@Override
	public SOAPMessage invoke(SOAPMessage request) {
		String messageId = MessageUtils.generateMessageId();
		try (final CloseableThreadContext.Instance ctc = CloseableThreadContext.put(LogUtil.MDC_MESSAGE_ID_KEY, messageId);
				PipeLineSession pipelineSession = new PipeLineSession()) {

			PipeLineSession.updateListenerParameters(pipelineSession, messageId, messageId);
			log.debug("received message");

			final Message response = request == null ? createSoapFault() : processRequest(request, pipelineSession);

			// Transform result string to SOAP message
			log.debug("transforming to SOAP message");
			String soapProtocol = pipelineSession.get(SOAP_PROTOCOL_KEY, SOAPConstants.SOAP_1_1_PROTOCOL);
			SOAPMessage soapMessage = convertResponseToSoapMessage(response, soapProtocol);

			try {
				String multipartXml = pipelineSession.getString(attachmentXmlSessionKey);
				log.debug("building multipart message with MultipartXmlSessionKey [{}]", multipartXml);
				if (StringUtils.isNotEmpty(multipartXml)) {
					Element partsElement;
					try {
						partsElement = XmlUtils.buildElement(multipartXml);
					}
					catch (DomBuilderException e) {
						String m = "error building multipart xml";
						log.error(m, e);
						throw new WebServiceException(m, e);
					}
					Collection<Node> parts = XmlUtils.getChildTags(partsElement, "part");
					if (parts.isEmpty()) {
						log.warn("no part(s) in multipart xml [{}]", multipartXml);
					}
					else {
						for (final Node part : parts) {
							Element partElement = (Element) part;

							String partSessionKey = partElement.getAttribute("sessionKey");
							String name = partElement.getAttribute("name");
							Message partObject = pipelineSession.getMessage(partSessionKey);

							if (!partObject.isNull()) {
								String mimeType = partElement.getAttribute("mimeType"); // Optional, auto-detected if not set
								partObject.unscheduleFromCloseOnExitOf(pipelineSession); // Closed by the SourceClosingDataHandler
								MessageDataSource ds = new MessageDataSource(partObject, mimeType);
								SourceClosingDataHandler dataHander = new SourceClosingDataHandler(ds);
								AttachmentPart attachmentPart = soapMessage.createAttachmentPart(dataHander);
								attachmentPart.setContentId(partSessionKey); // ContentID is URLDecoded, it may not contain special characters, see #4661

								String filename = StringUtils.isNotBlank(name) ? name : ds.getName();
								attachmentPart.addMimeHeader("Content-Disposition", "attachment; name=\""+filename+"\"; filename=\""+filename+"\"");
								soapMessage.addAttachmentPart(attachmentPart);

								log.debug("appended filepart [{}] key [{}]", filename, partSessionKey);
							} else {
								log.debug("skipping filepart [{}] key [{}], content is <NULL>", name, partSessionKey);
							}
						}
					}
				}
			} catch (IOException e) {
				String m = "could not transform attachment";
				log.error(m);
				throw new WebServiceException(m, e);
			}

			return soapMessage;
		}
	}

	private Message processRequest(SOAPMessage request, PipeLineSession pipelineSession) {
		// Make mime headers in request available as session key
		Iterator<MimeHeader> mimeHeaders = request.getMimeHeaders().getAllHeaders();
		String mimeHeadersXml = getMimeHeadersXml(mimeHeaders).asXmlString();
		pipelineSession.put("mimeHeaders", mimeHeadersXml);

		// Make attachments in request (when present) available as session keys
		if(multipartBackwardsCompatibilityMode) {
			handleIncomingAttachmentsLegacy(request.getAttachments(), pipelineSession);
		} else {
			MultipartMessages parts = MultipartUtils.parseMultipart(request.getAttachments());
			parts.messages().forEach(pipelineSession::put);
			pipelineSession.put(MultipartUtils.MULTIPART_ATTACHMENTS_SESSION_KEY, parts.multipartXml());
		}


		// Transform SOAP message to string
		String contentType = (String) webServiceContext.getMessageContext().get("Content-Type");
		Message soapMessage = parseSOAPMessage(request, contentType);
		String soapProtocol = SOAPConstants.SOAP_1_1_PROTOCOL;
		try {
			if(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE.equals(request.getSOAPPart().getEnvelope().getNamespaceURI())) {
				soapProtocol = SOAPConstants.SOAP_1_2_PROTOCOL;
			}
		} catch (SOAPException e) {
			log.error("unable to determine SOAP URI NS type, falling back to SOAP 1.1", e);
		}

		log.debug("transforming from SOAP message");
		pipelineSession.put(SOAP_PROTOCOL_KEY, soapProtocol);
		if(soapProtocol.equals(SOAPConstants.SOAP_1_1_PROTOCOL)) {
			String soapAction = (String) webServiceContext.getMessageContext().get(SoapBindingConstants.SOAP_ACTION);
			pipelineSession.put(SoapBindingConstants.SOAP_ACTION, soapAction);
		} else {
			if(StringUtils.isNotEmpty(contentType)) {
				String action = findAction(contentType);
				if(StringUtils.isNotEmpty(action)) {
					pipelineSession.put(SoapBindingConstants.SOAP_ACTION, action);
				} else {
					log.warn("no SOAPAction found!");
				}
			}
		}

		// Process message via WebServiceListener
		pipelineSession.setSecurityHandler(new SpringSecurityHandler());
		pipelineSession.put(PipeLineSession.HTTP_REQUEST_KEY, webServiceContext.getMessageContext().get(MessageContext.SERVLET_REQUEST));
		pipelineSession.put(PipeLineSession.HTTP_RESPONSE_KEY, webServiceContext.getMessageContext().get(MessageContext.SERVLET_RESPONSE));

		try {
			log.debug("processing message");
			return processRequest(soapMessage, pipelineSession);
		} catch (ListenerException e) {
			String m = "Could not process SOAP message: " + e.getMessage();
			log.error(m);
			throw new WebServiceException(m, e);
		}
	}

	/**
	 * This method uses a custom / different way to storing the multipart attachments in the PipeLineSession
	 */
	private void handleIncomingAttachmentsLegacy(Iterator<AttachmentPart> attachmentParts, PipeLineSession pipelineSession) {
		int i = 1;
		XmlBuilder attachments = new XmlBuilder("attachments");
		while (attachmentParts.hasNext()) {
			try {
				AttachmentPart attachmentPart = attachmentParts.next();

				XmlBuilder attachment = new XmlBuilder("attachment");
				attachments.addSubElement(attachment);
				XmlBuilder sessionKey = new XmlBuilder("sessionKey");
				sessionKey.setValue("attachment" + i);
				attachment.addSubElement(sessionKey);
				pipelineSession.put("attachment" + i, MessageUtils.parse(attachmentPart));
				log.debug("adding attachment [attachment{}] to session", i);

				Iterator<MimeHeader> attachmentMimeHeaders = attachmentPart.getAllMimeHeaders();
				attachment.addSubElement(getMimeHeadersXml(attachmentMimeHeaders));
			} catch (SOAPException e) {
				log.warn("Could not store attachment in session key", e);
			}
			i++;
		}
		pipelineSession.put("attachments", attachments.asXmlString());
	}

	/**
	 * Create a MessageFactory singleton
	 * @param soapProtocol see {@link SOAPConstants} for possible values
	 * @return previously initialized or newly created MessageFactory
	 * @throws SOAPException when it's not possible to instantiate a new MessageFactory
	 */
	private synchronized MessageFactory getMessageFactory(String soapProtocol) throws SOAPException {
		if(!factory.containsKey(soapProtocol)) {
			log.info("creating new MessageFactory for soapProtocol [{}]", soapProtocol);
			factory.put(soapProtocol, MessageFactory.newInstance(soapProtocol));
		}

		log.debug("using cached MessageFactory for soapProtocol [{}]", soapProtocol);
		return factory.get(soapProtocol);
	}

	/**
	 * Actually process the request
	 * @param message message that was received
	 * @param pipelineSession messageContext (containing attachments if available)
	 * @return response to send back
	 */
	abstract Message processRequest(Message message, PipeLineSession pipelineSession) throws ListenerException;

	/**
	 * SessionKey containing attachment information, or null if no attachments
	 * @param attachmentXmlSessionKey {@code <parts><part type="file" name="document.pdf" sessionKey="part_file" size="12345" mimeType="application/octet-stream"/></parts>}
	 */
	public void setAttachmentXmlSessionKey(String attachmentXmlSessionKey) {
		this.attachmentXmlSessionKey = attachmentXmlSessionKey;
	}

	private XmlBuilder getMimeHeadersXml(Iterator<MimeHeader> mimeHeaders) {
		XmlBuilder xmlMimeHeaders = new XmlBuilder("mimeHeaders");
		while (mimeHeaders.hasNext()) {
			MimeHeader mimeHeader = mimeHeaders.next();
			XmlBuilder xmlMimeHeader = new XmlBuilder("mimeHeader");
			xmlMimeHeader.addAttribute("name", mimeHeader.getName());
			xmlMimeHeader.setValue(mimeHeader.getValue());
			xmlMimeHeaders.addSubElement(xmlMimeHeader);
		}
		return xmlMimeHeaders;
	}

	protected String findAction(String contentType) {
		MimeType mimeType = MimeType.valueOf(contentType);
		return mimeType.getParameter("action");
	}

	private Message parseSOAPMessage(SOAPMessage soapMessage, String contentType) {
		org.frankframework.stream.MessageContext context = MessageUtils.getContext(soapMessage.getMimeHeaders().getAllHeaders());
		if(StringUtils.isNotEmpty(contentType)) {
			context.withMimeType(contentType);
		}
		SOAPPart part = soapMessage.getSOAPPart();
		return new Message(part, context);
	}

	private SOAPMessage convertResponseToSoapMessage(Message response, String soapProtocol) {
		try (response) {
			SOAPMessage soapMessage = getMessageFactory(soapProtocol).createMessage();
			soapMessage.getSOAPPart().setContent(response.asSource());
			return soapMessage;
		} catch (SOAPException | IOException | SAXException e) {
			String m = "could not transform string to SOAP message";
			log.error(m);
			throw new WebServiceException(m, e);
		}
	}
}
