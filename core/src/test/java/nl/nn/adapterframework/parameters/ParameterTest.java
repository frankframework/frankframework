package nl.nn.adapterframework.parameters;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.XmlUtils;

public class ParameterTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void testPatternUsername() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("{username}");
		p.setUserName("fakeUsername");
		p.configure();
		
		IPipeLineSession session = new PipeLineSessionBase();
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
		
		IPipeLineSession session = new PipeLineSessionBase();
		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		
		assertEquals("fakePassword", p.getValue(alreadyResolvedParameters, null, session, false));
	}

	@Test
	public void testPatternSessionVariable() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("{sessionKey}");
		p.configure();
		
		IPipeLineSession session = new PipeLineSessionBase();
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
		
		IPipeLineSession session = new PipeLineSessionBase();

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
		p.setUserName("fakeUsername");
		p.setPassword("fakePassword");
		p.configure();
		
		
		IPipeLineSession session = new PipeLineSessionBase();
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
	public void testPatternUnknownSessionVariableOrParameter() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("{unknown}");
		p.configure();
		
		IPipeLineSession session = new PipeLineSessionBase();
		
		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		
		exception.expectMessage("Parameter or session variable with name [unknown] in pattern [{unknown}] cannot be resolved");
		p.getValue(alreadyResolvedParameters, null, session, false);
	}

	@Test
	public void testPatternMessage() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.configure();
		
		IPipeLineSession session = new PipeLineSessionBase();
		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		
		Message message = new Message("fakeMessage");
		
		assertEquals("fakeMessage", p.getValue(alreadyResolvedParameters, message, session, false));
	}

	@Test
	public void testEmptyPatternUsedAsSourceForTransformation() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("{username}");
		p.setXpathExpression("root/username");
		p.setDefaultValue("fakeDefault");
		p.configure();
		PipeLineSessionBase session = new PipeLineSessionBase();
		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		
		assertEquals("fakeDefault", p.getValue(alreadyResolvedParameters, null, session, false));
	}

	@Test
	public void testEmptyDefault() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setSessionKey("dummy");
		p.setDefaultValue("");
		p.configure();
		
		IPipeLineSession session = new PipeLineSessionBase();
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

		IPipeLineSession session = new PipeLineSessionBase();
		session.put(sessionKey, new Message(sessionMessage));

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		assertEquals(sessionMessage, p.getValue(alreadyResolvedParameters, message, session, false));
	}

	@Test
	public void testParameterValueMessageStream() throws Exception {
		String sessionKey = "mySessionKey";
		String sessionMessage = "message goes here "+UUID.randomUUID();
		ByteArrayInputStream is = new ByteArrayInputStream(sessionMessage.getBytes());

		Parameter p = new Parameter();
		p.setName("readMyMessage");
		p.setSessionKey(sessionKey);
		p.configure();

		IPipeLineSession session = new PipeLineSessionBase();
		session.put(sessionKey, new Message(is));

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(result instanceof InputStream);

		assertEquals(sessionMessage, Message.asMessage(result).asString());
	}

	@Test
	public void testParameterValueList() throws Exception {
		String sessionKey = "mySessionKey";

		Parameter p = new Parameter();
		p.setName("myParameter");
		p.setSessionKey(sessionKey);
		p.setType("list");
		p.setXpathExpression("items/item");
		p.configure();

		IPipeLineSession session = new PipeLineSessionBase();
		session.put(sessionKey, Arrays.asList(new String[] {"fiets", "bel", "appel"}));

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(result instanceof String);

		String stringResult = Message.asMessage(result).asString();
		System.out.println(stringResult);
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

		PipeLineSessionBase session = new PipeLineSessionBase();

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

		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("emptySessionKey", "");

		Object result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertEquals("fakeDefault", Message.asMessage(result).asString());
	}

	@Test
	public void testParameterNumberBoolean() throws Exception {
		Parameter p = new Parameter();
		p.setName("number");
		p.setValue("a");
		p.setType(Parameter.TYPE_BOOLEAN);
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
		p.setType(Parameter.TYPE_MAP);
		p.configure();

		Map<String, String> map = new HashMap<String, String>();
		map.put("item", "value");
		map.put("item2", "value2");
		map.put("item3", "value3");
		map.put("item4", "value4");

		PipeLineSessionBase session = new PipeLineSessionBase();
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
		p.setType(Parameter.TYPE_NUMBER);
		p.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		assertThrows(ParameterException.class, () -> p.getValue(alreadyResolvedParameters, message, null, false));
	}

	@Test
	public void testParameterInteger() throws Exception {
		testParameterTypeHelper(Parameter.TYPE_INTEGER, Integer.class);
	}
	@Test
	public void testParameterNumber() throws Exception {
		testParameterTypeHelper(Parameter.TYPE_NUMBER, Number.class);
	}

	public <T> void testParameterTypeHelper(String type, Class<T> c) throws Exception {
		Parameter p = new Parameter();
		p.setName("integer");
		p.setValue("8");
		p.setType(type);
		p.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertTrue(c+" is expected type but was: "+result.getClass(), c.isAssignableFrom(result.getClass()));

		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("sessionkey", 8);
		p = new Parameter();
		p.setName("integer");
		p.setSessionKey("sessionkey");
		p.setType(type);
		p.configure();

		result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(c+" is expected type but was: "+result.getClass(), c.isAssignableFrom(result.getClass()));

		session = new PipeLineSessionBase();
		session.put("sessionkey", "8");

		result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(c+" is expected type but was: "+result.getClass(), c.isAssignableFrom(result.getClass()));

		session = new PipeLineSessionBase();
		session.put("sessionkey", Message.asMessage(8));

		result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(c+" is expected type but was: "+result.getClass(), c.isAssignableFrom(result.getClass()));

		session = new PipeLineSessionBase();
		session.put("sessionkey", "8".getBytes());

		result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(c+" is expected type but was: "+result.getClass(), c.isAssignableFrom(result.getClass()));

		session = new PipeLineSessionBase();
		session.put("sessionkey", Message.asMessage("8".getBytes()));

		result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(c+" is expected type but was: "+result.getClass(), c.isAssignableFrom(result.getClass()));

	}

	@Test
	public void testNumberWithLeftPadding() throws Exception {
		Parameter p = new Parameter();
		p.setName("number");
		p.setValue("8");
		p.setMinLength(10);
		p.setType(Parameter.TYPE_NUMBER);
		p.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertTrue("Expecting to be String but was:"+result.getClass(), result instanceof String);
		assertEquals("0000000008", (String) result);
	}

	@Test
	public void testNumberWithLeftPaddingAndMinExclusive() throws Exception {
		Parameter p = new Parameter();
		p.setName("number");
		p.setValue("3");
		p.setMinLength(10);
		p.setMinInclusive("5");
		p.setType(Parameter.TYPE_NUMBER);
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
		p.setType(Parameter.TYPE_NUMBER);
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
		p.setType(Parameter.TYPE_NUMBER);
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
		p.setType(Parameter.TYPE_NUMBER);
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
		testParameterFromURLToDomTypeHelper(Parameter.TYPE_DOMDOC, false, Document.class);
	}
	@Test
	public void testParameterFromURLToDomdocTypeRemoveNameSpace() throws Exception {
		testParameterFromURLToDomTypeHelper(Parameter.TYPE_DOMDOC, true, Document.class);
	}
	@Test
	public void testParameterFromURLToNodeTypeNoNameSpace() throws Exception {
		testParameterFromURLToDomTypeHelper(Parameter.TYPE_NODE, false, Node.class);
	}
	@Test
	public void testParameterFromURLToNodeTypeRemoveNameSpace() throws Exception {
		testParameterFromURLToDomTypeHelper(Parameter.TYPE_NODE, true, Node.class);
	}
	public <T> void testParameterFromURLToDomTypeHelper(String type, boolean removeNamespaces, Class<T> c) throws Exception {
		URL originalMessage = TestFileUtils.getTestFileURL("/Xslt/MultiNamespace/in.xml");

		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("originalMessage", Message.asMessage(originalMessage));

		Parameter parameter = new Parameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setType(type);
		parameter.setRemoveNamespaces(removeNamespaces);
		parameter.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(c+" is expected type but was: "+result.getClass(), c.isAssignableFrom(result.getClass()));
	}

	@Test
	public void testParameterFromURLToDomdocWithXpath() throws Exception {
		URL originalMessage = TestFileUtils.getTestFileURL("/Xslt/MultiNamespace/in.xml");

		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("originalMessage", Message.asMessage(originalMessage));

		Parameter parameter = new Parameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setType(Parameter.TYPE_DOMDOC);
		parameter.setRemoveNamespaces(true);
		parameter.setXpathExpression("block/XDOC[1]");
		parameter.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);
		assertTrue(result instanceof Document);
	}

	@Test
	public void testParameterFrombytesToDomdoc() throws Exception {
		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("originalMessage", "<someValue/>".getBytes());

		Parameter inputMessage = new Parameter();
		inputMessage.setName("InputMessage");
		inputMessage.setSessionKey("originalMessage");
		inputMessage.setType(Parameter.TYPE_DOMDOC);
		inputMessage.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = inputMessage.getValue(alreadyResolvedParameters, message, session, false);

		assertTrue(result instanceof Document);

	}

	@Test
	public void testParameterFromBytesToNode() throws Exception {
		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("originalMessage", "<someValue/>".getBytes());
		String expectedResultContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><someValue/>";

		Parameter parameter = new Parameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setType(Parameter.TYPE_NODE);
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

		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("originalMessage", domdoc);

		Parameter parameter = new Parameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setType(Parameter.TYPE_DOMDOC);
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

		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("originalMessage", domdoc);

		Parameter parameter = new Parameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setType(Parameter.TYPE_NODE);
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

		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("originalMessage", node);

		Parameter parameter = new Parameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setType(Parameter.TYPE_DOMDOC);
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

		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("originalMessage", node);

		Parameter parameter = new Parameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setType(Parameter.TYPE_NODE);
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

		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("originalMessage", date);

		Parameter parameter = new Parameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setType(Parameter.TYPE_DATE);
		parameter.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);
		assertThat(result,instanceOf(Date.class));
		
		assertEquals(date,result);
	}

	protected void testFromStringToDateType(String input, String expected, String typeDate) throws ConfigurationException, ParameterException {

		Parameter parameter = new Parameter();
		parameter.setName("InputMessage");
		parameter.setValue(input);
		parameter.setType(typeDate);
		parameter.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, null, true);
		assertThat(result,instanceOf(Date.class));
		
		assertEquals(expected,DateUtils.format((Date)result));
		
	}
	@Test
	public void testParameterFromStringToDate() throws Exception {
		String input = "2022-01-23";
		String expected = "2022-01-23 00:00:00.000";
		testFromStringToDateType(input, expected, Parameter.TYPE_DATE);
	}
	@Test
	public void testParameterFromStringToDateTime() throws Exception {
		String input = "2022-01-23 11:14:17";
		String expected = "2022-01-23 11:14:17.000";
		testFromStringToDateType(input, expected, Parameter.TYPE_DATETIME);
	}
	@Test
	public void testParameterFromStringToTimestamp() throws Exception {
		String input = "2022-01-23 11:14:17.123";
		String expected = "2022-01-23 11:14:17.123";
		testFromStringToDateType(input, expected, Parameter.TYPE_TIMESTAMP);
	}
	@Test
	public void testParameterFromStringToTime() throws Exception {
		String input = "11:14:17";
		String expected = "1970-01-01 11:14:17.000";
		testFromStringToDateType(input, expected, Parameter.TYPE_TIME);
	}
	@Test
	public void testParameterFromStringToXmlDateTime() throws Exception {
		String input = "2022-01-23T11:14:17";
		String expected = "2022-01-23 11:14:17.000";
		testFromStringToDateType(input, expected, Parameter.TYPE_XMLDATETIME);
	}

	@Test
	public void testParameterFromDateToXmlDateTime() throws Exception {
		Date date = new Date();

		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("originalMessage", date);

		Parameter parameter = new Parameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setType(Parameter.TYPE_XMLDATETIME);
		parameter.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);
		assertThat(result,instanceOf(Date.class));
		
		assertEquals(date,result);
	}

	@Test
	public void testFixedDate() throws Exception {
		Parameter p = new Parameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try {
			p.setName("date");
			p.setPattern("{fixedDate}");
			p.setType(Parameter.TYPE_DATE);
			p.configure();
			PipeLineSessionBase session = new PipeLineSessionBase();
	
			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");
	
			Object result = p.getValue(alreadyResolvedParameters, message, session, false); //Should return PutSystemDateInSession.FIXEDDATETIME
			assertTrue(result instanceof Date);
	
			Date resultDate = (Date) result;
			SimpleDateFormat sdf = new SimpleDateFormat(Parameter.TYPE_DATE_PATTERN);
			String formattedDate = sdf.format(resultDate);
			assertEquals("2001-12-17", formattedDate);

		} finally {
			System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "false");
		}
	}

	@Test
	public void testFixedDateWithSession() throws Exception {
		Parameter p = new Parameter();
		p.setName("date");
		p.setPattern("{fixedDate}");
		p.setType(Parameter.TYPE_DATE);
		p.configure();
		PipeLineSessionBase session = new PipeLineSessionBase();
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
		p.setType(Parameter.TYPE_DATE);
		p.configure();
		PipeLineSessionBase session = new PipeLineSessionBase();
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
			System.setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY,"false");
		}
	}

	@Test
	public void testFixedDateWithDateInSessionFromTesttool() throws Exception {
		Parameter p = new Parameter();
		p.setName("date");
		p.setPattern("{fixedDate}");
		p.setType(Parameter.TYPE_DATE);
		p.configure();
		PipeLineSessionBase session = new PipeLineSessionBase();
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
			System.setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY,"false");
		}
	}

	@Test
	public void testFixedDateWithDateObjectInSession() throws Exception {
		Parameter p = new Parameter();
		p.setName("date");
		p.setPattern("{fixedDate}");
		p.setType(Parameter.TYPE_DATE);
		p.configure();
		PipeLineSessionBase session = new PipeLineSessionBase();
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
			p.setType(Parameter.TYPE_DATE);
			p.configure();
			PipeLineSessionBase session = new PipeLineSessionBase();
	
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
			System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "false");
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
			PipeLineSessionBase session = new PipeLineSessionBase();
	
			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");
	
			Object result = p.getValue(alreadyResolvedParameters, message, session, false); //Should return PutSystemDateInSession.FIXEDDATETIME
			assertTrue(result instanceof String);
	
			SimpleDateFormat sdf = new SimpleDateFormat(DateUtils.FORMAT_FULL_GENERIC);
			String expectedDate = sdf.format(new Date()); // dit gaat echt meestal wel goed
			assertEquals(expectedDate.substring(0, 10), ((String)result).substring(0, 10));

		} finally {
			System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "false");
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
			PipeLineSessionBase session = new PipeLineSessionBase();
	
			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");
	
			Object result = p.getValue(alreadyResolvedParameters, message, session, false); //Should return PutSystemDateInSession.FIXEDDATETIME
			assertTrue(result instanceof String);
	
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			String expectedDate = sdf.format(new Date()); // dit gaat echt meestal wel goed
			assertEquals(expectedDate.substring(0, 10), ((String)result).substring(0, 10));

		} finally {
			System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "false");
		}
	}
	
	@Test
	public void testPatternFixedDateWithDateFormatType() throws Exception {
		String expectedDate = "2001-12-17T09:30:47";
		Parameter p = new Parameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try {
			p.setName("EsbSoapWrapperPipeTimestamp");
			p.setPattern("{fixeddate,date,yyyy-MM-dd'T'HH:mm:ss}");
			p.configure();
			PipeLineSessionBase session = new PipeLineSessionBase();
	
			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");
	
			Object result = p.getValue(alreadyResolvedParameters, message, session, false); //Should return PutSystemDateInSession.FIXEDDATETIME
			assertTrue(result instanceof String);
	
			SimpleDateFormat sdf = new SimpleDateFormat(DateUtils.FORMAT_FULL_GENERIC);
			assertEquals(expectedDate, result);

		} finally {
			System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "false");
		}
	}



	@Test
	public void testDefaultValueMethodDefaultNoDefaultValue() throws Exception {
		Parameter p = new Parameter();
		p.setXpathExpression("*/*");
		p.setValue("<doc/>");
		p.configure();
		PipeLineSessionBase session = new PipeLineSessionBase();

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
		PipeLineSessionBase session = new PipeLineSessionBase();

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
		PipeLineSessionBase session = new PipeLineSessionBase();

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
		PipeLineSessionBase session = new PipeLineSessionBase();

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
		PipeLineSessionBase session = new PipeLineSessionBase();

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
		PipeLineSessionBase session = new PipeLineSessionBase();

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
		PipeLineSessionBase session = new PipeLineSessionBase();

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
		PipeLineSessionBase session = new PipeLineSessionBase();

		session.put("sessionKeyForDefaultValue", "fakeDefaultValueSessionKey");
		session.put("sessionKeyForPattern", "fakePatternSessionKey");
		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		String result = (String)p.getValue(alreadyResolvedParameters, message, session, false);

		assertEquals("<doc/>", result);
	}
	
}
