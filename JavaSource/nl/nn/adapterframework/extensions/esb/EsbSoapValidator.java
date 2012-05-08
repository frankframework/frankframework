/*
 * $Log: EsbSoapValidator.java,v $
 * Revision 1.4  2012-05-08 15:55:38  m00f069
 * Fix wrong namespace prefix in wsdl:part element.
 *
 * Revision 1.3  2012/04/12 08:50:02  Jaco de Groot <jaco.de.groot@ibissource.org>
 * CommonMessageHeader.xsd namespace has changed (uri -> http)
 *
 */
package nl.nn.adapterframework.extensions.esb;

import java.util.Collection;
import java.util.Collections;

import javax.xml.namespace.QName;

import nl.nn.adapterframework.soap.SoapValidator;

/**
 * This is a SoapValidator, but it presupposes ESB wrapping of the body.
 * @author Michiel Meeuwissen
 * @author Jaco de Groot
 * @version Id
 */
public class EsbSoapValidator extends SoapValidator {

    public  static final String GENERIC_HEADER_XMLNS   = "http://nn.nl/XSD/Generic/MessageHeader/1";
    public  static final QName  GENERIC_HEADER_TAG     = new QName(GENERIC_HEADER_XMLNS, "MessageHeader");

    private static final String GENERIC_HEADER_XSD     = "/xml/xsd/CommonMessageHeader.xsd";

    // This is unused now, we use to to have an extra tag on the output.
    private static final QName  GENERIC_RESULT_TAG     = new QName(GENERIC_HEADER_XMLNS, "Result");

    public static enum Direction {
        INPUT,
        OUTPUT
    }

    private Direction direction = null;

    @Override
    public void setSchemaLocation(String schemaLocation) {
        super.setSchemaLocation(GENERIC_HEADER_XMLNS + " " + GENERIC_HEADER_XSD + " " + schemaLocation);
    }

    protected int getDefaultNamespaceIndex() {
        return 2;
    }

    @Override
    public void setSoapHeader(String bodyTags) {
        throw new IllegalArgumentException("Esb soap is unmodifiable, it is: " + getSoapHeaderTags());
    }

    @Override
    public Collection<QName> getSoapHeaderTags() {
        return Collections.singleton(GENERIC_HEADER_TAG);
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = Direction.valueOf(direction.toUpperCase());
    }
}
