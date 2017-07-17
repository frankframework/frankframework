/*
   Copyright 2017 Nationale-Nederlanden

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang.StringUtils;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.impl.xs.SchemaGrammar;
import org.apache.xerces.parsers.SAXParser;
import org.apache.xerces.parsers.XMLGrammarPreparser;
import org.apache.xerces.util.SymbolTable;
import org.apache.xerces.util.XMLGrammarPoolImpl;
import org.apache.xerces.xni.grammars.Grammar;
import org.apache.xerces.xni.grammars.XMLGrammarDescription;
import org.apache.xerces.xni.grammars.XMLGrammarPool;
import org.apache.xerces.xni.grammars.XSGrammar;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xs.XSModel;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

import nl.nn.adapterframework.cache.EhCache;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.util.AppConstants;


/**
 * Xerces based XML validator with ValidatorHandler and XSModel extension.
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
public class XercesJavaxXmlValidator extends AbstractXmlValidator {

	/** Property identifier: grammar pool. */
	public static final String GRAMMAR_POOL = Constants.XERCES_PROPERTY_PREFIX + Constants.XMLGRAMMAR_POOL_PROPERTY;

	/** Namespaces feature id (http://xml.org/sax/features/namespaces). */
	protected static final String NAMESPACES_FEATURE_ID = Constants.SAX_FEATURE_PREFIX + Constants.NAMESPACES_FEATURE;

	/** Validation feature id (http://xml.org/sax/features/validation). */
	protected static final String VALIDATION_FEATURE_ID = Constants.SAX_FEATURE_PREFIX + Constants.VALIDATION_FEATURE;

	/** Schema validation feature id (http://apache.org/xml/features/validation/schema). */
	protected static final String SCHEMA_VALIDATION_FEATURE_ID = Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_VALIDATION_FEATURE;

	/** Schema full checking feature id (http://apache.org/xml/features/validation/schema-full-checking). */
	protected static final String SCHEMA_FULL_CHECKING_FEATURE_ID = Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_FULL_CHECKING;

	private static final int maxInitialised = AppConstants.getInstance().getInt("xmlValidator.maxInitialised", -1);

	private static EhCache cache;
	static {
		if (maxInitialised != -1) {
			cache = new EhCache();
			cache.setMaxElementsInMemory(maxInitialised);
			cache.setEternal(true);
			try {
				cache.configure("XercesJavaxXmlValidator");
			} catch (ConfigurationException e) {
				cache = null;
				ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
				configWarnings.add(log,
						"Could not configure EhCache for XercesJavaxXmlValidator (xmlValidator.maxInitialised will be ignored)",
						e);
			}
			cache.open();
		}
	}
	private static AtomicLong counter = new AtomicLong();
	private String preparseResultId;
	private PreparseResult2 preparseResult;

	public XercesJavaxXmlValidator() {
		preparseResultId = "" + counter.getAndIncrement();
	}

	@Override
	protected void init() throws ConfigurationException {
		if (needsInit) {
			super.init();
			if (schemasProvider == null) throw new IllegalStateException("No schema provider");
			String schemasId = schemasProvider.getSchemasId();
			if (schemasId != null) {
				PreparseResult2 preparseResult = preparse(schemasId, schemasProvider.getSchemas());
				if (cache == null || isIgnoreCaching()) {
					this.preparseResult = preparseResult;
				} else {
					cache.putObject(preparseResultId, preparseResult);
				}
			}
		}
	}

	private synchronized PreparseResult2 preparse(String schemasId, List<Schema> schemas) throws ConfigurationException {
		SymbolTable symbolTable = new SymbolTable(BIG_PRIME);
		XMLGrammarPool grammarPool = new XMLGrammarPoolImpl();
		Set<String> namespaceSet = new HashSet<String>();
		XMLGrammarPreparser preparser = new XMLGrammarPreparser(symbolTable);
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
		javax.xml.validation.Schema schemaObject=getSchemaObject(schemasId, schemasProvider.getSchemas());
		PreparseResult2 preparseResult = new PreparseResult2();
		preparseResult.setSchemasId(schemasId);
		preparseResult.setGrammarPool(grammarPool);
		preparseResult.setNamespaceSet(namespaceSet);
		preparseResult.setSchemaObject(schemaObject);
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
			List imported = ((SchemaGrammar)grammar).getImportedGrammars();
			if (imported != null) {
				for (Object g : imported) {
					Grammar gr = (Grammar)g;
					registerNamespaces(gr, namespaces);
				}
			}
		}
	}


	
	
	public ValidatorHandler getValidatorHandler(IPipeLineSession session) throws ConfigurationException, PipeRunException {
		PreparseResult2 preparseResult = getPreparseResult(session);
		javax.xml.validation.Schema schemaObject=preparseResult.getSchemaObject();
	
		ValidatorHandler validatorHandler=schemaObject.newValidatorHandler();
		try {
			//validatorHandler.setFeature(NAMESPACES_FEATURE_ID, true);
			validatorHandler.setFeature(VALIDATION_FEATURE_ID, true);
			validatorHandler.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
			validatorHandler.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, isFullSchemaChecking());
		} catch (SAXNotRecognizedException e) {
			throw new ConfigurationException(logPrefix + "ValidatorHandler does not recognize necessary feature", e);
		} catch (SAXNotSupportedException e) {
			throw new ConfigurationException(logPrefix + "ValidatorHandler does not support necessary feature", e);
		}
		return validatorHandler;
	}
	
	public List<XSModel> getXSModels(IPipeLineSession session) throws ConfigurationException, PipeRunException {
		PreparseResult2 preparseResult = getPreparseResult(session);
		XMLGrammarPool grammarPool = preparseResult.getGrammarPool();
		Grammar[] grammars=grammarPool.retrieveInitialGrammarSet(XMLGrammarDescription.XML_SCHEMA);
		List<XSModel> result = new LinkedList<XSModel>();
		for(int i=0;i<grammars.length;i++) {
			result.add(((XSGrammar)grammars[i]).toXSModel());
		}
		return result;
	}
	
	protected PreparseResult2 getPreparseResult(IPipeLineSession session) throws ConfigurationException, PipeRunException {
		PreparseResult2 preparseResult;
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
				preparseResult = (PreparseResult2)cache.getObject(preparseResultId);
				if (preparseResult == null) {
					preparseResult = preparse(schemasId, schemasProvider.getSchemas());
					cache.putObject(preparseResultId, preparseResult);
				}
			}
		}
		return preparseResult;
	}

	
	
	/**
	 * Validate the XML string
	 * @param input a String
	 * @param session a {@link nl.nn.adapterframework.core.IPipeLineSession pipeLineSession}
	 * @return MonitorEvent declared in{@link AbstractXmlValidator}
	 * @throws XmlValidatorException when <code>isThrowException</code> is true and a validationerror occurred.
	 * @throws PipeRunException
	 * @throws ConfigurationException
	 */
	@Override
	public String validate(Object input, IPipeLineSession session, String logPrefix) throws XmlValidatorException, PipeRunException, ConfigurationException {
		return validate(input, session, logPrefix, getValidatorHandler(session), null);
	}
	
	public String validate(Object input, IPipeLineSession session, String logPrefix, ValidatorHandler validatorHandler, XMLFilterImpl filter) throws XmlValidatorException, PipeRunException, ConfigurationException {
		if (StringUtils.isNotEmpty(getReasonSessionKey())) {
			log.debug(logPrefix+ "removing contents of sessionKey ["+getReasonSessionKey()+ "]");
			session.remove(getReasonSessionKey());
		}

		if (StringUtils.isNotEmpty(getXmlReasonSessionKey())) {
			log.debug(logPrefix+ "removing contents of sessionKey ["+getXmlReasonSessionKey()+ "]");
			session.remove(getXmlReasonSessionKey());
		}

		PreparseResult2 preparseResult = getPreparseResult(session);

		Set<String> namespacesSet = preparseResult.getNamespaceSet();
		String schemasId = preparseResult.getSchemasId();

		String mainFailureMessage = "Validation using "
				+ schemasProvider.getClass().getSimpleName() + " with '"
				+ schemasId + "' failed";

		XmlValidatorContentHandler xmlValidatorContentHandler =
				new XmlValidatorContentHandler(namespacesSet, rootValidations,
						invalidRootNamespaces, getIgnoreUnknownNamespaces());
		XmlValidatorErrorHandler xmlValidatorErrorHandler =
				new XmlValidatorErrorHandler(xmlValidatorContentHandler,
						mainFailureMessage);
		xmlValidatorContentHandler.setXmlValidatorErrorHandler(xmlValidatorErrorHandler);

		if (filter!=null) {
			filter.setContentHandler(xmlValidatorContentHandler);
			filter.setErrorHandler(xmlValidatorErrorHandler);
//			validatorHandler.setContentHandler(filter);
//			validatorHandler.setErrorHandler(filter);
		} else {
			validatorHandler.setContentHandler(xmlValidatorContentHandler);
			validatorHandler.setErrorHandler(xmlValidatorErrorHandler);
		}
		
		XMLReader parser = new SAXParser();
		parser.setErrorHandler(xmlValidatorErrorHandler);
		parser.setContentHandler(validatorHandler);
		try {
			parser.setFeature(NAMESPACES_FEATURE_ID, true);
			parser.setFeature(VALIDATION_FEATURE_ID, false);
			parser.setFeature(SCHEMA_VALIDATION_FEATURE_ID, false);
			parser.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, false);
		} catch (SAXNotRecognizedException e) {
			throw new XmlValidatorException(logPrefix + "parser does not recognize necessary feature", e);
		} catch (SAXNotSupportedException e) {
			throw new XmlValidatorException(logPrefix + "parser does not support necessary feature", e);
		}

		InputSource is = getInputSource(input);

		try {
			parser.parse(is);
		} catch (Exception e) {
			return handleFailures(xmlValidatorErrorHandler,
					session, XML_VALIDATOR_PARSER_ERROR_MONITOR_EVENT, e);
		}

		if (xmlValidatorErrorHandler.hasErrorOccured()) {
			return handleFailures(xmlValidatorErrorHandler, session,
					XML_VALIDATOR_NOT_VALID_MONITOR_EVENT, null);
		}
		return XML_VALIDATOR_VALID_MONITOR_EVENT;
	}


	private static XMLInputSource stringToXMLInputSource(Schema schema) throws IOException, ConfigurationException {
		// SystemId is needed in case the schema has an import. Maybe we should
		// already resolve this at the SchemaProvider side (except when
		// noNamespaceSchemaLocation is being used this is already done in
		// (Wsdl)XmlValidator (using
		// mergeXsdsGroupedByNamespaceToSchemasWithoutIncludes)).
		// See comment in method XmlValidator.getSchemas() too.
		return new XMLInputSource(null, schema.getSystemId(), null, schema.getInputStream(), null);
	}

	/**
	 * Returns the {@link Schema} associated with this validator. This ia an XSD schema containing knowledge about the
	 * schema source as returned by {@link #getSchemaSources(List)}
	 * @throws ConfigurationException
	 */
	protected synchronized javax.xml.validation.Schema getSchemaObject(String schemasId, List<nl.nn.adapterframework.validation.Schema> schemas) throws  ConfigurationException {
//		Schema schema = javaxSchemas.get(schemasId);
		javax.xml.validation.Schema schema=null;
		if (schema == null) {
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			factory.setResourceResolver(new LSResourceResolver() {
				public LSInput resolveResource(String s, String s1, String s2, String s3, String s4) {
					return null;
				}
			});
			try {
				Collection<Source> sources = getSchemaSources(schemas);
				schema = factory.newSchema(sources.toArray(new Source[sources.size()]));
//				javaxSchemas.put(schemasId, schema);
			} catch (Exception e) {
				throw new ConfigurationException("cannot read schema's [" + schemasId + "]", e);
			}
		}
		return schema;
	}

	protected List<Source> getSchemaSources(List<nl.nn.adapterframework.validation.Schema> schemas) throws IOException, XMLStreamException, ConfigurationException {
		List<Source> result = new ArrayList<Source>();
		for (nl.nn.adapterframework.validation.Schema schema : schemas) {
			result.add(new StreamSource(schema.getInputStream(), schema.getSystemId()));
		}
		return result;
	}

	
	
}


class PreparseResult2 {
	private String schemasId;
	private XMLGrammarPool grammarPool;
	private Set<String> namespaceSet;
	private javax.xml.validation.Schema schemaObject;

	public String getSchemasId() {
		return schemasId;
	}
	public void setSchemasId(String schemasId) {
		this.schemasId = schemasId;
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
	
	public javax.xml.validation.Schema getSchemaObject() {
		return schemaObject;
	}
	public void setSchemaObject(javax.xml.validation.Schema schemaObject) {
		this.schemaObject = schemaObject;
	}
}