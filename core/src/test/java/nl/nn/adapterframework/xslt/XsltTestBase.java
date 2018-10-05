package nl.nn.adapterframework.xslt;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.mockito.Mock;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.pipes.PipeTestBase;

public abstract class XsltTestBase<P extends IPipe> extends PipeTestBase<P> {

	@Mock
	private IPipeLineSession session = new PipeLineSessionBase();

	protected abstract void setStyleSheetName(String styleSheetName);
	protected abstract void setXslt2(boolean xslt2);
	
	@Test
	public void basic() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		setStyleSheetName("/Xslt3/orgchart.xslt");
		setXslt2(true);
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
