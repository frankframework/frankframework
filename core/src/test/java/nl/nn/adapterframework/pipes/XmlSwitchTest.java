package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;
import org.mockito.Mock;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class XmlSwitchTest extends PipeTestBase<XmlSwitch> {

	@Mock
	private IPipeLineSession session;

	@Override
	public XmlSwitch createPipe() {
		return new XmlSwitch();
	}
	
	public void testSwitch(String input, String expectedForwardName) throws PipeRunException {
		log.debug("inputfile ["+input+"]");
		PipeRunResult prr = pipe.doPipe(input,session);
		String xmlOut=(String)prr.getResult();
		assertEquals(input,xmlOut.trim());
		
		PipeForward forward=prr.getPipeForward();
		assertNotNull(forward);
		
		String actualForwardName=forward.getName();
		assertEquals(expectedForwardName,actualForwardName);
		
	}
	
	@Test
	public void basic() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.registerForward(new PipeForward("Envelope","Envelope-Path"));
		configurePipe();
		pipe.start();
		String input=TestFileUtils.getTestFile("/XmlSwitch/in.xml");
		testSwitch(input,"Envelope");
	}

	@Test
	public void basicXpath1() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.registerForward(new PipeForward("Envelope","Envelope-Path"));
		pipe.setXpathExpression("name(/node()[position()=last()])");
//		pipe.setXslt2(true);
		configurePipe();
		pipe.start();
		String input=TestFileUtils.getTestFile("/XmlSwitch/in.xml");
		testSwitch(input,"Envelope");
	}

	
	@Test
	public void basicXpath3() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.registerForward(new PipeForward("Envelope","Envelope-Path"));
		pipe.registerForward(new PipeForward("SetRequest","SetRequest-Path"));
		pipe.setXpathExpression("name(/Envelope/Body/*[name()!='MessageHeader'])");
		pipe.setNamespaceAware(false);
		configurePipe();
		pipe.start();
		String input=TestFileUtils.getTestFile("/XmlSwitch/in.xml");
		testSwitch(input,"SetRequest");
	}


}
