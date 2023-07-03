package nl.nn.adapterframework.pipes;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.validation.ValidatorTestBase;
import nl.nn.adapterframework.validation.XmlValidatorContentHandler;
import nl.nn.adapterframework.validation.XmlValidatorException;


/**
  * @author Michiel Meeuwissen
 */

public class WsdlXmlValidatorTest extends PipeTestBase<WsdlXmlValidator> {
	private static final String SIMPLE					= ValidatorTestBase.BASE_DIR_VALIDATION+"/Wsdl/SimpleWsdl/simple.wsdl";
	private static final String SIMPLE_WITH_INCLUDE		= ValidatorTestBase.BASE_DIR_VALIDATION+"/Wsdl/SimpleWsdl/simple_withinclude.wsdl";
	private static final String SIMPLE_WITH_REFERENCE 	= ValidatorTestBase.BASE_DIR_VALIDATION+"/Wsdl/SimpleWsdl/simple_withreference.wsdl";
	private static final String TIBCO					= ValidatorTestBase.BASE_DIR_VALIDATION+"/Tibco/wsdl/BankingCustomer_01_GetPartyBasicDataBanking_01_concrete1.wsdl";
	private static final String DOUBLE_BODY				= ValidatorTestBase.BASE_DIR_VALIDATION+"/Wsdl/GetPolicyDetails/GetPolicyDetailsDoubleBody.wsdl";
	private static final String BASIC					= ValidatorTestBase.BASE_DIR_VALIDATION+"/Wsdl/GetPolicyDetails/GetPolicyDetails.wsdl";
	private static final String SIVTR					= ValidatorTestBase.BASE_DIR_VALIDATION+"/Wsdl/IgnoreImport/StartIncomingValueTransferProcess_1.wsdl";
	private static final String SIVTRX					= ValidatorTestBase.BASE_DIR_VALIDATION+"/Wsdl/IgnoreImport/StartIncomingValueTransferProcess_1x.wsdl";
	private static final String MULTIPLE_OPERATIONS		= ValidatorTestBase.BASE_DIR_VALIDATION+"/Wsdl/multipleOperations.wsdl";

	@Override
	public WsdlXmlValidator createPipe() {
		return new WsdlXmlValidator();
	}

	@Test
	public void wsdlValidate() throws Exception {
		WsdlXmlValidator val = pipe;
		val.setWsdl(SIMPLE);
		val.setSoapBody("TradePriceRequest");
		val.setThrowException(true);
		val.registerForward(new PipeForward("success", null));
		val.configure();
		val.start();
		val.validate("<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\"><Body><TradePriceRequest xmlns=\"http://example.com/stockquote.xsd\"><tickerSymbol>foo</tickerSymbol></TradePriceRequest></Body></Envelope>", session);
	}

	@Test
	public void wsdlValidateWithInclude() throws Exception {
		WsdlXmlValidator val = pipe;
		val.setWsdl(SIMPLE_WITH_INCLUDE);
		val.setSoapBody("TradePriceRequest");
		val.setThrowException(true);
		val.registerForward(new PipeForward("success", null));
		val.configure();
		val.start();
		val.validate("<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\"><Body><TradePriceRequest xmlns=\"http://example.com/stockquote.xsd\"><tickerSymbol>foo</tickerSymbol></TradePriceRequest></Body></Envelope>", session);
	}

	@Test // when a Soap Fault is returned, it should also pass the validator. It's a XML native element, but has to be supplied as body regardless.
	public void testSoapFaultResponse() throws Exception {
		WsdlXmlValidator val = pipe;
		val.setWsdl(SIMPLE);
		val.setSoapBody("TradePriceRequest,TradePrice,Fault");
		val.setTargetNamespace("");
		val.setThrowException(true);
		val.registerForward(new PipeForward("success", null));
		val.configure();
		val.start();

		String soapFault = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
				+ "<soapenv:Body>"
				+ "  <soapenv:Fault>\n"
				+ "    <faultcode>soapenv:Server</faultcode>\n"
				+ "    <faultstring>test</faultstring>\n"
				+ "  </soapenv:Fault>\n"
				+ "</soapenv:Body>"
				+ "</soapenv:Envelope>";
		val.validate(soapFault, session);
	}

	@Test
	public void testXpath() {
		XmlValidatorContentHandler handler = new XmlValidatorContentHandler(null, null, null, null);
		List<String> path = new ArrayList<>();
		assertEquals("/", handler.getXpath(path));
		path.add("soap");
		assertEquals("/soap", handler.getXpath(path));
		path.add("element");
		assertEquals("/soap/element", handler.getXpath(path));
	}

	@Test
	public void testSoapInputBodyFromSoapAction() throws Exception {
		WsdlXmlValidator val = pipe;
		val.setWsdl(MULTIPLE_OPERATIONS);
		val.setThrowException(true);
		val.registerForward(new PipeForward("success", null));
		session.put("SOAPAction", "add");
		val.configure();
		val.start();
		val.validate("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:impl=\"http://test.example.com\">\n"
				+ "	<s:Header/>\n"
				+ "	<s:Body>\n"
				+ "		<impl:add>\n"
				+ "			<impl:numA>3.14</impl:numA>\n"
				+ "			<impl:numB>3.14</impl:numB>\n"
				+ "		</impl:add>\n"
				+ "	</s:Body>\n"
				+ "</s:Envelope>", session);

		session.put("SOAPAction", "sub");
		val.validate("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:impl=\"http://test.example.com\">\n"
				+ "	<s:Header/>\n"
				+ "	<s:Body>\n"
				+ "		<impl:sub>\n"
				+ "			<impl:numA>3.14</impl:numA>\n"
				+ "			<impl:numC>3.14</impl:numC>\n"
				+ "		</impl:sub>\n"
				+ "	</s:Body>\n"
				+ "</s:Envelope>", session);
	}

	@Test
	public void testSoapOutputBodyFromSoapAction() throws Exception {
		WsdlXmlValidator val = pipe;
		val.setWsdl(MULTIPLE_OPERATIONS);
		val.setThrowException(true);
		val.registerForward(new PipeForward("success", null));
		session.put("SOAPAction", "add");
		val.configure();
		val.start();
		val.validate(Message.asMessage("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:impl=\"http://test.example.com\">\n"
				+ "	<s:Header/>\n"
				+ "	<s:Body>\n"
				+ "		<impl:addResponse>\n"
				+ "			<impl:addReturn>3.14</impl:addReturn>\n"
				+ "		</impl:addResponse>\n"
				+ "	</s:Body>\n"
				+ "</s:Envelope>"), session, true, null);

		session.put("SOAPAction", "sub");
		val.validate(Message.asMessage("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:impl=\"http://test.example.com\">\n"
				+ "	<s:Header/>\n"
				+ "	<s:Body>\n"
				+ "		<impl:subResponse>\n"
				+ "			<impl:subReturn>3.14</impl:subReturn>\n"
				+ "		</impl:subResponse>\n"
				+ "	</s:Body>\n"
				+ "</s:Envelope>"), session, true, null);

		val.validate(Message.asMessage("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:impl=\"http://test.example.com\">\n"
				+ "	<s:Header/>\n"
				+ "	<s:Body>\n"
				+ "		<impl:addResponse>\n"
				+ "			<impl:addReturn>3.14</impl:addReturn>\n"
				+ "		</impl:addResponse>\n"
				+ "	</s:Body>\n"
				+ "</s:Envelope>"), session, true, null);
	}

	@Test(expected = XmlValidatorException.class)
	public void testSoapBodyFromSoapActionWithoutSoapAction() throws Exception {
		WsdlXmlValidator val = pipe;
		val.setWsdl(MULTIPLE_OPERATIONS);
		val.setThrowException(true);
		val.registerForward(new PipeForward("success", null));
		val.configure();
		val.start();
		val.validate("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:impl=\"http://test.example.com\">\n"
				+ "	<s:Header/>\n"
				+ "	<s:Body>\n"
				+ "		<impl:add>\n"
				+ "			<impl:numA>3.14</impl:numA>\n"
				+ "			<impl:numB>3.14</impl:numB>\n"
				+ "		</impl:add>\n"
				+ "	</s:Body>\n"
				+ "</s:Envelope>", session);
	}

	@Test
	public void wsdlValidateWithReference() throws Exception {
		WsdlXmlValidator val = pipe;
		val.setWsdl(SIMPLE_WITH_REFERENCE);
		val.setSoapBody("TradePriceRequest");
		val.setThrowException(true);
		val.registerForward(new PipeForward("success", null));
		val.configure();
		val.start();
		val.validate("<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\"><Body><TradePriceRequest xmlns=\"http://example.com/stockquote.xsd\"><tickerSymbol>foo</tickerSymbol></TradePriceRequest></Body></Envelope>", session);
	}

	@Test(expected = XmlValidatorException.class)
	public void wsdlValidateWithReferenceFail() throws Exception {
		WsdlXmlValidator val = pipe;
		val.setWsdl(SIMPLE_WITH_REFERENCE);
		val.setThrowException(true);
		val.registerForward(new PipeForward("success", null));
		val.configure();
		val.start();
		val.validate("<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\"><Body><TradePriceRequest xmlns=\"http://example.com/stockquote.xsd\"><tickerSymbolERROR>foo</tickerSymbolERROR></TradePriceRequest></Body></Envelope>", session);
	}

	@Test
	public void wsdlTibco() throws Exception {
		WsdlXmlValidator val = pipe;
		val.setWsdl(TIBCO);
		val.setSoapHeader("MessageHeader");
		val.setSoapBody("Request");
		val.setThrowException(true);
		val.registerForward(new PipeForward("success", null));
		val.configure();
		val.start();
		val.validate("<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
				"  <Header>\n" +
				"	<MessageHeader xmlns=\"http://www.ing.com/CSP/XSD/General/Message_2\">\n" +
				"	  <From>\n" +
				"		<Id>Ibis4Toegang</Id>\n" +
				"	  </From>\n" +
				"	  <HeaderFields>\n" +
				"		<ConversationId/>\n" +
				"		<MessageId>WPNLD8921975_0a4ac029-7747a1ed_12da7d4b033_-7ff3</MessageId>\n" +
				"		<ExternalRefToMessageId/>\n" +
				"		<Timestamp>2001-12-17T09:30:47</Timestamp>\n" +
				"	  </HeaderFields>\n" +
				"	</MessageHeader>\n" +
				"  </Header>\n" +
				"  <Body>\n" +
				"	<Request xmlns=\"http://www.ing.com/nl/banking/coe/xsd/bankingcustomer_generate_01/getpartybasicdatabanking_01\">\n" +
				"	  <BankSparen xmlns=\"http://www.ing.com/bis/xsd/nl/banking/bankingcustomer_generate_01_getpartybasicdatabanking_request_01\">\n" +
				"		<PRD>\n" +
				"		  <KLT>\n" +
				"			<KLT_NA_RELNUM>181373377001</KLT_NA_RELNUM>\n" +
				"		  </KLT>\n" +
				"		</PRD>\n" +
				"	  </BankSparen>\n" +
				"	</Request>\n" +
				"  </Body>\n" +
				"</Envelope>\n" +
				"", session);
	}

	@Test(expected = XmlValidatorException.class)
	public void wsdlTibcoFailEnvelop() throws Exception {
		WsdlXmlValidator val = pipe;
		val.setWsdl(TIBCO);
		val.setThrowException(true);
		val.registerForward(new PipeForward("success", null));
		val.configure();
		val.start();
		val.validate("<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
				"  <BodyERROR>\n" +
				"	<MessageHeader xmlns=\"http://www.ing.com/CSP/XSD/General/Message_2\">\n" +
				"	  <From>\n" +
				"		<Id>Ibis4Toegang</Id>\n" +
				"	  </From>\n" +
				"	  <HeaderFields>\n" +
				"		<ConversationId/>\n" +
				"		<MessageId>WPNLD8921975_0a4ac029-7747a1ed_12da7d4b033_-7ff3</MessageId>\n" +
				"		<ExternalRefToMessageId/>\n" +
				"		<Timestamp>2001-12-17T09:30:47</Timestamp>\n" +
				"	  </HeaderFields>\n" +
				"	</MessageHeader>\n" +
				"	<Request xmlns=\"http://www.ing.com/nl/banking/coe/xsd/bankingcustomer_generate_01/getpartybasicdatabanking_01\">\n" +
				"	  <BankSparen xmlns=\"http://www.ing.com/bis/xsd/nl/banking/bankingcustomer_generate_01_getpartybasicdatabanking_request_01\">\n" +
				"		<PRD>\n" +
				"		  <KLT>\n" +
				"			<KLT_NA_RELNUM>181373377001</KLT_NA_RELNUM>\n" +
				"		  </KLT>\n" +
				"		</PRD>\n" +
				"	  </BankSparen>\n" +
				"	</Request>\n" +
				"  </BodyERROR>\n" +
				"</Envelope>\n" +
				"", session);
	}

	@Test(expected = XmlValidatorException.class)
	public void wsdlTibcoFailMessage() throws Exception {
		WsdlXmlValidator val = pipe;
		val.setWsdl(TIBCO);
		val.setThrowException(true);
		val.registerForward(new PipeForward("success", null));
		val.configure();
		val.start();
		val.validate("<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
				"  <Body>\n" +
				"	<MessageHeader xmlns=\"http://www.ing.com/CSP/XSD/General/Message_2\">\n" +
				"	  <From>\n" +
				"		<Id>Ibis4Toegang</Id>\n" +
				"	  </From>\n" +
				"	  <HeaderFields>\n" +
				"		<ConversationId/>\n" +
				"		<MessageId>WPNLD8921975_0a4ac029-7747a1ed_12da7d4b033_-7ff3</MessageId>\n" +
				"		<ExternalRefToMessageId/>\n" +
				"		<Timestamp>2001-12-17T09:30:47</Timestamp>\n" +
				"	  </HeaderFields>\n" +
				"	</MessageHeader>\n" +
				"	<Request xmlns=\"http://www.ing.com/nl/banking/coe/xsd/bankingcustomer_generate_01/getpartybasicdatabanking_01\">\n" +
				"	  <BankSparen xmlns=\"http://www.ing.com/bis/xsd/nl/banking/bankingcustomer_generate_01_getpartybasicdatabanking_request_01\">\n" +
				"		<PRD>\n" +
				"		  <KLTERROR>\n" +
				"			<KLT_NA_RELNUM>181373377001</KLT_NA_RELNUM>\n" +
				"		  </KLTERROR>\n" +
				"		</PRD>\n" +
				"	  </BankSparen>\n" +
				"	</Request>\n" +
				"  </Body>\n" +
				"</Envelope>\n" +
				"", session);
	}

	@Ignore("Travis has problems with this")
	@Test
	public void wsdlReasonSessionKey() throws Exception {
		WsdlXmlValidator val = pipe;
		val.setWsdl(SIMPLE);
		val.setSoapBody("TradePriceRequest");
		val.setForwardFailureToSuccess(true);
		val.registerForward(new PipeForward("success", null));
		val.configure();
		val.start();
		PipeLineSession pls = new PipeLineSession();
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
	public void wSoapBodyExistsMultipleTimes() throws Exception {
		WsdlXmlValidator val = pipe;
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
	public void warnSchemaLocationAlreadyDefaultValue() throws Exception {
		pipe.setWsdl(BASIC);
		pipe.setSoapHeader("MessageHeader");
		pipe.setSoapBody("GetPolicyDetails_Request");
		pipe.setAddNamespaceToSchema(true);
		pipe.setSchemaLocation("http://ibissource.org/XSD/LifeRetailCB/PolicyJuice/1/GetPolicyDetails/1 schema2 http://ibissource.org/XSD/Generic/MessageHeader/2 schema1 ");
		pipe.setThrowException(true);
		pipe.registerForward(new PipeForward("success", null));
		configureAndStartPipe();

		assertEquals(1, getConfigurationWarnings().size());
		assertEquals("WsdlXmlValidator [WsdlXmlValidator under test] attribute [schemaLocation] for wsdl [/Validation/Wsdl/GetPolicyDetails/GetPolicyDetails.wsdl] already has a "
				+ "default value [http://ibissource.org/XSD/Generic/MessageHeader/2 schema1 http://ibissource.org/XSD/LifeRetailCB/PolicyJuice/1/GetPolicyDetails/1 schema2]",
				getConfigurationWarnings().get(0));
	}

	@Test
	public void warnUseSoapBodyNameSpace() throws Exception {
		pipe.setWsdl(BASIC);
		pipe.setSoapHeader("MessageHeader");
		pipe.setSoapBody("GetPolicyDetails_Request");
		pipe.setAddNamespaceToSchema(true);
		pipe.setSchemaLocation("http://ibissource.org/XSD/Generic/MessageHeader/2 schema1 http://ibissource.org/XSD/LifeRetailCB/PolicyJuice/1/GetPolicyDetails/2 schema2");
		pipe.setThrowException(true);
		pipe.registerForward(new PipeForward("success", null));
		configureAndStartPipe();

		assertEquals(1, getConfigurationWarnings().size());
		assertEquals("WsdlXmlValidator [WsdlXmlValidator under test] use attribute [soapBodyNamespace] instead of attribute [schemaLocation] with value [http://ibissource.org/XSD/Generic/MessageHeader/2 schema1"
				+ " http://ibissource.org/XSD/LifeRetailCB/PolicyJuice/1/GetPolicyDetails/1 schema2] for wsdl [/Validation/Wsdl/GetPolicyDetails/GetPolicyDetails.wsdl]",
				getConfigurationWarnings().get(0));
	}

	@Test
	public void warnSetAddNamespaceToSchemaTrue() throws Exception {
		pipe.setWsdl(SIMPLE);
		pipe.setSoapBody("TradePriceRequest");
		pipe.setSchemaLocation("dummy schema1");
		pipe.setThrowException(true);
		pipe.registerForward(new PipeForward("success", null));
		configureAndStartPipe();

		assertEquals(1, getConfigurationWarnings().size());
		assertEquals("WsdlXmlValidator [WsdlXmlValidator under test] attribute [schemaLocation] for wsdl [/Validation/Wsdl/SimpleWsdl/simple.wsdl]"
				+ " should only be set when addNamespaceToSchema=true",
				getConfigurationWarnings().get(0));

		pipe.validate("<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\"><Body><TradePriceRequest xmlns=\"http://example.com/stockquote.xsd\"><tickerSymbol>foo</tickerSymbol></TradePriceRequest></Body></Envelope>", session);
	}

	@Test // copied from iaf-test /WsdlXmlValidator/scenario02a
	public void testImportedNamespacesToIgnoreOK() throws Exception {
		pipe.setWsdl(SIVTR);
		pipe.setSoapHeader("MessageHeader");
		pipe.setSoapBody("StartIncomingValueTransferRequest");
		pipe.setImportedNamespacesToIgnore("http://nn.nl/XSD/PensionsSMB/ValueTransfer/ValueTransferLegacy/1/StartIncomingValueTransferProcess/1");
		pipe.setThrowException(true);
		pipe.registerForward(new PipeForward("success", null));
		configureAndStartPipe();

		String input = TestFileUtils.getTestFile(ValidatorTestBase.BASE_DIR_VALIDATION+"/Wsdl/IgnoreImport/in-ok.xml");
		PipeForward forward = pipe.validate(input, session);

		assertEquals("success", forward.getName());
	}

	@Test // copied from iaf-test /WsdlXmlValidator/scenario02b
	public void testImportedNamespacesToIgnoreErr() throws Exception {
		pipe.setWsdl(SIVTR);
		pipe.setSoapHeader("MessageHeader");
		pipe.setSoapBody("StartIncomingValueTransferRequest");
		pipe.setImportedNamespacesToIgnore("http://nn.nl/XSD/PensionsSMB/ValueTransfer/ValueTransferLegacy/1/StartIncomingValueTransferProcess/1");
		pipe.setThrowException(true);
		pipe.registerForward(new PipeForward("success", null));
		configureAndStartPipe();

		String input = TestFileUtils.getTestFile(ValidatorTestBase.BASE_DIR_VALIDATION+"/Wsdl/IgnoreImport/in-err.xml");
		Exception e = assertThrows(Exception.class, ()->pipe.validate(input, session));

		assertThat(e.getMessage(), containsString("Invalid content was found starting with element"));
		assertThat(e.getMessage(), containsString("CountryKode"));
	}

	@Test // copied from iaf-test /WsdlXmlValidator/scenario03a
	public void testImportedSchemaLocationsToIgnoreOK() throws Exception {
		pipe.setWsdl(SIVTRX);
		pipe.setSoapHeader("MessageHeader");
		pipe.setSoapBody("StartIncomingValueTransferRequest");
		pipe.setImportedSchemaLocationsToIgnore("schema1.xsd");
		pipe.setThrowException(true);
		pipe.registerForward(new PipeForward("success", null));
		configureAndStartPipe();

		String input = TestFileUtils.getTestFile(ValidatorTestBase.BASE_DIR_VALIDATION+"/Wsdl/IgnoreImport/in-ok.xml");
		PipeForward forward = pipe.validate(input, session);

		assertEquals("success", forward.getName());
	}

	@Test // copied from iaf-test /WsdlXmlValidator/scenario03b
	public void testImportedSchemaLocationsToIgnoreErr() throws Exception {
		pipe.setWsdl(SIVTRX);
		pipe.setSoapHeader("MessageHeader");
		pipe.setSoapBody("StartIncomingValueTransferRequest");
		pipe.setImportedSchemaLocationsToIgnore("schema1.xsd");
		pipe.setThrowException(true);
		pipe.registerForward(new PipeForward("success", null));
		configureAndStartPipe();

		String input = TestFileUtils.getTestFile(ValidatorTestBase.BASE_DIR_VALIDATION+"/Wsdl/IgnoreImport/in-err.xml");
		Exception e = assertThrows(Exception.class, ()->pipe.validate(input, session));

		assertThat(e.getMessage(), containsString("Invalid content was found starting with element"));
		assertThat(e.getMessage(), containsString("CountryKode"));
	}

}

