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

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.soap.SoapValidator;

/**
 * XmlValidator that will automatically add the SOAP envelope XSD and the ESB
 * XSD (e.g. CommonMessageHeader.xsd) to the set of XSD's used for validation.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>*</td><td>all attributes available on {@link SoapValidator} can be used</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMode(String) mode}</td><td>TODO</td><td>TODO</td></tr>
 * </table>
 *
 * @author Michiel Meeuwissen
 * @author Jaco de Groot
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

    private static final Map<String, HeaderInformation> GENERIC_HEADER;
    static {
        Map<String, HeaderInformation> temp = new HashMap<String, HeaderInformation>();
        temp.put(getModeKey(EsbSoapWrapperPipe.Mode.REG,1), new HeaderInformation("http://nn.nl/XSD/Generic/MessageHeader/1", "/xml/xsd/CommonMessageHeader.xsd"));
        temp.put(getModeKey(EsbSoapWrapperPipe.Mode.REG,2), new HeaderInformation("http://nn.nl/XSD/Generic/MessageHeader/2", "/xml/xsd/CommonMessageHeader_2.xsd"));
        temp.put(getModeKey(EsbSoapWrapperPipe.Mode.I2T), new HeaderInformation("http://nn.nl/XSD/Generic/MessageHeader/1", "/xml/xsd/CommonMessageHeader.xsd"));
        temp.put(getModeKey(EsbSoapWrapperPipe.Mode.BIS), new HeaderInformation("http://www.ing.com/CSP/XSD/General/Message_2", "/xml/xsd/cspXML_2.xsd"));
        GENERIC_HEADER = new HashMap<String, HeaderInformation>(temp);
    }

    // This is unused now, we use to to have an extra tag on the output.
    //private static final QName  GENERIC_RESULT_TAG     = new QName(GENERIC_HEADER_XMLNS, "Result");

    public static enum Direction {
        INPUT,
        OUTPUT
    }

    private Direction direction = null;
    private EsbSoapWrapperPipe.Mode mode = EsbSoapWrapperPipe.Mode.REG;
    private int cmhVersion = 0;

    @Override
    public void configure() throws ConfigurationException {
		if (mode == EsbSoapWrapperPipe.Mode.REG) {
			if (cmhVersion == 0) {
				cmhVersion = 1;
			} else if (cmhVersion < 0 || cmhVersion > 2) {
				ConfigurationWarnings.add(this, log, "cmhVersion ["+cmhVersion+ "] for mode ["+mode.toString()+"] should be set to '1' or '2', assuming '1'");
				cmhVersion = 1;
			}
		} else {
			if (cmhVersion != 0) {
				ConfigurationWarnings.add(this, log, "cmhVersion ["+cmhVersion+"] for mode ["+mode.toString()+"] should not be set, assuming '0'");
				cmhVersion = 0;
			}
		}
		super.setSchemaLocation(schemaLocation + " " + GENERIC_HEADER.get(getModeKey()).xmlns + " " + GENERIC_HEADER.get(getModeKey()).xsd);
		super.setSoapHeader(GENERIC_HEADER.get(getModeKey()).tag.getLocalPart());
		if (mode == EsbSoapWrapperPipe.Mode.I2T) {
			super.setImportedSchemaLocationsToIgnore("CommonMessageHeader.xsd");
			super.setUseBaseImportedSchemaLocationsToIgnore(true);
		} else if (mode == EsbSoapWrapperPipe.Mode.REG) {
			if (cmhVersion == 1) {
				super.setImportedSchemaLocationsToIgnore("CommonMessageHeader.xsd");
			} else {
				super.setImportedSchemaLocationsToIgnore("CommonMessageHeader_2.xsd");
			}
			super.setUseBaseImportedSchemaLocationsToIgnore(true);
		}
		super.configure();
	}

    private String getModeKey() {
    	return getModeKey(mode, cmhVersion);
    }

    private static String getModeKey(EsbSoapWrapperPipe.Mode mode) {
    	return getModeKey(mode, 0);
    }

    private static String getModeKey(EsbSoapWrapperPipe.Mode mode, int cmhVersion) {
    	return mode.toString() + "_" + cmhVersion;
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
    }

    public String getMode() {
        return mode.toString();
    }

	public void setCmhVersion(int i) {
		cmhVersion = i;
	}

	public int getCmhVersion() {
		return cmhVersion;
	}
}