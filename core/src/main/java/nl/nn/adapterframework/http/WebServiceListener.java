/*
   Copyright 2013, 2018-2019 Nationale-Nederlanden, 2020, 2022-2023 WeAreFrank!

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
package nl.nn.adapterframework.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.soap.SOAPConstants;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.jaxws.EndpointImpl;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.HasSpecialDefaultValues;
import nl.nn.adapterframework.configuration.SuppressKeys;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.http.cxf.MessageProvider;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.receivers.ServiceDispatcher;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.StringUtil;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Listener that allows a {@link Receiver} to receive messages as a SOAP webservice.
 * The structure of the SOAP messages is expressed in a WSDL (Web Services Description Language) document.
 * The Frank!Framework generates a WSDL document for each adapter that contains WebServiceListeners. You can
 * find these documents in the Frank!Console under main menu item Webservices, heading Available WSDL's.
 * The WSDL documents that we generate document how the SOAP services can be accessed. In particular, the
 * URL of a SOAP service can be found in an XML element <code>&lt;soap:address&gt;</code> with
 * <code>soap</code> pointing to namespace <code>http://schemas.xmlsoap.org/wsdl/soap/</code>.
 *
 * <br/>If <code>address</code> is set, then for each request:<ul>
 * <li>MIME headers are described in a 'mimeHeaders'-XML stored under session key 'mimeHeaders'</li>
 * <li>Attachments present in the request are described by an 'attachments'-XML stored under session key 'attachments'</li>
 * <li>SOAP protocol is stored under a session key 'soapProtocol'</li>
 * <li>SOAP action is stored under a session key 'SOAPAction'</li>
 * </ul>
 * and for each response a multipart message is constructed if a 'multipart'-XML is provided in sessionKey specified by multipartXmlSessionKey.
 *
 * @author Gerrit van Brakel
 * @author Jaco de Groot
 * @author Niels Meijer
 */
public class WebServiceListener extends PushingListenerAdapter implements HasPhysicalDestination, HasSpecialDefaultValues {

	private final @Getter(onMethod = @__(@Override)) String domain = "Http";
	private @Getter boolean soap = true;
	private @Getter String serviceNamespaceURI;
	private SoapWrapper soapWrapper = null;
	private String servletUrlMapping = AppConstants.getInstance().getString("servlet.SoapProviderServlet.urlMapping", "services");

	/* CXF Implementation */
	private @Getter String address;
	private @Getter boolean mtomEnabled = false;
	private @Getter String attachmentSessionKeys = "";
	private @Getter String multipartXmlSessionKey = "multipartXml";
	private List<String> attachmentSessionKeysList = new ArrayList<>();
	private EndpointImpl endpoint = null;
	private SpringBus cxfBus;

	/**
	 * initialize listener and register <code>this</code> to the JNDI
	 */
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if(StringUtils.isEmpty(getAddress()) && isMtomEnabled())
			throw new ConfigurationException("can only use MTOM when address attribute has been set");

		if(StringUtils.isNotEmpty(getAddress()) && getAddress().contains(":"))
			throw new ConfigurationException("address cannot contain colon ( : ) character");

		if (StringUtils.isNotEmpty(getAttachmentSessionKeys())) {
			attachmentSessionKeysList.addAll(StringUtil.split(getAttachmentSessionKeys(), " ,;"));
		}

		if (isSoap()) {
//			String msg = ClassUtils.nameOf(this) +"["+getName()+"]: the use of attribute soap=true has been deprecated. Please use the SoapWrapperPipe instead";
//			ConfigurationWarnings.getInstance().add(log, msg, true);

			soapWrapper = SoapWrapper.getInstance();
		}

		if (StringUtils.isEmpty(getServiceNamespaceURI()) && StringUtils.isEmpty(getAddress())) {
			String msg = "calling webservices via de ServiceDispatcher_ServiceProxy is deprecated. Please specify an address or serviceNamespaceURI and modify the call accordingly";
			ConfigurationWarnings.add(this, log, msg, SuppressKeys.DEPRECATION_SUPPRESS_KEY, null);
		}
		if (StringUtils.isNotEmpty(getServiceNamespaceURI()) && StringUtils.isNotEmpty(getAddress())) {
			String msg = "Please specify either an address or serviceNamespaceURI but not both";
			ConfigurationWarnings.add(this, log, msg);
		}

		Bus bus = getApplicationContext().getBean("cxf", Bus.class);
		if(bus instanceof SpringBus) {
			cxfBus = (SpringBus) bus;
			log.debug("found CXF SpringBus id ["+bus.getId()+"]");
		} else {
			throw new ConfigurationException("unable to find SpringBus, cannot register "+this.getClass().getSimpleName());
		}
	}

	@Override
	public void open() throws ListenerException {
		if (StringUtils.isNotEmpty(getAddress())) {
			log.debug("registering listener ["+getName()+"] with JAX-WS CXF Dispatcher on SpringBus ["+cxfBus.getId()+"]");
			endpoint = new EndpointImpl(cxfBus, new MessageProvider(this, getMultipartXmlSessionKey()));
			endpoint.publish("/"+getAddress()); //TODO: prepend with `local://` when used without application server
			SOAPBinding binding = (SOAPBinding)endpoint.getBinding();
			binding.setMTOMEnabled(isMtomEnabled());

			if(endpoint.isPublished()) {
				log.debug("published listener ["+getName()+"] on CXF endpoint ["+getAddress()+"]");
			} else {
				log.error("unable to publish listener ["+getName()+"] on CXF endpoint ["+getAddress()+"]");
			}
		} else {
			if (StringUtils.isNotEmpty(getServiceNamespaceURI())) {
				log.debug("registering listener ["+getName()+"] with ServiceDispatcher by serviceNamespaceURI ["+getServiceNamespaceURI()+"]");
				ServiceDispatcher.getInstance().registerServiceClient(getServiceNamespaceURI(), this);
			}
			else {
				log.debug("registering listener ["+getName()+"] with ServiceDispatcher");
				ServiceDispatcher.getInstance().registerServiceClient(getName(), this); //Backwards compatibility
			}
		}

		super.open();
	}

	@Override
	public void close() {
		super.close();

		if(endpoint != null && endpoint.isPublished()) {
			endpoint.stop();
		}

		if (StringUtils.isEmpty(getAddress())) {
			if (StringUtils.isNotEmpty(getServiceNamespaceURI())) {
				log.debug("unregistering listener ["+getName()+"] from ServiceDispatcher by serviceNamespaceURI ["+getServiceNamespaceURI()+"]");
				ServiceDispatcher.getInstance().unregisterServiceClient(getServiceNamespaceURI());
			}
			else {
				log.debug("unregistering listener ["+getName()+"] from ServiceDispatcher");
				ServiceDispatcher.getInstance().unregisterServiceClient(getName()); //Backwards compatibility
			}
		}
	}

	@Override
	public Message processRequest(Message message, PipeLineSession session) throws ListenerException {
		if (!attachmentSessionKeysList.isEmpty()) {
			XmlBuilder xmlMultipart = new XmlBuilder("parts");
			for(String attachmentSessionKey: attachmentSessionKeysList) {
				//<parts><part type=\"file\" name=\"document.pdf\" sessionKey=\"part_file\" size=\"12345\" mimeType=\"application/octet-stream\"/></parts>
				XmlBuilder part = new XmlBuilder("part");
				part.addAttribute("name", attachmentSessionKey);
				part.addAttribute("sessionKey", attachmentSessionKey);
				part.addAttribute("mimeType", "application/octet-stream");
				xmlMultipart.addSubElement(part);
			}
			session.put(getMultipartXmlSessionKey(), xmlMultipart.toXML());
		}

		if (isSoap()) {
			try {
				if (log.isDebugEnabled()) log.debug(getLogPrefix()+"received SOAPMSG [" + message + "]");
				Message request = soapWrapper.getBody(message, false, session, null);
				Message result = super.processRequest(request, session);

				String soapNamespace = SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE;
				String soapProtocol = (String) session.get("soapProtocol");
				if(SOAPConstants.SOAP_1_2_PROTOCOL.equals(soapProtocol)) {
					soapNamespace = SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE;
				}
				Message reply = soapWrapper.putInEnvelope(result, null, null, null, null, soapNamespace, null, false);
				if (log.isDebugEnabled()) log.debug(getLogPrefix()+"replied SOAPMSG [" + reply + "]");
				return reply;
			} catch (Exception e) {
				throw new ListenerException(e);
			}
		}

		return super.processRequest(message, session);
	}

	public String getLogPrefix() {
		return "WebServiceListener ["+getName()+"] listening on ["+getPhysicalDestinationName()+"] ";
	}

	@Override
	public String getPhysicalDestinationName() {
		if(StringUtils.isNotEmpty(getAddress())) {
			return "address [/"+servletUrlMapping+"/"+getAddress()+"]";
		}
		else if (StringUtils.isNotEmpty(getServiceNamespaceURI())) {
			return "serviceNamespaceURI ["+getServiceNamespaceURI()+"]";
		}
		return "name ["+getName()+"]";
	}

	/**
	 * If <code>true</code> the SOAP envelope is removed from received messages and a SOAP envelope is added to returned messages (SOAP envelope will not be visible to the pipeline)
	 * @ff.default true
	 */
	public void setSoap(boolean b) {
		soap = b;
	}

	/**
	 * Namespace of the service that is provided by the adapter of this listener.
	 * If specified, requests posted to https://mydomain.com/ibis4something/servlet/rpcrouter that have this namespace in their body  will be handled by this listener,
	 * where mydomain.com and ibis4something refer to 'your ibis'.
	 */
	public void setServiceNamespaceURI(String string) {
		serviceNamespaceURI = string;
	}

	public void setApplicationFaultsAsSoapFaults(boolean b) {
		setApplicationFaultsAsExceptions(b);
	}

	/**
	 * The address to listen to, e.g the part &lt;address&gt; in https://mydomain.com/ibis4something/services/&lt;address&gt;,
	 * where mydomain.com and ibis4something refer to 'your ibis'.
	 */
	public void setAddress(String address) {
		if(!address.isEmpty()) {
			if(address.startsWith("/"))
				this.address = address.substring(1);
			else
				this.address = address;
		}
	}

	/** If set, MTOM is enabled on the SOAP binding */
	public void setMtomEnabled(boolean mtomEnabled) {
		this.mtomEnabled = mtomEnabled;
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

	@Override
	public Object getSpecialDefaultValue(String attributeName, Object defaultValue, Map<String, String> attributes) {
		if ("address".equals(attributeName)) {
			return getAddressDefaultValue(attributes.get("name"));
		}
		return defaultValue;
	}

	private static String getAddressDefaultValue(String name) {
		return "/" + name;
	}
}
