package nl.nn.adapterframework.soap;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javanet.staxutils.IndentingXMLStreamWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.pipes.XmlValidator;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.xerces.util.XMLChar;

/**
 * @author Michiel Meeuwissen
 * @author Jaco de Groot
 */
public abstract class WsdlUtils {

    private WsdlUtils() {
        // this class has no instances
    }

    public static Collection<IListener> getListeners(IAdapter a) {
        List<IListener> result = new ArrayList<IListener>();
        Iterator j = a.getReceiverIterator();
        while (j.hasNext()) {
            Object o = j.next();
            if (o instanceof ReceiverBase) {
                ReceiverBase r = (ReceiverBase) o;
                result.add(r.getListener());
            }
        }
        return result;
    }

    public static String getEsbSoapParadigm(XmlValidator xmlValidator) {
        if (xmlValidator instanceof SoapValidator) {
            String soapBody = ((SoapValidator)xmlValidator).getSoapBody();
            if (soapBody != null) {
                int i = soapBody.lastIndexOf('_');
                if (i != -1) {
                    return soapBody.substring(i + 1);
                }
            }
        }
        return null;
    }

    public static String getFirstNamespaceFromSchemaLocation(XmlValidator inputValidator) {
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
