/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.console.runner;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import org.apache.commons.io.FilenameUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.core.SmartClassLoader;

import lombok.extern.log4j.Log4j2;

import org.frankframework.util.StreamUtil;

/**
 * Abstract base class for for IBIS Configuration ClassLoaders.
 *
 * Appends a BasePath to every resource when set. This allows the use of sub config's in src/main/resources
 * When a file with prepended BasePath cannot be found it will traverse through it's classpath to find it.
 *
 * @author Niels Meijer
 */
@Log4j2
public class DirectoryClassLoader extends ClassLoader implements SmartClassLoader {

	private File directory;

	private final List<String> loadedCustomClasses = new ArrayList<>();

	protected DirectoryClassLoader(String directory) {
		this(Thread.currentThread().getContextClassLoader(), directory);
	}

	protected DirectoryClassLoader(ClassLoader parent, String directory) {
		super(parent);

		File dir = new File(directory);
		if(!dir.isDirectory()) {
			throw new IllegalStateException("directory ["+directory+"] not found");
		}

		this.directory = dir.getAbsoluteFile();

		log.info("Using additional classpath [{}]", directory);
	}

	/**
	 * Override this method and make it final so nobody can overwrite it.
	 * Implementations of this class should use {@link DirectoryClassLoader#getLocalResource(String)}
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

		return getResource(name, true);
	}

	/**
	 * @param name of the file to search for in the current local classpath
	 * @return the URL of the file if found in the ClassLoader or <code>null</code> when the file cannot be found
	 */
	public URL getLocalResource(String name) {
		File file = new File(directory, name);
		if (file.exists()) {
			try {
				return file.toURI().toURL();
			} catch (MalformedURLException e) {
				log.error("could not create url for [{}]", name, e);
			}
		}

		return null;
	}

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
			log.trace("{} local resource [{}]", url == null ? "failed to retrieve" : "retrieved", normalizedFilename);

		// URL without basepath cannot be found, follow parent hierarchy
		if(url == null && useParent) {
			url = getParent().getResource(name);
			if(log.isTraceEnabled())
				log.trace("{} resource [{}] from parent", url == null ? "failed to retrieve" : "retrieved", name);
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

		if(log.isTraceEnabled()) log.trace("retrieved files [{}] found urls {}", name, urls);

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
	@NonNull
	@Override
	public Class<?> publicDefineClass(@NonNull String name, @NonNull byte[] b, @Nullable ProtectionDomain protectionDomain) {
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

		throw new ClassNotFoundException("class ["+name+"] not found in classloader ["+this+"]"); // Throw ClassNotFoundException when nothing was found
	}

	@Override
	public String toString() {
		return "%s directory [%s]".formatted(super.toString(), directory);
	}
}
