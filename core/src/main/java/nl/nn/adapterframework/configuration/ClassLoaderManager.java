/*
   Copyright 2018 Nationale-Nederlanden

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
package nl.nn.adapterframework.configuration;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.classloaders.BasePathClassLoader;
import nl.nn.adapterframework.configuration.classloaders.DatabaseClassLoader;
import nl.nn.adapterframework.configuration.classloaders.DirectoryClassLoader;
import nl.nn.adapterframework.configuration.classloaders.DummyClassLoader;
import nl.nn.adapterframework.configuration.classloaders.JarFileClassLoader;
import nl.nn.adapterframework.configuration.classloaders.ReloadAware;
import nl.nn.adapterframework.configuration.classloaders.ServiceClassLoader;
import nl.nn.adapterframework.configuration.classloaders.WebAppClassLoader;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

public class ClassLoaderManager {

	private static final Logger LOG = LogUtil.getLogger(IbisContext.class);
	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();
	private Map<String, ClassLoader> classLoaders = new TreeMap<String, ClassLoader>();

	private IbisContext ibisContext;
	private IbisManager ibisManager;

	public ClassLoaderManager(IbisContext ibisContext) {
		this.ibisContext = ibisContext;
		this.ibisManager = ibisContext.getIbisManager();
	}

	private ClassLoader createClassloader(String configurationName, String configurationFile, String classLoaderType) throws ConfigurationException {
		return createClassloader(configurationName, configurationFile, Thread.currentThread().getContextClassLoader(), classLoaderType);
	}

	private ClassLoader createClassloader(String configurationName, String configurationFile, ClassLoader parentClassLoader, String clazzLoaderType) throws ConfigurationException {
		String classLoaderType;
		if (clazzLoaderType == null) {
			classLoaderType = APP_CONSTANTS.getString(
					"configurations." + configurationName + ".classLoaderType", "WebAppClassLoader");
		} else {
			classLoaderType = clazzLoaderType;
		}

		ClassLoader classLoader = null;
		if ("DirectoryClassLoader".equals(classLoaderType)) {
			String directory = APP_CONSTANTS.getResolvedProperty(
					"configurations." + configurationName + ".directory");
			classLoader = new DirectoryClassLoader(directory, parentClassLoader);
		} else if ("JarFileClassLoader".equals(classLoaderType)) {
			String jar = APP_CONSTANTS.getResolvedProperty(
					"configurations." + configurationName + ".jar");
			classLoader = new JarFileClassLoader(jar, configurationName, parentClassLoader);
		} else if ("ServiceClassLoader".equals(classLoaderType)) {
			String adapterName = APP_CONSTANTS.getResolvedProperty(
					"configurations." + configurationName + ".adapterName");
			classLoader = new ServiceClassLoader(ibisManager, adapterName, configurationName, parentClassLoader);
		} else if ("DatabaseClassLoader".equals(classLoaderType)) {
			try {
				classLoader = new DatabaseClassLoader(ibisContext, configurationName, parentClassLoader);
			}
			catch (ConfigurationException ce) {
				String configNotFoundReportLevel = APP_CONSTANTS.getString(
						"configurations." + configurationName + ".configNotFoundReportLevel", "ERROR").toUpperCase();

				String msg = "Could not get config '" + configurationName + "' from database, skipping";
				if(configNotFoundReportLevel.equals("DEBUG")) {
					LOG.debug(msg);
				}
				else if(configNotFoundReportLevel.equals("INFO")) {
					ibisContext.log(msg);
				}
				else if(configNotFoundReportLevel.equals("WARN")) {
					ConfigurationWarnings.getInstance().add(LOG, msg);
				}
				else {
					if(!configNotFoundReportLevel.equals("ERROR"))
						ConfigurationWarnings.getInstance().add(LOG, "Invalid configNotFoundReportLevel ["+configNotFoundReportLevel+"], using default [ERROR]");
					throw ce;
				}

				//Break here, we cannot continue when there are ConfigurationExceptions!
				return null;
			}
		} else if ("DummyClassLoader".equals(classLoaderType)) {
			classLoader = new DummyClassLoader(configurationName, configurationFile);
		} else if ("WebAppClassLoader".equals(classLoaderType) || "".equals(classLoaderType)) {
			classLoader = new WebAppClassLoader(parentClassLoader);
		} else if (classLoaderType != null) {
			throw new ConfigurationException("Invalid classLoaderType: " + classLoaderType);
		}

		//It is possible that no classloader has been defined, use default contextClassloader.
		if (classLoader == null) {
			classLoader = parentClassLoader;
		}
		LOG.debug(configurationName + " created classloader [" + classLoader.getClass().getSimpleName() + "]");
		return classLoader;
	}

	public ClassLoader init(String configurationName) throws ConfigurationException {
		String parentConfig = APP_CONSTANTS.getString(
				"configurations." + configurationName + ".parentConfig", null);
		return init(configurationName, parentConfig);
	}

	public ClassLoader init(String configurationName, String parentConfig) throws ConfigurationException {
		return init(configurationName, parentConfig, null);
	}

	public ClassLoader init(String configurationName, String parentConfig, String classLoaderType) throws ConfigurationException {
		if(contains(configurationName))
			throw new ConfigurationException("unable to add configuration with duplicate name ["+configurationName+"]");

		String configurationFile = ibisContext.getConfigurationFile(configurationName);
		LOG.info("attempting to create new configurationClassLoader for configuration ["+configurationName+"] with file ["+configurationFile+"]");

		ClassLoader classLoader;
		if(parentConfig != null) {
			if(!contains(parentConfig))
				throw new ConfigurationException("failed to locate parent configuration ["+parentConfig+"]");

			classLoader = createClassloader(configurationName, configurationFile, get(parentConfig), classLoaderType);
			LOG.debug("wrapped configuration ["+configurationName+"] in parentConfig ["+parentConfig+"]");
		}
		else
			classLoader = createClassloader(configurationName, configurationFile, classLoaderType);

		if(classLoader == null) {
			//A databaseClassloader error occurred, cancel, break, abort (but don't throw a ConfigurationException!
			//If this is thrown, the ibis developer specifically did not want to throw an exception.
			return null;
		}

		String basePath = "";
		int i = configurationFile.lastIndexOf('/');
		if (i != -1) {
			basePath = configurationFile.substring(0, i + 1);
		}

		classLoader = new BasePathClassLoader(classLoader, basePath);

		classLoaders.put(configurationName, classLoader);
		return classLoader;
	}

	/**
	 * Returns the ClassLoader for a specific configuration.
	 * @param configurationName to get the ClassLoader for
	 * @return ClassLoader or null on error
	 * @throws ConfigurationException when a ClassLoader failed to initialize
	 */
	public ClassLoader get(String configurationName) throws ConfigurationException {
		return get(configurationName, null);
	}

	public ClassLoader get(String configurationName, String classLoaderType) throws ConfigurationException {
		LOG.debug("get configuration ClassLoader ["+configurationName+"]");
		ClassLoader classLoader = classLoaders.get(configurationName);
		if (classLoader == null) {
			if (classLoaderType == null) {
				classLoader = init(configurationName);
			} else {
				classLoader = init(configurationName, null, classLoaderType);
			}
		}

		return classLoader;
	}

	public void reload(String currentConfigurationName) throws ConfigurationException {
		reload(get(currentConfigurationName));
	}

	/*
	 * Reuse class loader as it is difficult to have all
	 * references to the class loader removed (see also
	 * http://zeroturnaround.com/rebellabs/rjc201/).
	 * Create a heapdump after an unload and garbage collect and
	 * view the references to the instances of the root class
	 * loader class (BasePathClassLoader when a base path is
	 * used).
	 */
	public void reload(ClassLoader classLoader) throws ConfigurationException {
		if (classLoader == null)
			throw new ConfigurationException("classloader cannot be null");

		if (classLoader instanceof ReloadAware) {
			((ReloadAware)classLoader).reload();
		}
	}

	public boolean contains(String currentConfigurationName) {
		return (classLoaders.containsKey(currentConfigurationName));
	}
}
