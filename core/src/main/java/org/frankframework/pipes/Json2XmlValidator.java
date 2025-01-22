/*
   Copyright 2017, 2018 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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
package org.frankframework.pipes;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.xml.validation.ValidatorHandler;

import jakarta.json.Json;
import jakarta.json.JsonStructure;

import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.xs.XSModel;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.XMLFilterImpl;

import lombok.Getter;

import org.frankframework.align.Json2Xml;
import org.frankframework.align.Xml2Json;
import org.frankframework.align.XmlAligner;
import org.frankframework.align.XmlTypeToJsonSchemaConverter;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.parameters.ParameterList;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;
import org.frankframework.stream.MessageContext;
import org.frankframework.util.EnumUtils;
import org.frankframework.util.StringUtil;
import org.frankframework.util.XmlException;
import org.frankframework.util.XmlUtils;
import org.frankframework.validation.AbstractValidationContext;
import org.frankframework.validation.AbstractXmlValidator.ValidationResult;
import org.frankframework.validation.RootValidations;
import org.frankframework.validation.XmlValidatorException;
import org.frankframework.xml.NamespaceRemovingFilter;
import org.frankframework.xml.RootElementToSessionKeyFilter;
import org.frankframework.xml.XmlWriter;

/**
 *<code>Pipe</code> that validates the XML or JSON input message against a XML Schema and returns either XML or JSON.
 *
 * @author Gerrit van Brakel
 */
public class Json2XmlValidator extends XmlValidator implements HasPhysicalDestination {

	private final @Getter String domain = "XML Schema";
	public static final String INPUT_FORMAT_SESSION_KEY_PREFIX = "Json2XmlValidator.inputFormat ";

	private @Getter boolean compactJsonArrays=true;
	private @Getter boolean strictJsonArraySyntax=false;
	private @Getter boolean jsonWithRootElements=false;
	private @Getter boolean deepSearch=false;
	private @Getter boolean ignoreUndeclaredElements=false;
	private @Getter String targetNamespace;
	private @Getter DocumentFormat outputFormat = DocumentFormat.XML;
	private @Getter boolean autoFormat=true;
	private @Getter String inputFormatSessionKey=null;
	private @Getter String outputFormatSessionKey="outputFormat";
	private @Getter boolean failOnWildcards=true;
	private @Getter boolean acceptNamespacelessXml=false;
	private @Getter boolean produceNamespacelessXml=false;
	private @Getter boolean validateJsonToRootElementOnly=true;
	private @Getter boolean allowJson = true;

	{
		setSoapNamespace("");
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isNotEmpty(getSoapNamespace())) {
			throw new ConfigurationException("soapNamespace attribute not supported");
		}
		if (StringUtils.isEmpty(getInputFormatSessionKey())) {
			setInputFormatSessionKey(INPUT_FORMAT_SESSION_KEY_PREFIX+getName());
		}
	}

	public DocumentFormat getOutputFormat(PipeLineSession session, boolean responseMode) {
		DocumentFormat format=null;
		if (StringUtils.isNotEmpty(getOutputFormatSessionKey())) {
			String outputFormat = session.getString(getOutputFormatSessionKey());
			if (StringUtils.isNotEmpty(outputFormat)) {
				format=EnumUtils.parse(DocumentFormat.class, outputFormat);
			}
		}
		if (format==null && isAutoFormat() && responseMode && session.containsKey(getInputFormatSessionKey())) {
			String inputFormat = session.getString(getInputFormatSessionKey()).toLowerCase();
			if (inputFormat.contains("json")) {
				format = DocumentFormat.JSON;
			} else if (inputFormat.contains("xml")) {
				format = DocumentFormat.XML;
			}
		}
		if (format==null) {
			format=getOutputFormat();
		}
		return format;
	}

	protected void storeInputFormat(DocumentFormat format, Message input, PipeLineSession session, boolean responseMode) {
		if (!responseMode) {
			String sessionKey = getInputFormatSessionKey();

			if (!session.containsKey(sessionKey)) {
				if(isAutoFormat()) {
					String acceptHeaderValue = (String) input.getContext().get(MessageContext.HEADER_PREFIX + "Accept");
					String determinedFormat = parseAcceptHeader(format, acceptHeaderValue);

					log.debug("storing MessageContext inputFormat [{}] under session key [{}]", determinedFormat, sessionKey);
					session.put(sessionKey, determinedFormat);
				} else {
					log.debug("storing default inputFormat [{}] under session key [{}]", format, sessionKey);
					session.put(sessionKey, format);
				}
			}
		}
	}

	/**
	 * Default format has precedence over the accept header, accept header may be invalid or * slash *, in which case it should be ignored. First accept value wins.
	 */
	private String parseAcceptHeader(DocumentFormat detectedFormat, String acceptHeaderValue) {
		if(StringUtils.isEmpty(acceptHeaderValue) || "*/*".equals(acceptHeaderValue)) {
			return detectedFormat.name();
		}

		Optional<String> value = StringUtil.splitToStream(acceptHeaderValue)
				.filter(Json2XmlValidator::acceptableValues)
				.sorted(Json2XmlValidator::sortByQualifier)
				.findFirst();
		return value.orElse(detectedFormat.name());
	}

	/** Filter on only potential 'acceptable' results */
	private static boolean acceptableValues(String mimeType) {
		return !mimeType.contains("*/*") && (mimeType.contains("xml") || mimeType.contains("json"));
	}

	/** Ensure q=1.0 wins over 0.8 */
	private static int sortByQualifier(String o1, String o2) {
		double q1 = getQuality(o1);
		double q2 = getQuality(o2);
		return Double.compare(q2, q1);
	}

	private static double getQuality(String mimeType) {
		String q = MimeTypeUtils.parseMimeType(mimeType).getParameter("q");
		return StringUtils.isNotBlank(q) ? Double.parseDouble(q) : 1.0;
	}

	/**
	 * Validate the XML or JSON input, and align/convert it into JSON or XML according to a XML Schema.
	 * The format of the input message (XML or JSON) is automatically detected.
	 * @throws PipeRunException when <code>isThrowException</code> is true and a validationerror occurred.
	 */
	@Override
	public PipeRunResult doPipe(Message input, PipeLineSession session, boolean responseMode, String messageRoot) throws PipeRunException {
		String messageToValidate;
		try {
			messageToValidate = Message.isNull(input) ? "{}" : input.asString();
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot open stream", e);
		}
		int i=0;
		while (i<messageToValidate.length() && Character.isWhitespace(messageToValidate.charAt(i))) i++; //Trim leading whitespaces
		if (i>=messageToValidate.length()) { // if Message is empty
			messageToValidate="{}";
			storeInputFormat(getOutputFormat(), input, session, responseMode); //Message is empty, but could be either XML or JSON. Look at the accept header, and if not set fall back to the default OutputFormat.
		} else {
			char firstChar=messageToValidate.charAt(i);
			if (firstChar=='<') {
				// message is XML
				if (isAcceptNamespacelessXml()) {
					messageToValidate=addNamespace(messageToValidate); // TODO: do this via a filter
					//log.debug("added namespace to message [{}]", messageToValidate);
				}
				storeInputFormat(DocumentFormat.XML, input, session, responseMode);
				if (getOutputFormat(session,responseMode) != DocumentFormat.JSON) {
					final Message xmlInputMessage = createResultMessage(messageToValidate, MediaType.APPLICATION_XML);
					PipeRunResult result=super.doPipe(xmlInputMessage, session, responseMode, messageRoot);
					if (isProduceNamespacelessXml()) {
						try {
							result.setResult(XmlUtils.removeNamespaces(result.getResult().asString()));
						} catch (IOException | XmlException e) {
							throw new PipeRunException(this, "Cannot remove namespaces",e);
						}
					}
					return result;
				}
				try {
					return alignXml2Json(messageToValidate, session, responseMode);
				} catch (Exception e) {
					throw new PipeRunException(this, "Alignment of XML to JSON failed",e);
				}
			}
			if (!isAllowJson() && !responseMode) {
				return getErrorResult("message is not XML, because it starts with ["+firstChar+"] and not with '<'", session, responseMode);
			}
			if (firstChar!='{' && firstChar!='[') {
				return getErrorResult("message is not XML or JSON, because it starts with ["+firstChar+"] and not with '<', '{' or '['", session, responseMode);
			}
			storeInputFormat(DocumentFormat.JSON, input, session, responseMode);
		}

		try {
			return alignJson(messageToValidate, session, responseMode);
		} catch (XmlValidatorException e) {
			throw new PipeRunException(this, "Cannot align JSON", e);
		}
	}

	protected RootValidations getJsonRootValidations(boolean responseMode) {
		if (isValidateJsonToRootElementOnly()) {
			String root=getMessageRoot(responseMode);
			if (StringUtils.isEmpty(root)) {
				return null;
			}
			return new RootValidations(root);
		}
		return getRootValidations(responseMode);
	}

	protected PipeRunResult alignXml2Json(String messageToValidate, PipeLineSession session, boolean responseMode) throws XmlValidatorException, PipeRunException, ConfigurationException {

		AbstractValidationContext context = validator.createValidationContext(session, getJsonRootValidations(responseMode), getInvalidRootNamespaces());
		ValidatorHandler validatorHandler = validator.getValidatorHandler(session,context);

		// Make sure to use Xerces' ValidatorHandlerImpl, otherwise casting below will fail.
		XmlAligner aligner = new XmlAligner(validatorHandler, context.getXsModels());
		if (isIgnoreUndeclaredElements()) {
			log.warn("cannot ignore undeclared elements when converting from XML");
		}
		//aligner.setIgnoreUndeclaredElements(isIgnoreUndeclaredElements()); // cannot ignore XML Schema Validation failure in this case, currently
		Xml2Json xml2json = new Xml2Json(aligner, isCompactJsonArrays(), !isJsonWithRootElements());

		XMLFilterImpl handler = xml2json;

		if (StringUtils.isNotEmpty(getRootElementSessionKey())) {
			handler = new RootElementToSessionKeyFilter(session, getRootElementSessionKey(), getRootNamespaceSessionKey(), handler);
		}

		aligner.setContentHandler(handler);
		aligner.setErrorHandler(context.getErrorHandler());

		ValidationResult validationResult = validator.validate(messageToValidate, session, validatorHandler, xml2json, context);
		final Message jsonMessage = createResultMessage(xml2json.toString(), MediaType.APPLICATION_JSON);
		PipeForward forward = determineForward(validationResult, session, responseMode);
		return new PipeRunResult(forward, jsonMessage);
	}

	protected PipeRunResult alignJson(String messageToValidate, PipeLineSession session, boolean responseMode) throws PipeRunException, XmlValidatorException {
		AbstractValidationContext context;
		ValidatorHandler validatorHandler;
		try {
			context = validator.createValidationContext(session, getJsonRootValidations(responseMode), getInvalidRootNamespaces());
			validatorHandler = validator.getValidatorHandler(session, context);
		} catch (ConfigurationException e) {
			throw new PipeRunException(this,"Cannot create ValidationContext",e);
		}
		ValidationResult validationResult;
		Message resultMessage = null;
		try {
			Json2Xml aligner = new Json2Xml(validatorHandler, context.getXsModels(), isCompactJsonArrays(), getMessageRoot(responseMode), isStrictJsonArraySyntax());
			if (StringUtils.isNotEmpty(getTargetNamespace())) {
				aligner.setTargetNamespace(getTargetNamespace());
			}
			aligner.setDeepSearch(isDeepSearch());
			aligner.setErrorHandler(context.getErrorHandler());
			aligner.setFailOnWildcards(isFailOnWildcards());
			aligner.setIgnoreUndeclaredElements(isIgnoreUndeclaredElements());
			ParameterList parameterList = getParameterList();
			Map<String, Object> parameterValues = parameterList.getValues(new Message(messageToValidate), session).getValueMap();
			// remove parameters with null values, to support optional request parameters
			parameterValues.values().removeIf(Objects::isNull);
			aligner.setOverrideValues(parameterValues);
			JsonStructure jsonStructure = Json.createReader(new StringReader(messageToValidate)).read();

			// cannot build filter chain as usual backwardly, because it ends differently.
			// This will be fixed once an OutputStream can be provided to Xml2Json
			XMLFilterImpl sourceFilter = aligner;
			if (StringUtils.isNotEmpty(getRootElementSessionKey())) {
				XMLFilterImpl storeRootFilter = new RootElementToSessionKeyFilter(session, getRootElementSessionKey(), getRootNamespaceSessionKey(), null);
				aligner.setContentHandler(storeRootFilter);
				sourceFilter=storeRootFilter;
			}

			if (getOutputFormat(session,responseMode) == DocumentFormat.JSON) {
				Xml2Json xml2json = new Xml2Json(aligner, isCompactJsonArrays(), !isJsonWithRootElements());
				sourceFilter.setContentHandler(xml2json);
				aligner.startParse(jsonStructure);
				resultMessage = createResultMessage(xml2json.toString(), MediaType.APPLICATION_JSON);
			} else {
				MessageBuilder messageBuilder = new MessageBuilder();
				XmlWriter xmlWriter = messageBuilder.asXmlWriter();
				xmlWriter.setIncludeXmlDeclaration(true);
				ContentHandler handler = xmlWriter;
				if (isProduceNamespacelessXml()) {
					handler = new NamespaceRemovingFilter(handler);
				}
				sourceFilter.setContentHandler(handler);
				aligner.startParse(jsonStructure);
				resultMessage = messageBuilder.build();
			}
			validationResult = validator.finalizeValidation(context, session, null);
		} catch (Exception e) {
			validationResult = validator.finalizeValidation(context, session, e);
		}

		PipeForward forward = determineForward(validationResult, session, responseMode);
		return new PipeRunResult(forward, resultMessage);
	}

	private Message createResultMessage(String content, MimeType mimeType) {
		return new Message(content, new MessageContext().withMimeType(mimeType));
	}

	public String addNamespace(String xml) {
		if (StringUtils.isEmpty(xml) || xml.indexOf("xmlns")>0) {
			return xml;
		}
		String namespace;
		if (StringUtils.isNotEmpty(getTargetNamespace())) {
			namespace = getTargetNamespace();
		} else if (StringUtils.isNotEmpty(getSchemaLocation())) {
			namespace = getSchemaLocation().split(" ")[0];
		} else {
			return xml;
		}
		log.debug("setting namespace [{}]", namespace);
		int startPos=0;
		if (xml.trim().startsWith("<?")) {
			startPos=xml.indexOf("?>")+2;
		}
		int elementEnd=xml.indexOf('>',startPos);
		if (elementEnd<0) {
			return xml;
		}
		if (xml.charAt(elementEnd-1)=='/') {
			elementEnd--;
		}
		return xml.substring(0, elementEnd)+" xmlns=\""+namespace+"\""+xml.substring(elementEnd);
	}

	public JsonStructure createJsonSchema(String elementName) {
		return createJsonSchema(elementName, getTargetNamespace());
	}
	public JsonStructure createJsonSchema(String elementName, String namespace) {
		List<XSModel> models = validator.getXSModels();
		XmlTypeToJsonSchemaConverter converter = new XmlTypeToJsonSchemaConverter(models, isCompactJsonArrays(), !isJsonWithRootElements(), getSchemaLocation());
		return converter.createJsonSchema(elementName, namespace);
	}



	@Override
	public String getPhysicalDestinationName() {
		StringBuilder result = new StringBuilder();
		if (StringUtils.isNotEmpty(getRoot())) {
			result.append("request message [")
					.append(getRoot())
					.append("]");
		}
		if (StringUtils.isNotEmpty(getResponseRoot())) {
			if (result.length() > 0) {
				result.append("; ");
			}
			result.append("response message [")
					.append(getResponseRoot())
					.append("]");
		}
		return result.toString();
	}


	/** Only for JSON input: namespace of the resulting XML. Need only be specified when the namespace of root name is ambiguous in the schema */
	public void setTargetNamespace(String targetNamespace) {
		this.targetNamespace = targetNamespace;
	}

	/**
	 * Default format of the result, that is used if the format cannot be found from outputFormatSessionKey or from inputFormatSessionKey (when validating responses and autoFormat=true)
	 * @ff.default XML
	 */
	public void setOutputFormat(DocumentFormat outputFormat) {
		this.outputFormat = outputFormat;
	}

	/**
	 * Session key to retrieve outputFormat from
	 * @ff.default outputFormat
	 */
	public void setOutputFormatSessionKey(String outputFormatSessionKey) {
		this.outputFormatSessionKey = outputFormatSessionKey;
	}

	/**
	 * Session key to store the inputFormat in, to be able to set the outputformat when autoFormat=true. Can also be used to pass the value of an HTTP Accept header, to obtain a properly formatted response
	 * @ff.default {@value #INPUT_FORMAT_SESSION_KEY_PREFIX}&lt;name of the pipe&gt;
	 */
	public void setInputFormatSessionKey(String inputFormatSessionKey) {
		this.inputFormatSessionKey = inputFormatSessionKey;
	}

	/**
	 * If true, the format on 'output' is set to the same as the format of the input message on 'input'. The format of the input message is stored in and retrieved from the session variable specified by outputFormatSessionKey
	 * @ff.default true
	 */
	public void setAutoFormat(boolean autoFormat) {
		this.autoFormat = autoFormat;
	}

	/**
	 * If true assume arrays in JSON do not have the element containers like in XML
	 * @ff.default true
	 */
	public void setCompactJsonArrays(boolean compactJsonArrays) {
		this.compactJsonArrays = compactJsonArrays;
	}

	/**
	 * If true check that incoming JSON adheres to the specified syntax (compact or full), otherwise both types are accepted for conversion from JSON to XML
	 * @ff.default false
	 */
	public void setStrictJsonArraySyntax(boolean strictJsonArraySyntax) {
		this.strictJsonArraySyntax = strictJsonArraySyntax;
	}

	/**
	 * If true, assume that JSON contains/must contain a root element
	 * @ff.default false
	 */
	public void setJsonWithRootElements(boolean jsonWithRootElements) {
		this.jsonWithRootElements = jsonWithRootElements;
	}

	/**
	 * If true, and converting from JSON to XML, parameter substitutions are searched for optional sub elements too. By default, only mandatory elements are searched for parameter substitutions. N.B. Currently this option might cause problems. Please try using more qualified parameters names (using '/') first
	 * @ff.default false
	 */
	public void setDeepSearch(boolean deepSearch) {
		this.deepSearch = deepSearch;
	}

	/**
	 * If true, and converting from JSON to XML, elements in JSON that are not found in the XML Schema are ignored
	 * @ff.default false
	 */
	public void setIgnoreUndeclaredElements(boolean ignoreUndeclaredElements) {
		this.ignoreUndeclaredElements = ignoreUndeclaredElements;
	}

	/**
	 * If true, an exception is thrown when a wildcard is found in the XML Schema when parsing an object. This often indicates that an element is not properly typed in the XML Schema, and could lead to ambuigities.
	 * @ff.default true
	 */
	public void setFailOnWildcards(boolean failOnWildcards) {
		this.failOnWildcards = failOnWildcards;
	}

	/**
	 * If true, all XML is allowed to be without namespaces. If no namespaces are detected (by the presence of the string 'xmlns') in the XML, the root namespace is added to the XML
	 * @ff.default false
	 */
	public void setAcceptNamespacelessXml(boolean acceptNamespacelessXml) {
		this.acceptNamespacelessXml = acceptNamespacelessXml;
	}
	@Deprecated
	@ConfigurationWarning("The attribute 'acceptNamespaceLessXml' has been renamed 'acceptNamespacelessXml'")
	public void setAcceptNamespaceLessXml(boolean acceptNamespacelessXml) {
		setAcceptNamespacelessXml(acceptNamespacelessXml);
	}

	/**
	 * If true, all XML that is generated is without a namespace set
	 * @ff.default false
	 */
	public void setProduceNamespacelessXml(boolean produceNamespacelessXml) {
		this.produceNamespacelessXml = produceNamespacelessXml;
	}
	@Deprecated
	@ConfigurationWarning("The attribute 'produceNamespaceLessXml' has been renamed 'produceNamespacelessXml'")
	public void setProduceNamespaceLessXml(boolean produceNamespacelessXml) {
		setProduceNamespacelessXml(produceNamespacelessXml);
	}

	/**
	 * If true, and converting to or from JSON, then the message root is the only rootValidation, ignoring root validations like for SOAP envelope and header set by descender classes like SoapValidator
	 * @ff.default true
	 */
	public void setValidateJsonToRootElementOnly(boolean validateJsonToRootElementOnly) {
		this.validateJsonToRootElementOnly = validateJsonToRootElementOnly;
	}

	/**
	 * Allow JSON input
	 * @ff.default true
	 */
	public void setAllowJson(boolean allowJson) {
		this.allowJson = allowJson;
	}

}
