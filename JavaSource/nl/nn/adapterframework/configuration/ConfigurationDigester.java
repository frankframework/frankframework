/*
 * $Log: ConfigurationDigester.java,v $
 * Revision 1.15.4.3  2007-10-10 14:30:40  europe\L190409
 * synchronize with HEAD (4.8-alpha1)
 *
 * Revision 1.17  2007/10/10 09:23:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * spring enabled version
 *
 * Revision 1.16  2007/09/19 13:06:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * split digest in url and string
 *
 * Revision 1.15  2007/05/21 12:18:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * add messageLog to attributeChecker-rules
 *
 * Revision 1.14  2007/05/11 09:37:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attributeCheckingRule
 *
 * Revision 1.13  2007/02/12 13:38:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.12  2006/01/05 13:52:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved error-handling
 *
 * Revision 1.11  2005/07/05 10:54:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * created 'include' facility
 *
 * Revision 1.10  2005/06/13 12:47:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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

import java.net.URL;

import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.commons.digester.xmlrules.FromXmlRuleSet;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;

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
abstract public class ConfigurationDigester implements BeanFactoryAware {
	public static final String version = "$RCSfile: ConfigurationDigester.java,v $ $Revision: 1.15.4.3 $ $Date: 2007-10-10 14:30:40 $";
    protected static Logger log = LogUtil.getLogger(ConfigurationDigester.class);

	private static final String CONFIGURATION_FILE_DEFAULT  = "Configuration.xml";
	private static final String DIGESTER_RULES_DEFAULT      = "digester-rules.xml";

	private String configurationFile=null;
	private String digesterRulesFile=DIGESTER_RULES_DEFAULT;
    
    private BeanFactory beanFactory;
    
	private Configuration configuration;
/*	
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
	
*/	
    /**
     * This method is runtime implemented by Spring Framework to
     * return a Digester instance created from the Spring Context
     * 
     */
    abstract protected Digester createDigester();
    
    protected void digestConfiguration(Object stackTop, URL digesterRulesURL, URL configurationFileURL) throws ConfigurationException {
		
		if (digesterRulesURL==null) {
			digesterRulesURL = ClassUtils.getResourceURL(stackTop, DIGESTER_RULES_DEFAULT);
		}
        if (configurationFileURL == null) {
            configurationFileURL = ClassUtils.getResourceURL(stackTop, CONFIGURATION_FILE_DEFAULT);
        }
		Digester digester = createDigester();
		//digester.setUseContextClassLoader(true);

		//	set the entity resolver to load entity references from the classpath
		//digester.setEntityResolver(new ClassPathEntityResolver());
		
		// push config on the stack
		digester.push(stackTop);
		try {
			// digester-rules.xml bevat de rules voor het digesten
            
			FromXmlRuleSet ruleSet = new FromXmlRuleSet(digesterRulesURL);

			digester.addRuleSet(ruleSet);
			
			Rule attributeChecker=new AttributeCheckingRule(); 
			
			digester.addRule("*/jmsRealms", attributeChecker);
			digester.addRule("*/jmsRealm", attributeChecker);
			digester.addRule("*/sapSystem", attributeChecker);
			digester.addRule("*/adapter", attributeChecker);
			digester.addRule("*/pipeline", attributeChecker);
			digester.addRule("*/errorMessageFormatter", attributeChecker);
			digester.addRule("*/receiver", attributeChecker);
			digester.addRule("*/sender", attributeChecker);
			digester.addRule("*/listener", attributeChecker);
			digester.addRule("*/postboxSender", attributeChecker);
			digester.addRule("*/postboxListener", attributeChecker);
			digester.addRule("*/errorSender", attributeChecker);
			digester.addRule("*/messageLog", attributeChecker);
			digester.addRule("*/inProcessStorage", attributeChecker);
			digester.addRule("*/errorStorage", attributeChecker);
			digester.addRule("*/pipe", attributeChecker);
			digester.addRule("*/forward", attributeChecker);
			digester.addRule("*/child", attributeChecker);
			digester.addRule("*/param", attributeChecker);
			digester.addRule("*/pipeline/exits/exit", attributeChecker);
			digester.addRule("*/scheduler/job", attributeChecker);

// Resolving variables is now done by Digester
//			// ensure that lines are seperated, usefulls when a parsing error occurs
//			String lineSeperator=SystemUtils.LINE_SEPARATOR;
//			if (null==lineSeperator) lineSeperator="\n";
//			String configString=Misc.resourceToString(configurationFileURL, lineSeperator, false);
//			configString=XmlUtils.identityTransform(configString);
//			log.debug(configString);
//			//Resolve any variables
//			String resolvedConfig=StringResolver.substVars(configString, AppConstants.getInstance());
//
//			Variant var=new Variant(resolvedConfig);
//			InputSource is=var.asXmlInputSource();
				
			digester.parse(configurationFileURL);

		} catch (Throwable t) {
			// wrap exception to be sure it gets rendered via the IbisException-renderer
			ConfigurationException e = new ConfigurationException("error during unmarshalling configuration from file ["+configurationFileURL +
			"] with digester-rules-file ["+digesterRulesURL+"]", t);
			log.error(e);
			throw (e);
		}
	}

	public void include(Object stackTop) throws ConfigurationException {
		URL configuration = ClassUtils.getResourceURL(this, getConfigurationFile());
		if (configuration == null) {
			throw new ConfigurationException("cannot find resource ["+getConfigurationFile()+"] to include");
		}
		URL digesterRules = ClassUtils.getResourceURL(this, getDigesterRules());
		if (digesterRules == null) {
			throw new ConfigurationException("cannot find resource ["+getDigesterRules()+"] use as digesterrules to include");
		}
		digestConfiguration(stackTop, digesterRules, configuration);
	}
	
	public Configuration unmarshalConfiguration() throws ConfigurationException
	{
		return unmarshalConfiguration(getDigesterRules(), getConfigurationFile());
	}
	
    public Configuration unmarshalConfiguration(String digesterRulesFile, String configurationFile) throws ConfigurationException
    {
		return unmarshalConfiguration(ClassUtils.getResourceURL(this, digesterRulesFile), ClassUtils.getResourceURL(this, configurationFile));
    }
    
    public Configuration unmarshalConfiguration(URL digesterRulesURL, URL configurationFileURL) throws ConfigurationException{
        configuration.setDigesterRulesURL(digesterRulesURL);
        configuration.setConfigurationURL(configurationFileURL);
		digestConfiguration(configuration,digesterRulesURL,configurationFileURL);

        log.info("************** Configuration completed **************");
		return configuration;
    }
    
    
	public String getConfigurationFile() {
		return configurationFile;
	}

	public String getDigesterRules() {
		return digesterRulesFile;
	}

	public void setConfigurationFile(String string) {
		configurationFile = string;
	}

	public void setDigesterRules(String string) {
		digesterRulesFile = string;
	}

    /**
     * @return
     */
    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    /**
     * @param factory
     */
    public void setBeanFactory(BeanFactory factory) {
        beanFactory = factory;
        AbstractSpringPoweredDigesterFactory.factory = (ListableBeanFactory) beanFactory;
    }

    /**
     * @param configuration
     */
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
    public Configuration getConfiguration() {
        return configuration;
    }
}
