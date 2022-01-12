/*
Copyright 2017, 2019 Integration Partners B.V.

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
import java.net.URL;
import java.util.Enumeration;
import java.util.Vector;

import nl.nn.adapterframework.configuration.classloaders.ClassLoaderBase;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

/**
 * Extra layer between ClassLoaders to retrieve the database migrator file.
 * Don't ever go through it's parent in order to prevent duplicate files being found.
 * 
 * @author Niels Meijer
 *
 */
public class LiquibaseClassLoaderWrapper extends ClassLoader {

	private Logger log = LogUtil.getLogger(this);

	public LiquibaseClassLoaderWrapper(ClassLoader parent) {
		super(parent);
	}

	/**
	 * Make sure what the parent ClassLoader only traverses 1 layer (loader) deep.
	 * The DatabaseChangelog.xml file should only ever be found in the current ClassLoader and never in it's parent.
	 * This in order to prevent the file from being executed multiple times by different configurations.
	 * The file might be present on the ClassPath or in the WAR/EAR. If it would traverse more then 1 loader, 
	 * all configurations (!) would read this file and execute it.
	 */
	@Override
	public URL getResource(String name) {
		if(getParent() instanceof ClassLoaderBase) {
			ClassLoaderBase classLoader = (ClassLoaderBase) getParent();
			return classLoader.getResource(name, false);
		}

		// Return null by default if not an instance of ClassLoaderBase
		log.info("unable to determine ClassLoader hierarchy using classloader ["+getParent()+"], returning null");
		return null;
	}

	/**
	 * Make sure what the parent ClassLoader only traverses 1 layer deep!
	 * Name may be EMPTY as liquibase searches for packages/folders to find files in..
	 */
	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		Vector<URL> urls = new Vector<URL>();

		if(StringUtils.isNotEmpty(name) && getResource(name) != null)
			urls.add(getResource(name));

		return urls.elements();
	}
}
