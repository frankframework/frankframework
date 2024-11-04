package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterType;
import org.frankframework.stream.Message;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.XmlParameterBuilder;

public class XmlSwitchTest extends PipeTestBase<XmlSwitch> {


	@Override
	public XmlSwitch createPipe() {
		return new XmlSwitch();
	}

	public void testSwitch(Message input, String expectedForwardName) throws Exception {
		log.debug("inputfile ["+input+"]");
		configureAdapter();
		PipeRunResult prr = doPipe(pipe,input,session);

		assertEquals(input,prr.getResult());

		PipeForward forward=prr.getPipeForward();
		assertNotNull(forward);

		String actualForwardName=forward.getName();
		assertEquals(expectedForwardName,actualForwardName);
	}

	@Test
	void basic() throws Exception {
		pipe.addForward(new PipeForward("Envelope","Envelope-Path"));
		Message input=MessageTestUtils.getMessage("/XmlSwitch/in.xml");
		testSwitch(input,"Envelope");
	}

	@Test
	void basicXpath1() throws Exception {
		pipe.addForward(new PipeForward("Envelope","Envelope-Path"));
		pipe.setXpathExpression("name(/node()[position()=last()])");
		Message input=MessageTestUtils.getMessage("/XmlSwitch/in.xml");
		testSwitch(input,"Envelope");
	}

	@Test
	void basicXpath3() throws Exception {
		pipe.addForward(new PipeForward("Envelope","Envelope-Path"));
		pipe.addForward(new PipeForward("SetRequest","SetRequest-Path"));
		pipe.setXpathExpression("name(/Envelope/Body/*[name()!='MessageHeader'])");
		pipe.setNamespaceAware(false);
		Message input=MessageTestUtils.getMessage("/XmlSwitch/in.xml");
		testSwitch(input,"SetRequest");
	}

	@Test
	void xPathFromParameter() throws Exception {
		pipe.addForward(new PipeForward("1","Path1"));
		pipe.addForward(new PipeForward("2","Path2"));

		Message input = MessageTestUtils.getMessage("/XmlSwitch/in.xml");

		XmlParameterBuilder inputParameter = XmlParameterBuilder.create()
				.withName("source")
				.withType(ParameterType.DOMDOC);
		inputParameter.setRemoveNamespaces(true);

		pipe.addParameter(inputParameter);
		pipe.setXsltVersion(1);
		pipe.setXpathExpression("$source/Envelope/Body/SetRequest/CaseData/CASE_ID");
		pipe.setNamespaceAware(false);
		testSwitch(input,"2");
	}

	@Test
	void xPathFromParameterWildCardNamespaced() throws Exception {
		pipe.addForward(new PipeForward("1","Path1"));
		pipe.addForward(new PipeForward("2","Path2"));

		Message input = MessageTestUtils.getMessage("/XmlSwitch/in.xml");

		pipe.addParameter(XmlParameterBuilder.create().withName("source").withType(ParameterType.DOMDOC));
		pipe.setXpathExpression("$source/*:Envelope/*:Body/*:SetRequest/*:CaseData/*:CASE_ID");
		pipe.setNamespaceAware(false);

		testSwitch(input,"2");
	}

	@Test
	void xPathFromParameterNamespaced() throws Exception {
		pipe.addForward(new PipeForward("1","Path1"));
		pipe.addForward(new PipeForward("2","Path2"));

		Message input = MessageTestUtils.getMessage("/XmlSwitch/in.xml");

		pipe.addParameter(XmlParameterBuilder.create().withName("source").withType(ParameterType.DOMDOC));
		pipe.setXpathExpression("$source/soap:Envelope/soap:Body/case:SetRequest/case:CaseData/case:CASE_ID");
		pipe.setNamespaceDefs("soap=http://schemas.xmlsoap.org/soap/envelope/,case=http://www.ing.com/nl/pcretail/ts/migrationcasedata_01");

		testSwitch(input,"2");
	}

	@Test
	void testSettingXsltVersion1() throws Exception {
		String message = "<root>2</root>";

		pipe.setXpathExpression("$param1 = 2");
		pipe.setGetInputFromFixedValue("<dummy/>");

		Parameter param1 = new Parameter();
		param1.setName("param1");
		param1.setXpathExpression("/root");
		param1.setSessionKey("originalMessage");
		pipe.addParameter(param1);

		pipe.setXsltVersion(1);
		pipe.addForward(new PipeForward("true","Envelope-Path"));
		pipe.addForward(new PipeForward("false","dummy-Path"));

		session=new PipeLineSession();
		session.put(PipeLineSession.ORIGINAL_MESSAGE_KEY, message);

		testSwitch(new Message("<dummy/>"),"true");
	}

	@Test
	void testXsltVersionAutoDetect() throws Exception {
		String message = "<root>2</root>";

		pipe.setXpathExpression("$param1 = 2");
		pipe.setGetInputFromFixedValue("<dummy/>");

		Parameter param1 = new Parameter();
		param1.setName("param1");
		param1.setXpathExpression("/root");
		param1.setSessionKey("originalMessage");
		pipe.addParameter(param1);

		pipe.setXsltVersion(0);
		pipe.addForward(new PipeForward("true","Envelope-Path"));
		pipe.addForward(new PipeForward("false","dummy-Path"));

		session=new PipeLineSession();
		session.put(PipeLineSession.ORIGINAL_MESSAGE_KEY, message);

		testSwitch(new Message("<dummy/>"),"true");
	}

	@Test
	void xPathFromParameterWithXpath() throws Exception {
		pipe.addForward(new PipeForward("1","Path1"));
		pipe.addForward(new PipeForward("2","Path2"));

		Message input=MessageTestUtils.getMessage("/XmlSwitch/in.xml");
		Parameter inputParameter = new Parameter();
		inputParameter.setName("source");
		inputParameter.setXpathExpression("/soap:Envelope/soap:Body/case:SetRequest/case:CaseData/case:CASE_ID");
		inputParameter.setNamespaceDefs("soap=http://schemas.xmlsoap.org/soap/envelope/,case=http://www.ing.com/nl/pcretail/ts/migrationcasedata_01");
		inputParameter.setType(ParameterType.DOMDOC);
		pipe.addParameter(inputParameter);
		pipe.setXpathExpression("$source");
		pipe.setNamespaceDefs("soap=http://schemas.xmlsoap.org/soap/envelope/,case=http://www.ing.com/nl/pcretail/ts/migrationcasedata_01");
		testSwitch(input,"2");
	}

	@Test
	void withSessionKey() throws Exception {
		pipe.addForward(new PipeForward("Envelope","Envelope-Path"));
		pipe.addForward(new PipeForward("selectValue","SelectValue-Path"));
		pipe.setSessionKey("selectKey");
		session=new PipeLineSession();
		session.put("selectKey", "selectValue");
		Message input=MessageTestUtils.getMessage("/XmlSwitch/in.xml");
		testSwitch(input,"selectValue");
	}

	@Test
	void storeForwardInSessionKey() throws Exception {
		pipe.addForward(new PipeForward("Envelope","Envelope-Path"));
		pipe.addForward(new PipeForward("selectValue","SelectValue-Path"));
		pipe.setSessionKey("selectKey");
		String forwardName = "forwardName";
		pipe.setStoreForwardInSessionKey(forwardName);
		session=new PipeLineSession();
		session.put("selectKey", "selectValue");
		Message input=MessageTestUtils.getMessage("/XmlSwitch/in.xml");
		testSwitch(input,"selectValue");
		assertEquals("selectValue", session.get(forwardName).toString());
	}

	@Test
	void basicSelectionWithStylesheet() throws Exception {
		pipe.addForward(new PipeForward("Envelope","Envelope-Path"));
		pipe.addForward(new PipeForward("SetRequest","SetRequest-Path"));
		pipe.setStyleSheetName("/XmlSwitch/selection.xsl");
		pipe.setNamespaceAware(false);
		Message input=MessageTestUtils.getMessage("/XmlSwitch/in.xml");
		testSwitch(input,"SetRequest");
	}

	@Test
	void basicSelectionWithStylesheetXslt3() throws Exception {
		pipe.addForward(new PipeForward("Envelope","Envelope-Path"));
		pipe.addForward(new PipeForward("SetRequest","SetRequest-Path"));
		pipe.setXsltVersion(3);
		pipe.setStyleSheetName("/XmlSwitch/selectionXslt3.0.xsl");
		pipe.setNamespaceAware(false);
		Message input=MessageTestUtils.getMessage("/XmlSwitch/in.json");
		testSwitch(input,"SetRequest");
	}

	@Test
	void testForwardNameFromSessionKey() throws Exception {
		pipe.addForward(new PipeForward("forwardName","Envelope-Path"));
		pipe.setForwardNameSessionKey("forwardNameSessionKey");
		session=new PipeLineSession();
		session.put("forwardNameSessionKey", "forwardName");
		Message input=MessageTestUtils.getMessage("/XmlSwitch/in.xml");
		testSwitch(input,"forwardName");
	}

	@Test
	void basicXpathSessionKeyUsedAsInput() throws Exception {
		pipe.addForward(new PipeForward("Envelope","Envelope-Path"));
		pipe.setSessionKey("sessionKey");
		pipe.setXpathExpression("name(/node()[position()=last()])");
		session=new PipeLineSession();
		Message input=MessageTestUtils.getMessage("/XmlSwitch/in.xml");
		session.put("sessionKey", input);
		testSwitch(new Message("dummy"),"Envelope");
	}

	@Test
	void emptyParameterList() throws Exception {
		pipe.setSessionKey("sessionKey");
		assertThrows(PipeRunException.class, () -> testSwitch(new Message("dummy"), "Envelope"), "cannot find forward or pipe named");
	}

	@Test
	void emptyForward() throws Exception {
		pipe.setEmptyForwardName("emptyForward");
		pipe.setSessionKey("sessionKey");
		pipe.addForward(new PipeForward("emptyForward", "test"));
		testSwitch(new Message("dummy"),"emptyForward");
	}

	@Test
	void notFoundForward() throws Exception {
		pipe.setNotFoundForwardName("notFound");
		pipe.setSessionKey("sessionKey");
		session=new PipeLineSession();
		session.put("sessionKey", "someForward");
		pipe.addForward(new PipeForward("notFound", "test"));
		testSwitch(new Message("dummy"),"notFound");
	}

	@Test
	void withSessionKeyOverridesGetInputFromSessionKey() throws Exception {
		pipe.addForward(new PipeForward("Envelope","Envelope-Path"));
		pipe.addForward(new PipeForward("dummy","dummy-Path"));
		pipe.setGetInputFromSessionKey("input");
		pipe.setSessionKey("selectKey");
		pipe.setXpathExpression("name(/node()[position()=last()])");
		session=new PipeLineSession();
		session.put("selectKey", "<dummy/>");
		Message input=MessageTestUtils.getMessage("/XmlSwitch/in.xml");
		session.put("input", input);
		testSwitch(input,"dummy");
	}

	@Test
	void configureNotFoundForwardNotRegistered() throws Exception {
		pipe.setXpathExpression("name(/node()[position()=last()])");
		pipe.setStyleSheetName("/XmlSwitch/selection.xsl");
		assertThrows(ConfigurationException.class, () -> pipe.configure(), "cannot have both an xpathExpression and a styleSheetName specified");
	}

	@Test
	void styleSheetNotExists() throws Exception {
		pipe.setStyleSheetName("/XmlSwitch/dummy.xsl");
		assertThrows(ConfigurationException.class, () -> pipe.configure(), "cannot find stylesheet");
	}
}
