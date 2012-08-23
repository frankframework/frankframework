package nl.nn.adapterframework.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javanet.staxutils.XMLStreamUtils;
import javanet.staxutils.events.AttributeEvent;

/**
 * @author Michiel Meeuwissen
 */
public class XsdUtils {

    protected static final Logger LOG = LogUtil.getLogger(XsdUtils.class);

    protected static final QName SCHEMA        = new QName("http://www.w3.org/2001/XMLSchema", "schema");
    protected static final QName TNS           = new QName(null, "targetNamespace");
    protected static final QName ELFORMDEFAULT = new QName(null, "elementFormDefault");
    protected static final QName DEFAULTXMLNS  = new QName(null, XMLConstants.XMLNS_ATTRIBUTE);


    /**
     * Wraps an inputstream into another InputStream which will implicetely add a targetNamespace to an XSD if it is missing, or correct it, if it is different
     * If there is no default namespace, it will also set that to this targetNamespace, under that assumption that that indeed is wat is meant.
     */
    public static InputStream targetNameSpaceAdding(InputStream source, String targetNameSpace) throws XMLStreamException {

        if (StringUtils.isBlank(targetNameSpace)) {
            LOG.debug("Given target namespace is blank, so no need to actually filter the input stream");
            return source;
        }
        XMLEventReader reader = XmlUtils.INPUT_FACTORY.createXMLEventReader(source, "UTF-8");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(); // using a copy-thread, it would be possible to avoid this buffer too.
        XMLEventWriter writer = XmlUtils.OUTPUT_FACTORY.createXMLEventWriter(buffer, "UTF-8");

        while(reader.hasNext()) {
            XMLEvent e = reader.nextEvent();
            if (e.isStartElement()) {
            StartElement el = e.asStartElement();
                if (el.getName().equals(SCHEMA)) {
                    boolean foundDefaultPrefix = false;
                    Iterator<Namespace> i = el.getNamespaces();
                    while (i.hasNext()) {
                        Namespace namespace = i.next();
                        if (StringUtils.isEmpty(namespace.getPrefix())) {
                            foundDefaultPrefix = true;
                            break;
                        }
                    }
                    List<AttributeEvent> attributes =
                        foundDefaultPrefix
                            ? Collections.singletonList(new AttributeEvent(TNS, targetNameSpace))
                            : Arrays.asList(new AttributeEvent(TNS, targetNameSpace), new AttributeEvent(DEFAULTXMLNS, targetNameSpace));

                    StartElement ne =
                        XMLStreamUtils.mergeAttributes(el, attributes.iterator(), XmlUtils.EVENT_FACTORY);


                    if (!ne.equals(e)) {
                        e = XMLStreamUtils.mergeAttributes(ne,
                            Collections.singletonList(new AttributeEvent(ELFORMDEFAULT, "qualified")).iterator(), XmlUtils.EVENT_FACTORY);
                        LOG.warn(" Corrected " + el + " -> " + e);

                    }
                }
            }
            writer.add(e);
        }
        writer.close();
        return new ByteArrayInputStream(buffer.toByteArray());
    }
}
