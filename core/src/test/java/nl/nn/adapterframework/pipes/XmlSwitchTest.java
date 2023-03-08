package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Assert;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.Parameter.ParameterType;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.ParameterBuilder;
import nl.nn.adapterframework.testutil.TestFileUtils;

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
	public void basic() throws Exception {
		pipe.registerForward(new PipeForward("Envelope","Envelope-Path"));
		Message input=TestFileUtils.getTestFileMessage("/XmlSwitch/in.xml");
		testSwitch(input,"Envelope");
	}

	@Test
	public void basicXpath1() throws Exception {
		pipe.registerForward(new PipeForward("Envelope","Envelope-Path"));
		pipe.setXpathExpression("name(/node()[position()=last()])");
		Message input=TestFileUtils.getTestFileMessage("/XmlSwitch/in.xml");
		testSwitch(input,"Envelope");
	}

	@Test
	public void basicXpath3() throws Exception {
		pipe.registerForward(new PipeForward("Envelope","Envelope-Path"));
		pipe.registerForward(new PipeForward("SetRequest","SetRequest-Path"));
		pipe.setXpathExpression("name(/Envelope/Body/*[name()!='MessageHeader'])");
		pipe.setNamespaceAware(false);
		Message input=TestFileUtils.getTestFileMessage("/XmlSwitch/in.xml");
		testSwitch(input,"SetRequest");
	}

	@Test
	public void xPathFromParameter() throws Exception {
		pipe.registerForward(new PipeForward("1","Path1"));
		pipe.registerForward(new PipeForward("2","Path2"));

		Message input=TestFileUtils.getTestFileMessage("/XmlSwitch/in.xml");
		Parameter inputParameter = new Parameter();
		inputParameter.setName("source");
		inputParameter.setType(ParameterType.DOMDOC);
		inputParameter.setRemoveNamespaces(true);

		pipe.addParameter(inputParameter);
		pipe.setXsltVersion(1);
		pipe.setXpathExpression("$source/Envelope/Body/SetRequest/CaseData/CASE_ID");
		pipe.setNamespaceAware(false);
		testSwitch(input,"2");
	}

	@Test
	public void xPathFromParameterWildCardNamespaced() throws Exception {
		pipe.registerForward(new PipeForward("1","Path1"));
		pipe.registerForward(new PipeForward("2","Path2"));

		Message input=TestFileUtils.getTestFileMessage("/XmlSwitch/in.xml");
		pipe.addParameter(ParameterBuilder.create().withName("source").withType(ParameterType.DOMDOC));

		pipe.setXpathExpression("$source/*:Envelope/*:Body/*:SetRequest/*:CaseData/*:CASE_ID");
		pipe.setNamespaceAware(false);
		testSwitch(input,"2");
	}

	@Test
	public void xPathFromParameterNamespaced() throws Exception {
		pipe.registerForward(new PipeForward("1","Path1"));
		pipe.registerForward(new PipeForward("2","Path2"));

		Message input=TestFileUtils.getTestFileMessage("/XmlSwitch/in.xml");
		pipe.addParameter(ParameterBuilder.create().withName("source").withType(ParameterType.DOMDOC));
		pipe.setXpathExpression("$source/soap:Envelope/soap:Body/case:SetRequest/case:CaseData/case:CASE_ID");
		pipe.setNamespaceDefs("soap=http://schemas.xmlsoap.org/soap/envelope/,case=http://www.ing.com/nl/pcretail/ts/migrationcasedata_01");

		testSwitch(input,"2");
	}

	@Test
	public void testSettingXsltVersion1() throws Exception {
		String message = "<root>2</root>";

		pipe.setXpathExpression("$param1 = 2");
		pipe.setGetInputFromFixedValue("<dummy/>");

		Parameter param1 = new Parameter();
		param1.setName("param1");
		param1.setXpathExpression("/root");
		param1.setSessionKey("originalMessage");
		pipe.addParameter(param1);

		pipe.setXsltVersion(1);
		pipe.registerForward(new PipeForward("true","Envelope-Path"));
		pipe.registerForward(new PipeForward("false","dummy-Path"));

		session=new PipeLineSession();
		session.put(PipeLineSession.originalMessageKey, message);

		testSwitch(new Message("<dummy/>"),"true");
	}

	@Test
	public void testXsltVersionAutoDetect() throws Exception {
		String message = "<root>2</root>";

		pipe.setXpathExpression("$param1 = 2");
		pipe.setGetInputFromFixedValue("<dummy/>");

		Parameter param1 = new Parameter();
		param1.setName("param1");
		param1.setXpathExpression("/root");
		param1.setSessionKey("originalMessage");
		pipe.addParameter(param1);

		pipe.setXsltVersion(0);
		pipe.registerForward(new PipeForward("true","Envelope-Path"));
		pipe.registerForward(new PipeForward("false","dummy-Path"));

		session=new PipeLineSession();
		session.put(PipeLineSession.originalMessageKey, message);

		testSwitch(new Message("<dummy/>"),"true");
	}

	@Test
	public void xPathFromParameterWithXpath() throws Exception {
		pipe.registerForward(new PipeForward("1","Path1"));
		pipe.registerForward(new PipeForward("2","Path2"));

		Message input=TestFileUtils.getTestFileMessage("/XmlSwitch/in.xml");
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
	public void withSessionKey() throws Exception {
		pipe.registerForward(new PipeForward("Envelope","Envelope-Path"));
		pipe.registerForward(new PipeForward("selectValue","SelectValue-Path"));
		pipe.setSessionKey("selectKey");
		session=new PipeLineSession();
		session.put("selectKey", "selectValue");
		Message input=TestFileUtils.getTestFileMessage("/XmlSwitch/in.xml");
		testSwitch(input,"selectValue");
	}

	@Test
	public void storeForwardInSessionKey() throws Exception {
		pipe.registerForward(new PipeForward("Envelope","Envelope-Path"));
		pipe.registerForward(new PipeForward("selectValue","SelectValue-Path"));
		pipe.setSessionKey("selectKey");
		String forwardName = "forwardName";
		pipe.setStoreForwardInSessionKey(forwardName);
		session=new PipeLineSession();
		session.put("selectKey", "selectValue");
		Message input=TestFileUtils.getTestFileMessage("/XmlSwitch/in.xml");
		testSwitch(input,"selectValue");
		assertEquals("selectValue", session.get(forwardName).toString());
	}

	@Test
	public void basicSelectionWithStylesheet() throws Exception {
		pipe.registerForward(new PipeForward("Envelope","Envelope-Path"));
		pipe.registerForward(new PipeForward("SetRequest","SetRequest-Path"));
		pipe.setStyleSheetName("/XmlSwitch/selection.xsl");
		pipe.setNamespaceAware(false);
		Message input=TestFileUtils.getTestFileMessage("/XmlSwitch/in.xml");
		testSwitch(input,"SetRequest");
	}

	@Test
	public void basicSelectionWithStylesheetXslt3() throws Exception {
		pipe.registerForward(new PipeForward("Envelope","Envelope-Path"));
		pipe.registerForward(new PipeForward("SetRequest","SetRequest-Path"));
		pipe.setXsltVersion(3);
		pipe.setStyleSheetName("/XmlSwitch/selectionXslt3.0.xsl");
		pipe.setNamespaceAware(false);
		Message input=TestFileUtils.getTestFileMessage("/XmlSwitch/in.json");
		testSwitch(input,"SetRequest");
	}

	@Test
	public void testForwardNameFromSessionKey() throws Exception {
		pipe.registerForward(new PipeForward("forwardName","Envelope-Path"));
		pipe.setForwardNameSessionKey("forwardNameSessionKey");
		session=new PipeLineSession();
		session.put("forwardNameSessionKey", "forwardName");
		Message input=TestFileUtils.getTestFileMessage("/XmlSwitch/in.xml");
		testSwitch(input,"forwardName");
	}

	@Test
	public void basicXpathSessionKeyUsedAsInput() throws Exception {
		pipe.registerForward(new PipeForward("Envelope","Envelope-Path"));
		pipe.setSessionKey("sessionKey");
		pipe.setXpathExpression("name(/node()[position()=last()])");
		session=new PipeLineSession();
		Message input=TestFileUtils.getTestFileMessage("/XmlSwitch/in.xml");
		session.put("sessionKey", input);
		testSwitch(new Message("dummy"),"Envelope");
	}

	@Test
	public void emptyParameterList() throws Exception {
		pipe.setSessionKey("sessionKey");
		Assert.assertThrows("cannot find forward or pipe named", PipeRunException.class, () -> testSwitch(new Message("dummy"),"Envelope"));
	}

	@Test
	public void emptyForward() throws Exception {
		pipe.setEmptyForwardName("emptyForward");
		pipe.setSessionKey("sessionKey");
		pipe.registerForward(new PipeForward("emptyForward", "test"));
		testSwitch(new Message("dummy"),"emptyForward");
	}

	@Test
	public void notFoundForward() throws Exception {
		pipe.setNotFoundForwardName("notFound");
		pipe.setSessionKey("sessionKey");
		session=new PipeLineSession();
		session.put("sessionKey", "someForward");
		pipe.registerForward(new PipeForward("notFound", "test"));
		testSwitch(new Message("dummy"),"notFound");
	}

	@Test
	public void withSessionKeyOverridesGetInputFromSessionKey() throws Exception {
		pipe.registerForward(new PipeForward("Envelope","Envelope-Path"));
		pipe.registerForward(new PipeForward("dummy","dummy-Path"));
		pipe.setGetInputFromSessionKey("input");
		pipe.setSessionKey("selectKey");
		pipe.setXpathExpression("name(/node()[position()=last()])");
		session=new PipeLineSession();
		session.put("selectKey", "<dummy/>");
		Message input=TestFileUtils.getTestFileMessage("/XmlSwitch/in.xml");
		session.put("input", input);
		testSwitch(input,"dummy");
	}

	@Test
	public void configureNotFoundForwardNotRegistered() throws Exception {
		pipe.setXpathExpression("name(/node()[position()=last()])");
		pipe.setStyleSheetName("/XmlSwitch/selection.xsl");
		Assert.assertThrows("cannot have both an xpathExpression and a styleSheetName specified", ConfigurationException.class, () -> pipe.configure());
	}

	@Test
	public void styleSheetNotExists() throws Exception {
		pipe.setStyleSheetName("/XmlSwitch/dummy.xsl");
		Assert.assertThrows("cannot find stylesheet", ConfigurationException.class, () -> pipe.configure());
	}
}
