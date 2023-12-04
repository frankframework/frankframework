package nl.nn.adapterframework.pipes;


import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.TransformerPoolNamespaceUnawarenessTest;

public class XmlIfNamespaceUnawarenessTest extends PipeTestBase<XmlIf>{

	private final String NAMESPACE_UNAWARENESS_XPATH = TransformerPoolNamespaceUnawarenessTest.NAMESPACELESS_XPATH;
	private final String NAMESPACE_UNAWARENESS_INPUT = TransformerPoolNamespaceUnawarenessTest.NAMESPACED_INPUT_MESSAGE;

	private final String pipeForwardThen = "then";
	private final String pipeForwardElse = "else";

	@Override
	public XmlIf createPipe() {
		XmlIf xmlIf = new XmlIf();

		//Add default pipes
		try {
			xmlIf.registerForward(new PipeForward(pipeForwardThen, null));
			xmlIf.registerForward(new PipeForward(pipeForwardElse, null));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return xmlIf;
	}

	@Test
	public void testNamespaceAwareWithXpath() throws Exception {
		testNamespaceUnawareness(true, pipeForwardThen);
	}

	@Test
	public void testNotNamespaceAwareWithXpath() throws Exception {
		testNamespaceUnawareness(false, pipeForwardThen);
	}


	public void testNamespaceUnawareness(boolean namespaceAware, String expectedForward) throws Exception {
		pipe.setXpathExpression(NAMESPACE_UNAWARENESS_XPATH);
		configureAdapter();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, NAMESPACE_UNAWARENESS_INPUT, session);
		assertEquals(expectedForward, prr.getPipeForward().getName());
	}

	@Test
	public void test3156() throws Exception {
		pipe.setXpathExpression("root/code = 'OK'");
		pipe.setNamespaceAware(false);
		configureAdapter();
		pipe.start();

		String input = "<root xmlns=\"urn:something\"><code>OK</code><body>xxx</body></root>";

		PipeRunResult prr = doPipe(pipe, input, session);
		assertEquals(pipeForwardThen, prr.getPipeForward().getName());
	}

}
