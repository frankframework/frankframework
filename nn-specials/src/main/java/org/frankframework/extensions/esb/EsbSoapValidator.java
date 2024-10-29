/*
   Copyright 2013 Nationale-Nederlanden, 2023 WeAreFrank!

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
package org.frankframework.extensions.esb;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.IListener;
import org.frankframework.core.IPipe;
import org.frankframework.core.IXmlValidator;
import org.frankframework.core.PipeLine;
import org.frankframework.doc.Category;
import org.frankframework.http.WebServiceListener;
import org.frankframework.jms.JmsListener;
import org.frankframework.soap.SoapValidator;
import org.frankframework.soap.WsdlGeneratorExtension;
import org.frankframework.soap.WsdlGeneratorUtils;

/**
 * XmlValidator that will automatically add the SOAP envelope XSD and the ESB XSD (e.g. a CommonMessageHeader.xsd)
 * to the set of XSDs used for validation.
 *
 * @author Michiel Meeuwissen
 * @author Jaco de Groot
 */
@Category(Category.Type.NN_SPECIAL)
public class EsbSoapValidator extends SoapValidator implements WsdlGeneratorExtension<EsbSoapWsdlGeneratorContext> {

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

	@Override
	public EsbSoapWsdlGeneratorContext buildExtensionContext(PipeLine pipeLine) {
		EsbSoapWsdlGeneratorContext context = new EsbSoapWsdlGeneratorContext();

		boolean esbNamespaceWithoutServiceContext = false;
		String schemaLocation = WsdlGeneratorUtils.getFirstNamespaceFromSchemaLocation(this);
		if (EsbSoapWrapperPipe.isValidNamespace(schemaLocation)) {
			String s = schemaLocation;
			esbNamespaceWithoutServiceContext = EsbSoapWrapperPipe.isEsbNamespaceWithoutServiceContext(s);
			int i = s.lastIndexOf('/');
			context.esbSoapOperationVersion = s.substring(i + 1);
			s = s.substring(0, i);
			i = s.lastIndexOf('/');
			context.esbSoapOperationName = s.substring(i + 1);
			s = s.substring(0, i);
			i = s.lastIndexOf('/');
			context.esbSoapServiceContextVersion = s.substring(i + 1);
			s = s.substring(0, i);
			i = s.lastIndexOf('/');
			if (!esbNamespaceWithoutServiceContext) {
				context.esbSoapServiceContext = s.substring(i + 1);
				s = s.substring(0, i);
				i = s.lastIndexOf('/');
			}
			context.esbSoapServiceName = s.substring(i + 1);
			s = s.substring(0, i);
			i = s.lastIndexOf('/');
			context.esbSoapBusinessDomain = s.substring(i + 1);
		} else {
			context.warn("Namespace '" + schemaLocation + "' invalid according to ESB SOAP standard");
			IPipe outputWrapper = pipeLine.getOutputWrapper();
			if (outputWrapper instanceof EsbSoapWrapperPipe esbSoapWrapper) {
				context.esbSoapBusinessDomain = esbSoapWrapper.getBusinessDomain();
				context.esbSoapServiceName = esbSoapWrapper.getServiceName();
				context.esbSoapServiceContext = esbSoapWrapper.getServiceContext();
				context.esbSoapServiceContextVersion = esbSoapWrapper.getServiceContextVersion();
				context.esbSoapOperationName = esbSoapWrapper.getOperationName();
				context.esbSoapOperationVersion = esbSoapWrapper.getOperationVersion();
			}
		}
		if (context.esbSoapBusinessDomain == null) {
			context.warn("Could not determine business domain");
		} else if (context.esbSoapServiceName == null) {
			context.warn("Could not determine service name");
		} else if (context.esbSoapServiceContext == null && !esbNamespaceWithoutServiceContext) {
			context.warn("Could not determine service context");
		} else if (context.esbSoapServiceContextVersion == null) {
			context.warn("Could not determine service context version");
		} else if (context.esbSoapOperationName == null) {
			context.warn("Could not determine operation name");
		} else if (context.esbSoapOperationVersion == null) {
			context.warn("Could not determine operation version");
		} else {
			context.wsdlType = "abstract";
			for (IListener<?> listener : WsdlGeneratorUtils.getListeners(pipeLine.getAdapter())) {
				if (listener instanceof WebServiceListener
						|| listener instanceof JmsListener) {
					context.wsdlType = "concrete";
					break;
				}
			}


			String inputParadigm = WsdlGeneratorUtils.getEsbSoapParadigm(this);
			if (inputParadigm != null) {
				if (!"Action".equals(inputParadigm)
						&& !"Event".equals(inputParadigm)
						&& !"Request".equals(inputParadigm)
						&& !"Solicit".equals(inputParadigm)) {
					context.warn("Paradigm for input message which was extracted from soapBody should be on of Action, Event, Request or Solicit instead of '"
							+ inputParadigm + "'");
				}
			} else {
				context.warn("Could not extract paradigm from soapBody attribute of inputValidator (should end with _Action, _Event, _Request or _Solicit)");
			}
//					if (outputValidator != null || isMixedValidator) {
			IXmlValidator outputValidator = (IXmlValidator) pipeLine.getOutputValidator();
			if (outputValidator != null) {
				String outputParadigm = WsdlGeneratorUtils.getEsbSoapParadigm(outputValidator);
				if (outputParadigm != null) {
					if (!"Response".equals(outputParadigm)) {
						context.warn("Paradigm for output message which was extracted from soapBody should be Response instead of '"
								+ outputParadigm + "'");
					}
				} else {
					context.warn("Could not extract paradigm from soapBody attribute of outputValidator (should end with _Response)");
				}
			}
		}
		return context;
	}
}
