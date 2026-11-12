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
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.lang.Contract;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.classloaders.ClassLoadingLeakDetector;
import org.frankframework.configuration.classloaders.IConfigurationClassLoader;
import org.frankframework.configuration.classloaders.WebAppClassLoader;
import org.frankframework.lifecycle.events.MessageEventLevel;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.PropertyLoader;
import org.frankframework.util.SpringUtils;

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
	private @Setter IbisContext ibisContext;

	public ClassLoaderFactory() {
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

	public @Nullable ClassLoader createClassLoader(@NonNull String configurationName, @NonNull Class<? extends ClassLoader> clas, @Nullable ClassLoader parentClassLoader) throws ClassLoaderException {
		final ClassLoader classLoader;
		try {
			Constructor<?> con = ClassUtils.getConstructorOnType(clas, new Class[] {ClassLoader.class});
			classLoader = (ClassLoader) con.newInstance(parentClassLoader);
		} catch (Exception | LinkageError e) {
			throw new ClassLoaderException("invalid classLoaderType ["+clas.getCanonicalName()+"]", e);
		}
		log.debug("successfully instantiated classloader [{}] with parent classloader [{}]", () -> ClassUtils.nameOf(classLoader), () -> ClassUtils.nameOf(parentClassLoader));
		// Register classloader with LeakDetector
		ClassLoadingLeakDetector.registerClassLoader(configurationName, classLoader);

		try {
			applyConfigurationProperties(configurationName, classLoader);
		} catch (ClassLoaderException | RuntimeException ce) {
			String msg = "error configuring ClassLoader for configuration ["+configurationName+"]";
			switch (getReportLevel(classLoader)) {
				case DEBUG -> log.debug(msg, ce);
				case INFO -> ibisContext.log(msg, MessageEventLevel.INFO, ce);
				case WARN -> ApplicationWarnings.add(log, msg, ce);
				case ERROR -> throw ce;
				case null -> throw ce;
			}

			// Break here, we cannot continue when there are ConfigurationExceptions!
			return null;
		}
		log.info("configured classloader [{}]", () -> ClassUtils.nameOf(classLoader));


		return classLoader;
	}

	private static IConfigurationClassLoader. @Nullable ReportLevel getReportLevel(ClassLoader classLoader) {
		if (classLoader instanceof IConfigurationClassLoader configurationClassLoader) {
			return configurationClassLoader.getReportLevel();
		}
		return null;
	}

	private void applyConfigurationProperties(@NonNull String configurationName, @NonNull ClassLoader loader) throws ClassLoaderException {
		String parentProperty = "configurations." + configurationName + ".";
		Properties subProperties = PropertyLoader.getSubProperties(appConstants, parentProperty);
		subProperties.setProperty("configurationName", configurationName);
		ClassUtils.invokeSetters(loader, subProperties);
		try {
			SpringUtils.autowireByType(ibisContext.getApplicationContext(), loader);
		} catch (Exception e) {
			log.error("error configuring ClassLoader for configuration [{}]", configurationName, e);
			throw new ClassLoaderException(e);
		}

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
