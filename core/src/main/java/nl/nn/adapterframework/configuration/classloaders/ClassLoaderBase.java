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

import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Abstract base class for for IBIS Configuration ClassLoaders.
 * 
 * @author Niels Meijer
 *
 */
public abstract class ClassLoaderBase extends ClassLoader implements IConfigurationClassLoader, ReloadAware {
	private IbisContext ibisContext = null;
	private String configurationName = null;

	protected Logger log = LogUtil.getLogger(this);
	private ReportLevel reportLevel = ReportLevel.ERROR;

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
	}

	public String getConfigurationName() {
		return configurationName;
	}

	public IbisContext getIbisContext() {
		return ibisContext;
	}

	public String getConfigurationFile() {
		return ibisContext.getConfigurationFile(getConfigurationName());
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

	@Override
	public void reload() throws ConfigurationException {
		if (getParent() instanceof ReloadAware) {
			((ReloadAware)getParent()).reload();
		}
	}
}
