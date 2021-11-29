package nl.nn.adapterframework.parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.w3c.dom.Document;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.parameters.Parameter.ParameterType;
import nl.nn.adapterframework.stream.Message;
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
	public void testPatternMessage() throws ConfigurationException, ParameterException {
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
	public void testParameterNumberBooelan() throws Exception {
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
	public void testParameterFromURLToDomdoc() throws Exception {
		URL originalMessage = TestFileUtils.getTestFileURL("/Xslt/AnyXml/in.xml");

		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", Message.asMessage(originalMessage));

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

}
