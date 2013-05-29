package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.validation.XmlValidatorException;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.wsdl.WSDLException;
import java.io.IOException;

import static org.mockito.Mockito.mock;


/**
  * @author Michiel Meeuwissen
 */


public class WsdlXmlValidatorTest {

    private static final String SIMPLE                = "test/simple.wsdl";
    private static final String SIMPLE_WITH_INCLUDE   = "test/simple_withinclude.wsdl";
    private static final String SIMPLE_WITH_REFERENCE = "test/simple_withreference.wsdl";
    private static final String TIBCO                 = "Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1.wsdl";


    private IPipeLineSession session = mock(IPipeLineSession.class);


    @Test
    public void getInputSchema() throws IOException, WSDLException, ConfigurationException {
        WsdlXmlValidator val = new WsdlXmlValidator();
        val.setWsdl(SIMPLE);
        /*Schema inputSchema = val.getInputSchema();
        assertNotNull(inputSchema);
        System.out.println("" + val.toString(inputSchema));*/
    }

    @Test
    public void wsdlValidate() throws IOException, PipeRunException, SAXException, WSDLException, ConfigurationException, XmlValidatorException {
        WsdlXmlValidator val = new WsdlXmlValidator();
        val.setValidateSoapEnvelope(null);
        val.setWsdl(SIMPLE);
        val.setThrowException(true);
        val.registerForward(new PipeForward("success", null));
        val.configure();
        val.validate("<TradePriceRequest xmlns=\"http://example.com/stockquote.xsd\"><tickerSymbol>foo</tickerSymbol></TradePriceRequest>", session);
    }

    @Test
    public void wsdlInValidate() throws IOException, PipeRunException, SAXException, WSDLException, ConfigurationException, XmlValidatorException {
        WsdlXmlValidator val = new WsdlXmlValidator();
        val.setValidateSoapEnvelope("false");
        val.setWsdl(SIMPLE);
        val.setThrowException(true);
        val.registerForward(new PipeForward("success", null));
        val.configure();
        val.validate("<TradePriceRequest xmlns=\"http://example.com/stockquote.xsd\"><tickerSymbol>foo</tickerSymbol></TradePriceRequest>", session);
    }


    @Test
    public void getInputSchemaWithInclude() throws IOException, WSDLException, ConfigurationException {
        WsdlXmlValidator val = new WsdlXmlValidator();
        val.setValidateSoapEnvelope("false");
        val.setWsdl(SIMPLE_WITH_INCLUDE);
       /* Schema inputSchema = val.getInputSchema();
        assertNotNull(inputSchema);
        System.out.println("" + val.toString(inputSchema));*/
    }
    @Test
    public void wsdlValidateWithInclude() throws IOException, PipeRunException, SAXException, WSDLException, ConfigurationException, XmlValidatorException {
        WsdlXmlValidator val = new WsdlXmlValidator();
        val.setValidateSoapEnvelope("false");
        val.setWsdl(SIMPLE_WITH_INCLUDE);
        val.setThrowException(true);
        val.registerForward(new PipeForward("success", null));
        val.configure();
        val.validate("<TradePriceRequest xmlns=\"http://example.com/stockquote.xsd\"><tickerSymbol>foo</tickerSymbol></TradePriceRequest>", session);
    }

    @Test
    public void wsdlValidateWithReference() throws IOException, PipeRunException, SAXException, WSDLException, ConfigurationException, XmlValidatorException {
        WsdlXmlValidator val = new WsdlXmlValidator();
        val.setValidateSoapEnvelope(null);
        val.setWsdl(SIMPLE_WITH_REFERENCE);
        val.setThrowException(true);
        val.registerForward(new PipeForward("success", null));
        val.configure();
        val.validate("<TradePriceRequest xmlns=\"http://example.com/stockquote.xsd\"><tickerSymbol>foo</tickerSymbol></TradePriceRequest>", session);
    }

    @Test(expected = SAXParseException.class)
    public void wsdlValidateWithReferenceFail() throws IOException, PipeRunException, SAXException, WSDLException, ConfigurationException, XmlValidatorException {
        WsdlXmlValidator val = new WsdlXmlValidator();
        val.setValidateSoapEnvelope(null);
        val.setWsdl(SIMPLE_WITH_REFERENCE);
        val.setThrowException(true);
        val.registerForward(new PipeForward("success", null));
        val.configure();
        val.validate("<TradePriceRequestERROR xmlns=\"http://example.com/stockquote.xsd\"><tickerSymbol>foo</tickerSymbol></TradePriceRequestERROR>", session);
    }



    @Test
    public void wsdlTibco() throws IOException, PipeRunException, SAXException, WSDLException, ConfigurationException, XmlValidatorException {
        WsdlXmlValidator val = new WsdlXmlValidator();
        val.setWsdl(TIBCO);
        val.setThrowException(true);
        val.registerForward(new PipeForward("success", null));
        val.configure();
        val.validate("<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "  <Body>\n" +
                "    <MessageHeader xmlns=\"http://www.ing.com/CSP/XSD/General/Message_2\">\n" +
                "      <From>\n" +
                "        <Id>Ibis4Toegang</Id>\n" +
                "      </From>\n" +
                "      <HeaderFields>\n" +
                "        <ConversationId/>\n" +
                "        <MessageId>WPNLD8921975_0a4ac029-7747a1ed_12da7d4b033_-7ff3</MessageId>\n" +
                "        <ExternalRefToMessageId/>\n" +
                "        <Timestamp>2001-12-17T09:30:47</Timestamp>\n" +
                "      </HeaderFields>\n" +
                "    </MessageHeader>\n" +
                "    <Request xmlns=\"http://www.ing.com/nl/banking/coe/xsd/bankingcustomer_generate_01/getpartybasicdatabanking_01\">\n" +
                "      <BankSparen xmlns=\"http://www.ing.com/bis/xsd/nl/banking/bankingcustomer_generate_01_getpartybasicdatabanking_request_01\">\n" +
                "        <PRD>\n" +
                "          <KLT>\n" +
                "            <KLT_NA_RELNUM>181373377001</KLT_NA_RELNUM>\n" +
                "          </KLT>\n" +
                "        </PRD>\n" +
                "      </BankSparen>\n" +
                "    </Request>\n" +
                "  </Body>\n" +
                "</Envelope>\n" +
                "", session);
    }

    @Test(expected = SAXParseException.class)
    public void wsdlTibcoFailEnvelop() throws IOException, PipeRunException, SAXException, WSDLException, ConfigurationException, XmlValidatorException {
        WsdlXmlValidator val = new WsdlXmlValidator();
        val.setWsdl(TIBCO);
        val.setThrowException(true);
        val.registerForward(new PipeForward("success", null));
        val.configure();
        val.validate("<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "  <BodyERROR>\n" +
                "    <MessageHeader xmlns=\"http://www.ing.com/CSP/XSD/General/Message_2\">\n" +
                "      <From>\n" +
                "        <Id>Ibis4Toegang</Id>\n" +
                "      </From>\n" +
                "      <HeaderFields>\n" +
                "        <ConversationId/>\n" +
                "        <MessageId>WPNLD8921975_0a4ac029-7747a1ed_12da7d4b033_-7ff3</MessageId>\n" +
                "        <ExternalRefToMessageId/>\n" +
                "        <Timestamp>2001-12-17T09:30:47</Timestamp>\n" +
                "      </HeaderFields>\n" +
                "    </MessageHeader>\n" +
                "    <Request xmlns=\"http://www.ing.com/nl/banking/coe/xsd/bankingcustomer_generate_01/getpartybasicdatabanking_01\">\n" +
                "      <BankSparen xmlns=\"http://www.ing.com/bis/xsd/nl/banking/bankingcustomer_generate_01_getpartybasicdatabanking_request_01\">\n" +
                "        <PRD>\n" +
                "          <KLT>\n" +
                "            <KLT_NA_RELNUM>181373377001</KLT_NA_RELNUM>\n" +
                "          </KLT>\n" +
                "        </PRD>\n" +
                "      </BankSparen>\n" +
                "    </Request>\n" +
                "  </BodyERROR>\n" +
                "</Envelope>\n" +
                "", session);
    }

    @Test(expected = SAXParseException.class)
    public void wsdlTibcoFailMessage() throws IOException, PipeRunException, SAXException, WSDLException, ConfigurationException, XmlValidatorException {
        WsdlXmlValidator val = new WsdlXmlValidator();
        val.setWsdl(TIBCO);
        val.setThrowException(true);
        val.registerForward(new PipeForward("success", null));
        val.configure();
        val.validate("<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "  <Body>\n" +
                "    <MessageHeader xmlns=\"http://www.ing.com/CSP/XSD/General/Message_2\">\n" +
                "      <From>\n" +
                "        <Id>Ibis4Toegang</Id>\n" +
                "      </From>\n" +
                "      <HeaderFields>\n" +
                "        <ConversationId/>\n" +
                "        <MessageId>WPNLD8921975_0a4ac029-7747a1ed_12da7d4b033_-7ff3</MessageId>\n" +
                "        <ExternalRefToMessageId/>\n" +
                "        <Timestamp>2001-12-17T09:30:47</Timestamp>\n" +
                "      </HeaderFields>\n" +
                "    </MessageHeader>\n" +
                "    <Request xmlns=\"http://www.ing.com/nl/banking/coe/xsd/bankingcustomer_generate_01/getpartybasicdatabanking_01\">\n" +
                "      <BankSparen xmlns=\"http://www.ing.com/bis/xsd/nl/banking/bankingcustomer_generate_01_getpartybasicdatabanking_request_01\">\n" +
                "        <PRD>\n" +
                "          <KLTERROR>\n" +
                "            <KLT_NA_RELNUM>181373377001</KLT_NA_RELNUM>\n" +
                "          </KLTERROR>\n" +
                "        </PRD>\n" +
                "      </BankSparen>\n" +
                "    </Request>\n" +
                "  </Body>\n" +
                "</Envelope>\n" +
                "", session);
    }



}

