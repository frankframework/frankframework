/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.soap;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.xerces.util.XMLChar;

import javanet.staxutils.IndentingXMLStreamWriter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IXmlValidator;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * @author Michiel Meeuwissen
 * @author Jaco de Groot
 */
public abstract class WsdlUtils {

    private WsdlUtils() {
        // this class has no instances
    }

	public static Collection<IListener> getListeners(IAdapter adapter) {
		List<IListener> result = new ArrayList<IListener>();
		for (Iterator<Receiver> it = adapter.getReceiverIterator(); it.hasNext();) {
			Receiver receiver = it.next();
			result.add(receiver.getListener());
		}
		return result;
	}

      // 2017-10-17 Previous version, before 
//    public static String getEsbSoapParadigm(XmlValidator xmlValidator, boolean outputMode) {
//        if (xmlValidator instanceof SoapValidator) {
//        	String soapBody;
//        	if (outputMode) {
//            	soapBody = ((SoapValidator)xmlValidator).getOutputSoapBody();
//        	} else {
//            	soapBody = ((SoapValidator)xmlValidator).getSoapBody();
//        	}
//            if (soapBody != null) {
//                int i = soapBody.lastIndexOf('_');
//                if (i != -1) {
//                    return soapBody.substring(i + 1);
//                }
//            }
//        }
//        return null;
//    }
    
    
    
    public static String getEsbSoapParadigm(IXmlValidator xmlValidator) {
    	String soapBody = xmlValidator.getMessageRoot();
        if (soapBody != null) {
            int i = soapBody.lastIndexOf('_');
            if (i != -1) {
                return soapBody.substring(i + 1);
            }
        }
        return null;
    }

    public static String getFirstNamespaceFromSchemaLocation(IXmlValidator inputValidator) {
        String schemaLocation = inputValidator.getSchemaLocation();
        if (schemaLocation != null) {
            String[] split =  schemaLocation.trim().split("\\s+");
            if (split.length > 0) {
                return split[0];
            }
        }
        return null;
    }

    public static XMLStreamWriter getWriter(OutputStream out, boolean indentWsdl) throws XMLStreamException {
        XMLStreamWriter w = XmlUtils.REPAIR_NAMESPACES_OUTPUT_FACTORY
                .createXMLStreamWriter(out, XmlUtils.STREAM_FACTORY_ENCODING);
        if (indentWsdl) {
            IndentingXMLStreamWriter iw = new IndentingXMLStreamWriter(w);
            iw.setIndent("\t");
            w = iw;
        }
        return w;
    }

    static String getNCName(String name) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            if (i == 0) {
                buf.append(XMLChar.isNCNameStart(name.charAt(i)) ? name.charAt(i) : '_');
            } else {
                buf.append(XMLChar.isNCName(name.charAt(i)) ? name.charAt(i) : '_');
            }
        }
        return buf.toString();
    }

    static String validUri(String uri) {
        return uri == null ? null : uri.replaceAll(" ", "_");
    }

}
