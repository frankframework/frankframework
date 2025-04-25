/*
   Copyright 2018, 2019 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.configuration;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.classloaders.IConfigurationClassLoader;
import org.frankframework.configuration.classloaders.WebAppClassLoader;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.MessageKeeper.MessageKeeperLevel;
import org.frankframework.util.StringUtil;

/**
 * Loads a ClassLoader on a per Configuration basis. It is possible to specify the ClassLoader type and to make
 * ClassLoaders inherit each other. If no ClassLoader is specified the WebAppClassLoader is used, which will
 * first try to search for resources on the basepath and then the webapp and classpath.
 *
 * @author Niels Meijer
 *
 */
@Log4j2
public class ClassLoaderManager {

	private final AppConstants APP_CONSTANTS = AppConstants.getInstance(); // Cannot make this final because of tests
	private final int MAX_CLASSLOADER_ITEMS = APP_CONSTANTS.getInt("classloader.items.max", 100);
	private final Map<String, ClassLoader> classLoaders = new TreeMap<>();
	private final ClassLoader classPathClassLoader = Thread.currentThread().getContextClassLoader();

	private IbisContext ibisContext;

	public ClassLoaderManager(IbisContext ibisContext) {
		this.ibisContext = ibisContext;
	}

	private ClassLoader createClassloader(String configurationName, String classLoaderType) throws ClassLoaderException {
		return createClassloader(configurationName, classLoaderType, classPathClassLoader);
	}

	private ClassLoader createClassloader(String configurationName, String classLoaderType, ClassLoader parentClassLoader) throws ClassLoaderException {
		// It is possible that no ClassLoader has been defined, use default ClassLoader
		if(classLoaderType == null || classLoaderType.isEmpty())
			throw new ClassLoaderException("classLoaderType cannot be empty");

		String className = classLoaderType;
		if(classLoaderType.indexOf(".") == -1)
			className = "org.frankframework.configuration.classloaders." + classLoaderType;

		log.debug("trying to create classloader of type[{}]", className);

		final ClassLoader classLoader;
		try {
			Class<?> clas = ClassUtils.loadClass(className);
			Constructor<?> con = ClassUtils.getConstructorOnType(clas, new Class[] {ClassLoader.class});
			classLoader = (ClassLoader) con.newInstance(new Object[] {parentClassLoader});
		}
		catch (Exception e) {
			throw new ClassLoaderException("invalid classLoaderType ["+className+"]", e);
		}
		log.debug("successfully instantiated classloader [{}] with parent classloader [{}]", () -> ClassUtils.nameOf(classLoader), () -> ClassUtils.nameOf(parentClassLoader));

		// If the classLoader implements IClassLoader, configure it
		if(classLoader instanceof IConfigurationClassLoader loader) {

			String parentProperty = "configurations." + configurationName + ".";

			for(Method method: loader.getClass().getMethods()) {
				if(!method.getName().startsWith("set") || method.getParameterTypes().length != 1)
					continue;

				String setter = StringUtil.lcFirst(method.getName().substring(3));
				String value = APP_CONSTANTS.getProperty(parentProperty+setter);
				if(value == null)
					continue;

				// Only always grab the first value because we explicitly check method.getParameterTypes().length != 1
				Object castValue = getCastValue(method.getParameterTypes()[0], value);
				log.debug("trying to set property [{}{}] with value [{}] of type [{}] on [{}]", parentProperty, setter, value, castValue.getClass()
						.getCanonicalName(), ClassUtils.nameOf(loader));

				try {
					method.invoke(loader, castValue);
				} catch (Exception e) {
					throw new ClassLoaderException("error while calling method ["+setter+"] on classloader ["+ClassUtils.nameOf(loader)+"]", e);
				}
			}

			try {
				loader.configure(ibisContext, configurationName);
			}
			catch (ClassLoaderException ce) {
				String msg = "error configuring ClassLoader for configuration ["+configurationName+"]";
				switch(loader.getReportLevel()) {
					case DEBUG:
						log.debug(msg, ce);
						break;
					case INFO:
						ibisContext.log(msg, MessageKeeperLevel.INFO, ce);
						break;
					case WARN:
						ApplicationWarnings.add(log, msg, ce);
						break;
					case ERROR:
					default:
						throw ce;
				}

				// Break here, we cannot continue when there are ConfigurationExceptions!
				return null;
			}
			log.info("configured classloader [{}]", () -> ClassUtils.nameOf(loader));
		}

		return classLoader;
	}

	private Object getCastValue(Class<?> class1, String value) {
		String className = class1.getName().toLowerCase();
		if("boolean".equals(className))
			return Boolean.parseBoolean(value);
		else if("int".equals(className) || "integer".equals(className))
			return Integer.parseInt(value);
		else
			return value;
	}

	private ClassLoader init(String configurationName, String classLoaderType) throws ClassLoaderException {
		return init(configurationName, classLoaderType, APP_CONSTANTS.getString("configurations." + configurationName + ".parentConfig", null));
	}

	private ClassLoader init(String configurationName, String classLoaderType, String parentConfig) throws ClassLoaderException {
		if(contains(configurationName))
			throw new ClassLoaderException("unable to add configuration with duplicate name ["+configurationName+"]");

		if(StringUtils.isEmpty(classLoaderType)) {
			classLoaderType = APP_CONSTANTS.getString("configurations." + configurationName + ".classLoaderType", "");
			if(StringUtils.isEmpty(classLoaderType)) {
				classLoaderType = WebAppClassLoader.class.getSimpleName();
			}
		}

		log.info("attempting to create new ClassLoader of type [{}] for configuration [{}]", classLoaderType, configurationName);

		ClassLoader classLoader;
		if(StringUtils.isNotEmpty(parentConfig)) {
			if(!contains(parentConfig))
				throw new ClassLoaderException("failed to locate parent configuration ["+parentConfig+"]");

			classLoader = createClassloader(configurationName, classLoaderType, get(parentConfig));
			log.debug("created a new classLoader [{}] with parentConfig [{}]", () -> ClassUtils.nameOf(classLoader), () -> parentConfig);
		}
		else
			classLoader = createClassloader(configurationName, classLoaderType);

		if(classLoader == null) {
			// A databaseClassloader error occurred, cancel, break, abort (but don't throw a ConfigurationException!
			// If this is thrown, the ibis developer specifically did not want to throw an exception.
			return null;
		}

		classLoaders.put(configurationName, classLoader);
		if (classLoaders.size() > MAX_CLASSLOADER_ITEMS) {
			String msg = "Number of ClassLoader instances exceeds [" + MAX_CLASSLOADER_ITEMS + "]. Too many ClassLoader instances can cause an OutOfMemoryError";
			ApplicationWarnings.add(log, msg);
		}
		return classLoader;
	}

	/**
	 * Returns the ClassLoader for a specific configuration.
	 * @param configurationName to get the ClassLoader for
	 * @return ClassLoader or null on error
	 * @throws ClassLoaderException when a ClassLoader failed to initialize
	 */
	public ClassLoader get(String configurationName) throws ClassLoaderException {
		return get(configurationName, null);
	}

	/**
	 * Returns the ClassLoader for a specific configuration. Creates the ClassLoader if it doesn't exist yet.
	 * @param configurationName to get the ClassLoader for
	 * @param classLoaderType null or type of ClassLoader to load
	 * @return ClassLoader or null on error
	 * @throws ClassLoaderException when a ClassLoader failed to initialize
	 */
	public ClassLoader get(String configurationName, String classLoaderType) throws ClassLoaderException {
		if(ibisContext == null) {
			throw new IllegalStateException("shutting down");
		}

		log.debug("get configuration ClassLoader [{}]", configurationName);
		ClassLoader classLoader = classLoaders.get(configurationName);
		if (classLoader == null) {
			classLoader = init(configurationName, classLoaderType);
		}
		return classLoader;
	}

	/**
	 * Reloads a configuration if it exists. Does not create a new one!
	 * See {@link #reload(ClassLoader)} for more information
	 */
	public void reload(String configurationName) throws ClassLoaderException {
		if(ibisContext == null) {
			throw new IllegalStateException("shutting down");
		}

		ClassLoader classLoader = classLoaders.get(configurationName);
		if (classLoader != null) {
			reload(classLoader);
		} else {
			log.warn("classloader for configuration [{}] not found, ignoring reload", configurationName);
		}
	}

	/**
	 * Reuse class loader as it is difficult to have all
	 * references to the class loader removed (see also
	 * http://zeroturnaround.com/rebellabs/rjc201/).
	 * Create a heapdump after an unload and garbage collect and
	 * view the references to the instances of the root class
	 * loader class (BasePathClassLoader when a base path is
	 * used).
	 */
	public void reload(ClassLoader classLoader) throws ClassLoaderException {
		if(ibisContext == null) {
			throw new IllegalStateException("shutting down");
		}

		if (classLoader == null)
			throw new ClassLoaderException("classloader cannot be null");

		if (classLoader instanceof IConfigurationClassLoader loader) {
			loader.reload();
		} else {
			log.warn("classloader [{}] does not derive from IConfigurationClassLoader, ignoring reload", () -> ClassUtils.nameOf(classLoader));
		}
	}

	public boolean contains(String currentConfigurationName) {
		return classLoaders.containsKey(currentConfigurationName);
	}

	/**
	 * Removes all created ClassLoaders
	 */
	public void shutdown() {
		ibisContext = null; // Remove ibisContext reference

		for (Iterator<String> iterator = classLoaders.keySet().iterator(); iterator.hasNext();) {
			String configurationClassLoader = iterator.next();
			ClassLoader classLoader = classLoaders.get(configurationClassLoader);
			if(classLoader instanceof IConfigurationClassLoader loader) {
				loader.destroy();
			} else {
				log.warn("classloader [{}] does not derive from IConfigurationClassLoader, ignoring destroy", () -> ClassUtils.nameOf(classLoader));
			}
			iterator.remove();
			log.info("removed classloader [{}]", ClassUtils.nameOf(classLoader));
		}
		if(!classLoaders.isEmpty()) {
			log.warn("not all ClassLoaders where removed. Removing references to remaining classloaders {}", classLoaders);

			classLoaders.clear();
		}
	}
}
