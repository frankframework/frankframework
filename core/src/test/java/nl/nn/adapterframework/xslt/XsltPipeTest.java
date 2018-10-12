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
	protected void setOmitXmlDeclaration(boolean omitXmlDeclaration) {
		pipe.setOmitXmlDeclaration(omitXmlDeclaration);
	}

	@Override
	protected void setIndent(boolean indent) {
		pipe.setIndentXml(indent);
	}

	@Override
	protected void setSkipEmptyTags(boolean skipEmptyTags) {
		pipe.setSkipEmptyTags(skipEmptyTags);
	}

	@Override
	protected void setRemoveNamespaces(boolean removeNamespaces) {
		pipe.setRemoveNamespaces(removeNamespaces);
	}

	@Override
	protected void setXslt2(boolean xslt2) {
		pipe.setXslt2(xslt2);
	}


}
