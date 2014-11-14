/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.configuration;

import nl.nn.adapterframework.monitoring.MonitorManager;
import nl.nn.adapterframework.util.*;

import javax.xml.transform.Transformer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.commons.digester.xmlrules.FromXmlRuleSet;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.xml.sax.*;

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
 * @author Johan Verrips
 * @see Configuration
 */
abstract public class ConfigurationDigester {
    private static final Logger LOG = LogUtil.getLogger(ConfigurationDigester.class);
	private ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();

	//private static final String CONFIGURATION_FILE_DEFAULT  = "Configuration.xml";
	private static final String DIGESTER_RULES_DEFAULT      = "digester-rules.xml";

	private static final String CONFIGURATION_VALIDATION_KEY = "validate.configuration";
	private static final String CONFIGURATION_STUB4TESTTOOL_KEY = "stub4testtool.configuration";

	private static final String stub4testtool_xslt = "/xml/xsl/stub4testtool.xsl";
	private static final String attributesGetter_xslt = "/xml/xsl/AttributesGetter.xsl";

	private String configurationFile = null;
	private String digesterRulesFile = DIGESTER_RULES_DEFAULT;

	private Configuration configuration;


	String lastResolvedEntity = null;

    /**
     * This method is runtime implemented by Spring Framework to
     * return a Digester instance created from the Spring Context
     *
     */
    abstract protected Digester createDigester();

	private class XmlErrorHandler implements ErrorHandler  {
		public void warning(SAXParseException exception) throws SAXParseException {
			LOG.error(exception);
			throw(exception);
		}
		public void error(SAXParseException exception) throws SAXParseException {
			LOG.error(exception);
			throw(exception);
		}
		public void fatalError(SAXParseException exception) throws SAXParseException {
			LOG.error(exception);
			throw(exception);
		}
	}

	private class NameTrackingEntityResolver implements EntityResolver {

		EntityResolver resolver;

		NameTrackingEntityResolver(EntityResolver resolver) {
			super();
			this.resolver = resolver;
		}

		public InputSource resolveEntity(String publicID, String systemID) throws SAXException, IOException {
			if (StringUtils.isNotEmpty(systemID)) {
				lastResolvedEntity = systemID;
			} else {
				lastResolvedEntity = publicID;
			}
			return resolver.resolveEntity(publicID,systemID);
		}
	}

    public Digester getDigester() throws SAXNotSupportedException, SAXNotRecognizedException {
        Digester digester = createDigester();
            //digester.setUseContextClassLoader(true);

            digester.setEntityResolver(new NameTrackingEntityResolver(digester.getEntityResolver()));

            // push config on the stack
            digester.push(this.configuration);
            URL digesterRulesURL = ClassUtils.getResourceURL(this, getDigesterRules());
            //digester.push("URL", configurationFileURL);
        // digester-rules.xml bevat de rules voor het digesten

            FromXmlRuleSet ruleSet = new FromXmlRuleSet(digesterRulesURL);

            digester.addRuleSet(ruleSet);

            Rule attributeChecker = new AttributeCheckingRule();

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
            digester.addRule("*/readerFactory", attributeChecker);
            digester.addRule("*/manager", attributeChecker);
            digester.addRule("*/manager/flow", attributeChecker);
            digester.addRule("*/recordHandler", attributeChecker);
            digester.addRule("*/resultHandler", attributeChecker);
            digester.addRule("*/forward", attributeChecker);
            digester.addRule("*/child", attributeChecker);
            digester.addRule("*/param", attributeChecker);
            digester.addRule("*/pipeline/exits/exit", attributeChecker);
            digester.addRule("*/scheduler/job", attributeChecker);
            digester.addRule("*/locker", attributeChecker);
            digester.addRule("*/statistics", attributeChecker);
            digester.addRule("*/handler", attributeChecker);
            digester.addRule("*/cache", attributeChecker);
            digester.addRule("*/inputValidator", attributeChecker);
            digester.addRule("*/outputValidator", attributeChecker);
            digester.addRule("*/inputWrapper", attributeChecker);
            digester.addRule("*/outputWrapper", attributeChecker);
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

            boolean validation = AppConstants.getInstance().getBoolean(CONFIGURATION_VALIDATION_KEY, false);
            if (validation) {
                digester.setValidating(true);
                digester.setNamespaceAware(true);
                digester.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
                digester.setProperty("http://java.sun.com/xml/jaxp/properties/schemaSource", "AdapterFramework.xsd");
                XmlErrorHandler xeh = new XmlErrorHandler();
                digester.setErrorHandler(xeh);
            }

        return digester;
    }

	private void digestConfiguration(URL configurationFileURL) throws ConfigurationException, SAXNotSupportedException, SAXNotRecognizedException {

        URL digesterRulesURL = ClassUtils.getResourceURL(this, getDigesterRules());
        Object stackTop = this.configuration;

		if (configurationFileURL == null) {
			configurationFileURL = ClassUtils.getResourceURL(stackTop, IbisManager.DFLT_CONFIGURATION);
		}
		if (configurationFileURL == null) {
			configWarnings.add(LOG, "Could not find " + IbisManager.DFLT_CONFIGURATION);
			return;
		}

        Digester digester = getDigester();
        try {
			fillConfigWarnDefaultValueExceptions(configurationFileURL);

			String lineSeparator = SystemUtils.LINE_SEPARATOR;
			if (null == lineSeparator) lineSeparator = "\n";
			String configString = Misc.resourceToString(configurationFileURL, lineSeparator, false);
			configString = XmlUtils.identityTransform(configString);
			configString = StringResolver.substVars(configString, AppConstants.getInstance());

			configString = ConfigurationUtils.getActivatedConfiguration(configString);

			if (ConfigurationUtils.stubConfiguration()) {
				configString = ConfigurationUtils.getStubbedConfiguration(configString);
			}

			saveConfig(configString);			
			digester.parse(new StringReader(configString));
		} catch (Throwable t) {
            // wrap exception to be sure it gets rendered via the IbisException-renderer
			String currentElementName = digester.getCurrentElementName();
            ConfigurationException e = new ConfigurationException("error during unmarshalling configuration from file [" + configurationFileURL +
                "] with digester-rules-file ["+digesterRulesURL+"] in element ["+currentElementName+"]"+(StringUtils.isEmpty(lastResolvedEntity)?"":" last resolved entity ["+lastResolvedEntity+"]"), t);
            if (configuration != null) {
				configuration.setConfigurationException(e);
			}
			LOG.error(e);
			throw e;
		}
	}

	private void saveConfig(String config) {
		String directoryName = AppConstants.getInstance().getResolvedProperty("log.dir");
		String fileName = AppConstants.getInstance().getResolvedProperty("instance.name")+"-config.xml";
		File file = new File(directoryName, fileName);
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(file,false);
			fileWriter.write(config);
		} catch (IOException e) {
			LOG.warn("Could not write configuration to file ["+file.getPath()+"]",e);
		} finally {
			if (fileWriter!=null) {
				try {
					fileWriter.close();
				} catch (Exception e) {
					LOG.warn("Could not close configuration file ["+file.getPath()+"]",e);
				}
			}
		}
	}
	
	private  void fillConfigWarnDefaultValueExceptions(URL configurationFileURL) throws Exception {
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

	private void include() throws ConfigurationException, SAXNotSupportedException, SAXNotRecognizedException {
		URL includedConfigUrl = ClassUtils.getResourceURL(this, getConfigurationFile());
		if (includedConfigUrl == null) {
			throw new ConfigurationException("cannot find resource ["+getConfigurationFile()+"] to include");
		}

		digestConfiguration(includedConfigUrl);
	}

	private Configuration unmarshalConfiguration() throws ConfigurationException, SAXNotSupportedException, SAXNotRecognizedException {
		return unmarshalConfiguration(getDigesterRules(), getConfigurationFile());
	}

    private  Configuration unmarshalConfiguration(String digesterRulesFile, String configurationFile) throws ConfigurationException, SAXNotSupportedException, SAXNotRecognizedException {
		return unmarshalConfiguration(ClassUtils.getResourceURL(this, digesterRulesFile), ClassUtils.getResourceURL(this, configurationFile));
    }

    public Configuration unmarshalConfiguration(URL digesterRulesURL, URL configurationFileURL) throws ConfigurationException, SAXNotSupportedException, SAXNotRecognizedException {
        configuration.setDigesterRulesURL(digesterRulesURL);
        configuration.setConfigurationURL(configurationFileURL);
		digestConfiguration(configurationFileURL);
		MonitorManager.getInstance().configure(configuration);

        LOG.info("************** Configuration completed **************");
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
