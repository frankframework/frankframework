/*
   Copyright 2013, 2016, 2019 Nationale-Nederlanden, 2021 WeAreFrank!

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

import org.apache.logging.log4j.Logger;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.core.IHasConfigurationClassLoader;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.util.LogUtil;

/**
 * @see org.xml.sax.EntityResolver
 * This class offers the resolveEntity method to resolve a systemId to a resource on the classpath.
 * @since 4.1.1
 */

public class ClassLoaderEntityResolver implements EntityResolver {
	protected Logger log = LogUtil.getLogger(this);
	private IHasConfigurationClassLoader classLoaderProvider;

	public ClassLoaderEntityResolver(IHasConfigurationClassLoader classLoaderProvider) {
		this.classLoaderProvider = classLoaderProvider;
	}

	public ClassLoaderEntityResolver(Resource resource) {
		this(resource.getClassLoaderProvider());
	}

	/**
	 * @see org.xml.sax.EntityResolver#resolveEntity(String, String)
	 */
	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {

		if (log.isDebugEnabled()) log.debug("Resolving publicId [" + publicId +"] systemId [" + systemId +"] classLoaderProvider ["+classLoaderProvider+"]");
		Resource resource = Resource.getResource(classLoaderProvider, systemId);
		if(resource != null) {
			return resource.asInputSource();
		}
		// If nothing found, null is returned, for normal processing
		return null;
	}

}
