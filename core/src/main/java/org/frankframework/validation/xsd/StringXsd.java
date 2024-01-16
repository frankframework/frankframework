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
import java.io.StringReader;
import java.io.StringWriter;

import lombok.Setter;
import org.frankframework.validation.XSD;

/**
 * XSD implementation of an internally created schema, used as result of SchemaUtils.mergeXsdsGroupedByNamespaceToSchemasWithoutIncludes().
 *
 * @author Gerrit van Brakel
 */
public class StringXsd extends XSD {

	private @Setter StringWriter schemaContentsWriter;

	@Override
	public Reader getReader() throws IOException {
		return new StringReader(schemaContentsWriter.toString());
	}

}
