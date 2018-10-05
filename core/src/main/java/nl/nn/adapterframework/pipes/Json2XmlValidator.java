/*
   Copyright 2017,2018 Nationale-Nederlanden

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

import java.io.StringReader;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonStructure;
import javax.xml.transform.Source;
import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang.StringUtils;
import org.apache.xerces.xs.PSVIProvider;
import org.xml.sax.XMLReader;

import nl.nn.adapterframework.align.Json2Xml;
import nl.nn.adapterframework.align.Xml2Json;
import nl.nn.adapterframework.align.XmlAligner;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.validation.ValidationContext;
import nl.nn.adapterframework.validation.XmlValidatorException;

/**
*<code>Pipe</code> that validates the XML or JSON input message against a XML-Schema and returns either XML or JSON.
*
* <p><b>Configuration:</b>
* <table border="1">
* <tr><th>attributes</th><th>description</th><th>default</th></tr>
* <tr><td>className</td><td>nl.nn.adapterframework.pipes.XmlValidator</td><td>&nbsp;</td></tr>
* <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
* <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, IPipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
* <tr><td>{@link #setDurationThreshold(long) durationThreshold}</td><td>if durationThreshold >=0 and the duration (in milliseconds) of the message processing exceeded the value specified the message is logged informatory</td><td>-1</td></tr>
* <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
* <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
* <tr><td>{@link #setSchema(String) schema}</td><td>The filename of the schema on the classpath. See doc on the method. (effectively the same as noNamespaceSchemaLocation)</td><td>&nbsp;</td></tr>
* <tr><td>{@link #setNoNamespaceSchemaLocation(String) noNamespaceSchemaLocation}</td><td>A URI reference as a hint as to the location of a schema document with no target namespace. See doc on the method.</td><td>&nbsp;</td></tr>
* <tr><td>{@link #setSchemaLocation(String) schemaLocation}</td><td>Pairs of URI references (one for the namespace name, and one for a hint as to the location of a schema document defining names for that namespace name). See doc on the method.</td><td>&nbsp;</td></tr>
* <tr><td>{@link #setSchemaSessionKey(String) schemaSessionKey}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
* <tr><td>{@link #setFullSchemaChecking(boolean) fullSchemaChecking}</td><td>Perform addional memory intensive checks</td><td><code>false</code></td></tr>
* <tr><td>{@link #setThrowException(boolean) throwException}</td><td>Should the XmlValidator throw a PipeRunException on a validation error (if not, a forward with name "failure" should be defined.</td><td><code>false</code></td></tr>
* <tr><td>{@link #setReasonSessionKey(String) reasonSessionKey}</td><td>if set: key of session variable to store reasons of mis-validation in</td><td>failureReason</td></tr>
* <tr><td>{@link #setXmlReasonSessionKey(String) xmlReasonSessionKey}</td><td>like <code>reasonSessionKey</code> but stores reasons in xml format and more extensive</td><td>xmlFailureReason</td></tr>
* <tr><td>{@link #setRoot(String) root}</td><td>name of the root element. Or a comma separated list of names to choose from (only one is allowed)</td><td>&nbsp;</td></tr>
* <tr><td>{@link #setValidateFile(boolean) validateFile}</td><td>when set <code>true</code>, the input is assumed to be the name of the file to be validated. Otherwise the input itself is validated</td><td><code>false</code></td></tr>
* <tr><td>{@link #setCharset(String) charset}</td><td>characterset used for reading file, only used when {@link #setValidateFile(boolean) validateFile} is <code>true</code></td><td>UTF-8</td></tr>
* <tr><td>{@link #setSoapNamespace(String) soapNamespace}</td><td>the namespace of the SOAP Envelope, when this property has a value and the input message is a SOAP Message the content of the SOAP Body is used for validation, hence the SOAP Envelope and SOAP Body elements are not considered part of the message to validate. Please note that this functionality is deprecated, using {@link nl.nn.adapterframework.soap.SoapValidator2} is now the preferred solution in case a SOAP Message needs to be validated, in other cases give this property an empty value</td><td>http://schemas.xmlsoap.org/soap/envelope/</td></tr>
* <tr><td>{@link #setIgnoreUnknownNamespaces(boolean) ignoreUnknownNamespaces}</td><td>ignore namespaces in the input message which are unknown</td><td>true when schema or noNamespaceSchemaLocation is used, false otherwise</td></tr>
* <tr><td>{@link #setWarn(boolean) warn}</td><td>when set <code>true</code>, send warnings to logging and console about syntax problems in the configured schema('s)</td><td><code>true</code></td></tr>
* <tr><td>{@link #setForwardFailureToSuccess(boolean) forwardFailureToSuccess}</td><td>when set <code>true</code>, the failure forward is replaced by the success forward (like a warning mode)</td><td><code>false</code></td></tr>
* <tr><td>{@link #setAddNamespaceToSchema(boolean) addNamespaceToSchema}</td><td>when set <code>true</code>, the namespace from schemaLocation is added to the schema document as targetNamespace</td><td><code>false</code></td></tr>
* <tr><td>{@link #setImportedSchemaLocationsToIgnore(String) importedSchemaLocationsToIgnore}</td><td>comma separated list of schemaLocations which are excluded from an import or include in the schema document</td><td>&nbsp;</td></tr>
* <tr><td>{@link #setUseBaseImportedSchemaLocationsToIgnore(boolean) useBaseImportedSchemaLocationsToIgnore}</td><td>when set <code>true</code>, the comparison for importedSchemaLocationsToIgnore is done on base filename without any path</td><td><code>false</code></td></tr>
* <tr><td>{@link #setImportedNamespacesToIgnore(String) importedNamespacesToIgnore}</td><td>comma separated list of namespaces which are excluded from an import or include in the schema document</td><td>&nbsp;</td></tr>
* <tr><td>{@link #setIgnoreCaching(boolean) ignoreCaching}</td><td>when set <code>true</code>, the number for caching validators in AppConstants is ignored and NO caching is done (for this validator only)</td><td><code>false</code></td></tr>
* <tr><td>{@link #setLazyInit(boolean) lazyInit}</td><td>when set, the value in AppConstants is overwritten (for this validator only)</td><td><code>application default (false)</code></td></tr>
* <tr><td>{@link #setCompactJsonArrays(boolean) compactJsonArrays}</td><td>when true assume arrays in json do not have the element containers like in XML</td><td>true</td></tr>
* <tr><td>{@link #setStrictJsonArraySyntax(boolean) strictJsonArraySyntax}</td><td>when true check that incoming json adheres to the specified syntax (compact or full), otherwise both types are accepted for conversion from json to xml</td><td>false</td></tr>
* <tr><td>{@link #setJsonWithRootElements(boolean) jsonWithRootElements}</td><td>when true, assume that JSON contains/must contain a root element</td><td>false</td></tr>
* <tr><td>{@link #setTargetNamespace(String) targetNamespace}</td><td>Ony for JSON input: namespace of the resulting XML. Need only be specified when the namespace of root name is ambiguous in the Schema</td><td>&nbsp;</td></tr>
* <tr><td>{@link #setOutputFormat(String) outputFormat}</td><td>default format of the result. Either 'xml' or 'json'</td><td>xml</td></tr>
* <tr><td>{@link #setAutoFormat(boolean) autoFormat}</td><td>when true, the format on 'output' is set to the same as the format of the input message on 'input'</td><td>true</td></tr>
* <tr><td>{@link #setOutputFormatSessionKey(String) outputFormatSessionKey}</td><td>session key to retrieve outputFormat from.</td><td>outputFormat</td></tr>
* <tr><td>{@link #setFailOnWildcards(boolean) failOnWildcards}</td><td>when true, an exception is thrown when a Wildcard is found in the XML Schema when parsing an object. This often indicates that an element is not properly typed in the XML Schema, and could lead to ambuigities.</td><td>true</td></tr>
* <tr><td>{@link #setAcceptNamespaceLessXml(boolean) acceptNamespaceLessXml}</td><td>when true, all XML is allowed to be without namespaces. When no namespaces are detected (by the presence of the string 'xmlns') in the XML string, the root namespace is added to the XML</td><td>false</td></tr>
* <tr><td>{@link #setProduceNamespaceLessXml(boolean) produceNamespaceLessXml}</td><td>when true, all XML that is generated is without a namespace set</td><td>false</td></tr>
* </table>
* <p><b>Exits:</b>
* <table border="1">
* <tr><th>state</th><th>condition</th></tr>
* <tr><td>"success"</td><td>default</td></tr>
* <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified, the value for "success"</td></tr>
* <tr><td>"parserError"</td><td>a parser exception occurred, probably caused by non-well-formed XML. If not specified, "failure" is used in such a case</td></tr>
* <tr><td>"illegalRoot"</td><td>if the required root element is not found. If not specified, "failure" is used in such a case</td></tr>
* <tr><td>"failure"</td><td>if a validation error occurred</td></tr>
* </table>
* <br>
* @author Gerrit van Brakel
*/
public class Json2XmlValidator extends XmlValidator {

	public static final String INPUT_FORMAT_SESSION_KEY_PREFIX = "Json2XmlValidator.inputformat ";
	
	public final String FORMAT_XML="xml";
	public final String FORMAT_JSON="json";
	public final String FORMAT_AUTO="auto";
	
	private boolean compactJsonArrays=true;
	private boolean strictJsonArraySyntax=false;
	private boolean jsonWithRootElements=false;
	private boolean deepSearch=false;
	private String targetNamespace;
	private String outputFormat=FORMAT_XML;
	private boolean autoFormat=true;
	private String outputFormatSessionKey="outputFormat";
	//private String requestFormat=FORMAT_AUTO;
	private boolean failOnWildcards=true;
	private boolean acceptNamespaceLessXml=false;
	private boolean produceNamespaceLessXml=false;


	{
		setSoapNamespace("");
	}
	
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isNotEmpty(getSoapNamespace())) {
			throw new ConfigurationException("soapNamespace attribute not supported");
		}
	}
	
	public String getOutputFormat(IPipeLineSession session, boolean responseMode) {
		String format=null;
		if (StringUtils.isNotEmpty(getOutputFormatSessionKey())) {
			format=(String)session.get(getOutputFormatSessionKey());
		}
		if (StringUtils.isEmpty(format) && isAutoFormat() && responseMode) {
			format=(String)session.get(INPUT_FORMAT_SESSION_KEY_PREFIX+getName());
		}	
		if (StringUtils.isEmpty(format)) {
			format=getOutputFormat();
		}	
		return format;
	}
	
	protected void storeInputFormat(String format, IPipeLineSession session, boolean responseMode) {
		if (!responseMode) {
			session.put(INPUT_FORMAT_SESSION_KEY_PREFIX+getName(), format);
		}
	}
	
    /**
     * Validate the XML or JSON string. The format is automatically detected.
     * @param input a String
     * @param session a {@link nl.nn.adapterframework.core.IPipeLineSession Pipelinesession}

     * @throws PipeRunException when <code>isThrowException</code> is true and a validationerror occurred.
     */
	@Override
	public PipeRunResult doPipe(Object input, IPipeLineSession session, boolean responseMode) throws PipeRunException {
		String messageToValidate=input==null?"{}":input.toString();
		int i=0;
		while (i<messageToValidate.length() && Character.isWhitespace(messageToValidate.charAt(i))) i++;
		if (i>=messageToValidate.length()) {
			messageToValidate="{}";
			storeInputFormat(FORMAT_JSON,session, responseMode);
		} else {
			char firstChar=messageToValidate.charAt(i);
			if (firstChar=='<') {
				// message is XML
				if (isAcceptNamespaceLessXml()) {
					messageToValidate=addNamespace(messageToValidate);
					//if (log.isDebugEnabled()) log.debug("added namespace to message ["+messageToValidate+"]");
				}
				storeInputFormat(FORMAT_XML,session, responseMode);
				if (!getOutputFormat(session,responseMode).equalsIgnoreCase(FORMAT_JSON)) {
					PipeRunResult result=super.doPipe(messageToValidate,session, responseMode);
					if (isProduceNamespaceLessXml()) {
						String msg=(String)result.getResult();
						msg=XmlUtils.removeNamespaces(msg);
						result.setResult(msg);
					}
					return result;
				}
				try {
					return alignXml2Json(messageToValidate, session, responseMode);
				} catch (Exception e) {
					throw new PipeRunException(this, "Alignment of XML to JSON failed",e);
				}
			}
			if (firstChar!='{' && firstChar!='[') {
				throw new PipeRunException(this,"message is not XML or JSON, because it starts with ["+firstChar+"] and not with '<', '{' or '['");
			}
			storeInputFormat(FORMAT_JSON,session, responseMode);
		}
		try {
			return alignJson(messageToValidate, session, responseMode);
		} catch (XmlValidatorException e) {
			throw new PipeRunException(this, "Cannot align JSON", e);
		}
	}
	
	protected PipeRunResult alignXml2Json(String messageToValidate, IPipeLineSession session, boolean responseMode)
			throws XmlValidatorException, PipeRunException, ConfigurationException {

		ValidationContext context = validator.createValidationContext(session, getRootValidations(responseMode), getInvalidRootNamespaces());
		XMLReader parser = validator.getValidatingParser(session,context);
		XmlAligner aligner = new XmlAligner((PSVIProvider)parser);
		Xml2Json xml2json = new Xml2Json(aligner, isCompactJsonArrays(), !isJsonWithRootElements());	
		parser.setContentHandler(aligner);
		aligner.setContentHandler(xml2json);
		aligner.setErrorHandler(context.getErrorHandler());
		
		String resultEvent= validator.validate(messageToValidate, session, getLogPrefix(session), parser, xml2json, context);
		String out=xml2json.toString();
		PipeForward forward=determineForward(resultEvent, session, responseMode);
		PipeRunResult result=new PipeRunResult(forward,out);
		return result;
	}
	
	protected PipeRunResult alignJson(String messageToValidate, IPipeLineSession session, boolean responseMode) throws PipeRunException, XmlValidatorException {

		ValidationContext context;
		ValidatorHandler validatorHandler;
		try {
			context = validator.createValidationContext(session, getRootValidations(responseMode), getInvalidRootNamespaces());
			validatorHandler = validator.getValidatorHandler(session, context);
		} catch (ConfigurationException e) {
			throw new PipeRunException(this,"Cannot create ValidationContext",e);
		}
		String resultEvent;
		String out=null;
		try {
			Json2Xml aligner = new Json2Xml(validatorHandler, context.getXsModels(), isCompactJsonArrays(), getMessageRoot(responseMode), isStrictJsonArraySyntax());
			if (StringUtils.isNotEmpty(getTargetNamespace())) {
				aligner.setTargetNamespace(getTargetNamespace());
			}
			aligner.setDeepSearch(isDeepSearch());
			aligner.setErrorHandler(context.getErrorHandler());
			aligner.setFailOnWildcards(isFailOnWildcards());
			ParameterList parameterList = getParameterList();
			if (parameterList!=null) {
				ParameterResolutionContext prc = new ParameterResolutionContext(messageToValidate, session, isNamespaceAware());
				Map<String,Object> parametervalues = null;
				parametervalues = prc.getValueMap(parameterList);
				aligner.setOverrideValues(parametervalues);
			}
			JsonStructure jsonStructure = Json.createReader(new StringReader(messageToValidate)).read();
			
			if (getOutputFormat(session,responseMode).equalsIgnoreCase(FORMAT_JSON)) {
				Xml2Json xml2json = new Xml2Json(aligner, isCompactJsonArrays(), !isJsonWithRootElements());
				aligner.setContentHandler(xml2json);
				aligner.startParse(jsonStructure);
				out=xml2json.toString();
			} else {
				Source source = aligner.asSource(jsonStructure);
				out = XmlUtils.source2String(source,isProduceNamespaceLessXml());
			}
		} catch (Exception e) {
			resultEvent= validator.finalizeValidation(context, session, e);
		}
		resultEvent= validator.finalizeValidation(context, session, null);
		PipeForward forward=determineForward(resultEvent, session, responseMode);
		PipeRunResult result=new PipeRunResult(forward,out);
		return result;
	}

	public String addNamespace(String xml) {
		if (StringUtils.isEmpty(xml) || xml.indexOf("xmlns")>0) {
			return xml;
		}	
		String namespace = getSchemaLocation().split(" ")[0];
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
	
	
	public String getTargetNamespace() {
		return targetNamespace;
	}
	public void setTargetNamespace(String targetNamespace) {
		this.targetNamespace = targetNamespace;
	}
	
	public String getOutputFormat() {
		return outputFormat;
	}
	public void setOutputFormat(String outputFormat) {
		this.outputFormat = outputFormat;
	}
	
	public String getOutputFormatSessionKey() {
		return outputFormatSessionKey;
	}
	public void setOutputFormatSessionKey(String outputFormatSessionKey) {
		this.outputFormatSessionKey = outputFormatSessionKey;
	}
	
	public boolean isCompactJsonArrays() {
		return compactJsonArrays;
	}
	public void setCompactJsonArrays(boolean compactJsonArrays) {
		this.compactJsonArrays = compactJsonArrays;
	}

	public boolean isStrictJsonArraySyntax() {
		return strictJsonArraySyntax;
	}
	public void setStrictJsonArraySyntax(boolean strictJsonArraySyntax) {
		this.strictJsonArraySyntax = strictJsonArraySyntax;
	}

	public boolean isJsonWithRootElements() {
		return jsonWithRootElements;
	}
	public void setJsonWithRootElements(boolean jsonWithRootElements) {
		this.jsonWithRootElements = jsonWithRootElements;
	}

	public boolean isAutoFormat() {
		return autoFormat;
	}
	public void setAutoFormat(boolean autoFormat) {
		this.autoFormat = autoFormat;
	}

	public boolean isDeepSearch() {
		return deepSearch;
	}
	public void setDeepSearch(boolean deepSearch) {
		this.deepSearch = deepSearch;
	}

	public boolean isFailOnWildcards() {
		return failOnWildcards;
	}
	public void setFailOnWildcards(boolean failOnWildcards) {
		this.failOnWildcards = failOnWildcards;
	}

	public boolean isAcceptNamespaceLessXml() {
		return acceptNamespaceLessXml;
	}
	public void setAcceptNamespaceLessXml(boolean acceptNamespaceLessXml) {
		this.acceptNamespaceLessXml = acceptNamespaceLessXml;
	}

	public boolean isProduceNamespaceLessXml() {
		return produceNamespaceLessXml;
	}
	public void setProduceNamespaceLessXml(boolean produceNamespaceLessXml) {
		this.produceNamespaceLessXml = produceNamespaceLessXml;
	}

}
