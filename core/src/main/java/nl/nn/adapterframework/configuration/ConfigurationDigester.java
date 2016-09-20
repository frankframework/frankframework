/*
   Copyright 2013, 2016 Nationale-Nederlanden

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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.transform.Transformer;

import nl.nn.adapterframework.monitoring.MonitorManager;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StringResolver;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.commons.digester.xmlrules.FromXmlRuleSet;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
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
&lt;?xml version="1.0"?&gt;
&lt;!DOCTYPE configuration
[
&lt;!ENTITY HelloWorld SYSTEM "./ConfigurationHelloWorld.xml"&gt;
]&gt;

&lt;configuration name="HelloWorld"&gt;

&HelloWorld;

&lt;/configuration&gt;
</pre></code>
 * @author Johan Verrips
 * @see Configuration
 */
public class ConfigurationDigester {
	private static final Logger LOG = LogUtil.getLogger(ConfigurationDigester.class);
	private ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();

	private static final String DIGESTER_RULES_DEFAULT = "digester-rules.xml";

	private static final String CONFIGURATION_VALIDATION_KEY = "validate.configuration";
	private static final String CONFIGURATION_STUB4TESTTOOL_KEY = "stub4testtool.configuration";

	private static final String stub4testtool_xslt = "/xml/xsl/stub4testtool.xsl";
	private static final String attributesGetter_xslt = "/xml/xsl/AttributesGetter.xsl";

	private String configurationFile = null;
	private String digesterRulesFile = DIGESTER_RULES_DEFAULT;

	String lastResolvedEntity = null;

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

	public Digester getDigester(Configuration configuration) throws SAXNotSupportedException, SAXNotRecognizedException {
		Digester digester = new Digester();
		digester.setUseContextClassLoader(true);
		digester.push(configuration);

		URL digesterRulesURL = ClassUtils.getResourceURL(this, getDigesterRules());
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
		digester.addRule("*/directoryCleaner", attributeChecker);
		digester.addRule("*/statistics", attributeChecker);
		digester.addRule("*/handler", attributeChecker);
		digester.addRule("*/cache", attributeChecker);
		digester.addRule("*/inputValidator", attributeChecker);
		digester.addRule("*/outputValidator", attributeChecker);
		digester.addRule("*/inputWrapper", attributeChecker);
		digester.addRule("*/outputWrapper", attributeChecker);
		if (MonitorManager.getInstance().isEnabled()) {
			MonitorManager.getInstance().setDigesterRules(digester);
		}

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

	public void digestConfiguration(ClassLoader classLoader, Configuration configuration, String configurationFile, boolean configLogAppend) throws ConfigurationException {
		Digester digester = null;
		try {
			digester = getDigester(configuration);
			URL digesterRulesURL = ClassUtils.getResourceURL(this, getDigesterRules());
			if (digesterRulesURL == null) {
				throw new ConfigurationException("Digester rules file not found: " + getDigesterRules());
			}
			URL configurationFileURL= ClassUtils.getResourceURL(classLoader, configurationFile);
			if (configurationFileURL == null) {
				throw new ConfigurationException("Configuration file not found: " + configurationFile);
			}
			configuration.setDigesterRulesURL(digesterRulesURL);
			configuration.setConfigurationURL(configurationFileURL);
			fillConfigWarnDefaultValueExceptions(configuration);
			String lineSeparator = SystemUtils.LINE_SEPARATOR;
			if (null == lineSeparator) lineSeparator = "\n";
			String original = Misc.resourceToString(configurationFileURL, lineSeparator, false);
			original = XmlUtils.identityTransform(classLoader, original);
			configuration.setOriginalConfiguration(original);
			List<String> propsToHide = new ArrayList<String>();
			String propertiesHideString = AppConstants.getInstance().getString("properties.hide", null);
			if (propertiesHideString != null) {
				propsToHide.addAll(Arrays.asList(propertiesHideString.split("[,\\s]+")));
			}
			String loaded = StringResolver.substVars(original, AppConstants.getInstance(Thread.currentThread().getContextClassLoader()));
			String loadedHide = StringResolver.substVars(original, AppConstants.getInstance(Thread.currentThread().getContextClassLoader()), null, propsToHide);
			loaded = ConfigurationUtils.getActivatedConfiguration(configuration, loaded);
			loadedHide = ConfigurationUtils.getActivatedConfiguration(configuration, loadedHide);
			if (ConfigurationUtils.stubConfiguration()) {
				loaded = ConfigurationUtils.getStubbedConfiguration(configuration, loaded);
				loadedHide = ConfigurationUtils.getStubbedConfiguration(configuration, loadedHide);
			}
			configuration.setLoadedConfiguration(loadedHide);
			saveConfig(loadedHide, configLogAppend);
			digester.parse(new StringReader(loaded));
		} catch (Throwable t) {
			// wrap exception to be sure it gets rendered via the IbisException-renderer
			String currentElementName = null;
			if (digester != null ) {
				currentElementName = digester.getCurrentElementName();
			}
			ConfigurationException e = new ConfigurationException("error during unmarshalling configuration from file [" + configurationFile +
				"] with digester-rules-file ["+getDigesterRules()+"] in element ["+currentElementName+"]"+(StringUtils.isEmpty(lastResolvedEntity)?"":" last resolved entity ["+lastResolvedEntity+"]"), t);
			throw e;
		}
		if (MonitorManager.getInstance().isEnabled()) {
			MonitorManager.getInstance().configure(configuration);
		}
	}

	private void saveConfig(String config, boolean append) {
		String directoryName = AppConstants.getInstance().getResolvedProperty("log.dir");
		String fileName = AppConstants.getInstance().getResolvedProperty("instance.name.lc")+"-config.xml";
		File file = new File(directoryName, fileName);
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(file, append);
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
	
	private  void fillConfigWarnDefaultValueExceptions(Configuration configuration) throws Exception {
		URL xsltSource = ClassUtils.getResourceURL(this, attributesGetter_xslt);
		if (xsltSource == null) {
			throw new ConfigurationException("cannot find resource ["+stub4testtool_xslt+"]");
		}
		Transformer transformer = XmlUtils.createTransformer(xsltSource);
		String lineSeparator=SystemUtils.LINE_SEPARATOR;
		if (null==lineSeparator) lineSeparator="\n";
		String configString=Misc.resourceToString(configuration.getConfigurationURL(), lineSeparator, false);
		configString=XmlUtils.identityTransform(configuration.getClassLoader(), configString);
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

}
