package org.frankframework.parameters;

import static org.frankframework.testutil.MatchUtils.assertXmlEquals;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMSource;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.frankframework.core.Adapter;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineExit;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.parameters.XmlParameter.XmlType;
import org.frankframework.pipes.PutInSessionPipe;
import org.frankframework.processors.CorePipeLineProcessor;
import org.frankframework.processors.CorePipeProcessor;
import org.frankframework.stream.Message;
import org.frankframework.stream.UrlMessage;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.XmlUtils;

public class XmlParameterTest {

	@Test
	public void testParameterFromURLToDomdocTypeNoNameSpace() throws Exception {
		testParameterFromURLToDomTypeHelper(ParameterType.DOMDOC, false, Document.class);
	}

	private Transformer createTransformer() throws TransformerConfigurationException {
		return XmlUtils.getTransformerFactory().newTransformer();
	}

	@Test
	public void testParameterFromURLToDomdocTypeRemoveNameSpace() throws Exception {
		testParameterFromURLToDomTypeHelper(ParameterType.DOMDOC, true, Document.class);
	}

	@Test
	public void testParameterFromURLToNodeTypeNoNameSpace() throws Exception {
		testParameterFromURLToDomTypeHelper(ParameterType.NODE, false, Node.class);
	}

	@Test
	public void testParameterFromURLToNodeTypeRemoveNameSpace() throws Exception {
		testParameterFromURLToDomTypeHelper(ParameterType.NODE, true, Node.class);
	}

	public <T> void testParameterFromURLToDomTypeHelper(ParameterType type, boolean removeNamespaces, Class<T> c) throws Exception {
		URL originalMessage = TestFileUtils.getTestFileURL("/Xslt/MultiNamespace/in.xml");

		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", new UrlMessage(originalMessage));

		XmlParameter inputMessage = new XmlParameter();
		inputMessage.setName("InputMessage");
		inputMessage.setSessionKey("originalMessage");
		inputMessage.setType(type);
		inputMessage.setRemoveNamespaces(removeNamespaces);
		inputMessage.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = inputMessage.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(c.isAssignableFrom(result.getClass()), c + " is expected type but was: " + result.getClass());
	}

	@Test
	public void testParameterFromDomToNode() throws Exception {
		Document domdoc = XmlUtils.buildDomDocument("<someValue/>");
		String expectedResultContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><someValue/>";

		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", domdoc);

		XmlParameter parameter = new XmlParameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setType(ParameterType.NODE);
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);
		assertThat(result, instanceOf(Node.class));
		assertThat(result, not(instanceOf(Document.class)));

		String contents = XmlUtils.transformXml(createTransformer(), new DOMSource((Node) result));
		assertEquals(expectedResultContents, contents);
	}

	@Test
	public void testParameterFromNodeToDomdoc() throws Exception {
		Node node = XmlUtils.buildDomDocument("<someValue/>").getFirstChild();
		String expectedResultContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><someValue/>";

		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", node);

		XmlParameter parameter = new XmlParameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setType(ParameterType.DOMDOC);
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);
		assertInstanceOf(Document.class, result);

		String contents = XmlUtils.transformXml(createTransformer(), new DOMSource((Document) result));
		assertEquals(expectedResultContents, contents);
	}

	@Test
	public void testParameterFromBytesToDomdoc() throws Exception {
		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", "<someValue/>".getBytes());
		String expectedResultContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><someValue/>";

		XmlParameter parameter = new XmlParameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setType(ParameterType.DOMDOC);
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);
		assertThat(result, instanceOf(Document.class));

		String contents = XmlUtils.transformXml(createTransformer(), new DOMSource((Document) result));
		assertEquals(expectedResultContents, contents);
	}

	@Test
	public void testParameterFromBytesToNode() throws Exception {
		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", "<someValue/>".getBytes());
		String expectedResultContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><someValue/>";

		XmlParameter parameter = new XmlParameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setType(ParameterType.NODE);
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);
		assertThat(result, instanceOf(Node.class));
		assertThat(result, not(instanceOf(Document.class)));

		String contents = XmlUtils.transformXml(createTransformer(), new DOMSource((Node) result));
		assertEquals(expectedResultContents, contents);
	}

	@Test
	public void testParameterFromURLToDomdocWithXpath() throws Exception {
		URL originalMessage = TestFileUtils.getTestFileURL("/Xslt/MultiNamespace/in.xml");
		String expectedResultContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><block><XDOC><REF_ID>0</REF_ID><XX>0</XX></XDOC><XDOC><REF_ID>1</REF_ID></XDOC><XDOC><REF_ID>2</REF_ID></XDOC></block>";
		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", new UrlMessage(originalMessage));

		XmlParameter parameter = new XmlParameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setRemoveNamespaces(true);
		parameter.setXpathExpression("*");
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);
		Document domDoc = assertInstanceOf(Document.class, result);

		String contents = XmlUtils.transformXml(createTransformer(), new DOMSource(domDoc));
		assertEquals(expectedResultContents, contents);
	}

	@Test
	public void testParameterFromNodeToNode() throws Exception {
		Node node = XmlUtils.buildDomDocument("<someValue/>").getFirstChild();
		String expectedResultContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><someValue/>";

		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", node);

		XmlParameter parameter = new XmlParameter();
		parameter.setXmlType(XmlType.NODE);
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);

		assertInstanceOf(Node.class, result);
		assertThat(result, not(instanceOf(Document.class)));

		String contents = XmlUtils.transformXml(createTransformer(), new DOMSource((Node) result));
		assertEquals(expectedResultContents, contents);

		assertFalse(parameter.requiresInputValueForResolution());
		assertTrue(parameter.consumesSessionVariable("originalMessage"));
	}

	@Test
	public void testParameterFromURLToNodeWithXpath() throws Exception {
		URL originalMessage = TestFileUtils.getTestFileURL("/Xslt/MultiNamespace/in.xml");
		String expectedResultContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><block><XDOC><REF_ID>0</REF_ID><XX>0</XX></XDOC><XDOC><REF_ID>1</REF_ID></XDOC><XDOC><REF_ID>2</REF_ID></XDOC></block>";
		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", new UrlMessage(originalMessage));

		XmlParameter parameter = new XmlParameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setType(ParameterType.NODE);
		parameter.setRemoveNamespaces(true);
		parameter.setXpathExpression("*");
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);
		assertThat(result, instanceOf(Node.class));
		assertThat(result, not(instanceOf(Document.class)));

		String contents = XmlUtils.transformXml(createTransformer(), new DOMSource((Node) result));
		assertEquals(expectedResultContents, contents);
	}

	@Test
	// Test for #2256 PutInSessionPipe with xpathExpression with type=domdoc
	// results in "Content is not allowed in prolog"
	public void testPutInSessionPipeWithDomdocParamsUsedMoreThanOnce() throws Exception {
		try (TestConfiguration configuration = new TestConfiguration()) {
			Adapter adapter = configuration.createBean();
			adapter.setName("testAdapter"); // Required for Metrics
			PipeLine pipeline = SpringUtils.createBean(adapter);
			String firstPipe = "PutInSessionPipe under test";
			String secondPipe = "PutInSessionPipe next pipe";

			String testMessage = """
					<Test>
						<Child><name>X</name></Child>
						<Child><name>Y</name></Child>
						<Child><name>Z</name></Child>
					</Test>\
					""";

			String testMessageChild1 = "<Child><name>X</name></Child>";

			PutInSessionPipe pipe = configuration.createBean();
			pipe.setName(firstPipe);
			pipe.setPipeLine(pipeline);
			XmlParameter p = new XmlParameter();
			p.setName("xmlMessageChild");
			p.setXpathExpression("Test/Child[1]");
			pipe.addParameter(p);
			pipeline.addPipe(pipe);

			PutInSessionPipe pipe2 = configuration.createBean();
			pipe2.setName(secondPipe);
			pipe2.setPipeLine(pipeline);
			Parameter p2 = new Parameter();
			p2.setName("xmlMessageChild2");
			p2.setSessionKey("xmlMessageChild");
			p2.setXpathExpression("Child/name/text()");
			pipe2.addParameter(p2);
			pipeline.addPipe(pipe2);

			PipeLineExit exit = new PipeLineExit();
			exit.setName("exit");
			exit.setState(ExitState.SUCCESS);
			pipeline.addPipeLineExit(exit);
			pipeline.configure();

			CorePipeLineProcessor cpp = configuration.createBean();
			CorePipeProcessor pipeProcessor = configuration.createBean();
			cpp.setPipeProcessor(pipeProcessor);
			PipeLineSession session = configuration.createBean();
			PipeLineResult pipeRunResult = cpp.processPipeLine(pipeline, "messageId", new Message(testMessage), session, firstPipe);

			assertEquals(ExitState.SUCCESS, pipeRunResult.getState());
			assertEquals(testMessage, pipeRunResult.getResult().asString());

			assertXmlEquals(testMessageChild1, session.getString("xmlMessageChild"));
			assertEquals("X", session.getString("xmlMessageChild2"));
		}
	}
}
