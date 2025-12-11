package org.frankframework.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.http.MediaType;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;
import org.frankframework.util.TimeProvider;
import org.frankframework.util.TransformerPool.OutputType;
import org.frankframework.util.XmlUtils;

public class StringParameterTest {

	@AfterEach
	void tearDown() {
		TimeProvider.resetClock();
	}

	private Transformer createTransformer() throws TransformerConfigurationException {
		return XmlUtils.getTransformerFactory().newTransformer();
	}

	@Test
	public void testParameterXPathToValue() throws Exception {
		Parameter p = new Parameter();
		p.setName("number");
		p.setValue("<dummy>a</dummy>");
		p.setXpathExpression("/dummy");
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertEquals("a", Message.asMessage(result).asString());

		assertFalse(p.requiresInputValueForResolution());
	}

	@Test
	public void testXPathParameterWithDocumentAsInput() throws Exception {
		Document domdoc = XmlUtils.buildDomDocument("<root>value</root>");

		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", domdoc);

		Parameter parameter = new Parameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setXpathExpression("/root");

		// This should have the same effect as setting the new outputType attribute: parameter.setType(ParameterType.XML);
		parameter.setXpathResult(OutputType.XML);
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);

		Message msg = assertInstanceOf(Message.class, result);
		assertEquals("<root>value</root>", msg.asString());
		assertEquals(MediaType.APPLICATION_XML, msg.getContext().getMimeType());
	}

	@Test
	public void testParameterFromNode() throws Exception {
		Node node = XmlUtils.buildDomDocument("<root>value</root>").getFirstChild();

		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", node);

		Parameter parameter = new Parameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setXpathExpression("/root");
		parameter.setXpathResult(OutputType.XML);
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);
		Message msg = assertInstanceOf(Message.class, result);
		assertEquals("<root>value</root>", msg.asString());
		assertEquals(MediaType.APPLICATION_XML, msg.getContext().getMimeType());
	}

	@Test
	public void testStringWithMaxLength() throws Exception {
		Parameter p = new Parameter();
		p.setName("string");
		p.setValue("abcdefghijklmnop");
		p.setMaxLength(5);
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		Message msg = assertInstanceOf(Message.class, result);
		assertEquals("abcde", msg.asString());
	}

	@Test
	public void testShortStringWithMaxLength() throws Exception {
		Parameter p = new Parameter();
		p.setName("string");
		p.setValue("abcde");
		p.setMaxLength(12);
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		Message msg = assertInstanceOf(Message.class, result);
		assertEquals("abcde", msg.asString());
	}

	@Test
	public void testLongStringWithMinLength() throws Exception {
		Parameter p = new Parameter();
		p.setName("string");
		p.setValue("abcdefghijklmnop");
		p.setMinLength(12);
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		Message msg = assertInstanceOf(Message.class, result);
		assertEquals("abcdefghijklmnop", msg.asString());
	}

	@Test
	public void testStringWithMinLength() throws Exception {
		Parameter p = new Parameter();
		p.setName("string");
		p.setValue("abcde");
		p.setMinLength(15);
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		Message msg = assertInstanceOf(Message.class, result);
		assertEquals("abcde          ", msg.asString());
	}

	@Test
	public void testStringWithMinAndMaxLength() throws Exception {
		Parameter p = new Parameter();
		p.setName("string");
		p.setValue("abcdefg");
		p.setMinLength(5);
		p.setMaxLength(10);
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		Message msg = assertInstanceOf(Message.class, result);
		assertEquals("abcdefg", msg.asString());
	}

	@Test
	public void testMessageWithMinAndMaxLength() throws Exception {
		Parameter p = new Parameter();
		p.setName("string");
		p.setSessionKey("testkey");
		p.setMinLength(5);
		p.setMaxLength(10);
		p.configure();
		try (PipeLineSession session = new PipeLineSession()) {
			session.put("testkey", new Message("abcdefg"));

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false);

			Message msg = assertInstanceOf(Message.class, result);
			assertEquals("abcdefg", msg.asString());
		}
	}

	@Test
	public void testByteArrayWithMinAndMaxLength() throws Exception {
		Parameter p = new Parameter();
		p.setName("string");
		p.setSessionKey("testkey");
		p.setMinLength(5);
		p.setMaxLength(10);
		p.configure();
		try (PipeLineSession session = new PipeLineSession()) {
			session.put("testkey", new Message("abcdefghijklmnop").asByteArray());

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false);

			Message resultMsg = assertInstanceOf(Message.class, result);
			assertEquals("abcdefghijklmnop", resultMsg.asString());
		}
	}
}
