/*
   Copyright 2013 Nationale-Nederlanden, 2021 WeAreFrank!

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
package org.frankframework.soap;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import javanet.staxutils.IndentingXMLStreamWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.util.XMLChar;
import org.frankframework.core.Adapter;
import org.frankframework.core.IListener;
import org.frankframework.core.IValidator;
import org.frankframework.core.IXmlValidator;
import org.frankframework.http.WebServiceListener;
import org.frankframework.receivers.Receiver;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.XmlUtils;

/**
 * @author Michiel Meeuwissen
 * @author Jaco de Groot
 */
public class WsdlGeneratorUtils {

	private WsdlGeneratorUtils() {
		throw new IllegalStateException("Don't construct utility class");
	}

	public static Collection<IListener<?>> getListeners(Adapter adapter) {
		List<IListener<?>> result = new ArrayList<>();
		for (Receiver<?> receiver: adapter.getReceivers()) {
			result.add(receiver.getListener());
		}
		return result;
	}

	public static String getEsbSoapParadigm(IXmlValidator xmlValidator) {
		String soapBody = xmlValidator.getMessageRoot();
		if(soapBody != null) {
			int i = soapBody.lastIndexOf('_');
			if(i != -1) {
				return soapBody.substring(i + 1);
			}
		}
		return null;
	}

	public static String getFirstNamespaceFromSchemaLocation(IXmlValidator inputValidator) {
		String schemaLocation = inputValidator.getSchemaLocation();
		if(schemaLocation != null) {
			String[] split = schemaLocation.trim().split("\\s+");
			if(split.length > 0) {
				return split[0];
			}
		}
		return null;
	}

	public static XMLStreamWriter getWriter(OutputStream out, boolean indentWsdl) throws XMLStreamException {
		XMLStreamWriter w = XmlUtils.REPAIR_NAMESPACES_OUTPUT_FACTORY.createXMLStreamWriter(out, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
		if(indentWsdl) {
			IndentingXMLStreamWriter iw = new IndentingXMLStreamWriter(w);
			iw.setIndent("\t");
			w = iw;
		}
		return w;
	}

	static String getNCName(String name) {
		StringBuilder buf = new StringBuilder();
		for(int i = 0; i < name.length(); i++) {
			if(i == 0) {
				buf.append(XMLChar.isNCNameStart(name.charAt(i)) ? name.charAt(i) : '_');
			} else {
				buf.append(XMLChar.isNCName(name.charAt(i)) ? name.charAt(i) : '_');
			}
		}
		return buf.toString();
	}

	static String validUri(String uri) {
		return uri == null ? null : uri.replace(" ", "_");
	}

	// Check if the adapter has WebServiceListener with an InputValidator OR
	// InputValidator==SoapValidator OR
	// IXmlValidator.getSchema()!=NULL && webServiceListenerNamespace!=NULL
	public static boolean canProvideWSDL(Adapter adapter) {
		boolean hasWebServiceListener = false;
		String webServiceListenerNamespace = null;
		for (IListener<?> listener : WsdlGeneratorUtils.getListeners(adapter)) {
			if(listener instanceof WebServiceListener serviceListener) {
				hasWebServiceListener = true;
				webServiceListenerNamespace = serviceListener.getServiceNamespaceURI();
			}
		}

		IValidator inputValidator = adapter.getPipeLine().getInputValidator();
		if(inputValidator instanceof SoapValidator) { //We have to check this first as the SoapValidator cannot use getSchema()
			return true;
		} else if(inputValidator instanceof IXmlValidator xmlValidator) {
			if(xmlValidator.getSchema() != null) {
				return StringUtils.isNotEmpty(webServiceListenerNamespace);
			}
		}

		return hasWebServiceListener; //If not an IXmlValidator, return false
	}

}
