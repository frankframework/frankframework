package nl.nn.adapterframework.validation;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.log4j.Logger;
import org.custommonkey.xmlunit.Diff;
import org.xml.sax.InputSource;

/**
 * The representation of a XSD.
 * 
 * @author Michiel Meeuwissen
 * @author  Jaco de Groot
 */
public class XSD implements Comparable<XSD> {
    private static final Logger LOG = LogUtil.getLogger(XSD.class);

    private static String TEST_RESOURCE_IN_THE_ROOT = "Configuration.xml";

    public final URL url;
    public final String namespace;
    public final boolean addNamespaceToSchema;
    public final String parentLocation;
    public final boolean isRootXsd;
    public final String targetNamespace;
    public final List<String> rootTags = new ArrayList<String>();

    public XSD(URL url, String namespace, boolean addNamespaceToSchema,
            String parentLocation, boolean isRootXsd) throws IOException, XMLStreamException {
        this.url = url;
        this.namespace = namespace;
        this.addNamespaceToSchema = addNamespaceToSchema;
        this.parentLocation = parentLocation;
        this.isRootXsd = isRootXsd;
        String tns = null;
        if (url == null) throw new IllegalArgumentException("No resource " + url + " found");
        InputStream in = url.openStream();
        XMLEventReader er = XmlUtils.INPUT_FACTORY.createXMLEventReader(in,
                XmlUtils.STREAM_FACTORY_ENCODING);
        int elementDepth = 0;
        while (er.hasNext()) {
            XMLEvent e = er.nextEvent();
            switch (e.getEventType()) {
            case XMLStreamConstants.START_ELEMENT:
                elementDepth++;
                StartElement el = e.asStartElement();
                if (el.getName().equals(SchemaUtils.SCHEMA)) {
                    Attribute a = el.getAttributeByName(SchemaUtils.TNS);
                    if (a != null) {
                        tns = a.getValue();
                    }
                } else if (el.getName().equals(SchemaUtils.ELEMENT)) {
                    if (elementDepth == 2) {
                        rootTags.add(el.getAttributeByName(SchemaUtils.NAME).getValue());
                    }
                }
                break;
            case XMLStreamConstants.END_ELEMENT:
                elementDepth--;
                break;
            }
        }
        this.targetNamespace = tns;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof XSD) {
            XSD other = (XSD) o;
            if (compareTo(other) == 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return namespace.hashCode();
    }

    @Override
    public String toString() {
        return namespace + " " + url;
    }

    public int compareTo(XSD x) {
        if (x == null) return 1;
        if (namespace != null && x.namespace != null) {
            int c = namespace.compareTo(x.namespace);
            if (c != 0) return c;
        }
        if (url.toString().compareTo(x.url.toString()) != 0) {
            // Compare XSD content to prevent copies of the same XSD showing up
            // more than once in the WSDL. For example the
            // CommonMessageHeader.xsd used by the EsbSoapValidator will
            // normally also be imported by the XSD for the business response
            // message (for the Result part).
            try {
                InputSource control = new InputSource(url.openStream());
                InputSource test = new InputSource(x.url.openStream());
                Diff diff = new Diff(control, test);
                if (diff.similar()) {
                    return 0;
                }
            } catch (Exception e) {
                LOG.warn("Exception during XSD compare", e);
            }
        }
        return url.toString().compareTo(x.url.toString());
    }

    /**
     * Tries to determin the base url wich must be used for relative resolving of resources.
     * @TODO Too much ad hoc logic here
     * @return An url representing the 'directory' of the current XSD. Ending in /.
     */
    public String getBaseUrl() {
        String u = url.toString();
        String baseUrl;
        if (url.toString().startsWith("file:")) {
            URL testRoot = XSD.class.getResource("/" + TEST_RESOURCE_IN_THE_ROOT);
            if (testRoot != null) {
                baseUrl = u.substring(
                    testRoot.toString().length()
                        - TEST_RESOURCE_IN_THE_ROOT.length());
            } else {
                throw new IllegalStateException(TEST_RESOURCE_IN_THE_ROOT + " not found");
            }
        } else {
            baseUrl = u.substring(u.indexOf("!/") + 2);
        }
        String classes = "WEB-INF/classes/";
        int index= baseUrl.indexOf(classes);
        if (index > -1) {
            baseUrl = baseUrl.substring(index + classes.length());
        }
        baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf('/') + 1);
        return baseUrl;
    }

    public String getName() {
        String u = url.toString();
        int slash = u.lastIndexOf('/');
        u = u.substring(slash + 1);
        try {
            return URLDecoder.decode(u, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // cannot happen, UTF-8 is supported
            return u;
        }
    }

    public Set<XSD> getXsdsRecursive()
            throws IOException, XMLStreamException {
        return getXsdsRecursive(new HashSet<XSD>());
    }

    public Set<XSD> getXsdsRecursive(Set<XSD> xsds)
            throws IOException, XMLStreamException {
        InputStream in = url.openStream();
        if (in == null) return null;
        XMLEventReader er = XmlUtils.INPUT_FACTORY.createXMLEventReader(in,
                XmlUtils.STREAM_FACTORY_ENCODING);
        while (er.hasNext()) {
            XMLEvent e = er.nextEvent();
            switch (e.getEventType()) {
            case XMLStreamConstants.START_ELEMENT:
                StartElement el = e.asStartElement();
                if (el.getName().equals(SchemaUtils.IMPORT) ||
                    el.getName().equals(SchemaUtils.INCLUDE)
                    ) {
                    Attribute schemaLocation = el.getAttributeByName(SchemaUtils.SCHEMALOCATION);
                    String namespace = this.namespace;
                    boolean addNamespaceToSchema = this.addNamespaceToSchema;
                    if (el.getName().equals(SchemaUtils.IMPORT)) {
                        Attribute attribute =
                                el.getAttributeByName(SchemaUtils.NAMESPACE);
                        if (attribute != null) {
                            namespace = attribute.getValue();
                        } else {
                            namespace = targetNamespace;
                        }
                        addNamespaceToSchema = false;
                    }
                    XSD x = new XSD(
                            ClassUtils.getResourceURL(getBaseUrl() + schemaLocation.getValue()),
                            namespace, addNamespaceToSchema, getBaseUrl(),
                            false
                            );
                    if (xsds.add(x)) {
                        x.getXsdsRecursive(xsds);
                    }
                }
                break;
            }
        }
        return xsds;
    }

}
