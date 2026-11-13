/*
   Copyright 2018, 2019 Nationale-Nederlanden, 2020-2026 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.lang.Contract;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.classloaders.ClassLoadingLeakDetector;
import org.frankframework.configuration.classloaders.IConfigurationClassLoader;
import org.frankframework.configuration.classloaders.WebAppClassLoader;
import org.frankframework.lifecycle.events.MessageEventLevel;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;
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
public class ClassLoaderFactory {
	public static final String CLASSLOADER_PACKAGE_LOCATION = "org.frankframework.configuration.classloaders.%s";

	// Cannot make this static final because of tests
	private final AppConstants appConstants = AppConstants.getInstance();
	private final ClassLoader classPathClassLoader;
	private IbisContext ibisContext;

	public ClassLoaderFactory(IbisContext ibisContext) {
		this.ibisContext = ibisContext;

		classPathClassLoader = Thread.currentThread().getContextClassLoader();
	}

	private @NonNull Class<? extends ClassLoader> loadClassLoaderClass(@NonNull String classLoaderType) throws ClassLoaderException {
		String className = classLoaderType.contains(".") ? classLoaderType : CLASSLOADER_PACKAGE_LOCATION.formatted(classLoaderType);
		log.debug("trying to create classloader of type[{}]", className);

		try {
			Class<?> clas = ClassUtils.loadClass(className);
			//noinspection unchecked
			return (Class<? extends ClassLoader>) clas;
		} catch (Exception | LinkageError e) {
			throw new ClassLoaderException("invalid classLoaderType ["+className+"]", e);
		}
	}

	private @Nullable ClassLoader createClassLoader(@NonNull String configurationName, @NonNull Class<? extends ClassLoader> clas, @Nullable ClassLoader parentClassLoader) throws ClassLoaderException {
		final ClassLoader classLoader;
		try {
			Constructor<?> con = ClassUtils.getConstructorOnType(clas, new Class[] {ClassLoader.class});
			classLoader = (ClassLoader) con.newInstance(parentClassLoader);
		} catch (Exception | LinkageError e) {
			throw new ClassLoaderException("invalid classLoaderType ["+clas.getCanonicalName()+"]", e);
		}
		log.debug("successfully instantiated classloader [{}] with parent classloader [{}]", () -> ClassUtils.nameOf(classLoader), () -> ClassUtils.nameOf(parentClassLoader));
		// Register any classloader regardless if it's a ConfigurationClassLoader or not
		ClassLoadingLeakDetector.registerClassLoader(configurationName, classLoader);


		if (classLoader instanceof IConfigurationClassLoader configurationClassLoader) {
			applyConfigurationProperties(configurationName, configurationClassLoader);
			try {
				configurationClassLoader.configure(ibisContext, configurationName);
			} catch (ClassLoaderException ce) {
				String msg = "error configuring ClassLoader for configuration ["+configurationName+"]";
				switch (configurationClassLoader.getReportLevel()) {
					case DEBUG -> log.debug(msg, ce);
					case INFO -> ibisContext.log(msg, MessageEventLevel.INFO, ce);
					case WARN -> ApplicationWarnings.add(log, msg, ce);
					case ERROR -> throw ce;
					case null -> throw ce; // NULL cannot really happen but this makes Codacy happy
				}

				// Break here, we cannot continue when there are ConfigurationExceptions!
				return null;
			}
			log.info("configured classloader [{}]", () -> ClassUtils.nameOf(classLoader));
		}

		return classLoader;
	}

	@SuppressWarnings("java:S135") // Suppress warning for multiple continue statements in for loop
	private void applyConfigurationProperties(@NonNull String configurationName, @NonNull IConfigurationClassLoader loader) throws ClassLoaderException {
		String parentProperty = "configurations." + configurationName + ".";

		for (Method method: loader.getClass().getMethods()) {
			if(!method.getName().startsWith("set") || method.getParameterTypes().length != 1)
				continue;

			String setter = StringUtil.lcFirst(method.getName().substring(3));
			String value = appConstants.getProperty(parentProperty+setter);
			if (value == null)
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
	}

	private @NonNull Object getCastValue(@NonNull Class<?> class1, @NonNull String value) {
		String className = class1.getName().toLowerCase();
		return switch (className) {
			case "boolean" -> Boolean.parseBoolean(value.trim());
			case "int", "integer" -> Integer.parseInt(value.trim());
			case "long" -> Long.parseLong(value.trim());
			default -> value;
		};
	}

	public @Nullable ClassLoader createClassLoader(@NonNull String configurationName, @Nullable Class<? extends  ClassLoader> defaultClassLoaderType, @Nullable Configuration parentConfiguration) throws ClassLoaderException {

		Class<? extends ClassLoader> actualClassLoaderType = getClassLoaderType(configurationName, defaultClassLoaderType);

		return createClassLoader(configurationName, actualClassLoaderType, parentConfiguration != null ? parentConfiguration.getClassLoader() : classPathClassLoader);
	}

	@Contract("_, !null -> param2")
	private @NonNull Class<? extends ClassLoader> getClassLoaderType(@NonNull String configurationName, @Nullable Class<? extends ClassLoader> classLoaderType) throws ClassLoaderException {
		if (classLoaderType != null) {
			return classLoaderType;
		}
		String classLoaderClassName = appConstants.getString("configurations." + configurationName + ".classLoaderType", "");
		if (StringUtils.isEmpty(classLoaderClassName)) {
			return WebAppClassLoader.class;
		}
		return loadClassLoaderClass(classLoaderClassName);
	}

	/**
	 * Removes all created ClassLoaders
	 */
	public void shutdown() {
		ibisContext = null; // Remove ibisContext reference
		ClassLoadingLeakDetector.logLeakStatistics();
	}
}
