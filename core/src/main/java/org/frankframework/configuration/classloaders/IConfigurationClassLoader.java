/*
   Copyright 2019 Nationale-Nederlanden, 2020-2026 WeAreFrank!

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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.frankframework.configuration.ClassLoaderException;
import org.frankframework.configuration.IbisContext;

/**
 * Interface for IBIS Configuration ClassLoaders.
 *
 * @author Niels Meijer
 */
public interface IConfigurationClassLoader {

	String CLASSPATH_RESOURCE_SCHEME="classpath:";

	enum ReportLevel {
		DEBUG, INFO, WARN, ERROR;
	}

	@Nullable URL getResource(@Nullable String name);

	/**
	 * Configure the {@link IConfigurationClassLoader}'s implementation
	 * @throws ClassLoaderException when the {@link IConfigurationClassLoader}'s implementation cannot retrieve or load the configuration files
	 */
	void configure(@Nullable IbisContext ibisContext, @NonNull String configurationName) throws ClassLoaderException;

	/**
	 * Retrieve the name of the configuration that uses this {@link IConfigurationClassLoader}
	 */
	@NonNull String getConfigurationName();

	/**
	 * Defines the log level for errors caused by the {@link IConfigurationClassLoader#configure(IbisContext, String) configure} method
	 * @param level ReportLevel in string format to be parsed by the ClassLoaderFactory digester
	 */
	void setReportLevel(@NonNull String level);

	/**
	 * @return the {@link ReportLevel} set for this {@link IConfigurationClassLoader}
	 */
	@NonNull ReportLevel getReportLevel();

	/**
	 * Unload the current IConfigurationClassLoader and load again upon successful unload.
	 */
	void reload() throws ClassLoaderException;

	/**
	 * Called upon destroying (and thus removing) the IConfigurationClassLoader
	 */
	void destroy();
}
