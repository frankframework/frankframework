/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.http.rpc;

import java.io.IOException;
import java.util.Map.Entry;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.ThreadContext;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SpringSecurityHandler;
import org.frankframework.http.AbstractHttpServlet;
import org.frankframework.http.HttpEntityType;
import org.frankframework.http.WebServiceListener;
import org.frankframework.http.mime.HttpEntityFactory;
import org.frankframework.http.mime.MultipartUtils;
import org.frankframework.lifecycle.DynamicRegistration;
import org.frankframework.lifecycle.IbisInitializer;
import org.frankframework.receivers.ServiceClient;
import org.frankframework.receivers.ServiceDispatcher;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;
import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;
import org.frankframework.util.XmlBuilder;

@Log4j2
@IbisInitializer
public class WebServiceListenerServlet extends AbstractHttpServlet implements DynamicRegistration.Servlet {
	private static final long serialVersionUID = 1L;
	private transient ServiceDispatcher sd;

	private static final boolean BACKWARDS_COMPATIBILITY_MODE = AppConstants.getInstance().getBoolean("WebServiceListener.backwardsCompatibleMultipartNotation", false);
	private static final boolean USE_CXF = AppConstants.getInstance().getBoolean("WebServiceListener.cxfServlet", false);

	private static final String SOAP_PROTOCOL_KEY = "soapProtocol";
	private static final String SOAP_ACTION = "SOAPAction";

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		sd = ServiceDispatcher.getInstance();
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String uri = cleanseURL(request.getPathInfo());
		final SoapMessage soapMessage;
		WebServiceListener listener;

		try {
			if (!"rpcrouter".equals(uri)) {
				// Address lookup, try and find the listener by name directly.
				listener = findService(uri);

				// Only parse the message if we've found a WebServiceListener
				soapMessage = (listener != null) ? SoapMessage.from(request) : null;
			} else {
				// RPC Router lookup, which requires parsing the entire message.
				// First try finding by SOAPAction.
				soapMessage = SoapMessage.from(request);
				SoapContext context = soapMessage.getSoapContext();
				listener = findService(context.getSoapAction());

				// If unsuccessful try namespaceURI instead.
				if (listener == null) {
					listener = findService(context.getNamespaceURI());
				}
			}
		} catch (Exception e) {
			log.warn("caught exception while trying to parse SOAP message", e);
			createSoapFault(response, "Could not process SOAP message", e);
			return ;
		}

		// Nothing was found?
		if (listener == null) {
			log.warn("no listener found for uri [{}], returning 404", uri);
			createSoapFault(response, "no service found for uri");
			return ;
		}

		try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.put(LogUtil.MDC_LISTENER_KEY, listener.getName());
			final PipeLineSession pipelineSession = new PipeLineSession()) {

			pipelineSession.put(PipeLineSession.HTTP_REQUEST_KEY, request);
			pipelineSession.put(PipeLineSession.HTTP_RESPONSE_KEY, response);
			pipelineSession.setSecurityHandler(new SpringSecurityHandler());

			Message result = processRequest(soapMessage, pipelineSession, listener);

			final boolean outputWritten = writeToResponseStream(response, result, listener, pipelineSession);
			if (!outputWritten) {
				log.debug("No output written, set content-type header to null");
				response.resetBuffer();
				response.setContentType(null);
			}
		} catch (Exception e) {
			log.warn("caught exception while processing message", e);
			createSoapFault(response, "Could not process SOAP message", e);
		} finally {
			ThreadContext.clearAll();
		}
	}

	@Nullable
	private WebServiceListener findService(String serviceName) {
		if (StringUtils.isBlank(serviceName) || sd == null) {
			log.debug("unable to find serviceName [{}]", serviceName);
			return null;
		}
		log.debug("trying to find serviceName from soapMessage [{}]", serviceName);

		ServiceClient service = sd.getListener(serviceName);
		if (!(service instanceof WebServiceListener listener)) {
			return null;
		}
		return listener;
	}

	protected static Message processRequest(SoapMessage request, PipeLineSession session, WebServiceListener listener) throws SOAPException, IOException {
		SoapContext context = request.getSoapContext();
		String messageId = context.getMessageId();
		PipeLineSession.updateListenerParameters(session, messageId, messageId);
		log.debug("received message");

		String soapProtocol = context.getSoapProtocol();
		log.debug("transforming from SOAP message using protocol [{}]", soapProtocol);
		session.put(SOAP_PROTOCOL_KEY, soapProtocol);

		// Should never be empty, but does happen at times?
		session.put(SOAP_ACTION, context.getSoapAction());

		// Make attachments in request (when present) available as session keys
		if (BACKWARDS_COMPATIBILITY_MODE) {
			handleIncomingAttachmentsLegacy(request, session);
		} else {
			session.putAll(request.getAttachments());
			session.put(MultipartUtils.MULTIPART_ATTACHMENTS_SESSION_KEY, request.getMultipartXml());
		}

		final Message output;
		try {
			log.debug("processing message");
			output = listener.processRequest(request.getBody(), session);
		} catch (ListenerException e) {
			throw new IOException("Could not process SOAP message", e);
		}

		return context.setMessageId(output);
	}

	/**
	 * This method uses a custom / different way to storing the multipart attachments in the PipeLineSession.
	 */
	private static void handleIncomingAttachmentsLegacy(SoapMessage request, PipeLineSession session) {
		int i = 1;
		XmlBuilder attachments = new XmlBuilder("attachments");
		for (Entry<String, Message> string : request.getAttachments().entrySet()) {
			Message attachmentPart = string.getValue();

			XmlBuilder attachment = new XmlBuilder("attachment");
			attachments.addSubElement(attachment);
			XmlBuilder sessionKey = new XmlBuilder("sessionKey");
			sessionKey.setValue("attachment" + i);
			attachment.addSubElement(sessionKey);
			session.put("attachment" + i, attachmentPart);
			log.debug("adding attachment [attachment{}] to session", i);

			attachment.addSubElement(getMimeHeadersXml(attachmentPart.getContext()));
			i++;
		}
		session.put("attachments", attachments.asXmlString());
	}

	private static XmlBuilder getMimeHeadersXml(MessageContext messageContext) {
		XmlBuilder xmlMimeHeaders = new XmlBuilder("mimeHeaders");
		messageContext.entrySet().forEach(e -> {
			String name = e.getKey();
			if (name.startsWith(MessageContext.HEADER_PREFIX)) {
				XmlBuilder xmlMimeHeader = new XmlBuilder("mimeHeader");
				xmlMimeHeader.addAttribute("name", name.substring(7));
				xmlMimeHeader.setValue("" + e.getValue());
				xmlMimeHeaders.addSubElement(xmlMimeHeader);
			}
		});
		return xmlMimeHeaders;
	}

	private static String cleanseURL(String pathInfo) {
		if (pathInfo == null) {
			return null;
		}
		String uri = pathInfo;

		if (uri.endsWith("/")) {
			uri = uri.substring(0, uri.length()-1);
		}
		if (uri.startsWith("/")) {
			uri = uri.substring(1);
		}
		return uri;
	}

	private static boolean writeToResponseStream(HttpServletResponse response, Message result, WebServiceListener listener, PipeLineSession session) throws IOException {
		response.resetBuffer();

		String attachmentXmlSessionKey = listener.getMultipartXmlSessionKey();
		final HttpEntityFactory entityFactory;
		if (StringUtils.isNotEmpty(attachmentXmlSessionKey) && session.containsKey(attachmentXmlSessionKey)) {
			log.debug("building multipart message with MultipartXmlSessionKey [{}]", attachmentXmlSessionKey);
			entityFactory = HttpEntityFactory.Builder.create()
					.entityType(listener.isMtomEnabled() ? HttpEntityType.MTOM : HttpEntityType.FORMDATA)
					.firstBodyPartName("message")
					.multipartXmlSessionKey(attachmentXmlSessionKey)
					.build();
		} else {
			String soapProtocol = session.get(SOAP_PROTOCOL_KEY, SOAPConstants.SOAP_1_1_PROTOCOL);
			ContentType contentType = SOAPConstants.SOAP_1_1_PROTOCOL.equals(soapProtocol) ? ContentType.create("text/xml") : ContentType.APPLICATION_SOAP_XML;
			entityFactory = HttpEntityFactory.Builder.create()
					.entityType(HttpEntityType.BINARY)
					.contentType(contentType)
					.build();
		}

		HttpEntity entity = entityFactory.create(result, null, session);

		long contentLength = entity.getContentLength();
		if (contentLength == 0L) {
			log.warn("no response entity to return, result [{}]", result);
			return false;
		}
		if (contentLength != Message.MESSAGE_SIZE_UNKNOWN) {
			response.setContentLengthLong(contentLength);
		}

		// Content-type might not be same as set before if we have a form. However it might also not be set.
		if (entity.getContentType() != null) {
			response.setContentType(entity.getContentType().getValue());
		}
		entity.writeTo(response.getOutputStream());

		// After reading the entire entity, we may have a more accurate value for content-length before flushing the output.
		if (entity.getContentLength() != contentLength) {
			response.setContentLengthLong(entity.getContentLength());
		}
		return true;
	}

	private void createSoapFault(HttpServletResponse response, String string) {
		createSoapFault(response, string, null);
	}

	private void createSoapFault(HttpServletResponse response, String faultString, Exception e) {
		response.resetBuffer();

		String msg = """
				<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
				<soap:Body><soap:Fault>
				<faultcode>soap:Server</faultcode>
				<faultstring>%s</faultstring>
				</soap:Fault></soap:Body></soap:Envelope>""".formatted(faultString);

		try {
			response.setContentType(MediaType.TEXT_XML_VALUE);
			response.getWriter().printf(msg);
		} catch (IOException e1) {
			e1.addSuppressed(e);
			log.error("unable to process SOAPFault", e1);
		}
	}

	@Override
	public String[] getAccessGrantingRoles() {
		return IBIS_FULL_SERVICE_ACCESS_ROLES;
	}

	@Override
	public String getUrlMapping() {
		return "/services/*,/servlet/*";
	}

	@Override
	public boolean isEnabled() {
		return !USE_CXF;
	}
}
