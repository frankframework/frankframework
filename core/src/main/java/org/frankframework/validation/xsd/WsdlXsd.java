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
package org.frankframework.validation.xsd;

import java.io.IOException;
import java.io.Reader;

import javax.wsdl.WSDLException;

import org.frankframework.validation.IXSD;
import org.frankframework.validation.SchemaUtils;

/**
 * Extension of ResourceXsd, where the schema is retrieved from a WSDL.
 *
 * @author Gerrit van Brakel
 */
public class WsdlXsd extends ResourceXsd {

	private javax.wsdl.Definition wsdlDefinition;
	private javax.wsdl.extensions.schema.Schema wsdlSchema;

	public void setWsdlSchema(javax.wsdl.Definition wsdlDefinition, javax.wsdl.extensions.schema.Schema wsdlSchema) {
		this.wsdlDefinition = wsdlDefinition;
		this.wsdlSchema = wsdlSchema;
	}

	@Override
	public Reader getReader() throws IOException {
		try {
			return SchemaUtils.toReader(wsdlDefinition, wsdlSchema);
		} catch (WSDLException e) {
			throw new IOException(e);
		}
	}

	@Override
	public int compareToByReferenceOrContents(IXSD other) {
		// Compare XSD content to prevent copies of the same XSD showing up
		// more than once in the WSDL. For example the
		// CommonMessageHeader.xsd used by the EsbSoapValidator will
		// normally also be imported by the XSD for the business response
		// message (for the Result part).
		return compareToByContents(other);
	}

}
