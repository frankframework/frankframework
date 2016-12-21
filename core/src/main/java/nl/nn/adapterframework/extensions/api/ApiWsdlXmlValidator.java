/*
   Copyright 2013, 2015, 2016 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.api;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.pipes.WsdlXmlValidator;

/**
 * Extension to WsdlXmlValidator for API Management.
 * 
 * @author Peter Leeuwenburgh
 */

public class ApiWsdlXmlValidator extends WsdlXmlValidator {
	protected static final String API_NAMESPACE = "http://api.nn.nl/MessageHeader";

	public void configure() throws ConfigurationException {
		setSoapHeader("MessageHeader,");
		setSoapHeaderNamespace(API_NAMESPACE);
		setSchemaLocationToAdd(API_NAMESPACE
				+ " /xml/xsd/api/MessageHeader.xsd");
		setAddNamespaceToSchema(true);
		super.configure();
	}
}
