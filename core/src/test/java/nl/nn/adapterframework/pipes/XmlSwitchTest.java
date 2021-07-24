package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class XmlSwitchTest extends PipeTestBase<XmlSwitch> {

	@Override
	public XmlSwitch createPipe() {
		return new XmlSwitch();
	}

	public void testSwitch(String input, String expectedForwardName) throws Exception {
		log.debug("inputfile ["+input+"]");
		configureAdapter();
		PipeRunResult prr = doPipe(pipe,input,session);

		String result = Message.asString(prr.getResult());
		
		assertEquals(input,result.trim());
		
		PipeForward forward=prr.getPipeForward();
		assertNotNull(forward);
		
		String actualForwardName=forward.getName();
		assertEquals(expectedForwardName,actualForwardName);
	}

	@Test
	public void basic() throws Exception {
		pipe.registerForward(new PipeForward("Envelope","Envelope-Path"));
		String input=TestFileUtils.getTestFile("/XmlSwitch/in.xml");
		testSwitch(input,"Envelope");
	}

	@Test
	public void basicXpath1() throws Exception {
		pipe.registerForward(new PipeForward("Envelope","Envelope-Path"));
		pipe.setXpathExpression("name(/node()[position()=last()])");
//		pipe.setXslt2(true);
		String input=TestFileUtils.getTestFile("/XmlSwitch/in.xml");
		testSwitch(input,"Envelope");
	}

	@Test
	public void basicXpath3() throws Exception {
		pipe.registerForward(new PipeForward("Envelope","Envelope-Path"));
		pipe.registerForward(new PipeForward("SetRequest","SetRequest-Path"));
		pipe.setXpathExpression("name(/Envelope/Body/*[name()!='MessageHeader'])");
		pipe.setNamespaceAware(false);
		String input=TestFileUtils.getTestFile("/XmlSwitch/in.xml");
		testSwitch(input,"SetRequest");
	}

	@Test
	public void xPathFromParameter() throws Exception {
		pipe.registerForward(new PipeForward("1","Path1"));
		pipe.registerForward(new PipeForward("2","Path2"));

		String input=TestFileUtils.getTestFile("/XmlSwitch/in.xml");
		Parameter inputParameter = new Parameter();
		inputParameter.setName("source");
		inputParameter.setValue(input);
		inputParameter.setType("domdoc");
		inputParameter.setRemoveNamespaces(true);
		
		pipe.addParameter(inputParameter);
		pipe.setXsltVersion(1);
		pipe.setXpathExpression("$source/Envelope/Body/SetRequest/CaseData/CASE_ID");
		pipe.setNamespaceAware(false);
		testSwitch("<dummy name=\"input\"/>","2");
	}

	@Test
	public void xPathFromParameterWildCardNamespaced() throws Exception {
		pipe.registerForward(new PipeForward("1","Path1"));
		pipe.registerForward(new PipeForward("2","Path2"));

		String input=TestFileUtils.getTestFile("/XmlSwitch/in.xml");
		Parameter inputParameter = new Parameter();
		inputParameter.setName("source");
		inputParameter.setValue(input);
		inputParameter.setType("domdoc");
		pipe.addParameter(inputParameter);
		pipe.setXsltVersion(1);
		pipe.setXpathExpression("$source/*:Envelope/*:Body/*:SetRequest/*:CaseData/*:CASE_ID");
		pipe.setNamespaceAware(false);
		testSwitch("<dummy name=\"input\"/>","2");
	}

	@Test
	public void xPathFromParameterNamespaced() throws Exception {
		pipe.registerForward(new PipeForward("1","Path1"));
		pipe.registerForward(new PipeForward("2","Path2"));

		String input=TestFileUtils.getTestFile("/XmlSwitch/in.xml");
		Parameter inputParameter = new Parameter();
		inputParameter.setName("source");
		inputParameter.setValue(input);
		inputParameter.setType("domdoc");
		pipe.addParameter(inputParameter);
		pipe.setXpathExpression("$source/soap:Envelope/soap:Body/case:SetRequest/case:CaseData/case:CASE_ID");
		pipe.setNamespaceDefs("soap=http://schemas.xmlsoap.org/soap/envelope/,case=http://www.ing.com/nl/pcretail/ts/migrationcasedata_01");

		testSwitch("<dummy name=\"input\"/>","2");
	}
	
	@Test
	public void withSessionKey() throws Exception {
		pipe.registerForward(new PipeForward("Envelope","Envelope-Path"));
		pipe.registerForward(new PipeForward("selectValue","SelectValue-Path"));
		pipe.setSessionKey("selectKey");
		session=new PipeLineSession();
		session.put("selectKey", "selectValue");
		String input=TestFileUtils.getTestFile("/XmlSwitch/in.xml");
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
		String input=TestFileUtils.getTestFile("/XmlSwitch/in.xml");
		testSwitch(input,"selectValue");
		assertEquals("selectValue", session.get(forwardName).toString());
	}

	@Test
	public void basicSelectionWithStylesheet() throws Exception {
		pipe.registerForward(new PipeForward("Envelope","Envelope-Path"));
		pipe.registerForward(new PipeForward("SetRequest","SetRequest-Path"));
		pipe.setStyleSheetName("/XmlSwitch/selection.xsl");
		pipe.setNamespaceAware(false);
		String input=TestFileUtils.getTestFile("/XmlSwitch/in.xml");
		testSwitch(input,"SetRequest");
	}


}
