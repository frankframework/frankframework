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
package nl.nn.adapterframework.configuration.classloaders;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Abstract base class for for IBIS Configuration ClassLoaders.
 * 
 * Appends a BasePath to every resource when set. This allows the use of sub config's in src/main/resources
 * When a file with prepended BasePath cannot be found it will traverse through it's classpath to find it.
 * 
 * @author Niels Meijer
 *
 */
public abstract class ClassLoaderBase extends ClassLoader implements IConfigurationClassLoader, ReloadAware {
	private IbisContext ibisContext = null;
	private String configurationName = null;

	protected Logger log = LogUtil.getLogger(this);
	private ReportLevel reportLevel = ReportLevel.ERROR;

	private String instanceName = AppConstants.getInstance().getResolvedProperty("instance.name");
	private String basePath = null;

	public ClassLoaderBase() {
		this(Thread.currentThread().getContextClassLoader());
	}

	public ClassLoaderBase(ClassLoader parent) {
		super(parent);
	}

	@Override
	public void configure(IbisContext ibisContext, String configurationName) throws ConfigurationException {
		this.ibisContext = ibisContext;
		this.configurationName = configurationName;

		if(basePath == null && !getConfigurationName().equalsIgnoreCase(instanceName))
			setBasePath(getConfigurationName());

		log.info("["+getConfigurationName()+"] created classloader ["+this.getClass().getSimpleName()+"]");
	}

	/**
	 * Sets the path prefix used when {@link ClassLoaderBase#getLocalResource(String) getLocalResource()} is called.
	 * @param basePath path to use, defaults to the configuration name
	 */
	public void setBasePath(String basePath) {
		if(StringUtils.isNotEmpty(basePath) && !basePath.endsWith("/"))
			basePath += "/";

		if(StringUtils.isNotEmpty(basePath))
			this.basePath = basePath;
	}

	/**
	 * @return the path prefix that is used for retrieving files through this ClassLoader
	 */
	public String getBasePath() {
		return basePath;
	}

	@Override
	public String getConfigurationName() {
		return configurationName;
	}

	@Override
	public IbisContext getIbisContext() {
		return ibisContext;
	}

	@Override
	public void setReportLevel(String level) {
		try {
			this.reportLevel = ReportLevel.valueOf(level.toUpperCase());
		}
		catch (IllegalArgumentException e) {
			ConfigurationWarnings.getInstance().add(log, "Invalid reportLevel ["+level+"], using default [ERROR]");
		}
	}

	@Override
	public ReportLevel getReportLevel() {
		return reportLevel;
	}

	/**
	 * Override this method and make it final so nobody can overwrite it.
	 * Implementations of this class should use {@link ClassLoaderBase#getLocalResource(String)}
	 */
	@Override
	public final URL getResource(String name) {
		//It will and should never find files that are in the META-INF folder in this classloader, so always traverse to it's parent
		if(name.startsWith("META-INF/")) {
			return getParent().getResource(name);
		}

		return getResource(name, true);
	}

	/**
	 * @param name of the file to search for in the current local classpath
	 * @return the URL of the file if found in the ClassLoader or <code>NULL</code> when the file cannot be found
	 */
	public abstract URL getLocalResource(String name);

	/**
	 * In case of the {@link #getResources(String)} we only want the local paths and not the parent path
	 * @param name of the file to retrieve
	 * @param useParent only use local classpath or also traverse down the classpath
	 * @return the URL of the file if found in the ClassLoader or <code>NULL</code>
	 */
	public URL getResource(String name, boolean useParent) {
		URL url = null;
		if(getBasePath() != null) {
			url = getLocalResource(getBasePath() + name);
			if(log.isTraceEnabled() && url != null) log.trace("["+getConfigurationName()+"] retrieved file ["+getBasePath()+name+"]");
		}

		//URL without basepath cannot be found, follow parent hierarchy
		if(url == null && useParent) {
			url = getParent().getResource(name);
			if(log.isTraceEnabled() && url != null) log.trace("["+getConfigurationName()+"] retrieved file ["+name+"] from parent");
		}

		return url;
	}

	@Override
	public final Enumeration<URL> getResources(String name) throws IOException {
		//It will and should never find files that are in the META-INF folder in this classloader, so always traverse to it's parent
		if(name.startsWith("META-INF/")) {
			return getParent().getResources(name);
		}

		Vector<URL> urls = new Vector<URL>();

		//Search for the file in the local classpath only
		URL localResource = getResource(name, false);
		if (localResource != null) {
			urls.add(localResource);
		}

		//Add all files found in the classpath's parent
		urls.addAll(Collections.list(getParent().getResources(name)));

		if(log.isTraceEnabled()) log.trace("["+getConfigurationName()+"] retrieved files ["+name+"] found urls " + urls);

		return urls.elements();
	}

	@Override
	public void reload() throws ConfigurationException {
		if (getParent() instanceof ReloadAware) {
			((ReloadAware)getParent()).reload();
		}

		AppConstants.removeInstance(this);
	}
}
