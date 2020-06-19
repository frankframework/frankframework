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
package nl.nn.adapterframework.soap;

import java.util.HashSet;
import java.util.Set;

import javax.xml.soap.SOAPConstants;

import org.apache.commons.lang.StringUtils;


public enum SoapVersion {
	
	SOAP11("1.1", SOAPConstants.URI_NS_SOAP_ENVELOPE,     "/xml/xsd/soap/envelope.xsd"),
	SOAP12("1.2", SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, "/xml/xsd/soap/envelope-1.2.xsd"),
	NONE("none", null, null),
	AUTO("auto", null, null);

	public final String description;;
	public final String namespace;
	public final String location;
	
	private SoapVersion(String description, String namespace, String location) {
		this.description = description;
		this.namespace = namespace;
		this.location = location;
	}
	
	public static SoapVersion getSoapVersion(String s) {
		if (StringUtils.isEmpty(s)) {
			return SOAP11;
		}
		if (s.equals("any")) {
			return AUTO;
		}
		if (s.startsWith("1")) {
			return valueOf("SOAP" + s.replaceAll("\\.", ""));
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
			return SOAP11.toString()+" "+SOAP12.toString();
		default:
			return "";
		}
	}

	@Override
	public String toString() {
		return namespace + " " + location;
	}

}
