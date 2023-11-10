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

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.doc.Category;
import nl.nn.adapterframework.soap.SoapValidator;

/**
 * XmlValidator that will automatically add the SOAP envelope XSD and the ESB XSD (e.g. a CommonMessageHeader.xsd)
 * to the set of XSD's used for validation.
 *
 * @author Michiel Meeuwissen
 * @author Jaco de Groot
 */
@Category("NN-Special")
public class EsbSoapValidator extends SoapValidator {

	private @Getter Direction direction = null;
	private @Getter EsbSoapWrapperPipe.Mode mode = EsbSoapWrapperPipe.Mode.REG;
	private @Getter int cmhVersion = 0;
	private static final Map<String, HeaderInformation> GENERIC_HEADER;


	static {
		Map<String, HeaderInformation> temp = new HashMap<>();
		temp.put(getModeKey(EsbSoapWrapperPipe.Mode.REG,1), new HeaderInformation("http://nn.nl/XSD/Generic/MessageHeader/1", "/xml/xsd/CommonMessageHeader.xsd"));
		temp.put(getModeKey(EsbSoapWrapperPipe.Mode.REG,2), new HeaderInformation("http://nn.nl/XSD/Generic/MessageHeader/2", "/xml/xsd/CommonMessageHeader_2.xsd"));
		temp.put(getModeKey(EsbSoapWrapperPipe.Mode.I2T), new HeaderInformation("http://nn.nl/XSD/Generic/MessageHeader/1", "/xml/xsd/CommonMessageHeader.xsd"));
		temp.put(getModeKey(EsbSoapWrapperPipe.Mode.BIS), new HeaderInformation("http://www.ing.com/CSP/XSD/General/Message_2", "/xml/xsd/cspXML_2.xsd"));
		GENERIC_HEADER = new HashMap<>(temp);
	}

	public enum Direction {
		INPUT, OUTPUT
	}

	private static class HeaderInformation {
		final String xmlns;
		final String xsd;
		final QName tag;

		HeaderInformation(String xmlns, String xsd) {
			this.xmlns = xmlns;
			this.xsd = xsd;
			this.tag = new QName(this.xmlns, "MessageHeader");
		}
	}

	@Override
	public void configure() throws ConfigurationException {
		if (getMode() == EsbSoapWrapperPipe.Mode.REG) {
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
		if (StringUtils.isEmpty(getSoapBody())) {
			ConfigurationWarnings.add(this, log, "soapBody not specified");
		}
		super.setSchemaLocation(getSchemaLocation() + " " + GENERIC_HEADER.get(getModeKey()).xmlns + " " + GENERIC_HEADER.get(getModeKey()).xsd);
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

	public void setDirection(Direction direction) {
		this.direction = direction;
	}

	public void setMode(EsbSoapWrapperPipe.Mode mode) {
		this.mode = mode;
	}

	/**
	 * Only used when <code>mode=reg</code>!</b> Sets the Common Message Header version. 1 or 2
	 * @ff.default 1
	 */
	public void setCmhVersion(int i) {
		cmhVersion = i;
	}

}