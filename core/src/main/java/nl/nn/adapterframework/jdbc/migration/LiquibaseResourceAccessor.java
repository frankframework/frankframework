/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.jdbc.migration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import liquibase.resource.ResourceAccessor;
import nl.nn.adapterframework.configuration.classloaders.ClassLoaderBase;

/**
 * A special ResourceAccessor which only looks for resources in the Local ClassPath and does not traverse to it's parents ClassPath.
 * 
 * The DatabaseChangelog.xml file should only ever be found in the current ClassLoader and never in it's parent.
 * This in order to prevent the file from being executed multiple times by different configurations.
 * The file might be present on the ClassPath or in the WAR/EAR. If it would traverse more then 1 loader, 
 * all configurations (!) would read this file and execute it.
 * 
 * @author Niels Meijer
 * 
 */
public class LiquibaseResourceAccessor implements ResourceAccessor {

	private ClassLoaderBase classLoader;

	public LiquibaseResourceAccessor(ClassLoader classLoader) {
		if(classLoader instanceof ClassLoaderBase) {
			this.classLoader = (ClassLoaderBase) classLoader;
		} else {
			throw new IllegalArgumentException("ClassLoader must be of type ClassLoaderBase");
		}
	}

	@Override
	public Set<InputStream> getResourcesAsStream(String path) throws IOException {
		Set<InputStream> returnSet = new HashSet<>();

		URL url = classLoader.getResource(path, false);
		if(url != null) {
			returnSet.add(url.openStream());
		}

		return returnSet;
	}

	@Override
	public Set<String> list(String relativeTo, String path, boolean includeFiles, boolean includeDirectories, boolean recursive) throws IOException {
		return null; //Always fetch the resource directly through the local ClassLoader
	}

	@Override
	public ClassLoader toClassLoader() {
		return classLoader;
	}
}
