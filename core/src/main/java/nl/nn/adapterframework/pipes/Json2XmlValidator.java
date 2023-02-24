/*
   Copyright 2017, 2018 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
package nl.nn.adapterframework.pipes;

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.xs.XSModel;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.XMLFilterImpl;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonStructure;
import lombok.Getter;
import nl.nn.adapterframework.align.DomTreeAligner;
import nl.nn.adapterframework.align.Json2Xml;
import nl.nn.adapterframework.align.Tree2Xml;
import nl.nn.adapterframework.align.Xml2Json;
import nl.nn.adapterframework.align.XmlAligner;
import nl.nn.adapterframework.align.XmlTypeToJsonSchemaConverter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.functional.ThrowingRunnable;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageContext;
import nl.nn.adapterframework.stream.document.DocumentFormat;
import nl.nn.adapterframework.util.EnumUtils;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.validation.AbstractXmlValidator.ValidationResult;
import nl.nn.adapterframework.validation.RootValidations;
import nl.nn.adapterframework.validation.ValidationContext;
import nl.nn.adapterframework.validation.XmlValidatorException;
import nl.nn.adapterframework.xml.NamespaceRemovingFilter;
import nl.nn.adapterframework.xml.RootElementToSessionKeyFilter;
import nl.nn.adapterframework.xml.XmlWriter;

/**
 *<code>Pipe</code> that validates the XML or JSON input message against a XML Schema and returns either XML or JSON.
 *
 * @author Gerrit van Brakel
 */
public class Json2XmlValidator extends XmlValidator implements HasPhysicalDestination {

	private final @Getter(onMethod = @__(@Override)) String domain = "XML Schema";
	public static final String INPUT_FORMAT_SESSION_KEY_PREFIX = "Json2XmlValidator.inputFormat ";

	private @Getter boolean compactJsonArrays=true;
	private @Getter boolean strictJsonArraySyntax=false;
	private @Getter boolean jsonWithRootElements=false;
	private @Getter boolean deepSearch=false;
	private @Getter boolean ignoreUndeclaredElements=false;
	private @Getter String targetNamespace;
	private @Getter DocumentFormat outputFormat=DocumentFormat.XML;
	private @Getter boolean autoFormat=true;
	private @Getter String inputFormatSessionKey=null;
	private @Getter String outputFormatSessionKey="outputFormat";
	private @Getter boolean failOnWildcards=true;
	private @Getter boolean acceptNamespacelessXml=false;
	private @Getter boolean produceNamespacelessXml=false;
	private @Getter boolean validateJsonToRootElementOnly=true;
	private @Getter boolean allowJson = true;
	private @Getter boolean alignXml = false;

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

	public DocumentFormat getOutputFormat(PipeLineSession session, boolean responseMode) throws PipeRunException {
		DocumentFormat format=null;
		try {
			if (StringUtils.isNotEmpty(getOutputFormatSessionKey())) {
				String outputFormat = session.getMessage(getOutputFormatSessionKey()).asString();
				if (StringUtils.isNotEmpty(outputFormat)) {
					format=EnumUtils.parse(DocumentFormat.class, outputFormat);
				}
			}
			if (format==null && isAutoFormat() && responseMode && session.containsKey(getInputFormatSessionKey())) {
				String inputFormat = session.getMessage(getInputFormatSessionKey()).asString().toLowerCase();
				if (inputFormat.contains("json")) {
					format = DocumentFormat.JSON;
				} else if (inputFormat.contains("xml")) {
					format = DocumentFormat.XML;
				}
			}
			if (format==null) {
				format=getOutputFormat();
			}
		} catch(IOException e) {
			throw new PipeRunException(this, "cannot get output format", e);
		}
		return format;
	}

	protected void storeInputFormat(DocumentFormat format, Message input, PipeLineSession session, boolean responseMode) {
		if (!responseMode) {
			String sessionKey = getInputFormatSessionKey();

			if (!session.containsKey(sessionKey)) {
				String acceptHeader = (String) input.getContext().get(MessageContext.HEADER_PREFIX + "Accept");
				if(isAutoFormat() && StringUtils.isNotEmpty(acceptHeader)) {
					log.debug("storing MessageContext inputFormat [{}] under session key [{}]", acceptHeader, sessionKey);
					session.put(sessionKey, acceptHeader);
				} else {
					log.debug("storing default inputFormat [{}] under session key [{}]", format, sessionKey);
					session.put(sessionKey, format);
				}
			}
		}
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
		while (i<messageToValidate.length() && Character.isWhitespace(messageToValidate.charAt(i))) i++;
		if (i>=messageToValidate.length()) {
			messageToValidate="{}";
			storeInputFormat(DocumentFormat.JSON, input, session, responseMode);
		} else {
			char firstChar=messageToValidate.charAt(i);
			if (firstChar=='<') {
				// message is XML
				if (isAcceptNamespacelessXml()) {
					messageToValidate=addNamespace(messageToValidate); // TODO: do this via a filter
					//log.debug("added namespace to message [{}]", messageToValidate);
				}

				// Align XML if required
				storeInputFormat(DocumentFormat.XML, input, session, responseMode);
				if (isAlignXml() || getParameterList()!=null && !getParameterList().isEmpty()) {
					try {
						return align(DocumentFormat.XML, messageToValidate, session, responseMode);
					} catch (Exception e) {
						throw new PipeRunException(this, "Alignment of XML to JSON failed",e);
					}
				}

				// Just validate XML if that is sufficient
				if (getOutputFormat(session,responseMode) != DocumentFormat.JSON) {
					PipeRunResult result=super.doPipe(new Message(messageToValidate),session, responseMode, messageRoot);
					if (isProduceNamespacelessXml()) {
						try {
							result.setResult(XmlUtils.removeNamespaces(result.getResult().asString()));
						} catch (IOException e) {
							throw new PipeRunException(this, "Cannot remove namespaces",e);
						}
					}
					return result;
				}

				// otherwise validate XML and convert to JSON
				try {
					return validateXml2Json(messageToValidate, session, responseMode);
				} catch (Exception e) {
					throw new PipeRunException(this, "Validation of XML to JSON failed",e);
				}
			}

			if (!isAllowJson() && !responseMode) {
				return getErrorResult(ValidationResult.PARSER_ERROR, "message is not XML, because it starts with ["+firstChar+"] and not with '<'", session, responseMode);
			}
			if (firstChar!='{' && firstChar!='[') {
				return getErrorResult(ValidationResult.PARSER_ERROR, "message is not XML or JSON, because it starts with ["+firstChar+"] and not with '<', '{' or '['", session, responseMode);
			}

			// message is JSON
			storeInputFormat(DocumentFormat.JSON, input, session, responseMode);
		}

		try {
			return align(DocumentFormat.JSON, messageToValidate, session, responseMode);
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

	protected PipeRunResult validateXml2Json(String messageToValidate, PipeLineSession session, boolean responseMode) throws XmlValidatorException, PipeRunException, ConfigurationException {

		ValidationContext context = validator.createValidationContext(session, getJsonRootValidations(responseMode), getInvalidRootNamespaces());
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

		ValidationResult validationResult= validator.validate(messageToValidate, session, validatorHandler, xml2json, context);
		String out=xml2json.toString();
		PipeForward forward=determineForward(validationResult, session, responseMode);
		PipeRunResult result=new PipeRunResult(forward,out);
		return result;
	}

	protected PipeRunResult align(DocumentFormat inputFormat, String messageToValidate, PipeLineSession session, boolean responseMode) throws PipeRunException, XmlValidatorException {

		ValidationContext context;
		ValidatorHandler validatorHandler;
		try {
			context = validator.createValidationContext(session, getJsonRootValidations(responseMode), getInvalidRootNamespaces());
			validatorHandler = validator.getValidatorHandler(session, context);
		} catch (ConfigurationException e) {
			throw new PipeRunException(this,"Cannot create ValidationContext",e);
		}
		ValidationResult validationResult;
		String out=null;
		try {
			switch (inputFormat) {
				case XML:
					DomTreeAligner domTreeAligner = new DomTreeAligner(validatorHandler, context.getXsModels());
					out = configureAndRunAligner(domTreeAligner, context, messageToValidate, session, responseMode, ()->domTreeAligner.startParse(XmlUtils.buildDomDocument(messageToValidate)));
					break;
				case JSON:
					final Json2Xml json2Xml = new Json2Xml(validatorHandler, context.getXsModels(), isCompactJsonArrays(), getMessageRoot(responseMode), isStrictJsonArraySyntax());
					out = configureAndRunAligner(json2Xml, context, messageToValidate, session, responseMode, ()->json2Xml.startParse(Json.createReader(new StringReader(messageToValidate)).read()));
					break;
				default:
					throw new IllegalArgumentException("DocumentFormat ["+inputFormat+"] unknown");
			}
			validationResult= validator.finalizeValidation(context, session, null);
		} catch (Exception e) {
			validationResult= validator.finalizeValidation(context, session, e);
		}

		PipeForward forward=determineForward(validationResult, session, responseMode);
		return new PipeRunResult(forward,out);
	}

	private String configureAndRunAligner(Tree2Xml aligner, ValidationContext context, String messageToValidate, PipeLineSession session, boolean responseMode, ThrowingRunnable<Exception> startParse) throws Exception {
		if (StringUtils.isNotEmpty(getTargetNamespace())) {
			aligner.setTargetNamespace(getTargetNamespace());
		}
		aligner.setDeepSearch(isDeepSearch());
		aligner.setErrorHandler(context.getErrorHandler());
		aligner.setFailOnWildcards(isFailOnWildcards());
		aligner.setIgnoreUndeclaredElements(isIgnoreUndeclaredElements());
		ParameterList parameterList = getParameterList();
		if (parameterList!=null) {
			Map<String,Object> parametervalues = null;
			parametervalues = parameterList.getValues(new Message(messageToValidate), session).getValueMap();
			// remove parameters with null values, to support optional request parameters
			for(Iterator<String> it=parametervalues.keySet().iterator();it.hasNext();) {
				String key=it.next();
				if (parametervalues.get(key)==null) {
					it.remove();
				}
			}
			aligner.setOverrideValues(parametervalues);
		}
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
			startParse.run();
			return xml2json.toString();
		}
		XmlWriter xmlWriter = new XmlWriter();
		xmlWriter.setIncludeXmlDeclaration(true);
		ContentHandler handler = xmlWriter;
		if (isProduceNamespacelessXml()) {
			handler = new NamespaceRemovingFilter(handler);
		}
		sourceFilter.setContentHandler(handler);
		startParse.run();
		return xmlWriter.toString();
	}

	public String addNamespace(String xml) {
		if (StringUtils.isEmpty(xml) || xml.indexOf("xmlns")>0) {
			return xml;
		}
		String namespace=null;
		if (StringUtils.isNotEmpty(getTargetNamespace())) {
			namespace = getTargetNamespace();
		} else {
			if (StringUtils.isNotEmpty(getSchemaLocation())) {
				namespace = getSchemaLocation().split(" ")[0];
			}
		}
		if (namespace==null) {
			return xml;
		}
		if (log.isDebugEnabled()) log.debug("setting namespace ["+namespace+"]");
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

	public JsonStructure createRequestJsonSchema() {
		return createJsonSchema(getRoot());
	}
	public JsonStructure createResponseJsonSchema() {
		return createJsonSchema(getResponseRoot());
	}

	public JsonObject createJsonSchemaDefinitions(String definitionsPath) {
		List<XSModel> models = validator.getXSModels();
		XmlTypeToJsonSchemaConverter converter = new XmlTypeToJsonSchemaConverter(models, isCompactJsonArrays(), !isJsonWithRootElements(), getSchemaLocation(), definitionsPath);
		JsonObject jsonschema = converter.getDefinitions();
		return jsonschema;
	}
	public JsonStructure createJsonSchema(String elementName) {
		return createJsonSchema(elementName, getTargetNamespace());
	}
	public JsonStructure createJsonSchema(String elementName, String namespace) {
		List<XSModel> models = validator.getXSModels();
		XmlTypeToJsonSchemaConverter converter = new XmlTypeToJsonSchemaConverter(models, isCompactJsonArrays(), !isJsonWithRootElements(), getSchemaLocation());
		JsonStructure jsonschema = converter.createJsonSchema(elementName, namespace);
		return jsonschema;
	}



	@Override
	public String getPhysicalDestinationName() {
		String result=null;
		if (StringUtils.isNotEmpty(getRoot())) {
			result="request message ["+getRoot()+"]";
		}
		if (StringUtils.isNotEmpty(getResponseRoot())) {
			if (result==null) {
				result = "";
			} else {
				result += "; ";
			}
			result+="response message ["+getResponseRoot()+"]";
		}
		return result;
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

	/**
	 * If true, XML input is aligned (elements put in the right order, substitutes applied from parameters), rather then only validated. This is implied when parameters are present.
	 * @ff.default true
	 */
	public void setAlignXml(boolean alignXml) {
		this.alignXml = alignXml;
	}

}
