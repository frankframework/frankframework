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
import org.frankframework.util.EnumUtils;
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

	/**
	 * Create and autowire a {@link ClassLoader} for a {@link Configuration} based on the configuration-specific properties for the ClassLoader.
	 *
	 * @param configurationName Name of the configuration
	 * @param defaultClassLoaderType Class for the default classloader type, if {@code null} a default will be selected first from the
	 *                               configuration property {@code 'configurations.<configuration-name>.classLoaderType'} from the {@link AppConstants}   ,
	 *                               or if that is not specified, a {@link WebAppClassLoader} will be instantiated
	 * @param parentClassLoader Optional parent classloader
	 * @return {@link ClassLoader} created, or {@code null} if there was an error, but reporting-level was not set to throw
	 * @throws ClassLoaderException If there was an error creating the class instance, or configuring it and the reportLevel was ERROR
	 */
	public @Nullable ClassLoader createClassLoader(@NonNull String configurationName, @Nullable Class<? extends  ClassLoader> defaultClassLoaderType, @Nullable ClassLoader parentClassLoader) throws ClassLoaderException {
		Class<? extends ClassLoader> actualClassLoaderType = getClassLoaderType(configurationName, defaultClassLoaderType);

		// If this fails, it should always throw
		final ClassLoader classLoader = createInstance(configurationName, actualClassLoaderType, parentClassLoader);

		Properties classLoaderConfigProperties = getConfigurationProperties(configurationName);
		try {
			// If this fail, configuration decides if it should throw or return NULL
			configureClassLoader(configurationName, classLoader, classLoaderConfigProperties);
			return classLoader;
		} catch (ClassLoaderException | RuntimeException ce) {
			String msg = "error configuring ClassLoader for configuration ["+configurationName+"]";
			switch (getReportLevel(classLoaderConfigProperties)) {
				case DEBUG -> log.debug(msg, ce);
				case INFO -> ibisContext.log(msg, MessageEventLevel.INFO, ce);
				case WARN -> ApplicationWarnings.add(log, msg, ce);
				case ERROR -> throw ce;
				case null -> throw ce; // This cannot really happen but it keeps Codacy happy
			}

			// Break here, we cannot continue when there are ConfigurationExceptions!
			return null;
		}
	}

	/**
	 * Create and autowire a {@link ClassLoader} from the given ClassLoader Class, optional parent classloader, and the configuration properties to be applied
	 * to the ClassLoader before autowiring.
	 *
	 * @param name Name for logging, and for registering the classloader with the LeakDetector
	 * @param classLoaderType Class for the type of ClassLoader
	 * @param parentClassLoader Optional parent ClassLoader
	 * @param classLoaderConfigProperties Configuration properties to be applied to this ClassLoader instance before autowiring
	 * @return ClassLoader instance
	 * @throws ClassLoaderException Thrown if any error happens while creating or configuring the ClassLoader
	 */
	public <T extends ClassLoader> @NonNull T createClassLoader(@NonNull String name, @NonNull Class<T> classLoaderType, @Nullable ClassLoader parentClassLoader, @NonNull Properties classLoaderConfigProperties) throws ClassLoaderException {
		classLoaderConfigProperties.putIfAbsent("configurationName", name);
		classLoaderConfigProperties.putIfAbsent("name", name);
		final T classLoader = createInstance(name, classLoaderType, parentClassLoader);
		configureClassLoader(name, classLoader, classLoaderConfigProperties);
		return classLoader;
	}

	private void configureClassLoader(@NonNull String name, @NonNull ClassLoader classLoader, @NonNull Properties classLoaderConfigProperties) throws ClassLoaderException {
		try {
			applyConfigurationProperties(name, classLoader, classLoaderConfigProperties);
		} catch (RuntimeException e) {
			throw new ClassLoaderException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
		}
		log.info("configured classloader [{}]", () -> ClassUtils.nameOf(classLoader));
	}

	private <T extends ClassLoader> @NonNull T createInstance(@NonNull String name, @NonNull Class<T> classLoaderType, @Nullable ClassLoader parentClassLoader) throws ClassLoaderException {
		final ClassLoader classLoader;
		try {
			Constructor<?> con = ClassUtils.getConstructorOnType(classLoaderType, new Class[] {ClassLoader.class});
			classLoader = (ClassLoader) con.newInstance(parentClassLoader != null ? parentClassLoader : classPathClassLoader);
		} catch (Exception | LinkageError e) {
			throw new ClassLoaderException("invalid classLoaderType ["+ classLoaderType.getCanonicalName()+"]", e);
		}
		log.debug("successfully instantiated classloader [{}] with parent classloader [{}]", () -> ClassUtils.nameOf(classLoader), () -> ClassUtils.nameOf(parentClassLoader));
		// Register classloader with LeakDetector
		ClassLoadingLeakDetector.registerClassLoader(name, classLoader);
		//noinspection unchecked
		return (T)classLoader;
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

	private static IConfigurationClassLoader. @NonNull ReportLevel getReportLevel(@NonNull Properties configurationProperties) {
		if (configurationProperties.containsKey("reportLevel")) {
			String reportLevel = configurationProperties.getProperty("reportLevel");
			try {
				return EnumUtils.parse(IConfigurationClassLoader.ReportLevel.class, reportLevel);
			} catch (IllegalArgumentException e) {
				log.warn("invalid reportLevel ["+reportLevel+"]", e);
			}
		}
		return IConfigurationClassLoader.ReportLevel.INFO;
	}

	private void applyConfigurationProperties(@NonNull String name, @NonNull ClassLoader loader, Properties configurationProperties) throws ClassLoaderException {
		ClassUtils.invokeSetters(loader, configurationProperties);
		try {
			SpringUtils.autowireByType(ibisContext.getApplicationContext(), loader);
		} catch (Exception e) {
			log.error("error configuring ClassLoader for [{}]", name, e);
			throw new ClassLoaderException(e);
		}
	}

	private @NonNull Properties getConfigurationProperties(@NonNull String configurationName) {
		String parentProperty = "configurations." + configurationName + ".";
		Properties subProperties = PropertyLoader.getSubProperties(appConstants, parentProperty);
		subProperties.setProperty("configurationName", configurationName);
		return subProperties;
	}

	/**
	 * Removes all created ClassLoaders
	 */
	public void shutdown() {
		ibisContext = null; // Remove ibisContext reference
		ClassLoadingLeakDetector.logLeakStatistics();
	}
}
