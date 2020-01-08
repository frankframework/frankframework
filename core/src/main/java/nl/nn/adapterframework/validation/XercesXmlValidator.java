/*
   Copyright 2013-2017 Nationale-Nederlanden

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
package nl.nn.adapterframework.validation;


import nl.nn.adapterframework.cache.EhCache;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.ClassLoaderXmlEntityResolver;
import nl.nn.adapterframework.xml.NonResolvingExternalEntityResolver;
import org.apache.log4j.Logger;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.impl.xs.SchemaGrammar;
import org.apache.xerces.jaxp.validation.ValidatorHandlerImpl;
import org.apache.xerces.jaxp.validation.XMLSchema11Factory;
import org.apache.xerces.jaxp.validation.XMLSchemaFactory;
import org.apache.xerces.parsers.XMLGrammarPreparser;
import org.apache.xerces.util.SecurityManager;
import org.apache.xerces.util.SymbolTable;
import org.apache.xerces.util.XMLGrammarPoolImpl;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.grammars.Grammar;
import org.apache.xerces.xni.grammars.XMLGrammarDescription;
import org.apache.xerces.xni.grammars.XMLGrammarPool;
import org.apache.xerces.xni.grammars.XSGrammar;
import org.apache.xerces.xni.parser.XMLEntityResolver;
import org.apache.xerces.xni.parser.XMLErrorHandler;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParseException;
import org.apache.xerces.xs.XSModel;
import org.json.XML;
import org.xml.sax.*;

import javax.xml.XMLConstants;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.ValidatorHandler;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.xerces.parsers.XMLGrammarCachingConfiguration.BIG_PRIME;


/**
 * Xerces based XML validator.
 *
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link nl.nn.adapterframework.pipes.XmlValidator#setForwardName(String) forwardName}</i></td><td>if specified, the value for "success"</td></tr>
 * <tr><td>"parserError"</td><td>a parser exception occurred, probably caused by non-well-formed XML. If not specified, "failure" is used in such a case</td></tr>
 * <tr><td>"illegalRoot"</td><td>if the required root element is not found. If not specified, "failure" is used in such a case</td></tr>
 * <tr><td>"notValid"</td><td>if a validation error occurred</td></tr>
 * </table>
 * <br>
 * N.B. noNamespaceSchemaLocation may contain spaces, but not if the schema is stored in a .jar or .zip file on the class path.
 *
 * @author Johan Verrips IOS
 * @author Jaco de Groot
 */
public class XercesXmlValidator extends AbstractXmlValidator {

	/**
	 * Property identifier: grammar pool.
	 */
	public static final String GRAMMAR_POOL = Constants.XERCES_PROPERTY_PREFIX + Constants.XMLGRAMMAR_POOL_PROPERTY;

	/**
	 * Namespaces feature id (http://xml.org/sax/features/namespaces).
	 */
	protected static final String NAMESPACES_FEATURE_ID = Constants.SAX_FEATURE_PREFIX + Constants.NAMESPACES_FEATURE;

	/**
	 * Validation feature id (http://xml.org/sax/features/validation).
	 */
	protected static final String VALIDATION_FEATURE_ID = Constants.SAX_FEATURE_PREFIX + Constants.VALIDATION_FEATURE;

	/**
	 * Schema validation feature id (http://apache.org/xml/features/validation/schema).
	 */
	protected static final String SCHEMA_VALIDATION_FEATURE_ID = Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_VALIDATION_FEATURE;

	/**
	 * Schema full checking feature id (http://apache.org/xml/features/validation/schema-full-checking).
	 */
	protected static final String SCHEMA_FULL_CHECKING_FEATURE_ID = Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_FULL_CHECKING;

	protected static final String SECURITY_MANAGER_PROPERTY_ID = Constants.XERCES_PROPERTY_PREFIX + Constants.SECURITY_MANAGER_PROPERTY;

	protected static final String XML_SCHEMA_VERSION_PROPERTY = Constants.XERCES_PROPERTY_PREFIX + Constants.XML_SCHEMA_VERSION_PROPERTY;

	/**
	 * Property identifier: entity resolver.
	 */
	private static final String ENTITY_RESOLVER =
			Constants.XERCES_PROPERTY_PREFIX + Constants.ENTITY_RESOLVER_PROPERTY;

	private static final int maxInitialised = AppConstants.getInstance().getInt("xmlValidator.maxInitialised", -1);
	private static final boolean sharedSymbolTable = AppConstants.getInstance().getBoolean("xmlValidator.sharedSymbolTable", false);
	private static final int sharedSymbolTableSize = AppConstants.getInstance().getInt("xmlValidator.sharedSymbolTable.size", BIG_PRIME);
	private int entityExpansionLimit = AppConstants.getInstance().getInt("xmlValidator.entityExpansionLimit", 100000);

	private static AtomicLong counter = new AtomicLong();
	private String preparseResultId;
	private PreparseResult preparseResult;

	// To be able to allow users to specify 1.0.
	// Technically xsd 1.1 should be backwards compatible.
	private double xsdVersion = 1.1;

	private static EhCache cache;

	static {
		if (maxInitialised != -1) {
			cache = new EhCache();
			cache.setMaxElementsInMemory(maxInitialised);
			cache.setEternal(true);
			try {
				cache.configure("XercesXmlValidator");
			} catch (ConfigurationException e) {
				cache = null;
				ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
				configWarnings.add(log,
						"Could not configure EhCache for XercesXmlValidator (xmlValidator.maxInitialised will be ignored)",
						e);
			}
			cache.open();
		}
	}

	public XercesXmlValidator() {
		preparseResultId = "" + counter.getAndIncrement();
	}

	@Override
	protected void init() throws ConfigurationException {
		if (needsInit) {
			super.init();
			if (schemasProvider == null) throw new IllegalStateException("No schema provider");
			String schemasId = schemasProvider.getSchemasId();
			if (schemasId != null) {
				PreparseResult preparseResult = preparse(schemasId, schemasProvider.getSchemas());
				if (cache == null || isIgnoreCaching()) {
					this.preparseResult = preparseResult;
				} else {
					cache.putObject(preparseResultId, preparseResult);
				}
			}
		}
	}

	private static class SymbolTableSingletonHelper {
		private static final SymbolTable INSTANCE = new SymbolTable(sharedSymbolTableSize);
	}

	public static SymbolTable getSymbolTableInstance() {
		return SymbolTableSingletonHelper.INSTANCE;
	}

	private SymbolTable getSymbolTable() {
		if (sharedSymbolTable) {
			return getSymbolTableInstance();
		}
		return new SymbolTable(BIG_PRIME);
	}

	private synchronized PreparseResult preparse(String schemasId, List<Schema> schemas) throws ConfigurationException {
		SymbolTable symbolTable = getSymbolTable();
		XMLGrammarPool grammarPool = new XMLGrammarPoolImpl();
		Set<String> namespaceSet = new HashSet<>();
		XMLGrammarPreparser preparser = new XMLGrammarPreparser(symbolTable);
		preparser.setEntityResolver(new ClassLoaderXmlEntityResolver(getConfigurationClassLoader()));
		preparser.registerPreparser(XMLGrammarDescription.XML_SCHEMA, null);
		preparser.setProperty(GRAMMAR_POOL, grammarPool);
		preparser.setFeature(NAMESPACES_FEATURE_ID, true);
		preparser.setFeature(VALIDATION_FEATURE_ID, true);
		preparser.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
		preparser.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, isFullSchemaChecking());
		preparser.setProperty(XML_SCHEMA_VERSION_PROPERTY,
				(xsdVersion == 1.0) ? Constants.W3C_XML_SCHEMA10EX_NS_URI : Constants.W3C_XML_SCHEMA11_NS_URI);
		MyErrorHandler errorHandler = new MyErrorHandler();
		errorHandler.warn = warn;
		preparser.setErrorHandler(errorHandler);
		for (Schema schema : schemas) {
			Grammar grammar = preparse(preparser, schemasId, schema);
			registerNamespaces(grammar, namespaceSet);
		}
		grammarPool.lockPool();
		PreparseResult preparseResult = new PreparseResult();
		preparseResult.setSchemasId(schemasId);
		preparseResult.setSymbolTable(symbolTable);
		preparseResult.setGrammarPool(grammarPool);
		preparseResult.setNamespaceSet(namespaceSet);
		return preparseResult;
	}

	private static Grammar preparse(XMLGrammarPreparser preparser,
									String schemasId, Schema schema) throws ConfigurationException {
		try {
			XMLInputSource inputSource = stringToXMLInputSource(schema);
			return preparser.preparseGrammar(XMLGrammarDescription.XML_SCHEMA,
					inputSource);
		} catch (IOException e) {
			throw new ConfigurationException("cannot compile schema's ["
					+ schemasId + "]", e);
		}
	}

	private static void registerNamespaces(Grammar grammar, Set<String> namespaces) {
		namespaces.add(grammar.getGrammarDescription().getNamespace());
		if (grammar instanceof SchemaGrammar) {
			List<?> imported = ((SchemaGrammar) grammar).getImportedGrammars();
			if (imported != null) {
				for (Object g : imported) {
					Grammar gr = (Grammar) g;
					registerNamespaces(gr, namespaces);
				}
			}
		}
	}

	protected PreparseResult getPreparseResult(IPipeLineSession session) throws ConfigurationException, PipeRunException {
		PreparseResult preparseResult;
		String schemasId = schemasProvider.getSchemasId();
		if (schemasId == null) {
			schemasId = schemasProvider.getSchemasId(session);
			preparseResult = preparse(schemasId, schemasProvider.getSchemas(session));
		} else {
			if (cache == null || isIgnoreCaching()) {
				preparseResult = this.preparseResult;
				if (preparseResult == null) {
					init();
					preparseResult = this.preparseResult;
				}
			} else {
				preparseResult = (PreparseResult) cache.getObject(preparseResultId);
				if (preparseResult == null) {
					preparseResult = preparse(schemasId, schemasProvider.getSchemas());
					cache.putObject(preparseResultId, preparseResult);
				}
			}
		}
		return preparseResult;
	}

	@Override
	public XercesValidationContext createValidationContext(IPipeLineSession session, Set<List<String>> rootValidations, Map<List<String>, List<String>> invalidRootNamespaces) throws ConfigurationException, PipeRunException {
		// clear session variables
		super.createValidationContext(session, rootValidations, invalidRootNamespaces);

		PreparseResult preparseResult = getPreparseResult(session);
		XercesValidationContext result = new XercesValidationContext(preparseResult);

		result.init(schemasProvider, preparseResult.getSchemasId(), preparseResult.getNamespaceSet(), rootValidations, invalidRootNamespaces, ignoreUnknownNamespaces);
		return result;
	}

	@Override
	public ValidatorHandler getValidatorHandler(IPipeLineSession session, ValidationContext context) throws ConfigurationException {
		// User Impl version from xerces, because it has extra validate() function.
		ValidatorHandlerImpl validatorHandler;

		try {
			javax.xml.validation.Schema schemaObject;
			if (xsdVersion == 1.0) {
				XMLSchemaFactory schemaFactory = new XMLSchemaFactory();
				schemaObject = schemaFactory.newSchema(((XercesValidationContext) context).getGrammarPool());
			} else {
				XMLSchema11Factory schemaFactory = new XMLSchema11Factory();
				schemaObject = schemaFactory.newSchema(((XercesValidationContext) context).getGrammarPool());
			}

			validatorHandler = (ValidatorHandlerImpl) schemaObject.newValidatorHandler();
		} catch (SAXException e) {
			throw new ConfigurationException(logPrefix + "Cannot create schema", e);
		}
		try {
			validatorHandler.setFeature(VALIDATION_FEATURE_ID, true);
			validatorHandler.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
			validatorHandler.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, isFullSchemaChecking());
			validatorHandler.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
			validatorHandler.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);


			SecurityManager securityManager = new SecurityManager();
			securityManager.setEntityExpansionLimit(entityExpansionLimit);
			validatorHandler.setProperty(SECURITY_MANAGER_PROPERTY_ID, securityManager);

			validatorHandler.setContentHandler(context.getContentHandler());
			validatorHandler.setErrorHandler(context.getErrorHandler());
		} catch (SAXNotRecognizedException e) {
			throw new ConfigurationException(logPrefix + "ValidatorHandler does not recognize necessary feature", e);
		} catch (SAXNotSupportedException e) {
			throw new ConfigurationException(logPrefix + "ValidatorHandler does not support necessary feature", e);
		}
		return validatorHandler;
	}

	private static XMLInputSource stringToXMLInputSource(Schema schema) throws IOException, ConfigurationException {
		// SystemId is needed in case the schema has an import. Maybe we should
		// already resolve this at the SchemaProvider side (except when
		// noNamespaceSchemaLocation is being used this is already done in
		// (Wsdl)XmlValidator (using
		// mergeXsdsGroupedByNamespaceToSchemasWithoutIncludes)).
		// See comment in method XmlValidator.getSchemas() too.
		// See ClassLoaderXmlEntityResolver too.
		return new XMLInputSource(null, schema.getSystemId(), null, schema.getInputStream(), null);
	}

	public void setXsdVersion(double xsdVersion) {
		this.xsdVersion = xsdVersion;
	}
}


class XercesValidationContext extends ValidationContext {

	private PreparseResult preparseResult;

	XercesValidationContext(PreparseResult preparseResult) {
		super();
		this.preparseResult = preparseResult;
	}

	@Override
	public String getSchemasId() {
		return preparseResult.getSchemasId();
	}

	public SymbolTable getSymbolTable() {
		return preparseResult.getSymbolTable();
	}

	public XMLGrammarPool getGrammarPool() {
		return preparseResult.getGrammarPool();
	}

	@Override
	public Set<String> getNamespaceSet() {
		return preparseResult.getNamespaceSet();
	}

	@Override
	public List<XSModel> getXsModels() {
		return preparseResult.getXsModels();
	}

}

class PreparseResult {
	private String schemasId;
	private SymbolTable symbolTable;
	private XMLGrammarPool grammarPool;
	private Set<String> namespaceSet;
	private List<XSModel> xsModels = null;

	public String getSchemasId() {
		return schemasId;
	}

	public void setSchemasId(String schemasId) {
		this.schemasId = schemasId;
	}

	public SymbolTable getSymbolTable() {
		return symbolTable;
	}

	public void setSymbolTable(SymbolTable symbolTable) {
		this.symbolTable = symbolTable;
	}

	public XMLGrammarPool getGrammarPool() {
		return grammarPool;
	}

	public void setGrammarPool(XMLGrammarPool grammarPool) {
		this.grammarPool = grammarPool;
	}

	public Set<String> getNamespaceSet() {
		return namespaceSet;
	}

	public void setNamespaceSet(Set<String> namespaceSet) {
		this.namespaceSet = namespaceSet;
	}

	public List<XSModel> getXsModels() {
		if (xsModels == null) {
			xsModels = new LinkedList<>();
			Grammar[] grammars = grammarPool.retrieveInitialGrammarSet(XMLGrammarDescription.XML_SCHEMA);
			for (int i = 0; i < grammars.length; i++) {
				xsModels.add(((XSGrammar) grammars[i]).toXSModel());
			}
		}
		return xsModels;
	}

	public void setXsModels(List<XSModel> xsModels) {
		this.xsModels = xsModels;
	}

}

class MyErrorHandler implements XMLErrorHandler {
	protected Logger log = LogUtil.getLogger(this);
	protected boolean warn = true;

	@Override
	public void warning(String domain, String key, XMLParseException e) throws XNIException {
		if (warn) {
			ConfigurationWarnings.getInstance().add(log, e.getMessage());
		}
	}

	@Override
	public void error(String domain, String key, XMLParseException e) throws XNIException {
		// In case the XSD doesn't exist throw an exception to prevent the
		// the adapter from starting.
		if (e.getMessage() != null
				&& e.getMessage().startsWith("schema_reference.4: Failed to read schema document '")) {
			throw e;
		}
		if (warn) {
			ConfigurationWarnings.getInstance().add(log, e.getMessage());
		}
	}

	@Override
	public void fatalError(String domain, String key, XMLParseException e) throws XNIException {
		if (warn) {
			ConfigurationWarnings.getInstance().add(log, e.getMessage());
		}
		throw new XNIException(e);
	}
}

