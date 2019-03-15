package nl.nn.adapterframework.configuration.classloaders;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;

public interface IClassLoader extends ReloadAware {
	public enum ReportLevel {
		DEBUG, INFO, WARN, ERROR;
	}

	public void configure(IbisContext ibisContext, String configurationName) throws ConfigurationException;

	public IbisContext getIbisContext();
	public String getConfigurationName();
	public String getConfigurationFile();

	public void setReportLevel(String level);
	public ReportLevel getReportLevel();
}
