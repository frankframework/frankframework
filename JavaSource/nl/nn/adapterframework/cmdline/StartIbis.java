/*
 * Created on 26-apr-04
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.nn.adapterframework.cmdline;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.HelpFormatter;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationDigester;
import nl.nn.adapterframework.util.ClassUtils;
import org.apache.log4j.Logger;



/**
 *  
 * Configures a queue connection and receiver and starts listening to it.
 * @author     Johan Verrips
 * @created    14 februari 2003
 */
public class StartIbis {
	public static final String version="$Id: StartIbis.java,v 1.1 2004-04-26 07:08:54 NNVZNL01#L180564 Exp $";
	static final String DFLT_DIGESTER_RULES = "digester-rules.xml";
	static final String DFLT_CONFIGURATION = "Configuration.xml";
	static final String DFLT_AUTOSTART = "TRUE";
	private static Logger log=Logger.getLogger("StartIbis");


	/**
	 * 
	 */
	public StartIbis() {
		super();
		// TODO Auto-generated constructor stub
	}
	public static void main(String[] args) {
		initConfig(DFLT_CONFIGURATION, DFLT_DIGESTER_RULES, DFLT_AUTOSTART);
			
	}
	public static  boolean initConfig(
			   String configurationFile,
			   String digesterRulesFile,
			   String autoStart) {
		Configuration config=null;
		ConfigurationDigester cd=new ConfigurationDigester();
		if (null == configurationFile)
		  configurationFile = DFLT_CONFIGURATION;
	  if (null == digesterRulesFile)
		  digesterRulesFile = DFLT_DIGESTER_RULES;
	  if (null == autoStart)
		  autoStart = DFLT_AUTOSTART;
	try {
		  config =
				  cd.unmarshalConfiguration(
						  ClassUtils.getResourceURL(null, digesterRulesFile),
						  ClassUtils.getResourceURL(null, configurationFile));
	  } catch (Throwable e) {
		  log.error("Error occured unmarshalling configuration:", e);
	  }
	  // if configuration did not succeed, log and return.
	  if (null == config) {
		  log.error("Error occured unmarshalling configuration. See previous messages.");
		  return false;
	  }

	  if (autoStart.equalsIgnoreCase("TRUE")) {
		  log.info("Starting configuration");
		  config.startAdapters();
	  }
	  log.info("****" +"********** ConfigurationServlet complete **************");
	  return true;
}

}
