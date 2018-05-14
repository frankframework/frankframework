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


import static org.apache.xerces.parsers.XMLGrammarCachingConfiguration.BIG_PRIME;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.validation.ValidatorHandler;

import nl.nn.adapterframework.cache.EhCache;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.validation.xerces_2_11.XMLSchemaFactory;

import org.apache.log4j.Logger;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.impl.xs.SchemaGrammar;
//import org.apache.xerces.jaxp.validation.XMLSchemaFactory;
import org.apache.xerces.parsers.SAXParser;
import org.apache.xerces.parsers.XMLGrammarPreparser;
import org.apache.xerces.util.ShadowedSymbolTable;
import org.apache.xerces.util.SymbolTable;
import org.apache.xerces.util.XMLGrammarPoolImpl;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.grammars.Grammar;
import org.apache.xerces.xni.grammars.XMLGrammarDescription;
import org.apache.xerces.xni.grammars.XMLGrammarPool;
import org.apache.xerces.xni.grammars.XSGrammar;
import org.apache.xerces.xni.parser.XMLErrorHandler;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParseException;
import org.apache.xerces.xs.XSModel;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;


/**
 * Xerces based XML validator.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.XmlValidator</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFullSchemaChecking(boolean) fullSchemaChecking}</td><td>Perform addional memory intensive checks</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setThrowException(boolean) throwException}</td><td>Should the XmlValidator throw a PipeRunException on a validation error (if not, a forward with name "failure" should be defined.</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setReasonSessionKey(String) reasonSessionKey}</td><td>if set: key of session variable to store reasons of mis-validation in</td><td>failureReason</td></tr>
 * <tr><td>{@link #setXmlReasonSessionKey(String) xmlReasonSessionKey}</td><td>like <code>reasonSessionKey</code> but stores reasons in xml format and more extensive</td><td>xmlFailureReason</td></tr>
 * <tr><td>{@link #setRoot(String) root}</td><td>name of the root element</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setValidateFile(boolean) validateFile}</td><td>when set <code>true</code>, the input is assumed to be the name of the file to be validated. Otherwise the input itself is validated</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setCharset(String) charset}</td><td>characterset used for reading file, only used when {@link #setValidateFile(boolean) validateFile} is <code>true</code></td><td>UTF-8</td></tr>
 * <tr><td>{@link #setWarn(boolean) warn}</td><td>when set <code>true</code>, send warnings to logging and console about syntax problems in the configured schema('s)</td><td><code>true</code></td></tr>
 * </table>
 * <p><b>Exits:</b>
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
 * @author Johan Verrips IOS
 * @author Jaco de Groot
 */
public class XercesXmlValidator extends AbstractXmlValidator {

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
	protected static final String DISSALLOW_DOCTYPE_DECL_FEATURE_ID = Constants.XERCES_FEATURE_PREFIX + Constants.DISALLOW_DOCTYPE_DECL_FEATURE;

	/** Schema full checking feature id (http://apache.org/xml/features/validation/schema-full-checking). */
	protected static final String SCHEMA_FULL_CHECKING_FEATURE_ID = Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_FULL_CHECKING;

	protected static final String SECURITY_MANAGER_PROPERTY_ID = Constants.XERCES_PROPERTY_PREFIX + Constants.SECURITY_MANAGER_PROPERTY;

	private static final int maxInitialised = AppConstants.getInstance().getInt("xmlValidator.maxInitialised", -1);
	private static final boolean sharedSymbolTable = AppConstants.getInstance().getBoolean("xmlValidator.sharedSymbolTable", false);
	private static final int sharedSymbolTableSize = AppConstants.getInstance().getInt("xmlValidator.sharedSymbolTable.size", BIG_PRIME);
	private int entityExpansionLimit = AppConstants.getInstance().getInt("xmlValidator.entityExpansionLimit", 100000);
	private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

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
	private static AtomicLong counter = new AtomicLong();
	private String preparseResultId;
	private PreparseResult preparseResult;

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

    private static class SymbolTableSingletonHelper{
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
		Set<String> namespaceSet = new HashSet<String>();
		XMLGrammarPreparser preparser = new XMLGrammarPreparser(symbolTable);
		preparser.setEntityResolver(new ClassLoaderXmlEntityResolver(classLoader));
		preparser.registerPreparser(XMLGrammarDescription.XML_SCHEMA, null);
		preparser.setProperty(GRAMMAR_POOL, grammarPool);
		preparser.setFeature(NAMESPACES_FEATURE_ID, true);
		preparser.setFeature(VALIDATION_FEATURE_ID, true);
		preparser.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
		preparser.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, isFullSchemaChecking());
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
			return preparser.preparseGrammar(XMLGrammarDescription.XML_SCHEMA,
					stringToXMLInputSource(schema));
		} catch (IOException e) {
			throw new ConfigurationException("cannot compile schema's ["
					+ schemasId + "]", e);
		}
	}

	private static void registerNamespaces(Grammar grammar, Set<String> namespaces) {
		namespaces.add(grammar.getGrammarDescription().getNamespace());
		if (grammar instanceof SchemaGrammar) {
			List<?> imported = ((SchemaGrammar)grammar).getImportedGrammars();
			if (imported != null) {
				for (Object g : imported) {
					Grammar gr = (Grammar)g;
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
				preparseResult = (PreparseResult)cache.getObject(preparseResultId);
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
	

	public ValidatorHandler getValidatorHandler(IPipeLineSession session, ValidationContext context) throws ConfigurationException, PipeRunException {

		ValidatorHandler validatorHandler=null;
		try {
			XMLSchemaFactory schemaFactory = new XMLSchemaFactory();
			javax.xml.validation.Schema schemaObject=schemaFactory.newSchema(((XercesValidationContext)context).getGrammarPool());
			validatorHandler=schemaObject.newValidatorHandler();
		} catch (SAXException e) {
			throw new ConfigurationException(logPrefix + "Cannot create schema", e);
		}
		try {
			//validatorHandler.setFeature(NAMESPACES_FEATURE_ID, true);
			validatorHandler.setFeature(VALIDATION_FEATURE_ID, true);
			validatorHandler.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
			validatorHandler.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, isFullSchemaChecking());
			validatorHandler.setErrorHandler(context.getErrorHandler());
		} catch (SAXNotRecognizedException e) {
			throw new ConfigurationException(logPrefix + "ValidatorHandler does not recognize necessary feature", e);
		} catch (SAXNotSupportedException e) {
			throw new ConfigurationException(logPrefix + "ValidatorHandler does not support necessary feature", e);
		}
		return validatorHandler;
	}
	public XMLReader createValidatingParser(IPipeLineSession session, ValidationContext context) throws XmlValidatorException, ConfigurationException, PipeRunException {
		SymbolTable symbolTable = ((XercesValidationContext)context).getSymbolTable();
		XMLGrammarPool grammarPool = ((XercesValidationContext)context).getGrammarPool();

		XMLReader parser = new SAXParser(new ShadowedSymbolTable(symbolTable), grammarPool);
		try {
			parser.setFeature(NAMESPACES_FEATURE_ID, true);
			parser.setFeature(VALIDATION_FEATURE_ID, true);
			// parser.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true); // this feature is not recognized
//			parser.setFeature(EXTERNAL_GENERAL_ENTITIES_FEATURE_ID, false); // this one appears to be not working
//			parser.setFeature(EXTERNAL_PARAMETER_ENTITIES_FEATURE_ID, false);
//			parser.setFeature(DISSALLOW_DOCTYPE_DECL_FEATURE_ID, true);
			parser.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
			parser.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, isFullSchemaChecking());
			parser.setErrorHandler(context.getErrorHandler());
			org.apache.xerces.util.SecurityManager mgr = new org.apache.xerces.util.SecurityManager();
			mgr.setEntityExpansionLimit(entityExpansionLimit);
			parser.setProperty(SECURITY_MANAGER_PROPERTY_ID, mgr);
		} catch (SAXNotRecognizedException e) {
			throw new XmlValidatorException(logPrefix + "parser does not recognize necessary feature", e);
		} catch (SAXNotSupportedException e) {
			throw new XmlValidatorException(logPrefix + "parser does not support necessary feature", e);
		}
		return parser;
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

}


class XercesValidationContext extends ValidationContext {

	private PreparseResult preparseResult;
	
	XercesValidationContext(PreparseResult preparseResult) {
		super();
		this.preparseResult=preparseResult;
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
	private List<XSModel> xsModels=null;

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
		if (xsModels==null) {
			xsModels=new LinkedList<XSModel>();
			Grammar[] grammars=grammarPool.retrieveInitialGrammarSet(XMLGrammarDescription.XML_SCHEMA);
			for(int i=0;i<grammars.length;i++) {
				xsModels.add(((XSGrammar)grammars[i]).toXSModel());
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

