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
import org.frankframework.processors.CorePipeLineProcessor;
import org.frankframework.processors.CorePipeProcessor;
import org.frankframework.processors.InputOutputPipeProcessor;
import org.frankframework.processors.PipeProcessor;
import org.frankframework.stream.Message;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.XmlParameterBuilder;

public class SwitchPipeTest extends PipeTestBase<SwitchPipe> {

	@Override
	public SwitchPipe createPipe() {
		// Correctly chain the pipe processors
		CorePipeLineProcessor pipeLineProcessor = new CorePipeLineProcessor();
		InputOutputPipeProcessor inputOutputPipeProcessor = new InputOutputPipeProcessor();
		PipeProcessor pipeProcessor = new CorePipeProcessor();
		inputOutputPipeProcessor.setPipeProcessor(pipeProcessor);

		pipeLineProcessor.setPipeProcessor(inputOutputPipeProcessor);

		pipeline.setPipeLineProcessor(pipeLineProcessor);

		return new SwitchPipe();
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
		pipe.setForwardNameSessionKey("selectKey");
		session=new PipeLineSession();
		session.put("selectKey", "selectValue");
		Message input=MessageTestUtils.getMessage("/XmlSwitch/in.xml");
		testSwitch(input,"selectValue");
	}

	@Test
	void storeForwardInSessionKey() throws Exception {
		pipe.addForward(new PipeForward("Envelope","Envelope-Path"));
		pipe.addForward(new PipeForward("selectValue","SelectValue-Path"));
		pipe.setForwardNameSessionKey("selectKey");
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
		pipe.addForward(new PipeForward("Envelope", "Envelope-Path"));
		pipe.setGetInputFromSessionKey("sessionKey");
		pipe.setXpathExpression("name(/node()[position()=last()])");

		session = new PipeLineSession();
		Message input = MessageTestUtils.getMessage("/XmlSwitch/in.xml");
		session.put("sessionKey", input);


		// Configure input/output pipe processor to enable getInputFromSessionKey
		InputOutputPipeProcessor ioProcessor = new InputOutputPipeProcessor();
		CorePipeProcessor coreProcessor = new CorePipeProcessor();
		ioProcessor.setPipeProcessor(coreProcessor);

		pipe.configure();

		PipeRunResult prr = ioProcessor.processPipe(pipeline, pipe, new Message("dummy"), session);

		// Expect emptyForward to be returned
		PipeForward forward = prr.getPipeForward();
		assertNotNull(forward);

		String actualForwardName = forward.getName();
		assertEquals("Envelope", actualForwardName);
	}

	@Test
	void emptyParameterList() {
		pipe.setGetInputFromSessionKey("sessionKey");
		assertThrows(PipeRunException.class, () -> testSwitch(new Message("dummy"), "Envelope"), "cannot find forward or pipe named");
	}

	@Test
	void emptyForward() throws Exception {
		pipe.setEmptyForwardName("emptyForward");
		pipe.setGetInputFromSessionKey("sessionKey");
		pipe.addForward(new PipeForward("emptyForward", "test"));
		session.put("sessionKey", "");

		// Configure input/output pipe processor to enable getInputFromSessionKey
		InputOutputPipeProcessor ioProcessor = new InputOutputPipeProcessor();
		CorePipeProcessor coreProcessor = new CorePipeProcessor();
		ioProcessor.setPipeProcessor(coreProcessor);

		pipe.configure();

		PipeRunResult prr = ioProcessor.processPipe(pipeline, pipe, new Message("dummy"), session);

		// Expect emptyForward to be returned
		PipeForward forward = prr.getPipeForward();
		assertNotNull(forward);

		String actualForwardName = forward.getName();
		assertEquals("emptyForward", actualForwardName);
	}

	@Test
	void notFoundForward() throws Exception {
		pipe.setNotFoundForwardName("notFound");
		pipe.setForwardNameSessionKey("sessionKey");
		session=new PipeLineSession();
		session.put("sessionKey", "someForward");
		pipe.addForward(new PipeForward("notFound", "test"));
		testSwitch(new Message("dummy"),"notFound");
	}

	@Test
	void configureNotFoundForwardNotRegistered() {
		pipe.setXpathExpression("name(/node()[position()=last()])");
		pipe.setStyleSheetName("/XmlSwitch/selection.xsl");
		assertThrows(ConfigurationException.class, () -> pipe.configure(), "cannot have both an xpathExpression and a styleSheetName specified");
	}

	@Test
	void styleSheetNotExists() {
		pipe.setStyleSheetName("/XmlSwitch/dummy.xsl");
		assertThrows(ConfigurationException.class, () -> pipe.configure(), "cannot find stylesheet");
	}
}
