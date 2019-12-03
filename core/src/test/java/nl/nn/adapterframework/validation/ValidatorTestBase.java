package nl.nn.adapterframework.validation;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.pipes.XmlValidator;
import nl.nn.adapterframework.testutil.TestFileUtils;

/**
 * @author Gerrit van Brakel
 */
public abstract class ValidatorTestBase extends TestCase {

	public String MSG_INVALID_CONTENT="Failed"; // Travis does not see the 'Invalid content' message 
	public String MSG_CANNOT_FIND_DECLARATION="Cannot find the declaration of element";
	public String MSG_UNKNOWN_NAMESPACE="Unknown namespace";
	public String MSG_SCHEMA_NOT_FOUND="Cannot find";
	public String MSG_CANNOT_RESOLVE="Cannot resolve the name";
	public String MSG_IS_NOT_COMPLETE="is not complete";

	public static String BASE_DIR_VALIDATION="/Validation";

	public String ROOT_NAMESPACE_GPBDB="http://schemas.xmlsoap.org/soap/envelope/";
	public String SCHEMA_LOCATION_SOAP_ENVELOPE ="http://schemas.xmlsoap.org/soap/envelope/ "+		BASE_DIR_VALIDATION+"/Tibco/xsd/soap/envelope.xsd";
	public static String SCHEMA_LOCATION_GPBDB_MESSAGE ="http://www.ing.com/CSP/XSD/General/Message_2 "+	BASE_DIR_VALIDATION+"/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/Message_2.xsd";
	public static String SCHEMA_LOCATION_GPBDB_AFDTYPES="http://ing.nn.afd/AFDTypes "+						BASE_DIR_VALIDATION+"/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/AFDTypes.xsd";
	public static String SCHEMA_LOCATION_GPBDB_REQUEST ="http://www.ing.com/bis/xsd/nl/banking/bankingcustomer_generate_01_getpartybasicdatabanking_request_01 "+	BASE_DIR_VALIDATION+"/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/bankingcustomer_generate_01_getpartybasicdatabanking_request_01.xsd";
	public static String SCHEMA_LOCATION_GPBDB_RESPONSE="http://www.ing.com/bis/xsd/nl/banking/bankingcustomer_generate_01_getpartybasicdatabanking_response_01 "+	BASE_DIR_VALIDATION+"/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/bankingcustomer_generate_01_getpartybasicdatabanking_response_01.xsd";
	public static String SCHEMA_LOCATION_GPBDB_GPBDB   ="http://www.ing.com/nl/banking/coe/xsd/bankingcustomer_generate_01/getpartybasicdatabanking_01 "+			BASE_DIR_VALIDATION+"/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/getpartybasicdatabanking_01.xsd";
	
	public String INPUT_FILE_GPBDB_NOBODY=BASE_DIR_VALIDATION+"/Tibco/in/noBody";
	public String INPUT_FILE_GPBDB_OK=BASE_DIR_VALIDATION+"/Tibco/in/step5";
	public String INPUT_FILE_GPBDB_ERR1=BASE_DIR_VALIDATION+"/Tibco/in/step5error_unknown_namespace";
	public String INPUT_FILE_GPBDB_ERR2=BASE_DIR_VALIDATION+"/Tibco/in/step5error_wrong_tag";
	
	
	public String ROOT_NAMESPACE_BASIC="http://www.ing.com/testxmlns";
	public String SCHEMA_LOCATION_BASIC_A_OK                            =ROOT_NAMESPACE_BASIC+" "			+BASE_DIR_VALIDATION+"/Basic/xsd/A_correct.xsd"	;
	public String SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE            =ROOT_NAMESPACE_BASIC+" "			+BASE_DIR_VALIDATION+"/Basic/xsd/A_without_targetnamespace.xsd";
	public String SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE_MISMATCH   =ROOT_NAMESPACE_BASIC+"_mismatch "	+BASE_DIR_VALIDATION+"/Basic/xsd/A_without_targetnamespace.xsd";
	
	public String INPUT_FILE_BASIC_A_OK					=BASE_DIR_VALIDATION+"/Basic/in/ok";
	public String INPUT_FILE_BASIC_A_OK_IN_ENVELOPE		=BASE_DIR_VALIDATION+"/Basic/in/ok-in-envelope";
	public String INPUT_FILE_BASIC_A_ERR				=BASE_DIR_VALIDATION+"/Basic/in/with_errors";
	public String INPUT_FILE_BASIC_A_ERR_IN_ENVELOPE	=BASE_DIR_VALIDATION+"/Basic/in/with_errors-in-envelope";
	public String INPUT_FILE_BASIC_A_ENTITY_EXPANSION	=BASE_DIR_VALIDATION+"/Basic/in/entityExpansion";
	public String INPUT_FILE_BASIC_PLAIN_TEXT			=BASE_DIR_VALIDATION+"/Basic/in/plainText";

	public String NO_NAMESPACE_SCHEMA        = BASE_DIR_VALIDATION+"/GetVehicleTypeDetails/XSD_GetVehicleTypeDetails_Request.xsd";
	public String NO_NAMESPACE_SOAP_FILE     = BASE_DIR_VALIDATION+"/GetVehicleTypeDetails/in";
	public String NO_NAMESPACE_SOAP_MSGROOT  = "GetVehicleTypeDetailsREQ";
	
	
	public String SCHEMA_LOCATION_ARRAYS                            	="urn:arrays /Arrays/arrays.xsd";
	public String INPUT_FILE_SCHEMA_LOCATION_ARRAYS_COMPACT_JSON		="/Arrays/arrays-compact";
	public String INPUT_FILE_SCHEMA_LOCATION_ARRAYS_FULL_JSON			="/Arrays/arrays-full";
	
	private ClassLoader testClassLoader = this.getClass().getClassLoader();

    public void validate(String rootNamespace, String schemaLocation, String inputFile) throws Exception {
    	validate(rootNamespace,schemaLocation, false, inputFile, null);
    }
    public void validate(String rootNamespace, String schemaLocation, String inputFile, String expectedFailureReason) throws Exception {
    	validate(rootNamespace,schemaLocation, false, inputFile, expectedFailureReason);
    }
    public void validate(String rootNamespace, String schemaLocation, String inputFile, String[] expectedFailureReasons) throws Exception {
    	validate(rootNamespace, schemaLocation, false, false, inputFile, expectedFailureReasons);
    }

    protected void validation(String rootElement, String rootNamespace, String schemaLocation, String inputfile, boolean addNamespaceToSchema, String expectedFailureReason) throws IllegalAccessException, InstantiationException, XmlValidatorException, IOException, PipeRunException, ConfigurationException {
    	String expected[]={ expectedFailureReason };
    	if (expectedFailureReason==null) expected=null;
    	validate(rootElement, rootNamespace,schemaLocation,addNamespaceToSchema,false,inputfile, expected);
    }

    protected void validation(String rootNamespace, String schemaLocation, String inputfile, boolean addNamespaceToSchema, String expectedFailureReason) throws IllegalAccessException, InstantiationException, XmlValidatorException, IOException, PipeRunException, ConfigurationException {
    	validation(null,rootNamespace,schemaLocation,inputfile,addNamespaceToSchema, expectedFailureReason);
    }

    public void validate(String rootNamespace, String schemaLocation, boolean addNamespaceToSchema, String inputFile, String expectedFailureReason) throws Exception {
    	String expected[]={ expectedFailureReason };
    	if (expectedFailureReason==null) expected=null;
    	validate(rootNamespace, schemaLocation, addNamespaceToSchema, false, inputFile, expected);
    }
	public void validateIgnoreUnknownNamespacesOn(String rootNamespace, String schemaLocation, String inputFile, String expectedFailureReason) throws Exception {
    	String expected[]={ expectedFailureReason };
    	if (expectedFailureReason==null) expected=null;
    	validate(rootNamespace, schemaLocation, false, true, inputFile, expected);
	}
    public void validateIgnoreUnknownNamespacesOff(String rootNamespace, String schemaLocation, String inputFile, String expectedFailureReason) throws Exception {
    	String expected[]={ expectedFailureReason };
    	if (expectedFailureReason==null) expected=null;
    	validate(rootNamespace, schemaLocation, false, false, inputFile, expected );
    }

    public abstract String validate(String rootElement, String rootNamespace, String schemaLocation, boolean addNamespaceToSchema, boolean ignoreUnknownNamespaces, String inputFile, String[] expectedFailureReasons) throws ConfigurationException, InstantiationException, IllegalAccessException, XmlValidatorException, PipeRunException, IOException;
    public String validate(String rootNamespace, String schemaLocation, boolean addNamespaceToSchema, boolean ignoreUnknownNamespaces, String inputFile, String[] expectedFailureReasons) throws ConfigurationException, InstantiationException, IllegalAccessException, XmlValidatorException, PipeRunException, IOException {
    	return validate(null, rootNamespace, schemaLocation, addNamespaceToSchema, ignoreUnknownNamespaces, inputFile, expectedFailureReasons);
    }

    public void evaluateResult(String event, IPipeLineSession session, Exception e, String[] expectedFailureReasons) {
        String failureReason=(String)(session.get("failureReason"));
        if (failureReason!=null) {
        	System.out.println("no failure reason");
        } else {
        	System.out.println("failure reason ["+failureReason+"]");
        }
        if (e!=null) {
        	System.out.println("exception ("+e.getClass().getName()+"): "+e.getMessage());
        }

    	if (expectedFailureReasons==null) {
    		// expected valid XML
    		if (e!=null) {
    			e.printStackTrace();
    			fail("expected XML to pass");
    		}
    		if (!event.equals("valid XML") && !event.equals("success")) {
    			fail("result must be 'valid XML' or 'success' but was ["+event+"]");
    		}
    	} else {
    		// expected invalid XML
    		if (failureReason!=null) {
    			if (e==null) {
    				assertEquals("Invalid XML", event);
    			}
    			checkFailureReasons(failureReason, "failure reason", expectedFailureReasons);
    		} else {
    			if (e!=null) {
        			checkFailureReasons(e.getMessage(), "exception message", expectedFailureReasons);
    			} else {
    	       		assertEquals("Invalid XML", event);
        			checkFailureReasons("", "failure reason", expectedFailureReasons);
    			}
	    	}
    	}
    }
 
    public void checkFailureReasons(String errorMessage, String messagetype, String[] expectedFailureReasons) {
    	String msg=null;
    	if (expectedFailureReasons==null || expectedFailureReasons.length==0) {
    		return;
    	}
    	if (errorMessage==null) {
    		fail("errorMessage is null");
    	}
    	for (String expected:expectedFailureReasons) {
    		if (errorMessage.toLowerCase().contains(expected.toLowerCase())) {
    			return;
    		}
    		if (msg==null) {
    			msg="expected ["+expected+"]";
    		} else {
    			msg+=" or ["+expected+"]";
    		}
    	}
    	msg+=" in "+messagetype+" ["+errorMessage+"]";
    	fail(msg);
    }
    
    protected String getTestXml(String testxml) throws IOException {
    	return TestFileUtils.getTestFile(testxml);
    }
       
	public SchemasProvider getSchemasProvider(final String schemaLocation, final boolean addNamespaceToSchema) {
		return new SchemasProvider() {
			
			public Set<XSD> getXsds() throws ConfigurationException {
				Set<XSD> xsds = new HashSet<XSD>();
//				if (StringUtils.isNotEmpty(getNoNamespaceSchemaLocation())) {
//					XSD xsd = new XSD();
//					xsd.setClassLoader(classLoader);
//					xsd.setNoNamespaceSchemaLocation(getNoNamespaceSchemaLocation());
//					xsd.setResource(getNoNamespaceSchemaLocation());
//					xsd.init();
//					xsds.add(xsd);
//				} else {
					String[] split =  schemaLocation.trim().split("\\s+");
					if (split.length % 2 != 0) throw new ConfigurationException("The schema must exist from an even number of strings, but it is " + schemaLocation);
					for (int i = 0; i < split.length; i += 2) {
						XSD xsd = new XSD();
						xsd.setAddNamespaceToSchema(addNamespaceToSchema);
//						xsd.setImportedSchemaLocationsToIgnore(getImportedSchemaLocationsToIgnore());
//						xsd.setUseBaseImportedSchemaLocationsToIgnore(isUseBaseImportedSchemaLocationsToIgnore());
//						xsd.setImportedNamespacesToIgnore(getImportedNamespacesToIgnore());
						xsd.initNamespace(split[i], testClassLoader, split[i + 1]);
						xsds.add(xsd);
					}
//				}
				return xsds;
			}

			@Override
			public List<Schema> getSchemas() throws ConfigurationException {
				Set<XSD> xsds = getXsds();
				xsds = SchemaUtils.getXsdsRecursive(xsds);
				//checkRootValidations(xsds);
				try {
					Map<String, Set<XSD>> xsdsGroupedByNamespace =
							SchemaUtils.getXsdsGroupedByNamespace(xsds, false);
					xsds = SchemaUtils.mergeXsdsGroupedByNamespaceToSchemasWithoutIncludes(
							this.getClass().getClassLoader(), xsdsGroupedByNamespace, null);
				} catch(Exception e) {
					throw new ConfigurationException("could not merge schema's", e);
				}
				List<Schema> schemas = new ArrayList<Schema>();
				SchemaUtils.sortByDependencies(xsds, schemas);
				return schemas;
			}

			@Override
			public String getSchemasId() throws ConfigurationException {
				return schemaLocation;
			}

			@Override
			public String getSchemasId(IPipeLineSession session) throws PipeRunException {
				return null;
			}

			@Override
			public List<Schema> getSchemas(IPipeLineSession session) throws PipeRunException {
				return null;
			}

		};
	}

}
