package nl.nn.adapterframework.pipes;


import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.validation.AbstractXmlValidator;
import nl.nn.adapterframework.validation.JavaxXmlValidator;
import nl.nn.adapterframework.validation.XercesXmlValidator;
import nl.nn.adapterframework.validation.XmlValidatorException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Michiel Meeuwissen
 */
@RunWith(value = Parameterized.class)
public class XmlValidatorTest {

    private Class<AbstractXmlValidator> implementation;

    public XmlValidatorTest(Class<AbstractXmlValidator> implementation) {
        this.implementation = implementation;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
            {XercesXmlValidator.class},
            {JavaxXmlValidator.class}
        };
        return Arrays.asList(data);
    }

    @Test
    public void step5() throws PipeRunException, ConfigurationException, IOException, XmlValidatorException {
        getValidator(
            "http://schemas.xmlsoap.org/soap/envelope/ " +
                "/Tibco/xsd/soap/envelope.xsd " +

                "http://www.ing.com/CSP/XSD/General/Message_2 " +
                "/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/Message_2.xsd " +

                "http://ing.nn.afd/AFDTypes " +
                "/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/AFDTypes.xsd " +

                "http://www.ing.com/bis/xsd/nl/banking/bankingcustomer_generate_01_getpartybasicdatabanking_request_01 " +
                "/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/bankingcustomer_generate_01_getpartybasicdatabanking_request_01.xsd " +

                "http://www.ing.com/bis/xsd/nl/banking/bankingcustomer_generate_01_getpartybasicdatabanking_response_01 " +
                "/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/bankingcustomer_generate_01_getpartybasicdatabanking_response_01.xsd " +

                "http://www.ing.com/nl/banking/coe/xsd/bankingcustomer_generate_01/getpartybasicdatabanking_01 " +
                "/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/getpartybasicdatabanking_01.xsd"
        ).
            validate(getTestXml("/step5.xml"), new PipeLineSessionBase());
    }

    @Test(expected = XmlValidatorException.class)
    // Fails for XmlValidatorBaseXerces26 Hard to fix....
    public void step5MissingNamespace() throws PipeRunException, ConfigurationException, IOException, XmlValidatorException {
        getValidator(
            "http://schemas.xmlsoap.org/soap/envelope/ " +
                "/Tibco/xsd/soap/envelope.xsd " +

                "http://www.ing.com/CSP/XSD/General/Message_2 " +
                "/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/Message_2.xsd " +

                "http://www.ing.com/bis/xsd/nl/banking/bankingcustomer_generate_01_getpartybasicdatabanking_request_01 " +
                "/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/bankingcustomer_generate_01_getpartybasicdatabanking_request_01.xsd " +

                "http://www.ing.com/nl/banking/coe/xsd/bankingcustomer_generate_01/getpartybasicdatabanking_01 " +
                "/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/getpartybasicdatabanking_01.xsd"
        ).
            validate(getTestXml("/step5.xml"), new PipeLineSessionBase());
    }

    @Test
    public void step5WrongOrder() throws PipeRunException, ConfigurationException, IOException, XmlValidatorException {
        getValidator(
            "http://schemas.xmlsoap.org/soap/envelope/ " +
                "/Tibco/xsd/soap/envelope.xsd " +

                "http://www.ing.com/CSP/XSD/General/Message_2 " +
                "/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/Message_2.xsd " +

                "http://www.ing.com/nl/banking/coe/xsd/bankingcustomer_generate_01/getpartybasicdatabanking_01 " +
                "/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/getpartybasicdatabanking_01.xsd " +

                "http://www.ing.com/bis/xsd/nl/banking/bankingcustomer_generate_01_getpartybasicdatabanking_request_01 " +
                "/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/bankingcustomer_generate_01_getpartybasicdatabanking_request_01.xsd " +

                "http://ing.nn.afd/AFDTypes " +
                "/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/AFDTypes.xsd " +

                "http://www.ing.com/bis/xsd/nl/banking/bankingcustomer_generate_01_getpartybasicdatabanking_response_01 " +
                "/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/bankingcustomer_generate_01_getpartybasicdatabanking_response_01.xsd "
        ).
            validate(getTestXml("/step5.xml"), new PipeLineSessionBase());
    }

    @Test(expected = ConfigurationException.class)
    public void unresolvableSchema() throws PipeRunException, ConfigurationException, IOException {
        getValidator(
            "http://www.ing.com/BESTAATNIET " +
                "/Bestaatniet.xsd ");
    }
    @Test(expected = XmlValidatorException.class) // step4errorr1.xml uses the namespace xmlns="http://www.ing.com/BESTAATNIET
    public void step5ValidationErrorUnknownNamespace() throws PipeRunException, ConfigurationException, IOException, XmlValidatorException {
        getValidator(
            "http://schemas.xmlsoap.org/soap/envelope/ " +
                "/Tibco/xsd/soap/envelope.xsd " +

                "http://www.ing.com/CSP/XSD/General/Message_2 " +
                "/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/Message_2.xsd " +

                "http://www.ing.com/nl/banking/coe/xsd/bankingcustomer_generate_01/getpartybasicdatabanking_01 " +
                "/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/getpartybasicdatabanking_01.xsd " +

                "http://www.ing.com/bis/xsd/nl/banking/bankingcustomer_generate_01_getpartybasicdatabanking_request_01 " +
                "/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/bankingcustomer_generate_01_getpartybasicdatabanking_request_01.xsd"
        ).
            validate(getTestXml("/step5errors1.xml"), new PipeLineSessionBase());
    }

    @Test
    public void validationUnknownNamespaceSwitchedOff() throws PipeRunException, ConfigurationException, IOException, XmlValidatorException {
        XmlValidator validator = getValidator(
            "http://schemas.xmlsoap.org/soap/envelope/ " +
                "/Tibco/xsd/soap/envelope.xsd " // every other namespace is thus unknown
        );
		validator.setIgnoreUnknownNamespaces(false);
        validator.validate(getTestXml("/step5errors1.xml"), new PipeLineSessionBase());
    }

    @Test(expected = XmlValidatorException.class)
    public void validationUnknownNamespaceSwitchedOn() throws PipeRunException, ConfigurationException, IOException, XmlValidatorException {
        XmlValidator validator = getValidator(
            "http://schemas.xmlsoap.org/soap/envelope/ " +
                "/Tibco/xsd/soap/envelope.xsd " // every other namespace is thus unknown
        );
		validator.setIgnoreUnknownNamespaces(true);
        validator.validate(getTestXml("/step5errors1.xml"), new PipeLineSessionBase());
    }

    @Test(expected = XmlValidatorException.class)
    public void step5ValidationErrorUnknownTag() throws PipeRunException, ConfigurationException, IOException, XmlValidatorException {
        getValidator(
            "http://schemas.xmlsoap.org/soap/envelope/ " +
                "/Tibco/xsd/soap/envelope.xsd " +

                "http://www.ing.com/CSP/XSD/General/Message_2 " +
                "/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/Message_2.xsd " +

                "http://www.ing.com/nl/banking/coe/xsd/bankingcustomer_generate_01/getpartybasicdatabanking_01 " +
                "/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/getpartybasicdatabanking_01.xsd " +

                "http://www.ing.com/bis/xsd/nl/banking/bankingcustomer_generate_01_getpartybasicdatabanking_request_01 " +
                "/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/bankingcustomer_generate_01_getpartybasicdatabanking_request_01.xsd"
        ).
            validate(getTestXml("/step5errors2.xml"), new PipeLineSessionBase());
    }

    @Test(expected = XmlValidatorException.class)
    @Ignore("This fails in JavaXmlValidator, and this is actually defendable, because the soap envelope accepts any children, so also the namespace doesn't really matter.")
    public void step5ValidationUnknownNamespaces() throws PipeRunException, ConfigurationException, IOException, XmlValidatorException {
        XmlValidator validator = getValidator(
            "http://schemas.xmlsoap.org/soap/envelope/ " +
                "/Tibco/xsd/soap/envelope.xsd "
        );
        validator.validate(getTestXml("/step5.xml"), new PipeLineSessionBase());
    }

    @Test
    public void addNamespaceToSchema() throws ConfigurationException, IOException, PipeRunException, XmlValidatorException {
        XmlValidator validator = getValidator(
            "http://www.ing.com/testxmlns " +
            "/GetIntermediaryAgreementDetails/xsd/A.xsd");
        validator.setAddNamespaceToSchema(true);

        validator.validate(getTestXml("/intermediaryagreementdetails.xml"), new PipeLineSessionBase());
    }

    @Test(expected = XmlValidatorException.class)
    public void addNamespaceToSchemaWithErrors() throws ConfigurationException, IOException, PipeRunException, XmlValidatorException {
        XmlValidator validator = getValidator(
            "http://www.ing.com/testxmlns " +
                "/GetIntermediaryAgreementDetails/xsd/A.xsd");
        validator.setAddNamespaceToSchema(true);

        validator.validate(getTestXml("/intermediaryagreementdetails_with_errors.xml"), new PipeLineSessionBase());
    }

    @Test(expected = XmlValidatorException.class)
    public void addNamespaceToSchemaNamesspaceMismatch() throws ConfigurationException, IOException, PipeRunException, XmlValidatorException {
        XmlValidator validator = getValidator(
            "http://www.ing.com/testxmlns_mismatch " +
                "/GetIntermediaryAgreementDetails/xsd/A.xsd");
        validator.setAddNamespaceToSchema(true);

        validator.validate(getTestXml("/intermediaryagreementdetails.xml"), new PipeLineSessionBase());
    }


    @Test
    public  void unusedXsi() throws ConfigurationException, IOException, PipeRunException, XmlValidatorException {
        XmlValidator validator = getValidator(
            "http://www.ing.com/geenidee " +
                "/xsd/Unisys_xsd/REQ1000.xsd");
        validator.validate(getTestXml("/unusedXsi.xml"), new PipeLineSessionBase());

    }
    static PipeForward getSuccess() {
        PipeForward forward = new PipeForward();
        forward.setName("success");
        return forward;
    }

    XmlValidator getValidator(String schemaLocation) throws ConfigurationException {
        return getValidator(schemaLocation, implementation);
    }
    public  static  XmlValidator getValidator(String schemaLocation, Class<AbstractXmlValidator> implementation) throws ConfigurationException {
        XmlValidator validator = new XmlValidator();
        try {
            validator.setImplementation(implementation);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        validator.setSchemaLocation(schemaLocation);
        validator.registerForward(getSuccess());
        validator.setThrowException(true);
        validator.configure();
        validator.setFullSchemaChecking(true);
        return validator;
    }
    private String getTestXml(String testxml) throws IOException {
        BufferedReader buf = new BufferedReader(new InputStreamReader(XmlValidator.class.getResourceAsStream(testxml)));
        StringBuilder string = new StringBuilder();
        String line = buf.readLine();
        while (line != null) {
            string.append(line);
            line = buf.readLine();
        }
        return string.toString();

    }

}
