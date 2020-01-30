/*
   Copyright 2013, 2015-2017 Nationale-Nederlanden

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


import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;
import javax.xml.transform.TransformerConfigurationException;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.HasSpecialDefaultValues;
import nl.nn.adapterframework.core.IDualModeValidator;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.IXmlValidator;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.validation.AbstractXmlValidator;
import nl.nn.adapterframework.validation.Schema;
import nl.nn.adapterframework.validation.SchemaUtils;
import nl.nn.adapterframework.validation.SchemasProvider;
import nl.nn.adapterframework.validation.XSD;
import nl.nn.adapterframework.validation.XercesXmlValidator;
import nl.nn.adapterframework.validation.XmlValidatorException;


/**
*<code>Pipe</code> that validates the input message against a XML-Schema.
*
* <table border="1">
* <tr><th>state</th><th>condition</th></tr>
* <tr><td>"success"</td><td>default</td></tr>
* <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified, the value for "success"</td></tr>
* <tr><td>"parserError"</td><td>a parser exception occurred, probably caused by non-well-formed XML. If not specified, "failure" is used in such a case</td></tr>
* <tr><td>"illegalRoot"</td><td>if the required root element is not found. If not specified, "failure" is used in such a case</td></tr>
* <tr><td>"failure"</td><td>if a validation error occurred</td></tr>
* </table>
* <br>
* 
* @author Johan Verrips IOS
* @author Jaco de Groot
*/
public class XmlValidator extends FixedForwardPipe implements SchemasProvider, HasSpecialDefaultValues, IDualModeValidator, IXmlValidator {

	private String soapNamespace = "http://schemas.xmlsoap.org/soap/envelope/";
	private boolean forwardFailureToSuccess = false;
 
	private String root;
	private String responseRoot;
	private Set<List<String>> requestRootValidations;
	private Set<List<String>> responseRootValidations;
	private Map<List<String>, List<String>> invalidRootNamespaces;

	protected AbstractXmlValidator validator = new XercesXmlValidator();

	private TransformerPool transformerPoolExtractSoapBody;  // only used in getMessageToValidate(), TODO: avoid setting it up when not necessary
	private TransformerPool transformerPoolGetRootNamespace; // only used in getMessageToValidate(), TODO: avoid setting it up when not necessary
	private TransformerPool transformerPoolRemoveNamespaces; // only used in getMessageToValidate(), TODO: avoid setting it up when not necessary

	protected String schemaLocation;
	protected String noNamespaceSchemaLocation;
	protected String schemaSessionKey;

	protected double xsdProcessorVersion = AppConstants.getInstance(getConfigurationClassLoader()).getDouble("xsd.processor.version", 1.1);

	protected ConfigurationException configurationException;

	{
		setNamespaceAware(true);
	}

	/**
	 * Configure the XmlValidator
	 * @throws ConfigurationException when:
	 * <ul><li>the schema cannot be found</li>
	 * <ul><li><{@link #isThrowException()} is false and there is no forward defined
	 * for "failure"</li>
	 * <li>when the parser does not accept setting the properties for validating</li>
	 * </ul>
	 */
	@Override
	public void configure() throws ConfigurationException {
		try {
			super.configure();
			if ((StringUtils.isNotEmpty(getNoNamespaceSchemaLocation()) ||
					StringUtils.isNotEmpty(getSchemaLocation())) &&
					StringUtils.isNotEmpty(getSchemaSessionKey())) {
				throw new ConfigurationException(getLogPrefix(null) + "cannot have schemaSessionKey together with schemaLocation or noNamespaceSchemaLocation");
			}
			checkSchemaSpecified();
			if (StringUtils.isNotEmpty(getSoapNamespace())) {
				// Don't use this warning yet as it is used for the IFSA to Tibco
				// migration where an adapter with Tibco listener (with SOAP
				// Envelope and an adapter with IFSA listener (without SOAP Envelop)
				// call an adapter with XmlValidator which should validate both.
				// ConfigurationWarnings.getInstance().add(log, "Using XmlValidator with soapNamespace for Soap validation is deprecated. Please use " + SoapValidator.class.getName());
				String extractNamespaceDefs = "soapenv=" + getSoapNamespace();
				String extractBodyXPath     = "/soapenv:Envelope/soapenv:Body/*";
				try {
					transformerPoolExtractSoapBody = TransformerPool.getInstance(XmlUtils.createXPathEvaluatorSource(extractNamespaceDefs, extractBodyXPath, "xml"));
				} catch (TransformerConfigurationException te) {
					throw new ConfigurationException(getLogPrefix(null) + "got error creating transformer from getSoapBody", te);
				}
	
				transformerPoolGetRootNamespace = XmlUtils.getGetRootNamespaceTransformerPool();
				transformerPoolRemoveNamespaces = XmlUtils.getRemoveNamespacesTransformerPool(true, false);
			}
	
			if (!isForwardFailureToSuccess() && !isThrowException()){
				if (findForward("failure")==null) {
					throw new ConfigurationException(getLogPrefix(null)+ "must either set throwException true, forwardFailureToSuccess true or have a forward with name [failure]");
				}
			}
	
			// Different default value for ignoreUnknownNamespaces when using
			// noNamespaceSchemaLocation.
			if (validator.getIgnoreUnknownNamespaces() == null) {
				if (StringUtils.isNotEmpty(getNoNamespaceSchemaLocation())) {
					validator.setIgnoreUnknownNamespaces(true);
				} else {
					validator.setIgnoreUnknownNamespaces(false);
				}
			}
			validator.setSchemasProvider(this);

			// Set xsd processor version if xsdProcessorVersion is set
			if (validator instanceof XercesXmlValidator)
				((XercesXmlValidator) validator).setXsdVersion(xsdProcessorVersion);

			//do initial schema check
			if (getSchemasId()!=null) {
				getSchemas(true);
			}
			if (isRecoverAdapter()) {
				validator.reset();
			}
			validator.configure(getLogPrefix(null));
			registerEvent(AbstractXmlValidator.XML_VALIDATOR_PARSER_ERROR_MONITOR_EVENT);
			registerEvent(AbstractXmlValidator.XML_VALIDATOR_NOT_VALID_MONITOR_EVENT);
			registerEvent(AbstractXmlValidator.XML_VALIDATOR_VALID_MONITOR_EVENT);
		} catch(ConfigurationException e) {
			configurationException = e;
			throw e;
		}
		if (getRoot() == null) {
			ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
			String msg = getLogPrefix(null) + "Root not specified";
			configWarnings.add(log, msg);
		}
	}

	protected void checkSchemaSpecified() throws ConfigurationException {
		if (StringUtils.isEmpty(getNoNamespaceSchemaLocation()) &&
				StringUtils.isEmpty(getSchemaLocation()) &&
				StringUtils.isEmpty(getSchemaSessionKey())) {
			throw new ConfigurationException(getLogPrefix(null) + "must have either schemaSessionKey, schemaLocation or noNamespaceSchemaLocation");
		}
	}

	@Override
	public ConfigurationException getConfigurationException() {
		return configurationException;
	}


	/**
	 * Validate the XML string
	 * 
	 * @param input   a String
	 * @param session a {@link IPipeLineSession Pipelinesession}
	 * 
	 * @throws PipeRunException when <code>isThrowException</code> is true and a validationerror occurred.
	 */
	@Override
	public final PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		return doPipe(input, session, false);
	}

	public PipeRunResult doPipe(Object input, IPipeLineSession session, boolean responseMode) throws PipeRunException {
		String messageToValidate;
		if (StringUtils.isNotEmpty(getSoapNamespace())) {
			messageToValidate = getMessageToValidate(input, session);
		} else {
			messageToValidate = input.toString();
		}
		try {
			PipeForward forward = validate(messageToValidate, session, responseMode);
			return new PipeRunResult(forward, input);
		} catch (Exception e) {
			throw new PipeRunException(this, getLogPrefix(session), e);
		}

	}

	protected final PipeForward validate(String messageToValidate, IPipeLineSession session) throws XmlValidatorException, PipeRunException, ConfigurationException {
		return validate(messageToValidate, session, false);
	}

	protected PipeForward validate(String messageToValidate, IPipeLineSession session, boolean responseMode) throws XmlValidatorException, PipeRunException, ConfigurationException {
		String resultEvent = validator.validate(messageToValidate, session, getLogPrefix(session), getRootValidations(responseMode), getInvalidRootNamespaces(), false);
		return determineForward(resultEvent, session, responseMode);
	}

	protected PipeForward determineForward(String resultEvent, IPipeLineSession session, boolean responseMode) throws PipeRunException {
		throwEvent(resultEvent);
		if (AbstractXmlValidator.XML_VALIDATOR_VALID_MONITOR_EVENT.equals(resultEvent)) {
			return getForward();
		}
		PipeForward forward = null;
		if (AbstractXmlValidator.XML_VALIDATOR_PARSER_ERROR_MONITOR_EVENT.equals(resultEvent)) {
			if (responseMode) {
				forward = findForward("outputParserError");
			}
			if (forward == null) {
				forward = findForward("parserError");
			}
		}
		if (forward == null) {
			if (responseMode) {
				forward = findForward("outputFailure");
			}
			if (forward == null) {
				forward = findForward("failure");
			}
		}
		if (forward == null) {
			if (isForwardFailureToSuccess()) {
				forward = findForward("success");
			} else {
				throw new PipeRunException(this, "not implemented: should get reason from validator");
			}
		}
		return forward;
	}

	@Deprecated
	private String getMessageToValidate(Object input, IPipeLineSession session) throws PipeRunException {
		String inputStr = input.toString();
		if (XmlUtils.isWellFormed(inputStr, "Envelope")) {
			String inputRootNs;
			try {
				inputRootNs = transformerPoolGetRootNamespace.transform(inputStr, null);
			} catch (Exception e) {
				throw new PipeRunException(this, "cannot extract root namespace", e);
			}
			if (inputRootNs.equals(getSoapNamespace())) {
				log.debug(getLogPrefix(session) + "message to validate is a SOAP message");
				boolean extractSoapBody = true;
				if (StringUtils.isNotEmpty(getSchemaLocation())) {
					StringTokenizer st = new StringTokenizer(getSchemaLocation(), ", \t\r\n\f");
					while (st.hasMoreTokens() && extractSoapBody) {
						if (st.nextToken().equals(getSoapNamespace())) {
							extractSoapBody = false;
						}
					}
				}
				if (extractSoapBody) {
					log.debug(getLogPrefix(session) + "extract SOAP body for validation");
					try {
						inputStr = transformerPoolExtractSoapBody.transform(inputStr, null, true);
					} catch (Exception e) {
						throw new PipeRunException(this, "cannot extract SOAP body", e);
					}
					try {
						inputRootNs = transformerPoolGetRootNamespace.transform(inputStr, null);
					} catch (Exception e) {
						throw new PipeRunException(this, "cannot extract root namespace", e);
					}
					if (StringUtils.isNotEmpty(inputRootNs) && StringUtils.isEmpty(getSchemaLocation())) {
						log.debug(getLogPrefix(session) + "remove namespaces from extracted SOAP body");
						try {
							inputStr = transformerPoolRemoveNamespaces.transform(inputStr, null, true);
						} catch (Exception e) {
							throw new PipeRunException(this, "cannot remove namespaces", e);
						}
					}
				}
			}
		}
		return inputStr;
	}

	protected boolean isConfiguredForMixedValidation() {
		return responseRootValidations!=null && !responseRootValidations.isEmpty();
	}


	/**
	 * Enable full schema grammar constraint checking, including checking which may
	 * be time-consuming or memory intensive. Currently, particle unique attribution
	 * constraint checking and particle derivation resriction checking are
	 * controlled by this option.
	 * <p>
	 * see property http://apache.org/xml/features/validation/schema-full-checking
	 * </p>
	 * Defaults to <code>false</code>;
	 */
	@IbisDoc({ "perform addional memory intensive checks", "<code>false</code>" })
	public void setFullSchemaChecking(boolean fullSchemaChecking) {
		validator.setFullSchemaChecking(fullSchemaChecking);
	}

	public boolean isFullSchemaChecking() {
		return validator.isFullSchemaChecking();
	}

	/**
	 * <p>
	 * The filename of the schema on the classpath. It is not possible to specify a
	 * namespace using this attribute.
	 * <p>
	 * An example value would be "xml/xsd/GetPartyDetail.xsd"
	 * </p>
	 * <p>
	 * The value of the schema attribute is only used if the schemaLocation
	 * attribute and the noNamespaceSchemaLocation are not set
	 * </p>
	 * 
	 * @see ClassUtils#getResourceURL
	 */
	@IbisDoc({"the filename of the schema on the classpath. see doc on the method. (effectively the same as nonamespaceschemalocation)", "" })
	public void setSchema(String schema) {
		setNoNamespaceSchemaLocation(schema);
	}
	@Override
	public String getSchema() {
		return getNoNamespaceSchemaLocation();
	}

	/**
	 * <p>Pairs of URI references (one for the namespace name, and one for a
	 * hint as to the location of a schema document defining names for that
	 * namespace name).</p>
	 * <p> The syntax is the same as for schemaLocation attributes
	 * in instance documents: e.g, "http://www.example.com file%20name.xsd".</p>
	 * <p>The user can specify more than one XML Schema in the list.</p>
	 * <p><b>Note</b> that spaces are considered separators for this attributed.
	 * This means that, for example, spaces in filenames should be escaped to %20.
	 * </p>
	 *
	 * N.B. since 4.3.0 schema locations are resolved automatically, without the need for ${baseResourceURL}
	 */
	@IbisDoc({"pairs of uri references (one for the namespace name, and one for a hint as to the location of a schema document defining names for that namespace name). see doc on the method.", ""})
	public void setSchemaLocation(String schemaLocation) {
		this.schemaLocation = schemaLocation;
	}

	@Override
	public String getSchemaLocation() {
		return schemaLocation;
	}

	/**
	 * <p>A URI reference as a hint as to the location of a schema document with
	 * no target namespace.</p>
	 */
	@IbisDoc({"a uri reference as a hint as to the location of a schema document with no target namespace. see doc on the method.", ""})
	public void setNoNamespaceSchemaLocation(String noNamespaceSchemaLocation) {
		this.noNamespaceSchemaLocation = noNamespaceSchemaLocation;
	}

	public String getNoNamespaceSchemaLocation() {
		return noNamespaceSchemaLocation;
	}

	/**
	 * <p>The sessionkey to a value that is the uri to the schema definition.</P>
	 */
	@IbisDoc({" ", ""})
	public void setSchemaSessionKey(String schemaSessionKey) {
		this.schemaSessionKey = schemaSessionKey;
	}
	public String getSchemaSessionKey() {
		return schemaSessionKey;
	}

	/**
	 * @deprecated attribute name changed to {@link #setSchemaSessionKey(String) schemaSessionKey}
	 */
	public void setSchemaSession(String schemaSessionKey) {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = getLogPrefix(null) + "attribute 'schemaSession' is deprecated. Please use 'schemaSessionKey' instead.";
		configWarnings.add(log, msg);
		setSchemaSessionKey(schemaSessionKey);
	}


	/**
	 * Indicates wether to throw an error (piperunexception) when the xml is not
	 * compliant.
	 */
	@IbisDoc({"should the xmlvalidator throw a piperunexception on a validation error (if not, a forward with name 'failure' should be defined.", "<code>false</code>"})
	public void setThrowException(boolean throwException) {
		validator.setThrowException(throwException);
	}
	public boolean isThrowException() {
		return validator.isThrowException();
	}

	@IbisDoc({"if set: key of session variable to store reasons of mis-validation in", "failurereason"})
	public void setReasonSessionKey(String reasonSessionKey) {
		validator.setReasonSessionKey(reasonSessionKey);
	}
	public String getReasonSessionKey() {
		return validator.getReasonSessionKey();
	}

	@IbisDoc({"like <code>reasonsessionkey</code> but stores reasons in xml format and more extensive", "xmlfailurereason"})
	public void setXmlReasonSessionKey(String xmlReasonSessionKey) {
		validator.setXmlReasonSessionKey(xmlReasonSessionKey);
	}
	public String getXmlReasonSessionKey() {
		return validator.getXmlReasonSessionKey();
	}

	@IbisDoc({"name of the root element. or a comma separated list of names to choose from (only one is allowed)", ""})
	public void setRoot(String root) {
		this.root = root;
		addRequestRootValidation(Arrays.asList(root));
	}
	public String getRoot() {
		return root;
	}
	public void setResponseRoot(String responseRoot) {
		this.responseRoot = responseRoot;
		addResponseRootValidation(Arrays.asList(responseRoot));
	}
	protected String getResponseRoot() {
		return responseRoot;
	}

	@Override
	public String getMessageRoot() {
		return getRoot();
	}
	
	public String getMessageRoot(boolean responseMode) {
		return responseMode?getResponseRoot():getMessageRoot();
	}
    /**
     * Not ready yet (namespace not yet correctly parsed)
     */
    public QName getRootTag() {
        return new QName(getSchema()/* TODO*/, getRoot());
    }

	@IbisDoc({"when set <code>true</code>, the input is assumed to be the name of the file to be validated. otherwise the input itself is validated", "<code>false</code>"})
	public void setValidateFile(boolean b) {
		validator.setValidateFile(b);
	}
	public boolean isValidateFile() {
		return validator.isValidateFile();
	}

	@IbisDoc({"characterset used for reading file, only used when {@link #setValidateFile(boolean) validateFile} is <code>true</code>", "utf-8"})
	public void setCharset(String string) {
		validator.setCharset(string);
	}
	public String getCharset() {
		return  validator.getCharset();
	}

    public void setImplementation(Class<AbstractXmlValidator> clazz) throws IllegalAccessException, InstantiationException {
        validator = clazz.newInstance();
    }

    public boolean isAddNamespaceToSchema() {
        return validator.isAddNamespaceToSchema();
    }

	@IbisDoc({"when set <code>true</code>, the namespace from schemalocation is added to the schema document as targetnamespace", "<code>false</code>"})
    public void setAddNamespaceToSchema(boolean addNamespaceToSchema) {
        validator.setAddNamespaceToSchema(addNamespaceToSchema);
    }

	@IbisDoc({"comma separated list of schemalocations which are excluded from an import or include in the schema document", ""})
	public void setImportedSchemaLocationsToIgnore(String string) {
		validator.setImportedSchemaLocationsToIgnore(string);
    }

	public String getImportedSchemaLocationsToIgnore() {
		return validator.getImportedSchemaLocationsToIgnore();
	}

    public boolean isUseBaseImportedSchemaLocationsToIgnore() {
        return validator.isUseBaseImportedSchemaLocationsToIgnore();
    }

	@IbisDoc({"when set <code>true</code>, the comparison for importedschemalocationstoignore is done on base filename without any path", "<code>false</code>"})
    public void setUseBaseImportedSchemaLocationsToIgnore(boolean useBaseImportedSchemaLocationsToIgnore) {
        validator.setUseBaseImportedSchemaLocationsToIgnore(useBaseImportedSchemaLocationsToIgnore);
    }

	@IbisDoc({"comma separated list of namespaces which are excluded from an import or include in the schema document", ""})
	public void setImportedNamespacesToIgnore(String string) {
		validator.setImportedNamespacesToIgnore(string);
	}

	public String getImportedNamespacesToIgnore() {
		return validator.getImportedNamespacesToIgnore();
	}

    @Deprecated
	@IbisDoc({"The namespace of the SOAP envelope, when this property has a value and the input message is a SOAP message, " +
		"the content of the SOAP Body is used for validation, hence the SOAP Envelope and SOAP Body elements are not considered part of the message to validate. " +
		"Please note that this functionality is deprecated, using {@link nl.nn.adapterframework.soap.SoapValidator} "+
		"is now the preferred solution in case a SOAP message needs to be validated, in other cases give this property an empty value", "http://schemas.xmlsoap.org/soap/envelope/"})
	public void setSoapNamespace(String string) {
		soapNamespace = string;
    }

    @Deprecated
	public String getSoapNamespace() {
		return soapNamespace;
	}

	@IbisDoc({"when set <code>true</code>, send warnings to logging and console about syntax problems in the configured schema('s)", "<code>true</code>"})
	public void setWarn(boolean warn) {
        validator.setWarn(warn);
    }

	@IbisDoc({"ignore namespaces in the input message which are unknown", "true when schema or nonamespaceschemalocation is used, false otherwise"})
	public void setIgnoreUnknownNamespaces(boolean ignoreUnknownNamespaces) {
		validator.setIgnoreUnknownNamespaces(ignoreUnknownNamespaces);
	}

	public boolean getIgnoreUnknownNamespaces() {
		return validator.getIgnoreUnknownNamespaces();
	}

	@Override
	public String getSchemasId() {
		if (StringUtils.isNotEmpty(getNoNamespaceSchemaLocation())) {
			return getNoNamespaceSchemaLocation();
		} else if (StringUtils.isNotEmpty(getSchemaLocation())) {
			return getSchemaLocation();
		}
		return null;
	}

	@Override
	public Set<XSD> getXsds() throws ConfigurationException {
		Set<XSD> xsds = new HashSet<XSD>();
		if (StringUtils.isNotEmpty(getNoNamespaceSchemaLocation())) {
			XSD xsd = new XSD();
			xsd.initNoNamespace(getConfigurationClassLoader(), getNoNamespaceSchemaLocation());
			xsds.add(xsd);
		} else {
			String[] split =  schemaLocation.trim().split("\\s+");
			if (split.length % 2 != 0) throw new ConfigurationException("The schema must exist from an even number of strings, but it is " + schemaLocation);
			for (int i = 0; i < split.length; i += 2) {
				XSD xsd = new XSD();
				xsd.setAddNamespaceToSchema(isAddNamespaceToSchema());
				xsd.setImportedSchemaLocationsToIgnore(getImportedSchemaLocationsToIgnore());
				xsd.setUseBaseImportedSchemaLocationsToIgnore(isUseBaseImportedSchemaLocationsToIgnore());
				xsd.setImportedNamespacesToIgnore(getImportedNamespacesToIgnore());
				xsd.initNamespace(split[i], getConfigurationClassLoader(), split[i + 1]);
				xsds.add(xsd);
			}
		}
		return xsds;
	}

	@Override
	public List<Schema> getSchemas() throws ConfigurationException {
		return getSchemas(false);
	}

	public List<Schema> getSchemas(boolean checkRootValidations) throws ConfigurationException {
		Set<XSD> xsds = getXsds();
		if (StringUtils.isEmpty(getNoNamespaceSchemaLocation())) {
			xsds = SchemaUtils.getXsdsRecursive(xsds);
			if (checkRootValidations) {
				checkInputRootValidations(xsds);
				checkOutputRootValidations(xsds);
			}
			try {
				Map<String, Set<XSD>> xsdsGroupedByNamespace = SchemaUtils.getXsdsGroupedByNamespace(xsds, false);
				xsds = SchemaUtils.mergeXsdsGroupedByNamespaceToSchemasWithoutIncludes(getConfigurationClassLoader(), xsdsGroupedByNamespace, null);
			} catch(Exception e) {
				throw new ConfigurationException(getLogPrefix(null) + "could not merge schema's", e);
			}
		} else {
			// Support redefine for noNamespaceSchemaLocation for backwards
			// compatibility. It's deprecated in the latest specification:
			// http://www.w3.org/TR/xmlschema11-1/#modify-schema
			// It's difficult to support redefine in
			// mergeXsdsGroupedByNamespaceToSchemasWithoutIncludes so
			// we only use getXsdsRecursive here for root validation. Xerces
			// will need to resolve the imports, includes an redefines itself.
			// See comment in method XercesXmlValidator.stringToXMLInputSource()
			// too. The latest specification also specifies override:
			// http://www.w3.org/TR/xmlschema11-1/#override-schema
			// But this functionality doesn't seem to be (properly) supported by
			// Xerces (yet). WSDL generation was the main reason to introduce
			// mergeXsdsGroupedByNamespaceToSchemasWithoutIncludes, in case of
			// noNamespaceSchemaLocation the WSDL generator doesn't use
			// XmlValidator.getXsds(). See comment in Wsdl.getXsds() too.
			Set<XSD> xsds_temp = SchemaUtils.getXsdsRecursive(xsds, false);
			if (checkRootValidations) {
				checkInputRootValidations(xsds_temp);
				checkOutputRootValidations(xsds_temp);
			}
		}
		List<Schema> schemas = new ArrayList<Schema>();
		SchemaUtils.sortByDependencies(xsds, schemas);
		return schemas;
	}

	@Override
	public IPipe getResponseValidator() {
		if (isConfiguredForMixedValidation()) {
			return new ResponseValidatorWrapper(this);
		}
		return null;
	}
	
	public class ResponseValidatorWrapper implements IPipe,IXmlValidator {

		private String name;
		private Map<String, PipeForward> forwards=new HashMap<String, PipeForward>();
		
		protected XmlValidator owner;
		public ResponseValidatorWrapper(XmlValidator owner) {
			super();
			this.owner=owner;
			name="ResponseValidator of "+owner.getName();
		}
		
		@Override
		public String getName() {
			return name;
		}

	@IbisDoc({"name of the pipe", ""})
		@Override
		public void setName(String name) {
			this.name=name;
		}

		@Override
		public void configure() throws ConfigurationException {
		}

		@Override
		public ConfigurationException getConfigurationException() {
			return null;
		}

		@Override
		public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
			return owner.doPipe(input, session, true);
		}

		@Override
		public String getMessageRoot() {
			return owner.getResponseRoot();
		}
		
		@Override
		public int getMaxThreads() {
			return 0;
		}

		@Override
		public Map<String, PipeForward> getForwards() {
			return forwards;
		}

		@Override
		public String getType() {
			return owner.getType();
		}

		@Override
		public void registerForward(PipeForward forward) {
			forwards.put(forward.getName(), forward);
		}

		@Override
		public void start() throws PipeStartException {
		}

		@Override
		public void stop() {
		}

		@Override
		public String getSchemaLocation() {
			return owner.getSchemaLocation();
		}

		@Override
		public String getSchema() {
			return owner.getSchema();
		}

		@Override
		public Set<XSD> getXsds() throws ConfigurationException {
			return owner.getXsds();
		}

		
	}
	
	public boolean isMixedValidator(Object outputValidator) {
		return outputValidator==null && isConfiguredForMixedValidation();
	}

	public Set<List<String>> getRootValidations(boolean responseMode) {
		return responseMode ? responseRootValidations : requestRootValidations;
	} 

	private void checkInputRootValidations(Set<XSD> xsds) throws ConfigurationException {
		if (getRequestRootValidations() != null) {
			for (List<String> path: getRequestRootValidations()) {
				checkRootValidation(path, xsds);
			}
		}
	}

	private void checkOutputRootValidations(Set<XSD> xsds) throws ConfigurationException {
		if (getResponseRootValidations() != null) {
			for (List<String> path: getResponseRootValidations()) {
				checkRootValidation(path, xsds);
			}
		}
	}

	private void checkRootValidation(List<String> path, Set<XSD> xsds) throws ConfigurationException {
		boolean found = false;
		String validElements = path.get(path.size() - 1);
		List<String> validElementsAsList = Arrays.asList(validElements.split(","));
		for (String validElement : validElementsAsList) {
			if (StringUtils.isNotEmpty(validElement)) {
				List<String> allRootTags = new ArrayList<String>();
				for (XSD xsd : xsds) {
					for (String rootTag : xsd.getRootTags()) {
						allRootTags.add(rootTag);
						if (validElement.equals(rootTag)) {
							found = true;
						}
					}
				}
				if (!found) {
					ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
					String msg = getLogPrefix(null) + "Element '" + validElement +
					"' not in list of available root elements " + allRootTags;
					configWarnings.add(log, msg);
				}
			}
		}
	}

	@Override
	public String getSchemasId(IPipeLineSession session) throws PipeRunException {
		String schemaSessionKey = getSchemaSessionKey();
		if (schemaSessionKey != null) {
			if (session.containsKey(schemaSessionKey)) {
				return session.get(schemaSessionKey).toString();
			} 
			throw new PipeRunException(null, getLogPrefix(session) + "cannot retrieve xsd from session variable [" + schemaSessionKey + "]");
		}
		return null;
	}

	@Override
	public List<Schema> getSchemas(IPipeLineSession session) throws PipeRunException {
		List<Schema> xsds = new ArrayList<Schema>();
		String schemaLocation = getSchemasId(session);
		if (schemaSessionKey != null) {
			final URL url = ClassUtils.getResourceURL(getConfigurationClassLoader(), schemaLocation);
			if (url == null) {
				throw new PipeRunException(this, getLogPrefix(session) + "could not find schema at [" + schemaLocation + "]");
			}
			XSD xsd = new XSD();
			try {
				xsd.initNoNamespace(getConfigurationClassLoader(), schemaLocation);
			} catch (ConfigurationException e) {
				throw new PipeRunException(this, "Could not init xsd ["+schemaLocation+"]", e);
			}
			xsds.add(xsd);
			return xsds;
		}
		return null;
	}

	@IbisDoc({"when set <code>true</code>, the failure forward is replaced by the success forward (like a warning mode)", "<code>false</code>"})
	public void setForwardFailureToSuccess(boolean b) {
		this.forwardFailureToSuccess = b;
	}

	public Boolean isForwardFailureToSuccess() {
		return forwardFailureToSuccess;
	}

	@Override
	public Object getSpecialDefaultValue(String attributeName,
			Object defaultValue, Map<String, String> attributes) {
		// Different default value for ignoreUnknownNamespaces when using
		// noNamespaceSchemaLocation.
		if ("ignoreUnknownNamespaces".equals(attributeName)) {
			if (StringUtils.isNotEmpty(attributes.get("schema"))
					|| StringUtils.isNotEmpty(attributes.get("noNamespaceSchemaLocation"))) {
				return true;
			}
			return false;
		}
		return defaultValue;
	}
	@Deprecated
	protected void addRootValidation(List<String> path) {
		addRequestRootValidation(path);
	}
	
	protected void addRequestRootValidation(List<String> path) {
		if (requestRootValidations == null) {
			requestRootValidations = new LinkedHashSet<List<String>>();
		}
		requestRootValidations.add(path);
	}

	protected Set<List<String>> getRequestRootValidations() {
		return requestRootValidations;
	}

	protected void addResponseRootValidation(List<String> path) {
		if (responseRootValidations == null) {
			responseRootValidations = new LinkedHashSet<List<String>>();
		}
		responseRootValidations.add(path);
	}

	protected Set<List<String>> getResponseRootValidations() {
		return responseRootValidations;
	}

	public void addInvalidRootNamespaces(List<String> path, List<String> invalidRootNamespaces) {
		if (this.invalidRootNamespaces == null) {
			this.invalidRootNamespaces = new LinkedHashMap<List<String>, List<String>>();
		}
		this.invalidRootNamespaces.put(path, invalidRootNamespaces);
	}

	@IbisDoc({"when set <code>true</code>, the number for caching validators in appconstants is ignored and no caching is done (for this validator only)", "<code>false</code>"})
	public void setIgnoreCaching(boolean ignoreCaching) {
		validator.setIgnoreCaching(ignoreCaching);
	}

	@IbisDoc({"when set, the value in appconstants is overwritten (for this validator only)", "<code>application default (false)</code>"})
	public void setLazyInit(boolean lazyInit) {
		validator.setLazyInit(lazyInit);
	}


	@IbisDoc({"When set to <code>1.0</code>, Xerces's previous XML Schema factory will be used, which would make all XSD 1.1 features illegal. The default behaviour can also be set with <code>xsd.processor.version</code> property. ", "<code>1.1</code>"})
	public void setVersion(double version) {
		xsdProcessorVersion = version;
	}

	public double getVersion() {
		return xsdProcessorVersion;
	}

	public Map<List<String>, List<String>> getInvalidRootNamespaces() {
		return invalidRootNamespaces;
	}
}