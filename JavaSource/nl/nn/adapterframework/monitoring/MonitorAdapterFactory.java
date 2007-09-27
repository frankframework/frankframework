/*
 * $Log: MonitorAdapterFactory.java,v $
 * Revision 1.1  2007-09-27 12:55:41  europe\L190409
 * introduction of monitoring
 *
 */
package nl.nn.adapterframework.monitoring;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Factory to provide a (configurable) MonitorAdapter.
 * 
 * @author  Gerrit van Brakel
 * @since   4.7  
 * @version Id
 */
public class MonitorAdapterFactory {
	protected static Logger log = LogUtil.getLogger(MonitorAdapterFactory.class);
	
	public static final String MONITOR_ADAPTER_CLASS_KEY="monitor.adapter";
	public static final String GALM_KEY="galm";
	
	private static IMonitorAdapter monitorAdapter=null;
	private static boolean configured=false;
	
	public static IMonitorAdapter getMonitorAdapter() throws ConfigurationException {
		if (!configured) {
			configured=true;
			if ("true".equals(AppConstants.getInstance().getResolvedProperty(GALM_KEY))) {
				log.info("instantiating GALM Monitor Adapter");
				monitorAdapter = new GalmMonitorAdapter();
			} else {
				String monitorAdapterClass=AppConstants.getInstance().getProperty(MONITOR_ADAPTER_CLASS_KEY);
				if (StringUtils.isNotEmpty(monitorAdapterClass)) {
					log.info("intantiating MonitorAdapter ["+monitorAdapterClass+"]");

					try {
						monitorAdapter=(IMonitorAdapter)ClassUtils.newInstance(monitorAdapterClass);
					} catch (Exception e) {
						log.error("could not load MonitorAdapter class ["+monitorAdapterClass+"]",e);
					}
					if (monitorAdapter==null) {
						log.warn("as no MonitorAdapter class could be loaded from ["+monitorAdapterClass+"] the default will be used ["+DummyMonitorAdapter.class.getName()+"]");
						monitorAdapter = new DummyMonitorAdapter();
					}
				} else {
					log.info("no MonitorAdapter class configured in ["+MONITOR_ADAPTER_CLASS_KEY+"]");
				}
			}
		}
		return monitorAdapter;
	}

}
