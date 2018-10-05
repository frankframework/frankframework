package nl.nn.adapterframework.xslt;

import org.mockito.Mock;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.pipes.XsltPipe;

public class XsltPipeTest extends XsltErrorTestBase<XsltPipe> {

	@Mock
	private IPipeLineSession session;

	@Override
	public XsltPipe createPipe() {
		return new XsltPipe();
	}

	@Override
	protected void setStyleSheetName(String styleSheetName) {
		pipe.setStyleSheetName(styleSheetName);
	}

	@Override
	protected void setXslt2(boolean xslt2) {
		pipe.setXslt2(xslt2);
	}


}
