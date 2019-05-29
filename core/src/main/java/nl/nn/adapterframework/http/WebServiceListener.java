/*
   Copyright 2013, 2018 - 2019 Nationale-Nederlanden

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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.soap.SOAPConstants;
import javax.xml.ws.soap.SOAPBinding;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.HasSpecialDefaultValues;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.extensions.cxf.MessageProvider;
import nl.nn.adapterframework.receivers.ServiceDispatcher;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.EndpointImpl;

/**
 * Implementation of a {@link nl.nn.adapterframework.core.IPushingListener IPushingListener} that enables a {@link nl.nn.adapterframework.receivers.GenericReceiver}
 * to receive messages as a web-service.
 * 
 * @author Gerrit van Brakel
 * @author Jaco de Groot
 * @author Niels Meijer
 */
public class WebServiceListener extends PushingListenerAdapter implements Serializable, HasPhysicalDestination, HasSpecialDefaultValues {

	private static final long serialVersionUID = 1L;

	private boolean soap = true;
	private String serviceNamespaceURI;
	private SoapWrapper soapWrapper = null;

	/* CXF Implementation */
	private String address;
	private boolean mtomEnabled = false;
	private String attachmentSessionKeys = "";
	private String multipartXmlSessionKey = "multipartXml";
	private List<String> attachmentSessionKeysList = new ArrayList<String>();
	private EndpointImpl endpoint = null;

	/**
	 * initialize listener and register <code>this</code> to the JNDI
	 */
	public void configure() throws ConfigurationException {
		super.configure();

		if(StringUtils.isEmpty(getAddress()) && isMtomEnabled())
			throw new ConfigurationException("can only use MTOM when address attribute has been set");

		if(StringUtils.isNotEmpty(getAddress()) && getAddress().contains(":"))
			throw new ConfigurationException("address cannot contain colon ( : ) character");

		if (StringUtils.isNotEmpty(getAttachmentSessionKeys())) {
			StringTokenizer stringTokenizer = new StringTokenizer(getAttachmentSessionKeys(), " ,;");
			while (stringTokenizer.hasMoreTokens()) {
				attachmentSessionKeysList.add(stringTokenizer.nextToken());
			}
		}

		if (isSoap()) {
//			String msg = ClassUtils.nameOf(this) +"["+getName()+"]: the use of attribute soap=true has been deprecated. Please use the SoapWrapperPipe instead";
//			ConfigurationWarnings.getInstance().add(log, msg, true);

			soapWrapper = SoapWrapper.getInstance();
		}

		if (StringUtils.isEmpty(getServiceNamespaceURI()) && StringUtils.isEmpty(getAddress())) {
			String msg = ClassUtils.nameOf(this) +"["+getName()+"]: calling webservices via de ServiceDispatcher_ServiceProxy is deprecated. Please specify an address or serviceNamespaceURI and modify the call accordingly";
			ConfigurationWarnings.getInstance().add(log, msg, true);
		}
	}

	@Override
	public void open() throws ListenerException {
		super.open();

		if (StringUtils.isNotEmpty(getAddress())) {
			log.debug("registering listener ["+getName()+"] with JAX-WS CXF Dispatcher");
			endpoint = new EndpointImpl(BusFactory.getDefaultBus(), new MessageProvider(this, getMultipartXmlSessionKey()));
			endpoint.publish("/"+getAddress());
			SOAPBinding binding = (SOAPBinding)endpoint.getBinding();
			binding.setMTOMEnabled(isMtomEnabled());

			if(endpoint.isPublished())
				log.debug("published listener ["+getName()+"] on CXF endpoint["+getAddress()+"] with SpringBus["+endpoint.getBus().getId()+"]");
			else
				log.error("unable to publish listener ["+getName()+"] on CXF endpoint["+getAddress()+"]");
		}

		//Can bind on multiple endpoints
		if (StringUtils.isNotEmpty(getServiceNamespaceURI())) {
			log.debug("registering listener ["+getName()+"] with ServiceDispatcher by serviceNamespaceURI ["+getServiceNamespaceURI()+"]");
			ServiceDispatcher.getInstance().registerServiceClient(getServiceNamespaceURI(), this);
		}
		else {
			log.debug("registering listener ["+getName()+"] with ServiceDispatcher");
			ServiceDispatcher.getInstance().registerServiceClient(getName(), this); //Backwards compatibility
		}
	}

	@Override
	public void close() {
		super.close();

		if(endpoint != null && endpoint.isPublished())
			endpoint.stop();

		//TODO maybe unregister oldschool rpc based serviceclients!?
		//How does this work when reloading a configuration??
	}

	@Override
	public String processRequest(String correlationId, String message, Map requestContext) throws ListenerException {
		if (attachmentSessionKeysList.size() > 0) {
			XmlBuilder xmlMultipart = new XmlBuilder("parts");
			for(String attachmentSessionKey: attachmentSessionKeysList) {
				//<parts><part type=\"file\" name=\"document.pdf\" sessionKey=\"part_file\" size=\"12345\" mimeType=\"application/octet-stream\"/></parts>
				XmlBuilder part = new XmlBuilder("part");
				part.addAttribute("name", attachmentSessionKey);
				part.addAttribute("sessionKey", attachmentSessionKey);
				part.addAttribute("mimeType", "application/octet-stream");
				xmlMultipart.addSubElement(part);
			}
			requestContext.put(getMultipartXmlSessionKey(), xmlMultipart.toXML());
		}

		if (isSoap()) {
			try {
				log.debug(getLogPrefix()+"received SOAPMSG [" + message + "]");
				String request = soapWrapper.getBody(message);
				String result = super.processRequest(correlationId, request, requestContext);

				String soapNamespace = SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE;
				String soapProtocol = (String) requestContext.get("soapProtocol");
				if(SOAPConstants.SOAP_1_2_PROTOCOL.equals(soapProtocol))
					soapNamespace = SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE;

				String reply = soapWrapper.putInEnvelope(result, null, null, null, null, soapNamespace, null, false);
				log.debug(getLogPrefix()+"replied SOAPMSG [" + reply + "]");
				return reply;
			} catch (Exception e) {
				throw new ListenerException(e);
			}
		}
		else
			return super.processRequest(correlationId, message, requestContext);
	}

	public String getLogPrefix() {
		return "WebServiceListener ["+getName()+"] listening on ["+getPhysicalDestinationName()+"] ";
	}

	public String getPhysicalDestinationName() {
		if(StringUtils.isNotEmpty(getAddress())) {
			return "address ["+getAddress()+"]";
		}
		else if (StringUtils.isNotEmpty(getServiceNamespaceURI())) {
			return "serviceNamespaceURI ["+getServiceNamespaceURI()+"]";
		}
		return "name ["+getName()+"]";
	}

	@IbisDoc({"when <code>true</code> the soap envelope is removed from received messages and a soap envelope is added to returned messages (soap envelope will not be visible to the pipeline)", "<code>true</code>"})
	public void setSoap(boolean b) {
		soap = b;
	}
	public boolean isSoap() {
		return soap;
	}

	public String getServiceNamespaceURI() {
		return serviceNamespaceURI;
	}

	@IbisDoc({"namespace of the service that is provided by the adapter of this listener", ""})
	public void setServiceNamespaceURI(String string) {
		serviceNamespaceURI = string;
	}

	public boolean isApplicationFaultsAsSoapFaults() {
		return isApplicationFaultsAsExceptions();
	}
	public void setApplicationFaultsAsSoapFaults(boolean b) {
		setApplicationFaultsAsExceptions(b);
	}

	@IbisDoc({ "The address to listen to, e.g the part <address> in https://mydomain.com/ibis4something/services/<address>, where mydomain.com and ibis4something refer to 'your ibis'","" })
	public void setAddress(String address) {
		if(!address.isEmpty()) {
			if(address.startsWith("/"))
				this.address = address.substring(1);
			else
				this.address = address;
		}
	}
	public String getAddress() {
		return address;
	}

	public void setMtomEnabled(boolean mtomEnabled) {
		this.mtomEnabled = mtomEnabled;
	}
	public boolean isMtomEnabled() {
		return mtomEnabled;
	}

	public void setAttachmentSessionKeys(String attachmentSessionKeys) {
		this.attachmentSessionKeys = attachmentSessionKeys;
	}
	public String getAttachmentSessionKeys() {
		return attachmentSessionKeys;
	}

	public void setMultipartXmlSessionKey(String multipartXmlSessionKey) {
		this.multipartXmlSessionKey = multipartXmlSessionKey;
	}
	public String getMultipartXmlSessionKey() {
		return multipartXmlSessionKey;
	}

	public Object getSpecialDefaultValue(String attributeName,
			Object defaultValue, Map<String, String> attributes) {
		if ("address".equals(attributeName)) {
			return getAddressDefaultValue(attributes.get("name"));
		}
		return defaultValue;
	}

	private static String getAddressDefaultValue(String name) {
		return "/" + name;
	}
}
