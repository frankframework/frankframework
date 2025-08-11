package org.frankframework.parameters;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;
import org.frankframework.stream.UrlMessage;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.XmlUtils;

public class XmlParameterTest {
	@Test
	public void testParameterFromURLToDomdocTypeNoNameSpace() throws Exception {
		testParameterFromURLToDomTypeHelper(ParameterType.DOMDOC, false, Document.class);
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

		String contents = XmlUtils.transformXml(TransformerFactory.newInstance().newTransformer(), new DOMSource((Node) result));
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

		String contents = XmlUtils.transformXml(TransformerFactory.newInstance().newTransformer(), new DOMSource((Document) result));
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

		String contents = XmlUtils.transformXml(TransformerFactory.newInstance().newTransformer(), new DOMSource((Document) result));
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

		String contents = XmlUtils.transformXml(TransformerFactory.newInstance().newTransformer(), new DOMSource((Node) result));
		assertEquals(expectedResultContents, contents);
	}
}
