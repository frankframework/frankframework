/*
   Copyright 2013, 2015-2017 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.xs.XSModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.xml.sax.helpers.XMLFilterImpl;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.HasSpecialDefaultValues;
import nl.nn.adapterframework.core.IValidator;
import nl.nn.adapterframework.core.IXmlValidator;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.doc.Category;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDocRef;
import nl.nn.adapterframework.soap.SoapVersion;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.SpringUtils;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.TransformerPool.OutputType;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.validation.AbstractXmlValidator;
import nl.nn.adapterframework.validation.AbstractXmlValidator.ValidationResult;
import nl.nn.adapterframework.validation.RootValidation;
import nl.nn.adapterframework.validation.RootValidations;
import nl.nn.adapterframework.validation.Schema;
import nl.nn.adapterframework.validation.SchemaUtils;
import nl.nn.adapterframework.validation.SchemasProvider;
import nl.nn.adapterframework.validation.ValidationContext;
import nl.nn.adapterframework.validation.XSD;
import nl.nn.adapterframework.validation.XercesXmlValidator;
import nl.nn.adapterframework.validation.XmlValidatorException;
import nl.nn.adapterframework.xml.RootElementToSessionKeyFilter;


/**
 * Pipe that validates the input message against a XML Schema.
 *
 * @ff.forward parserError a parser exception occurred, probably caused by non-well-formed XML. If not specified, <code>failure</code> is used in such a case.
 * @ff.forward failure The document is not valid according to the configured schema.
 * @ff.forward warnings warnings occurred. If not specified, <code>success</code> is used.
 * @ff.forward outputParserError a <code>parserError</code> when validating a response. If not specified, <code>parserError</code> is used.
 * @ff.forward outputFailure a <code>failure</code> when validating a response. If not specified, <code>failure</code> is used.
 * @ff.forward outputWarnings warnings occurred when validating a response. If not specified, <code>warnings</code> is used.
 *
 * @author Johan Verrips IOS
 * @author Jaco de Groot
 */
@Category("Basic")
public class XmlValidator extends ValidatorBase implements SchemasProvider, HasSpecialDefaultValues, IXmlValidator, InitializingBean {

	private @Getter String schemaLocation;
	private @Getter String noNamespaceSchemaLocation;
	private String soapNamespace = SoapVersion.SOAP11.namespace;
	private @Getter String rootElementSessionKey;
	private @Getter String rootNamespaceSessionKey;
	private @Getter boolean addNamespaceToSchema = false;
	private @Getter String importedSchemaLocationsToIgnore;
	private @Getter boolean useBaseImportedSchemaLocationsToIgnore = false;
	private @Getter String importedNamespacesToIgnore;

	/*
	 * Root validations are a set of lists.
	 * Each list corresponds to a path of elements from the root through the document
	 * All paths in the set must be matched for the validation to pass
	 * Therefore, rootValidations are a set of required paths.
	 * However, each element in a path can be a comma separated list of elements, of which one needs to match at that place.
	 */
	private RootValidations requestRootValidations;
	private RootValidations responseRootValidations;
	private Map<List<String>, List<String>> invalidRootNamespaces;

	protected AbstractXmlValidator validator = new XercesXmlValidator();

	private TransformerPool transformerPoolExtractSoapBody;  // only used in getMessageToValidate(), TODO: avoid setting it up when not necessary
	private TransformerPool transformerPoolGetRootNamespace; // only used in getMessageToValidate(), TODO: avoid setting it up when not necessary
	private TransformerPool transformerPoolRemoveNamespaces; // only used in getMessageToValidate(), TODO: avoid setting it up when not necessary

	protected ConfigurationException configurationException;

	protected final String ABSTRACTXMLVALIDATOR="nl.nn.adapterframework.validation.AbstractXmlValidator";

	@Override
	public void afterPropertiesSet() throws Exception {
		SpringUtils.autowireByName(getApplicationContext(), validator);
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
					transformerPoolExtractSoapBody = TransformerPool.getInstance(XmlUtils.createXPathEvaluatorSource(extractNamespaceDefs, extractBodyXPath, OutputType.XML));
				} catch (TransformerConfigurationException te) {
					throw new ConfigurationException("got error creating transformer from getSoapBody", te);
				}

				transformerPoolGetRootNamespace = XmlUtils.getGetRootNamespaceTransformerPool();
				transformerPoolRemoveNamespaces = XmlUtils.getRemoveNamespacesTransformerPool(true, false);
			}

			if (!isForwardFailureToSuccess() && !isThrowException()){
				if (findForward("failure")==null) {
					throw new ConfigurationException("must either set throwException=true or have a forward with name [failure]");
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

			//do initial schema check
			if (getSchemasId()!=null) {
				getSchemas(true);
			}

			validator.configure(this);
		} catch(ConfigurationException e) {
			configurationException = e;
			throw e;
		}
		if (getRoot() == null) {
			ConfigurationWarnings.add(this, log, "root not specified");
		}
	}

	@Override
	public void start() throws PipeStartException {
		try {
			validator.start();
			super.start();
		} catch (ConfigurationException e) {
			throw new PipeStartException("unable to start validator", e);
		}
	}

	@Override
	public void stop() {
		validator.stop();
		super.stop();
	}

	protected void checkSchemaSpecified() throws ConfigurationException {
		if (StringUtils.isNotEmpty(getSchemaLocation()) && StringUtils.isNotEmpty(getSchemaSessionKey())) {
			throw new ConfigurationException("cannot have schemaSessionKey together with schemaLocation");
		}
		if (StringUtils.isEmpty(getNoNamespaceSchemaLocation()) && StringUtils.isEmpty(getSchemaLocation()) && StringUtils.isEmpty(getSchemaSessionKey())) {
			throw new ConfigurationException("must have either schemaSessionKey, schemaLocation or noNamespaceSchemaLocation");
		}
	}

	@Override
	public ConfigurationException getConfigurationException() {
		return configurationException;
	}

	public List<XSModel> getXSModels() {
		return validator.getXSModels();
	}


	@Override
	public PipeRunResult doPipe(Message input, PipeLineSession session, boolean responseMode, String messageRoot) throws PipeRunException {
		try {
			Message messageToValidate;
			input.preserve();
			if (StringUtils.isNotEmpty(getSoapNamespace())) {
				messageToValidate = getMessageToValidate(input, session);
			} else {
				messageToValidate = input;
			}

			PipeForward forward = validate(messageToValidate, session, responseMode, messageRoot);
			return new PipeRunResult(forward, input);
		} catch (Exception e) {
			throw new PipeRunException(this, "Could not validate", e);
		}

	}

	@Override
	protected PipeForward validate(Message messageToValidate, PipeLineSession session, boolean responseMode, String messageRoot) throws XmlValidatorException, PipeRunException, ConfigurationException {
		ValidationContext context = null;
		if(StringUtils.isNotEmpty(messageRoot)) {
			context = validator.createValidationContext(session, createRootValidation(messageRoot), getInvalidRootNamespaces());
		} else {
			context = validator.createValidationContext(session, getRootValidations(responseMode), getInvalidRootNamespaces());
		}
		ValidatorHandler validatorHandler = validator.getValidatorHandler(session, context);
		XMLFilterImpl storeRootFilter = StringUtils.isNotEmpty(getRootElementSessionKey()) ? new RootElementToSessionKeyFilter(session, getRootElementSessionKey(), getRootNamespaceSessionKey(), null) : null;
		if (storeRootFilter!=null) {
			validatorHandler.setContentHandler(storeRootFilter);
		}
		ValidationResult resultEvent = validator.validate(messageToValidate, session, validatorHandler, storeRootFilter, context);
		return determineForward(resultEvent, session, responseMode);
	}

	protected RootValidations createRootValidation(String messageRoot) {
		return new RootValidations(messageRoot);
	}

	protected PipeForward determineForward(ValidationResult validationResult, PipeLineSession session, boolean responseMode) throws PipeRunException {
		return determineForward(validationResult, session, responseMode, ()-> {
			String errorMessage=session.get(getReasonSessionKey(), null);
			if (StringUtils.isEmpty(errorMessage)) {
				errorMessage = session.get(getXmlReasonSessionKey(), "unknown error");
			}
			return errorMessage;
		});
	}

	protected PipeRunResult getErrorResult(ValidationResult result, String reason, PipeLineSession session, boolean responseMode) throws PipeRunException {
		if (StringUtils.isNotEmpty(getReasonSessionKey())) {
			session.put(getReasonSessionKey(), reason);
		}
		PipeForward forward = determineForward(ValidationResult.PARSER_ERROR, session, responseMode);
		return new PipeRunResult(forward, Message.nullMessage());
	}

	@Deprecated
	private Message getMessageToValidate(Message message, PipeLineSession session) throws PipeRunException {
		String input;
		try {
			input = message.asString();
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot open stream", e);
		}
		if (XmlUtils.isWellFormed(input, "Envelope")) {
			String inputRootNs;
			try {
				inputRootNs = transformerPoolGetRootNamespace.transform(input, null);
			} catch (Exception e) {
				throw new PipeRunException(this, "cannot extract root namespace", e);
			}
			if (inputRootNs.equals(getSoapNamespace())) {
				log.debug("message to validate is a SOAP message");
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
					log.debug("extract SOAP body for validation");
					try {
						input = transformerPoolExtractSoapBody.transform(input, null, true);
					} catch (Exception e) {
						throw new PipeRunException(this, "cannot extract SOAP body", e);
					}
					try {
						inputRootNs = transformerPoolGetRootNamespace.transform(input, null);
					} catch (Exception e) {
						throw new PipeRunException(this, "cannot extract root namespace", e);
					}
					if (StringUtils.isNotEmpty(inputRootNs) && StringUtils.isEmpty(getSchemaLocation())) {
						log.debug("remove namespaces from extracted SOAP body");
						try {
							input = transformerPoolRemoveNamespaces.transform(input, null, true);
						} catch (Exception e) {
							throw new PipeRunException(this, "cannot remove namespaces", e);
						}
					}
				}
			}
		}
		return new Message(input);
	}

	@Override
	protected boolean isConfiguredForMixedValidation() {
		return responseRootValidations!=null;
	}

	public String getMessageRoot(boolean responseMode) {
		return responseMode ? getResponseRoot() : getMessageRoot();
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
		Set<XSD> xsds = new LinkedHashSet<XSD>();
		if (StringUtils.isNotEmpty(getNoNamespaceSchemaLocation())) {
			XSD xsd = new XSD();
			xsd.initNoNamespace(this, getNoNamespaceSchemaLocation());
			xsds.add(xsd);
		} else {
			String[] split =  getSchemaLocation().trim().split("\\s+");
			if (split.length % 2 != 0) throw new ConfigurationException("The schema must exist from an even number of strings, but it is " + getSchemaLocation());
			for (int i = 0; i < split.length; i += 2) {
				XSD xsd = new XSD();
				xsd.setAddNamespaceToSchema(isAddNamespaceToSchema());
				xsd.setImportedSchemaLocationsToIgnore(getImportedSchemaLocationsToIgnore());
				xsd.setUseBaseImportedSchemaLocationsToIgnore(isUseBaseImportedSchemaLocationsToIgnore());
				xsd.setImportedNamespacesToIgnore(getImportedNamespacesToIgnore());
				xsd.initNamespace(split[i], this, split[i + 1]);
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
		if (StringUtils.isNotEmpty(getNoNamespaceSchemaLocation())) {
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
			Set<XSD> xsds_temp = SchemaUtils.getXsdsRecursive(xsds, true);
			if (checkRootValidations) {
				checkInputRootValidations(xsds_temp);
				checkOutputRootValidations(xsds_temp);
			}
		} else {
			xsds = SchemaUtils.getXsdsRecursive(xsds);
			if (checkRootValidations) {
				checkInputRootValidations(xsds);
				checkOutputRootValidations(xsds);
			}
			try {
				Map<String, Set<XSD>> xsdsGroupedByNamespace = SchemaUtils.getXsdsGroupedByNamespace(xsds, false);
				xsds = SchemaUtils.mergeXsdsGroupedByNamespaceToSchemasWithoutIncludes(this, xsdsGroupedByNamespace, null); // also handles addNamespaceToSchema
			} catch(Exception e) {
				throw new ConfigurationException("could not merge schema's", e);
			}
		}
		List<Schema> schemas = new ArrayList<Schema>();
		SchemaUtils.sortByDependencies(xsds, schemas);
		return schemas;
	}

	@Override
	public IValidator getResponseValidator() {
		if (isConfiguredForMixedValidation()) {
			return new ResponseValidatorWrapper(this);
		}
		return null;
	}

	public class ResponseValidatorWrapper implements IXmlValidator {

		private @Getter @Setter String name;

		private Map<String, PipeForward> forwards=new HashMap<String, PipeForward>();

		protected XmlValidator owner;
		public ResponseValidatorWrapper(XmlValidator owner) {
			super();
			this.owner=owner;
			name="ResponseValidator of "+owner.getName();
		}

		@Override
		public void configure() throws ConfigurationException {
		}

		@Override
		public ConfigurationException getConfigurationException() {
			return null;
		}

		@Override
		public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
			return owner.doPipe(message, session, true, null);
		}

		@Override
		public PipeRunResult validate(Message message, PipeLineSession session, String messageRoot) throws PipeRunException {
			return owner.doPipe(message, session, true, messageRoot);
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

		@Override
		public String getDocumentation() {
			return null;
		}

		@Override
		public ApplicationContext getApplicationContext() {
			return owner.getApplicationContext();
		}

		@Override
		public ClassLoader getConfigurationClassLoader() {
			return owner.getConfigurationClassLoader();
		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) {
			//Can ignore this as it's not set through Spring
		}

		@Override
		public boolean consumesSessionVariable(String sessionKey) {
			return owner.consumesSessionVariable(sessionKey);
		}
	}

//	public boolean isMixedValidator(Object outputValidator) {
//		return outputValidator==null && isConfiguredForMixedValidation();
//	}
//
	public RootValidations getRootValidations(boolean responseMode) {
		return responseMode ? responseRootValidations : requestRootValidations;
	}

	private void checkInputRootValidations(Set<XSD> xsds) throws ConfigurationException {
		if (getRequestRootValidations() != null) {
			getRequestRootValidations().check(this, xsds);
		}
	}

	private void checkOutputRootValidations(Set<XSD> xsds) throws ConfigurationException {
		if (getResponseRootValidations() != null) {
			getResponseRootValidations().check(this, xsds);
		}
	}

	@Override
	public String getSchemasId(PipeLineSession session) throws PipeRunException {
		String schemaSessionKey = getSchemaSessionKey();
		if (schemaSessionKey != null) {
			if (session.containsKey(schemaSessionKey)) {
				try {
					return session.getMessage(schemaSessionKey).asString();
				} catch(IOException e) {
					throw new PipeRunException(null, "cannot retrieve xsd from session variable [" + schemaSessionKey + "]");
				}
			}
			throw new PipeRunException(null, "cannot retrieve xsd from session variable [" + schemaSessionKey + "]");
		}
		return null;
	}

	@Override
	public List<Schema> getSchemas(PipeLineSession session) throws PipeRunException {
		List<Schema> xsds = new ArrayList<Schema>();
		String schemaLocation = getSchemasId(session);
		if (getSchemaSessionKey() != null) {
			final URL url = ClassUtils.getResourceURL(this, schemaLocation);
			if (url == null) {
				throw new PipeRunException(this, "could not find schema at [" + schemaLocation + "]");
			}
			XSD xsd = new XSD();
			try {
				xsd.initNoNamespace(this, schemaLocation);
			} catch (ConfigurationException e) {
				throw new PipeRunException(this, "Could not init xsd ["+schemaLocation+"]", e);
			}
			xsds.add(xsd);
			return xsds;
		}
		return null;
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
	protected void addRootValidation(RootValidation path) {
		addRequestRootValidation(path);
	}

	protected void addRequestRootValidation(RootValidation path) {
		if (requestRootValidations == null) {
			requestRootValidations = new RootValidations(path);
		} else {
			requestRootValidations.add(path);
		}
	}

	protected RootValidations getRequestRootValidations() {
		return requestRootValidations;
	}

	protected void addResponseRootValidation(RootValidation path) {
		if (responseRootValidations == null) {
			responseRootValidations = new RootValidations(path);
		} else {
			responseRootValidations.add(path);
		}
	}

	protected RootValidations getResponseRootValidations() {
		return responseRootValidations;
	}

	public void addInvalidRootNamespaces(List<String> path, List<String> invalidRootNamespaces) {
		if (this.invalidRootNamespaces == null) {
			this.invalidRootNamespaces = new LinkedHashMap<List<String>, List<String>>();
		}
		this.invalidRootNamespaces.put(path, invalidRootNamespaces);
	}

	public Map<List<String>, List<String>> getInvalidRootNamespaces() {
		return invalidRootNamespaces;
	}

	@Override
	public String getDocumentation() {
		return null;
	}

	public void setImplementation(Class<AbstractXmlValidator> clazz) throws IllegalAccessException, InstantiationException {
		validator = clazz.newInstance();
	}


	/**
	 * The filename of the schema on the classpath. It is not possible to specify a namespace using this attribute. (effectively the same as noNamespaceSchemaLocation)
	 * An example value would be "xml/xsd/GetPartyDetail.xsd".
	 * The value of the schema attribute is only used if the schemaLocation attribute and the noNamespaceSchemaLocation are not set.
	 */
	public void setSchema(String schema) {
		setNoNamespaceSchemaLocation(schema);
	}
	@Override
	public String getSchema() {
		return getNoNamespaceSchemaLocation();
	}

	/**
	 * Pairs of URI references (one for the namespace name, and one for a hint as to the location of a schema document defining names for that namespace name).<br/>
	 * The syntax is the same as for schemaLocation attributes in instance documents: e.g, "http://www.example.com file%20name.xsd".<br/>
	 * The user can specify more than one XML Schema in the list.<br/>
	 * <b>Note</b> that spaces are considered separators for this attributed. This means that, for example, spaces in filenames should be escaped to %20.
	 */
	public void setSchemaLocation(String schemaLocation) {
		this.schemaLocation = schemaLocation;
	}

	@IbisDoc({"A uri reference as a hint as to the location of a schema document with no target namespace.", ""})
	public void setNoNamespaceSchemaLocation(String noNamespaceSchemaLocation) {
		this.noNamespaceSchemaLocation = noNamespaceSchemaLocation;
	}


	@Override
	@IbisDoc({"Name of the root element, or a comma separated list of element names. The validation fails if the root element is not present in the list. N.B. for WSDL generation only the first element is used", ""})
	public void setRoot(String root) {
		super.setRoot(root);
		if (StringUtils.isNotEmpty(root)) {
			addRequestRootValidation(new RootValidation(root));
		}
	}
	@Override
	@IbisDoc({"Name of the response root element, or a comma separated list of element names. The validation fails if the root element is not present in the list. N.B. for WSDL generation only the first element is used", ""})
	public void setResponseRoot(String responseRoot) {
		super.setResponseRoot(responseRoot);
		if (StringUtils.isNotEmpty(responseRoot)) {
			addResponseRootValidation(new RootValidation(responseRoot));
		}
	}

	@Override
	public String getMessageRoot() {
		return getRoot();
	}

	@IbisDocRef({ABSTRACTXMLVALIDATOR})
	public void setFullSchemaChecking(boolean fullSchemaChecking) {
		validator.setFullSchemaChecking(fullSchemaChecking);
	}
	public boolean isFullSchemaChecking() {
		return validator.isFullSchemaChecking();
	}

	@IbisDocRef({ABSTRACTXMLVALIDATOR})
	public void setThrowException(boolean throwException) {
		validator.setThrowException(throwException);
	}
	@Override
	public boolean isThrowException() {
		return validator.isThrowException();
	}

	@IbisDocRef({ABSTRACTXMLVALIDATOR})
	public void setReasonSessionKey(String reasonSessionKey) {
		validator.setReasonSessionKey(reasonSessionKey);
	}
	public String getReasonSessionKey() {
		return validator.getReasonSessionKey();
	}

	@IbisDocRef({ABSTRACTXMLVALIDATOR})
	public void setXmlReasonSessionKey(String xmlReasonSessionKey) {
		validator.setXmlReasonSessionKey(xmlReasonSessionKey);
	}
	public String getXmlReasonSessionKey() {
		return validator.getXmlReasonSessionKey();
	}

	@IbisDocRef({ABSTRACTXMLVALIDATOR})
	public void setValidateFile(boolean b) {
		validator.setValidateFile(b);
	}
	public boolean isValidateFile() {
		return validator.isValidateFile();
	}

	@IbisDocRef({ABSTRACTXMLVALIDATOR})
	public void setCharset(String string) {
		validator.setCharset(string);
	}
	public String getCharset() {
		return  validator.getCharset();
	}

	@IbisDoc({"If set <code>true</code>, the namespace from schemalocation is added to the schema document as targetnamespace", "false"})
	public void setAddNamespaceToSchema(boolean addNamespaceToSchema) {
		this.addNamespaceToSchema = addNamespaceToSchema;
	}

	@IbisDoc({"Comma separated list of schemaLocations which are excluded from an import or include in the schema document", ""})
	public void setImportedSchemaLocationsToIgnore(String string) {
		importedSchemaLocationsToIgnore = string;
	}

	@IbisDoc({"If set <code>true</code>, the comparison for importedSchemaLocationsToIgnore is done on base filename without any path", "false"})
	public void setUseBaseImportedSchemaLocationsToIgnore(boolean useBaseImportedSchemaLocationsToIgnore) {
		this.useBaseImportedSchemaLocationsToIgnore = useBaseImportedSchemaLocationsToIgnore;
	}

	@IbisDoc({"Comma separated list of namespaces which are excluded from an import or include in the schema document", ""})
	public void setImportedNamespacesToIgnore(String string) {
		importedNamespacesToIgnore = string;
	}


	@IbisDocRef({ABSTRACTXMLVALIDATOR})
	public void setWarn(boolean warn) {
		validator.setWarn(warn);
	}

	@IbisDocRef({ABSTRACTXMLVALIDATOR})
	public void setIgnoreUnknownNamespaces(Boolean ignoreUnknownNamespaces) {
		validator.setIgnoreUnknownNamespaces(ignoreUnknownNamespaces);
	}
	public Boolean getIgnoreUnknownNamespaces() {
		return validator.getIgnoreUnknownNamespaces();
	}

	@IbisDocRef({ABSTRACTXMLVALIDATOR})
	public void setIgnoreCaching(boolean ignoreCaching) {
		validator.setIgnoreCaching(ignoreCaching);
	}

	@IbisDocRef({ABSTRACTXMLVALIDATOR})
	public void setXmlSchemaVersion(String xmlSchemaVersion) {
		validator.setXmlSchemaVersion(xmlSchemaVersion);
	}

	@Deprecated
	@IbisDoc({"The namespace of the SOAP envelope, when this property has a value and the input message is a SOAP message, " +
		"the content of the SOAP Body is used for validation, hence the SOAP Envelope and SOAP Body elements are not considered part of the message to validate. " +
		"Please note that this functionality is deprecated, using {@link SoapValidator} "+
		"is now the preferred solution in case a SOAP message needs to be validated, in other cases give this property an empty value", "http://schemas.xmlsoap.org/soap/envelope/"})
	public void setSoapNamespace(String string) {
		soapNamespace = string;
	}
	@Deprecated
	public String getSoapNamespace() {
		return soapNamespace;
	}

	@IbisDoc({"Key of session variable to store the name of the root element",""})
	public void setRootElementSessionKey(String rootElementSessionKey) {
		this.rootElementSessionKey = rootElementSessionKey;
	}

	@IbisDoc({"Key of session variable to store the namespace of the root element",""})
	public void setRootNamespaceSessionKey(String rootNamespaceSessionKey) {
		this.rootNamespaceSessionKey = rootNamespaceSessionKey;
	}

}