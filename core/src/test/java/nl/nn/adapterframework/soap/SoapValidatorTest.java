package nl.nn.adapterframework.soap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.pipes.XmlValidator;
import nl.nn.adapterframework.validation.ValidatorTestBase;

/**
 * @author Michiel Meeuwissen
 */
public class SoapValidatorTest {

	public String SCHEMALOCATION_SET_GPBDB = ValidatorTestBase.SCHEMA_LOCATION_GPBDB_MESSAGE+" "+
			ValidatorTestBase.SCHEMA_LOCATION_GPBDB_GPBDB+" "+
			ValidatorTestBase.SCHEMA_LOCATION_GPBDB_RESPONSE+" "+
            ValidatorTestBase.SCHEMA_LOCATION_GPBDB_REQUEST;
	public String INPUT_FILE_GPBDB_VALID_SOAP					=ValidatorTestBase.BASE_DIR_VALIDATION+"/Tibco/Soap/valid_soap.xml";
	public String INPUT_FILE_GPBDB_VALID_SOAP_1_2				=ValidatorTestBase.BASE_DIR_VALIDATION+"/Tibco/Soap/valid_soap_1.2.xml";
	public String INPUT_FILE_GPBDB_INVALID_SOAP					=ValidatorTestBase.BASE_DIR_VALIDATION+"/Tibco/Soap/invalid_soap.xml";
	public String INPUT_FILE_GPBDB_INVALID_SOAP_BODY			=ValidatorTestBase.BASE_DIR_VALIDATION+"/Tibco/Soap/invalid_soap_body.xml";
	public String INPUT_FILE_GPBDB_UNKNOWN_NAMESPACE_SOAP_BODY	=ValidatorTestBase.BASE_DIR_VALIDATION+"/Tibco/Soap/unknown_namespace_soap_body.xml";
	

    @Test
	@Ignore("Don't know what it should test any more")
    public void basic() {
        SoapValidator validator = new SoapValidator();
        validator.setSchemaLocation("http://www.ing.com/pim test.xsd");
        validator.setSoapBody("{http://www.ing.com/pim}a");
		System.out.println(validator.getSoapBody());
		// WTF it was something with QName, it is a string. I have no idea whether and how that must be tested (I don't know what the string is meant to represent any more)
        // assertEquals(new QName("http://www.ing.com/pim", "a"), validator.getSoapBodyTags().iterator().next());

    }
    @Test
	@Ignore("Don't know what it should test any more")
    public void defaultNamespace()  {
        SoapValidator validator = new SoapValidator();
        validator.setSchemaLocation("http://www.ing.com/pim test.xsd");
        validator.setSoapBody("a");
//		WTF assertEquals(new QName("http://www.ing.com/pim", "a"), validator.getSoapBodyTags().iterator().next());
    }

    @Test
    public void validate11() throws ConfigurationException, IOException, PipeRunException {
        XmlValidator xml = getSoapValidator(true);
    	xml.setSchemaLocation(SCHEMALOCATION_SET_GPBDB);
    	xml.configure();
        xml.doPipe(getTestXml(INPUT_FILE_GPBDB_VALID_SOAP), new PipeLineSessionBase());
    }

    @Test
    public void validate12() throws ConfigurationException, IOException, PipeRunException {
    	SoapValidator xml = getSoapValidator(true);
    	xml.setSchemaLocation(SCHEMALOCATION_SET_GPBDB);
    	xml.configure();
        System.out.println("1 " + new Date());
        xml.doPipe(getTestXml(INPUT_FILE_GPBDB_VALID_SOAP), new PipeLineSessionBase());
        System.out.println("2" + new Date());
        xml.doPipe(getTestXml(INPUT_FILE_GPBDB_VALID_SOAP), new PipeLineSessionBase());
        System.out.println("3" + new Date());
    }

    @Test
    public void validate12_explicitversion() throws ConfigurationException, IOException, PipeRunException {
    	SoapValidator xml = getSoapValidator(true, "1.2");
    	xml.setSchemaLocation(SCHEMALOCATION_SET_GPBDB);
    	xml.configure();
        xml.doPipe(getTestXml(INPUT_FILE_GPBDB_VALID_SOAP_1_2), new PipeLineSessionBase());
    }

    @Test(expected = PipeRunException.class)
    public void validate12_invalid() throws ConfigurationException, IOException, PipeRunException {
        SoapValidator xml = getSoapValidator();
    	xml.setSchemaLocation(SCHEMALOCATION_SET_GPBDB);
        xml.setSoapVersion("1.2");
    	xml.configure();
        xml.doPipe(getTestXml(INPUT_FILE_GPBDB_INVALID_SOAP), new PipeLineSessionBase());

    }

    @Test(expected = PipeRunException.class)
    public void validate12_invalid_body() throws ConfigurationException, IOException, PipeRunException {
        SoapValidator xml = getSoapValidator();
    	xml.setSchemaLocation(SCHEMALOCATION_SET_GPBDB);
        xml.setSoapVersion("1.1");
    	xml.configure();
        xml.doPipe(getTestXml(INPUT_FILE_GPBDB_INVALID_SOAP_BODY), new PipeLineSessionBase());
    }

    @Test(expected = PipeRunException.class)
    public void validate12_unknown_namespace_body() throws ConfigurationException, IOException, PipeRunException {
        SoapValidator xml = getSoapValidator();
    	xml.setSchemaLocation(SCHEMALOCATION_SET_GPBDB);
        xml.setSoapVersion("1.1");
    	xml.configure();
        xml.doPipe(getTestXml(INPUT_FILE_GPBDB_UNKNOWN_NAMESPACE_SOAP_BODY), new PipeLineSessionBase());
    }

 
    
    
    private String getTestXml(String testxml) throws IOException {
        BufferedReader buf = new BufferedReader(new InputStreamReader(XmlValidator.class.getResourceAsStream(testxml)));
        StringBuilder string = new StringBuilder();
        String line = buf.readLine();
        while (line != null) {
            string.append(line).append("\n");
            line = buf.readLine();
        }
        return string.toString();

    }


    private PipeForward getSuccess() {
        PipeForward forward = new PipeForward();
        forward.setName("success");
        return forward;
    }

    private SoapValidator getSoapValidator() throws ConfigurationException {
        return getSoapValidator(false);
    }

    private SoapValidator getSoapValidator(boolean addNamespaceToSchema) throws ConfigurationException {
        return getSoapValidator(addNamespaceToSchema, null);
    }

    private SoapValidator getSoapValidator(boolean addNamespaceToSchema, String soapVersion) throws ConfigurationException {
    	SoapValidator validator = new SoapValidator();
        if (addNamespaceToSchema) {
            validator.setAddNamespaceToSchema(addNamespaceToSchema);
        }
        if (soapVersion != null) {
            validator.setSoapVersion(soapVersion);
        }
        validator.setSoapHeader("MessageHeader");
        validator.setSoapBody("Request");
        validator.registerForward(getSuccess());
        validator.setThrowException(true);
        validator.setFullSchemaChecking(true);
        return validator;
    }
}
