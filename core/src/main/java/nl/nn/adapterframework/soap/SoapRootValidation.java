package nl.nn.adapterframework.soap;

import java.util.List;
import java.util.Map;

import javax.xml.soap.SOAPConstants;

import nl.nn.adapterframework.validation.RootValidation;

public class SoapRootValidation extends RootValidation {

	public SoapRootValidation(String... rootElement) {
		super(rootElement);
	}

	@Override
	public boolean isNamespaceAllowedOnElement(Map<List<String>, List<String>> invalidRootNamespaces, String namespaceURI, String localName) {
		if("Fault".equals(localName) && SOAPConstants.URI_NS_SOAP_ENVELOPE.equals(namespaceURI)) {
			return true;
		}

		return super.isNamespaceAllowedOnElement(invalidRootNamespaces, namespaceURI, localName);
	}
}
