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
package nl.nn.adapterframework.pipes;

import java.io.StringWriter;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.json.JSONTokener;

import nl.nn.adapterframework.align.Json2Xml;
import nl.nn.adapterframework.align.Xml2Json;
import nl.nn.adapterframework.align.XmlAligner;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.validation.AbstractXmlValidator;
import nl.nn.adapterframework.validation.XercesJavaxXmlValidator;
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
* <tr><td>{@link #setSoapNamespace(String) soapNamespace}</td><td>the namespace of the SOAP Envelope, when this property has a value and the input message is a SOAP Message the content of the SOAP Body is used for validation, hence the SOAP Envelope and SOAP Body elements are not considered part of the message to validate. Please note that this functionality is deprecated, using {@link nl.nn.adapterframework.soap.SoapValidator} is now the preferred solution in case a SOAP Message needs to be validated, in other cases give this property an empty value</td><td>http://schemas.xmlsoap.org/soap/envelope/</td></tr>
* <tr><td>{@link #setIgnoreUnknownNamespaces(boolean) ignoreUnknownNamespaces}</td><td>ignore namespaces in the input message which are unknown</td><td>true when schema or noNamespaceSchemaLocation is used, false otherwise</td></tr>
* <tr><td>{@link #setWarn(boolean) warn}</td><td>when set <code>true</code>, send warnings to logging and console about syntax problems in the configured schema('s)</td><td><code>true</code></td></tr>
* <tr><td>{@link #setForwardFailureToSuccess(boolean) forwardFailureToSuccess}</td><td>when set <code>true</code>, the failure forward is replaced by the success forward (like a warning mode)</td><td><code>false</code></td></tr>
* <tr><td>{@link #setAddNamespaceToSchema(boolean) addNamespaceToSchema}</td><td>when set <code>true</code>, the namespace from schemaLocation is added to the schema document as targetNamespace</td><td><code>false</code></td></tr>
* <tr><td>{@link #setImportedSchemaLocationsToIgnore(String) importedSchemaLocationsToIgnore}</td><td>comma separated list of schemaLocations which are excluded from an import or include in the schema document</td><td>&nbsp;</td></tr>
* <tr><td>{@link #setUseBaseImportedSchemaLocationsToIgnore(boolean) useBaseImportedSchemaLocationsToIgnore}</td><td>when set <code>true</code>, the comparison for importedSchemaLocationsToIgnore is done on base filename without any path</td><td><code>false</code></td></tr>
* <tr><td>{@link #setImportedNamespacesToIgnore(String) importedNamespacesToIgnore}</td><td>comma separated list of namespaces which are excluded from an import or include in the schema document</td><td>&nbsp;</td></tr>
* <tr><td>{@link #setCompactJsonArrays(boolean) compactJsonArrays}</td><td>when true assume arrays in json do not have the element containers like in XML</td><td>true</td></tr>
* <tr><td>{@link #setOutputFormat(String) outputFormat}</td><td>format of the result. Either 'xml' or 'json'</td><td>xml</td></tr>
* <tr><td>{@link #setOutputFormatSessionKey(String) outputFormatSessionKey}</td><td>session key to retrieve outputFormat from.</td><td>outputFormat</td></tr>
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
* N.B. noNamespaceSchemaLocation may contain spaces, but not if the schema is stored in a .jar or .zip file on the class path.
* @author Johan Verrips IOS / Jaco de Groot (***@dynasol.nl) / Gerit van Brakel
*/
public class Json2XmlValidator extends XmlValidator {

	private boolean compactJsonArrays=true;
	private String targetNamespace;
	private String outputFormat="xml";
	protected String outputFormatSessionKey="outputFormat";


	public static final String JSON_XML_VALIDATOR_VALID_MONITOR_EVENT = "valid JSON";
	
	public Json2XmlValidator() {
		super();
		validator=new XercesJavaxXmlValidator();
	}
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isNotEmpty(getSoapNamespace())) {
			throw new ConfigurationException("soapNamespace attribute not supported");
		}
	}
	
    /**
     * Validate the XML string
     * @param input a String
     * @param session a {@link nl.nn.adapterframework.core.IPipeLineSession Pipelinesession}

     * @throws PipeRunException when <code>isThrowException</code> is true and a validationerror occurred.
     */
	@Override
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		String messageToValidate;
		messageToValidate = input.toString();
		int i=0;
		while (i<messageToValidate.length() && Character.isWhitespace(messageToValidate.charAt(i))) i++;
		if (i>=messageToValidate.length()) {
			throw new PipeRunException(this,"message contains only whitespace");
		}
		if (messageToValidate.charAt(i)=='<') {
			// message is XML
			if (!getOutputFormat(session).equalsIgnoreCase("json")) {
				return super.doPipe(input,session);
			}
			try {
				String json = alignXml2Json(messageToValidate, session);
				throwEvent(JSON_XML_VALIDATOR_VALID_MONITOR_EVENT);
				return new PipeRunResult(getForward(), json);
			} catch (Exception e) {
				throwEvent(AbstractXmlValidator.XML_VALIDATOR_NOT_VALID_MONITOR_EVENT);
				throw new PipeRunException(this, "Alignment failed",e);
			}
		}
		if (messageToValidate.charAt(i)!='{') {
			throw new PipeRunException(this,"message is not XML or JSON, because it does not start with '{' or '<'");
		}
		try {
			String xml = alignJson(messageToValidate, session);
			throwEvent(JSON_XML_VALIDATOR_VALID_MONITOR_EVENT);
			return new PipeRunResult(getForward(), xml);
		} catch (Exception e) {
			throwEvent(AbstractXmlValidator.XML_VALIDATOR_NOT_VALID_MONITOR_EVENT);
			throw new PipeRunException(this, "Alignment failed",e);
		}
	}
	
	protected String alignXml2Json(String messageToValidate, IPipeLineSession session)
			throws XmlValidatorException, PipeRunException, ConfigurationException {

		ValidatorHandler validatorHandler = ((XercesJavaxXmlValidator) validator).getValidatorHandler(session);
		XmlAligner aligner = new XmlAligner(validatorHandler);
		Xml2Json xml2json = new Xml2Json(aligner, isCompactJsonArrays());
		aligner.setContentHandler(xml2json);
		aligner.setSchemaInformation(((XercesJavaxXmlValidator) validator).getXSModels(session));
		
		((XercesJavaxXmlValidator) validator).validate(messageToValidate, session, getLogPrefix(session), validatorHandler, xml2json);
		String out=xml2json.toString();
		return out;
	}
	
	protected String alignJson(String messageToValidate, IPipeLineSession session)
			throws XmlValidatorException, PipeRunException, ConfigurationException {

		ValidatorHandler validatorHandler = ((XercesJavaxXmlValidator) validator).getValidatorHandler(session);
		Json2Xml aligner = new Json2Xml(getTargetNamespace(), validatorHandler, isCompactJsonArrays());
		aligner.setSchemaInformation(((XercesJavaxXmlValidator) validator).getXSModels(session));
		JSONTokener jsonTokener = new JSONTokener(messageToValidate);
		try {
			JSONObject json = new JSONObject(jsonTokener);
			String out;
			if (getOutputFormat(session).equalsIgnoreCase("json")) {
				Xml2Json xml2json = new Xml2Json(aligner, isCompactJsonArrays());
				aligner.setContentHandler(xml2json);
				aligner.startParse(json);
				out=xml2json.toString();
			} else {
				Source source = aligner.asSource(json);
				out = source2String(source);
			}
			return out;
		} catch (Exception e) {
			throw new PipeRunException(this,"Cannot align json", e);
		}

	}

	public static String source2String(Source source) throws TransformerException {
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		TransformerFactory tf = XmlUtils.getTransformerFactory(false);
		Transformer transformer = tf.newTransformer();
		transformer.transform(source, result);
		writer.flush();
		return writer.toString();
	}

	public String getOutputFormat(IPipeLineSession session) {
		String format=null;
		if (StringUtils.isNotEmpty(getOutputFormatSessionKey())) {
			format=(String)session.get(getOutputFormatSessionKey());
		}
		if (StringUtils.isEmpty(format)) {
			format=getOutputFormat();
		}	
		return format;
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

}
