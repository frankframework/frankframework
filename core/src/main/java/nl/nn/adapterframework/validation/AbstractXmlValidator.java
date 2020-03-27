/*
   Copyright 2013, 2015, 2016 Nationale-Nederlanden

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


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.xerces.xs.XSModel;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * baseclass for validating input message against a XML-Schema.
 *
 * N.B. noNamespaceSchemaLocation may contain spaces, but not if the schema is stored in a .jar or .zip file on the class path.
 *
 * @author Johan Verrips IOS
 * @author Jaco de Groot
 */
public abstract class AbstractXmlValidator {
	protected static Logger log = LogUtil.getLogger(AbstractXmlValidator.class);

	public static final String XML_VALIDATOR_PARSER_ERROR_MONITOR_EVENT = "Invalid XML: parser error";
	public static final String XML_VALIDATOR_NOT_VALID_MONITOR_EVENT = "Invalid XML: does not comply to XSD";
	public static final String XML_VALIDATOR_VALID_MONITOR_EVENT = "valid XML";

	private ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();

	protected SchemasProvider schemasProvider;
	private boolean throwException = false;
	private boolean fullSchemaChecking = false;
	private String reasonSessionKey = "failureReason";
	private String xmlReasonSessionKey = "xmlFailureReason";

	private boolean validateFile = false;
	private String charset = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
	protected boolean warn = AppConstants.getInstance(configurationClassLoader).getBoolean("xmlValidator.warn", true);
	protected boolean needsInit = true;
	protected boolean lazyInit = AppConstants.getInstance(configurationClassLoader).getBoolean("xmlValidator.lazyInit", false);

	protected String logPrefix = "";
	protected boolean addNamespaceToSchema = false;
	protected String importedSchemaLocationsToIgnore;
	protected boolean useBaseImportedSchemaLocationsToIgnore = false;
	protected String importedNamespacesToIgnore;
	protected Boolean ignoreUnknownNamespaces;
	private boolean ignoreCaching = false;
	private String xmlSchemaVersion=null;
	

	/**
	 * Configure the XmlValidator
	 *
	 * @throws ConfigurationException when:
	 * <ul>
	 *     <li>the schema cannot be found</li>
	 *     <li><{@link #isThrowException()} is false and there is no forward defined for "failure"</li>
	 *     <li>when the parser does not accept setting the properties for validating</li>
	 * </ul>
	 */
	public void configure(String logPrefix) throws ConfigurationException {
		this.logPrefix = logPrefix;
		if (!lazyInit) {
			init();
		}
	}

	protected void init() throws ConfigurationException {
		if (needsInit) {
			needsInit = false;
		}
	}

	public void reset() {
		if (!needsInit) {
			needsInit = true;
		}
	}

	protected String handleFailures(XmlValidatorErrorHandler xmlValidatorErrorHandler, IPipeLineSession session, String event, Throwable t) throws XmlValidatorException {
		// A SAXParseException will already be reported by the parser to the
		// XmlValidatorErrorHandler through the ErrorHandler interface.
		if (t != null && !(t instanceof SAXParseException)) {
			xmlValidatorErrorHandler.addReason(t);
		}
		String fullReasons = xmlValidatorErrorHandler.getReasons();
		if (StringUtils.isNotEmpty(getReasonSessionKey())) {
			log.debug(getLogPrefix(session) + "storing reasons under sessionKey [" + getReasonSessionKey() + "]");
			session.put(getReasonSessionKey(), fullReasons);
		}
		if (StringUtils.isNotEmpty(getXmlReasonSessionKey())) {
			log.debug(getLogPrefix(session) + "storing reasons (in xml format) under sessionKey [" + getXmlReasonSessionKey() + "]");
			session.put(getXmlReasonSessionKey(), xmlValidatorErrorHandler.getXmlReasons());
		}
		if (isThrowException()) {
			throw new XmlValidatorException(fullReasons, t);
		}
		log.warn(getLogPrefix(session) + "validation failed: " + fullReasons, t);
		return event;
	}

	public ValidationContext createValidationContext(IPipeLineSession session, Set<List<String>> rootValidations, Map<List<String>, List<String>> invalidRootNamespaces) throws ConfigurationException, PipeRunException {
		// clear session variables
		if (StringUtils.isNotEmpty(getReasonSessionKey())) {
			log.debug(logPrefix + "removing contents of sessionKey [" + getReasonSessionKey() + "]");
			session.remove(getReasonSessionKey());
		}

		if (StringUtils.isNotEmpty(getXmlReasonSessionKey())) {
			log.debug(logPrefix + "removing contents of sessionKey [" + getXmlReasonSessionKey() + "]");
			session.remove(getXmlReasonSessionKey());
		}
		return null;
	}

	public abstract ValidatorHandler getValidatorHandler(IPipeLineSession session, ValidationContext context) throws ConfigurationException, PipeRunException;
	public abstract List<XSModel> getXSModels();

	/**
	 * @param input   the XML string to validate
	 * @param session a {@link IPipeLineSession pipeLineSession}
	 * @return MonitorEvent declared in{@link AbstractXmlValidator}
	 * @throws XmlValidatorException when <code>isThrowException</code> is true and a validationerror occurred.
	 */
	public String validate(Object input, IPipeLineSession session, String logPrefix, Set<List<String>> rootValidations, Map<List<String>, List<String>> invalidRootNamespaces, boolean resolveExternalEntities) throws XmlValidatorException, PipeRunException, ConfigurationException {
		ValidationContext context = createValidationContext(session, rootValidations, invalidRootNamespaces);
		ValidatorHandler validatorHandler = getValidatorHandler(session, context);
		return validate(input, session, logPrefix, validatorHandler, null, context, resolveExternalEntities);
	}

	public String validate(Object input, IPipeLineSession session, String logPrefix, ValidatorHandler validatorHandler, XMLFilterImpl filter, ValidationContext context) throws XmlValidatorException, PipeRunException, ConfigurationException {
		return validate(input, session, logPrefix, validatorHandler, filter, context, false);
	}

	public String validate(Object input, IPipeLineSession session, String logPrefix, ValidatorHandler validatorHandler, XMLFilterImpl filter, ValidationContext context, boolean resolveExternalEntities) throws XmlValidatorException, PipeRunException, ConfigurationException {

		if (filter != null) {
			filter.setContentHandler(context.getContentHandler());
			filter.setErrorHandler(context.getErrorHandler());
		} else {
			validatorHandler.setContentHandler(context.getContentHandler());
			validatorHandler.setErrorHandler(context.getErrorHandler());
		}

		InputSource is = getInputSource(Message.asMessage(input));

		return validate(is, validatorHandler, session, context, resolveExternalEntities);
	}

	public String validate(InputSource is, ValidatorHandler validatorHandler, IPipeLineSession session, ValidationContext context, boolean resolveExternalEntities) throws XmlValidatorException {
		try {
			XMLReader reader = XmlUtils.getXMLReader(true, resolveExternalEntities, validatorHandler);
			reader.setErrorHandler(context.getErrorHandler());

			reader.parse(is);
		} catch (IOException | SAXException | ParserConfigurationException e) {
			return finalizeValidation(context, session, e);
		}
		return finalizeValidation(context, session, null);
	}

	/**
	 * Evaluate the validation and set 'reason' session variables.
	 *
	 * @param context the validationContext of this attempt
	 * @param session the PipeLineSession
	 * @param t       the exception thrown by the validation, or null
	 * @return the result event, e.g. 'valid XML' or 'Invalid XML'
	 * @throws XmlValidatorException, when configured to do so
	 */
	public String finalizeValidation(ValidationContext context, IPipeLineSession session, Throwable t) throws XmlValidatorException {
		if (t != null) {
			return handleFailures(context.getErrorHandler(), session, XML_VALIDATOR_PARSER_ERROR_MONITOR_EVENT, t);
		}
		if (context.getErrorHandler().hasErrorOccured()) {
			return handleFailures(context.getErrorHandler(), session, XML_VALIDATOR_NOT_VALID_MONITOR_EVENT, null);
		}
		return XML_VALIDATOR_VALID_MONITOR_EVENT;
	}


	/**
	 * Sets schemas provider.
	 * @since 5.0
	 */
	public void setSchemasProvider(SchemasProvider schemasProvider) {
		this.schemasProvider = schemasProvider;
	}

	protected String getLogPrefix(IPipeLineSession session) {
		StringBuilder sb = new StringBuilder();
		sb.append(ClassUtils.nameOf(this)).append(' ');
		if (this instanceof INamedObject) {
			sb.append("[").append(((INamedObject) this).getName()).append("] ");
		}
		if (session != null) {
			sb.append("msgId [").append(session.getMessageId()).append("] ");
		}
		return sb.toString();
	}

	protected InputSource getInputSource(Message input) throws XmlValidatorException {
		final InputSource is;
		if (isValidateFile()) {
			String filename=null;
			try {
				filename = input.asString();
				is = new InputSource(StreamUtil.getCharsetDetectingInputStreamReader(new FileInputStream(filename), getCharset()));
			} catch (FileNotFoundException e) {
				throw new XmlValidatorException("could not find file [" + filename + "]", e);
			} catch (UnsupportedEncodingException e) {
				throw new XmlValidatorException("could not use charset [" + getCharset() + "] for file [" + filename + "]", e);
			} catch (IOException e) {
				throw new XmlValidatorException("could not determine filename", e);
			}
		} else {
			try {
				is = input.asInputSource();
			} catch (IOException e) {
				throw new XmlValidatorException("cannot obtain InputSource", e);
			}
		}
		return is;
	}


	/**
	 * Enable full schema grammar constraint checking, including checking which
	 * may be time-consuming or memory intensive. Currently, particle unique
	 * attribution constraint checking and particle derivation resriction
	 * checking are controlled by this option.
	 * <p>
	 * see property
	 * http://apache.org/xml/features/validation/schema-full-checking
	 * </p>
	 * Defaults to <code>false</code>;
	 */
	@IbisDoc({"Perform addional memory intensive checks", "<code>false</code>"})
	public void setFullSchemaChecking(boolean fullSchemaChecking) {
		this.fullSchemaChecking = fullSchemaChecking;
	}
	public boolean isFullSchemaChecking() {
		return fullSchemaChecking;
	}

	/**
	 * Indicates whether to throw an error (PipeRunexception) when the xml is not compliant.
	 */
	@IbisDoc({"Should the XmlValidator throw a PipeRunexception on a validation error (if not, a forward with name 'failure' should be defined.", "<code>false</code>"})
	public void setThrowException(boolean throwException) {
		this.throwException = throwException;
	}
	public boolean isThrowException() {
		return throwException;
	}

	/**
	 * The sessionkey to store the reasons of misvalidation in.
	 */
	@IbisDoc({"If set: key of session variable to store reasons of mis-validation in", "failureReason"})
	public void setReasonSessionKey(String reasonSessionKey) {
		this.reasonSessionKey = reasonSessionKey;
	}
	public String getReasonSessionKey() {
		return reasonSessionKey;
	}

	@IbisDoc({"Like <code>reasonSessionKey</code> but stores reasons in xml format and more extensive", "xmlFailureReason"})
	public void setXmlReasonSessionKey(String xmlReasonSessionKey) {
		this.xmlReasonSessionKey = xmlReasonSessionKey;
	}
	public String getXmlReasonSessionKey() {
		return xmlReasonSessionKey;
	}

	@IbisDoc({"When set <code>true</code>, the input is assumed to be the name of the file to be validated. Otherwise the input itself is validated", "<code>false</code>"})
	public void setValidateFile(boolean b) {
		validateFile = b;
	}
	public boolean isValidateFile() {
		return validateFile;
	}

	@IbisDoc({"Character set used for reading file, only used when <code>validateFile</code> is <code>true</code>", "utf-8"})
	public void setCharset(String string) {
		charset = string;
	}
	public String getCharset() {
		return charset;
	}

	@IbisDoc({"When set <code>true</code>, send warnings to logging and console about syntax problems in the configured schema('s)", "<code>true</code>"})
	public void setWarn(boolean warn) {
		this.warn = warn;
	}

	@IbisDoc({"When set <code>true</code>, the namespace from schemalocation is added to the schema document as targetnamespace", "<code>false</code>"})
	public void setAddNamespaceToSchema(boolean addNamespaceToSchema) {
		this.addNamespaceToSchema = addNamespaceToSchema;
	}
	public boolean isAddNamespaceToSchema() {
		return addNamespaceToSchema;
	}

	@IbisDoc({"Comma separated list of schemaLocations which are excluded from an import or include in the schema document", ""})
	public void setImportedSchemaLocationsToIgnore(String string) {
		importedSchemaLocationsToIgnore = string;
	}
	public String getImportedSchemaLocationsToIgnore() {
		return importedSchemaLocationsToIgnore;
	}

	@IbisDoc({"When set <code>true</code>, the comparison for importedSchemaLocationsToIgnore is done on base filename without any path", "<code>false</code>"})
	public void setUseBaseImportedSchemaLocationsToIgnore(boolean useBaseImportedSchemaLocationsToIgnore) {
		this.useBaseImportedSchemaLocationsToIgnore = useBaseImportedSchemaLocationsToIgnore;
	}
	public boolean isUseBaseImportedSchemaLocationsToIgnore() {
		return useBaseImportedSchemaLocationsToIgnore;
	}

	@IbisDoc({"Comma separated list of namespaces which are excluded from an import or include in the schema document", ""})
	public void setImportedNamespacesToIgnore(String string) {
		importedNamespacesToIgnore = string;
	}
	public String getImportedNamespacesToIgnore() {
		return importedNamespacesToIgnore;
	}

	@IbisDoc({"Ignore namespaces in the input message which are unknown", "<code>true</code> when schema or nonamespaceschemalocation is used, <code>false</code> otherwise"})
	public Boolean getIgnoreUnknownNamespaces() {
		return ignoreUnknownNamespaces;
	}
	public void setIgnoreUnknownNamespaces(boolean b) {
		this.ignoreUnknownNamespaces = b;
	}

	public void setIgnoreCaching(boolean ignoreCaching) {
		this.ignoreCaching = ignoreCaching;
	}
	public boolean isIgnoreCaching() {
		return ignoreCaching;
	}

	public void setLazyInit(boolean lazyInit) {
		this.lazyInit = lazyInit;
	}
	public boolean isLazyInit() {
		return lazyInit;
	}

	public void setXmlSchemaVersion(String xmlSchemaVersion) {
		this.xmlSchemaVersion = xmlSchemaVersion;
	}
	public String getXmlSchemaVersion() {
		return xmlSchemaVersion;
	}
	public boolean isXmlSchema1_0() {
		return getXmlSchemaVersion()==null || "1.0".equals(getXmlSchemaVersion());
	}

	/**
	 * This ClassLoader is set upon creation of the pipe, used to retrieve resources configured by the Ibis application.
	 */
	public ClassLoader getConfigurationClassLoader() {
		return configurationClassLoader;
	}
}
