package org.frankframework.validation;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.xerces.xni.XNIException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeStartException;

/**
 * @author Gerrit van Brakel / Michiel Meeuwissen
 */
public abstract class XmlValidatorTestBase extends ValidatorTestBase {

	protected Class<? extends AbstractXmlValidator> implementation;

	@ParameterizedTest
	@MethodSource("data")
	public void straightforward(Class<? extends AbstractXmlValidator> implementation) throws Exception {
		this.implementation = implementation;
		validate(ROOT_NAMESPACE_BASIC, SCHEMA_LOCATION_BASIC_A_OK, false, INPUT_FILE_BASIC_A_OK, null);
		validate(ROOT_NAMESPACE_BASIC, SCHEMA_LOCATION_BASIC_A_OK, false, INPUT_FILE_BASIC_A_ERR, MSG_INVALID_CONTENT);
		validate(ROOT_NAMESPACE_BASIC, SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE, false, INPUT_FILE_BASIC_A_OK, MSG_CANNOT_FIND_DECLARATION);
		validate(ROOT_NAMESPACE_BASIC, SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE, false, INPUT_FILE_BASIC_A_ERR, MSG_CANNOT_FIND_DECLARATION);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void addTargetNamespaceNoop(Class<? extends AbstractXmlValidator> implementation) throws Exception {
		this.implementation = implementation;
		validate(ROOT_NAMESPACE_BASIC, SCHEMA_LOCATION_BASIC_A_OK, true, INPUT_FILE_BASIC_A_OK, null);
	}
	public void addTargetNamespaceNoopWithErrors() throws Exception {
		validate(ROOT_NAMESPACE_BASIC, SCHEMA_LOCATION_BASIC_A_OK, true, INPUT_FILE_BASIC_A_ERR, MSG_INVALID_CONTENT);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void addNamespaceToSchema(Class<? extends AbstractXmlValidator> implementation) throws Exception {
		this.implementation = implementation;
		validate(ROOT_NAMESPACE_BASIC, SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE, true, INPUT_FILE_BASIC_A_OK, null);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void addNamespaceToSchemaWithErrors(Class<? extends AbstractXmlValidator> implementation) throws Exception {
		this.implementation = implementation;
		validate(ROOT_NAMESPACE_BASIC, SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE, true, INPUT_FILE_BASIC_A_ERR, MSG_INVALID_CONTENT);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void addNamespaceToSchemaNamespaceMismatch(Class<? extends AbstractXmlValidator> implementation) throws Exception {
		this.implementation = implementation;
		validate(ROOT_NAMESPACE_BASIC, SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE_MISMATCH, true, INPUT_FILE_BASIC_A_OK, MSG_CANNOT_FIND_DECLARATION);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void missingMandatoryElement(Class<? extends AbstractXmlValidator> implementation) throws Exception {
		this.implementation = implementation;
		validate(ROOT_NAMESPACE_GPBDB, SCHEMA_LOCATION_SOAP_ENVELOPE, INPUT_FILE_GPBDB_NOBODY, MSG_IS_NOT_COMPLETE);
	}

	public String getExpectedErrorForPlainText() {
		return "Content is not allowed in prolog";
	}

	@ParameterizedTest
	@MethodSource("data")
	public void validatePlainText(Class<? extends AbstractXmlValidator> implementation) throws Exception {
		this.implementation = implementation;
		validate(ROOT_NAMESPACE_BASIC, SCHEMA_LOCATION_BASIC_A_OK, INPUT_FILE_BASIC_PLAIN_TEXT, getExpectedErrorForPlainText());
	}

	@ParameterizedTest
	@MethodSource("data")
	public void step5(Class<? extends AbstractXmlValidator> implementation) throws Exception {
		this.implementation = implementation;
        validate(ROOT_NAMESPACE_GPBDB,
        		SCHEMA_LOCATION_SOAP_ENVELOPE+" "+
        		SCHEMA_LOCATION_GPBDB_MESSAGE+" "+
        		SCHEMA_LOCATION_GPBDB_AFDTYPES+" "+
				SCHEMA_LOCATION_GPBDB_REQUEST+" "+
				SCHEMA_LOCATION_GPBDB_RESPONSE+" "+
				SCHEMA_LOCATION_GPBDB_GPBDB,
				INPUT_FILE_GPBDB_OK);
    }

	@ParameterizedTest
	@MethodSource("data")
    @Disabled("Fails for XmlValidatorBaseXerces26 Hard to fix....")
    public void step5MissingNamespace(Class<? extends AbstractXmlValidator> implementation) {
		this.implementation = implementation;
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

	@ParameterizedTest
	@MethodSource("data")
    public void step5WrongOrderOfSchemaLocations(Class<? extends AbstractXmlValidator> implementation) throws Exception {
		this.implementation = implementation;
        validate(ROOT_NAMESPACE_GPBDB,
        		SCHEMA_LOCATION_SOAP_ENVELOPE+" "+
                SCHEMA_LOCATION_GPBDB_MESSAGE+" "+
				SCHEMA_LOCATION_GPBDB_GPBDB+" "+
				SCHEMA_LOCATION_GPBDB_REQUEST+" "+
        		SCHEMA_LOCATION_GPBDB_AFDTYPES+" "+
				SCHEMA_LOCATION_GPBDB_RESPONSE,
				INPUT_FILE_GPBDB_OK);
    }

    @ParameterizedTest
	@MethodSource("data")
    public void unresolvableSchema(Class<? extends AbstractXmlValidator> implementation) throws Exception {
		this.implementation = implementation;
    	try {
    		validate(ROOT_NAMESPACE_GPBDB, "http://www.ing.com/BESTAATNIET /Bestaatniet.xsd ",null,MSG_SCHEMA_NOT_FOUND);
    	} catch (ConfigurationException e) {
    		assertThat(e.getMessage(),containsString(MSG_SCHEMA_NOT_FOUND));
    	}
    }

	@ParameterizedTest // step4errorr1.xml uses the namespace xmlns="http://www.ing.com/BESTAATNIET
	@MethodSource("data")
    public void step5ValidationErrorUnknownNamespace(Class<? extends AbstractXmlValidator> implementation) throws Exception {
		this.implementation = implementation;
        validateIgnoreUnknownNamespacesOff(ROOT_NAMESPACE_GPBDB,
        		SCHEMA_LOCATION_SOAP_ENVELOPE+" "+
                SCHEMA_LOCATION_GPBDB_MESSAGE+" "+
				SCHEMA_LOCATION_GPBDB_REQUEST+" "+
				SCHEMA_LOCATION_GPBDB_RESPONSE+" "+
				SCHEMA_LOCATION_GPBDB_GPBDB,
				INPUT_FILE_GPBDB_ERR1,MSG_UNKNOWN_NAMESPACE);
    }

	@ParameterizedTest
	@MethodSource("data")
    public void validationUnknownNamespaceSwitchedOff(Class<? extends AbstractXmlValidator> implementation) throws Exception {
		this.implementation = implementation;
    	validateIgnoreUnknownNamespacesOff(ROOT_NAMESPACE_GPBDB,
        		SCHEMA_LOCATION_SOAP_ENVELOPE, // every other namespace is thus unknown
				INPUT_FILE_GPBDB_ERR1,MSG_UNKNOWN_NAMESPACE);
    }

	@ParameterizedTest
	@MethodSource("data")
    public void validationUnknownNamespaceSwitchedOn(Class<? extends AbstractXmlValidator> implementation) throws Exception {
		this.implementation = implementation;
    	validateIgnoreUnknownNamespacesOn(ROOT_NAMESPACE_GPBDB,
    			SCHEMA_LOCATION_SOAP_ENVELOPE, // every other namespace is thus unknown
				INPUT_FILE_GPBDB_ERR1,null);
    }

	@ParameterizedTest
	@MethodSource("data")
	public void step5ValidationErrorUnknownTag(Class<? extends AbstractXmlValidator> implementation) throws Exception {
		this.implementation = implementation;
		String[] expectedFailureReasons = {MSG_CANNOT_FIND_DECLARATION, MSG_INVALID_CONTENT};
		validate(ROOT_NAMESPACE_GPBDB,
				SCHEMA_LOCATION_SOAP_ENVELOPE + " " +
						SCHEMA_LOCATION_GPBDB_MESSAGE + " " +
						SCHEMA_LOCATION_GPBDB_GPBDB + " " +
						SCHEMA_LOCATION_GPBDB_REQUEST + " " +
						SCHEMA_LOCATION_GPBDB_RESPONSE,
				INPUT_FILE_GPBDB_ERR2,
				expectedFailureReasons);
	}

	@ParameterizedTest
	@MethodSource("data")
    public void step5ValidationUnknownNamespaces(Class<? extends AbstractXmlValidator> implementation) throws Exception {
		this.implementation = implementation;
        validate(ROOT_NAMESPACE_GPBDB,SCHEMA_LOCATION_SOAP_ENVELOPE, INPUT_FILE_GPBDB_OK,MSG_UNKNOWN_NAMESPACE);
    }

}
