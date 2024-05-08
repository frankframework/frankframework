/*
   Copyright 2023 WeAreFrank!

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
package org.frankframework.soap;

import java.util.List;
import java.util.Map;

import jakarta.xml.soap.SOAPConstants;

import org.frankframework.validation.RootValidation;

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
