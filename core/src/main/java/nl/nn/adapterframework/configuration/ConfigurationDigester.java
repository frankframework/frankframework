/*
   Copyright 2013, 2016, 2018, 2019 Nationale-Nederlanden, 2020-2021 WeAreFrank!

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

import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;

import org.apache.commons.digester3.Digester;
import org.apache.commons.digester3.binder.DigesterLoader;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.digester.FrankDigesterRules;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.monitoring.MonitorManager;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StringResolver;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.SaxException;

/**
 * The configurationDigester reads the configuration.xml and the digester rules
 * in XML format and factors a Configuration.
 *
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
public class ConfigurationDigester implements ApplicationContextAware {
	private final Logger log = LogUtil.getLogger(ConfigurationDigester.class);
	private final Logger configLogger = LogUtil.getLogger("CONFIG");
	private ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
	private @Getter @Setter ApplicationContext applicationContext;

	private static final String CONFIGURATION_VALIDATION_KEY = "configurations.validate";
	private static final String CONFIGURATION_VALIDATION_SCHEMA = "FrankFrameworkCanonical.xsd";

	private static final String ATTRIBUTEGETTER_XSLT = "xml/xsl/AttributesGetter.xsl";

	private String digesterRulesFile = FrankDigesterRules.DIGESTER_RULES_FILE;

	String lastResolvedEntity = null;

	private class XmlErrorHandler implements ErrorHandler  {
		private Configuration configuration;
		public XmlErrorHandler(Configuration configuration) {
			this.configuration = configuration;
		}
		@Override
		public void warning(SAXParseException exception) throws SAXParseException {
			ConfigurationWarnings.add(configuration, log, "Warning when validating against schema ["+CONFIGURATION_VALIDATION_SCHEMA+"] at line,column ["+exception.getLineNumber()+","+exception.getColumnNumber()+"]: " + exception.getMessage());
		}
		@Override
		public void error(SAXParseException exception) throws SAXParseException {
			ConfigurationWarnings.add(configuration, log, "Error when validating against schema ["+CONFIGURATION_VALIDATION_SCHEMA+"] at line,column ["+exception.getLineNumber()+","+exception.getColumnNumber()+"]: " + exception.getMessage());
		}
		@Override
		public void fatalError(SAXParseException exception) throws SAXParseException {
			ConfigurationWarnings.add(configuration, log, "FatalError when validating against schema ["+CONFIGURATION_VALIDATION_SCHEMA+"] at line,column ["+exception.getLineNumber()+","+exception.getColumnNumber()+"]: " + exception.getMessage());
		}
	}

	private Digester getDigester(Configuration configuration) throws ConfigurationException, ParserConfigurationException, SAXException {
		XMLReader reader = XmlUtils.getXMLReader(configuration);
		Digester digester = new Digester(reader) {
			// override Digester.createSAXException() implementations to obtain a clear unduplicated message and a properly nested stacktrace on IBM JDK 
			@Override
			public SAXException createSAXException(String message, Exception e) {
				return SaxException.createSaxException(message, getDocumentLocator(), e);
			}
			@Override
			public SAXException createSAXException(Exception e) {
				return SaxException.createSaxException(null, getDocumentLocator(), e);
			}
		};

		digester.setUseContextClassLoader(true);
		digester.push(configuration);

		Resource digesterRulesResource = Resource.getResource(configuration, getDigesterRules());

		FrankDigesterRules digesterRules = new FrankDigesterRules(digester, digesterRulesResource);
		//Populate the bean with Spring magic
		applicationContext.getAutowireCapableBeanFactory().autowireBeanProperties(digesterRules, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false); //TODO: use helper class to wire and init
		digesterRules = (FrankDigesterRules) applicationContext.getAutowireCapableBeanFactory().initializeBean(digesterRules, "digesterRules");
		DigesterLoader loader = DigesterLoader.newLoader(digesterRules);
		loader.addRules(digester);

		if (MonitorManager.getInstance().isEnabled()) {
			MonitorManager.getInstance().setDigesterRules(digester);
		}

		boolean validation = AppConstants.getInstance().getBoolean(CONFIGURATION_VALIDATION_KEY, false);
		if (validation) {
			digester.setValidating(true);
			digester.setNamespaceAware(true);
			digester.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
			URL xsdUrl = ClassUtils.getResourceURL(CONFIGURATION_VALIDATION_SCHEMA);
			if (xsdUrl==null) {
				throw new ConfigurationException("cannot get URL from ["+CONFIGURATION_VALIDATION_SCHEMA+"]");
			}
			digester.setProperty("http://java.sun.com/xml/jaxp/properties/schemaSource", xsdUrl.toExternalForm());
			XmlErrorHandler xeh = new XmlErrorHandler(configuration);
			digester.setErrorHandler(xeh);
		}

		return digester;
	}

	public void digestConfiguration() throws ConfigurationException {
		if(applicationContext instanceof Configuration) {
			digestConfiguration((Configuration)applicationContext);
		} else {
			throw new IllegalStateException("no suitable Configuration found");
		}
	}

	public void digestConfiguration(Configuration configuration) throws ConfigurationException {
		String configurationFile = ConfigurationUtils.getConfigurationFile(configuration.getClassLoader(), configuration.getName());
		Digester digester = null;
		try {
			digester = getDigester(configuration);

			Resource configurationResource = Resource.getResource(configuration, configurationFile);
			if (configurationResource == null) {
				throw new ConfigurationException("Configuration file not found: " + configurationFile);
			}
			if (log.isDebugEnabled()) log.debug("digesting configuration ["+configuration.getName()+"] configurationFile ["+configurationFile+"]");

			String original = XmlUtils.identityTransform(configurationResource);
			fillConfigWarnDefaultValueExceptions(XmlUtils.stringToSource(original)); // must use 'original', cannot use configurationResource, because EntityResolver will not be properly set
			configuration.setOriginalConfiguration(original);
			List<String> propsToHide = new ArrayList<>();
			String propertiesHideString = AppConstants.getInstance(Thread.currentThread().getContextClassLoader()).getString("properties.hide", null);
			if (propertiesHideString != null) {
				propsToHide.addAll(Arrays.asList(propertiesHideString.split("[,\\s]+")));
			}
			String loaded = StringResolver.substVars(original, AppConstants.getInstance(Thread.currentThread().getContextClassLoader()));
			String loadedHide = StringResolver.substVars(original, AppConstants.getInstance(Thread.currentThread().getContextClassLoader()), null, propsToHide);
			loaded = ConfigurationUtils.getCanonicalizedConfiguration(configuration, loaded);
			loadedHide = ConfigurationUtils.getCanonicalizedConfiguration(configuration, loadedHide);
			loaded = ConfigurationUtils.getActivatedConfiguration(configuration, loaded);
			loadedHide = ConfigurationUtils.getActivatedConfiguration(configuration, loadedHide);
			if (ConfigurationUtils.isConfigurationStubbed(configuration.getClassLoader())) {
				loaded = ConfigurationUtils.getStubbedConfiguration(configuration, loaded);
				loadedHide = ConfigurationUtils.getStubbedConfiguration(configuration, loadedHide);
			}
			configuration.setLoadedConfiguration(loadedHide);
			configLogger.info(loadedHide);
			digester.parse(new StringReader(loaded));
		} catch (Throwable t) {
			// wrap exception to be sure it gets rendered via the IbisException-renderer
			String currentElementName = null;
			if (digester != null ) {
				currentElementName = digester.getCurrentElementName();
			}

			throw new ConfigurationException("error during unmarshalling configuration from file [" + configurationFile +
				"] with digester-rules-file ["+getDigesterRules()+"] in element ["+currentElementName+"]"+(StringUtils.isEmpty(lastResolvedEntity)?"":" last resolved entity ["+lastResolvedEntity+"]"), t);
		}
		if (MonitorManager.getInstance().isEnabled()) {
			MonitorManager.getInstance().configure(configuration); //TODO fix memory leak when the configuration is reloaded
		}
	}

	private void fillConfigWarnDefaultValueExceptions(Source configurationSource) throws Exception {
		URL xsltSource = ClassUtils.getResourceURL(ATTRIBUTEGETTER_XSLT);
		if (xsltSource == null) {
			throw new ConfigurationException("cannot find resource ["+ATTRIBUTEGETTER_XSLT+"]");
		}
		Transformer transformer = XmlUtils.createTransformer(xsltSource);
		String attributes = XmlUtils.transformXml(transformer, configurationSource);
		Element attributesElement = XmlUtils.buildElement(attributes);
		Collection<Node> attributeElements = XmlUtils.getChildTags(attributesElement, "attribute");
		Iterator<Node> iter = attributeElements.iterator();
		while (iter.hasNext()) {
			Element attributeElement = (Element)iter.next();
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

	public void setDigesterRules(String string) {
		digesterRulesFile = string;
	}

	public String getDigesterRules() {
		return digesterRulesFile;
	}
}
