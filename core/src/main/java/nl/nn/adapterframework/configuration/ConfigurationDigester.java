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
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import nl.nn.adapterframework.configuration.digester.FrankDigesterRules;
import nl.nn.adapterframework.configuration.digester.IncludeFilter;
import nl.nn.adapterframework.configuration.filters.ElementRoleFilter;
import nl.nn.adapterframework.configuration.filters.InitialCapsFilter;
import nl.nn.adapterframework.configuration.filters.OnlyActiveFilter;
import nl.nn.adapterframework.configuration.filters.SkipContainersFilter;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.stream.xml.XmlTee;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassLoaderUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.PropertyLoader;
import nl.nn.adapterframework.util.SpringUtils;
import nl.nn.adapterframework.util.StringResolver;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.AttributePropertyResolver;
import nl.nn.adapterframework.xml.ElementPropertyResolver;
import nl.nn.adapterframework.xml.NamespacedContentsRemovingFilter;
import nl.nn.adapterframework.xml.PrettyPrintFilter;
import nl.nn.adapterframework.xml.SaxException;
import nl.nn.adapterframework.xml.TransformerFilter;
import nl.nn.adapterframework.xml.XmlWriter;

/**
 * The configurationDigester reads the configuration.xml and the digester rules
 * in XML format and factors a Configuration.
 *
 * <p>Since 4.0.1, the configuration.xml is first resolved using the {@link StringResolver resolver},
 * with tries to resolve ${variable} with the {@link AppConstants}, so that
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
@Log4j2
public class ConfigurationDigester implements ApplicationContextAware {
	private final Logger configLogger = LogUtil.getLogger("CONFIG");
	private @Getter @Setter ApplicationContext applicationContext;
	private @Setter ConfigurationWarnings configurationWarnings;

	private static final String CONFIGURATION_VALIDATION_SCHEMA = "FrankFrameworkCanonical.xsd";

	private @Getter @Setter String digesterRuleFile = FrankDigesterRules.DIGESTER_RULES_FILE;

	private final boolean suppressValidationWarnings = AppConstants.getInstance().getBoolean(SuppressKeys.CONFIGURATION_VALIDATION.getKey(), false);
	private final boolean validation = AppConstants.getInstance().getBoolean("configurations.validation", true);

	private class XmlErrorHandler implements ErrorHandler  {
		private String schema;
		public XmlErrorHandler(String schema) {
			this.schema = schema;
		}

		@Override
		public void warning(SAXParseException exception) {
			logErrorMessage("Validation warning", exception);
		}
		@Override
		public void error(SAXParseException exception) {
			logErrorMessage("Validation error", exception);
		}
		@Override
		public void fatalError(SAXParseException exception) {
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

		Resource digesterRulesResource = Resource.getResource(configuration, getDigesterRuleFile());
		loadDigesterRules(digester, digesterRulesResource);

		if (validation) {
			digester.setValidating(true);
			digester.setNamespaceAware(true);
			digester.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
			URL xsdUrl = ClassLoaderUtils.getResourceURL(CONFIGURATION_VALIDATION_SCHEMA);
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
		if(!(applicationContext instanceof Configuration)) {
			throw new IllegalStateException("no suitable Configuration found");
		}
		Configuration configurationContext = (Configuration)applicationContext;

		digestConfiguration(configurationContext);
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
			parseAndResolveEntitiesAndProperties(digester, configuration, configurationResource, appConstants);

			configLogger.info(configuration.getLoadedConfiguration());
		} catch (Throwable t) {
			// wrap exception to be sure it gets rendered via the IbisException-renderer
			String currentElementName = null;
			if (digester != null ) {
				currentElementName = digester.getCurrentElementName();
			}
			Locator locator = digester.getDocumentLocator();
			String location = locator!=null ? " systemId ["+locator.getSystemId()+"] line ["+locator.getLineNumber()+"] column ["+locator.getColumnNumber()+"]":"";
			throw new ConfigurationException("error during unmarshalling configuration from file [" + configurationFile + "] "+location+" with digester-rules-file ["+getDigesterRuleFile()+"] in element ["+currentElementName+"]", t);
		}
	}

	/**
	 * Performs an Identity-transform, which resolves entities with content from files found on the ClassPath.
	 * Resolve all non-attribute properties
	 */
	public void parseAndResolveEntitiesAndProperties(ContentHandler digester, Configuration configuration, Resource resource, PropertyLoader properties) throws IOException, SAXException, TransformerConfigurationException {
		ContentHandler handler;

		XmlWriter loadedHiddenWriter = new XmlWriter();
		handler = new PrettyPrintFilter(loadedHiddenWriter);
		handler = new AttributePropertyResolver(handler, properties, getPropsToHide(properties));
		handler = new XmlTee(digester, handler);

		handler = getStub4TesttoolContentHandler(handler, properties);
		handler = getConfigurationCanonicalizer(handler);
		handler = new OnlyActiveFilter(handler, properties);
		handler = new ElementPropertyResolver(handler, properties);

		boolean rewriteLegacyClassNames = properties.getBoolean("migration.rewrite.legacyClassNames", false);
		if (rewriteLegacyClassNames) {
			handler = new ClassNameRewriter(handler);
		}

		XmlWriter originalConfigWriter = new XmlWriter();
		handler = new XmlTee(handler, originalConfigWriter);

		handler = new IncludeFilter(handler, resource);

		XmlUtils.parseXml(resource, handler);
		configuration.setOriginalConfiguration(originalConfigWriter.toString());
		configuration.setLoadedConfiguration(loadedHiddenWriter.toString());
	}

	private Set<String> getPropsToHide(Properties appConstants) {
		Set<String> propsToHide = new HashSet<>();
		String propertiesHideString = appConstants.getProperty("properties.hide");
		if (propertiesHideString != null) {
			propsToHide.addAll(Arrays.asList(propertiesHideString.split("[,\\s]+")));
		}
		return propsToHide;
	}

	public ContentHandler getConfigurationCanonicalizer(ContentHandler writer) throws IOException {
		String frankConfigXSD = ConfigurationUtils.FRANK_CONFIG_XSD;
		return getConfigurationCanonicalizer(writer, frankConfigXSD, new XmlErrorHandler(frankConfigXSD));
	}

	public ContentHandler getConfigurationCanonicalizer(ContentHandler handler, String frankConfigXSD, ErrorHandler errorHandler) throws IOException {
		try {
			ElementRoleFilter elementRoleFilter = new ElementRoleFilter(handler);
			ValidatorHandler validatorHandler = XmlUtils.getValidatorHandler(ClassLoaderUtils.getResourceURL(frankConfigXSD));
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
	public ContentHandler getStub4TesttoolContentHandler(ContentHandler handler, PropertyLoader properties) throws IOException, TransformerConfigurationException {
		if (properties.getBoolean(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, false)) {
			Resource xslt = Resource.getResource(ConfigurationUtils.STUB4TESTTOOL_XSLT);
			TransformerPool tp = TransformerPool.getInstance(xslt);

			TransformerFilter filter = tp.getTransformerFilter(null, handler);

			Map<String,Object> parameters = new HashMap<>();
			parameters.put(ConfigurationUtils.STUB4TESTTOOL_XSLT_VALIDATORS_PARAM, Boolean.parseBoolean(properties.getProperty(ConfigurationUtils.STUB4TESTTOOL_VALIDATORS_DISABLED_KEY,"false")));

			XmlUtils.setTransformerParameters(filter.getTransformer(), parameters);

			return filter;
		}
		return handler;
	}
}
