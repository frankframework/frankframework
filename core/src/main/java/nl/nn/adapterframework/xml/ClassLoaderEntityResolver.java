/*
   Copyright 2013, 2016, 2019 Nationale-Nederlanden

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
//		String fileName=null; //relative path in a ClassLoader

		// strip any file info from systemId
//		int idx = systemId.lastIndexOf("/");
//		if (idx >= 0) {
//			fileName = systemId.substring(idx + 1); // this appears to be necessary to load configurations
//		}

		if (log.isDebugEnabled()) log.debug("Resolving [" + systemId +"]");
//		try {
			URL url = ClassUtils.getResourceURL(classLoader, systemId);
			if(url != null) {
				return new InputSource(url.openStream());
			}
//		} catch (IOException e) {
//			e.printStackTrace();
//			//URL is not null but the resource cannot be found. Ignore this exception and try to resolve the relative location
//		}
//		if (fileName != null) {
//			try {
//				log.warn("could not get entity via ["+absolutePath+"], now trying via ["+fileName+"]");
//				URL url2 = ClassUtils.getResourceURL(classLoader, fileName);
//				if(url2 != null)
//					return new InputSource(url2.openStream());
//			} catch (IOException e) {
//				//This should never be thrown, as relative paths are either found or not found (not NULL)
//				log.error("Exception resolving file ["+fileName+"] on classloader ["+classLoader+"]",e);
//			}
//		}

		// If nothing found, null is returned, for normal processing
		return null;
	}

}
