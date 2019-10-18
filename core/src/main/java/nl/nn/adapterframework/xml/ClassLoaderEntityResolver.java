/*
   Copyright 2013, 2016 Nationale-Nederlanden

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
import java.net.URL;

import org.apache.log4j.Logger;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

/**
 * @see org.xml.sax.EntityResolver
 * This class offers the resolveEntity method to resolve a systemId to a resource on the classpath.
 * @since 4.1.1
 */

public class ClassLoaderEntityResolver implements EntityResolver {
	protected Logger log = LogUtil.getLogger(this);
	private ClassLoader classLoader;

	public ClassLoaderEntityResolver(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}
	/**
	 * @see org.xml.sax.EntityResolver#resolveEntity(String, String)
	 */
	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
		InputSource inputSource = null;

		String ref1 = systemId;
		String ref2=null;

		// strip any file info from systemId 
		int idx = systemId.lastIndexOf("/");
		if (idx >= 0) {
			ref2 = systemId.substring(idx + 1); // this appears to be necessary to load configurations
		}
		log.debug("Resolving [" + ref1 +"]");
		try {
			URL url = ClassUtils.getResourceURL(classLoader, ref1);
			if (url==null && ref2!=null) {
				log.warn("could not get entity via ["+ref1+"], now trying  via ["+ref2+"]");
				url = ClassUtils.getResourceURL(classLoader, ref2);
			}
			if (url==null) {
				log.error("cannot find resource for entity [" + ref1 + "]"+(ref2==null?"":" or [" + ref2 + "]"));
				return null;
			}
			inputSource = new InputSource(url.openStream());
		} catch (Exception e) {
			log.error("Exception resolving entity [" + ref1 + "]"+(ref2==null?"":" or [" + ref2 + "]"),e);
			// No action; just let the null InputSource pass through
		}

		// If nothing found, null is returned, for normal processing
		return inputSource;
	}

}
