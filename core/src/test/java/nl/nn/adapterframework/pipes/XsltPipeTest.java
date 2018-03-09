package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.mockito.Mock;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;

public class XsltPipeTest extends PipeTestBase<XsltPipe> {

	@Mock
	private IPipeLineSession session;

	@Override
	public XsltPipe createPipe() {
		return new XsltPipe();
	}

	@Override
	@Test
	public void basicNoAdditionalConfig() throws ConfigurationException {
		exception.expect(ConfigurationException.class);
		super.basicNoAdditionalConfig();
	}
	
	@Test
	public void basic() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setStyleSheetName("/Xslt3/orgchart.xslt");
		pipe.setXslt2(true);
		pipe.configure();
		pipe.start();
		String input=getFile("/Xslt3/employees.xml");
		log.debug("inputfile ["+input+"]");
		String expected=getFile("/Xslt3/orgchart.xml");
		PipeRunResult prr = pipe.doPipe(input,session);
		String xmlOut=(String)prr.getResult();
		assertEquals(expected,xmlOut.trim());
	}

}
