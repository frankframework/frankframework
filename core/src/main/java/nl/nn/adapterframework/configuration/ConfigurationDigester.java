/*
   Copyright 2013, 2016, 2018, 2019 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.validation.ValidatorHandler;

import org.apache.commons.digester3.Digester;
import org.apache.commons.digester3.binder.DigesterLoader;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.digester.FrankDigesterRules;
import nl.nn.adapterframework.configuration.filters.ElementRoleFilter;
import nl.nn.adapterframework.configuration.filters.InitialCapsFilter;
import nl.nn.adapterframework.configuration.filters.OnlyActiveFilter;
import nl.nn.adapterframework.configuration.filters.SkipContainersFilter;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.stream.xml.XmlTee;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.SpringUtils;
import nl.nn.adapterframework.util.StringResolver;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.AttributePropertyResolver;
import nl.nn.adapterframework.xml.ElementPropertyResolver;
import nl.nn.adapterframework.xml.NamespacedContentsRemovingFilter;
import nl.nn.adapterframework.xml.SaxException;
import nl.nn.adapterframework.xml.TransformerFilter;
import nl.nn.adapterframework.xml.XmlWriter;

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
	private @Getter @Setter ApplicationContext applicationContext;
	private @Setter ConfigurationWarnings configurationWarnings;

	private static final String CONFIGURATION_VALIDATION_SCHEMA = "FrankFrameworkCanonical.xsd";

	private String digesterRulesFile = FrankDigesterRules.DIGESTER_RULES_FILE;

	private boolean schemaBasedParsing = AppConstants.getInstance().getBoolean("configurations.digester.schemaBasedParsing", true);
	private boolean suppressValidationWarnings = AppConstants.getInstance().getBoolean(SuppressKeys.CONFIGURATION_VALIDATION.getKey(), false);
	private boolean validation = AppConstants.getInstance().getBoolean("configurations.validation", true);

	private class XmlErrorHandler implements ErrorHandler  {
		private String schema;
		public XmlErrorHandler(String schema) {
			this.schema = schema;
		}

		@Override
		public void warning(SAXParseException exception) throws SAXParseException {
			logErrorMessage("Validation warning", exception);
		}
		@Override
		public void error(SAXParseException exception) throws SAXParseException {
			logErrorMessage("Validation error", exception);
		}
		@Override
		public void fatalError(SAXParseException exception) throws SAXParseException {
			logErrorMessage("Fatal validation error", exception);
		}

		private void logErrorMessage(String prefix, SAXParseException exception) {
			String msg = prefix+" in ["+exception.getSystemId()+"] at line ["+exception.getLineNumber()+"] when validating against schema ["+schema+"]: " + exception.getMessage();
			if (!suppressValidationWarnings) {
				configurationWarnings.add((Object)null, log, msg);
			} else {
				log.debug(msg);
			}
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
		loadDigesterRules(digester, digesterRulesResource);

		if (validation) {
			digester.setValidating(true);
			digester.setNamespaceAware(true);
			digester.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
			URL xsdUrl = ClassUtils.getResourceURL(CONFIGURATION_VALIDATION_SCHEMA);
			if (xsdUrl==null) {
				throw new ConfigurationException("cannot get URL from ["+CONFIGURATION_VALIDATION_SCHEMA+"]");
			}
			digester.setProperty("http://java.sun.com/xml/jaxp/properties/schemaSource", xsdUrl.toExternalForm());
			XmlErrorHandler xeh = new XmlErrorHandler(CONFIGURATION_VALIDATION_SCHEMA);
			digester.setErrorHandler(xeh);
		}

		return digester;
	}

	private void loadDigesterRules(Digester digester, Resource digesterRulesResource) {
		FrankDigesterRules digesterRules = new FrankDigesterRules(digester, digesterRulesResource);
		//Populate the bean with Spring magic
		SpringUtils.autowireByName(applicationContext, digesterRules);
		DigesterLoader loader = DigesterLoader.newLoader(digesterRules);
		loader.addRules(digester);
	}

	public void digest() throws ConfigurationException {
		if(applicationContext instanceof Configuration) {
			digestConfiguration((Configuration)applicationContext);
		} else {
			throw new IllegalStateException("no suitable Configuration found");
		}
	}

	private void digestConfiguration(Configuration configuration) throws ConfigurationException {
		String configurationFile = ConfigurationUtils.getConfigurationFile(configuration.getClassLoader(), configuration.getName());
		Digester digester = null;

		Resource configurationResource = Resource.getResource(configuration, configurationFile);
		if (configurationResource == null) {
			throw new ConfigurationException("Configuration file ["+configurationFile+"] not found in ClassLoader ["+configuration.getClassLoader()+"]");
		}

		try {
			digester = getDigester(configuration);

			if (log.isDebugEnabled()) log.debug("digesting configuration ["+configuration.getName()+"] configurationFile ["+configurationFile+"]");

			AppConstants appConstants = AppConstants.getInstance(configuration.getClassLoader());
			String loaded = resolveEntitiesAndProperties(configuration, configurationResource, appConstants);

			configLogger.info(configuration.getLoadedConfiguration());
			digester.parse(new StringReader(loaded));
		} catch (Throwable t) {
			// wrap exception to be sure it gets rendered via the IbisException-renderer
			String currentElementName = null;
			if (digester != null ) {
				currentElementName = digester.getCurrentElementName();
			}

			throw new ConfigurationException("error during unmarshalling configuration from file [" + configurationFile + "] with digester-rules-file ["+getDigesterRules()+"] in element ["+currentElementName+"]", t);
		}
	}

	private String resolveEntitiesAndProperties(Configuration configuration, Resource resource, Properties appConstants) throws IOException, SAXException, ConfigurationException, TransformerConfigurationException {
		return resolveEntitiesAndProperties(configuration, resource, appConstants, schemaBasedParsing);
	}

	/**
	 * Performs an Identity-transform, which resolves entities with content from files found on the ClassPath.
	 * Resolve all non-attribute properties
	 */
	public String resolveEntitiesAndProperties(Configuration configuration, Resource resource, Properties appConstants, boolean schemaBasedParsing) throws IOException, SAXException, ConfigurationException, TransformerConfigurationException {
		XmlWriter forDigesterLoadedWriter;
		XmlWriter forLoadedHiddenWriter = null;
		ContentHandler handler;
		if(schemaBasedParsing) {
			forDigesterLoadedWriter = new ElementPropertyResolver(appConstants);
			forLoadedHiddenWriter = new ElementPropertyResolver(appConstants);
			handler = new XmlTee(forDigesterLoadedWriter, new AttributePropertyResolver(forLoadedHiddenWriter, appConstants, getPropsToHide(appConstants)));
			handler = getStub4TesttoolContentHandler(handler, appConstants);
			handler = getCanonicalizedConfiguration(handler);
			handler = new OnlyActiveFilter(handler, appConstants);
		} else {
			forDigesterLoadedWriter = new XmlWriter();
			handler = forDigesterLoadedWriter;
		}

		XmlWriter originalConfigWriter = new XmlWriter();
		handler = new XmlTee(handler, originalConfigWriter);

		XmlUtils.parseXml(resource, handler);
		configuration.setOriginalConfiguration(originalConfigWriter.toString());

		String loadedForDigester = forDigesterLoadedWriter.toString();
		if(schemaBasedParsing) {
			configuration.setLoadedConfiguration(forLoadedHiddenWriter.toString());
		} else {
			String loadedHide = StringResolver.substVars(configuration.getOriginalConfiguration(), appConstants, null, getPropsToHide(appConstants));
			loadedHide = processCanonicalizedActivatedStubbedXslts(loadedHide, configuration.getClassLoader());
			configuration.setLoadedConfiguration(loadedHide);

			loadedForDigester = StringResolver.substVars(loadedForDigester, appConstants);
			loadedForDigester = processCanonicalizedActivatedStubbedXslts(loadedForDigester, configuration.getClassLoader());
		}

		return loadedForDigester;
	}

	private String processCanonicalizedActivatedStubbedXslts(String configuration, ClassLoader classLoader) throws ConfigurationException {
		configuration = ConfigurationUtils.getCanonicalizedConfiguration(configuration);
		configuration = ConfigurationUtils.getActivatedConfiguration(configuration);

		if (isConfigurationStubbed(classLoader)) {
			configuration = ConfigurationUtils.getStubbedConfiguration(classLoader, configuration);
		}
		return configuration;
	}

	//Fixes ConfigurationDigesterTest#testOldSchoolConfigurationParser test
	protected boolean isConfigurationStubbed(ClassLoader classLoader) {
		return ConfigurationUtils.isConfigurationStubbed(classLoader);
	}

	private List<String> getPropsToHide(Properties appConstants) {
		List<String> propsToHide = new ArrayList<>();
		String propertiesHideString = appConstants.getProperty("properties.hide");
		if (propertiesHideString != null) {
			propsToHide.addAll(Arrays.asList(propertiesHideString.split("[,\\s]+")));
		}
		return propsToHide;
	}

	public ContentHandler getCanonicalizedConfiguration(ContentHandler writer) throws IOException, SAXException {
		String frankConfigXSD = ConfigurationUtils.FRANK_CONFIG_XSD;
		return getCanonicalizedConfiguration(writer, frankConfigXSD, new XmlErrorHandler(frankConfigXSD));
	}

	public ContentHandler getCanonicalizedConfiguration(ContentHandler handler, String frankConfigXSD, ErrorHandler errorHandler) throws IOException, SAXException {
		try {
			ElementRoleFilter elementRoleFilter = new ElementRoleFilter(handler);
			ValidatorHandler validatorHandler = XmlUtils.getValidatorHandler(ClassUtils.getResourceURL(frankConfigXSD));
			validatorHandler.setContentHandler(elementRoleFilter);
			if (errorHandler != null) {
				validatorHandler.setErrorHandler(errorHandler);
			}
			NamespacedContentsRemovingFilter namespacedContentsRemovingFilter = new NamespacedContentsRemovingFilter(validatorHandler);
			SkipContainersFilter skipContainersFilter = new SkipContainersFilter(namespacedContentsRemovingFilter);
			return new InitialCapsFilter(skipContainersFilter);
		} catch (SAXException e) {
			throw new IOException("Cannot get canonicalizer using ["+ConfigurationUtils.FRANK_CONFIG_XSD+"]", e);
		}
	}

	/**
	 * Get the contenthandler to stub configurations
	 * If stubbing is disabled, the input ContentHandler is returned as-is
	 */
	public ContentHandler getStub4TesttoolContentHandler(ContentHandler handler, Properties properties) throws IOException, TransformerConfigurationException {
		if (Boolean.parseBoolean(properties.getProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY,"false"))) {
			Resource xslt = Resource.getResource(ConfigurationUtils.STUB4TESTTOOL_XSLT);
			TransformerPool tp = TransformerPool.getInstance(xslt);

			TransformerFilter filter = tp.getTransformerFilter(null, handler);
			
			Map<String,Object> parameters = new HashMap<String,Object>();
			parameters.put(ConfigurationUtils.STUB4TESTTOOL_XSLT_VALIDATORS_PARAM, Boolean.parseBoolean(properties.getProperty(ConfigurationUtils.STUB4TESTTOOL_VALIDATORS_DISABLED_KEY,"false")));
			
			XmlUtils.setTransformerParameters(filter.getTransformer(), parameters);
			
			return filter;
		} 
		return handler;
	}

	public void setDigesterRules(String string) {
		digesterRulesFile = string;
	}

	public String getDigesterRules() {
		return digesterRulesFile;
	}
}
