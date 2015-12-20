package nl.nn.adapterframework.soap;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.pipes.XmlValidator;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

/**
 * @author Michiel Meeuwissen
 */
public class SoapValidatorTest {


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
        xml.doPipe(getTestXml("/valid_soap.xml"), new PipeLineSessionBase());

    }

    @Test
    public void validate12() throws ConfigurationException, IOException, PipeRunException {
        SoapValidator xml = getSoapValidator(true);
        System.out.println("1 " + new Date());
        xml.doPipe(getTestXml("/valid_soap.xml"), new PipeLineSessionBase());
        System.out.println("2" + new Date());
        xml.doPipe(getTestXml("/valid_soap.xml"), new PipeLineSessionBase());
        System.out.println("3" + new Date());

    }

    @Test
    public void validate12_explicitversion() throws ConfigurationException, IOException, PipeRunException {
        SoapValidator xml = getSoapValidator(true, "1.2");
        xml.doPipe(getTestXml("/valid_soap_1.2.xml"), new PipeLineSessionBase());
    }

    @Test(expected = PipeRunException.class)
    public void validate12_invalidversion() throws ConfigurationException, IOException, PipeRunException {
        SoapValidator xml = getSoapValidator();
        xml.setSoapVersion("1.2");
        xml.doPipe(getTestXml("/valid_soap.xml"), new PipeLineSessionBase());

    }

    @Test(expected = PipeRunException.class)
    public void validate12_invalid() throws ConfigurationException, IOException, PipeRunException {
        SoapValidator xml = getSoapValidator();
        xml.setSoapVersion("1.2");
        xml.doPipe(getTestXml("/invalid_soap.xml"), new PipeLineSessionBase());

    }

    @Test(expected = PipeRunException.class)
    public void validate12_invalid_body() throws ConfigurationException, IOException, PipeRunException {
        SoapValidator xml = getSoapValidator();
        xml.setSoapVersion("1.1");
        xml.doPipe(getTestXml("/invalid_soap_body.xml"), new PipeLineSessionBase());

    }

    @Test(expected = PipeRunException.class)
    public void validate12_unknown_namespace_body() throws ConfigurationException, IOException, PipeRunException {
        SoapValidator xml = getSoapValidator();
        xml.setSoapVersion("1.1");
        xml.doPipe(getTestXml("/unknown_namespace_soap_body.xml"), new PipeLineSessionBase());

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
        validator.setSchemaLocation(
            "http://www.ing.com/CSP/XSD/General/Message_2 " +
            "/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/Message_2.xsd " +
            "http://www.ing.com/nl/banking/coe/xsd/bankingcustomer_generate_01/getpartybasicdatabanking_01 " +
            "/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/bankingcustomer_generate_01_getpartybasicdatabanking_response_01.xsd " +
            "http://www.ing.com/bis/xsd/nl/banking/bankingcustomer_generate_01_getpartybasicdatabanking_request_01 " +
            "/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/bankingcustomer_generate_01_getpartybasicdatabanking_request_01.xsd"

        );
        validator.setSoapHeader("MessageHeader");
        validator.setSoapBody("Request");
        validator.registerForward(getSuccess());
        validator.setThrowException(true);
        validator.setFullSchemaChecking(true);
        validator.configure();
        return validator;
    }
}
