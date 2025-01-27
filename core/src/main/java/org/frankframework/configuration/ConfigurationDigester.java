/*
   Copyright 2013-2019 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.configuration;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.validation.ValidatorHandler;

import jakarta.annotation.Nullable;

import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.digester.Digester;
import org.frankframework.configuration.digester.FrankDigesterRules;
import org.frankframework.configuration.digester.IncludeFilter;
import org.frankframework.configuration.filters.ElementRoleFilter;
import org.frankframework.configuration.filters.InitialCapsFilter;
import org.frankframework.configuration.filters.OnlyActiveFilter;
import org.frankframework.configuration.filters.SkipContainersFilter;
import org.frankframework.core.IScopeProvider;
import org.frankframework.core.Resource;
import org.frankframework.documentbuilder.xml.XmlTee;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.PropertyLoader;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.StringResolver;
import org.frankframework.util.TransformerPool;
import org.frankframework.util.XmlUtils;
import org.frankframework.xml.AttributePropertyResolver;
import org.frankframework.xml.ElementPropertyResolver;
import org.frankframework.xml.NamespacedContentsRemovingFilter;
import org.frankframework.xml.PrettyPrintFilter;
import org.frankframework.xml.TransformerFilter;
import org.frankframework.xml.XmlWriter;

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
 * <pre>{@code
 * <?xml version="1.0"?>
 * <!DOCTYPE configuration
 * [
 * <!ENTITY HelloWorld SYSTEM "./ConfigurationHelloWorld.xml">
 * ]>
 *
 * <configuration name="HelloWorld">
 *
 * &HelloWorld;
 *
 * </configuration>
 * }</pre>
 * @author Johan Verrips
 * @see Configuration
 */
@Log4j2
public class ConfigurationDigester implements ApplicationContextAware {
	public static final String MIGRATION_REWRITE_LEGACY_CLASS_NAMES_KEY = "migration.rewriteLegacyClassNames";
	private final Logger configLogger = LogUtil.getLogger("CONFIG");
	private @Getter @Setter ApplicationContext applicationContext;
	private @Setter ConfigurationWarnings configurationWarnings;

	private @Getter @Setter String digesterRuleFile = "digester-rules.xml";

	private final boolean suppressValidationWarnings = AppConstants.getInstance().getBoolean(SuppressKeys.CONFIGURATION_VALIDATION.getKey(), false);

	private class XmlErrorHandler implements ErrorHandler  {
		private final String schema;
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

	private Digester getDigester(Configuration configuration) throws ConfigurationException {
		Digester digester = SpringUtils.createBean(configuration, Digester.class);
		try {

			Resource digesterRulesResource = Resource.getResource(configuration, getDigesterRuleFile());
			loadDigesterRules(digester, digesterRulesResource);

			return digester;
		} catch (IOException | SAXException e) {
			throw new ConfigurationException("unable to create configuration parser", e);
		}
	}

	private void loadDigesterRules(Digester digester, Resource digesterRulesResource) throws IOException, SAXException {
		FrankDigesterRules rules = new FrankDigesterRules();
		XmlUtils.parseXml(digesterRulesResource.asInputSource(), rules);
		digester.setParsedPatterns(rules.getParsedPatterns());
	}

	public void digest() throws ConfigurationException {
		if(!(applicationContext instanceof Configuration configuration)) {
			throw new IllegalStateException("no suitable Configuration found");
		}

		Resource configurationResource = getConfigurationResource(configuration);
		if(configurationResource == null) {
			return;
		}

		digestConfiguration(configuration, configurationResource);
	}

	private void digestConfiguration(Configuration configuration, Resource configurationResource) throws ConfigurationException {
		Digester digester = getDigester(configuration);
		try {
			log.debug("digesting configuration [{}] configurationFile [{}]", configuration::getName, configurationResource::getSystemId);

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
			throw new ConfigurationException("error during unmarshalling configuration from file [" + configurationResource + "] "+location+" with digester-rules-file ["+getDigesterRuleFile()+"] in element ["+currentElementName+"]", t);
		}
	}

	@Nullable
	private Resource getConfigurationResource(Configuration configuration) throws ConfigurationException {
		String configurationFile = ConfigurationUtils.getConfigurationFile(configuration.getClassLoader(), configuration.getName());

		Resource configurationResource = Resource.getResource(configuration, configurationFile);
		if (configurationResource != null) {
			return configurationResource;
		}

		if(ConfigurationUtils.isConfigurationXmlOptional(configuration)) {
			configuration.log("no configuration file found, skipping xml digest");
			return null;
		}

		throw new ConfigurationException("Configuration file ["+configurationFile+"] not found in ClassLoader ["+configuration.getClassLoader()+"]");
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

		handler = getStub4TesttoolContentHandler(handler, configuration, properties);
		handler = getConfigurationCanonicalizer(handler);
		handler = new OnlyActiveFilter(handler, properties);
		handler = new ElementPropertyResolver(handler, properties);

		boolean rewriteLegacyClassNames = properties.getBoolean(MIGRATION_REWRITE_LEGACY_CLASS_NAMES_KEY, false);
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
	public ContentHandler getStub4TesttoolContentHandler(ContentHandler handler, IScopeProvider scope, PropertyLoader properties) throws IOException, TransformerConfigurationException {
		if (properties.getBoolean(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, false)) {
			String stubFile = properties.getString(ConfigurationUtils.STUB4TESTTOOL_XSLT_KEY, ConfigurationUtils.STUB4TESTTOOL_XSLT_DEFAULT);
			Resource xslt = Resource.getResource(scope, stubFile);
			TransformerPool tp = TransformerPool.getInstance(xslt);

			TransformerFilter filter = tp.getTransformerFilter(handler);

			Map<String,Object> parameters = new HashMap<>();
			parameters.put(ConfigurationUtils.STUB4TESTTOOL_XSLT_VALIDATORS_PARAM, Boolean.parseBoolean(properties.getProperty(ConfigurationUtils.STUB4TESTTOOL_VALIDATORS_DISABLED_KEY,"false")));

			XmlUtils.setTransformerParameters(filter.getTransformer(), parameters);

			return filter;
		}
		return handler;
	}
}
