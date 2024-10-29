/*
   Copyright 2013, 2015, 2016, 2020 Nationale-Nederlanden

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
package org.frankframework.extensions.api;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.pipes.WsdlXmlValidator;

/**
 * Extension to WsdlXmlValidator for API Management.
 *
 * The SOAP header can only contain the following schema (or it's empty):
 * <table border="1">
 * <tr><th>element</th><th>level</th><th>mandatory</th></tr>
 * <tr><td>MessageHeader</td><td>0</td><td>yes</td></tr>
 * <tr><td>xmlns="http://api.nn.nl/MessageHeader"</td><td>&nbsp;</td><td>yes</td></tr>
 * <tr><td>From</td><td>1</td><td>no</td></tr>
 * <tr><td>HeaderFields</td><td>1</td><td>yes</td></tr>
 * <tr><td>ConversationId</td><td>2</td><td>yes</td></tr>
 * </table>
 *
 * @author Peter Leeuwenburgh
 */

public class ApiWsdlXmlValidator extends WsdlXmlValidator {
	protected static final String API_NAMESPACE = "http://api.nn.nl/MessageHeader";

	private @Getter boolean multipart = false;

	@Override
	public void configure() throws ConfigurationException {
		setSoapHeader("MessageHeader,");
		setSoapHeaderNamespace(API_NAMESPACE);
		setSchemaLocationToAdd(API_NAMESPACE + " /xml/xsd/api/MessageHeader.xsd");
		setAddNamespaceToSchema(true);
		super.configure();
	}

	/**
	 * indicates whether the message is multipart/form-data. If so, the wsdl only represents the first part, other parts are attachments. This attribute is only used for generating the 'real' wsdl which is available in the ibis console (../rest/webservices)
	 * @ff.default false
	 */
	public void setMultipart(boolean b) {
		multipart = b;
	}

	@Override
	public String getDocumentation() {
		if (multipart) {
			return """
					<br/>\
					<b>Note: </b>this service is not a SOAP service but a REST service.\
					 A 'multipart/form-data' request is expected of which the first part is a SOAP message and each next part is a file(stream).\
					 This wsdl describes the SOAP message in the first part.\
					""";
		}
		return null;
	}
}
