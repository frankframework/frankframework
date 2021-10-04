/*
   Copyright 2018-2020 Nationale-Nederlanden, 2021 WeAreFrank!

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
package nl.nn.adapterframework.configuration.classloaders;

import java.net.URL;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.util.FilenameUtils;


/**
 * Default IBIS Configuration ClassLoader.
 * 
 * @author Niels Meijer
 */

public class WebAppClassLoader extends ClassLoaderBase {

	public WebAppClassLoader(ClassLoader parent) {
		super(parent);
	}

	/**
	 * The name that's being requested should only contain the BasePath of the configuration when set.
	 * The {@link WebAppClassLoader} does not contain any further logic and must always 
	 * search for the resource with BasePath in it's parent (the ClassPath or another ClassLoader).
	 */
	@Override
	public URL getLocalResource(String name) {
		if (name.startsWith("jar:")) { // when referencing a resource in a jar
			String normalizedName = FilenameUtils.normalize(name);
			URL result = getParent().getResource(normalizedName); 
			if (result!=null) {
				log.debug("returning url for ["+normalizedName+"]");
				return result;
			}
			log.debug("cannot open url for ["+normalizedName+"]");
			int prefixEndMarkerPos = name.indexOf("!");
			if (prefixEndMarkerPos>0) {
				name = name.substring(prefixEndMarkerPos+2); // if not found in the jar, then remove the jar prefix
				log.debug("--> reduced name to ["+name+"]");
				result = getParent().getResource(name);
				if (result != null) {
					log.debug("returning url for ["+name+"]");
					return result;
				}
				if (StringUtils.isNotEmpty(getBasePath()) && name.startsWith(getBasePath())) {
					name = name.substring(getBasePath().length());
					log.debug("returning url for basepath-less name ["+name+"]");
					return result;
				}
			}
		}
		return getParent().getResource((getBasePath()==null)?name:getBasePath()+name);
	}
}