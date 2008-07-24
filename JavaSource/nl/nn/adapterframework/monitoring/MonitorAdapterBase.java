/*
 * $Log: MonitorAdapterBase.java,v $
 * Revision 1.1  2008-07-24 12:34:01  europe\L190409
 * rework
 *
 */
package nl.nn.adapterframework.monitoring;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public abstract class MonitorAdapterBase implements IMonitorAdapter {
	protected Logger log = LogUtil.getLogger(this);

	private String name;

	public MonitorAdapterBase() {
		super();
		log.debug("creating Destination ["+ClassUtils.nameOf(this)+"]");
	}

	
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getName())) {
			setName(ClassUtils.nameOf(this));
		}
	}


	public void setName(String string) {
		name = string;
	}
	public String getName() {
		return name;
	}

	public void register(Object x) {
		MonitorManager.getInstance().registerDestination(this);
	}

}
