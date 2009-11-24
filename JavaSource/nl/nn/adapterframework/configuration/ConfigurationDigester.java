/*
 * $Log: ConfigurationDigester.java,v $
 * Revision 1.30  2009-11-24 08:32:00  m168309
 * excluded ${property.key} values from default value check
 *
 * Revision 1.29  2009/08/26 15:25:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for configurable statisticsHandlers
 *
 * Revision 1.28  2009/07/03 06:32:14  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * facilty to stub the configuration for testtool
 *
 * Revision 1.27  2009/05/13 08:18:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved monitoring: triggers can now be filtered multiselectable on adapterlevel
 *
 * Revision 1.26  2009/03/13 14:21:02  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * add locker to attributeChecker-rules
 *
 * Revision 1.25  2008/07/14 17:02:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for monitoring
 *
 * Revision 1.24  2008/05/21 08:37:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added elementname to errormessage
 *
 * Revision 1.23  2008/05/15 14:30:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * store configuration exception that is caught
 *
 * Revision 1.22  2008/05/14 11:45:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * optional perform validation of configuration using xsd
 *
 * Revision 1.21  2008/02/13 12:52:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.20  2007/11/22 08:24:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed include() code
 *
 * Revision 1.19  2007/10/24 08:28:03  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Allow 'include' to work when no digester-rules.xml is specified, and use default configuration for 'stackTop' when stackTop = null
 *
 * Revision 1.18  2007/10/16 13:18:30  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Make method 'digestConfiguration' public again so that it can be used from test cases.
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

import java.io.StringReader;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.transform.Transformer;

import nl.nn.adapterframework.monitoring.MonitorManager;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.commons.digester.xmlrules.FromXmlRuleSet;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

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
	public static final String version = "$RCSfile: ConfigurationDigester.java,v $ $Revision: 1.30 $ $Date: 2009-11-24 08:32:00 $";
    protected static Logger log = LogUtil.getLogger(ConfigurationDigester.class);
	private ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();

	private static final String CONFIGURATION_FILE_DEFAULT  = "Configuration.xml";
	private static final String DIGESTER_RULES_DEFAULT      = "digester-rules.xml";
	
	private static final String CONFIGURATION_VALIDATION_KEY = "validate.configuration";
	private static final String CONFIGURATION_STUB4TESTTOOL_KEY = "stub4testtool.configuration";

	private static String stub4testtool_xslt = "/xml/xsl/stub4testtool.xsl";
	private static String attributesGetter_xslt = "/xml/xsl/AttributesGetter.xsl";

	private String configurationFile=null;
	private String digesterRulesFile=DIGESTER_RULES_DEFAULT;
    
    private BeanFactory beanFactory;
    
	private Configuration configuration;

    /**
     * This method is runtime implemented by Spring Framework to
     * return a Digester instance created from the Spring Context
     * 
     */
    abstract protected Digester createDigester();

	public class XmlErrorHandler implements ErrorHandler  {
		public void warning(SAXParseException exception) throws SAXParseException {
			log.error(exception);
			throw(exception);
		}
		public void error(SAXParseException exception) throws SAXParseException {
			log.error(exception);
			throw(exception);
		}
		public void fatalError(SAXParseException exception) throws SAXParseException {
			log.error(exception);
			throw(exception);
		}
	}
    
    public void digestConfiguration(Object stackTop, URL digesterRulesURL, URL configurationFileURL) throws ConfigurationException {
		
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
			digester.addRule("*/locker", attributeChecker);
			digester.addRule("*/statistics", attributeChecker);
			digester.addRule("*/handler", attributeChecker);
			MonitorManager.getInstance().setDigesterRules(digester);

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
				
			boolean validation=AppConstants.getInstance().getBoolean(CONFIGURATION_VALIDATION_KEY,false);
			if (validation) {
				digester.setValidating(true);
				digester.setNamespaceAware(true);
				digester.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
				digester.setProperty("http://java.sun.com/xml/jaxp/properties/schemaSource","AdapterFramework.xsd");
				XmlErrorHandler xeh = new XmlErrorHandler();
				digester.setErrorHandler(xeh);
			}

			fillConfigWarnDefaultValueExceptions(configurationFileURL);

			boolean stub4testtool=AppConstants.getInstance().getBoolean(CONFIGURATION_STUB4TESTTOOL_KEY,false);
			if (stub4testtool) {
				URL xsltSource = ClassUtils.getResourceURL(this, stub4testtool_xslt);
				if (xsltSource == null) {
					throw new ConfigurationException("cannot find resource ["+stub4testtool_xslt+"]");
				}
				Transformer transformer = XmlUtils.createTransformer(xsltSource);
				String lineSeparator=SystemUtils.LINE_SEPARATOR;
				if (null==lineSeparator) lineSeparator="\n";
				String configString=Misc.resourceToString(configurationFileURL, lineSeparator, false);
				configString=XmlUtils.identityTransform(configString);
				String stubbedConfigString = XmlUtils.transformXml(transformer, configString);
				digester.parse(new StringReader(stubbedConfigString));
			} else {
				digester.parse(configurationFileURL);
			}
		} catch (Throwable t) {
			// wrap exception to be sure it gets rendered via the IbisException-renderer
			String currentElementName=digester.getCurrentElementName();
			ConfigurationException e = new ConfigurationException("error during unmarshalling configuration from file ["+configurationFileURL +
			"] with digester-rules-file ["+digesterRulesURL+"] in element ["+currentElementName+"]", t);
			if (configuration!=null) {
				configuration.setConfigurationException(e);
			}
			log.error(e);
			throw (e);
		}
	}

	public void fillConfigWarnDefaultValueExceptions(URL configurationFileURL) throws Exception {
		URL xsltSource = ClassUtils.getResourceURL(this, attributesGetter_xslt);
		if (xsltSource == null) {
			throw new ConfigurationException("cannot find resource ["+stub4testtool_xslt+"]");
		}
		Transformer transformer = XmlUtils.createTransformer(xsltSource);
		String lineSeparator=SystemUtils.LINE_SEPARATOR;
		if (null==lineSeparator) lineSeparator="\n";
		String configString=Misc.resourceToString(configurationFileURL, lineSeparator, false);
		configString=XmlUtils.identityTransform(configString);
		String attributes = XmlUtils.transformXml(transformer, configString);
		Element attributesElement = XmlUtils.buildElement(attributes);
		Collection attributeElements =	XmlUtils.getChildTags(attributesElement, "attribute");
		Iterator iter = attributeElements.iterator();
		while (iter.hasNext()) {
			Element attributeElement = (Element) iter.next();
			Element valueElement = XmlUtils.getFirstChildTag(attributeElement, "value");
			String value = XmlUtils.getStringValue(valueElement);
			if (value.startsWith("${") && value.endsWith("}")) {
				Element keyElement = XmlUtils.getFirstChildTag(attributeElement, "key");
				String key = XmlUtils.getStringValue(keyElement);
				Element elementElement = XmlUtils.getFirstChildTag(attributeElement, "element");
				String element = XmlUtils.getStringValue(elementElement);
				Element nameElement = XmlUtils.getFirstChildTag(attributeElement, "name");
				String name = XmlUtils.getStringValue(nameElement);
				String mergedKey = element + "/" + (name==null?"":name) + "/" + key;
				configWarnings.addDefaultValueExceptions(mergedKey);
			}
		}
	}

	public void include(Object stackTop) throws ConfigurationException {
		URL includedConfigUrl = ClassUtils.getResourceURL(this, getConfigurationFile());
		if (includedConfigUrl == null) {
			throw new ConfigurationException("cannot find resource ["+getConfigurationFile()+"] to include");
		}
		URL digesterRules = ClassUtils.getResourceURL(this, getDigesterRules());
		if (stackTop == null) {
			stackTop = this.configuration;
		}
		digestConfiguration(stackTop, digesterRules, includedConfigUrl);
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
		MonitorManager.getInstance().configure(configuration);

        log.info("************** Configuration completed **************");
		return configuration;
    }
    
    
	public void setConfigurationFile(String string) {
		configurationFile = string;
	}
	public String getConfigurationFile() {
		return configurationFile;
	}


	public void setDigesterRules(String string) {
		digesterRulesFile = string;
	}
	public String getDigesterRules() {
		return digesterRulesFile;
	}


    public void setBeanFactory(BeanFactory factory) {
        beanFactory = factory;
        AbstractSpringPoweredDigesterFactory.factory = (ListableBeanFactory) beanFactory;
    }
	public BeanFactory getBeanFactory() {
		return beanFactory;
	}

    /**
     * This method is used from the Spring configuration file. The Configuration is available as a Spring Bean.
     * @param configuration
     */
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
    public Configuration getConfiguration() {
        return configuration;
    }
}
