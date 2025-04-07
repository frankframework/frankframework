/*
   Copyright 2023 WeAreFrank!

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
package org.frankframework.extensions.esb;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.core.IJmsListener;
import org.frankframework.core.IListener;
import org.frankframework.core.PipeLine;
import org.frankframework.jms.JmsListener;
import org.frankframework.soap.WsdlGenerator;
import org.frankframework.soap.WsdlGeneratorExtensionContext;
import org.frankframework.soap.WsdlGeneratorUtils;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.StreamUtil;

public class EsbSoapWsdlGeneratorContext implements WsdlGeneratorExtensionContext {
	// Tibco BW will not detect the transport when SOAP_JMS_NAMESPACE is being used instead of ESB_SOAP_JMS_NAMESPACE.
	protected static final String ESB_SOAP_JMS_NAMESPACE		 = "http://www.tibco.com/namespaces/ws/2004/soap/binding/JMS";
	protected static final String ESB_SOAP_JNDI_NAMESPACE		= "http://www.tibco.com/namespaces/ws/2004/soap/apis/jndi";
	protected static final String ESB_SOAP_JNDI_NAMESPACE_PREFIX = "jndi";
	protected static final String ESB_SOAP_TNS_BASE_URI		  = "http://nn.nl/WSDL";

	String esbSoapBusinessDomain;
	String esbSoapServiceName;
	String esbSoapServiceContext;
	String esbSoapServiceContextVersion;
	String esbSoapOperationName;
	String esbSoapOperationVersion;
	String wsdlType;

	Set<String> warnings = new LinkedHashSet<>();

	protected void warn(String warning, Exception e) {
		warn(warning+": ("+ ClassUtils.nameOf(e)+") "+e.getMessage());
	}

	protected void warn(String warning) {
		warning = "Warning: " + warning;
		warnings.add(warning);
	}

	@Override
	public List<String> getWarnings() {
		return new ArrayList<>(warnings);
	}

	@Override
	public String getFilename() {
		return esbSoapBusinessDomain + "_"
				+ esbSoapServiceName + "_"
				+ (esbSoapServiceContext == null ? "" : esbSoapServiceContext + "_")
				+ esbSoapServiceContextVersion + "_"
				+ esbSoapOperationName + "_"
				+ esbSoapOperationVersion + "_"
				+ wsdlType;
	}

	@Override
	public String getTNS() {
		return ESB_SOAP_TNS_BASE_URI + "/"
				+ esbSoapBusinessDomain + "/"
				+ esbSoapServiceName + "/"
				+ (esbSoapServiceContext == null ? "" : esbSoapServiceContext + "/")
				+ esbSoapServiceContextVersion + "/"
				+ esbSoapOperationName + "/"
				+ esbSoapOperationVersion;
	}
	@Override
	public boolean hasSOAPActionName() {
		return esbSoapOperationName != null && esbSoapOperationVersion != null;
	}
	@Override
	public String getSOAPActionName() {
		return esbSoapOperationName + "_" + esbSoapOperationVersion;
	}

	@Override
	public void setExtensionNamespacePrefixes(XMLStreamWriter w) throws XMLStreamException {
		w.setPrefix(WsdlGenerator.SOAP_JMS_NAMESPACE_PREFIX, ESB_SOAP_JMS_NAMESPACE);
		w.setPrefix(ESB_SOAP_JNDI_NAMESPACE_PREFIX, ESB_SOAP_JNDI_NAMESPACE);
	}

	@Override
	public void addExtensionNamespaces(XMLStreamWriter w) throws XMLStreamException {
		w.writeNamespace(ESB_SOAP_JNDI_NAMESPACE_PREFIX, ESB_SOAP_JNDI_NAMESPACE);
	}

	@Override
	public void addJmsBindingInfo(XMLStreamWriter w, WsdlGenerator wsdlGenerator, PipeLine pipeLine) throws XMLStreamException {
		w.writeAttribute("transport", ESB_SOAP_JMS_NAMESPACE);
		w.writeEmptyElement(ESB_SOAP_JMS_NAMESPACE, "binding");
		w.writeAttribute("messageFormat", "Text");
		for (IListener<?> listener : WsdlGeneratorUtils.getListeners(pipeLine.getAdapter())) {
			if (listener instanceof JmsListener) {
				wsdlGenerator.writeSoapOperation(w, listener);
			}
		}
	}

	@Override
	public void addJmsServiceInfo(XMLStreamWriter w, IJmsListener listener) throws XMLStreamException {
		if (listener instanceof JmsListener jmsListener) {
			addJmsServiceInfo(w, jmsListener);
		}
	}

	public void addJmsServiceInfo(XMLStreamWriter w, JmsListener listener) throws XMLStreamException {
		writeEsbSoapJndiContext(w, listener);
		w.writeStartElement(ESB_SOAP_JMS_NAMESPACE, "connectionFactory"); {
			w.writeCharacters("externalJndiName-for-"
					+ listener.getQueueConnectionFactoryName()
					+ "-on-"
					+ AppConstants.getInstance().getProperty("dtap.stage"));
			w.writeEndElement();
		}
		w.writeStartElement(ESB_SOAP_JMS_NAMESPACE, "targetAddress"); {
			w.writeAttribute("destination", listener.getDestinationType().name().toLowerCase());
			String queueName = listener.getPhysicalDestinationShortName();
			if (queueName == null) {
				queueName = "queueName-for-"
						+ listener.getDestinationName() + "-on-"
						+ AppConstants.getInstance().getProperty("dtap.stage");
			}
			w.writeCharacters(queueName);
			w.writeEndElement();
		}
	}

	protected void writeEsbSoapJndiContext(XMLStreamWriter w, JmsListener listener) throws XMLStreamException {
		w.writeStartElement(ESB_SOAP_JNDI_NAMESPACE, "context"); {
			w.writeStartElement(ESB_SOAP_JNDI_NAMESPACE, "property"); {
				w.writeAttribute("name", "java.naming.factory.initial");
				w.writeAttribute("type", "java.lang.String");
				w.writeCharacters("com.tibco.tibjms.naming.TibjmsInitialContextFactory");
				w.writeEndElement();
			}
			w.writeStartElement(ESB_SOAP_JNDI_NAMESPACE, "property"); {
				w.writeAttribute("name", "java.naming.provider.url");
				w.writeAttribute("type", "java.lang.String");
				String qcf = listener.getQueueConnectionFactoryName();
				if (StringUtils.isEmpty(qcf)) {
					warn("Attribute queueConnectionFactoryName empty for listener '" + listener.getName() + "'");
				} else {
					try {
						qcf = URLEncoder.encode(qcf, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
					} catch (UnsupportedEncodingException e) {
						warn("Could not encode queueConnectionFactoryName for listener '" + listener.getName() + "'", e);
					}
				}
				String stage = AppConstants.getInstance().getProperty("dtap.stage");
				if (StringUtils.isEmpty(stage)) {
					warn("Property dtap.stage empty");
				} else {
					try {
						stage = URLEncoder.encode(stage, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
					} catch (UnsupportedEncodingException e) {
						warn("Could not encode property dtap.stage", e);
					}
				}
				w.writeCharacters("tibjmsnaming://host-for-" + qcf + "-on-" + stage + ":37222");
				w.writeEndElement();
			}
			w.writeStartElement(ESB_SOAP_JNDI_NAMESPACE, "property"); {
				w.writeAttribute("name", "java.naming.factory.object");
				w.writeAttribute("type", "java.lang.String");
				w.writeCharacters("com.tibco.tibjms.custom.CustomObjectFactory");
				w.writeEndElement();
			}
		}
		w.writeEndElement();
	}
}
