/*
   Copyright 2013, 2015-2017 Nationale-Nederlanden, 2020 WeAreFrank!

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
import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang.StringUtils;
import org.apache.xerces.xs.XSModel;
import org.xml.sax.helpers.XMLFilterImpl;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.HasSpecialDefaultValues;
import nl.nn.adapterframework.core.IDualModeValidator;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.IValidator;
import nl.nn.adapterframework.core.IXmlValidator;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDocRef;
import nl.nn.adapterframework.soap.SoapVersion;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.validation.AbstractXmlValidator;
import nl.nn.adapterframework.validation.Schema;
import nl.nn.adapterframework.validation.SchemaUtils;
import nl.nn.adapterframework.validation.SchemasProvider;
import nl.nn.adapterframework.validation.ValidationContext;
import nl.nn.adapterframework.validation.XSD;
import nl.nn.adapterframework.validation.XercesXmlValidator;
import nl.nn.adapterframework.validation.XmlValidatorException;
import nl.nn.adapterframework.xml.RootElementToSessionKeyFilter;


/**
*<code>Pipe</code> that validates the input message against a XML-Schema.
*
* <table border="1">
* <tr><th>state</th><th>condition</th></tr>
* <tr><td>"success"</td><td>default</td></tr>
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

	private String schemaLocation;
	private String noNamespaceSchemaLocation;
	private String schemaSessionKey;
	private String root;
	private String responseRoot;
	private boolean forwardFailureToSuccess = false;
	private String soapNamespace = SoapVersion.SOAP11.namespace;
	private String rootElementSessionKey;
	private String rootNamespaceSessionKey;


	private Set<List<String>> requestRootValidations;
	private Set<List<String>> responseRootValidations;
	private Map<List<String>, List<String>> invalidRootNamespaces;

	protected AbstractXmlValidator validator = new XercesXmlValidator();

	private TransformerPool transformerPoolExtractSoapBody;  // only used in getMessageToValidate(), TODO: avoid setting it up when not necessary
	private TransformerPool transformerPoolGetRootNamespace; // only used in getMessageToValidate(), TODO: avoid setting it up when not necessary
	private TransformerPool transformerPoolRemoveNamespaces; // only used in getMessageToValidate(), TODO: avoid setting it up when not necessary

	protected ConfigurationException configurationException;

	protected final String ABSTRACTXMLVALIDATOR="nl.nn.adapterframework.validation.AbstractXmlValidator";
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
				throw new ConfigurationException("cannot have schemaSessionKey together with schemaLocation or noNamespaceSchemaLocation");
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
					throw new ConfigurationException("got error creating transformer from getSoapBody", te);
				}
	
				transformerPoolGetRootNamespace = XmlUtils.getGetRootNamespaceTransformerPool();
				transformerPoolRemoveNamespaces = XmlUtils.getRemoveNamespacesTransformerPool(true, false);
			}
	
			if (!isForwardFailureToSuccess() && !isThrowException()){
				if (findForward("failure")==null) {
					throw new ConfigurationException("must either set throwException true, forwardFailureToSuccess true or have a forward with name [failure]");
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

			validator.configure(getLogPrefix(null));
			registerEvent(AbstractXmlValidator.XML_VALIDATOR_PARSER_ERROR_MONITOR_EVENT);
			registerEvent(AbstractXmlValidator.XML_VALIDATOR_NOT_VALID_MONITOR_EVENT);
			registerEvent(AbstractXmlValidator.XML_VALIDATOR_VALID_MONITOR_EVENT);
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
		if (StringUtils.isEmpty(getNoNamespaceSchemaLocation()) &&
				StringUtils.isEmpty(getSchemaLocation()) &&
				StringUtils.isEmpty(getSchemaSessionKey())) {
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
 
	/**
	 * Validate the XML string
	 * 
	 * @param message   a String
	 * @param session a {@link IPipeLineSession Pipelinesession}
	 * 
	 * @throws PipeRunException when <code>isThrowException</code> is true and a validationerror occurred.
	 */
	@Override
	public final PipeRunResult doPipe(Message message, IPipeLineSession session) throws PipeRunException {
		return doPipe(message, session, false);
	}

	public PipeRunResult doPipe(Message input, IPipeLineSession session, boolean responseMode) throws PipeRunException {
		String messageToValidate;
		if (StringUtils.isNotEmpty(getSoapNamespace())) {
			messageToValidate = getMessageToValidate(input, session);
		} else {
			try {
				messageToValidate = input.asString();
			} catch (IOException e) {
				throw new PipeRunException(this, getLogPrefix(session)+"cannot open stream", e);
			}
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
		ValidationContext context = validator.createValidationContext(session, getRootValidations(responseMode), getInvalidRootNamespaces());
		ValidatorHandler validatorHandler = validator.getValidatorHandler(session, context);
		XMLFilterImpl storeRootFilter = StringUtils.isNotEmpty(getRootElementSessionKey()) ? new RootElementToSessionKeyFilter(session, getRootElementSessionKey(), getRootNamespaceSessionKey(), null) : null;
		if (storeRootFilter!=null) {
			validatorHandler.setContentHandler(storeRootFilter);
		}
		String resultEvent = validator.validate(messageToValidate, session, getLogPrefix(session), validatorHandler, storeRootFilter, context);
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
	private String getMessageToValidate(Message message, IPipeLineSession session) throws PipeRunException {
		String input;
		try {
			input = message.asString();
		} catch (IOException e) {
			throw new PipeRunException(this, getLogPrefix(session)+"cannot open stream", e);
		}
		if (XmlUtils.isWellFormed(input, "Envelope")) {
			String inputRootNs;
			try {
				inputRootNs = transformerPoolGetRootNamespace.transform(input, null);
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
						log.debug(getLogPrefix(session) + "remove namespaces from extracted SOAP body");
						try {
							input = transformerPoolRemoveNamespaces.transform(input, null, true);
						} catch (Exception e) {
							throw new PipeRunException(this, "cannot remove namespaces", e);
						}
					}
				}
			}
		}
		return input;
	}

	protected boolean isConfiguredForMixedValidation() {
		return responseRootValidations!=null && !responseRootValidations.isEmpty();
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
	public IValidator getResponseValidator() {
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
		public PipeRunResult doPipe(Message message, IPipeLineSession session) throws PipeRunException {
			return owner.doPipe(message, session, true);
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
					ConfigurationWarnings.add(this, log, "Element ["+validElement+"] not in list of available root elements "+allRootTags);
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
		if (getSchemaSessionKey() != null) {
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
	@IbisDoc({"1", "the filename of the schema on the classpath. see doc on the method. (effectively the same as noNamespaceSchemaLocation)", "" })
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
	@IbisDoc({"2", "Pairs of uri references (one for the namespace name, and one for a hint as to the location of a schema document defining names for that namespace name). see doc on the method.", ""})
	public void setSchemaLocation(String schemaLocation) {
		this.schemaLocation = schemaLocation;
	}
	@Override
	public String getSchemaLocation() {
		return schemaLocation;
	}

	@IbisDoc({"3", "A uri reference as a hint as to the location of a schema document with no target namespace.", ""})
	public void setNoNamespaceSchemaLocation(String noNamespaceSchemaLocation) {
		this.noNamespaceSchemaLocation = noNamespaceSchemaLocation;
	}
	public String getNoNamespaceSchemaLocation() {
		return noNamespaceSchemaLocation;
	}

	@IbisDoc({"4", "session key for retrieving a schema", ""})
	public void setSchemaSessionKey(String schemaSessionKey) {
		this.schemaSessionKey = schemaSessionKey;
	}
	public String getSchemaSessionKey() {
		return schemaSessionKey;
	}

	@IbisDoc({"5", "Name of the root element, or a comma separated list of names to choose from (only one is allowed)", ""})
	public void setRoot(String root) {
		this.root = root;
		if (root!=null) {
			addRequestRootValidation(Arrays.asList(root.split(",")));
		}
	}
	public String getRoot() {
		return root;
	}
	@IbisDoc({"6", "Name of the response root element, or a comma separated list of names to choose from (only one is allowed)", ""})
	public void setResponseRoot(String responseRoot) {
		this.responseRoot = responseRoot;
		if (responseRoot!=null) {
			addResponseRootValidation(Arrays.asList(responseRoot.split(",")));
		}
	}
	protected String getResponseRoot() {
		return responseRoot;
	}

	@Override
	public String getMessageRoot() {
		return getRoot();
	}

	@IbisDoc({"7", "If set <code>true</code>, the failure forward is replaced by the success forward (like a warning mode)", "<code>false</code>"})
	public void setForwardFailureToSuccess(boolean b) {
		this.forwardFailureToSuccess = b;
	}
	public boolean isForwardFailureToSuccess() {
		return forwardFailureToSuccess;
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

	@IbisDocRef({ABSTRACTXMLVALIDATOR})
	public void setAddNamespaceToSchema(boolean addNamespaceToSchema) {
		validator.setAddNamespaceToSchema(addNamespaceToSchema);
	}
	public boolean isAddNamespaceToSchema() {
		return validator.isAddNamespaceToSchema();
	}

	@IbisDocRef({ABSTRACTXMLVALIDATOR})
	public void setImportedSchemaLocationsToIgnore(String string) {
		validator.setImportedSchemaLocationsToIgnore(string);
	}
	public String getImportedSchemaLocationsToIgnore() {
		return validator.getImportedSchemaLocationsToIgnore();
	}


	@IbisDocRef({ABSTRACTXMLVALIDATOR})
	public void setUseBaseImportedSchemaLocationsToIgnore(boolean useBaseImportedSchemaLocationsToIgnore) {
		validator.setUseBaseImportedSchemaLocationsToIgnore(useBaseImportedSchemaLocationsToIgnore);
	}
	public boolean isUseBaseImportedSchemaLocationsToIgnore() {
		return validator.isUseBaseImportedSchemaLocationsToIgnore();
	}

	@IbisDocRef({ABSTRACTXMLVALIDATOR})
	public void setImportedNamespacesToIgnore(String string) {
		validator.setImportedNamespacesToIgnore(string);
	}
	public String getImportedNamespacesToIgnore() {
		return validator.getImportedNamespacesToIgnore();
	}

	@IbisDocRef({ABSTRACTXMLVALIDATOR})
	public void setWarn(boolean warn) {
		validator.setWarn(warn);
	}

	@IbisDocRef({ABSTRACTXMLVALIDATOR})
	public void setIgnoreUnknownNamespaces(boolean ignoreUnknownNamespaces) {
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
		"Please note that this functionality is deprecated, using {@link nl.nn.adapterframework.soap.SoapValidator} "+
		"is now the preferred solution in case a SOAP message needs to be validated, in other cases give this property an empty value", "http://schemas.xmlsoap.org/soap/envelope/"})
	public void setSoapNamespace(String string) {
		soapNamespace = string;
	}
	@Deprecated
	public String getSoapNamespace() {
		return soapNamespace;
	}

	@IbisDoc({"40", "key of session variable to store the name of the root element",""})
	public void setRootElementSessionKey(String rootElementSessionKey) {
		this.rootElementSessionKey = rootElementSessionKey;
	}
	public String getRootElementSessionKey() {
		return rootElementSessionKey;
	}

	@IbisDoc({"41", "key of session variable to store the namespace of the root element",""})
	public void setRootNamespaceSessionKey(String rootNamespaceSessionKey) {
		this.rootNamespaceSessionKey = rootNamespaceSessionKey;
	}
	public String getRootNamespaceSessionKey() {
		return rootNamespaceSessionKey;
	}


}