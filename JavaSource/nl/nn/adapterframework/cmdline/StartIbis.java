/*
 * Created on 26-apr-04
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.nn.adapterframework.cmdline;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationDigester;
import nl.nn.adapterframework.util.ClassUtils;
import org.apache.log4j.Logger;
import javax.management.ObjectInstance;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

/**
 *  
 * Configures a queue connection and receiver and starts listening to it.
 * @author     Johan Verrips
 * created    14 februari 2003
 */
public class StartIbis {
	public static final String version =
		"$Id: StartIbis.java,v 1.4 2005-01-04 12:53:12 L190409 Exp $";
	static final String DFLT_DIGESTER_RULES = "digester-rules.xml";
	static final String DFLT_CONFIGURATION = "Configuration.xml";
	static final String DFLT_AUTOSTART = "TRUE";
	private  Logger log = Logger.getLogger(this.getClass());

	/**
	 * 
	 */
	public StartIbis() {
		super();
	}
	public static void main(String[] args) {
		StartIbis si=new StartIbis();
		si.initConfig(DFLT_CONFIGURATION, DFLT_DIGESTER_RULES, DFLT_AUTOSTART);

	}
	public boolean initConfig(
		String configurationFile,
		String digesterRulesFile,
		String autoStart) {
		Configuration config = null;
		ConfigurationDigester cd = new ConfigurationDigester();
		//Start MBean server
		MBeanServer server=MBeanServerFactory.createMBeanServer();
		try {
		ObjectInstance html = server.createMBean("com.sun.jdmk.comm.HtmlAdaptorServer", 
		null);
            
		server.invoke(html.getObjectName(), "start", new Object[0], new String[0]);
		} catch (Exception e) {
			log.error("Error with jmx:",e);
		}
		log.info("MBean server up and running. Monitor your application by pointing your browser to http://localhost:8082");
		if (null == configurationFile)
			configurationFile = DFLT_CONFIGURATION;
		if (null == digesterRulesFile)
			digesterRulesFile = DFLT_DIGESTER_RULES;
		if (null == autoStart)
			autoStart = DFLT_AUTOSTART;
		try {
			config =
				cd.unmarshalConfiguration(
					ClassUtils.getResourceURL(cd, digesterRulesFile),
					ClassUtils.getResourceURL(cd, configurationFile));
		} catch (Throwable e) {
			log.error("Error occured unmarshalling configuration:", e);
		}
		// if configuration did not succeed, log and return.
		if (null == config) {
			log.error(
				"Error occured unmarshalling configuration. See previous messages.");
			return false;
		}

		if (autoStart.equalsIgnoreCase("TRUE")) {
			log.info("Starting configuration");
			config.startAdapters();
		}
		log.info(
			"****" + "********** ConfigurationServlet complete **************");
		return true;
	}

}
