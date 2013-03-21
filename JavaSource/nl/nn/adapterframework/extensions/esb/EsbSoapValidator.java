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
package nl.nn.adapterframework.extensions.esb;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.soap.SoapValidator;

/**
 * This is a SoapValidator, but it presupposes ESB wrapping of the body.
 * @author Michiel Meeuwissen
 * @author Jaco de Groot
 * @version $Id$
 */
public class EsbSoapValidator extends SoapValidator {

    private static class HeaderInformation {
        final String xmlns;
        final String xsd;
        final QName tag;
        HeaderInformation(String xmlns, String xsd) {
            this.xmlns = xmlns;
            this.xsd   = xsd;
            this.tag   = new QName(this.xmlns, "MessageHeader");
        }

    }

    private static final Map<EsbSoapWrapperPipe.Mode, HeaderInformation> GENERIC_HEADER;
    static {
        Map<EsbSoapWrapperPipe.Mode, HeaderInformation> temp = new HashMap<EsbSoapWrapperPipe.Mode, HeaderInformation>();
        temp.put(EsbSoapWrapperPipe.Mode.REG, new HeaderInformation("http://nn.nl/XSD/Generic/MessageHeader/1", "/xml/xsd/CommonMessageHeader.xsd"));
        temp.put(EsbSoapWrapperPipe.Mode.I2T, new HeaderInformation("http://nn.nl/XSD/Generic/MessageHeader/1", "/xml/xsd/CommonMessageHeader.xsd"));
        temp.put(EsbSoapWrapperPipe.Mode.BIS, new HeaderInformation("http://www.ing.com/CSP/XSD/General/Message_2", "/xml/xsd/cspXML_2.xsd"));
        GENERIC_HEADER = new EnumMap<EsbSoapWrapperPipe.Mode, HeaderInformation>(temp);
    }

    // This is unused now, we use to to have an extra tag on the output.
    //private static final QName  GENERIC_RESULT_TAG     = new QName(GENERIC_HEADER_XMLNS, "Result");

    public static enum Direction {
        INPUT,
        OUTPUT
    }

    private Direction direction = null;
    private EsbSoapWrapperPipe.Mode mode = EsbSoapWrapperPipe.Mode.REG;
    private String explicitSchemaLocation = null;

    @Override
    public void configure() throws ConfigurationException {
        super.setSoapHeader(GENERIC_HEADER.get(mode).tag.getLocalPart());
        super.configure();
    }

    @Override
    public void setSchemaLocation(String schemaLocation) {
        super.setSchemaLocation(schemaLocation + " " + GENERIC_HEADER.get(mode).xmlns + " " + GENERIC_HEADER.get(mode).xsd);
        explicitSchemaLocation = schemaLocation;
    }

    @Override
    public void setSoapHeader(String soapHeader) {
        throw new IllegalArgumentException("Esb soap is unmodifiable, it is: " + getSoapHeader());
    }

    public String getDirection() {
        return direction.toString();
    }

    public void setDirection(String direction) {
        this.direction = Direction.valueOf(direction.toUpperCase());
    }

    public void setMode(String mode) { // Why does PropertyUtil not understand enums?
        this.mode = EsbSoapWrapperPipe.Mode.valueOf(mode.toUpperCase());
        if (explicitSchemaLocation != null) setSchemaLocation(explicitSchemaLocation);
    }

    public String getMode() {
        return mode.toString();
    }
}
