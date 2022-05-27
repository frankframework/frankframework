/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.validation;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLEntityResolver;
import org.apache.xerces.xni.parser.XMLInputSource;

import nl.nn.adapterframework.util.LogUtil;

/**
 * Xerces native EntityResolver, currently only used in XercesXmlValidator when initializing the grammar pool.
 * 
 * Resolves referenced schema documents by namespace to other schemas used to populate the grammar pool.
 * 
 * @author Gerrit van Brakel
 */
public class IntraGrammarPoolEntityResolver implements XMLEntityResolver {
	protected Logger log = LogUtil.getLogger(this);

	private List<Schema> schemas;

	public IntraGrammarPoolEntityResolver(List<Schema> schemas) {
		this.schemas = schemas;
	}

	@Override
	public XMLInputSource resolveEntity(XMLResourceIdentifier resourceIdentifier) throws XNIException, IOException {
		if (log.isDebugEnabled()) log.debug("resolveEntity publicId ["+resourceIdentifier.getPublicId()+"] baseSystemId ["+resourceIdentifier.getBaseSystemId()+"] expandedSystemId ["+resourceIdentifier.getExpandedSystemId()+"] literalSystemId ["+resourceIdentifier.getLiteralSystemId()+"] namespace ["+resourceIdentifier.getNamespace()+"]");
		if (resourceIdentifier.getBaseSystemId() == null
				&& resourceIdentifier.getExpandedSystemId() == null
				&& resourceIdentifier.getLiteralSystemId() == null
				&& resourceIdentifier.getNamespace() == null
				&& resourceIdentifier.getPublicId() == null) {
			// This seems to happen sometimes. For example with import of
			// sub01a.xsd and sub05.xsd without namespace in
			// /XmlValidator/import_include/root.xsd of Ibis4TestIAF. The
			// default resolve entity implementation seems to ignore it, hence
			// return null.
			if (log.isTraceEnabled()) log.trace("all relevant Ids are null, returning null");
			return null;
		}

		String targetNamespace = resourceIdentifier.getNamespace();
		if (targetNamespace==null) {
			log.warn("resolveEntity publicId ["+resourceIdentifier.getPublicId()+"] baseSystemId ["+resourceIdentifier.getBaseSystemId()+"] expandedSystemId ["+resourceIdentifier.getExpandedSystemId()+"] literalSystemId ["+resourceIdentifier.getLiteralSystemId()+"] namespace ["+resourceIdentifier.getNamespace()+"] has no namespace to resolve to");
			return null;
		}
		for(Schema schema:schemas) {
			if (log.isTraceEnabled()) log.trace("matching namespace ["+targetNamespace+"] to schema ["+schema.getSystemId()+"]");
			if (targetNamespace.equals(schema.getSystemId())) {
				return new XMLInputSource(null, targetNamespace, null, schema.getInputStream(), null);
			}
		}
		log.warn("namespace ["+targetNamespace+"] not found in list of schemas");

		return null;
	}

}
