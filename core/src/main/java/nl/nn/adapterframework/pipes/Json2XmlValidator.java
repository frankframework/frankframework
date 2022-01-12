/*
   Copyright 2017, 2018 Nationale-Nederlanden, 2020 WeAreFrank!

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.xs.XSModel;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.XMLFilterImpl;

import nl.nn.adapterframework.align.Json2Xml;
import nl.nn.adapterframework.align.Xml2Json;
import nl.nn.adapterframework.align.XmlAligner;
import nl.nn.adapterframework.align.XmlTypeToJsonSchemaConverter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.validation.ValidationContext;
import nl.nn.adapterframework.validation.XmlValidatorException;
import nl.nn.adapterframework.xml.NamespaceRemovingFilter;
import nl.nn.adapterframework.xml.RootElementToSessionKeyFilter;
import nl.nn.adapterframework.xml.XmlWriter;

/**
*<code>Pipe</code> that validates the XML or JSON input message against a XML-Schema and returns either XML or JSON.
*
* <table border="1">
* <tr><th>state</th><th>condition</th></tr>
* <tr><td>"success"</td><td>default</td></tr>
* <tr><td>"parserError"</td><td>a parser exception occurred, probably caused by non-well-formed XML. If not specified, "failure" is used in such a case</td></tr>
* <tr><td>"illegalRoot"</td><td>if the required root element is not found. If not specified, "failure" is used in such a case</td></tr>
* <tr><td>"failure"</td><td>if a validation error occurred</td></tr>
* </table>
* <br>
* @author Gerrit van Brakel
*/
public class Json2XmlValidator extends XmlValidator implements HasPhysicalDestination {

	public static final String INPUT_FORMAT_SESSION_KEY_PREFIX = "Json2XmlValidator.inputformat ";
	
	public final String FORMAT_XML="xml";
	public final String FORMAT_JSON="json";
	
	private boolean compactJsonArrays=true;
	private boolean strictJsonArraySyntax=false;
	private boolean jsonWithRootElements=false;
	private boolean deepSearch=false;
	private String targetNamespace;
	private String outputFormat=FORMAT_XML;
	private boolean autoFormat=true;
	private String inputFormatSessionKey=null;
	private String outputFormatSessionKey="outputFormat";
	private boolean failOnWildcards=true;
	private boolean acceptNamespaceLessXml=false;
	private boolean produceNamespaceLessXml=false;
	private boolean validateJsonToRootElementOnly=true;

	{
		setSoapNamespace("");
	}
	
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isNotEmpty(getSoapNamespace())) {
			throw new ConfigurationException("soapNamespace attribute not supported");
		}
		if (log.isDebugEnabled()) log.debug(getLogPrefix(null)+getPhysicalDestinationName());
		if (StringUtils.isEmpty(getInputFormatSessionKey())) {
			setInputFormatSessionKey(INPUT_FORMAT_SESSION_KEY_PREFIX+getName());
		}
	}
	
	public String getOutputFormat(PipeLineSession session, boolean responseMode) throws PipeRunException {
		String format=null;
		try {
			if (StringUtils.isNotEmpty(getOutputFormatSessionKey())) {
				format=session.getMessage(getOutputFormatSessionKey()).asString();
			}
			if (StringUtils.isEmpty(format) && isAutoFormat() && responseMode) {
				format=session.getMessage(getInputFormatSessionKey()).asString();
			}
			if (StringUtils.isEmpty(format)) {
				format=getOutputFormat();
			}
		} catch(IOException e) {
			throw new PipeRunException(this, "cannot get output format", e);
		}
		return format;
	}
	
	protected void storeInputFormat(String format, PipeLineSession session, boolean responseMode) {
		if (!responseMode) {
			String sessionKey = getInputFormatSessionKey();
			if (log.isDebugEnabled()) log.debug("storing inputFormat ["+format+"] under session key ["+sessionKey+"]");
			session.put(sessionKey, format);
		}
	}
	
	/**
	 * Validate the XML or JSON input, and align/convert it into JSON or XML according to a XML-Schema. 
	 * The format of the input message (XML or JSON) is automatically detected.
	 * @throws PipeRunException when <code>isThrowException</code> is true and a validationerror occurred.
	 */
	@Override
	public PipeRunResult doPipe(Message input, PipeLineSession session, boolean responseMode, String messageRoot) throws PipeRunException {
		String messageToValidate;
		try {
			messageToValidate=input==null || input.asObject()==null?"{}":input.asString();
		} catch (IOException e) {
			throw new PipeRunException(this, getLogPrefix(session)+"cannot open stream", e);
		}
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
					messageToValidate=addNamespace(messageToValidate); // TODO: do this via a filter
					//if (log.isDebugEnabled()) log.debug("added namespace to message ["+messageToValidate+"]");
				}
				storeInputFormat(FORMAT_XML,session, responseMode);
				if (!getOutputFormat(session,responseMode).equalsIgnoreCase(FORMAT_JSON)) {
					PipeRunResult result=super.doPipe(new Message(messageToValidate),session, responseMode, messageRoot);
					if (isProduceNamespaceLessXml()) {
						try {
							result.setResult(XmlUtils.removeNamespaces(result.getResult().asString()));
						} catch (IOException e) {
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
	
	protected Set<List<String>> getJsonRootValidations(boolean responseMode) {
		if (isValidateJsonToRootElementOnly()) {
			String root=getMessageRoot(responseMode);
			if (StringUtils.isEmpty(root)) {
				return null;
			}
			List<String> resultList = new LinkedList<>();
			resultList.add(root);
			Set<List<String>> resultSet = new HashSet<>();
			resultSet.add(resultList);
			return resultSet;
		} 
		return getRootValidations(responseMode);
	}
	
	protected PipeRunResult alignXml2Json(String messageToValidate, PipeLineSession session, boolean responseMode) throws XmlValidatorException, PipeRunException, ConfigurationException {

		ValidationContext context = validator.createValidationContext(session, getJsonRootValidations(responseMode), getInvalidRootNamespaces());
		ValidatorHandler validatorHandler = validator.getValidatorHandler(session,context);
		
		// Make sure to use Xerces' ValidatorHandlerImpl, otherwise casting below will fail.
		XmlAligner aligner = new XmlAligner(validatorHandler);
		Xml2Json xml2json = new Xml2Json(aligner, isCompactJsonArrays(), !isJsonWithRootElements());

		XMLFilterImpl handler = xml2json;

		if (StringUtils.isNotEmpty(getRootElementSessionKey())) {
			handler = new RootElementToSessionKeyFilter(session, getRootElementSessionKey(), getRootNamespaceSessionKey(), handler);
		}
		
		aligner.setContentHandler(handler);
		aligner.setErrorHandler(context.getErrorHandler());
		
		String resultEvent= validator.validate(messageToValidate, session, getLogPrefix(session), validatorHandler, xml2json, context);
		String out=xml2json.toString();
		PipeForward forward=determineForward(resultEvent, session, responseMode);
		PipeRunResult result=new PipeRunResult(forward,out);
		return result;
	}
	
	protected PipeRunResult alignJson(String messageToValidate, PipeLineSession session, boolean responseMode) throws PipeRunException, XmlValidatorException {

		ValidationContext context;
		ValidatorHandler validatorHandler;
		try {
			context = validator.createValidationContext(session, getJsonRootValidations(responseMode), getInvalidRootNamespaces());
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
			JsonStructure jsonStructure = Json.createReader(new StringReader(messageToValidate)).read();
	
			// cannot build filter chain as usual backwardly, because it ends differently. 
			// This will be fixed once an OutputStream can be provided to Xml2Json
			XMLFilterImpl sourceFilter = aligner;
			if (StringUtils.isNotEmpty(getRootElementSessionKey())) {
				XMLFilterImpl storeRootFilter = new RootElementToSessionKeyFilter(session, getRootElementSessionKey(), getRootNamespaceSessionKey(), null);
				aligner.setContentHandler(storeRootFilter);
				sourceFilter=storeRootFilter;
			}
			
			if (getOutputFormat(session,responseMode).equalsIgnoreCase(FORMAT_JSON)) {
				Xml2Json xml2json = new Xml2Json(aligner, isCompactJsonArrays(), !isJsonWithRootElements());
				sourceFilter.setContentHandler(xml2json);
				aligner.startParse(jsonStructure);
				out=xml2json.toString();
			} else {
				XmlWriter xmlWriter = new XmlWriter();
				xmlWriter.setIncludeXmlDeclaration(true);
				ContentHandler handler = xmlWriter;
				if (isProduceNamespaceLessXml()) {
					handler = new NamespaceRemovingFilter(handler);
				}
				sourceFilter.setContentHandler(handler);
				aligner.startParse(jsonStructure);
				out = xmlWriter.toString();
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


	@IbisDoc({"1", "Only for json input: namespace of the resulting xml. Need only be specified when the namespace of root name is ambiguous in the schema", ""})
	public void setTargetNamespace(String targetNamespace) {
		this.targetNamespace = targetNamespace;
	}
	public String getTargetNamespace() {
		return targetNamespace;
	}

	@IbisDoc({"2", "Default format of the result. Either 'xml' or 'json'", "xml"})
	public void setOutputFormat(String outputFormat) {
		this.outputFormat = outputFormat;
	}
	public String getOutputFormat() {
		return outputFormat;
	}

	@IbisDoc({"3", "Session key to retrieve outputformat from.", "outputformat"})
	public void setOutputFormatSessionKey(String outputFormatSessionKey) {
		this.outputFormatSessionKey = outputFormatSessionKey;
	}
	public String getOutputFormatSessionKey() {
		return outputFormatSessionKey;
	}

	@IbisDoc({"4", "Session key to store the inputformat in, to be able to set the outputformat when autoForamat=true.", "Json2XmlValidator.inputformat +<name of the pipe>"})
	public void setInputFormatSessionKey(String inputFormatSessionKey) {
		this.inputFormatSessionKey = inputFormatSessionKey;
	}
	public String getInputFormatSessionKey() {
		return inputFormatSessionKey;
	}

	@IbisDoc({"5", "If true, the format on 'output' is set to the same as the format of the input message on 'input'. The format of the input message is stored in and retrieved from the session variable specified by outputFormatSessionKey", "true"})
	public void setAutoFormat(boolean autoFormat) {
		this.autoFormat = autoFormat;
	}
	public boolean isAutoFormat() {
		return autoFormat;
	}

	@IbisDoc({"6", "If true assume arrays in json do not have the element containers like in xml", "true"})
	public void setCompactJsonArrays(boolean compactJsonArrays) {
		this.compactJsonArrays = compactJsonArrays;
	}
	public boolean isCompactJsonArrays() {
		return compactJsonArrays;
	}

	@IbisDoc({"7", "If true check that incoming json adheres to the specified syntax (compact or full), otherwise both types are accepted for conversion from json to xml", "false"})
	public void setStrictJsonArraySyntax(boolean strictJsonArraySyntax) {
		this.strictJsonArraySyntax = strictJsonArraySyntax;
	}
	public boolean isStrictJsonArraySyntax() {
		return strictJsonArraySyntax;
	}

	@IbisDoc({"8", "If true, assume that json contains/must contain a root element", "false"})
	public void setJsonWithRootElements(boolean jsonWithRootElements) {
		this.jsonWithRootElements = jsonWithRootElements;
	}
	public boolean isJsonWithRootElements() {
		return jsonWithRootElements;
	}

	@IbisDoc({"9", "If true, and converting from json to xml, parameter substitutions are searched for optional sub elements too. By default, only mandatory elements are searched for parameter substitutions. N.B. Currenlty this option might cause problems. Please try using more qualified parameters names (using '/') first", "false"})
	public void setDeepSearch(boolean deepSearch) {
		this.deepSearch = deepSearch;
	}
	public boolean isDeepSearch() {
		return deepSearch;
	}

	@IbisDoc({"10", "If true, an exception is thrown when a wildcard is found in the xml schema when parsing an object. This often indicates that an element is not properly typed in the xml schema, and could lead to ambuigities.", "true"})
	public void setFailOnWildcards(boolean failOnWildcards) {
		this.failOnWildcards = failOnWildcards;
	}
	public boolean isFailOnWildcards() {
		return failOnWildcards;
	}

	@IbisDoc({"11", "If true, all xml is allowed to be without namespaces. If no namespaces are detected (by the presence of the string 'xmlns') in the xml string, the root namespace is added to the xml", "false"})
	public void setAcceptNamespaceLessXml(boolean acceptNamespaceLessXml) {
		this.acceptNamespaceLessXml = acceptNamespaceLessXml;
	}
	public boolean isAcceptNamespaceLessXml() {
		return acceptNamespaceLessXml;
	}

	@IbisDoc({"12", "If true, all xml that is generated is without a namespace set", "false"})
	public void setProduceNamespaceLessXml(boolean produceNamespaceLessXml) {
		this.produceNamespaceLessXml = produceNamespaceLessXml;
	}
	public boolean isProduceNamespaceLessXml() {
		return produceNamespaceLessXml;
	}

	@IbisDoc({"13", "If true, and converting to or from json, then the message root is the only rootValidation, ignoring root validations like for SOAP envelope and header set by descender classes like SoapValidator", "true"})
	public void setValidateJsonToRootElementOnly(boolean validateJsonToRootElementOnly) {
		this.validateJsonToRootElementOnly = validateJsonToRootElementOnly;
	}
	public boolean isValidateJsonToRootElementOnly() {
		return validateJsonToRootElementOnly;
	}

}
