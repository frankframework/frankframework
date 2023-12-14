package nl.nn.adapterframework.parameters;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLine.ExitState;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.parameters.Parameter.ParameterType;
import nl.nn.adapterframework.pipes.PutInSession;
import nl.nn.adapterframework.processors.CorePipeLineProcessor;
import nl.nn.adapterframework.processors.CorePipeProcessor;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageContext;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.ParameterBuilder;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.DateFormatUtils;
import nl.nn.adapterframework.util.XmlUtils;

public class ParameterTest {

	@Test
	public void testPatternUsername() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("{username}");
		p.setUsername("fakeUsername");
		p.configure();

		PipeLineSession session = new PipeLineSession();
		ParameterValueList alreadyResolvedParameters=new ParameterValueList();

		assertEquals("fakeUsername", p.getValue(alreadyResolvedParameters, null, session, false));
	}

	@Test
	public void testPatternPassword() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("{password}");
		p.setPassword("fakePassword");
		p.configure();

		PipeLineSession session = new PipeLineSession();
		ParameterValueList alreadyResolvedParameters=new ParameterValueList();

		assertEquals("fakePassword", p.getValue(alreadyResolvedParameters, null, session, false));
	}

	@Test
	public void testPatternSessionVariable() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("{sessionKey}");
		p.configure();

		PipeLineSession session = new PipeLineSession();
		session.put("sessionKey", "fakeSessionVariable");

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();

		assertEquals("fakeSessionVariable", p.getValue(alreadyResolvedParameters, null, session, false));
	}

	@Test
	public void testPatternParameter() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("{siblingParameter}");
		p.configure();

		PipeLineSession session = new PipeLineSession();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Parameter siblingParameter = new Parameter();
		siblingParameter.setName("siblingParameter");
		siblingParameter.setValue("fakeParameterValue");
		siblingParameter.configure();
		alreadyResolvedParameters.add(new ParameterValue(siblingParameter, siblingParameter.getValue(alreadyResolvedParameters, null, session, false)));

		assertEquals("fakeParameterValue", p.getValue(alreadyResolvedParameters, null, session, false));
	}

	@Test
	public void testPatternCombined() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("param [{siblingParameter}] sessionKey [{sessionKey}] username [{username}] password [{password}]");
		p.setUsername("fakeUsername");
		p.setPassword("fakePassword");
		p.configure();

		PipeLineSession session = new PipeLineSession();
		session.put("sessionKey", "fakeSessionVariable");

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Parameter siblingParameter = new Parameter();
		siblingParameter.setName("siblingParameter");
		siblingParameter.setValue("fakeParameterValue");
		siblingParameter.configure();
		alreadyResolvedParameters.add(new ParameterValue(siblingParameter, siblingParameter.getValue(alreadyResolvedParameters, null, session, false)));

		assertEquals("param [fakeParameterValue] sessionKey [fakeSessionVariable] username [fakeUsername] password [fakePassword]", p.getValue(alreadyResolvedParameters, null, session, false));
	}

	@Test
	public void testPatternUsedAsSourceForTransformation() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("<root><username>{username}</username></root>");
		p.setXpathExpression("root/username");
		p.setUsername("fakeUsername");
		p.configure();

		PipeLineSession session = new PipeLineSession();
		ParameterValueList alreadyResolvedParameters=new ParameterValueList();

		assertEquals("fakeUsername", p.getValue(alreadyResolvedParameters, null, session, false));
	}

	@Test
	public void testEmptyPatternUsedAsSourceForTransformation() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("{username}");
		p.setXpathExpression("root/username");
		p.setDefaultValue("fakeDefault");
		p.configure();

		PipeLineSession session = new PipeLineSession();
		ParameterValueList alreadyResolvedParameters=new ParameterValueList();

		assertEquals("fakeDefault", p.getValue(alreadyResolvedParameters, null, session, false));
	}

	@Test
	public void testPatternUnknownSessionVariableOrParameter() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("{unknown}");
		p.configure();

		PipeLineSession session = new PipeLineSession();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();

		ParameterException e = assertThrows(ParameterException.class, () -> p.getValue(alreadyResolvedParameters, null, session, false));
		assertEquals("Parameter or session variable with name [unknown] in pattern [{unknown}] cannot be resolved", e.getMessage());
	}

	@Test
	public void testPatternUnknownSessionVariableOrParameterSilentlyIgnored() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("{unknown}");
		p.setIgnoreUnresolvablePatternElements(true);
		p.configure();

		PipeLineSession session = new PipeLineSession();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();

		assertEquals("", p.getValue(alreadyResolvedParameters, null, session, false));
	}

	@Test
	public void testContextKey() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setContextKey("fakeContextKey");
		p.configure();

		Message input = new Message("fakeMessage", new MessageContext().with("fakeContextKey", "fakeContextValue"));
		PipeLineSession session = new PipeLineSession();
		ParameterValueList alreadyResolvedParameters=new ParameterValueList();

		assertEquals("fakeContextValue", p.getValue(alreadyResolvedParameters, input, session, false));
	}

	@Test
	public void testContextKeyWithSessionKey() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setSessionKey("fakeSessionKey");
		p.setContextKey("fakeContextKey");
		p.configure();

		Message input = new Message("fakeMessage1", new MessageContext().with("fakeContextKey", "fakeContextValue1"));
		Message sessionValue = new Message("fakeMessage2", new MessageContext().with("fakeContextKey", "fakeContextValue2"));

		PipeLineSession session = new PipeLineSession();
		session.put("fakeSessionKey", sessionValue);
		ParameterValueList alreadyResolvedParameters=new ParameterValueList();

		assertEquals("fakeContextValue2", p.getValue(alreadyResolvedParameters, input, session, false));
	}
	@Test
	public void testContextKeyWithXPath() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setContextKey("fakeContextKey");
		p.setXpathExpression("count(root/a)");
		p.configure();

		Message input = new Message("fakeMessage", new MessageContext().with("fakeContextKey", "<root><a/><a/></root>"));
		PipeLineSession session = new PipeLineSession();
		ParameterValueList alreadyResolvedParameters=new ParameterValueList();

		assertEquals("2", p.getValue(alreadyResolvedParameters, input, session, false));
	}
	@Test
	public void testContextKeyWithSessionKeyAndXPath() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setSessionKey("fakeSessionKey");
		p.setContextKey("fakeContextKey");
		p.setXpathExpression("count(root/a)");
		p.configure();

		Message input = new Message("fakeMessage1", new MessageContext().with("fakeContextKey", "<root><a/><a/></root>"));
		Message sessionValue = new Message("fakeMessage2", new MessageContext().with("fakeContextKey", "<root><a/><a/><a/></root>"));

		PipeLineSession session = new PipeLineSession();
		session.put("fakeSessionKey", sessionValue);
		ParameterValueList alreadyResolvedParameters=new ParameterValueList();

		assertEquals("3", p.getValue(alreadyResolvedParameters, input, session, false));
	}

	@Test
	public void testEmptyParameterResolvesToMessage() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.configure();

		PipeLineSession session = new PipeLineSession();
		ParameterValueList alreadyResolvedParameters=new ParameterValueList();

		Message message = new Message("fakeMessage");

		assertEquals("fakeMessage", p.getValue(alreadyResolvedParameters, message, session, false));
	}

	@Test
	public void testValueSetEmptyDoesNotResolveToMessage() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setValue("");
		p.configure();

		PipeLineSession session = new PipeLineSession();
		ParameterValueList alreadyResolvedParameters=new ParameterValueList();

		Message message = new Message("fakeMessage");

		assertEquals("", p.getValue(alreadyResolvedParameters, message, session, false));
	}

	@Test
	public void testValueSetEmptyDoesResolveToMessageWhenDefaultValueIsSetToInput() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setValue("");
		p.setDefaultValueMethods("input");
		p.configure();

		PipeLineSession session = new PipeLineSession();
		ParameterValueList alreadyResolvedParameters=new ParameterValueList();

		Message message = new Message("fakeMessage");

		assertEquals("fakeMessage", p.getValue(alreadyResolvedParameters, message, session, false));
	}

	@Test
	public void testEmptyDefault() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setSessionKey("dummy");
		p.setDefaultValue("");
		p.configure();

		PipeLineSession session = new PipeLineSession();
		ParameterValueList alreadyResolvedParameters=new ParameterValueList();

		Message message = new Message("fakeMessage");

		assertEquals("", p.getValue(alreadyResolvedParameters, message, session, false));
	}

	@Test
	public void testParameterValueMessageString() throws ConfigurationException, ParameterException {
		String sessionKey = "mySessionKey";
		String sessionMessage = "message goes here "+UUID.randomUUID();

		Parameter p = new Parameter();
		p.setName("readMyMessage");
		p.setSessionKey(sessionKey);
		p.configure();

		PipeLineSession session = new PipeLineSession();
		session.put(sessionKey, new Message(sessionMessage));

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		assertEquals(sessionMessage, p.getValue(alreadyResolvedParameters, message, session, false));
	}

	@Test
	public void testParameterValueMessage() throws Exception {
		String sessionKey = "mySessionKey";
		String sessionMessage = "message goes here "+UUID.randomUUID();
		ByteArrayInputStream is = new ByteArrayInputStream(sessionMessage.getBytes());

		Parameter p = new Parameter();
		p.setName("readMyMessage");
		p.setSessionKey(sessionKey);
		p.configure();

		PipeLineSession session = new PipeLineSession();
		session.put(sessionKey, new Message(is));

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(result instanceof Message);

		assertEquals(sessionMessage, Message.asMessage(result).asString());
	}

	@Test
	public void testParameterValueList() throws Exception {
		String sessionKey = "mySessionKey";

		Parameter p = new Parameter();
		p.setName("myParameter");
		p.setSessionKey(sessionKey);
		p.setType(ParameterType.LIST);
		p.setXpathExpression("items/item");
		p.configure();

		PipeLineSession session = new PipeLineSession();
		session.put(sessionKey, Arrays.asList(new String[] {"fiets", "bel", "appel"}));

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(result instanceof String);

		String stringResult = Message.asMessage(result).asString();
		assertEquals("fiets bel appel", stringResult);
	}

	@Test
	public void testParameterXPathToValue() throws Exception {
		Parameter p = new Parameter();
		p.setName("number");
		p.setValue("<dummy>a</dummy>");
		p.setXpathExpression("/dummy");
		p.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertEquals("a", Message.asMessage(result).asString());
	}

	@Test
	public void testParameterXPathToMessage() throws Exception {
		Parameter p = new Parameter();
		p.setName("number");
		p.setXpathExpression("/dummy");
		p.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("<dummy>a</dummy>");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertEquals("a", Message.asMessage(result).asString());
	}

	@Test
	public void testParameterXPathUnknownSessionKey() throws Exception {
		Parameter p = new Parameter();
		p.setName("number");
		p.setSessionKey("unknownSessionKey");
		p.setXpathExpression("/dummy");
		p.setDefaultValue("fakeDefault");
		p.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		PipeLineSession session = new PipeLineSession();

		Object result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertEquals("fakeDefault", Message.asMessage(result).asString());
	}

	@Test
	public void testParameterXPathEmptySessionKey() throws Exception {
		Parameter p = new Parameter();
		p.setName("number");
		p.setSessionKey("emptySessionKey");
		p.setXpathExpression("/dummy");
		p.setDefaultValue("fakeDefault");
		p.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		PipeLineSession session = new PipeLineSession();
		session.put("emptySessionKey", "");

		Object result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertEquals("fakeDefault", Message.asMessage(result).asString());
	}

	@Test
	public void testParameterNumberBoolean() throws Exception {
		Parameter p = new Parameter();
		p.setName("number");
		p.setValue("a");
		p.setType(ParameterType.BOOLEAN);
		p.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertTrue(result instanceof Boolean);
	}

	@Test
	public void testParameterMap() throws Exception {
		Parameter p = new Parameter();
		p.setName("map");
		p.setSessionKey("sessionKey");
		p.setXpathExpression("items/item");
		p.setType(ParameterType.MAP);
		p.configure();

		Map<String, String> map = new HashMap<>();
		map.put("item", "value");
		map.put("item2", "value2");
		map.put("item3", "value3");
		map.put("item4", "value4");

		PipeLineSession session = new PipeLineSession();
		session.put("sessionKey", map);

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(result instanceof String);

		String stringResult = Message.asMessage(result).asString();
		assertEquals("value value2 value4 value3", stringResult);
	}

	@Test
	public void testParameterNumberParseException() throws Exception {
		Parameter p = new Parameter();
		p.setName("number");
		p.setValue("a");
		p.setType(ParameterType.NUMBER);
		p.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		assertThrows(ParameterException.class, () -> p.getValue(alreadyResolvedParameters, message, null, false));
	}

	@Test
	public void testParameterInteger() throws Exception {
		testParameterTypeHelper(ParameterType.INTEGER, Integer.class);
	}
	@Test
	public void testParameterNumber() throws Exception {
		testParameterTypeHelper(ParameterType.NUMBER, Number.class);
	}

	public <T> void testParameterTypeHelper(ParameterType type, Class<T> c) throws Exception {
		Parameter p = new Parameter();
		p.setName("integer");
		p.setValue("8");
		p.setType(type);
		p.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertTrue(c.isAssignableFrom(result.getClass()), c+" is expected type but was: "+result.getClass());

		PipeLineSession session = new PipeLineSession();
		session.put("sessionkey", 8);
		p = new Parameter();
		p.setName("integer");
		p.setSessionKey("sessionkey");
		p.setType(type);
		p.configure();

		result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(c.isAssignableFrom(result.getClass()), c+" is expected type but was: "+result.getClass());

		session = new PipeLineSession();
		session.put("sessionkey", "8");

		result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(c.isAssignableFrom(result.getClass()), c+" is expected type but was: "+result.getClass());

		session = new PipeLineSession();
		session.put("sessionkey", Message.asMessage(8));

		result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(c.isAssignableFrom(result.getClass()), c+" is expected type but was: "+result.getClass());

		session = new PipeLineSession();
		session.put("sessionkey", "8".getBytes());

		result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(c.isAssignableFrom(result.getClass()), c+" is expected type but was: "+result.getClass());

		session = new PipeLineSession();
		session.put("sessionkey", Message.asMessage("8".getBytes()));

		result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(c.isAssignableFrom(result.getClass()), c+" is expected type but was: "+result.getClass());

	}

	@Test
	public void testNumberWithLeftPadding() throws Exception {
		Parameter p = new Parameter();
		p.setName("number");
		p.setValue("8");
		p.setMinLength(10);
		p.setType(ParameterType.NUMBER);
		p.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertTrue(result instanceof String, "Expecting to be String but was:"+result.getClass());
		assertEquals("0000000008", (String) result);
	}

	@Test
	public void testNumberWithLeftPaddingAndMinExclusive() throws Exception {
		Parameter p = new Parameter();
		p.setName("number");
		p.setValue("3");
		p.setMinLength(10);
		p.setMinInclusive("5");
		p.setType(ParameterType.NUMBER);
		p.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertTrue(result instanceof String);
		assertEquals("0000000005", (String) result);
	}

	@Test
	public void testNumberWithLeftPaddingAndMaxExclusive() throws Exception {
		Parameter p = new Parameter();
		p.setName("number");
		p.setValue("8");
		p.setMinLength(10);
		p.setMaxInclusive("5");
		p.setType(ParameterType.NUMBER);
		p.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertTrue(result instanceof String);
		assertEquals("0000000005", (String) result);
	}

	@Test
	public void testNumberWithLeftPaddingAndMaxExclusiveNotExceeding() throws Exception {
		Parameter p = new Parameter();
		p.setName("number");
		p.setValue("3");
		p.setMinLength(10);
		p.setMaxInclusive("5");
		p.setType(ParameterType.NUMBER);
		p.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertTrue(result instanceof String);
		assertEquals("0000000003", (String) result);
	}

	@Test
	public void testNumberWithLeftPaddingAndMinExclusiveNotExceeding() throws Exception {
		Parameter p = new Parameter();
		p.setName("number");
		p.setValue("5");
		p.setMinLength(10);
		p.setMinInclusive("3");
		p.setType(ParameterType.NUMBER);
		p.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertTrue(result instanceof String);
		assertEquals("0000000005", (String) result);
	}

	@Test
	public void testStringWithMaxLength() throws Exception {
		Parameter p = new Parameter();
		p.setName("string");
		p.setValue("1234567890");
		p.setMaxLength(5);
		p.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertTrue(result instanceof String);
		assertEquals("12345", (String) result);
	}

	@Test
	public void testStringWithMinLength() throws Exception {
		Parameter p = new Parameter();
		p.setName("string");
		p.setValue("1234567890");
		p.setMinLength(15);
		p.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertTrue(result instanceof String);
		assertEquals("1234567890     ", (String) result);
	}

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
		session.put("originalMessage", Message.asMessage(originalMessage));

		Parameter inputMessage = new Parameter();
		inputMessage.setName("InputMessage");
		inputMessage.setSessionKey("originalMessage");
		inputMessage.setType(type);
		inputMessage.setRemoveNamespaces(removeNamespaces);
		inputMessage.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = inputMessage.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(c.isAssignableFrom(result.getClass()), c+" is expected type but was: "+result.getClass());
	}

	@Test
	public void testParameterFromURLToDomdocWithXpath() throws Exception {
		URL originalMessage = TestFileUtils.getTestFileURL("/Xslt/MultiNamespace/in.xml");
		String expectedResultContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><block><XDOC><REF_ID>0</REF_ID><XX>0</XX></XDOC><XDOC><REF_ID>1</REF_ID></XDOC><XDOC><REF_ID>2</REF_ID></XDOC></block>";
		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", Message.asMessage(originalMessage));

		Parameter parameter = new Parameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setType(ParameterType.DOMDOC);
		parameter.setRemoveNamespaces(true);
		parameter.setXpathExpression("*");
		parameter.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);
		assertThat(result,instanceOf(Document.class));

		String contents = XmlUtils.transformXml(TransformerFactory.newInstance().newTransformer(), new DOMSource((Document)result));
		assertEquals(expectedResultContents,contents);
	}

	@Test
	public void testParameterFromURLToNodeWithXpath() throws Exception {
		URL originalMessage = TestFileUtils.getTestFileURL("/Xslt/MultiNamespace/in.xml");
		String expectedResultContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><block><XDOC><REF_ID>0</REF_ID><XX>0</XX></XDOC><XDOC><REF_ID>1</REF_ID></XDOC><XDOC><REF_ID>2</REF_ID></XDOC></block>";
		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", Message.asMessage(originalMessage));

		Parameter parameter = new Parameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setType(ParameterType.NODE);
		parameter.setRemoveNamespaces(true);
		parameter.setXpathExpression("*");
		parameter.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);
		assertThat(result,instanceOf(Node.class));
		assertThat(result,not(instanceOf(Document.class)));

		String contents = XmlUtils.transformXml(TransformerFactory.newInstance().newTransformer(), new DOMSource((Node)result));
		assertEquals(expectedResultContents,contents);
	}

	@Test
	public void testParameterFromBytesToDomdoc() throws Exception {
		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", "<someValue/>".getBytes());
		String expectedResultContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><someValue/>";

		Parameter parameter = new Parameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setType(ParameterType.DOMDOC);
		parameter.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);
		assertThat(result,instanceOf(Document.class));

		String contents = XmlUtils.transformXml(TransformerFactory.newInstance().newTransformer(), new DOMSource((Document)result));
		assertEquals(expectedResultContents,contents);
	}

	@Test
	public void testParameterFromBytesToNode() throws Exception {
		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", "<someValue/>".getBytes());
		String expectedResultContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><someValue/>";

		Parameter parameter = new Parameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setType(ParameterType.NODE);
		parameter.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);
		assertThat(result,instanceOf(Node.class));
		assertThat(result,not(instanceOf(Document.class)));

		String contents = XmlUtils.transformXml(TransformerFactory.newInstance().newTransformer(), new DOMSource((Node)result));
		assertEquals(expectedResultContents,contents);
	}

	@Test
	public void testParameterFromDomToDomdoc() throws Exception {
		Document domdoc = XmlUtils.buildDomDocument("<someValue/>");
		String expectedResultContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><someValue/>";

		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", domdoc);

		Parameter parameter = new Parameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setType(ParameterType.DOMDOC);
		parameter.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);
		assertThat(result,instanceOf(Document.class));

		String contents = XmlUtils.transformXml(TransformerFactory.newInstance().newTransformer(), new DOMSource((Document)result));
		assertEquals(expectedResultContents,contents);
	}

	@Test
	public void testParameterFromDomToNode() throws Exception {
		Document domdoc = XmlUtils.buildDomDocument("<someValue/>");
		String expectedResultContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><someValue/>";

		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", domdoc);

		Parameter parameter = new Parameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setType(ParameterType.NODE);
		parameter.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);
		assertThat(result,instanceOf(Node.class));
		assertThat(result,not(instanceOf(Document.class)));

		String contents = XmlUtils.transformXml(TransformerFactory.newInstance().newTransformer(), new DOMSource((Node)result));
		assertEquals(expectedResultContents,contents);
	}

	@Test
	public void testParameterFromNodeToDomdoc() throws Exception {
		Node node = XmlUtils.buildDomDocument("<someValue/>").getFirstChild();
		String expectedResultContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><someValue/>";

		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", node);

		Parameter parameter = new Parameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setType(ParameterType.DOMDOC);
		parameter.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);
		assertThat(result,instanceOf(Document.class));

		String contents = XmlUtils.transformXml(TransformerFactory.newInstance().newTransformer(), new DOMSource((Document)result));
		assertEquals(expectedResultContents,contents);
	}

	@Test
	public void testParameterFromNodeToNode() throws Exception {
		Node node = XmlUtils.buildDomDocument("<someValue/>").getFirstChild();
		String expectedResultContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><someValue/>";

		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", node);

		Parameter parameter = new Parameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setType(ParameterType.NODE);
		parameter.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);
		assertThat(result,instanceOf(Node.class));
		assertThat(result,not(instanceOf(Document.class)));

		String contents = XmlUtils.transformXml(TransformerFactory.newInstance().newTransformer(), new DOMSource((Node)result));
		assertEquals(expectedResultContents,contents);
	}



	@Test
	public void testParameterFromDateToDate() throws Exception {
		Date date = new Date();

		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", date);

		Parameter parameter = new Parameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setType(ParameterType.DATE);
		parameter.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);
		assertThat(result,instanceOf(Date.class));

		assertEquals(date,result);
	}

	protected void testFromStringToDateType(String input, String expected, ParameterType type) throws ConfigurationException, ParameterException {

		Parameter parameter = new Parameter();
		parameter.setName("InputMessage");
		parameter.setValue(input);
		parameter.setType(type);
		parameter.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, null, true);
		assertThat(result,instanceOf(Date.class));

		assertEquals(expected, DateFormatUtils.format((Date)result));

	}
	@Test
	public void testParameterFromStringToDate() throws Exception {
		String input = "2022-01-23";
		String expected = "2022-01-23 00:00:00.000";
		testFromStringToDateType(input, expected, ParameterType.DATE);
	}
	@Test
	public void testParameterFromStringToDateTime() throws Exception {
		String input = "2022-01-23 11:14:17";
		String expected = "2022-01-23 11:14:17.000";
		testFromStringToDateType(input, expected, ParameterType.DATETIME);
	}
	@Test
	public void testParameterFromStringToTimestamp() throws Exception {
		String input = "2022-01-23 11:14:17.123";
		String expected = "2022-01-23 11:14:17.123";
		testFromStringToDateType(input, expected, ParameterType.TIMESTAMP);
	}
	@Test
	public void testParameterFromStringToTime() throws Exception {
		String input = "11:14:17";
		String expected = "1970-01-01 11:14:17.000";
		testFromStringToDateType(input, expected, ParameterType.TIME);
	}
	@Test
	public void testParameterFromStringToXmlDateTime() throws Exception {
		String input = "2022-01-23T11:14:17";
		String expected = "2022-01-23 11:14:17.000";
		testFromStringToDateType(input, expected, ParameterType.XMLDATETIME);
	}

	@Test
	public void testParameterFromDateToXmlDateTime() throws Exception {
		Date date = new Date();

		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", date);

		Parameter parameter = new Parameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setType(ParameterType.XMLDATETIME);
		parameter.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);
		assertThat(result,instanceOf(Date.class));

		assertEquals(date,result);
	}


	@Test
	// Test for #2256 PutParametersInSession with xpathExpression with type=domdoc results in "Content is not allowed in prolog"
	public void testPutInSessionPipeWithDomdocParamsUsedMoreThanOnce() throws Exception {
		try (TestConfiguration configuration = new TestConfiguration()) {
			PipeLine pipeline = configuration.createBean(PipeLine.class);
			String firstPipe = "PutInSession under test";
			String secondPipe = "PutInSession next pipe";

			String testMessage = "<Test>\n" +
					"	<Child><name>X</name></Child>\n" +
					"	<Child><name>Y</name></Child>\n" +
					"	<Child><name>Z</name></Child>\n" +
					"</Test>";

			String testMessageChild1 = "<Child><name>X</name></Child>";

			PutInSession pipe = configuration.createBean(PutInSession.class);
			pipe.setName(firstPipe);
			pipe.setPipeLine(pipeline);
			Parameter p = new Parameter();
			p.setName("xmlMessageChild");
			p.setXpathExpression("Test/Child[1]");
			p.setType(ParameterType.DOMDOC);
			pipe.addParameter(p);
			pipeline.addPipe(pipe);

			PutInSession pipe2 = configuration.createBean(PutInSession.class);
			pipe2.setName(secondPipe);
			pipe2.setPipeLine(pipeline);
			Parameter p2 = new Parameter();
			p2.setName("xmlMessageChild2");
			p2.setSessionKey("xmlMessageChild");
			p2.setXpathExpression("Child/name/text()");
			pipe2.addParameter(p2);
			pipeline.addPipe(pipe2);

			PipeLineExit exit = new PipeLineExit();
			exit.setPath("exit");
			exit.setState(ExitState.SUCCESS);
			pipeline.registerPipeLineExit(exit);
			pipeline.configure();

			CorePipeLineProcessor cpp = configuration.createBean(CorePipeLineProcessor.class);
			CorePipeProcessor pipeProcessor = configuration.createBean(CorePipeProcessor.class);
			cpp.setPipeProcessor(pipeProcessor);
			PipeLineSession session = configuration.createBean(PipeLineSession.class);
			pipeline.setOwner(pipe);
			PipeLineResult pipeRunResult=cpp.processPipeLine(pipeline, "messageId", new Message(testMessage), session, firstPipe);

			assertEquals(ExitState.SUCCESS, pipeRunResult.getState());
			assertEquals(testMessage, pipeRunResult.getResult().asString());

			MatchUtils.assertXmlEquals(testMessageChild1, session.getString("xmlMessageChild"));
			assertEquals("X", session.getString("xmlMessageChild2"));
		}
	}

	@Test
	public void testFixedDate() throws Exception {
		Parameter p = new Parameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try {
			p.setName("date");
			p.setPattern("{fixedDate}");
			p.setType(ParameterType.DATE);
			p.configure();
			PipeLineSession session = new PipeLineSession();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false); //Should return PutSystemDateInSession.FIXEDDATETIME
			assertTrue(result instanceof Date);

			Date resultDate = (Date) result;
			SimpleDateFormat sdf = new SimpleDateFormat(Parameter.TYPE_DATE_PATTERN);
			String formattedDate = sdf.format(resultDate);
			assertEquals("2001-12-17", formattedDate);

		} finally {
			System.getProperties().remove(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY);
		}
	}

	@Test
	public void testFixedDateWithSession() throws Exception {
		Parameter p = new Parameter();
		p.setName("date");
		p.setPattern("{fixedDate}");
		p.setType(ParameterType.DATE);
		p.configure();
		PipeLineSession session = new PipeLineSession();
		session.put("fixedDate", "1995-01-23");
		session.put("stub4testtool.fixeddate", "1996-02-24");

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(result instanceof Date);

		Date resultDate = (Date) result;
		SimpleDateFormat sdf = new SimpleDateFormat(Parameter.TYPE_DATE_PATTERN);
		String formattedDate = sdf.format(resultDate);
		assertEquals("1995-01-23", formattedDate);
	}

	@Test
	public void testFixedDateWithSessionFromTesttool() throws Exception {
		Parameter p = new Parameter();
		p.setName("date");
		p.setPattern("{fixedDate}");
		p.setType(ParameterType.DATE);
		p.configure();
		PipeLineSession session = new PipeLineSession();
		session.put("stub4testtool.fixeddate", "1996-02-24");

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		try {
			System.setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY,"true");
			Object result = p.getValue(alreadyResolvedParameters, message, session, false);
			assertTrue(result instanceof Date);
			Date resultDate = (Date) result;
			SimpleDateFormat sdf = new SimpleDateFormat(Parameter.TYPE_DATE_PATTERN);
			String formattedDate = sdf.format(resultDate);
			assertEquals("1996-02-24", formattedDate);
		} finally {
			System.getProperties().remove(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY);
		}
	}

	@Test
	public void testFixedDateWithDateInSessionFromTesttool() throws Exception {
		Parameter p = new Parameter();
		p.setName("date");
		p.setPattern("{fixedDate}");
		p.setType(ParameterType.DATE);
		p.configure();
		PipeLineSession session = new PipeLineSession();
		Date date = new Date();
		session.put("stub4testtool.fixeddate", date);

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		try {
			System.setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY,"true");
			Object result = p.getValue(alreadyResolvedParameters, message, session, false);
			assertTrue(result instanceof Date);
			Date resultDate = (Date) result;
			SimpleDateFormat sdf = new SimpleDateFormat(Parameter.TYPE_DATE_PATTERN);
			String formattedDate = sdf.format(resultDate);
			String formattedExpected = sdf.format(date);
			assertEquals(formattedExpected, formattedDate);
		} finally {
			System.getProperties().remove(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY);
		}
	}

	@Test
	public void testFixedDateWithDateObjectInSession() throws Exception {
		Parameter p = new Parameter();
		p.setName("date");
		p.setPattern("{fixedDate}");
		p.setType(ParameterType.DATE);
		p.configure();
		PipeLineSession session = new PipeLineSession();
		SimpleDateFormat sdf = new SimpleDateFormat(Parameter.TYPE_DATE_PATTERN);
		session.put("fixedDate",sdf.parse("1995-01-23"));

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(result instanceof Date);

		Date resultDate = (Date) result;
		String formattedDate = sdf.format(resultDate);
		assertEquals("1995-01-23", formattedDate);
	}

	@Test
	public void testPatternNowWithDateType() throws Exception {
		Parameter p = new Parameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try {
			p.setName("date");
			p.setPattern("{now}");
			p.setType(ParameterType.DATE);
			p.configure();
			PipeLineSession session = new PipeLineSession();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false); //Should return PutSystemDateInSession.FIXEDDATETIME
			assertTrue(result instanceof Date);

			Date resultDate = (Date) result;
			SimpleDateFormat sdf = new SimpleDateFormat(Parameter.TYPE_DATE_PATTERN);
			String formattedDate = sdf.format(resultDate);
			String expectedDate = sdf.format(new Date()); // dit gaat echt meestal wel goed
			assertEquals(expectedDate, formattedDate);

		} finally {
			System.getProperties().remove(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY);
		}
	}

	@Test
	public void testPatternNowWithStringType() throws Exception {
		Parameter p = new Parameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try {
			p.setName("date");
			p.setPattern("{now}");
			p.configure();
			PipeLineSession session = new PipeLineSession();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false); //Should return PutSystemDateInSession.FIXEDDATETIME
			assertTrue(result instanceof String);

			SimpleDateFormat sdf = new SimpleDateFormat(DateFormatUtils.FORMAT_FULL_GENERIC);
			String expectedDate = sdf.format(new Date()); // dit gaat echt meestal wel goed
			assertEquals(expectedDate.substring(0, 10), ((String)result).substring(0, 10));

		} finally {
			System.getProperties().remove(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY);
		}
	}

	@Test
	public void testPatternNowWithDateFormatType() throws Exception {
		Parameter p = new Parameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try {
			p.setName("EsbSoapWrapperPipeTimestamp");
			p.setPattern("{now,date,yyyy-MM-dd'T'HH:mm:ss}");
			p.configure();
			PipeLineSession session = new PipeLineSession();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false); //Should return PutSystemDateInSession.FIXEDDATETIME
			assertTrue(result instanceof String);

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			String expectedDate = sdf.format(new Date()); // dit gaat echt meestal wel goed
			assertEquals(expectedDate.substring(0, 10), ((String)result).substring(0, 10));

		} finally {
			System.getProperties().remove(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY);
		}
	}

	@Test
	public void testPatternFixedDateWithDateFormatType() throws Exception {
		String expectedDate = "2001-12-17T09:30";
		Parameter p = new Parameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try {
			p.setName("EsbSoapWrapperPipeTimestamp");
			p.setPattern("{fixeddate,date,yyyy-MM-dd'T'HH:mm}");
			p.configure();
			PipeLineSession session = new PipeLineSession();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false); //Should return PutSystemDateInSession.FIXEDDATETIME
			assertTrue(result instanceof String);

			assertEquals(expectedDate, result);

		} finally {
			System.getProperties().remove(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY);
		}
	}

	@Test
	public void testPatternFixedDateWithExtendedDateFormatType() throws Exception {
		String expectedDate = "2001-12-17 09:30:47.000";
		Parameter p = new Parameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try {
			p.setName("EsbSoapWrapperPipeTimestamp");
			p.setPattern("{fixeddate,date,yyyy-MM-dd HH:mm:ss.SSS}");
			p.configure();
			PipeLineSession session = new PipeLineSession();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false); //Should return PutSystemDateInSession.FIXEDDATETIME
			assertTrue(result instanceof String);

			assertEquals(expectedDate, result);

		} finally {
			System.getProperties().remove(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY);
		}
	}

	@Test
	public void testPatternFixedDateWithDateFormatTypeAndParameterTypeSet() throws Exception {
		String expectedDate = "2001-12-17 09:30:47.000";
		Parameter p = new Parameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try {
			p.setName("EsbSoapWrapperPipeTimestamp");
			p.setPattern("{fixeddate,date,yyyy-MM-dd'T'HH:mm:ss}");
			p.setType(ParameterType.TIMESTAMP);
			p.setFormatString("yyyy-MM-dd'T'HH:mm:ss");
			p.configure();
			PipeLineSession session = new PipeLineSession();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false); //Should return PutSystemDateInSession.FIXEDDATETIME
			assertTrue(result instanceof Date);

			SimpleDateFormat sdf = new SimpleDateFormat(DateFormatUtils.FORMAT_FULL_GENERIC);
			assertEquals(expectedDate, sdf.format(result));

		} finally {
			System.getProperties().remove(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY);
		}
	}

	@Test
	public void testPatternFixedDateWithParameterTypeDateTime() throws Exception {
		String expectedDate = "2001-12-17 09:30:47.000";
		Parameter p = new Parameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try {
			p.setName("EsbSoapWrapperPipeTimestamp");
			p.setPattern("{fixeddate}");
			p.setType(ParameterType.DATETIME);
			p.configure();
			PipeLineSession session = new PipeLineSession();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false); //Should return PutSystemDateInSession.FIXEDDATETIME
			assertTrue(result instanceof Date);

			SimpleDateFormat sdf = new SimpleDateFormat(DateFormatUtils.FORMAT_FULL_GENERIC);
			assertEquals(expectedDate, sdf.format(result));

		} finally {
			System.getProperties().remove(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY);
		}
	}

	@Test
	public void testPatternFixedDateWithParameterTypeTimestamp() throws Exception {
		String expectedDate = "2001-12-17 09:30:47.000";
		Parameter p = new Parameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try {
			p.setName("EsbSoapWrapperPipeTimestamp");
			p.setPattern("{fixeddate}");
			p.setFormatString("yyyy-MM-dd HH:mm:ss");
			p.setType(ParameterType.TIMESTAMP);
			p.configure();
			PipeLineSession session = new PipeLineSession();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false); //Should return PutSystemDateInSession.FIXEDDATETIME
			assertTrue(result instanceof Date);

			SimpleDateFormat sdf = new SimpleDateFormat(DateFormatUtils.FORMAT_FULL_GENERIC);
			assertEquals(expectedDate, sdf.format(result));

		} finally {
			System.getProperties().remove(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY);
		}
	}

	@Test
	public void testPatternFixedDateWithExtendedDateFormatTypeAndParameterTypeSet() throws Exception {
		String expectedDate = "2001-12-17 09:30:47.000";
		Parameter p = new Parameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try {
			p.setName("EsbSoapWrapperPipeTimestamp");
			p.setPattern("{fixeddate,date,yyyy-MM-dd HH:mm:ss.SSS}");
			p.setType(ParameterType.TIMESTAMP);
			p.configure();
			PipeLineSession session = new PipeLineSession();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false); //Should return PutSystemDateInSession.FIXEDDATETIME
			assertTrue(result instanceof Date);

			SimpleDateFormat sdf = new SimpleDateFormat(DateFormatUtils.FORMAT_FULL_GENERIC);
			assertEquals(expectedDate, sdf.format(result));

		} finally {
			System.getProperties().remove(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY);
		}
	}


	@Test
	public void testDefaultValueMethodDefaultNoDefaultValue() throws Exception {
		Parameter p = new Parameter();
		p.setXpathExpression("*/*");
		p.setValue("<doc/>");
		p.configure();
		PipeLineSession session = new PipeLineSession();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		String result = (String)p.getValue(alreadyResolvedParameters, message, session, false);

		assertEquals(null, result);
	}

	@Test
	public void testDefaultValueMethodDefault() throws Exception {
		Parameter p = new Parameter();
		p.setXpathExpression("*/*");
		p.setValue("<doc/>");
		p.setDefaultValue("fakeDefaultValue");
		p.setSessionKey("sessionKeyForDefaultValue");
		p.setPattern("{sessionKeyForPattern}");
		p.configure();
		PipeLineSession session = new PipeLineSession();

		session.put("sessionKeyForDefaultValue", "fakeDefaultValueSessionKey");
		session.put("sessionKeyForPattern", "fakePatternSessionKey");
		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		String result = (String)p.getValue(alreadyResolvedParameters, message, session, false);

		assertEquals("fakeDefaultValue", result);
	}

	@Test
	public void testDefaultValueMethodSessionKey() throws Exception {
		Parameter p = new Parameter();
		p.setXpathExpression("*/*");
		p.setValue("<doc/>");
		p.setDefaultValue("fakeDefaultValue");
		p.setSessionKey("sessionKeyForDefaultValue");
		p.setPattern("{sessionKeyForPattern}");
		p.setDefaultValueMethods("sessionKey");
		p.configure();
		PipeLineSession session = new PipeLineSession();

		session.put("sessionKeyForDefaultValue", "fakeDefaultValueSessionKey");
		session.put("sessionKeyForPattern", "fakePatternSessionKey");
		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		String result = (String)p.getValue(alreadyResolvedParameters, message, session, false);

		assertEquals("fakeDefaultValueSessionKey", result);
	}

	@Test
	public void testDefaultValueMethodPattern() throws Exception {
		Parameter p = new Parameter();
		p.setXpathExpression("*/*");
		p.setValue("<doc/>");
		p.setDefaultValue("fakeDefaultValue");
		p.setSessionKey("sessionKeyForDefaultValue");
		p.setPattern("{sessionKeyForPattern}");
		p.setDefaultValueMethods("pattern");
		p.configure();
		PipeLineSession session = new PipeLineSession();

		session.put("sessionKeyForDefaultValue", "fakeDefaultValueSessionKey");
		session.put("sessionKeyForPattern", "fakePatternSessionKey");
		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		String result = (String)p.getValue(alreadyResolvedParameters, message, session, false);

		assertEquals("fakePatternSessionKey", result);
	}

	@Test
	public void testDefaultValueMethodValue() throws Exception {
		Parameter p = new Parameter();
		p.setXpathExpression("*/*");
		p.setValue("<doc/>");
		p.setDefaultValue("fakeDefaultValue");
		p.setSessionKey("sessionKeyForDefaultValue");
		p.setPattern("{sessionKeyForPattern}");
		p.setDefaultValueMethods("value");
		p.configure();
		PipeLineSession session = new PipeLineSession();

		session.put("sessionKeyForDefaultValue", "fakeDefaultValueSessionKey");
		session.put("sessionKeyForPattern", "fakePatternSessionKey");
		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		String result = (String)p.getValue(alreadyResolvedParameters, message, session, false);

		assertEquals("<doc/>", result);
	}

	@Test
	public void testDefaultValueMethodInput() throws Exception {
		Parameter p = new Parameter();
		p.setXpathExpression("*/*");
		p.setValue("<doc/>");
		p.setDefaultValue("fakeDefaultValue");
		p.setSessionKey("sessionKeyForDefaultValue");
		p.setPattern("{sessionKeyForPattern}");
		p.setDefaultValueMethods("input");
		p.configure();
		PipeLineSession session = new PipeLineSession();

		session.put("sessionKeyForDefaultValue", "fakeDefaultValueSessionKey");
		session.put("sessionKeyForPattern", "fakePatternSessionKey");
		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		String result = (String)p.getValue(alreadyResolvedParameters, message, session, false);

		assertEquals("fakeMessage", result);
	}

	@Test
	public void testDefaultValueMethodMulti() throws Exception {
		Parameter p = new Parameter();
		p.setXpathExpression("*/*");
		p.setValue("<doc/>");
		p.setDefaultValue("fakeDefaultValue");
		p.setPattern("{sessionKeyForPattern}");
		p.setDefaultValueMethods("sessionKey,value,pattern");
		p.configure();
		PipeLineSession session = new PipeLineSession();

		session.put("sessionKeyForDefaultValue", "fakeDefaultValueSessionKey");
		session.put("sessionKeyForPattern", "fakePatternSessionKey");
		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		String result = (String)p.getValue(alreadyResolvedParameters, message, session, false);

		assertEquals("<doc/>", result);
	}

	@Test
	public void testDefaultValueMethodMultiLoose() throws Exception {
		Parameter p = new Parameter();
		p.setXpathExpression("*/*");
		p.setValue("<doc/>");
		p.setDefaultValue("fakeDefaultValue");
		p.setPattern("{sessionKeyForPattern}");
		p.setDefaultValueMethods("SessionKey, VALUE, Pattern");
		p.configure();
		PipeLineSession session = new PipeLineSession();

		session.put("sessionKeyForDefaultValue", "fakeDefaultValueSessionKey");
		session.put("sessionKeyForPattern", "fakePatternSessionKey");
		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		String result = (String)p.getValue(alreadyResolvedParameters, message, session, false);

		assertEquals("<doc/>", result);
	}

	@Test
	public void testParameterFromNullMessage() throws Exception {
		Parameter p = ParameterBuilder.create().withName("parameter");
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();

		String result = (String)p.getValue(alreadyResolvedParameters, Message.nullMessage(), null, false);

		assertEquals(null, result);
	}

	@Test
	// see https://github.com/frankframework/frankframework/issues/3232
	public void testPotentialProblematicSysId() throws ConfigurationException {
		Parameter p = new Parameter();
		p.setName("pid");
		p.setXpathExpression("'#'"); // when this xpath expression is made part of the sysid, then an Exception occurs: '(TransformerException) Did not find the stylesheet root!'
		p.setXsltVersion(1);
		p.configure();
	}

	@Test
	public void testParameterfromStylesheetXsltVersion3() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setXsltVersion(3);
		p.setStyleSheetName("Param/ParameterXslt3.0.xsl");
		p.configure();

		Message input = new Message("<data>{\"pad\":{\"naar\":{\"wat\":{\"de\":{\"waarde\":{\"moet\":{\"zijn\":\"hallo\"}}}}}}}</data>");
		PipeLineSession session = new PipeLineSession();
		ParameterValueList alreadyResolvedParameters=new ParameterValueList();

		assertEquals("hallo", p.getValue(alreadyResolvedParameters, input, session, false));
	}
}
