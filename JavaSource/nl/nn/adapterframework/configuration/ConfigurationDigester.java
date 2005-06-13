/*
 * $Log: ConfigurationDigester.java,v $
 * Revision 1.10  2005-06-13 12:47:15  europe\L190409
 * rework to prepare for 'include'-feature
 *
 * Revision 1.9  2005/02/24 10:48:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added display of error message at reinitialize
 *
 * Revision 1.8  2004/10/14 15:32:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging
 *
 * Revision 1.7  2004/06/16 13:04:28  Johan Verrips <johan.verrips@ibissource.org>
 * improved  the ClassPathEntityResolver functionality in combination with
 * resolving variables
 *
 * Revision 1.6  2004/06/16 06:57:00  Johan Verrips <johan.verrips@ibissource.org>
 * Added the ClassPathEntityResolver to resolve entities within the classpath
 *
 * Revision 1.5  2004/03/30 07:30:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.4  2004/03/26 10:42:50  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 */
package nl.nn.adapterframework.configuration;

import nl.nn.adapterframework.util.StringResolver;
import nl.nn.adapterframework.util.AppConstants;
import org.xml.sax.InputSource;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.Variant;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.xmlrules.FromXmlRuleSet;
import org.apache.log4j.Logger;
import org.apache.commons.lang.SystemUtils;
import nl.nn.adapterframework.util.ClassPathEntityResolver;

import java.net.URL;

/**
 * The configurationDigester reads the configuration.xml and the digester rules
 * in XML format and factors a Configuration.
 *
 * Default JNDI properties may be specified on the commandline, e.g. <br/>
 * <p>
 * -Djava.naming.factory.initial=org.exolab.jms.jndi.mipc.IpcJndiInitialContextFactory<br/>
 * -Djava.naming.provider.url=tcp://localhost:3035/<br/>
 * </p>
 * <p>Since 4.0.1, the configuration.xml is first resolved using the {@link nl.nn.adapterframework.util.StringResolver resolver},
 * with tries to resolve ${variable} with the {@link nl.nn.adapterframework.util.AppConstants AppConstants}, so that
 * both the values from the property files as the environment setting are available.<p>
 * <p>Since 4.1.1 the configuration.xml is parsed with a entityresolver that uses the classpath, which
 * means that you may specify entities that will be resolved during parsing.
 * </p>
 * Example:
 * <code><pre>
 * &lt;?xml version="1.0"?&gt;
&lt;!DOCTYPE IOS-Adaptering
[
&lt;!ENTITY Y04 SYSTEM "./ConfigurationY04.xml"&gt;
]&gt;

&lt;IOS-Adaptering configurationName="AdapterFramework (v4.0) configuratie voor GIJuice"&gt;
	
&Y04;

&lt;/IOS-Adaptering&gt;	
</pre></code>
 * @version Id
 * @author Johan Verrips
 * @see Configuration
 */
public class ConfigurationDigester {
	public static final String version = "$RCSfile: ConfigurationDigester.java,v $ $Revision: 1.10 $ $Date: 2005-06-13 12:47:15 $";
    protected static Logger log = Logger.getLogger(ConfigurationDigester.class);

	private static final String CONFIGURATION_FILE_DEFAULT  = "Configuration.xml";
	private static final String DIGESTER_RULES_DEFAULT      = "digester-rules.xml";

	public static void main(String args[]) {
	    String configuration_file = CONFIGURATION_FILE_DEFAULT;
	    String digester_rules_file = DIGESTER_RULES_DEFAULT;
	
	    Configuration config = null;
	    ConfigurationDigester cd = new ConfigurationDigester();
	
	    if (args.length>=1)
	      configuration_file = args[0];
	    if (args.length>=2)
	      digester_rules_file = args[1];
	      
	    try {
			config = cd.unmarshalConfiguration(digester_rules_file, configuration_file);
		} catch (ConfigurationException e) {
			System.out.println(e.getMessage());
		}
	
	    if (null == config) {
	        System.out.println("Errors occured during configuration");
	        return;
	    } else {
	        System.out.println("       Object List:");
	        if (null!=config) 
	        config.listObjects();
	
	        System.out.println("------------------------------------------");
	        System.out.println("       start adapters");
	    }
	
	    if (null!=config)config.startAdapters();
	
	}
	
	public static void digestConfiguration(Object stackTop, URL digesterRulesURL, URL configurationFileURL) throws ConfigurationException {
		
		if (digesterRulesURL==null) {
			digesterRulesURL = ClassUtils.getResourceURL(stackTop, DIGESTER_RULES_DEFAULT);
		}
		
		Digester digester = new Digester();
		digester.setUseContextClassLoader(true);

		//	set the entity resolver to load entity references from the classpath
		digester.setEntityResolver(new ClassPathEntityResolver());
		
		// push config on the stack
		digester.push(stackTop);
		try {
			// digester-rules.xml bevat de rules voor het digesten
            
			FromXmlRuleSet ruleSet = new FromXmlRuleSet(digesterRulesURL);

			digester.addRuleSet(ruleSet);
			// ensure that lines are seperated, usefulls when a parsing error occurs
			String lineSeperator=SystemUtils.LINE_SEPARATOR;
			if (null==lineSeperator) lineSeperator="\n";
			String configString=Misc.resourceToString(configurationFileURL, lineSeperator, false);
			configString=XmlUtils.identityTransform(configString);
			log.debug(configString);
			//Resolve any variables
			String resolvedConfig=StringResolver.substVars(configString, AppConstants.getInstance());

			Variant var=new Variant(resolvedConfig);
			InputSource is=var.asXmlInputSource();
				
			digester.parse(is);

		} catch (Throwable t) {
			// wrap exception to be sure it gets rendered via the IbisException-renderer
			ConfigurationException e = new ConfigurationException("error during unmarshalling configuration from file ["+configurationFileURL +
			"] with digester-rules-file ["+digesterRulesURL+"]", t);
			log.error(e);
			throw (e);
		}
	}
	
    public Configuration unmarshalConfiguration(String digesterRulesFile, String configurationFile) throws ConfigurationException
    {
        Configuration config = unmarshalConfiguration(ClassUtils.getResourceURL(this, digesterRulesFile), ClassUtils.getResourceURL(this, configurationFile));
 
		return config;
    }
    
    public Configuration unmarshalConfiguration(URL digesterRulesURL, URL configurationFileURL) throws ConfigurationException{
        Configuration config = new Configuration(digesterRulesURL, configurationFileURL);
        
		digestConfiguration(config,digesterRulesURL,configurationFileURL);

        log.info("************** Configuration completed **************");
		return config;
    }
}
