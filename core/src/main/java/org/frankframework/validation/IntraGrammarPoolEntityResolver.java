/*
   Copyright 2022-2023 WeAreFrank!

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
package org.frankframework.validation;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLEntityResolver;
import org.apache.xerces.xni.parser.XMLInputSource;

import org.frankframework.core.IScopeProvider;
import org.frankframework.core.Resource;
import org.frankframework.util.LogUtil;

/**
 * EntityResolver for XercesXmlValidator to resolve imported schema documents to other schemas used to populate the grammar pool.
 *
 * References are resolved by namespace.
 *
 * @author Gerrit van Brakel
 */
public class IntraGrammarPoolEntityResolver implements XMLEntityResolver { //ClassLoaderXmlEntityResolver
	protected Logger log = LogUtil.getLogger(this);

	private final List<Schema> schemas;
	private final IScopeProvider scopeProvider;

	public IntraGrammarPoolEntityResolver(IScopeProvider scopeProvider, List<Schema> schemas) {
		this.schemas = schemas;
		this.scopeProvider = scopeProvider;
	}

	@Override
	public XMLInputSource resolveEntity(XMLResourceIdentifier resourceIdentifier) throws XNIException, IOException {
		if (log.isDebugEnabled())
			log.debug("resolveEntity publicId [{}] baseSystemId [{}] expandedSystemId [{}] literalSystemId [{}] namespace [{}]", resourceIdentifier.getPublicId(), resourceIdentifier.getBaseSystemId(), resourceIdentifier.getExpandedSystemId(), resourceIdentifier.getLiteralSystemId(), resourceIdentifier.getNamespace());
		if (resourceIdentifier.getExpandedSystemId() == null
				&& resourceIdentifier.getLiteralSystemId() == null
				&& resourceIdentifier.getNamespace() == null
				&& resourceIdentifier.getPublicId() == null) {
			// The baseSystemId may be resolved to a namespace with all other values being NULL. This behavior changed between the 7.7 and 7.8 branch.
			// This seems to happen sometimes. For example with import of
			// sub01a.xsd and sub05.xsd without namespace in
			// /XmlValidator/import_include/root.xsd of Ibis4TestIAF. The
			// default resolve entity implementation seems to ignore it, hence
			// return null.
			log.trace("all relevant Ids are null, returning null. baseSystemId [{}]", resourceIdentifier::getBaseSystemId);
			return null;
		}

		String targetNamespace = resourceIdentifier.getNamespace();
		if (targetNamespace != null) {
			for(Schema schema:schemas) {
				if (log.isTraceEnabled()) log.trace("matching namespace [{}] to schema [{}]", targetNamespace, schema.getSystemId());
				if (targetNamespace.equals(schema.getSystemId())) {
					return new XMLInputSource(null, targetNamespace, null, schema.getReader(), null);
				}
			}
			log.warn("namespace [{}] not found in list of schemas", targetNamespace);
		}

		if(resourceIdentifier.getExpandedSystemId() != null) {
			Resource resource = Resource.getResource(scopeProvider, resourceIdentifier.getExpandedSystemId());
			if(resource != null) {
				return resource.asXMLInputSource();
			}
		}

		// Throw an exception so the XercesValidationErrorHandler picks this up as ERROR.
		// Do not rely on the fallback resource resolver, this will bypass configuration classloaders.
		// See https://github.com/frankframework/frankframework/issues/3973
		throw new XNIException("Cannot find resource [" + resourceIdentifier.getExpandedSystemId() +
			"] from systemId [" + resourceIdentifier.getLiteralSystemId() +
			"] with base [" + resourceIdentifier.getBaseSystemId() + "]");
	}

}
