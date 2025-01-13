/*
   Copyright 2013, 2015, 2016 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.xs.XSModel;
import org.springframework.context.ApplicationContext;
import org.springframework.context.Lifecycle;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.XMLFilterImpl;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.SuppressKeys;
import org.frankframework.core.FrankElement;
import org.frankframework.core.HasApplicationContext;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.XmlUtils;

/**
 * baseclass for validating input message against a XML Schema.
 *
 * N.B. noNamespaceSchemaLocation may contain spaces, but not if the schema is stored in a .jar or .zip file on the class path.
 *
 * @author Johan Verrips IOS
 * @author Jaco de Groot
 */
public abstract class AbstractXmlValidator implements FrankElement, Lifecycle {
	protected static Logger log = LogUtil.getLogger(AbstractXmlValidator.class);

	public enum ValidationResult {
		PARSER_ERROR("Invalid XML: parser error"),
		INVALID("Invalid XML: does not comply to XSD"),
		VALID_WITH_WARNINGS("valid XML with warnings"),
		VALID("valid XML");

		private final @Getter String event;

		ValidationResult(String event) {
			this.event = event;
		}
	}

	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;
	private @Getter HasApplicationContext owner;

	private @Getter boolean throwException = false;
	private @Getter boolean fullSchemaChecking = false;
	private @Getter String reasonSessionKey = "failureReason";
	private @Getter String xmlReasonSessionKey = "xmlFailureReason";

	private @Getter boolean validateFile = false;
	private @Getter String charset = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
	protected boolean warn = AppConstants.getInstance(configurationClassLoader).getBoolean("xmlValidator.warn", true);

	protected String logPrefix = "";
	protected @Getter Boolean ignoreUnknownNamespaces;
	private @Getter boolean ignoreCaching = false;
	private @Getter String xmlSchemaVersion=null;

	protected @Setter SchemasProvider schemasProvider;

	private @Getter boolean started = false;

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
	public void configure(HasApplicationContext owner) throws ConfigurationException {
		this.logPrefix = ClassUtils.nameOf(owner);
		this.owner = owner;
	}

	@Override
	public String getName() {
		return logPrefix;
	}

	public void start() {
		if (isStarted()) {
			log.info("already started {}", ClassUtils.nameOf(this));
		}

		started = true;
	}

	@Override
	public void stop() {
		started = false;
	}

	@Override
	public boolean isRunning() {
		return started;
	}

	public AbstractValidationContext createValidationContext(PipeLineSession session, RootValidations rootValidations, Map<List<String>, List<String>> invalidRootNamespaces) throws ConfigurationException, PipeRunException {
		// clear session variables
		if (StringUtils.isNotEmpty(getReasonSessionKey())) {
			log.debug("{} removing contents of sessionKey [{}]", logPrefix, getReasonSessionKey());
			session.remove(getReasonSessionKey());
		}

		if (StringUtils.isNotEmpty(getXmlReasonSessionKey())) {
			log.debug("{} removing contents of sessionKey [{}]", logPrefix, getXmlReasonSessionKey());
			session.remove(getXmlReasonSessionKey());
		}
		return null;
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
	public ValidationResult finalizeValidation(AbstractValidationContext context, PipeLineSession session, Throwable t) throws XmlValidatorException {
		XmlValidatorErrorHandler xmlValidatorErrorHandler = context.getErrorHandler();
		ValidationResult result;
		if (t != null) {
			result = ValidationResult.PARSER_ERROR;
			// A SAXParseException will already be reported by the parser to the
			// XmlValidatorErrorHandler through the ErrorHandler interface.
			if (!(t instanceof SAXParseException)) {
				xmlValidatorErrorHandler.addReason(t, XmlValidatorErrorHandler.ReasonType.ERROR);
			}
		} else if (xmlValidatorErrorHandler.isErrorOccurred()) {
			result = ValidationResult.INVALID;
		} else if (xmlValidatorErrorHandler.isWarningsOccurred()) {
			result = ValidationResult.VALID_WITH_WARNINGS;
		} else {
			return ValidationResult.VALID;
		}

		String fullReasons = xmlValidatorErrorHandler.getReasons();
		if (StringUtils.isNotEmpty(getReasonSessionKey())) {
			log.debug("{}storing reasons under sessionKey [{}]", getLogPrefix(session), getReasonSessionKey());
			session.put(getReasonSessionKey(), fullReasons);
		}
		if (StringUtils.isNotEmpty(getXmlReasonSessionKey())) {
			log.debug("{}storing reasons (in xml format) under sessionKey [{}]", getLogPrefix(session), getXmlReasonSessionKey());
			session.put(getXmlReasonSessionKey(), xmlValidatorErrorHandler.getXmlReasons());
		}
		if (isThrowException()) {
			throw new XmlValidatorException(fullReasons, t);
		}
		if (log.isWarnEnabled()) log.warn("%svalidation failed: %s".formatted(getLogPrefix(session), fullReasons), t);
		return result;
	}

	public abstract ValidatorHandler getValidatorHandler(PipeLineSession session, AbstractValidationContext context) throws ConfigurationException, PipeRunException;
	public abstract List<XSModel> getXSModels();

	/**
	 * @param input   the XML string to validate
	 * @param session a {@link PipeLineSession pipeLineSession}
	 * @return ValidationResult
	 * @throws XmlValidatorException when <code>isThrowException</code> is true and a validationerror occurred.
	 */
	public ValidationResult validate(Object input, PipeLineSession session, RootValidations rootValidations, Map<List<String>, List<String>> invalidRootNamespaces) throws XmlValidatorException, PipeRunException, ConfigurationException {
		AbstractValidationContext context = createValidationContext(session, rootValidations, invalidRootNamespaces);
		ValidatorHandler validatorHandler = getValidatorHandler(session, context);
		return validate(input, session, validatorHandler, null, context);
	}

	public ValidationResult validate(Object input, PipeLineSession session, ValidatorHandler validatorHandler, XMLFilterImpl filter, AbstractValidationContext context) throws XmlValidatorException {

		if (filter != null) {
			// If a filter is present, connect its output to the context.contentHandler.
			// It is assumed that the filter input is already properly connected.
			filter.setContentHandler(context.getContentHandler());
			filter.setErrorHandler(context.getErrorHandler());
		} else {
			validatorHandler.setContentHandler(context.getContentHandler());
		}
		validatorHandler.setErrorHandler(context.getErrorHandler());

		InputSource is = getInputSource(Message.asMessage(input));

		return validate(is, validatorHandler, session, context);
	}

	public ValidationResult validate(InputSource inputSource, ValidatorHandler validatorHandler, PipeLineSession session, AbstractValidationContext context) throws XmlValidatorException {
		try {
			XmlUtils.parseXml(inputSource, validatorHandler, context.getErrorHandler());
		} catch (IOException | SAXException e) {
			return finalizeValidation(context, session, e);
		}
		return finalizeValidation(context, session, null);
	}

	protected String getLogPrefix(PipeLineSession session) {
		StringBuilder sb = new StringBuilder();
		sb.append(ClassUtils.nameOf(this)).append(' ');
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
	 * attribution constraint checking and particle derivation restriction
	 * checking are controlled by this option.
	 * <p>
	 * see property
	 * http://apache.org/xml/features/validation/schema-full-checking
	 * </p>
	 * @ff.default <code>false</code>
	 */
	public void setFullSchemaChecking(boolean fullSchemaChecking) {
		this.fullSchemaChecking = fullSchemaChecking;
	}

	/**
	 * Should the XmlValidator throw a PipeRunexception on a validation error. If not, a forward with name 'failure' must be defined.
	 * @ff.default false
	 */
	public void setThrowException(boolean throwException) {
		this.throwException = throwException;
	}

	/**
	 * If set: key of session variable to store reasons of mis-validation in
	 * @ff.default failureReason
	 */
	public void setReasonSessionKey(String reasonSessionKey) {
		this.reasonSessionKey = reasonSessionKey;
	}

	/**
	 * Like <code>reasonSessionKey</code> but stores reasons in xml format and more extensive
	 * @ff.default xmlFailureReason
	 */
	public void setXmlReasonSessionKey(String xmlReasonSessionKey) {
		this.xmlReasonSessionKey = xmlReasonSessionKey;
	}

	/**
	 * If set <code>true</code>, the input is assumed to be the name of the file to be validated. Otherwise the input itself is validated
	 * @ff.default false
	 */
	public void setValidateFile(boolean b) {
		validateFile = b;
	}

	/**
	 * Characterset used for reading file, only used when <code>validateFile</code> is <code>true</code>
	 * @ff.default utf-8
	 */
	public void setCharset(String string) {
		charset = string;
	}

	/**
	 * If set <code>true</code>, send warnings to logging and console about syntax problems in the configured schema('s).
	 * Alternatively, warnings can be switched off using suppression properties {@value SuppressKeys#XSD_VALIDATION_WARNINGS_SUPPRESS_KEY}, {@value SuppressKeys#XSD_VALIDATION_ERROR_SUPPRESS_KEY} and {@value SuppressKeys#XSD_VALIDATION_FATAL_ERROR_SUPPRESS_KEY}
	 * @ff.default true
	 */
	public void setWarn(boolean warn) {
		this.warn = warn;
	}

	/**
	 * Ignore namespaces in the input message which are unknown
	 * @ff.default true when <code>schema</code> or <code>noNamespaceSchemaLocation</code> is used, false otherwise
	 */
	public void setIgnoreUnknownNamespaces(Boolean b) {
		this.ignoreUnknownNamespaces = b;
	}

	/**
	 * If set <code>true</code>, the number for caching validators in appConstants is ignored and no caching is done (for this validator only)
	 * @ff.default false
	 */
	public void setIgnoreCaching(boolean ignoreCaching) {
		this.ignoreCaching = ignoreCaching;
	}

	/**
	 * If set to <code>1.0</code>, Xerces's previous XML Schema factory will be used, which would make all XSD 1.1 features illegal. The default behaviour can also be set with <code>xsd.processor.version</code> property.
	 * @ff.default <code>1.1</code>
	 */
	public void setXmlSchemaVersion(String xmlSchemaVersion) {
		this.xmlSchemaVersion = xmlSchemaVersion;
	}
	public boolean isXmlSchema1_0() {
		return getXmlSchemaVersion()==null || "1.0".equals(getXmlSchemaVersion());
	}
}
