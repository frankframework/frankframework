package nl.nn.adapterframework.soap;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
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
import nl.nn.adapterframework.util.XmlUtils;

/**
 * The representation of a XSD in a WSDL.
 * This essentially bundles a resource (as an {@link java.net.URL} with a few other properties like the xml prefix and the
 * encountered 'first' tag (which can be used as default for the root tag)
 * @author Michiel Meeuwissen
 */
class XSD implements Comparable<XSD> {

    final String nameSpace;
    final URI url;
    final String pref;
    List<String> rootTags = new ArrayList<String>();
    final String parentLocation;

    XSD(String parentLocation, String nameSpace, URI resource, int prefixCount) {
        this.parentLocation = parentLocation;
        this.pref = "ns" + prefixCount;
        this.nameSpace = nameSpace;
        url = resource;
        if (url == null) throw new IllegalArgumentException("No resource " + resource + " found");
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof XSD) {
            XSD other = (XSD) o;
            return nameSpace.equals(other.nameSpace) && url.equals(other.url);
        } else {
            return false;
        }
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
        int c = nameSpace != null ? nameSpace.compareTo(x.nameSpace  == null ? "" : x.nameSpace) : 0;
        if (c != 0) return c;
        return url.toString().compareTo(x.url.toString());

    }

    public QName getTag(String tag) {
        return new QName(nameSpace, tag, pref);
    }

    private static String TEST_RESOURCE_IN_THE_ROOT = "Configuration.xml";


    /**
     * Tries to determin the base url wich must be used for relative resolving of resources.
     * @TODO Too much ad hoc logic here
     * @return An url representing the 'directory' of the current XSD. Ending in /.
     */
    public String getBaseUrl() {
        String u = url.toString();
        String baseUrl;
        if (url.getScheme().equals("file")) {
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

    public Set<XSD> getImportXSDs(Set set) throws IOException, XMLStreamException, URISyntaxException {
        if (set == null) set = new HashSet();
        InputStream in = url.toURL().openStream();
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
                        ClassUtils.getResourceURL(getBaseUrl() + schemaLocation.getValue()).toURI(), 0);
                    if (set.add(x)) {
                        x.getImportXSDs(set);
                    }
                }
                break;
            }
        }
        return set;
    }

}
