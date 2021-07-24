package nl.nn.adapterframework.parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;

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
		p.setUserName("fakeUsername");
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
		p.setType("list");
		p.setXpathExpression("items/item");
		p.configure();

		PipeLineSession session = new PipeLineSession();
		session.put(sessionKey, Arrays.asList(new String[] {"fiets", "bel", "appel"}));

		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(result instanceof String);

		String stringResult = Message.asMessage(result).asString();
		System.out.println(stringResult);
		assertEquals("fiets bel appel", stringResult);
	}

}
