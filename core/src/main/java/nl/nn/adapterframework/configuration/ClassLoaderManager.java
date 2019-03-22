/*
   Copyright 2018, 2019 Nationale-Nederlanden

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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.classloaders.BasePathClassLoader;
import nl.nn.adapterframework.configuration.classloaders.IConfigurationClassLoader;
import nl.nn.adapterframework.configuration.classloaders.ReloadAware;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

public class ClassLoaderManager {

	private static final Logger LOG = LogUtil.getLogger(ClassLoaderManager.class);
	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();
	private Map<String, ClassLoader> classLoaders = new TreeMap<String, ClassLoader>();

	private IbisContext ibisContext;

	public ClassLoaderManager(IbisContext ibisContext) {
		this.ibisContext = ibisContext;
	}

	private ClassLoader createClassloader(String configurationName, String configurationFile) throws ConfigurationException {
		return createClassloader(configurationName, configurationFile, Thread.currentThread().getContextClassLoader());
	}

	private ClassLoader createClassloader(String configurationName, String configurationFile, ClassLoader parentClassLoader) throws ConfigurationException {

		String classLoaderType = APP_CONSTANTS.getString("configurations." + configurationName + ".classLoaderType", "");
		//It is possible that no ClassLoader has been defined, use default ClassLoader (wrapped in a WebAppClassLoader)
		if(classLoaderType == null || classLoaderType.isEmpty())
			classLoaderType = "WebAppClassLoader";

		String className = classLoaderType;
		if(classLoaderType.indexOf(".") == -1)
			className = "nl.nn.adapterframework.configuration.classloaders."+classLoaderType;

		LOG.debug("trying to create classloader of type["+className+"]");

		ClassLoader classLoader = null;
		try {
			Class<?> clas = ClassUtils.loadClass(className);
			Constructor<?> con = ClassUtils.getConstructorOnType(clas, new Class[] {ClassLoader.class});
			classLoader = (ClassLoader) con.newInstance(new Object[] {parentClassLoader});
		}
		catch (Exception e) {
			throw new ConfigurationException("invalid classLoaderType ["+className+"]", e);
		}
		LOG.debug("successfully instantiated classloader ["+classLoader.toString()+"] with parent classloader ["+parentClassLoader.toString()+"]");

		//If the classLoader implements IClassLoader, configure it
		if(classLoader instanceof IConfigurationClassLoader) {
			IConfigurationClassLoader loader = (IConfigurationClassLoader) classLoader;

			String parentProperty = "configurations." + configurationName + ".";
			Properties props = APP_CONSTANTS.getAppConstants(parentProperty);
			for(Object objKey: props.keySet()) {
				String key = (String) objKey;
				if(key.endsWith("classLoaderType") || key.endsWith("parentConfig"))
					continue;

				String setter = key.substring(parentProperty.length());
				String value = props.getProperty(key);
				LOG.debug("trying to set property ["+setter+"] with value ["+value+"] on ["+classLoader.toString()+"]");
				try {
					Method methodcall = loader.getClass().getMethod(getMethod(setter), String.class);
					methodcall.invoke(loader, value);
				}
				catch (NoSuchMethodException e) {
					throw new ConfigurationException("property ["+parentProperty+setter+"] does not exist in classloader ["+classLoader.toString()+"]", e);
				}
				catch (Exception e) {
					throw new ConfigurationException("error while calling method ["+getMethod(setter)+"] on classloader ["+classLoader.toString()+"]", e);
				}
			}

			try {
				loader.configure(ibisContext, configurationName);
			}
			catch (ConfigurationException ce) {
				String msg = "Could not get config '" + configurationName + "' from database, skipping";
				switch(loader.getReportLevel()) {
					case DEBUG:
						LOG.debug(msg);
						break;
					case INFO:
						ibisContext.log(msg);
						break;
					case WARN:
						ConfigurationWarnings.getInstance().add(LOG, msg);
						break;
					case ERROR:
					default:
						throw ce;
				}

				//Break here, we cannot continue when there are ConfigurationExceptions!
				return null;
			}
			LOG.info("configured classloader ["+classLoader.toString()+"] for configuration ["+configurationName+"]");
		}

		return classLoader;
	}

	private String getMethod(String input) {
		return "set" + input.substring(0, 1).toUpperCase() + input.substring(1);
	}

	public ClassLoader init(String configurationName) throws ConfigurationException {
		String parentConfig = APP_CONSTANTS.getString(
				"configurations." + configurationName + ".parentConfig", null);
		return init(configurationName, parentConfig);
	}

	public ClassLoader init(String configurationName, String parentConfig) throws ConfigurationException {
		if(contains(configurationName))
			throw new ConfigurationException("unable to add configuration with duplicate name ["+configurationName+"]");

		String configurationFile = ibisContext.getConfigurationFile(configurationName);
		LOG.info("attempting to create new ClassLoader for configuration ["+configurationName+"] with configurationFile ["+configurationFile+"]");

		ClassLoader classLoader;
		if(parentConfig != null) {
			if(!contains(parentConfig))
				throw new ConfigurationException("failed to locate parent configuration ["+parentConfig+"]");

			classLoader = createClassloader(configurationName, configurationFile, get(parentConfig));
			LOG.debug("wrapped classLoader ["+classLoader.toString()+"] in parentConfig ["+parentConfig+"]");
		}
		else
			classLoader = createClassloader(configurationName, configurationFile);

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
		LOG.debug("get configuration ClassLoader ["+configurationName+"]");
		ClassLoader classLoader = classLoaders.get(configurationName);
		if (classLoader == null)
			classLoader = init(configurationName);

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
