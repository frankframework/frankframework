/*
   Copyright 2013-2017 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
package org.frankframework.validation;


import static org.apache.xerces.parsers.XMLGrammarCachingConfiguration.BIG_PRIME;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.impl.xs.SchemaGrammar;
import org.apache.xerces.jaxp.validation.XMLSchema11Factory;
import org.apache.xerces.jaxp.validation.XMLSchemaFactory;
import org.apache.xerces.parsers.SAXParser;
import org.apache.xerces.parsers.XMLGrammarPreparser;
import org.apache.xerces.util.SecurityManager;
import org.apache.xerces.util.ShadowedSymbolTable;
import org.apache.xerces.util.SymbolTable;
import org.apache.xerces.util.XMLGrammarPoolImpl;
import org.apache.xerces.xni.grammars.Grammar;
import org.apache.xerces.xni.grammars.XMLGrammarDescription;
import org.apache.xerces.xni.grammars.XMLGrammarPool;
import org.apache.xerces.xni.grammars.XSGrammar;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xs.XSModel;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.cache.EhCache;
import org.frankframework.configuration.ApplicationWarnings;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.statistics.HasApplicationContext;
import org.frankframework.util.AppConstants;


/**
 * Xerces based XML validator.
 *
 * N.B. noNamespaceSchemaLocation may contain spaces, but not if the schema is stored in a .jar or .zip file on the class path.
 * @author Johan Verrips IOS
 * @author Jaco de Groot
 */
public class XercesXmlValidator extends AbstractXmlValidator {

	private static final String DEFAULT_XML_SCHEMA_VERSION = "1.1";

	/** Property identifier: grammar pool. */
	public static final String GRAMMAR_POOL = Constants.XERCES_PROPERTY_PREFIX + Constants.XMLGRAMMAR_POOL_PROPERTY;

	/** Namespaces feature id (http://xml.org/sax/features/namespaces). */
	protected static final String NAMESPACES_FEATURE_ID = Constants.SAX_FEATURE_PREFIX + Constants.NAMESPACES_FEATURE;

	/** Validation feature id (http://xml.org/sax/features/validation). */
	protected static final String VALIDATION_FEATURE_ID = Constants.SAX_FEATURE_PREFIX + Constants.VALIDATION_FEATURE;

	/** Schema validation feature id (http://apache.org/xml/features/validation/schema). */
	protected static final String SCHEMA_VALIDATION_FEATURE_ID = Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_VALIDATION_FEATURE;

	/** External general entities feature id (http://xml.org/sax/features/external-general-entities). */
	protected static final String EXTERNAL_GENERAL_ENTITIES_FEATURE_ID = Constants.SAX_FEATURE_PREFIX + Constants.EXTERNAL_GENERAL_ENTITIES_FEATURE;

	/** External paramter entities feature id (http://xml.org/sax/features/external-general-entities). */
	protected static final String EXTERNAL_PARAMETER_ENTITIES_FEATURE_ID = Constants.SAX_FEATURE_PREFIX + Constants.EXTERNAL_PARAMETER_ENTITIES_FEATURE;

	/** Disallow doctype declarations feature id (http://apache.org/xml/features/disallow-doctype-decl). */
	protected static final String DISALLOW_DOCTYPE_DECL_FEATURE_ID = Constants.XERCES_FEATURE_PREFIX + Constants.DISALLOW_DOCTYPE_DECL_FEATURE;

	/** Schema full checking feature id (http://apache.org/xml/features/validation/schema-full-checking). */
	protected static final String SCHEMA_FULL_CHECKING_FEATURE_ID = Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_FULL_CHECKING;

	protected static final String SECURITY_MANAGER_PROPERTY_ID = Constants.XERCES_PROPERTY_PREFIX + Constants.SECURITY_MANAGER_PROPERTY;

	protected static final String XML_SCHEMA_VERSION_PROPERTY = Constants.XERCES_PROPERTY_PREFIX + Constants.XML_SCHEMA_VERSION_PROPERTY;

	private static final int maxInitialised = AppConstants.getInstance().getInt("xmlValidator.maxInitialised", -1);
	private static final boolean sharedSymbolTable = AppConstants.getInstance().getBoolean("xmlValidator.sharedSymbolTable", false);
	private static final int sharedSymbolTableSize = AppConstants.getInstance().getInt("xmlValidator.sharedSymbolTable.size", BIG_PRIME);
	private final int entityExpansionLimit = AppConstants.getInstance().getInt("xmlValidator.entityExpansionLimit", 100000);

	private static final AtomicLong counter = new AtomicLong();
	private final String preparseResultId;
	private PreparseResult preparseResult;

	private static EhCache<PreparseResult> cache;
	static {
		if (maxInitialised != -1) {
			cache = new EhCache<>();
			cache.setMaxElementsInMemory(maxInitialised);
			cache.setEternal(true);
			try {
				cache.configure("XercesXmlValidator");
				cache.open();
			} catch (ConfigurationException e) {
				cache = null;
				ApplicationWarnings.add(log, "Could not configure EhCache for XercesXmlValidator (xmlValidator.maxInitialised will be ignored)", e);
			}
		}
	}

	public XercesXmlValidator() {
		preparseResultId = "" + counter.getAndIncrement();
	}

	@Override
	public void configure(HasApplicationContext owner) throws ConfigurationException {
		if (StringUtils.isEmpty(getXmlSchemaVersion())) {
			setXmlSchemaVersion(AppConstants.getInstance(getConfigurationClassLoader()).getString("xml.schema.version", DEFAULT_XML_SCHEMA_VERSION));
			if (!isXmlSchema1_0() && !"1.1".equals(getXmlSchemaVersion())) {
				throw new ConfigurationException("class ("+this.getClass().getName()+") only supports XmlSchema version 1.0 and 1.1, no ["+getXmlSchemaVersion()+"]");
			}
		}
		super.configure(owner);
	}

	@Override
	public void start() {
		super.start();

		if (schemasProvider == null) {
			throw new IllegalStateException("No schema provider");
		}

		try {
			String schemasId = schemasProvider.getSchemasId();
			if (schemasId != null) {
				PreparseResult preparseResult = preparse();

				if (cache == null || isIgnoreCaching()) {
					this.preparseResult = preparseResult;
				} else {
					cache.put(preparseResultId, preparseResult);
				}
			}
		} catch (ConfigurationException e) {
			throw new LifecycleException(e);
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

	private PreparseResult preparse() throws ConfigurationException {
		return preparse(schemasProvider.getSchemasId(), schemasProvider.getSchemas());
	}


	private synchronized PreparseResult preparse(String schemasId, List<Schema> schemas) throws ConfigurationException {
		SymbolTable symbolTable = getSymbolTable();
		XMLGrammarPool grammarPool = new XMLGrammarPoolImpl();
		Set<String> namespaceSet = new HashSet<>();
		XMLGrammarPreparser preparser = new XMLGrammarPreparser(symbolTable);
		preparser.setEntityResolver(new IntraGrammarPoolEntityResolver(this, schemas));
		preparser.registerPreparser(XMLGrammarDescription.XML_SCHEMA, null);
		preparser.setProperty(GRAMMAR_POOL, grammarPool);
		preparser.setFeature(NAMESPACES_FEATURE_ID, true);
		preparser.setFeature(VALIDATION_FEATURE_ID, true);
		preparser.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
		preparser.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, isFullSchemaChecking());
		try {
			preparser.setProperty(XML_SCHEMA_VERSION_PROPERTY, isXmlSchema1_0() ? Constants.W3C_XML_SCHEMA10EX_NS_URI : Constants.W3C_XML_SCHEMA11_NS_URI);
		} catch (NoSuchFieldError e) {
			String msg="Cannot set property ["+XML_SCHEMA_VERSION_PROPERTY+"], requested xmlSchemaVersion ["+getXmlSchemaVersion()+"] xercesVersion ["+org.apache.xerces.impl.Version.getVersion()+"]";
			if (isXmlSchema1_0()) {
				log.warn("{}, assuming XML Schema version 1.0 will be supported", msg, e);
			} else {
				throw new ConfigurationException(msg, e);
			}
		}
		XercesValidationErrorHandler errorHandler = new XercesValidationErrorHandler(getOwner()!=null ? getOwner() : this, warn);
		preparser.setErrorHandler(errorHandler);
		//namespaceSet.add(""); // allow empty namespace, to cover 'ElementFormDefault="Unqualified"'. N.B. beware, this will cause SoapValidator to miss validation failure of a non-namespaced SoapBody
		Set<Grammar> namespaceRegisteredGrammars = new HashSet<>();
		for (Schema schema : schemas) {
			Grammar grammar = preparse(preparser, schemasId, schema);
			registerNamespaces(grammar, namespaceSet, namespaceRegisteredGrammars);
		}
		grammarPool.lockPool();
		PreparseResult preparseResult = new PreparseResult();
		preparseResult.setSchemasId(schemasId);
		preparseResult.setSymbolTable(symbolTable);
		preparseResult.setGrammarPool(grammarPool);
		preparseResult.setNamespaceSet(namespaceSet);
		return preparseResult;
	}

	private static Grammar preparse(XMLGrammarPreparser preparser, String schemasId, Schema schema) throws ConfigurationException {
		try {
			return preparser.preparseGrammar(XMLGrammarDescription.XML_SCHEMA, schemaToXMLInputSource(schema));
		} catch (IOException e) {
			throw new ConfigurationException("cannot compile schema's [" + schemasId + "]", e);
		}
	}

	private static void registerNamespaces(Grammar grammar, Set<String> namespaces, Set<Grammar> namespaceRegisteredGrammars) {
		namespaceRegisteredGrammars.add(grammar);
		namespaces.add(grammar.getGrammarDescription().getNamespace());
		if (grammar instanceof SchemaGrammar schemaGrammar) {
			List<?> imported = schemaGrammar.getImportedGrammars();
			if (imported != null) {
				for (Object g : imported) {
					Grammar gr = (Grammar)g;
					if (!namespaceRegisteredGrammars.contains(gr)) {
						registerNamespaces(gr, namespaces, namespaceRegisteredGrammars);
					}
				}
			}
		}
	}

	protected PreparseResult getPreparseResult(PipeLineSession session) throws ConfigurationException, PipeRunException {
		PreparseResult preparseResult;
		String schemasId = schemasProvider.getSchemasId();
		if (schemasId == null) {
			schemasId = schemasProvider.getSchemasId(session);
			preparseResult = preparse(schemasId, schemasProvider.getSchemas(session));
		} else {
			if (cache == null || isIgnoreCaching()) {
				preparseResult = this.preparseResult; // this.preparseResult is set at start();
			} else {
				preparseResult = cache.get(preparseResultId);
				if (preparseResult == null) {
					preparseResult = preparse();
					cache.put(preparseResultId, preparseResult);
				}
			}
		}
		return preparseResult;
	}

	@Override
	public XercesValidationContext createValidationContext(PipeLineSession session, RootValidations rootValidations, Map<List<String>, List<String>> invalidRootNamespaces) throws ConfigurationException, PipeRunException {
		// clear session variables
		super.createValidationContext(session, rootValidations, invalidRootNamespaces);

		PreparseResult preparseResult = getPreparseResult(session);
		XercesValidationContext result = new XercesValidationContext(preparseResult);

		result.init(schemasProvider, preparseResult.getSchemasId(), preparseResult.getNamespaceSet(), rootValidations, invalidRootNamespaces, ignoreUnknownNamespaces);
		return result;
	}

	@Override
	public ValidatorHandler getValidatorHandler(PipeLineSession session, AbstractValidationContext context) throws ConfigurationException {
		ValidatorHandler validatorHandler;

		try {
			javax.xml.validation.Schema schemaObject;
			if (isXmlSchema1_0()) {
				XMLSchemaFactory schemaFactory = new XMLSchemaFactory();
				schemaObject = schemaFactory.newSchema(((XercesValidationContext) context).getGrammarPool());
			} else {
				XMLSchema11Factory schemaFactory = new XMLSchema11Factory();
				schemaObject = schemaFactory.newSchema(((XercesValidationContext) context).getGrammarPool());
			}

			validatorHandler=schemaObject.newValidatorHandler();
		} catch (SAXException e) {
			throw new ConfigurationException(logPrefix + " Cannot create schema", e);
		}
		try {
			//validatorHandler.setFeature(NAMESPACES_FEATURE_ID, true);
			validatorHandler.setFeature(VALIDATION_FEATURE_ID, true);
			validatorHandler.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
			validatorHandler.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, isFullSchemaChecking());
			validatorHandler.setFeature(DISALLOW_DOCTYPE_DECL_FEATURE_ID, true);


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
	public XMLReader createValidatingParser(PipeLineSession session, AbstractValidationContext context) throws XmlValidatorException {
		SymbolTable symbolTable = ((XercesValidationContext)context).getSymbolTable();
		XMLGrammarPool grammarPool = ((XercesValidationContext)context).getGrammarPool();

		XMLReader parser = new SAXParser(new ShadowedSymbolTable(symbolTable), grammarPool);
		try {
			parser.setFeature(NAMESPACES_FEATURE_ID, true);
			parser.setFeature(VALIDATION_FEATURE_ID, true);
			// parser.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true); // this feature is not recognized
//			parser.setFeature(EXTERNAL_GENERAL_ENTITIES_FEATURE_ID, false); // this one appears to be not working
//			parser.setFeature(EXTERNAL_PARAMETER_ENTITIES_FEATURE_ID, false);
//			parser.setFeature(DISALLOW_DOCTYPE_DECL_FEATURE_ID, true);
			parser.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
			parser.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, isFullSchemaChecking());
			parser.setErrorHandler(context.getErrorHandler());
			org.apache.xerces.util.SecurityManager mgr = new org.apache.xerces.util.SecurityManager();
			mgr.setEntityExpansionLimit(entityExpansionLimit);
			parser.setProperty(SECURITY_MANAGER_PROPERTY_ID, mgr);
		} catch (SAXNotRecognizedException e) {
			throw new XmlValidatorException(logPrefix + " parser does not recognize necessary feature", e);
		} catch (SAXNotSupportedException e) {
			throw new XmlValidatorException(logPrefix + " parser does not support necessary feature", e);
		}
		return parser;
	}


	private static XMLInputSource schemaToXMLInputSource(Schema schema) throws IOException {
		// SystemId is needed in case the schema has an import. Maybe we should
		// already resolve this at the SchemaProvider side (except when
		// noNamespaceSchemaLocation is being used this is already done in
		// (Wsdl)XmlValidator (using
		// mergeXsdsGroupedByNamespaceToSchemasWithoutIncludes)).
		// See comment in method XmlValidator.getSchemas() too.
		// See ClassLoaderXmlEntityResolver too.
		return new XMLInputSource(null, schema.getSystemId(), null, schema.getReader(), null);
	}

	@Override
	public List<XSModel> getXSModels() {
		return preparseResult.getXsModels();
	}

}


class XercesValidationContext extends AbstractValidationContext {

	private final PreparseResult preparseResult;

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
	private @Getter @Setter String schemasId;
	private @Getter @Setter SymbolTable symbolTable;
	private @Getter @Setter XMLGrammarPool grammarPool;
	private @Getter @Setter Set<String> namespaceSet;
	private @Setter List<XSModel> xsModels = null;

	public List<XSModel> getXsModels() {
		if (xsModels == null) {
			xsModels = new ArrayList<>();
			Grammar[] grammars = grammarPool.retrieveInitialGrammarSet(XMLGrammarDescription.XML_SCHEMA);
			for (Grammar grammar : grammars) {
				xsModels.add(((XSGrammar) grammar).toXSModel());
			}
		}
		return xsModels;
	}

}
