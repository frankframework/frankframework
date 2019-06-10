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
package nl.nn.adapterframework.util;
import java.io.IOException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author Frits Berger RSD SF OJM
 * @see EntityResolver
 * This class offers the resolveEntity method to resolve a systemId to
 * a resource on the classpath.
 * @since 4.1.1
 */

public class ClassPathEntityResolver implements EntityResolver {
	protected Logger log = LogUtil.getLogger(this);
	private ClassLoader classLoader;

	ClassPathEntityResolver(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}
	/**
	 * @see EntityResolver#resolveEntity(String, String)
	 */
	public InputSource resolveEntity(String publicId, String systemId)
		throws SAXException, IOException {
		InputSource inputSource = null;

		String classPathEntityUrl = systemId;
	
		// strip any file info from systemId 
		int idx = classPathEntityUrl.lastIndexOf("/");
		if (idx >= 0) {
			classPathEntityUrl = classPathEntityUrl.substring(idx + 1);
		}
		try {
			log.debug("Resolving [" + systemId + "] to [" + classPathEntityUrl+"]");
			URL url = ClassUtils.getResourceURL(classLoader, classPathEntityUrl);
			if (url==null) {
				log.error("cannot find resource for ClassPathEntity [" + systemId + "] from Url [" + classPathEntityUrl+"]");
				return null;
			}
			inputSource = new InputSource(url.openStream());
		} catch (Exception e) {
			log.error("Exception resolving ClassPathEntity [" + systemId + "] to [" + classPathEntityUrl+"]",e);
			// No action; just let the null InputSource pass through
		}

		// If nothing found, null is returned, for normal processing
		return inputSource;
	}

}
