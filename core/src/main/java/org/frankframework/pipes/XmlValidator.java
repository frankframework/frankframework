/*
   Copyright 2013, 2015-2017 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.xs.XSModel;
import org.springframework.beans.factory.InitializingBean;
import org.xml.sax.helpers.XMLFilterImpl;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.configuration.HasSpecialDefaultValues;
import org.frankframework.core.AbstractResponseValidatorWrapper;
import org.frankframework.core.IValidator;
import org.frankframework.core.IXmlValidator;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.Category;
import org.frankframework.doc.ReferTo;
import org.frankframework.soap.SoapValidator;
import org.frankframework.soap.SoapVersion;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.TransformerPool;
import org.frankframework.util.TransformerPool.OutputType;
import org.frankframework.util.XmlUtils;
import org.frankframework.validation.AbstractValidationContext;
import org.frankframework.validation.AbstractXSD;
import org.frankframework.validation.AbstractXmlValidator;
import org.frankframework.validation.AbstractXmlValidator.ValidationResult;
import org.frankframework.validation.IXSD;
import org.frankframework.validation.RootValidation;
import org.frankframework.validation.RootValidations;
import org.frankframework.validation.Schema;
import org.frankframework.validation.SchemaUtils;
import org.frankframework.validation.SchemasProvider;
import org.frankframework.validation.XercesXmlValidator;
import org.frankframework.validation.XmlValidatorException;
import org.frankframework.validation.xsd.ResourceXsd;
import org.frankframework.xml.RootElementToSessionKeyFilter;


/**
 * Pipe that validates the input message against an XML Schema.
 *
 * @author Johan Verrips IOS
 * @author Jaco de Groot
 */
@Category(Category.Type.BASIC)
public class XmlValidator extends AbstractValidator implements SchemasProvider, HasSpecialDefaultValues, IXmlValidator, InitializingBean {

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

	protected ConfigurationException configurationException;

	@Override
	public void afterPropertiesSet() throws Exception {
		SpringUtils.autowireByName(getApplicationContext(), validator);
	}

	/**
	 * Configure the XmlValidator
	 * @throws ConfigurationException when:
	 * <ul><li>the schema cannot be found</li>
	 * <ul><li>{@link #isThrowException()} is {@code false} and there is no forward defined
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
					transformerPoolExtractSoapBody = TransformerPool.getXPathTransformerPool(extractNamespaceDefs, extractBodyXPath, OutputType.XML);
				} catch (TransformerConfigurationException te) {
					throw new ConfigurationException("got error creating transformer from getSoapBody", te);
				}

				transformerPoolGetRootNamespace = XmlUtils.getGetRootNamespaceTransformerPool();
			}

			if (!isForwardFailureToSuccess() && !isThrowException() && findForward("failure") == null) {
				throw new ConfigurationException("must either set throwException=true or have a forward with name [failure]");
			}

			if (isThrowException () && findForward("failure") != null) {
				ConfigurationWarnings.add(this, log, "throwException=true, so the failure forward will be ignored");
			}

			// Different default value for ignoreUnknownNamespaces when using
			// noNamespaceSchemaLocation.
			if (validator.getIgnoreUnknownNamespaces() == null) {
				validator.setIgnoreUnknownNamespaces(StringUtils.isNotEmpty(getNoNamespaceSchemaLocation()));
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
	public void start() {
		validator.start();
		super.start();
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
				messageToValidate = getMessageToValidate(input);
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
		AbstractValidationContext context;
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

	protected PipeRunResult getErrorResult(String reason, PipeLineSession session, boolean responseMode) throws PipeRunException {
		if (StringUtils.isNotEmpty(getReasonSessionKey())) {
			session.put(getReasonSessionKey(), reason);
		}
		PipeForward forward = determineForward(ValidationResult.PARSER_ERROR, session, responseMode);
		return new PipeRunResult(forward, Message.nullMessage());
	}

	private Message getMessageToValidate(Message message) throws PipeRunException {
		if (XmlUtils.isWellFormed(message, "Envelope")) {
			String inputRootNs;
			try {
				inputRootNs = transformerPoolGetRootNamespace.transformToString(message);
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
					String input;

					try {
						SAXSource source = XmlUtils.inputSourceToSAXSource(message.asInputSource(), true, null);
						input = transformerPoolExtractSoapBody.transformToString(source);
					} catch (Exception e) {
						throw new PipeRunException(this, "cannot extract SOAP body", e);
					}
					try {
						inputRootNs = transformerPoolGetRootNamespace.transformToString(input, null);
					} catch (Exception e) {
						throw new PipeRunException(this, "cannot extract root namespace", e);
					}
					if (StringUtils.isNotEmpty(inputRootNs) && StringUtils.isEmpty(getSchemaLocation())) {
						log.debug("remove namespaces from extracted SOAP body");
						try {
							input = XmlUtils.removeNamespaces(input);
						} catch (Exception e) {
							throw new PipeRunException(this, "cannot remove namespaces", e);
						}
					}
					return new Message(input);
				}
			}
		}
		return message;
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
	public Set<IXSD> getXsds() throws ConfigurationException {
		Set<IXSD> xsds = new LinkedHashSet<>();
		if (StringUtils.isNotEmpty(getNoNamespaceSchemaLocation())) {
			ResourceXsd xsd = new ResourceXsd();
			xsd.initNoNamespace(this, getNoNamespaceSchemaLocation());
			xsds.add(xsd);
		} else {
			String[] split =  getSchemaLocation().trim().split("\\s+");
			if (split.length % 2 != 0) throw new ConfigurationException("The schema must exist from an even number of strings, but it is " + getSchemaLocation());
			for (int i = 0; i < split.length; i += 2) {
				ResourceXsd xsd = new ResourceXsd();
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
		Set<IXSD> xsds = getXsds();
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
			Set<IXSD> tempXsds = AbstractXSD.getXsdsRecursive(xsds, true);
			if (checkRootValidations) {
				checkInputRootValidations(tempXsds);
				checkOutputRootValidations(tempXsds);
			}
		} else {
			xsds = AbstractXSD.getXsdsRecursive(xsds);
			if (checkRootValidations) {
				checkInputRootValidations(xsds);
				checkOutputRootValidations(xsds);
			}
			try {
				Map<String, Set<IXSD>> xsdsGroupedByNamespace = SchemaUtils.groupXsdsByNamespace(xsds, false);
				xsds = SchemaUtils.mergeXsdsGroupedByNamespaceToSchemasWithoutIncludes(this, xsdsGroupedByNamespace); // also handles addNamespaceToSchema
			} catch(Exception e) {
				throw new ConfigurationException("could not merge schema's", e);
			}
		}
		List<Schema> schemas = new ArrayList<>();
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

	/**
	 * Wrapper for the XML response validator.
	 */
	public static class ResponseValidatorWrapper extends AbstractResponseValidatorWrapper<XmlValidator> implements IXmlValidator {

		public ResponseValidatorWrapper(XmlValidator owner) {
			super(owner);
		}

		@Override
		public ConfigurationException getConfigurationException() {
			return null;
		}

		@Override
		public String getMessageRoot() {
			return owner.getResponseRoot();
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
		public Set<IXSD> getXsds() throws ConfigurationException {
			return owner.getXsds();
		}

		@Override
		public String getDocumentation() {
			return null;
		}
	}

	public RootValidations getRootValidations(boolean responseMode) {
		return responseMode ? responseRootValidations : requestRootValidations;
	}

	private void checkInputRootValidations(Set<IXSD> xsds) throws ConfigurationException {
		if (getRequestRootValidations() != null) {
			getRequestRootValidations().check(this, xsds);
		}
	}

	private void checkOutputRootValidations(Set<IXSD> xsds) throws ConfigurationException {
		if (getResponseRootValidations() != null) {
			getResponseRootValidations().check(this, xsds);
		}
	}

	@Override
	public String getSchemasId(PipeLineSession session) throws PipeRunException {
		String schemaSessionKey = getSchemaSessionKey();
		if (schemaSessionKey != null) {
			if (session.containsKey(schemaSessionKey)) {
				return session.getString(schemaSessionKey);
			}
			throw new PipeRunException(null, "cannot retrieve xsd from session variable [" + schemaSessionKey + "]");
		}
		return null;
	}

	@Override
	public List<Schema> getSchemas(PipeLineSession session) throws PipeRunException {
		List<Schema> xsds = new ArrayList<>();
		String schemaLocation = getSchemasId(session);
		if (getSchemaSessionKey() != null) {
			final URL url = ClassLoaderUtils.getResourceURL(this, schemaLocation);
			if (url == null) {
				throw new PipeRunException(this, "could not find schema at [" + schemaLocation + "]");
			}
			ResourceXsd xsd = new ResourceXsd();
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

	/**
	 *
	 * @param path to the element from where to start validating namespaces
	 * @param invalidRootNamespaces XML namespace that is not allowed on the current element
	 */
	protected void addInvalidRootNamespaces(List<String> path, List<String> invalidRootNamespaces) {
		if (this.invalidRootNamespaces == null) {
			this.invalidRootNamespaces = new LinkedHashMap<>();
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

	public void setImplementation(Class<? extends AbstractXmlValidator> clazz) throws ReflectiveOperationException, SecurityException {
		validator = ClassUtils.newInstance(clazz);
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

	/** A URI reference as a hint as to the location of a schema document with no target namespace. */
	public void setNoNamespaceSchemaLocation(String noNamespaceSchemaLocation) {
		this.noNamespaceSchemaLocation = noNamespaceSchemaLocation;
	}


	/** Name of the root element, or a comma separated list of element names. The validation fails if the root element is not present in the list. N.B. for WSDL generation only the first element is used */
	@Override
	public void setRoot(String root) {
		super.setRoot(root);
		if (StringUtils.isNotEmpty(root)) {
			addRequestRootValidation(new RootValidation(root));
		}
	}
	/** Name of the response root element, or a comma separated list of element names. The validation fails if the root element is not present in the list. N.B. for WSDL generation only the first element is used */
	@Override
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

	@ReferTo(AbstractXmlValidator.class)
	public void setFullSchemaChecking(boolean fullSchemaChecking) {
		validator.setFullSchemaChecking(fullSchemaChecking);
	}
	public boolean isFullSchemaChecking() {
		return validator.isFullSchemaChecking();
	}

	@ReferTo(AbstractXmlValidator.class)
	public void setThrowException(boolean throwException) {
		validator.setThrowException(throwException);
	}
	@Override
	public boolean isThrowException() {
		return validator.isThrowException();
	}

	@ReferTo(AbstractXmlValidator.class)
	public void setReasonSessionKey(String reasonSessionKey) {
		validator.setReasonSessionKey(reasonSessionKey);
	}
	public String getReasonSessionKey() {
		return validator.getReasonSessionKey();
	}

	@ReferTo(AbstractXmlValidator.class)
	public void setXmlReasonSessionKey(String xmlReasonSessionKey) {
		validator.setXmlReasonSessionKey(xmlReasonSessionKey);
	}
	public String getXmlReasonSessionKey() {
		return validator.getXmlReasonSessionKey();
	}

	@ReferTo(AbstractXmlValidator.class)
	public void setValidateFile(boolean b) {
		validator.setValidateFile(b);
	}
	public boolean isValidateFile() {
		return validator.isValidateFile();
	}

	@ReferTo(AbstractXmlValidator.class)
	public void setCharset(String string) {
		validator.setCharset(string);
	}
	public String getCharset() {
		return  validator.getCharset();
	}

	/**
	 * If set <code>true</code>, the namespace from schemalocation is added to the schema document as targetnamespace
	 * @ff.default false
	 */
	public void setAddNamespaceToSchema(boolean addNamespaceToSchema) {
		this.addNamespaceToSchema = addNamespaceToSchema;
	}

	/** Comma separated list of schemaLocations which are excluded from an import or include in the schema document */
	public void setImportedSchemaLocationsToIgnore(String string) {
		importedSchemaLocationsToIgnore = string;
	}

	/**
	 * If {@code true}, the comparison for importedSchemaLocationsToIgnore is done on base filename without any path
	 * @ff.default false
	 */
	public void setUseBaseImportedSchemaLocationsToIgnore(boolean useBaseImportedSchemaLocationsToIgnore) {
		this.useBaseImportedSchemaLocationsToIgnore = useBaseImportedSchemaLocationsToIgnore;
	}

	/** Comma separated list of namespaces which are excluded from an import or include in the schema document */
	public void setImportedNamespacesToIgnore(String string) {
		importedNamespacesToIgnore = string;
	}


	@ReferTo(AbstractXmlValidator.class)
	public void setWarn(boolean warn) {
		validator.setWarn(warn);
	}

	@ReferTo(AbstractXmlValidator.class)
	public void setIgnoreUnknownNamespaces(Boolean ignoreUnknownNamespaces) {
		validator.setIgnoreUnknownNamespaces(ignoreUnknownNamespaces);
	}
	public Boolean getIgnoreUnknownNamespaces() {
		return validator.getIgnoreUnknownNamespaces();
	}

	@ReferTo(AbstractXmlValidator.class)
	public void setIgnoreCaching(boolean ignoreCaching) {
		validator.setIgnoreCaching(ignoreCaching);
	}

	@ReferTo(AbstractXmlValidator.class)
	public void setXmlSchemaVersion(String xmlSchemaVersion) {
		validator.setXmlSchemaVersion(xmlSchemaVersion);
	}

	/**
	 * The namespace of the SOAP envelope, when this property has a value and the input message is a SOAP message,
	 * the content of the SOAP Body is used for validation, hence the SOAP Envelope and SOAP Body elements are not considered part of the message to validate.
	 * Please note that this functionality is deprecated, using {@link SoapValidator} is now the preferred solution in case a SOAP
	 * message needs to be validated, in other cases give this property an empty value.
	 * @ff.default http://schemas.xmlsoap.org/soap/envelope/
	 */
	@Deprecated
	public void setSoapNamespace(String string) {
		soapNamespace = string;
	}
	@Deprecated
	public String getSoapNamespace() {
		return soapNamespace;
	}

	/** Key of session variable to store the name of the root element */
	public void setRootElementSessionKey(String rootElementSessionKey) {
		this.rootElementSessionKey = rootElementSessionKey;
	}

	/** Key of session variable to store the namespace of the root element */
	public void setRootNamespaceSessionKey(String rootNamespaceSessionKey) {
		this.rootNamespaceSessionKey = rootNamespaceSessionKey;
	}

}
