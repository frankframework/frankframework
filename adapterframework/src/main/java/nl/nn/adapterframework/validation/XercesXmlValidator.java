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
package nl.nn.adapterframework.validation;


import static org.apache.xerces.parsers.XMLGrammarCachingConfiguration.BIG_PRIME;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;

import org.apache.commons.lang.StringUtils;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.impl.xs.SchemaGrammar;
import org.apache.xerces.parsers.SAXParser;
import org.apache.xerces.parsers.XMLGrammarPreparser;
import org.apache.xerces.util.ShadowedSymbolTable;
import org.apache.xerces.util.SymbolTable;
import org.apache.xerces.util.XMLGrammarPoolImpl;
import org.apache.xerces.xni.grammars.Grammar;
import org.apache.xerces.xni.grammars.XMLGrammarDescription;
import org.apache.xerces.xni.grammars.XMLGrammarPool;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.xml.sax.InputSource;
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
 * <tr><td>{@link #setSchema(String) schema}</td><td>The filename of the schema on the classpath. See doc on the method. (effectively the same as noNamespaceSchemaLocation)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNoNamespaceSchemaLocation(String) noNamespaceSchemaLocation}</td><td>A URI reference as a hint as to the location of a schema document with no target namespace. See doc on the method.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSchemaLocation(String) schemaLocation}</td><td>Pairs of URI references (one for the namespace name, and one for a hint as to the location of a schema document defining names for that namespace name). See doc on the method.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSchemaSessionKey(String) schemaSessionKey}</td><td>key of session variable holding dynamic value of noNamespaceSchemaLocation</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFullSchemaChecking(boolean) fullSchemaChecking}</td><td>Perform addional memory intensive checks</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setThrowException(boolean) throwException}</td><td>Should the XmlValidator throw a PipeRunException on a validation error (if not, a forward with name "failure" should be defined.</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setReasonSessionKey(String) reasonSessionKey}</td><td>if set: key of session variable to store reasons of mis-validation in</td><td>none</td></tr>
 * <tr><td>{@link #setXmlReasonSessionKey(String) xmlReasonSessionKey}</td><td>like <code>reasonSessionKey</code> but stores reasons in xml format and more extensive</td><td>none</td></tr>
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
 * @version $Id$
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

    /** Schema full checking feature id (http://apache.org/xml/features/validation/schema-full-checking). */
    protected static final String SCHEMA_FULL_CHECKING_FEATURE_ID = Constants.XERCES_FEATURE_PREFIX + Constants.SCHEMA_FULL_CHECKING;

    private Map<String, SymbolTable> symbolTables =  new ConcurrentHashMap<String, SymbolTable>();
    private Map<String, XMLGrammarPool> grammarPools = new ConcurrentHashMap<String, XMLGrammarPool>();
    private Map<String, Set<String>> namespaceSets = new ConcurrentHashMap<String, Set<String>>();

	@Override
	protected void init() throws ConfigurationException {
		if (needsInit) {
			super.init();
			String schemasId = null;
			schemasId = schemasProvider.getSchemasId();
			if (schemasId != null) {
				preparse(schemasId, schemasProvider.getSchemas());
			}
		}
	}

	private synchronized void preparse(String schemasId, List<Schema> schemas) throws ConfigurationException {
		if (symbolTables.get(schemasId) == null) {
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
			// Loop over the definitions until nothing changes
			// This makes sure that the _order_ is not important in the 'schemas'.
			errorHandler.throwRetryException = true;
			boolean changes;
			do {
				changes = false;
				for (Iterator<Schema> i = schemas.iterator(); i.hasNext();) {
					Schema schema = i.next();
					try {
						Grammar grammar = preparse(preparser, schemasId, schema);
						registerNamespaces(grammar, namespaceSet);
						changes = true;
						i.remove();
					} catch (RetryException e) {
						// Try in next iteration
					}
				}
			} while (changes);
			// loop the remaining ones, they seem to be unresolvable, so let the exception go then
			errorHandler.throwRetryException = false;
			for (Schema schema : schemas) {
				Grammar grammar = preparse(preparser, schemasId, schema);
				registerNamespaces(grammar, namespaceSet);
			}
			grammarPool.lockPool();
			symbolTables.put(schemasId, symbolTable);
			grammarPools.put(schemasId, grammarPool);
			namespaceSets.put(schemasId, namespaceSet);
		}
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

	/**
	 * Validate the XML string
	 * @param input a String
	 * @param session a {@link nl.nn.adapterframework.core.PipeLineSession Pipelinesession}
	 * @return MonitorEvent declared in{@link AbstractXmlValidator}
	 * @throws XmlValidatorException when <code>isThrowException</code> is true and a validationerror occurred.
	 * @throws PipeRunException 
	 * @throws ConfigurationException 
	 */
	@Override
	public String validate(Object input, IPipeLineSession session, String logPrefix) throws XmlValidatorException, PipeRunException, ConfigurationException {
		try {
			init();
		} catch (ConfigurationException e) {
			throw new XmlValidatorException(e.getMessage(), e);
		}
		if (StringUtils.isNotEmpty(getReasonSessionKey())) {
			log.debug(logPrefix+ "removing contents of sessionKey ["+getReasonSessionKey()+ "]");
			session.remove(getReasonSessionKey());
		}

		if (StringUtils.isNotEmpty(getXmlReasonSessionKey())) {
			log.debug(logPrefix+ "removing contents of sessionKey ["+getXmlReasonSessionKey()+ "]");
			session.remove(getXmlReasonSessionKey());
		}

		String schemasId = schemasProvider.getSchemasId();
		if (schemasId == null) {
			schemasId = schemasProvider.getSchemasId(session);
			preparse(schemasId, schemasProvider.getSchemas(session));
		}

		SymbolTable symbolTable = symbolTables.get(schemasId);
		XMLGrammarPool grammarPool = grammarPools.get(schemasId);
		Set<String> namespacesSet = namespaceSets.get(schemasId);

		String mainFailureMessage = "Validation using "
				+ schemasProvider.getClass().getSimpleName() + " with '"
				+ schemasId + "' failed";

		XmlValidatorContentHandler xmlValidatorContentHandler =
				new XmlValidatorContentHandler(namespacesSet, rootValidations,
						getIgnoreUnknownNamespaces());
		XmlValidatorErrorHandler xmlValidatorErrorHandler =
				new XmlValidatorErrorHandler(xmlValidatorContentHandler,
						mainFailureMessage);
		xmlValidatorContentHandler.setXmlValidatorErrorHandler(xmlValidatorErrorHandler);
		XMLReader parser = new SAXParser(new ShadowedSymbolTable(symbolTable),
				grammarPool);
		parser.setErrorHandler(xmlValidatorErrorHandler);
		parser.setContentHandler(xmlValidatorContentHandler);
		try {
			parser.setFeature(NAMESPACES_FEATURE_ID, true);
			parser.setFeature(VALIDATION_FEATURE_ID, true);
			parser.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
			parser.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, isFullSchemaChecking());
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

	private static XMLInputSource stringToXMLInputSource(Schema schema) throws IOException {
		InputStream inputStream = schema.getInputStream();
		// SystemId is needed in case the schema has an import. Maybe we should
		// already resolve this at the SchemaProvider side (when using
		// addNamespaceToSchema this is now already done).
		if (inputStream != null) {
			return new XMLInputSource(null, schema.getSystemId(), null, inputStream, null);
		} else {
			return new XMLInputSource(null, schema.getSystemId(), null, schema.getReader(), null);
		}
	}

}
