package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.mockito.Mock;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class XsltPipeTest extends PipeTestBase<XsltPipe> {

	@Mock
	private IPipeLineSession session;

	@Override
	public XsltPipe createPipe() {
		return new XsltPipe();
	}

	
	@Test
	public void basic() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setStyleSheetName("/Xslt3/orgchart.xslt");
		pipe.setXslt2(true);
		pipe.configure();
		pipe.start();
		String input=TestFileUtils.getTestFile("/Xslt3/employees.xml");
		log.debug("inputfile ["+input+"]");
		String expected=TestFileUtils.getTestFile("/Xslt3/orgchart.xml");
		PipeRunResult prr = pipe.doPipe(input,session);
		String xmlOut=(String)prr.getResult();
		assertEquals(expected,xmlOut.trim());
	}

	@Test
	public void testDynamicStylesheet() throws ConfigurationException, IOException, PipeRunException, PipeStartException {
		pipe.setStyleSheetName("/Xslt/dynamicStylesheet/wrongDummy.xsl");
		pipe.configure();
		pipe.start();

		session = new PipeLineSessionBase();
		session.put("stylesheetLocation", "/Xslt/dynamicStylesheet/correctDummy.xsl");
		pipe.setStyleSheetLocationSessionKey("stylesheetLocation");

		String input = TestFileUtils.getTestFile("/Xslt/dynamicStylesheet/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected = TestFileUtils.getTestFile("/Xslt/dynamicStylesheet/out.txt");
		
		assertEquals(expected, pipe.doPipe(input, session).getResult().toString());
	}
}
