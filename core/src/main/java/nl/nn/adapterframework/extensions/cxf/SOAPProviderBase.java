/*
   Copyright 2018, 2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.cxf;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.activation.DataHandler;
import javax.annotation.Resource;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeader;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingType;
import javax.xml.ws.Provider;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.handler.MessageContext;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.soap.util.mime.ByteArrayDataSource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISecurityHandler;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Base class for handling JAX-WS SOAP messages
 * 
 * @author Jaco de Groot
 * @author Niels Meijer
 *
 */
@WebServiceProvider
@ServiceMode(value=javax.xml.ws.Service.Mode.MESSAGE)
@BindingType(javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
public abstract class SOAPProviderBase implements Provider<SOAPMessage> {
	protected Logger log = LogUtil.getLogger(this);

	private String attachmentXmlSessionKey = null;
	private Map<String, MessageFactory> factory = new HashMap<String, MessageFactory>();

	public SOAPProviderBase() {
		log.debug("initiating SOAP Service Provider");
	}

	/*
	 * autowired via spring bean, see springContext.xml
	 */
	@Resource
	WebServiceContext webServiceContext;

	@Override
	public SOAPMessage invoke(SOAPMessage request) {
		String result;
		PipeLineSessionBase pipelineSession = new PipeLineSessionBase();
		String correlationId = Misc.createSimpleUUID();
		log.debug(getLogPrefix(correlationId)+"received message");
		String soapProtocol = SOAPConstants.SOAP_1_1_PROTOCOL;

		if (request == null) {
			String faultcode = "soap:Server";
			String faultstring = "SOAPMessage is null";
			String httpRequestMethod = (String)webServiceContext.getMessageContext()
					.get(MessageContext.HTTP_REQUEST_METHOD);
			if (!"POST".equals(httpRequestMethod)) {
				faultcode = "soap:Client";
				faultstring = "Request was send using '" + httpRequestMethod + "' instead of 'POST'";
			}
			result =
					"<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
					"<soap:Body><soap:Fault>" +
					"<faultcode>" + faultcode + "</faultcode>" +
					"<faultstring>" + faultstring + "</faultstring>" +
					"</soap:Fault></soap:Body></soap:Envelope>";
		}
		else {
			// Make mime headers in request available as session key
			@SuppressWarnings("unchecked")
			Iterator<MimeHeader> mimeHeaders = request.getMimeHeaders().getAllHeaders();
			String mimeHeadersXml = getMimeHeadersXml(mimeHeaders).toXML();
			pipelineSession.put("mimeHeaders", mimeHeadersXml);

			// Make attachments in request (when present) available as session keys
			int i = 1;
			XmlBuilder attachments = new XmlBuilder("attachments");
			@SuppressWarnings("unchecked")
			Iterator<AttachmentPart> attachmentParts = request.getAttachments();
			while (attachmentParts.hasNext()) {
				try {
					InputStreamAttachmentPart attachmentPart = new InputStreamAttachmentPart(attachmentParts.next());

					XmlBuilder attachment = new XmlBuilder("attachment");
					attachments.addSubElement(attachment);
					XmlBuilder sessionKey = new XmlBuilder("sessionKey");
					sessionKey.setValue("attachment" + i);
					attachment.addSubElement(sessionKey);
					pipelineSession.put("attachment" + i, attachmentPart.getInputStream());
					log.debug(getLogPrefix(correlationId)+"adding attachment [attachment" + i+"] to session");

					@SuppressWarnings("unchecked")
					Iterator<MimeHeader> attachmentMimeHeaders = attachmentPart.getAllMimeHeaders();
					attachment.addSubElement(getMimeHeadersXml(attachmentMimeHeaders));
				} catch (SOAPException e) {
					log.warn("Could not store attachment in session key", e);
				}
				i++;
			}
			pipelineSession.put("attachments", attachments.toXML());

			// Transform SOAP message to string
			String message;
			try {
				SOAPPart part = request.getSOAPPart();
				try {
					if(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE.equals(part.getEnvelope().getNamespaceURI()))
						soapProtocol = SOAPConstants.SOAP_1_2_PROTOCOL;
				} catch (SOAPException e) {
					log.error("unable to determine SOAP URI NS type, falling back to SOAP 1.1", e);
				}

				message = XmlUtils.nodeToString(part);
				log.debug(getLogPrefix(correlationId)+"transforming from SOAP message");
			} catch (TransformerException e) {
				String m = "Could not transform SOAP message to string";
				log.error(m, e);
				throw new WebServiceException(m, e);
			}
			pipelineSession.put("soapProtocol", soapProtocol);

			// Process message via WebServiceListener
			ISecurityHandler securityHandler = new WebServiceContextSecurityHandler(webServiceContext);
			pipelineSession.setSecurityHandler(securityHandler);
			pipelineSession.put(IPipeLineSession.HTTP_REQUEST_KEY, webServiceContext.getMessageContext()
					.get(MessageContext.SERVLET_REQUEST));
			pipelineSession.put(IPipeLineSession.HTTP_RESPONSE_KEY, webServiceContext.getMessageContext()
					.get(MessageContext.SERVLET_RESPONSE));

			try {
				log.debug(getLogPrefix(correlationId)+"processing message");
				result = processRequest(correlationId, message, pipelineSession);
			}
			catch (ListenerException e) {
				String m = "Could not process SOAP message: " + e.getMessage();
				log.error(m);
				throw new WebServiceException(m, e);
			}
		}

		// Transform result string to SOAP message
		SOAPMessage soapMessage = null;
		try {
			log.debug(getLogPrefix(correlationId)+"transforming to SOAP message");
			soapMessage = getMessageFactory(soapProtocol).createMessage();
			StreamSource streamSource = new StreamSource(new StringReader(result));
			soapMessage.getSOAPPart().setContent(streamSource);
		} catch (SOAPException e) {
			String m = "Could not transform string to SOAP message";
			log.error(m);
			throw new WebServiceException(m, e);
		}

		String multipartXml = (String) pipelineSession.get(attachmentXmlSessionKey);
		log.debug(getLogPrefix(correlationId)+"building multipart message with MultipartXmlSessionKey ["+multipartXml+"]");
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
			if (parts==null || parts.size()==0) {
				log.warn(getLogPrefix(correlationId)+"no part(s) in multipart xml [" + multipartXml + "]");
			}
			else {
				Iterator<Node> iter = parts.iterator();
				while (iter.hasNext()) {
					Element partElement = (Element) iter.next();
					//String partType = partElement.getAttribute("type");
					String partName = partElement.getAttribute("name");
					String partSessionKey = partElement.getAttribute("sessionKey");
					String partMimeType = partElement.getAttribute("mimeType");
					Object partObject = pipelineSession.get(partSessionKey);
					if (partObject instanceof InputStream) {
						InputStream fis = (InputStream) partObject;

						DataHandler dataHander = null;
						try {
							dataHander = new DataHandler(new ByteArrayDataSource(fis, partMimeType));
						} catch (IOException e) {
							String m = "Unable to add session key '" + partSessionKey + "' as attachment";
							log.error(m, e);
							throw new WebServiceException(m, e);
						}
						AttachmentPart attachmentPart = soapMessage.createAttachmentPart(dataHander);
						attachmentPart.setContentId(partName);
						soapMessage.addAttachmentPart(attachmentPart);

						log.debug(getLogPrefix(correlationId)+"appended filepart ["+partSessionKey+"] with value ["+partObject+"] and name ["+partName+"]");
					}
					else { //String
						String partValue = (String) partObject;

						DataHandler dataHander = new DataHandler(new ByteArrayDataSource(partValue, partMimeType));
						AttachmentPart attachmentPart = soapMessage.createAttachmentPart(dataHander);
						attachmentPart.setContentId(partName);
						soapMessage.addAttachmentPart(attachmentPart);

						log.debug(getLogPrefix(correlationId)+"appended stringpart ["+partSessionKey+"] with value ["+partValue+"]");
					}
				}
			}
		}

		return soapMessage;
	}

	/**
	 * Create a MessageFactory singleton
	 * @param soapProtocol see {@link SOAPConstants} for possible values
	 * @return previously initialized or newly created MessageFactory
	 * @throws SOAPException when it's not possible to instantiate a new MessageFactory
	 */
	private synchronized MessageFactory getMessageFactory(String soapProtocol) throws SOAPException {
		if(!factory.containsKey(soapProtocol)) {
			log.info("creating new MessageFactory for soapProtocol ["+soapProtocol+"]");
			factory.put(soapProtocol, MessageFactory.newInstance(soapProtocol));
		}

		log.debug("using cached MessageFactory for soapProtocol ["+soapProtocol+"]");
		return factory.get(soapProtocol);
	}

	/**
	 * Add log prefix to make it easier to debug
	 * @param correlationId message identifier
	 */
	protected String getLogPrefix(String correlationId) {
		return "correlationId["+correlationId+"] ";
	}

	/**
	 * Actually process the request
	 * @param correlationId message identifier
	 * @param message message that was received
	 * @param pipelineSession messageContext (containing attachments if available)
	 * @return response to send back
	 */
	abstract String processRequest(String correlationId, String message, IPipeLineSession pipelineSession) throws ListenerException;

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
}
