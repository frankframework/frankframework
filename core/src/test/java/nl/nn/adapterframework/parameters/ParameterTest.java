package nl.nn.adapterframework.parameters;

import static org.junit.Assert.assertEquals;
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.parameters.Parameter.ParameterType;
import nl.nn.adapterframework.pipes.PutInSession;
import nl.nn.adapterframework.processors.CorePipeLineProcessor;
import nl.nn.adapterframework.processors.CorePipeProcessor;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class ParameterTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

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

		exception.expectMessage("Parameter or session variable with name [unknown] in pattern [{unknown}] cannot be resolved");
		p.getValue(alreadyResolvedParameters, null, session, false);
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
	public void testParameterValueMessageStream() throws Exception {
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
		assertTrue(result instanceof InputStream);

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

		Map<String, String> map = new HashMap<String, String>();
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
		assertTrue(c+" is expected type but was: "+result.getClass(), c.isAssignableFrom(result.getClass()));

		PipeLineSession session = new PipeLineSession();
		session.put("sessionkey", 8);
		p = new Parameter();
		p.setName("integer");
		p.setSessionKey("sessionkey");
		p.setType(type);
		p.configure();

		result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(c+" is expected type but was: "+result.getClass(), c.isAssignableFrom(result.getClass()));

		session = new PipeLineSession();
		session.put("sessionkey", "8");

		result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(c+" is expected type but was: "+result.getClass(), c.isAssignableFrom(result.getClass()));

		session = new PipeLineSession();
		session.put("sessionkey", Message.asMessage(8));

		result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(c+" is expected type but was: "+result.getClass(), c.isAssignableFrom(result.getClass()));

		session = new PipeLineSession();
		session.put("sessionkey", "8".getBytes());

		result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(c+" is expected type but was: "+result.getClass(), c.isAssignableFrom(result.getClass()));

		session = new PipeLineSession();
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
		p.setType(ParameterType.NUMBER);
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
		assertTrue(c+" is expected type but was: "+result.getClass(), c.isAssignableFrom(result.getClass()));
	}

	@Test
	public void testParameterFromURLToDomdocWithXpath() throws Exception {
		URL originalMessage = TestFileUtils.getTestFileURL("/Xslt/MultiNamespace/in.xml");

		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", Message.asMessage(originalMessage));

		Parameter inputMessage = new Parameter();
		inputMessage.setName("InputMessage");
		inputMessage.setSessionKey("originalMessage");
		inputMessage.setType(ParameterType.DOMDOC);
		inputMessage.setRemoveNamespaces(true);
		inputMessage.setXpathExpression("block/XDOC[1]");
		inputMessage.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = inputMessage.getValue(alreadyResolvedParameters, message, session, true);
		assertTrue(result instanceof Document);
	}

	@Test
	public void testParameterFrombytesToDomdoc() throws Exception {
		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", "<someValue/>".getBytes());

		Parameter inputMessage = new Parameter();
		inputMessage.setName("InputMessage");
		inputMessage.setSessionKey("originalMessage");
		inputMessage.setType(ParameterType.DOMDOC);
		inputMessage.configure();

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = inputMessage.getValue(alreadyResolvedParameters, message, session, false);

		assertTrue(result instanceof Document);

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
			exit.setState("success");
			pipeline.registerPipeLineExit(exit);
			pipeline.configure();
	
			CorePipeLineProcessor cpp = configuration.createBean(CorePipeLineProcessor.class);
			CorePipeProcessor pipeProcessor = configuration.createBean(CorePipeProcessor.class);
			cpp.setPipeProcessor(pipeProcessor);
			PipeLineSession session = configuration.createBean(PipeLineSession.class);
			pipeline.setOwner(pipe);
			PipeLineResult pipeRunResult=cpp.processPipeLine(pipeline, "messageId", new Message(testMessage), session, firstPipe);
	
			assertEquals("success", pipeRunResult.getState());
			assertEquals(testMessage, pipeRunResult.getResult().asString());
			
			assertEquals(testMessageChild1, session.getMessage("xmlMessageChild").asString());
			assertEquals("X", session.getMessage("xmlMessageChild2").asString());
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
			System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "false");
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

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(result instanceof Date);

		Date resultDate = (Date) result;
		SimpleDateFormat sdf = new SimpleDateFormat(Parameter.TYPE_DATE_PATTERN);
		String formattedDate = sdf.format(resultDate);
		assertEquals("1995-01-23", formattedDate);
	}
}
