package nl.nn.adapterframework.validation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javanet.staxutils.XMLStreamEventWriter;
import javanet.staxutils.XMLStreamUtils;
import javanet.staxutils.events.AttributeEvent;
import javanet.staxutils.events.StartElementEvent;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.log4j.Logger;

/**
 * @author Michiel Meeuwissen
 * @author Jaco de Groot
 */
public class SchemaUtils {
    protected static final Logger LOG = LogUtil.getLogger(SchemaUtils.class);

    public static final String XSD          = XMLConstants.W3C_XML_SCHEMA_NS_URI;//"http://www.w3.org/2001/XMLSchema";

    public static final QName SCHEMA         = new QName(XSD, "schema");
    public static final QName ELEMENT        = new QName(XSD,  "element");
    public static final QName IMPORT         = new QName(XSD,  "import");
    public static final QName INCLUDE        = new QName(XSD,  "include");
    public static final QName TNS            = new QName(null, "targetNamespace");
    public static final QName ELFORMDEFAULT  = new QName(null, "elementFormDefault");
    public static final QName SCHEMALOCATION = new QName(null, "schemaLocation");
    public static final QName NAMESPACE      = new QName(null, "namespace");
    public static final QName NAME           = new QName(null, "name");

    public static Set<XSD> getXsds(String schemaLocation,
            List<String> excludes, boolean addNamespaceToSchema,
            boolean checkSchemaLocationOnly)
            throws IOException, XMLStreamException {
        Set<XSD> xsds = new TreeSet<XSD>();
        if (schemaLocation != null) {
            String[] split =  schemaLocation.trim().split("\\s+");
            if (split.length % 2 != 0) throw new IllegalStateException("The schema must exist from an even number of strings, but it is " + schemaLocation);
            if (!checkSchemaLocationOnly) {
                for (int i = 0; i < split.length; i += 2) {
                    if (!(excludes != null
                            && excludes.contains(split[i]))) {
                        xsds.add(getXSD(split[i + 1], split[i],
                                addNamespaceToSchema, true));
                    }
                }
            }
        }
        return xsds;
    }

    public static Set<XSD> getXsdsRecursive(Set<XSD> xsds)
            throws IOException, XMLStreamException {
      Set<XSD> xsdsRecursive = new TreeSet<XSD>();
      xsdsRecursive.addAll(xsds);
      for (XSD xsd : xsds) {
          xsdsRecursive.addAll(xsd.getXsdsRecursive());
      }
      return xsdsRecursive;
    }

    public static Map<String, Set<XSD>> getXsdsGroupedByNamespace(Set<XSD> xsds)
                throws XMLStreamException, IOException {
        Map<String, Set<XSD>> result = new TreeMap<String, Set<XSD>>();
        for (XSD xsd : xsds) {
            Set<XSD> set = result.get(xsd.namespace);
            if (set == null) {
                set = new TreeSet<XSD>();
                result.put(xsd.namespace, set);
            }
            set.add(xsd);
        }
        return result;
    }

    public static XSD getXSD(String resource, String ns, boolean addNamespaceToSchema, boolean rootXsd) throws IOException, XMLStreamException {
        URL url = ClassUtils.getResourceURL(resource);
        if (url == null) {
            throw new IllegalArgumentException("No such resource " + resource);
        }
        XSD xsd = new XSD(url, ns, addNamespaceToSchema, "", rootXsd);
        return  xsd;
    }

    public static void
            mergeRootXsdsGroupedByNamespaceToSchemasWithIncludes(
            Map<String, Set<XSD>> rootXsdsGroupedByNamespace,
            XMLStreamWriter xmlStreamWriter)
            throws IOException, XMLStreamException {
        // As the root XSD's are written as includes there's no need to change
        // the imports and includes in the root XSD's.
        for (String namespace: rootXsdsGroupedByNamespace.keySet()) {
            xmlStreamWriter.writeStartElement(XSD, "schema");
            xmlStreamWriter.writeAttribute("targetNamespace", namespace);
            for (XSD xsd : rootXsdsGroupedByNamespace.get(namespace)) {
                xmlStreamWriter.writeEmptyElement(XSD, "include");
                xmlStreamWriter.writeAttribute("schemaLocation",
                        xsd.getBaseUrl() + xsd.getName());
            }
            xmlStreamWriter.writeEndElement();
        }
    }

    
    /**
     * @return a map with ByteArrayOutputStream's when xmlStreamWriter is null,
     *         otherwise write to xmlStreamWriter
     */
    public static Map<String, ByteArrayOutputStream>
            mergeXsdsGroupedByNamespaceToSchemasWithoutIncludes (
            Map<String, Set<XSD>> xsdsGroupedByNamespace,
            XMLStreamWriter xmlStreamWriter)
            throws XMLStreamException, IOException {
        Map<String, ByteArrayOutputStream> result =
                new HashMap<String, ByteArrayOutputStream>();
        for (String namespace: xsdsGroupedByNamespace.keySet()) {
            Set<XSD> xsds = xsdsGroupedByNamespace.get(namespace);
            // Get attributes of root element and get import elements from all XSD's
            List<Attribute> rootAttributes = new ArrayList<Attribute>();
            List<Attribute> rootNamespaceAttributes = new ArrayList<Attribute>();
            List<XMLEvent> imports = new ArrayList<XMLEvent>();
            for (XSD xsd: xsds) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                XMLStreamWriter w = XmlUtils.OUTPUT_FACTORY.createXMLStreamWriter(byteArrayOutputStream, XmlUtils.STREAM_FACTORY_ENCODING);
                xsdToXmlStreamWriter(xsd, w, false, true, false, false,
                        rootAttributes, rootNamespaceAttributes, imports, true);
            }
            // Remove doubles
            for (int i = 0; i < rootAttributes.size(); i++) {
                Attribute attribute1 = rootAttributes.get(i);
                for (int j = 0; j < rootAttributes.size(); j++) {
                    Attribute attribute2 = rootAttributes.get(j);
                    if (i != j && XmlUtils.attributesEqual(attribute1, attribute2)) {
                        rootAttributes.remove(j);
                    }
                }
            }
            for (int i = 0; i < rootNamespaceAttributes.size(); i++) {
                Attribute attribute1 = rootNamespaceAttributes.get(i);
                for (int j = 0; j < rootNamespaceAttributes.size(); j++) {
                    Attribute attribute2 = rootNamespaceAttributes.get(j);
                    if (i != j && XmlUtils.attributesEqual(attribute1, attribute2)) {
                        rootNamespaceAttributes.remove(j);
                    }
                }
            }
            // Write XSD's with merged root element
            XMLStreamWriter w;
            if (xmlStreamWriter == null) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                result.put(namespace, byteArrayOutputStream);
                w = XmlUtils.OUTPUT_FACTORY.createXMLStreamWriter(byteArrayOutputStream, XmlUtils.STREAM_FACTORY_ENCODING);
            } else {
                w = xmlStreamWriter;
            }
            int i = 0;
            for (XSD xsd: xsds) {
                i++;
                boolean skipFirstElement = true;
                boolean skipLastElement = true;
                if (xsds.size() == 1) {
                    skipFirstElement = false;
                    skipLastElement = false;
                } else {
                    if (i == 1) {
                        skipFirstElement = false;
                    } else if (i == xsds.size()) {
                        skipLastElement = false;
                    }
                }
                xsdToXmlStreamWriter(xsd, w, false, true,
                        skipFirstElement, skipLastElement, rootAttributes,
                        rootNamespaceAttributes, imports, false);
            }
        }
        return result;
    }

    public static void xsdToXmlStreamWriter(final XSD xsd,
            XMLStreamWriter xmlStreamWriter, boolean standalone)
                    throws IOException, XMLStreamException {
        xsdToXmlStreamWriter(xsd, xmlStreamWriter, standalone, false);
    }

    public static void xsdToXmlStreamWriter(XSD xsd,
            XMLStreamWriter xmlStreamWriter, boolean standalone,
            boolean stripSchemaLocationFromImport)
                    throws IOException, XMLStreamException {
        xsdToXmlStreamWriter(xsd, xmlStreamWriter, standalone,
                stripSchemaLocationFromImport, false, false, null, null, null,
                false);
    }

    /**
     * Including a {@link nl.nn.adapterframework.validation.XSD} into an
     * {@link javax.xml.stream.XMLStreamWriter} while parsing it. It is parsed
     * (using a low level {@link javax.xml.stream.XMLEventReader} so that
     * certain things can be corrected on the fly.
     * @param xsd
     * @param xmlStreamWriter
     * @param standalone
     * When standalone the start and end document contants are ignored, hence
     * the xml declaration is ignored.
     * @param stripSchemaLocationFromImport
     * Useful when generating a WSDL which should contain all XSD's inline
     * (without includes or imports). The XSD might have an import with
     * schemaLocation to make it valid on it's own, when
     * stripSchemaLocationFromImport is true it will be removed.
     * @throws java.io.IOException
     * @throws javax.xml.stream.XMLStreamException
     */
    public static void xsdToXmlStreamWriter(final XSD xsd,
            XMLStreamWriter xmlStreamWriter, boolean standalone,
            boolean stripSchemaLocationFromImport, boolean skipRootStartElement,
            boolean skipRootEndElement, List<Attribute> rootAttributes,
            List<Attribute> rootNamespaceAttributes, List<XMLEvent> imports,
            boolean noOutput) throws IOException, XMLStreamException {
        Map<String, String> namespacesToCorrect = new HashMap<String, String>();
        NamespaceCorrectingXMLStreamWriter namespaceCorrectingXMLStreamWriter =
                new NamespaceCorrectingXMLStreamWriter(xmlStreamWriter,
                        namespacesToCorrect);
        final XMLStreamEventWriter streamEventWriter = new XMLStreamEventWriter(
                namespaceCorrectingXMLStreamWriter);
        InputStream in = xsd.url.openStream();
        if (in == null) {
            throw new IllegalStateException("" + xsd + " not found");
        }
        XMLEventReader er =
                XmlUtils.INPUT_FACTORY.createXMLEventReader(
                        in, XmlUtils.STREAM_FACTORY_ENCODING);
        while (er.hasNext()) {
            XMLEvent e = er.nextEvent();
            switch (e.getEventType()) {
                case XMLStreamConstants.START_DOCUMENT:
                case XMLStreamConstants.END_DOCUMENT:
                    if (! standalone) {
                        continue;
                    }
                    // fall through
                case XMLStreamConstants.SPACE:
                case XMLStreamConstants.COMMENT:
                    break;
                case XMLStreamConstants.START_ELEMENT:
                    StartElement el = e.asStartElement();
                    if (SCHEMA.equals(el.getName())) {
                        if (skipRootStartElement) {
                            continue;
                        }
                        if (rootAttributes != null) {
                            // Collect or write attributes of schema element.
                            if (noOutput) {
                                // First call to this method collecting
                                // schema attributes.
                                Iterator iterator = el.getAttributes();
                                while (iterator.hasNext()) {
                                    Attribute attribute = (Attribute)iterator.next();
                                    rootAttributes.add(attribute);
                                }
                                iterator = el.getNamespaces();
                                while (iterator.hasNext()) {
                                    Attribute attribute = (Attribute)iterator.next();
                                    rootNamespaceAttributes.add(attribute);
                                }
                            } else {
                                // Second call to this method writing attributes
                                // from previous call.
                                el = XmlUtils.EVENT_FACTORY.createStartElement(
                                        el.getName().getPrefix(),
                                        el.getName().getNamespaceURI(),
                                        el.getName().getLocalPart(),
                                        rootAttributes.iterator(),
                                        rootNamespaceAttributes.iterator(),
                                        el.getNamespaceContext());
                            }
                        }
                        if (xsd.addNamespaceToSchema) {
                            e = XmlUtils.mergeAttributes(el,
                                    Arrays.asList(
                                        new AttributeEvent(TNS, xsd.namespace),
                                        new AttributeEvent(ELFORMDEFAULT, "qualified")
                                    ).iterator(),
                                    Arrays.asList(
                                        XmlUtils.EVENT_FACTORY.createNamespace(xsd.namespace)
                                    ).iterator(),
                                    XmlUtils.EVENT_FACTORY
                                );
                            if (!e.equals(el)) {
                                Attribute tns = el.getAttributeByName(TNS);
                                if (tns != null) {
                                    String s = tns.getValue();
                                    if (!s.equals(xsd.namespace)) {
                                        namespacesToCorrect.put(s, xsd.namespace);
                                    }
                                }
                                LOG.debug(xsd.url + " Corrected " + el + " -> " + e);
                            }
                        } else {
                            e = el;
                        }
                        if (imports != null && !noOutput) {
                            // Second call to this method writing imports
                            // collected in previous call.
                            // List contains start and end elements, hence add
                            // 2 on every iteration.
                            for (int i = 0; i < imports.size(); i = i + 2) {
                                boolean skip = false;
                                for (int j = 0; j < i; j = j + 2) {
                                    Attribute attribute1 =
                                            imports.get(i).asStartElement().getAttributeByName(NAMESPACE);
                                    Attribute attribute2 =
                                            imports.get(j).asStartElement().getAttributeByName(NAMESPACE);
                                    if (attribute1 != null && attribute2 != null
                                            && attribute1.getValue().equals(attribute2.getValue())) {
                                        skip = true;
                                    }
                                }
                                if (!skip) {
                                    streamEventWriter.add(e);
                                    e = imports.get(i);
                                    streamEventWriter.add(e);
                                    e = imports.get(i + 1);
                                }
                            }
                        }
                    } else if (el.getName().equals(INCLUDE)) {
                        continue;
                    } else if (el.getName().equals(IMPORT)) {
                        if (imports == null || noOutput) {
                            // Not collecting or writing import elements.
                            Attribute schemaLocation = el.getAttributeByName(SCHEMALOCATION);
                            if (schemaLocation != null) {
                                String location = schemaLocation.getValue();
                                if (stripSchemaLocationFromImport) {
                                    List<Attribute> attributes = new ArrayList<Attribute>();
                                    Iterator<Attribute> iterator = el.getAttributes();
                                    while (iterator.hasNext()) {
                                        Attribute a = iterator.next();
                                        if (!SCHEMALOCATION.equals(a.getName())) {
                                            attributes.add(a);
                                        }
                                    }
                                    e = new StartElementEvent(
                                            el.getName(),
                                            attributes.iterator(),
                                            el.getNamespaces(),
                                            el.getNamespaceContext(),
                                            el.getLocation(),
                                            el.getSchemaType());
                                } else {
                                    String relativeTo = xsd.parentLocation;
                                    if (relativeTo.length() > 0 && location.startsWith(relativeTo)) {
                                        location = location.substring(relativeTo.length());
                                    }
                                    e =
                                        XMLStreamUtils.mergeAttributes(el,
                                            Collections.singletonList(new AttributeEvent(SCHEMALOCATION, location)).iterator(), XmlUtils.EVENT_FACTORY);
                                    if (LOG.isDebugEnabled()) {
                                        LOG.debug(xsd.url + " Corrected " + el + " -> " + e);
                                        LOG.debug(xsd.url + " Relative to : " + relativeTo + " -> " + e);
                                    }
                                }
                            }
                        }
                        if (imports != null) {
                            // Collecting or writing import elements.
                            if (noOutput) {
                                // First call to this method collecting
                                // imports.
                                imports.add(e);
                            }
                            continue;
                        }
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    EndElement ee = e.asEndElement();
                    if (ee.getName().equals(SCHEMA)) {
                        if (skipRootEndElement) {
                            continue;
                        }
                    } else if (ee.getName().equals(INCLUDE)) {
                        continue;
                    } else if (imports != null) {
                        if (ee.getName().equals(IMPORT)) {
                            if (noOutput) {
                                imports.add(e);
                            }
                            continue;
                        }
                    }
                    break;
                default:
                    // simply copy
            }
            if (!noOutput) {
                streamEventWriter.add(e);
            }
        }
        streamEventWriter.flush();
    }

}
