/*
   Copyright 2019 Nationale-Nederlanden, 2024 WeAreFrank!

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

import java.io.IOException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import jakarta.annotation.Nullable;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.core.SmartClassLoader;

import org.frankframework.configuration.ApplicationWarnings;
import org.frankframework.configuration.ClassLoaderException;
import org.frankframework.configuration.ConfigurationUtils;
import org.frankframework.configuration.IbisContext;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StreamUtil;

/**
 * Abstract base class for for IBIS Configuration ClassLoaders.
 *
 * Appends a BasePath to every resource when set. This allows the use of sub config's in src/main/resources
 * When a file with prepended BasePath cannot be found it will traverse through it's classpath to find it.
 *
 * @author Niels Meijer
 */
public abstract class AbstractClassLoader extends ClassLoader implements IConfigurationClassLoader, SmartClassLoader {

	private IbisContext ibisContext = null;
	private String configurationName = null;
	private String configurationFile = ConfigurationUtils.DEFAULT_CONFIGURATION_FILE;

	protected Logger log = LogUtil.getLogger(this);
	private ReportLevel reportLevel = ReportLevel.ERROR;

	private String instanceName = AppConstants.getInstance().getProperty("instance.name");
	private String basePath = null;

	private boolean allowCustomClasses = AppConstants.getInstance().getBoolean("configurations.allowCustomClasses", false);
	private List<String> loadedCustomClasses = new ArrayList<>();

	protected AbstractClassLoader() {
		this(Thread.currentThread().getContextClassLoader());
	}

	protected AbstractClassLoader(ClassLoader parent) {
		super(parent);
	}

	@Override
	public void configure(IbisContext ibisContext, String configurationName) throws ClassLoaderException {
		this.ibisContext = ibisContext;
		this.configurationName = configurationName;

		if(StringUtils.isEmpty(configurationFile)) {
			throw new ClassLoaderException("unable to determine configurationFile");
		}

		if(basePath == null) {
			int i = configurationFile.lastIndexOf('/');
			if (i != -1) { // Configuration file contains a path, derive the BasePath from the path
				setBasePath(configurationFile.substring(0, i + 1));
				setConfigurationFile(configurationFile.substring(i + 1));
				log.info("derived basepath [{}] from configurationFile [{}]", getBasePath(), configurationFile);
			} else if(!(getConfigurationName().equalsIgnoreCase(instanceName) && this instanceof WebAppClassLoader)) {
				setBasePath(getConfigurationName());
			}
		}

		log.info("[{}] created classloader [{}] basepath [{}]", getConfigurationName(), this.toString(), getBasePath());
	}

	/**
	 * Sets the path prefix used when {@link AbstractClassLoader#getLocalResource(String) getLocalResource()} is called.
	 * @param basePath path to use, defaults to the configuration name
	 */
	public void setBasePath(String basePath) {
		if(StringUtils.isNotEmpty(basePath)) {
			if(!basePath.endsWith("/"))
				basePath += "/";

			this.basePath = FilenameUtils.normalize(basePath, true);
		}
	}

	/**
	 * The root directory where all resources are located. This may purely be used within the ClassLoader and resources
	 * should not be aware of this 'root' directory.
	 *
	 * @return the path prefix that is used for retrieving files through this ClassLoader
	 */
	protected String getBasePath() {
		return basePath;
	}

	@Override
	public String getConfigurationName() {
		return configurationName;
	}

	/**
	 * The configurationFile should only ever be found in the current classloader and never in it's parent
	 */
	public String getConfigurationFile() {
		return configurationFile;
	}
	public void setConfigurationFile(String configurationFile) {
		this.configurationFile = configurationFile;
	}

	/**
	 * Only for internal use within ClassLoaders
	 * Retrieve the IbisContext from the ClassLoader which is set when the {@link IConfigurationClassLoader#configure(IbisContext, String) configure} method is called
	 */
	protected IbisContext getIbisContext() {
		return ibisContext;
	}

	@Override
	public void setReportLevel(String level) {
		try {
			this.reportLevel = ReportLevel.valueOf(level.toUpperCase());
		}
		catch (IllegalArgumentException e) {
			ApplicationWarnings.add(log, "invalid reportLevel ["+level+"], using default [ERROR]");
		}
	}

	@Override
	public ReportLevel getReportLevel() {
		return reportLevel;
	}

	public void setAllowCustomClasses(boolean allow) {
		allowCustomClasses = allow;
	}

	protected boolean getAllowCustomClasses() {
		return allowCustomClasses;
	}

	/**
	 * Override this method and make it final so nobody can overwrite it.
	 * Implementations of this class should use {@link AbstractClassLoader#getLocalResource(String)}
	 */
	@Override
	public final URL getResource(String name) {
		if (name == null || name.startsWith("/")) { // Resources retrieved from ClassLoaders should never start with a leading slash
			log.warn(new IllegalStateException("resources retrieved from ClassLoaders should not use an absolute path ["+name+"]")); // Use an exception so we can 'trace the stack'
			return null;
		}

		// It will and should never find files that are in the META-INF folder in this classloader, so always traverse to it's parent classloader
		if(name.startsWith("META-INF/")) {
			return getParent().getResource(name);
		}

		// The configurationFile (Configuration.xml) should only be found in the current and not it's parent classloader
		if(getBasePath() != null && name.equals(getConfigurationFile())) {
			return getResource(name, false); // Search for the resource in the local ClassLoader only
		}

		return getResource(name, true);
	}

	/**
	 * @param name of the file to search for in the current local classpath
	 * @return the URL of the file if found in the ClassLoader or <code>null</code> when the file cannot be found
	 */
	protected abstract URL getLocalResource(String name);

	/**
	 * In case of the {@link #getResources(String)} we only want the local paths and not the parent path
	 * @param name of the file to retrieve
	 * @param useParent only use local classpath or also traverse down the classpath
	 * @return the URL of the file if found in the ClassLoader or <code>null</code>
	 */
	public URL getResource(String name, boolean useParent) {
		URL url = null;
		String normalizedFilename = FilenameUtils.normalize(name, true);
		if(normalizedFilename == null) {
			return null; // If the path after normalization equals null, return null
		}
		// Resources retrieved from ClassLoaders should never start with a leading slash
		if (normalizedFilename.startsWith("/")) {
			normalizedFilename = normalizedFilename.substring(1);
		}

		url = getLocalResource(normalizedFilename);
		if(log.isTraceEnabled())
			log.trace("[{}] {} local resource [{}]", getConfigurationName(), url == null ? "failed to retrieve" : "retrieved", normalizedFilename);

		// URL without basepath cannot be found, follow parent hierarchy
		if(url == null && useParent) {
			url = getParent().getResource(name);
			if(log.isTraceEnabled())
				log.trace("[{}] {} resource [{}] from parent", getConfigurationName(), url == null ? "failed to retrieve" : "retrieved", name);
		}

		return url;
	}

	@Override
	public final Enumeration<URL> getResources(String name) throws IOException {
		// It will and should never find files that are in the META-INF folder in this classloader, so always traverse to it's parent
		if (name.startsWith("META-INF/services")) {
			return getParent().getResources(name);
		}

		Vector<URL> urls = new Vector<>();

		// Search for the file in the local classpath only
		URL localResource = getResource(name, false);
		if (localResource != null) {
			urls.add(localResource);
		}

		// Add all files found in the classpath's parent
		urls.addAll(Collections.list(getParent().getResources(name)));

		if(log.isTraceEnabled()) log.trace("[{}] retrieved files [{}] found urls {}", getConfigurationName(), name, urls);

		return urls.elements();
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if(name == null) {
			throw new IllegalArgumentException("classname to load may not be null");
		}

		// This is required because when using an external WebAppClassloader (ClassPath) inner classes may be retrieved from the wrong ClassLoader
		int dollar = name.lastIndexOf("$");
		if (dollar > 0) {
			String baseClass = name.substring(0, dollar);
			if(loadedCustomClasses.contains(baseClass)) {
				return defineClass(name, resolve);
			}
		}

		Throwable throwable = null;
		try {
			return super.loadClass(name, resolve); // First try to load the class natively
		} catch (ClassNotFoundException | NoClassDefFoundError t) { // Catch NoClassDefFoundError and ClassNotFoundExceptions
			throwable = t;
		}

		try {
			return defineClass(name, resolve);
		} catch (ClassNotFoundException e) {
			e.addSuppressed(throwable);
			throw e;
		}
	}

	/**
	 * <p>
	 * Fixes <code>--add-opens=java.base/java.lang=ALL-UNNAMED</code> problem when loading classes dynamically when CGLIB is enabled.
	 * See <a href="https://github.com/spring-projects/spring-framework/issues/26403">spring github</a> for more background information.
	 * </p>
	 *
	 * {@inheritDoc}
	 */
	@Override
	public Class<?> publicDefineClass(String name, byte[] b, @Nullable ProtectionDomain protectionDomain) {
		return super.defineClass(name, b, 0, b.length, protectionDomain);
	}

	/**
	 * This method will only be called for classes that have not been previously loaded yet.
	 * Custom code will not update if you change the configuration.
	 *
	 * Introspector#findExplicitBeanInfo/BeanInfoFinder#find attempts to lookup classes with the 'BeanInfo' postfix.
	 * Introspector#findCustomizerClass attempts to lookup classes with the 'Customizer' postfix.
	 */
	private Class<?> defineClass(String name, boolean resolve) throws ClassNotFoundException {
		if(getAllowCustomClasses()) {
			synchronized (getClassLoadingLock(name)) {
				String path = name.replace(".", "/").concat(".class");
				log.trace("attempting to load custom class [{}] path [{}]", name, path);

				URL url = getResource(path);
				if(url != null) {
					log.debug("found custom class url [{}] from classloader [{}] with path [{}]", url, this, path);

					try {
						byte[] bytes = StreamUtil.streamToBytes(url.openStream());
						ProtectionDomain protectionDomain = ReflectUtils.getProtectionDomain(this.getClass());
						Class<?> clazz = publicDefineClass(name, bytes, protectionDomain);

						if(resolve) {
							resolveClass(clazz);
						}

						loadedCustomClasses.add(name);

						return clazz;
					} catch (Exception e) {
						throw new ClassNotFoundException("failed to load class ["+path+"] in classloader ["+this+"]", e);
					}
				}
			}
		}

		throw new ClassNotFoundException("class ["+name+"] not found in classloader ["+this+"]"); // Throw ClassNotFoundException when nothing was found
	}

	@Override
	public void reload() throws ClassLoaderException {
		log.debug("reloading classloader [{}]", getConfigurationName());

		AppConstants.removeInstance(this);
	}

	@Override
	public void destroy() {
		log.debug("removing classloader [{}]", this.toString());

		AppConstants.removeInstance(this);
	}

	@Override
	public String toString() {
		String logPrefix = ClassUtils.classNameOf(this) + "@" + Integer.toHexString(this.hashCode());

		String configurationName = getConfigurationName();
		if(StringUtils.isNotEmpty(configurationName)) {
			logPrefix += "["+configurationName+"]";
		}

		return logPrefix;
	}
}
