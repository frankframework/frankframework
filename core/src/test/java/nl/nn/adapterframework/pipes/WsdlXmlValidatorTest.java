package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.wsdl.WSDLException;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.DummyAdapterService;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.unmanaged.DefaultIbisManager;
import nl.nn.adapterframework.validation.ValidatorTestBase;
import nl.nn.adapterframework.validation.XmlValidatorException;


/**
  * @author Michiel Meeuwissen
 */

public class WsdlXmlValidatorTest extends Mockito {
    private static final String SIMPLE                = ValidatorTestBase.BASE_DIR_VALIDATION+"/Wsdl/SimpleWsdl/simple.wsdl";
    private static final String SIMPLE_WITH_INCLUDE   = ValidatorTestBase.BASE_DIR_VALIDATION+"/Wsdl/SimpleWsdl/simple_withinclude.wsdl";
    private static final String SIMPLE_WITH_REFERENCE = ValidatorTestBase.BASE_DIR_VALIDATION+"/Wsdl/SimpleWsdl/simple_withreference.wsdl";
    private static final String TIBCO                 = ValidatorTestBase.BASE_DIR_VALIDATION+"/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1.wsdl";
    private static final String DOUBLE_BODY           = ValidatorTestBase.BASE_DIR_VALIDATION+"/Wsdl/GetPolicyDetails/GetPolicyDetailsDoubleBody.wsdl";
    private static final String BASIC                 = ValidatorTestBase.BASE_DIR_VALIDATION+"/Wsdl/GetPolicyDetails/GetPolicyDetails.wsdl";

    private IPipeLineSession session = mock(IPipeLineSession.class);

    @Test
    public void wsdlValidate() throws IOException, PipeRunException, SAXException, WSDLException, ConfigurationException, XmlValidatorException {
        WsdlXmlValidator val = new WsdlXmlValidator();
        val.setWsdl(SIMPLE);
        val.setSoapBody("TradePriceRequest");
        val.setThrowException(true);
        val.registerForward(new PipeForward("success", null));
        val.configure();
        val.validate("<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\"><Body><TradePriceRequest xmlns=\"http://example.com/stockquote.xsd\"><tickerSymbol>foo</tickerSymbol></TradePriceRequest></Body></Envelope>", session);
    }

    @Test
    public void wsdlValidateWithInclude() throws IOException, PipeRunException, SAXException, WSDLException, ConfigurationException, XmlValidatorException {
        WsdlXmlValidator val = new WsdlXmlValidator();
        val.setWsdl(SIMPLE_WITH_INCLUDE);
        val.setSoapBody("TradePriceRequest");
        val.setThrowException(true);
        val.registerForward(new PipeForward("success", null));
        val.configure();
        val.validate("<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\"><Body><TradePriceRequest xmlns=\"http://example.com/stockquote.xsd\"><tickerSymbol>foo</tickerSymbol></TradePriceRequest></Body></Envelope>", session);
    }

    @Test
    public void wsdlValidateWithReference() throws IOException, PipeRunException, SAXException, WSDLException, ConfigurationException, XmlValidatorException {
        WsdlXmlValidator val = new WsdlXmlValidator();
        val.setWsdl(SIMPLE_WITH_REFERENCE);
        val.setSoapBody("TradePriceRequest");
        val.setThrowException(true);
        val.registerForward(new PipeForward("success", null));
        val.configure();
        val.validate("<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\"><Body><TradePriceRequest xmlns=\"http://example.com/stockquote.xsd\"><tickerSymbol>foo</tickerSymbol></TradePriceRequest></Body></Envelope>", session);
    }

    @Test(expected = XmlValidatorException.class)
    public void wsdlValidateWithReferenceFail() throws IOException, PipeRunException, SAXException, WSDLException, ConfigurationException, XmlValidatorException {
        WsdlXmlValidator val = new WsdlXmlValidator();
        val.setWsdl(SIMPLE_WITH_REFERENCE);
        val.setThrowException(true);
        val.registerForward(new PipeForward("success", null));
        val.configure();
        val.validate("<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\"><Body><TradePriceRequest xmlns=\"http://example.com/stockquote.xsd\"><tickerSymbolERROR>foo</tickerSymbolERROR></TradePriceRequest></Body></Envelope>", session);
    }

    @Test
    public void wsdlTibco() throws IOException, PipeRunException, SAXException, WSDLException, ConfigurationException, XmlValidatorException {
        WsdlXmlValidator val = new WsdlXmlValidator();
        val.setWsdl(TIBCO);
        val.setSoapHeader("MessageHeader");
        val.setSoapBody("Request");
        val.setThrowException(true);
        val.registerForward(new PipeForward("success", null));
        val.configure();
        val.validate("<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "  <Header>\n" +
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
                "  </Header>\n" +
                "  <Body>\n" +
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

    @Test(expected = XmlValidatorException.class)
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

    @Test(expected = XmlValidatorException.class)
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

    @Ignore("Travis has problems with this")
    @Test
	public void wsdlReasonSessionKey() throws ConfigurationException,
			XmlValidatorException, PipeRunException {
		WsdlXmlValidator val = new WsdlXmlValidator();
		val.setWsdl(SIMPLE);
		val.setSoapBody("TradePriceRequest");
		val.setForwardFailureToSuccess(true);
		val.registerForward(new PipeForward("success", null));
		val.configure();
		IPipeLineSession pls = new PipeLineSessionBase();
		val.validate("<xml/>", pls);
		List<String> lines = Arrays.asList(
				((String) pls.get(val.getReasonSessionKey())).split("\\r?\\n"));
		assertEquals("Validation using WsdlXmlValidator with '/Validation/Wsdl/SimpleWsdl/simple.wsdl' failed:", lines.get(0));
		assertEquals("/: at (1,7): cvc-elt.1.a: Cannot find the declaration of element 'xml'.", lines.get(1));
		assertEquals("/: Illegal element 'xml'. Element(s) 'Envelope' expected.", lines.get(2));
		assertEquals("/xml: Unknown namespace ''", lines.get(3));
		assertEquals("/Envelope/Body: Element(s) 'TradePriceRequest' not found", lines.get(4));
		assertEquals("/: Element(s) 'Envelope' not found", lines.get(5));
	}

	@Test(expected = ConfigurationException.class)
	public void wSoapBodyExistsMultipleTimes() throws IOException, PipeRunException, SAXException, WSDLException, ConfigurationException, XmlValidatorException {
		WsdlXmlValidator val = new WsdlXmlValidator();
		val.setWsdl(DOUBLE_BODY);
		val.setSoapHeader("MessageHeader");
		val.setSoapBody("GetPolicyDetails_Request");
		val.setSoapBodyNamespace("http://ibissource.org/XSD/LifeRetailCB/PolicyJuice/1/GetPolicyDetails/1");
		val.setAddNamespaceToSchema(true);
		val.setThrowException(true);
		val.registerForward(new PipeForward("success", null));
		val.configure();
	}

	@Test
	public void warnSchemaLocationAlreadyDefaultValue() throws IOException, PipeRunException, SAXException, WSDLException, ConfigurationException, XmlValidatorException {
		// Mock a configuration with an adapter in it
		IbisManager ibisManager = spy(new DefaultIbisManager());
		ibisManager.setIbisContext(spy(new IbisContext()));
		Configuration configuration = new Configuration(new DummyAdapterService());
		configuration.setName("dummyConfiguration");
		configuration.setVersion("1");
		configuration.setIbisManager(ibisManager);
		ConfigurationWarnings.getInstance().setActiveConfiguration(configuration);

		IAdapter adapter = spy(new Adapter());
		adapter.setName("dummy");
		PipeLine pl = new PipeLine();
		pl.setFirstPipe("dummy");

		WsdlXmlValidator val = new WsdlXmlValidator();
		val.setName("dummy");
		val.setWsdl(BASIC);
		val.setSoapHeader("MessageHeader");
		val.setSoapBody("GetPolicyDetails_Request");
		val.setAddNamespaceToSchema(true);
		val.setSchemaLocation("http://ibissource.org/XSD/LifeRetailCB/PolicyJuice/1/GetPolicyDetails/1 schema2 http://ibissource.org/XSD/Generic/MessageHeader/2 schema1 ");
		val.setThrowException(true);
		val.registerForward(new PipeForward("success", null));

		pl.addPipe(val);
		PipeLineExit ple = new PipeLineExit();
		ple.setPath("success");
		ple.setState("success");
		pl.registerPipeLineExit(ple);
		adapter.registerPipeLine(pl);

		adapter.setConfiguration(configuration);
		configuration.registerAdapter(adapter);

		assertEquals(1, configuration.getConfigurationWarnings().size());
		assertEquals("WsdlXmlValidator [dummy] attribute [schemaLocation] for wsdl [/Validation/Wsdl/GetPolicyDetails/GetPolicyDetails.wsdl] already has a default value [http://ibissource.org/XSD/Generic/MessageHeader/2 schema1 http://ibissource.org/XSD/LifeRetailCB/PolicyJuice/1/GetPolicyDetails/1 schema2]", configuration.getConfigurationWarnings().getFirst());
		ConfigurationWarnings.getInstance().setActiveConfiguration(null);
	}

	@Test
	public void warnUseSoapBodyNameSpace() throws IOException, PipeRunException, SAXException, WSDLException, ConfigurationException, XmlValidatorException {
		// Mock a configuration with an adapter in it
		IbisManager ibisManager = spy(new DefaultIbisManager());
		ibisManager.setIbisContext(spy(new IbisContext()));
		Configuration configuration = new Configuration(new DummyAdapterService());
		configuration.setName("dummyConfiguration");
		configuration.setVersion("1");
		configuration.setIbisManager(ibisManager);
		ConfigurationWarnings.getInstance().setActiveConfiguration(configuration);

		IAdapter adapter = spy(new Adapter());
		adapter.setName("dummy");
		PipeLine pl = new PipeLine();
		pl.setFirstPipe("dummy");

		WsdlXmlValidator val = new WsdlXmlValidator();
		val.setName("dummy");
		val.setWsdl(BASIC);
		val.setSoapHeader("MessageHeader");
		val.setSoapBody("GetPolicyDetails_Request");
		val.setAddNamespaceToSchema(true);
		val.setSchemaLocation("http://ibissource.org/XSD/Generic/MessageHeader/2 schema1 http://ibissource.org/XSD/LifeRetailCB/PolicyJuice/1/GetPolicyDetails/2 schema2");
		val.setThrowException(true);
		val.registerForward(new PipeForward("success", null));

		pl.addPipe(val);
		PipeLineExit ple = new PipeLineExit();
		ple.setPath("success");
		ple.setState("success");
		pl.registerPipeLineExit(ple);
		adapter.registerPipeLine(pl);

		adapter.setConfiguration(configuration);
		configuration.registerAdapter(adapter);

		assertEquals(1, configuration.getConfigurationWarnings().size());
		assertEquals("WsdlXmlValidator [dummy] use attribute [soapBodyNamespace] instead of attribute [schemaLocation] with value [http://ibissource.org/XSD/Generic/MessageHeader/2 schema1 http://ibissource.org/XSD/LifeRetailCB/PolicyJuice/1/GetPolicyDetails/1 schema2] for wsdl [/Validation/Wsdl/GetPolicyDetails/GetPolicyDetails.wsdl]", configuration.getConfigurationWarnings().getFirst());
		ConfigurationWarnings.getInstance().setActiveConfiguration(null);
	}
}

