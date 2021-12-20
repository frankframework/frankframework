/*
   Copyright 2017-2019 Nationale-Nederlanden, 2021 WeAreFrank!

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
package nl.nn.adapterframework.xml;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.TransformerException;

import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLEntityResolver;
import org.apache.xerces.xni.parser.XMLInputSource;

import nl.nn.adapterframework.core.IScopeProvider;
import nl.nn.adapterframework.core.Resource;

/**
 * Xerces native EntityResolver. Appears to be only used in XercesXmlValidator currently.
 * 
 * It's important that the XMLEntityResolver does not return NULL, when it cannot find a resource.
 * Returning NULL will cause the XmlReader to fall back to it's built in EntityResolver.
 * 
 * This EntityResolver can be set by using the following property on the XmlReader:
 * Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_RESOLVER_PROPERTY
 * 
 * @author Jaco de Groot
 * @author Gerrit van Brakel
 * @see ClassLoaderURIResolver
 */
public class ClassLoaderXmlEntityResolver extends ClassLoaderURIResolver implements XMLEntityResolver {

	public ClassLoaderXmlEntityResolver(IScopeProvider classLoaderProvider) {
		super(classLoaderProvider);
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
			return null;
		}

		String base = resourceIdentifier.getBaseSystemId();
		String href = resourceIdentifier.getLiteralSystemId();
		if (href == null) {
			// Ignore import with namespace but without schemaLocation
			return null;
		}

		Resource resource;
		try {
			resource = resolveToResource(href, base);
		} catch (TransformerException e) {
			throw new XNIException(e);
		}

		return resource.asXMLInputSource();
	}

}
