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
package org.frankframework.configuration.classloaders;

import java.net.URL;

/**
 * Default IBIS Configuration ClassLoader.
 *
 * @author Niels Meijer
 */
public class WebAppClassLoader extends AbstractClassLoader {

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
		int bangslash = name.indexOf("!/");
		if (name.startsWith("jar:") && bangslash > -1) { // when referencing a resource in a jar
			String localname = name.substring(bangslash+2); // remove the jar protocol + path prefix

			// if no basepath is present, or the localname already starts with the basepath
			if (getBasePath() == null || localname.startsWith(getBasePath())) {
				return getParent().getResource(localname);
			} else { // else consider the file illegal, it should not be found within this classloader.
				log.debug("unable to retrieve file [{}] with localname [{}] from [{}]", ()->name, ()->localname, ()->toString());
				return null;
			}
		}

		return getParent().getResource(getBasePath()==null?name:getBasePath()+name);
	}

	/**
	 * Always allow all classes from the deployed webapp.
	 */
	@Override
	protected boolean getAllowCustomClasses() {
		return true;
	}
}
