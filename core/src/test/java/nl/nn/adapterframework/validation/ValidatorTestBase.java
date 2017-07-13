package nl.nn.adapterframework.validation;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.pipes.XmlValidator;

/**
 * @author Gerrit van Brakel
 */
public abstract class ValidatorTestBase extends TestCase {

	public String MSG_INVALID_CONTENT="Invalid content";
	public String MSG_CANNOT_FIND_DECLARATION="Cannot find the declaration of element";
	public String MSG_UNKNOWN_NAMESPACE="Unknown namespace";
	public String MSG_SCHEMA_NOT_FOUND="Cannot find";
	public String MSG_CANNOT_RESOLVE="Cannot resolve the name";
	public String MSG_IS_NOT_COMPLETE="is not complete";
	
	public String ROOT_NAMESPACE_GPBDB="http://schemas.xmlsoap.org/soap/envelope/";
	public String SCHEMA_LOCATION_SOAP_ENVELOPE ="http://schemas.xmlsoap.org/soap/envelope/ /Tibco/xsd/soap/envelope.xsd";
	public String SCHEMA_LOCATION_GPBDB_MESSAGE ="http://www.ing.com/CSP/XSD/General/Message_2 /Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/Message_2.xsd";
	public String SCHEMA_LOCATION_GPBDB_AFDTYPES="http://ing.nn.afd/AFDTypes /Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/AFDTypes.xsd";
	public String SCHEMA_LOCATION_GPBDB_REQUEST ="http://www.ing.com/bis/xsd/nl/banking/bankingcustomer_generate_01_getpartybasicdatabanking_request_01 /Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/bankingcustomer_generate_01_getpartybasicdatabanking_request_01.xsd";
	public String SCHEMA_LOCATION_GPBDB_RESPONSE="http://www.ing.com/bis/xsd/nl/banking/bankingcustomer_generate_01_getpartybasicdatabanking_response_01 /Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/bankingcustomer_generate_01_getpartybasicdatabanking_response_01.xsd";
	public String SCHEMA_LOCATION_GPBDB_GPBDB   ="http://www.ing.com/nl/banking/coe/xsd/bankingcustomer_generate_01/getpartybasicdatabanking_01 /Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/getpartybasicdatabanking_01.xsd";
	
	public String INPUT_FILE_GPBDB_NOBODY="/Tibco/in/noBody";
	public String INPUT_FILE_GPBDB_OK="/Tibco/in/step5";
	public String INPUT_FILE_GPBDB_ERR1="/Tibco/in/step5error_unknown_namespace";
	public String INPUT_FILE_GPBDB_ERR2="/Tibco/in/step5error_wrong_tag";
	
	public String ROOT_NAMESPACE_BASIC="http://www.ing.com/testxmlns";
	public String SCHEMA_LOCATION_BASIC_A_OK                            ="http://www.ing.com/testxmlns /Basic/xsd/A_correct.xsd"	;
	public String SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE            ="http://www.ing.com/testxmlns /Basic/xsd/A_without_targetnamespace.xsd";
	public String SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE_MISMATCH   ="http://www.ing.com/testxmlns_mismatch /Basic/xsd/A_without_targetnamespace.xsd";

	public String INPUT_FILE_BASIC_A_OK="/Basic/in/ok";
	public String INPUT_FILE_BASIC_A_ERR="/Basic/in/with_errors";

    public void validate(String rootNamespace, String schemaLocation, String inputFile, String expectedFailureReason) throws Exception {
    	validate(rootNamespace,schemaLocation, false, inputFile, expectedFailureReason);
    }

    protected void validation(String rootNamespace, String schemaLocation, String inputfile, boolean addNamespaceToSchema, String expectedFailureReason) throws IllegalAccessException, InstantiationException, XmlValidatorException, IOException, PipeRunException, ConfigurationException {
    	validate(rootNamespace,schemaLocation,addNamespaceToSchema,false,inputfile, expectedFailureReason);
    }

    public void validate(String rootNamespace, String schemaLocation, boolean addNamespaceToSchema, String inputFile, String expectedFailureReason) throws Exception {
    	validate(rootNamespace, schemaLocation, addNamespaceToSchema, false, inputFile, expectedFailureReason);
    }
	public void validateIgnoreUnknownNamespacesOn(String rootNamespace, String schemaLocation, String inputFile, String expectedFailureReason) throws Exception {
    	validate(rootNamespace, schemaLocation, false, true, inputFile, expectedFailureReason);
	}
    public void validateIgnoreUnknownNamespacesOff(String rootNamespace, String schemaLocation, String inputFile, String expectedFailureReason) throws Exception {
    	validate(rootNamespace, schemaLocation, false, false, inputFile, expectedFailureReason);
    }

    public abstract String validate(String rootNamespace, String schemaLocation, boolean addNamespaceToSchema, boolean ignoreUnknownNamespaces, String inputFile, String expectedFailureReason) throws ConfigurationException, InstantiationException, IllegalAccessException, XmlValidatorException, PipeRunException, IOException;

    public void evaluateResult(String event, IPipeLineSession session, Exception e, String expectedFailureReason) {
        String failureReason=(String)(session.get("failureReason"));
        System.out.println("failure reason ["+failureReason+"]");
        if (e!=null) {
        	System.out.println("exception ("+e.getClass().getName()+"): "+e.getMessage());
        }

    	if (expectedFailureReason==null) {
    		// expected valid XML
    		if (e!=null) {
    			e.printStackTrace();
    			fail("expected XML to pass");
    		}
    		if (!event.equals("valid XML") && !event.equals("success")) {
    			fail("result must be 'valid XML' or 'success'");
    		}
    	} else {
    		// expected invalid XML
    		if (failureReason!=null) {
    			if (e==null) {
    				assertEquals("Invalid XML", event);
    			}
	    		if (failureReason.indexOf(expectedFailureReason)<0) {
	    			fail("expected ["+expectedFailureReason+"] in failure reason ["+failureReason+"]");
	    		} 
    		} else {
    			if (e!=null) {
		    		if (e.getMessage().indexOf(expectedFailureReason)<0) {
		    			e.printStackTrace();
		    			fail("expected ["+expectedFailureReason+"] in exception message ["+e.getMessage()+"]");
		    		} 
    			} else {
    	       		assertEquals("Invalid XML", event);
    	       		assertEquals("expected failure reason", "", expectedFailureReason);
    			}
	    	}
    	}
    }
 
    protected String getTestXml(String testxml) throws IOException {
        BufferedReader buf = new BufferedReader(new InputStreamReader(XmlValidator.class.getResourceAsStream(testxml)));
        StringBuilder string = new StringBuilder();
        String line = buf.readLine();
        while (line != null) {
            string.append(line);
            line = buf.readLine();
        }
        return string.toString();

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
						xsd.setClassLoader(this.getClass().getClassLoader());
						xsd.setNamespace(split[i]);
						xsd.setResource(split[i + 1]);
						xsd.setAddNamespaceToSchema(addNamespaceToSchema);
//						xsd.setImportedSchemaLocationsToIgnore(getImportedSchemaLocationsToIgnore());
//						xsd.setUseBaseImportedSchemaLocationsToIgnore(isUseBaseImportedSchemaLocationsToIgnore());
//						xsd.setImportedNamespacesToIgnore(getImportedNamespacesToIgnore());
						xsd.init();
						xsds.add(xsd);
					}
//				}
				return xsds;
			}

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
				throw new PipeRunException(null,"getSchemasId not implemented");
			}

			@Override
			public List<Schema> getSchemas(IPipeLineSession session) throws PipeRunException {
				throw new PipeRunException(null,"getSchemas not implemented");
			}

		};
	}

}
