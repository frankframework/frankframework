package nl.nn.adapterframework.soap;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;
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
 * The representation of a XSD in a WSDL.
 * This essentially bundles a resource (as an {@link java.net.URL} with a few other properties like the xml prefix and the
 * encountered 'first' tag (which can be used as default for the root tag)
 * @author Michiel Meeuwissen
 * @author  Jaco de Groot
 */
class XSD implements Comparable<XSD> {
    private static final Logger LOG = LogUtil.getLogger(XSD.class);

    private static String TEST_RESOURCE_IN_THE_ROOT = "Configuration.xml";

    final String parentLocation;
    final String nameSpace;
    final URL url;
    final boolean isRootXsd;
    String prefix;
    List<String> rootTags = new ArrayList<String>();

    XSD(String parentLocation, String nameSpace, URL resource, boolean isRootXsd) {
        this.parentLocation = parentLocation;
        this.nameSpace = nameSpace;
        this.isRootXsd = isRootXsd;
        url = resource;
        if (url == null) throw new IllegalArgumentException("No resource " + resource + " found");
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
        return nameSpace.hashCode();
    }

    @Override
    public String toString() {
        return nameSpace + " " + url;
    }

    //@Override
    public int compareTo(XSD x) {
        if (x == null) return 1;
        int c = 0;
        if (nameSpace != null && x.nameSpace != null) {
            c = nameSpace.compareTo(x.nameSpace);
        }
        if (c != 0) return c;
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

    public QName getTag(String tag) {
        return new QName(nameSpace, tag, prefix);
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
            URL testRoot = Wsdl.class.getResource("/" + TEST_RESOURCE_IN_THE_ROOT);
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

    public Set<XSD> getImportXsds() throws IOException, XMLStreamException {
        return getImportXsds(new HashSet<XSD>());
    }

    public Set<XSD> getImportXsds(Set<XSD> xsds) throws IOException, XMLStreamException {
        InputStream in = url.openStream();
        if (in == null) return null;
        XMLEventReader er = XmlUtils.NAMESPACE_AWARE_INPUT_FACTORY.createXMLEventReader(in, WsdlUtils.ENCODING);
        while (er.hasNext()) {
            XMLEvent e = er.nextEvent();
            switch (e.getEventType()) {
            case XMLStreamConstants.START_ELEMENT:
                StartElement el = e.asStartElement();
                if (el.getName().equals(Wsdl.IMPORT) ||
                    el.getName().equals(Wsdl.INCLUDE)
                    ) {
                    Attribute schemaLocation = el.getAttributeByName(Wsdl.SCHEMALOCATION);
                    Attribute namespace  = el.getAttributeByName(Wsdl.NAMESPACE);
                    XSD x = new XSD(getBaseUrl(),
                        namespace == null ? null : namespace.getValue(),
                        ClassUtils.getResourceURL(getBaseUrl() + schemaLocation.getValue()),
                        false);
                    if (xsds.add(x)) {
                        x.getImportXsds(xsds);
                    }
                }
                break;
            }
        }
        return xsds;
    }

}
