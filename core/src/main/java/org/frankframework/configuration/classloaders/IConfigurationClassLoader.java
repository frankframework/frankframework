/*
   Copyright 2019 Nationale-Nederlanden

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

import org.frankframework.configuration.ClassLoaderException;
import org.frankframework.configuration.IbisContext;

/**
 * Interface for IBIS Configuration ClassLoaders.
 *
 * @author Niels Meijer
 */
public interface IConfigurationClassLoader {

	String CLASSPATH_RESOURCE_SCHEME="classpath:";

	public enum ReportLevel {
		DEBUG, INFO, WARN, ERROR;
	}

	public URL getResource(String name);

	/**
	 * Configure the {@link IConfigurationClassLoader}'s implementation
	 * @throws ClassLoaderException when the {@link IConfigurationClassLoader}'s implementation cannot retrieve or load the configuration files
	 */
	public void configure(IbisContext ibisContext, String configurationName) throws ClassLoaderException;

	/**
	 * Retrieve the name of the configuration that uses this {@link IConfigurationClassLoader}
	 */
	public String getConfigurationName();

	/**
	 * Defines the log level for errors caused by the {@link IConfigurationClassLoader#configure(IbisContext, String) configure} method
	 * @param level ReportLevel in string format to be parsed by the ClassLoaderManager digester
	 */
	public void setReportLevel(String level);

	/**
	 * @return the {@link ReportLevel} set for this {@link IConfigurationClassLoader}
	 */
	public ReportLevel getReportLevel();

	/**
	 * Unload the current IConfigurationClassLoader and load again upon successful unload.
	 */
	public void reload() throws ClassLoaderException;

	/**
	 * Called upon destroying (and thus removing) the IConfigurationClassLoader
	 */
	public void destroy();
}
