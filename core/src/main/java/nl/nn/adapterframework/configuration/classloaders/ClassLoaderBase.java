package nl.nn.adapterframework.configuration.classloaders;

import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.util.LogUtil;

public class ClassLoaderBase extends ClassLoader implements IClassLoader, ReloadAware {
	private IbisContext ibisContext = null;
	private String configurationName = null;

	protected Logger log = LogUtil.getLogger(this);
	private ReportLevel reportLevel = ReportLevel.ERROR;

	public ClassLoaderBase() {
		super(Thread.currentThread().getContextClassLoader());
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
