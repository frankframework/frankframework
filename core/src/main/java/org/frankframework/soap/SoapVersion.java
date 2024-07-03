/*
   Copyright 2020 WeAreFrank!

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

import java.util.HashSet;
import java.util.Set;

import jakarta.xml.soap.SOAPConstants;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.doc.DocumentedEnum;
import org.frankframework.doc.EnumLabel;


public enum SoapVersion implements DocumentedEnum {

	@EnumLabel("1.1") SOAP11(SOAPConstants.URI_NS_SOAP_ENVELOPE,     "/xml/xsd/soap/envelope.xsd"),
	@EnumLabel("1.2") SOAP12(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "/xml/xsd/soap/envelope-1.2.xsd"),
	/** No wrapping or unwrapping will be done */
	@EnumLabel("none") NONE(null, null),
	/** Try to auto-detect the value */
	@EnumLabel("auto") AUTO(null, null);

	public final String namespace;
	public final String location;

	SoapVersion(String namespace, String location) {
		this.namespace = namespace;
		this.location = location;
	}

	public static SoapVersion getSoapVersion(String s) {
		if (StringUtils.isEmpty(s)) {
			return SOAP11;
		}
		if ("any".equals(s)) {
			return AUTO;
		}
		if (s.startsWith("1")) {
			return valueOf("SOAP" + s.replace(".", ""));
		}
		return valueOf(s.toUpperCase());
	}

	public Set<String> getNamespaces() {
		Set<String> result = new HashSet<>();
		switch (this) {
		case SOAP11:
		case SOAP12:
			result.add(namespace);
			break;
		case AUTO:
			result.add(SOAP11.namespace);
			result.add(SOAP12.namespace);
			break;
		default:
		}
		return result;
	}

	public String getSchemaLocation() {
		switch (this) {
		case SOAP11:
		case SOAP12:
			return toString();
		case AUTO:
			return SOAP11 + " " + SOAP12;
		default:
			return "";
		}
	}

	@Override
	public String toString() {
		return namespace + " " + location;
	}
}
