package nl.nn.adapterframework.configuration;

import nl.nn.adapterframework.util.StringResolver;
import nl.nn.adapterframework.util.AppConstants;
import org.xml.sax.InputSource;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.Variant;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.xmlrules.FromXmlRuleSet;
import org.apache.log4j.Logger;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.SystemUtils;

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
 * @author Johan Verrips
 * @see Configuration
 *
 */
public class ConfigurationDigester {
	public static final String version="$Id: ConfigurationDigester.java,v 1.1 2004-02-04 08:36:16 a1909356#db2admin Exp $";
    static Digester digester;
    protected Logger log = Logger.getLogger(this.getClass());
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
      
    config = cd.unmarshalConfiguration(digester_rules_file, configuration_file);

    if (null == config) {
        System.out.println("Errors occured during configuration");
        return;
    } else {
        System.out.println("       Object List:");
        if (null!=config) 
        config.listObjects();

//        System.out.println(
//            config.getRegisteredAdapter("Y01 Broker").toString());
        System.out.println("------------------------------------------");
        System.out.println("       start adapters");
    }

    if (null!=config)config.startAdapters();

}
    public Configuration unmarshalConfiguration(String digesterRulesFile, String configurationFile)
    {
        Configuration config = new Configuration();

        digester = new Digester();

  		config = unmarshalConfiguration(ClassUtils.getResourceURL(this, digesterRulesFile), ClassUtils.getResourceURL(this, configurationFile));
 
		return config;
    }
    public Configuration unmarshalConfiguration(URL digesterRulesURL, URL configurationFileURL){
        Configuration config = new Configuration(digesterRulesURL, configurationFileURL);

        digester = new Digester();
		digester.setUseContextClassLoader(true);
		
        // push config on the stack
        digester.push(config);

        try {
            // digester-rules.xml bevat de rules voor het digesten
            
            FromXmlRuleSet ruleSet = new FromXmlRuleSet(digesterRulesURL);

            digester.addRuleSet(ruleSet);
            // ensure that lines are seperated, usefulls when a parsing error occurs
			String lineSeperator=SystemUtils.LINE_SEPARATOR;
	        if (null==lineSeperator) lineSeperator="\n";
			String configString=Misc.resourceToString(configurationFileURL, lineSeperator, false);
			
			//Resolve any variables
			String resolvedConfig=StringResolver.substVars(configString, AppConstants.getInstance());

			Variant var=new Variant(resolvedConfig);
			InputSource is=var.asXmlInputSource();
				
            digester.parse(is);

        } catch (Throwable e) {
            log.error("error during unmarshalling configuration from file: "+configurationFileURL +
	            " with digester-rules-file:"+digesterRulesURL+ToStringBuilder.reflectionToString(e), e);
            return null;
        }
        log.info("************** Configuration completed **************");
		return config;
    }
}
