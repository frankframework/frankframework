package nl.nn.adapterframework.validation;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * @author Michiel Meeuwissenˇ
 */
public class JavaxXmlValidatorTest {


    @Test
    public void test() throws SAXException, IOException, XMLStreamException {
        JavaxXmlValidator validator = new JavaxXmlValidator();
        validator.setSchemaLocation(
            "http://www.ing.com/CSP/XSD/General/Message_2 " +
            "/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/Message_2.xsd "
              +
                 "http://www.ing.com/bis/xsd/nl/banking/bankingcustomer_generate_01_getpartybasicdatabanking_request_01 " +
                 "/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/bankingcustomer_generate_01_getpartybasicdatabanking_request_01.xsd "

                +
            "http://www.ing.com/nl/banking/coe/xsd/bankingcustomer_generate_01/getpartybasicdatabanking_01 " +
            "/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1/getpartybasicdatabanking_01.xsd "

        );
        System.out.println(validator.getSchemaObject());
    }
}
