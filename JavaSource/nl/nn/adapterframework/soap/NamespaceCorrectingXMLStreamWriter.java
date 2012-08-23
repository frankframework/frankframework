package nl.nn.adapterframework.soap;

import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import javanet.staxutils.helpers.StreamWriterDelegate;

/**
 * This writer simply filters an other writer. It copies everything, except it maps 'wrong' namespaces to correct ones.
 * @author Michiel Meeuwissen
 */
class NamespaceCorrectingXMLStreamWriter extends StreamWriterDelegate {
    private final Map<String, String> map;

    NamespaceCorrectingXMLStreamWriter(XMLStreamWriter out, Map<String, String> map) {
        super(out);
        this.map = map;
    }
    protected String correct(String ns) {
        return WsdlUtils.correct(map, ns);
    }
    @Override
    public void writeAttribute(String prefix, String namespaceURI, String localName, String value) throws XMLStreamException {
        super.writeAttribute(prefix, correct(namespaceURI), localName, value);
    }

    @Override
    public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
        super.writeNamespace(prefix, correct(namespaceURI));

    }
    @Override
    public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
        super.writeDefaultNamespace(correct(namespaceURI));
    }


}
