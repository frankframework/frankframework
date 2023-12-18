package org.frankframework.validation;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.fail;

import org.apache.xerces.xni.XNIException;
import org.junit.Ignore;
import org.junit.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeStartException;

/**
 * @author Gerrit van Brakel / Michiel Meeuwissen
 */
public abstract class XmlValidatorTestBase extends ValidatorTestBase {

	@Test
	public void straighforward() throws Exception {
		validate(ROOT_NAMESPACE_BASIC, SCHEMA_LOCATION_BASIC_A_OK, false, INPUT_FILE_BASIC_A_OK, null);
		validate(ROOT_NAMESPACE_BASIC, SCHEMA_LOCATION_BASIC_A_OK, false, INPUT_FILE_BASIC_A_ERR, MSG_INVALID_CONTENT);
		validate(ROOT_NAMESPACE_BASIC, SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE, false, INPUT_FILE_BASIC_A_OK, MSG_CANNOT_FIND_DECLARATION);
		validate(ROOT_NAMESPACE_BASIC, SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE, false, INPUT_FILE_BASIC_A_ERR, MSG_CANNOT_FIND_DECLARATION);
	}

//    @Test
//    public void straighforwardInEnvelope() throws IllegalAccessException, InstantiationException, XmlValidatorException, IOException, PipeRunException, ConfigurationException {
//    	validation("A",ROOT_NAMESPACE_BASIC,SCHEMA_LOCATION_BASIC_A_OK,INPUT_FILE_BASIC_A_OK_IN_ENVELOPE,false,null);
//    	validation("A",ROOT_NAMESPACE_BASIC,SCHEMA_LOCATION_BASIC_A_OK,INPUT_FILE_BASIC_A_ERR_IN_ENVELOPE,false,MSG_INVALID_CONTENT);
//    	validation("A",ROOT_NAMESPACE_BASIC,SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE,INPUT_FILE_BASIC_A_OK_IN_ENVELOPE,false,MSG_CANNOT_FIND_DECLARATION);
//    	validation("A",ROOT_NAMESPACE_BASIC,SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE,INPUT_FILE_BASIC_A_ERR_IN_ENVELOPE,false,MSG_CANNOT_FIND_DECLARATION);
//    }

	@Test
	public void addTargetNamespaceNoop() throws Exception {
		validate(ROOT_NAMESPACE_BASIC, SCHEMA_LOCATION_BASIC_A_OK, true, INPUT_FILE_BASIC_A_OK, null);
	}
	public void addTargetNamespaceNoopWithErrors() throws Exception {
		validate(ROOT_NAMESPACE_BASIC, SCHEMA_LOCATION_BASIC_A_OK, true, INPUT_FILE_BASIC_A_ERR, MSG_INVALID_CONTENT);
	}


	@Test
	public void addNamespaceToSchema() throws Exception {
		validate(ROOT_NAMESPACE_BASIC, SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE, true, INPUT_FILE_BASIC_A_OK, null);
	}

	@Test
	public void addNamespaceToSchemaWithErrors() throws Exception {
		validate(ROOT_NAMESPACE_BASIC, SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE, true, INPUT_FILE_BASIC_A_ERR, MSG_INVALID_CONTENT);
	}

	@Test
	public void addNamespaceToSchemaNamespaceMismatch() throws Exception {
		validate(ROOT_NAMESPACE_BASIC, SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE_MISMATCH, true, INPUT_FILE_BASIC_A_OK, MSG_CANNOT_FIND_DECLARATION);
	}

	@Test
	public void missingMandatoryElement() throws Exception {
		validate(ROOT_NAMESPACE_GPBDB, SCHEMA_LOCATION_SOAP_ENVELOPE, INPUT_FILE_GPBDB_NOBODY, MSG_IS_NOT_COMPLETE);
	}

	public String getExpectedErrorForPlainText() {
		return "Content is not allowed in prolog";
	}

	@Test
	public void validatePlainText() throws Exception {
		validate(ROOT_NAMESPACE_BASIC, SCHEMA_LOCATION_BASIC_A_OK, INPUT_FILE_BASIC_PLAIN_TEXT, getExpectedErrorForPlainText());
	}

    @Test
    public void step5() throws Exception {
        validate(ROOT_NAMESPACE_GPBDB,
        		SCHEMA_LOCATION_SOAP_ENVELOPE+" "+
        		SCHEMA_LOCATION_GPBDB_MESSAGE+" "+
        		SCHEMA_LOCATION_GPBDB_AFDTYPES+" "+
				SCHEMA_LOCATION_GPBDB_REQUEST+" "+
				SCHEMA_LOCATION_GPBDB_RESPONSE+" "+
				SCHEMA_LOCATION_GPBDB_GPBDB,
				INPUT_FILE_GPBDB_OK);
    }

    @Test
    @Ignore("Fails for XmlValidatorBaseXerces26 Hard to fix....")
    public void step5MissingNamespace() {
		try {
			validate(ROOT_NAMESPACE_GPBDB,
					SCHEMA_LOCATION_SOAP_ENVELOPE+" "+
					SCHEMA_LOCATION_GPBDB_MESSAGE+" "+
					SCHEMA_LOCATION_GPBDB_REQUEST+" "+
					SCHEMA_LOCATION_GPBDB_GPBDB,
					INPUT_FILE_GPBDB_OK,"[http://www.ing.com/nl/banking/coe/xsd/bankingcustomer_generate_01/getpartybasicdatabanking_01]");
			fail("Validation succeeded but an exception was expected");
		} catch (PipeStartException | XNIException e) {
			// Success.
			// Which exception exactly was caught depends on parser used.
			log.info("Expected exception thrown from validation method: ", e);
		} catch (Exception e) {
			log.error("Unexpected exception from validation, expected one of PipeStartException | XNIException depending on parser. Got:", e);
			fail("Unexpected exception thrown by method: " + e.getMessage() + "; expected one of PipeStartException | XNIException depending on parser");
		}
	}

    @Test
    public void step5WrongOrderOfSchemaLocations() throws Exception {
        validate(ROOT_NAMESPACE_GPBDB,
        		SCHEMA_LOCATION_SOAP_ENVELOPE+" "+
                SCHEMA_LOCATION_GPBDB_MESSAGE+" "+
				SCHEMA_LOCATION_GPBDB_GPBDB+" "+
				SCHEMA_LOCATION_GPBDB_REQUEST+" "+
        		SCHEMA_LOCATION_GPBDB_AFDTYPES+" "+
				SCHEMA_LOCATION_GPBDB_RESPONSE,
				INPUT_FILE_GPBDB_OK);
    }

    @Test
    public void unresolvableSchema() throws Exception {
    	try {
    		validate(ROOT_NAMESPACE_GPBDB, "http://www.ing.com/BESTAATNIET /Bestaatniet.xsd ",null,MSG_SCHEMA_NOT_FOUND);
    	} catch (ConfigurationException e) {
    		assertThat(e.getMessage(),containsString(MSG_SCHEMA_NOT_FOUND));
    	}
    }

    @Test // step4errorr1.xml uses the namespace xmlns="http://www.ing.com/BESTAATNIET
    public void step5ValidationErrorUnknownNamespace() throws Exception {
        validateIgnoreUnknownNamespacesOff(ROOT_NAMESPACE_GPBDB,
        		SCHEMA_LOCATION_SOAP_ENVELOPE+" "+
                SCHEMA_LOCATION_GPBDB_MESSAGE+" "+
				SCHEMA_LOCATION_GPBDB_REQUEST+" "+
				SCHEMA_LOCATION_GPBDB_RESPONSE+" "+
				SCHEMA_LOCATION_GPBDB_GPBDB,
				INPUT_FILE_GPBDB_ERR1,MSG_UNKNOWN_NAMESPACE);
    }

    @Test
    public void validationUnknownNamespaceSwitchedOff() throws Exception {
    	validateIgnoreUnknownNamespacesOff(ROOT_NAMESPACE_GPBDB,
        		SCHEMA_LOCATION_SOAP_ENVELOPE, // every other namespace is thus unknown
				INPUT_FILE_GPBDB_ERR1,MSG_UNKNOWN_NAMESPACE);
    }

    @Test
    public void validationUnknownNamespaceSwitchedOn() throws Exception {
    	validateIgnoreUnknownNamespacesOn(ROOT_NAMESPACE_GPBDB,
    			SCHEMA_LOCATION_SOAP_ENVELOPE, // every other namespace is thus unknown
				INPUT_FILE_GPBDB_ERR1,null);
    }

    @Test
    public void step5ValidationErrorUnknownTag() throws Exception {
    	String expectedFailureReasons[]={MSG_CANNOT_FIND_DECLARATION,MSG_INVALID_CONTENT};
        validate(ROOT_NAMESPACE_GPBDB,
        		SCHEMA_LOCATION_SOAP_ENVELOPE+" "+
           		SCHEMA_LOCATION_GPBDB_MESSAGE+" "+
				SCHEMA_LOCATION_GPBDB_GPBDB+" "+
				SCHEMA_LOCATION_GPBDB_REQUEST+" "+
				SCHEMA_LOCATION_GPBDB_RESPONSE,
				INPUT_FILE_GPBDB_ERR2,expectedFailureReasons);
    }

    @Test
    public void step5ValidationUnknownNamespaces() throws Exception {
        validate(ROOT_NAMESPACE_GPBDB,SCHEMA_LOCATION_SOAP_ENVELOPE, INPUT_FILE_GPBDB_OK,MSG_UNKNOWN_NAMESPACE);
    }



}
