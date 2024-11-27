package org.frankframework.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IScopeProvider;
import org.frankframework.core.PipeLineSession;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.testutil.TestScopeProvider;
import org.frankframework.util.LogUtil;
import org.frankframework.validation.AbstractXmlValidator.ValidationResult;
import org.frankframework.validation.xsd.ResourceXsd;

/**
 * @author Gerrit van Brakel
 */
public abstract class ValidatorTestBase {
	protected Logger log = LogUtil.getLogger(this);

	public static final String MSG_INVALID_CONTENT="Failed"; // Travis does not see the 'Invalid content' message
	public static final String MSG_CANNOT_FIND_DECLARATION="Cannot find the declaration of element";
	public static final String MSG_UNKNOWN_NAMESPACE="Unknown namespace";
	public static final String MSG_SCHEMA_NOT_FOUND="Cannot find";
	public static final String MSG_IS_NOT_COMPLETE="is not complete";

	public static final String BASE_DIR_VALIDATION="/Validation";

	public static final String ROOT_NAMESPACE_GPBDB="http://schemas.xmlsoap.org/soap/envelope/";
	public static final String SCHEMA_LOCATION_SOAP_ENVELOPE ="http://schemas.xmlsoap.org/soap/envelope/ "+		BASE_DIR_VALIDATION+"/Tibco/xsd/soap/envelope.xsd";
	public static String SCHEMA_LOCATION_GPBDB_MESSAGE ="http://www.ing.com/CSP/XSD/General/Message_2 "+	BASE_DIR_VALIDATION+"/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/Message_2.xsd";
	public static String SCHEMA_LOCATION_GPBDB_AFDTYPES="http://ing.nn.afd/AFDTypes "+						BASE_DIR_VALIDATION+"/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/AFDTypes.xsd";
	public static String SCHEMA_LOCATION_GPBDB_REQUEST ="http://www.ing.com/bis/xsd/nl/banking/bankingcustomer_generate_01_getpartybasicdatabanking_request_01 "+	BASE_DIR_VALIDATION+"/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/bankingcustomer_generate_01_getpartybasicdatabanking_request_01.xsd";
	public static String SCHEMA_LOCATION_GPBDB_RESPONSE="http://www.ing.com/bis/xsd/nl/banking/bankingcustomer_generate_01_getpartybasicdatabanking_response_01 "+	BASE_DIR_VALIDATION+"/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/bankingcustomer_generate_01_getpartybasicdatabanking_response_01.xsd";
	public static String SCHEMA_LOCATION_GPBDB_GPBDB   ="http://www.ing.com/nl/banking/coe/xsd/bankingcustomer_generate_01/getpartybasicdatabanking_01 "+			BASE_DIR_VALIDATION+"/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/getpartybasicdatabanking_01.xsd";

	public String INPUT_FILE_GPBDB_NOBODY=BASE_DIR_VALIDATION+"/Tibco/in/noBody";
	public String INPUT_FILE_GPBDB_OK=BASE_DIR_VALIDATION+"/Tibco/in/step5";
	public String INPUT_FILE_GPBDB_ERR1=BASE_DIR_VALIDATION+"/Tibco/in/step5error_unknown_namespace";
	public String INPUT_FILE_GPBDB_ERR2=BASE_DIR_VALIDATION+"/Tibco/in/step5error_wrong_tag";


	public static final String ROOT_NAMESPACE_BASIC="http://www.ing.com/testxmlns";
	public static final String SCHEMA_LOCATION_BASIC_A_OK                    		 =ROOT_NAMESPACE_BASIC+" "			+BASE_DIR_VALIDATION+"/Basic/xsd/A_correct.xsd"	;
	public static final String SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE            =ROOT_NAMESPACE_BASIC+" "			+BASE_DIR_VALIDATION+"/Basic/xsd/A_without_targetnamespace.xsd";
	public static final String SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE_MISMATCH   =ROOT_NAMESPACE_BASIC+"_mismatch "	+BASE_DIR_VALIDATION+"/Basic/xsd/A_without_targetnamespace.xsd";

	public static final String INPUT_FILE_BASIC_A_OK				=BASE_DIR_VALIDATION+"/Basic/in/ok";
	public static final String INPUT_FILE_BASIC_A_OK_IN_ENVELOPE	=BASE_DIR_VALIDATION+"/Basic/in/ok-in-envelope";
	public static final String INPUT_FILE_BASIC_A_ERR				=BASE_DIR_VALIDATION+"/Basic/in/with_errors";
	public static final String INPUT_FILE_BASIC_A_ERR_IN_ENVELOPE	=BASE_DIR_VALIDATION+"/Basic/in/with_errors-in-envelope";
	public static final String INPUT_FILE_BASIC_A_ENTITY_EXPANSION	=BASE_DIR_VALIDATION+"/Basic/in/entityExpansion";
	public static final String INPUT_FILE_BASIC_PLAIN_TEXT			=BASE_DIR_VALIDATION+"/Basic/in/plainText";

	public static final String NO_NAMESPACE_SCHEMA        = BASE_DIR_VALIDATION+"/GetVehicleTypeDetails/XSD_GetVehicleTypeDetails_Request.xsd";
	public static final String NO_NAMESPACE_SOAP_FILE     = BASE_DIR_VALIDATION+"/GetVehicleTypeDetails/in";
	public static final String NO_NAMESPACE_SOAP_MSGROOT  = "GetVehicleTypeDetailsREQ";

	public static final String ELEMENT_FORM_DEFAULT_UNQUALIFIED_NAMESPACE="urn:ElementFormDefaultUnqualified";
	public static final String ELEMENT_FORM_DEFAULT_UNQUALIFIED_SCHEMA=BASE_DIR_VALIDATION+"/ElementFormDefaultUnqualified/ElementFormDefaultUnqualified.xsd";
	public static final String ELEMENT_FORM_DEFAULT_UNQUALIFIED_INPUT=BASE_DIR_VALIDATION+"/ElementFormDefaultUnqualified/input.xml";
	public static final String ELEMENT_FORM_DEFAULT_UNQUALIFIED_MSGROOT="root";

	public static final String SCHEMA_LOCATION_ARRAYS                            	="urn:arrays /Arrays/arrays.xsd";
	public static final String INPUT_FILE_SCHEMA_LOCATION_ARRAYS_COMPACT_JSON		="/Arrays/arrays-compact";
	public static final String INPUT_FILE_SCHEMA_LOCATION_ARRAYS_FULL_JSON			="/Arrays/arrays-full";

	private final IScopeProvider testScopeProvider = new TestScopeProvider();

	public void validate(String rootNamespace, String schemaLocation, String inputFile) throws Exception {
		validate(rootNamespace, schemaLocation, false, inputFile, null);
	}

	public void validate(String rootNamespace, String schemaLocation, String inputFile, String expectedFailureReason) throws Exception {
		validate(rootNamespace, schemaLocation, false, inputFile, expectedFailureReason);
	}

	public void validate(String rootNamespace, String schemaLocation, String inputFile, String[] expectedFailureReasons) throws Exception {
		validate(rootNamespace, schemaLocation, false, false, inputFile, expectedFailureReasons);
	}

	public void validate(String rootNamespace, String schemaLocation, boolean addNamespaceToSchema, String inputFile, String expectedFailureReason) throws Exception {
		String[] expected = { expectedFailureReason };
		if (expectedFailureReason == null) expected = null;
		validate(rootNamespace, schemaLocation, addNamespaceToSchema, false, inputFile, expected);
	}

	public void validateIgnoreUnknownNamespacesOn(String rootNamespace, String schemaLocation, String inputFile, String expectedFailureReason) throws Exception {
		String[] expected = { expectedFailureReason };
		if (expectedFailureReason == null) expected = null;
		validate(rootNamespace, schemaLocation, false, true, inputFile, expected);
	}

	public void validateIgnoreUnknownNamespacesOff(String rootNamespace, String schemaLocation, String inputFile, String expectedFailureReason) throws Exception {
		String[] expected = { expectedFailureReason };
		if (expectedFailureReason==null) expected=null;
		validate(rootNamespace, schemaLocation, false, false, inputFile, expected );
	}

	public abstract ValidationResult validate(String rootElement, String rootNamespace, String schemaLocation, boolean addNamespaceToSchema, boolean ignoreUnknownNamespaces, String inputFile, String[] expectedFailureReasons) throws Exception;

	public ValidationResult validate(String rootNamespace, String schemaLocation, boolean addNamespaceToSchema, boolean ignoreUnknownNamespaces, String inputFile, String[] expectedFailureReasons) throws Exception {
		return validate(null, rootNamespace, schemaLocation, addNamespaceToSchema, ignoreUnknownNamespaces, inputFile, expectedFailureReasons);
	}

	public void evaluateResult(ValidationResult result, PipeLineSession session, Exception e, String[] expectedFailureReasons) {
		String failureReason=(String)(session.get("failureReason"));
		if (failureReason != null) {
			log.info("no failure reason");
		} else {
			log.warn("failure reason [" + failureReason + "]");
		}
		if (e != null) {
			log.warn("exception (" + e.getClass().getName() + "): " + e.getMessage());
		}

		if (expectedFailureReasons == null) {
			// expected valid XML
			if (e != null) {
				log.debug(e);
				fail("expected XML to pass: "+e.getMessage());
			}
			if (result != ValidationResult.VALID) {
				fail("result must be 'valid XML' but was [" + result + "]");
			}
		} else {
			// expected invalid XML
			if (failureReason != null) {
				if (e == null) {
					assertEquals(ValidationResult.PARSER_ERROR, result);
				}
				checkFailureReasons(failureReason, "failure reason", expectedFailureReasons);
			} else {
				if (e != null) {
					checkFailureReasons(e.getMessage(), "exception message", expectedFailureReasons);
				} else {
					assertEquals(ValidationResult.PARSER_ERROR, result);
					checkFailureReasons("", "failure reason", expectedFailureReasons);
				}
			}
		}
	}

	public void checkFailureReasons(String errorMessage, String messagetype, String[] expectedFailureReasons) {
		StringBuilder msg = null;
		if (expectedFailureReasons == null || expectedFailureReasons.length == 0) {
			return;
		}
		if (errorMessage == null) {
			fail("errorMessage is null");
		}
		for (String expected : expectedFailureReasons) {
			if (errorMessage.toLowerCase().contains(expected.toLowerCase())) {
				return;
			}
			if (msg == null) {
				msg = new StringBuilder("expected [" + expected + "]");
			} else {
				msg.append(" or [").append(expected).append("]");
			}
		}
		msg.append(" in ").append(messagetype).append(" [").append(errorMessage).append("]");
		fail(msg.toString());
	}

	protected String getTestXml(String testxml) throws IOException {
		return TestFileUtils.getTestFile(testxml);
	}

	public SchemasProvider getSchemasProvider(final String schemaLocation, final boolean addNamespaceToSchema) {
		return new SchemasProvider() {

			public Set<IXSD> getXsds() throws ConfigurationException {
				Set<IXSD> xsds = new LinkedHashSet<>();
				String[] split =  schemaLocation.trim().split("\\s+");
				if (split.length % 2 != 0) throw new ConfigurationException("The schema must exist from an even number of strings, but it is " + schemaLocation);
				for (int i = 0; i < split.length; i += 2) {
					AbstractXSD xsd = new ResourceXsd();
					xsd.setAddNamespaceToSchema(addNamespaceToSchema);
					xsd.initNamespace(split[i], testScopeProvider, split[i + 1]);
					xsds.add(xsd);
				}
				return xsds;
			}

			@Override
			public List<Schema> getSchemas() throws ConfigurationException {
				Set<IXSD> xsds = getXsds();
				xsds = AbstractXSD.getXsdsRecursive(xsds);
				//checkRootValidations(xsds);
				try {
					Map<String, Set<IXSD>> xsdsGroupedByNamespace = SchemaUtils.groupXsdsByNamespace(xsds, false);
					xsds = SchemaUtils.mergeXsdsGroupedByNamespaceToSchemasWithoutIncludes(testScopeProvider, xsdsGroupedByNamespace);
				} catch(Exception e) {
					throw new ConfigurationException("could not merge schema's", e);
				}
				List<Schema> schemas = new ArrayList<>();
				SchemaUtils.sortByDependencies(xsds, schemas);
				return schemas;
			}

			@Override
			public String getSchemasId() {
				return schemaLocation;
			}

			@Override
			public String getSchemasId(PipeLineSession session) {
				return null;
			}

			@Override
			public List<Schema> getSchemas(PipeLineSession session) {
				return null;
			}

		};
	}

}
