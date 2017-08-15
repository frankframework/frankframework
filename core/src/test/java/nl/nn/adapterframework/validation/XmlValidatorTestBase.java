package nl.nn.adapterframework.validation;


import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunException;

/**
 * @author Gerrit van Brakel / Michiel Meeuwissen
 */
public abstract class XmlValidatorTestBase extends ValidatorTestBase {

    @Test
    public void straighforward() throws IllegalAccessException, InstantiationException, XmlValidatorException, IOException, PipeRunException, ConfigurationException {
    	validation(ROOT_NAMESPACE_BASIC,SCHEMA_LOCATION_BASIC_A_OK,INPUT_FILE_BASIC_A_OK,false,null);
    	validation(ROOT_NAMESPACE_BASIC,SCHEMA_LOCATION_BASIC_A_OK,INPUT_FILE_BASIC_A_ERR,false,MSG_INVALID_CONTENT);
    	validation(ROOT_NAMESPACE_BASIC,SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE,INPUT_FILE_BASIC_A_OK,false,MSG_CANNOT_FIND_DECLARATION);
    	validation(ROOT_NAMESPACE_BASIC,SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE,INPUT_FILE_BASIC_A_ERR,false,MSG_CANNOT_FIND_DECLARATION);
    }


    @Test
    public void addTargetNamespace() throws IllegalAccessException, InstantiationException, XmlValidatorException, IOException, PipeRunException, ConfigurationException {
    	validation(ROOT_NAMESPACE_BASIC,SCHEMA_LOCATION_BASIC_A_OK,INPUT_FILE_BASIC_A_OK,true,null);
    	validation(ROOT_NAMESPACE_BASIC,SCHEMA_LOCATION_BASIC_A_OK,INPUT_FILE_BASIC_A_ERR,true,MSG_INVALID_CONTENT);
    	validation(ROOT_NAMESPACE_BASIC,SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE,INPUT_FILE_BASIC_A_OK,true,null);
    	validation(ROOT_NAMESPACE_BASIC,SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE,INPUT_FILE_BASIC_A_ERR,true,MSG_INVALID_CONTENT);
    }

    @Test
    public void addNamespaceToSchema() throws Exception {
        validate(ROOT_NAMESPACE_BASIC,SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE, true ,INPUT_FILE_BASIC_A_OK,null);
    }

    @Test
    public void addNamespaceToSchemaWithErrors() throws Exception {
        validate(ROOT_NAMESPACE_BASIC,SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE, true ,INPUT_FILE_BASIC_A_ERR,MSG_INVALID_CONTENT);
    }

    @Test
    public void addNamespaceToSchemaNamespaceMismatch() throws Exception {
        validate(ROOT_NAMESPACE_BASIC,SCHEMA_LOCATION_BASIC_A_NO_TARGETNAMESPACE_MISMATCH, true ,INPUT_FILE_BASIC_A_OK,MSG_CANNOT_FIND_DECLARATION);
    }

    @Test
    public void missingMandatoryElement() throws Exception {
        validate(ROOT_NAMESPACE_GPBDB,
        		SCHEMA_LOCATION_SOAP_ENVELOPE,
				INPUT_FILE_GPBDB_NOBODY,MSG_IS_NOT_COMPLETE);
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
				INPUT_FILE_GPBDB_OK,null);
    }

    @Test(expected = XmlValidatorException.class)
    @Ignore("Fails for XmlValidatorBaseXerces26 Hard to fix....")
    public void step5MissingNamespace() throws Exception {
        validate(ROOT_NAMESPACE_GPBDB,
        		SCHEMA_LOCATION_SOAP_ENVELOPE+" "+
                SCHEMA_LOCATION_GPBDB_MESSAGE+" "+
				SCHEMA_LOCATION_GPBDB_REQUEST+" "+
				SCHEMA_LOCATION_GPBDB_GPBDB,
				INPUT_FILE_GPBDB_OK,"weetniet");
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
				INPUT_FILE_GPBDB_OK,null);
    }

    @Test
    public void unresolvableSchema() throws Exception {
        validate(ROOT_NAMESPACE_GPBDB,
            "http://www.ing.com/BESTAATNIET /Bestaatniet.xsd ",null,MSG_SCHEMA_NOT_FOUND);
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
        validate(ROOT_NAMESPACE_GPBDB,
        		SCHEMA_LOCATION_SOAP_ENVELOPE+" "+
           		SCHEMA_LOCATION_GPBDB_MESSAGE+" "+
				SCHEMA_LOCATION_GPBDB_GPBDB+" "+
				SCHEMA_LOCATION_GPBDB_REQUEST+" "+
				SCHEMA_LOCATION_GPBDB_RESPONSE, 
				INPUT_FILE_GPBDB_ERR2,MSG_INVALID_CONTENT);
    }

    @Test
    public void step5ValidationUnknownNamespaces() throws Exception {
        validate(ROOT_NAMESPACE_GPBDB,SCHEMA_LOCATION_SOAP_ENVELOPE, INPUT_FILE_GPBDB_OK,MSG_UNKNOWN_NAMESPACE);
    }

}
