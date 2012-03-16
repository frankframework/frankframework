package nl.nn.adapterframework.soap;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.*;

import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.*;
import nl.nn.adapterframework.http.WebServiceListener;
import nl.nn.adapterframework.jms.JmsListener;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

import javanet.staxutils.*;
import javanet.staxutils.events.AttributeEvent;
import javanet.staxutils.events.StartElementEvent;

/**
 * @author Michiel Meeuwissen
 */
public abstract class WsdlUtils {


    private static final Logger LOG = LogUtil.getLogger(WsdlUtils.class);

    private WsdlUtils() {
        // this class has no instances
    }

    static Collection<IListener> getListeners(IAdapter a) {
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

    static String getServiceNameSpaceURI(IAdapter a) {
        for(IListener l : getListeners(a)) {
            if (l instanceof WebServiceListener) {
                WebServiceListener wsl = (WebServiceListener) l;
                return wsl.getServiceNamespaceURI();
            }
        }
        AppConstants appConstants = AppConstants.getInstance();
        String tns = appConstants.getProperty("wsdl.targetNamespace." + a.getName() + ".soapAction");
        if (tns == null) tns = appConstants.getProperty("wsdl.targetNamespace.domain");
        if (tns == null) tns = "${wsdl.targetNamespace.domain}";
        return "http://" + tns + "/" + a.getName();
    }

    static XMLStreamWriter createWriter(OutputStream out, boolean indentWsdl) throws XMLStreamException {
        XMLStreamWriter w = Wsdl.OUTPUT_FACTORY.createXMLStreamWriter(out, Wsdl.ENCODING);
        if (indentWsdl) {
            IndentingXMLStreamWriter iw = new IndentingXMLStreamWriter(w);
            iw.setIndent(" ");
            w = iw;
        }
        return w;
    }

    /**
     * A utility method to find the Adapter by namespace.
     * @TODO This is not very related to WSDL, and perhaps can move to a more generic utility class.
     * @param ibisManager
     * @param targetNamespace
     * @return
           */
    static Adapter getAdapterByNamespaceUri(IbisManager ibisManager, String targetNamespace) {
        Configuration configuration = ibisManager.getConfiguration();
        Iterator i = configuration.getRegisteredAdapters().iterator();
        while (i.hasNext()) {
            IAdapter a = (IAdapter) i.next();

            String nameSpace = getServiceNameSpaceURI(a);
            if (nameSpace != null && nameSpace.equals(targetNamespace)) {
                return (Adapter) a;
            }
        }
        throw new IllegalStateException("No adapter found for " + targetNamespace);
    }

    /**
     * 'Include' another XSD with an xsd:include tag. So this does not actually include the full XSD, just a reference.
     * @param xsds
     * @param w
     * @param correctingNameSpaces
     * @throws java.io.IOException
     * @throws javax.xml.stream.XMLStreamException
     */
    static void xsincludeXSDs(final String nameSpace, final Collection<XSD> xsds, final XMLStreamWriter w, final Map<String, String> correctingNameSpaces) throws IOException, XMLStreamException {
        w.writeStartElement(Wsdl.XSD, "schema");
        w.writeAttribute("targetNamespace", nameSpace); {
            for (XSD xsd : xsds) {
                if (! nameSpace.equals(xsd.nameSpace)) {
                    throw new IllegalArgumentException("The XSD " + xsd + " is for the wrong name space");
                }
                w.writeEmptyElement(Wsdl.XSD, "include");
                w.writeAttribute("schemaLocation", xsd.getBaseUrl() + xsd.getName());

                // including the XSD with includeXSD will also parse it, e.g. to determin the first tag.
                // We don't need to actually include it here, so we just throw away the result.
                includeXSD(xsd, Wsdl.OUTPUT_FACTORY.createXMLStreamWriter(new OutputStream() {
                    @Override
                    public void write(int i) throws IOException {
                        // /dev/null
                    }
                }, Wsdl.ENCODING), correctingNameSpaces, true);
            }
        }
        w.writeEndElement();


    }

    /**
     * Including a {@link nl.nn.adapterframework.soap.XSD} into an {@link javax.xml.stream.XMLStreamWriter} while parsing it. It is parsed (using a low level {@link javax.xml.stream.XMLEventReader} so that certain
     * things can be corrected on the fly.  Most importantly, sometimes the XSD uses namespaces which are undesired, and can be 'corrected'.
     * @param xsd
     * @param xmlStreamWriter
     * @param correctingNamespaces
     * @param standalone           When standalone the start and end document contants are ignored.
     * @throws java.io.IOException
     * @throws javax.xml.stream.XMLStreamException
     */
    static void includeXSD(final XSD xsd, XMLStreamWriter xmlStreamWriter, Map<String, String> correctingNamespaces, final boolean standalone) throws IOException, XMLStreamException {
        final XMLStreamEventWriter streamEventWriter = new XMLStreamEventWriter(
            new NamespaceCorrectingXMLStreamWriter(xmlStreamWriter, correctingNamespaces));
        InputStream in = xsd.url.toURL().openStream();

        String xsdNamespace = correct(correctingNamespaces, xsd.nameSpace);

        if (in == null) throw new IllegalStateException("" + xsd + " not found");
        XMLEventReader er = Wsdl.INPUT_FACTORY.createXMLEventReader(in, Wsdl.ENCODING);
        String wrongNamespace = null;
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
                    if (el.getName().equals(Wsdl.SCHEMA)) {
                        if (xsdNamespace == null) {
                            xsdNamespace = correct(correctingNamespaces, el.getAttributeByName(Wsdl.TNS).getValue());
                        }
                        if (xsdNamespace != null) {
                            StartElement ne =
                                XmlUtils.mergeAttributes(el,
                                    Arrays.asList(
                                        new AttributeEvent(Wsdl.TNS, xsdNamespace)
                                    ).iterator(),
                                    Arrays.asList(
                                        Wsdl.EVENT_FACTORY.createNamespace(xsdNamespace)
                                    ).iterator(),
                                    Wsdl.EVENT_FACTORY
                                );

                            if (!ne.equals(e)) {
                                List<AttributeEvent> list = new ArrayList<AttributeEvent>();
                                list.add(new AttributeEvent(Wsdl.ELFORMDEFAULT, "qualified"));
                                //list.add(

                                Attribute targetNameSpace = el.getAttributeByName(Wsdl.TNS);
                                e = XMLStreamUtils.mergeAttributes(ne, list.iterator(), Wsdl.EVENT_FACTORY);
                                if (targetNameSpace != null) {
                                    String currentTarget = targetNameSpace.getValue();
                                    if (! currentTarget.equals(xsdNamespace)) {
                                        wrongNamespace = currentTarget;
                                        if (!correctingNamespaces.containsKey(wrongNamespace)) {
                                            correctingNamespaces.put(wrongNamespace, xsdNamespace);
                                            LOG.warn(xsd.url + " Corrected " + el + " -> " + e);
                                        }
                                    }

                                }
                            }
                        }
                    } else if (el.getName().equals(Wsdl.IMPORT)) {
                        Attribute schemaLocation = el.getAttributeByName(Wsdl.SCHEMALOCATION);
                        if (schemaLocation != null) {
                            String location = schemaLocation.getValue();
                            String relativeTo = xsd.parentLocation;
                            if (relativeTo.length() > 0 && location.startsWith(relativeTo)) {
                                location = location.substring(relativeTo.length());
                            }
                            e =
                                XMLStreamUtils.mergeAttributes(el,
                                    Collections.singletonList(new AttributeEvent(Wsdl.SCHEMALOCATION, location)).iterator(), Wsdl.EVENT_FACTORY);
                            if (LOG.isDebugEnabled()) {
                                LOG.debug(xsd.url + " Corrected " + el + " -> " + e);
                                LOG.debug(xsd.url + " Relative to : " + relativeTo + " -> " + e);
                            }
                        }
                    } else if (el.getName().equals(Wsdl.ELEMENT)) {
                        if (xsd.firstTag == null) {
                            xsd.firstTag = el.getAttributeByName(Wsdl.NAME).getValue();
                        }

                    } else {
                        if (wrongNamespace != null) {
                            if (wrongNamespace.equals(el.getName().getNamespaceURI())) {
                                StartElementEvent ne = new StartElementEvent(
                                    new QName(xsdNamespace,
                                        el.getName().getLocalPart(),
                                        el.getName().getPrefix()),
                                    el.getAttributes(),
                                    el.getNamespaces(),
                                    el.getNamespaceContext(),
                                    el.getLocation(),
                                    el.getSchemaType());
                                e = ne;
                            }
                        }
                    }
                    break;
                default:
                    // simply copy
            }
            streamEventWriter.add(e);
        }
        streamEventWriter.flush();
    }

    /**
     * Uses a Map to 'correct' values. If there is no corresponding key in the map, the value itself will simply be returned, otherwise the corrected value which is the value in the map.
     */
    static <C> C correct(final Map<C, C> map, final C value) {
        C corrected = map.get(value);
        return corrected != null ? corrected : value;
    }
}
