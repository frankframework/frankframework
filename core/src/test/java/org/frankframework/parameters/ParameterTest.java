package org.frankframework.parameters;

import static org.frankframework.testutil.TestAssertions.assertJsonEquals;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationUtils;
import org.frankframework.core.Adapter;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.core.PipeLineExit;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.pipes.PutInSessionPipe;
import org.frankframework.pipes.PutSystemDateInSession;
import org.frankframework.processors.CorePipeLineProcessor;
import org.frankframework.processors.CorePipeProcessor;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;
import org.frankframework.stream.UrlMessage;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.ParameterBuilder;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.TimeProvider;
import org.frankframework.util.XmlUtils;

public class ParameterTest {

	@AfterEach
	void tearDown() {
		System.getProperties().remove(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY);
	}

	@Test
	public void testParameterWithHiddenValue() throws Exception {
		// NB: This test has been AI Generated
		Parameter p = new Parameter();
		p.setName("hiddenParam");
		p.setValue("secretValue");
		p.setHidden(true);
		p.configure();

		PipeLineSession session = new PipeLineSession();
		ParameterValueList alreadyResolvedParameters = new ParameterValueList();

		// The hidden flag doesn't affect the actual value, only how it's logged
		assertEquals("secretValue", p.getValue(alreadyResolvedParameters, null, session, false));
		assertTrue(p.isHidden());
	}

	@Test
	public void testParameterWithMode() throws Exception {
		// NB: This test has been AI Generated
		// Test INPUT mode (default)
		Parameter inputParam = new Parameter();
		inputParam.setName("inputParam");
		inputParam.setValue("inputValue");
		inputParam.configure();
		assertEquals(AbstractParameter.ParameterMode.INPUT, inputParam.getMode());

		// Test OUTPUT mode
		Parameter outputParam = new Parameter();
		outputParam.setName("outputParam");
		outputParam.setMode(AbstractParameter.ParameterMode.OUTPUT);
		outputParam.configure();
		assertEquals(AbstractParameter.ParameterMode.OUTPUT, outputParam.getMode());

		// Test INOUT mode
		Parameter inoutParam = new Parameter();
		inoutParam.setName("inoutParam");
		inoutParam.setValue("inoutValue");
		inoutParam.setMode(AbstractParameter.ParameterMode.INOUT);
		inoutParam.configure();
		assertEquals(AbstractParameter.ParameterMode.INOUT, inoutParam.getMode());
	}

	@Test
	public void testParameterWithSessionKeyXPath() throws Exception {
		// NB: This test has been AI Generated
		// Create an XML message with a session key name
		String xmlMessage = "<root><sessionKeyName>dynamicKey</sessionKeyName></root>";
		Message message = new Message(xmlMessage);

		// Create a parameter with sessionKeyXPath to extract the key name
		Parameter p = new Parameter();
		p.setName("dynamicSessionKeyParam");
		p.setSessionKeyXPath("root/sessionKeyName");
		p.configure();

		// Set up session with the dynamic key
		PipeLineSession session = new PipeLineSession();
		session.put("dynamicKey", "dynamicValue");

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();

		// The parameter should use the XPath to get the session key name, then use that to get the value
		assertEquals("dynamicValue", p.getValue(alreadyResolvedParameters, message, session, false));

		// Verify it requires input value for resolution (since it uses the message for XPath)
		assertTrue(p.requiresInputValueForResolution());
	}

	@Test
	public void testParameterWithSessionKeyJPath() throws Exception {
		// NB: This test has been AI Generated
		// Create a JSON message with a session key name
		String jsonMessage = "{\"root\":{\"sessionKeyName\":\"jsonDynamicKey\"}}";
		Message message = new Message(jsonMessage);

		// Create a parameter with sessionKeyJPath to extract the key name
		Parameter p = new Parameter();
		p.setName("dynamicJsonSessionKeyParam");
		p.setSessionKeyJPath("$.root.sessionKeyName");
		p.configure();

		// Set up session with the dynamic key
		PipeLineSession session = new PipeLineSession();
		session.put("jsonDynamicKey", "jsonDynamicValue");

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();

		// The parameter should use the JSONPath to get the session key name, then use that to get the value
		assertEquals("jsonDynamicValue", p.getValue(alreadyResolvedParameters, message, session, false));

		// Verify it requires input value for resolution (since it uses the message for JSONPath)
		assertTrue(p.requiresInputValueForResolution());
	}

	@Test
	public void testPatternUsername() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("{username}");
		p.setUsername("fakeUsername");
		p.configure();

		PipeLineSession session = new PipeLineSession();
		ParameterValueList alreadyResolvedParameters = new ParameterValueList();

		assertEquals("fakeUsername", p.getValue(alreadyResolvedParameters, null, session, false));

		assertFalse(p.requiresInputValueForResolution());
		assertFalse(p.consumesSessionVariable("test"));
	}

	@Test
	public void testPatternPassword() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("{password}");
		p.setPassword("fakePassword");
		p.configure();

		PipeLineSession session = new PipeLineSession();
		ParameterValueList alreadyResolvedParameters = new ParameterValueList();

		assertEquals("fakePassword", p.getValue(alreadyResolvedParameters, null, session, false));

		assertFalse(p.requiresInputValueForResolution());
	}

	@Test // Should use input value
	public void testMessageNull() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.configure();

		PipeLineSession session = new PipeLineSession();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();

		assertNull(p.getValue(alreadyResolvedParameters, null, session, false));

		assertTrue(p.requiresInputValueForResolution());
	}

	@Test
	public void testPatternSessionVariable() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("{sessionKey}");
		p.configure();

		PipeLineSession session = new PipeLineSession();
		session.put("sessionKey", "fakeSessionVariable");

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();

		assertEquals("fakeSessionVariable", p.getValue(alreadyResolvedParameters, null, session, false));

		assertFalse(p.requiresInputValueForResolution());
		assertTrue(p.consumesSessionVariable("sessionKey"));
	}

	@Test
	public void testPatternSessionVariableValueIsMessage() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("{sessionKey}");
		p.configure();

		PipeLineSession session = new PipeLineSession();
		session.put("sessionKey", new Message("fakeSessionVariable"));

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();

		assertEquals("fakeSessionVariable", p.getValue(alreadyResolvedParameters, null, session, false));

		assertFalse(p.requiresInputValueForResolution());
		assertTrue(p.consumesSessionVariable("sessionKey"));
	}

	@Test
	public void testPatternParameter() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("{siblingParameter}");
		p.configure();

		PipeLineSession session = new PipeLineSession();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Parameter siblingParameter = new Parameter();
		siblingParameter.setName("siblingParameter");
		siblingParameter.setValue("fakeParameterValue");
		siblingParameter.configure();
		alreadyResolvedParameters.add(new ParameterValue(siblingParameter, siblingParameter.getValue(alreadyResolvedParameters, null, session, false)));

		assertEquals("fakeParameterValue", p.getValue(alreadyResolvedParameters, null, session, false));

		assertFalse(p.requiresInputValueForResolution());
	}

	@Test
	public void testPatternParameterWithMessageAsValue() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("{siblingParameter}");
		p.configure();

		PipeLineSession session = new PipeLineSession();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Parameter siblingParameter = new Parameter();
		siblingParameter.setName("siblingParameter");
		siblingParameter.configure();
		Message input = new Message("valueFromMessage");
		alreadyResolvedParameters.add(new ParameterValue(siblingParameter, siblingParameter.getValue(alreadyResolvedParameters, input, session, false)));

		assertEquals("valueFromMessage", p.getValue(alreadyResolvedParameters, null, session, false));

		assertFalse(p.requiresInputValueForResolution());
	}

	@Test
	public void testPatternCombinedSibling() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("param [{siblingParameter}] sessionKey [{sessionKey}] username [{username}] password [{password}]");
		p.setUsername("fakeUsername");
		p.setPassword("fakePassword");
		p.configure();

		PipeLineSession session = new PipeLineSession();
		session.put("sessionKey", "fakeSessionVariable");

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Parameter siblingParameter = new Parameter();
		siblingParameter.setName("siblingParameter");
		siblingParameter.setValue("fakeParameterValue");
		siblingParameter.configure();
		alreadyResolvedParameters.add(new ParameterValue(siblingParameter, siblingParameter.getValue(alreadyResolvedParameters, null, session, false)));

		assertEquals("param [fakeParameterValue] sessionKey [fakeSessionVariable] username [fakeUsername] password [fakePassword]", p.getValue(alreadyResolvedParameters, null, session, false));

		assertFalse(p.requiresInputValueForResolution());
		assertTrue(p.consumesSessionVariable("sessionKey"));
	}

	@Test
	public void testPatternCombinedParent() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("param [{siblingParameter}] sessionKey [{sessionKey}] username [{username}] password [{password}]");
		p.setUsername("fakeUsername");
		p.setPassword("fakePassword");

		PipeLineSession session = new PipeLineSession();
		session.put("sessionKey", "fakeSessionVariable");

		Parameter siblingParameter = spy(new Parameter());
		siblingParameter.setName("siblingParameter");
		siblingParameter.setValue("fakeParameterValue");
		p.addParameter(siblingParameter);

		p.configure();
		verify(siblingParameter, times(1)).configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		alreadyResolvedParameters.add(new ParameterValue(siblingParameter, siblingParameter.getValue(alreadyResolvedParameters, null, session, false)));

		assertEquals("param [fakeParameterValue] sessionKey [fakeSessionVariable] username [fakeUsername] password [fakePassword]", p.getValue(alreadyResolvedParameters, null, session, false));

		assertFalse(p.requiresInputValueForResolution());
		assertTrue(p.consumesSessionVariable("sessionKey"));
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
		ParameterValueList alreadyResolvedParameters = new ParameterValueList();

		assertEquals("fakeUsername", p.getValue(alreadyResolvedParameters, null, session, false));

		assertFalse(p.requiresInputValueForResolution());
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
		ParameterValueList alreadyResolvedParameters = new ParameterValueList();

		assertEquals("fakeDefault", p.getValue(alreadyResolvedParameters, null, session, false));

		assertFalse(p.requiresInputValueForResolution());
	}

	@Test
	public void testPatternUnknownSessionVariableOrParameter() throws ConfigurationException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("{unknown}");
		p.configure();

		PipeLineSession session = new PipeLineSession();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();

		ParameterException e = assertThrows(ParameterException.class, () -> p.getValue(alreadyResolvedParameters, null, session, false));
		assertEquals("Parameter or session variable with name [unknown] in pattern [{unknown}] cannot be resolved", e.getMessage());

		assertFalse(p.requiresInputValueForResolution());
	}

	@Test
	public void testPatternUnknownSessionVariableOrParameterSilentlyIgnored() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("{unknown}");
		p.setIgnoreUnresolvablePatternElements(true);
		p.configure();

		PipeLineSession session = new PipeLineSession();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();

		assertEquals("", p.getValue(alreadyResolvedParameters, null, session, false));

		assertFalse(p.requiresInputValueForResolution());
	}

	@Test
	public void testContextKey() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setContextKey("fakeContextKey");
		p.configure();

		Message input = new Message("fakeMessage", new MessageContext().with("fakeContextKey", "fakeContextValue"));
		PipeLineSession session = new PipeLineSession();
		ParameterValueList alreadyResolvedParameters = new ParameterValueList();

		assertEquals("fakeContextValue", p.getValue(alreadyResolvedParameters, input, session, false));

		assertFalse(p.requiresInputValueForResolution());
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
		ParameterValueList alreadyResolvedParameters = new ParameterValueList();

		assertEquals("fakeContextValue2", p.getValue(alreadyResolvedParameters, input, session, false));

		assertFalse(p.requiresInputValueForResolution());
		assertFalse(p.consumesSessionVariable("fakeSessionKey"));
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
		ParameterValueList alreadyResolvedParameters = new ParameterValueList();

		assertEquals("2", p.getValue(alreadyResolvedParameters, input, session, false));

		assertFalse(p.requiresInputValueForResolution());
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
		ParameterValueList alreadyResolvedParameters = new ParameterValueList();

		assertEquals("3", p.getValue(alreadyResolvedParameters, input, session, false));

		assertFalse(p.requiresInputValueForResolution());
		assertFalse(p.consumesSessionVariable("fakeSessionKey"));
	}

	@Test
	public void testEmptyParameterResolvesToMessage() throws Exception {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.configure();

		PipeLineSession session = new PipeLineSession();
		ParameterValueList alreadyResolvedParameters = new ParameterValueList();

		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, session, false);
		Message msg = assertInstanceOf(Message.class, result);
		assertEquals("fakeMessage", msg.asString());

		assertTrue(p.requiresInputValueForResolution());
	}

	@Test
	public void testValueSetEmptyDoesNotResolveToMessage() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setValue("");
		p.configure();

		PipeLineSession session = new PipeLineSession();
		ParameterValueList alreadyResolvedParameters = new ParameterValueList();

		Message message = new Message("fakeMessage");

		assertEquals("", p.getValue(alreadyResolvedParameters, message, session, false));

		assertTrue(p.requiresInputValueForResolution());
		assertFalse(p.consumesSessionVariable("test"));
	}

	@Test
	public void testValueSetEmptyDoesResolveToMessageWhenDefaultValueIsSetToInput() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setValue("");
		p.setDefaultValueMethods("input");
		p.configure();

		PipeLineSession session = new PipeLineSession();
		ParameterValueList alreadyResolvedParameters = new ParameterValueList();

		Message message = new Message("fakeMessage");

		assertEquals("fakeMessage", p.getValue(alreadyResolvedParameters, message, session, false));

		assertTrue(p.requiresInputValueForResolution());
		assertFalse(p.consumesSessionVariable("test"));
	}

	@Test
	public void testEmptyDefault() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setSessionKey("dummy");
		p.setDefaultValue("");
		p.configure();

		PipeLineSession session = new PipeLineSession();
		ParameterValueList alreadyResolvedParameters = new ParameterValueList();

		Message message = new Message("fakeMessage");

		assertEquals("", p.getValue(alreadyResolvedParameters, message, session, false));

		assertFalse(p.requiresInputValueForResolution());
	}

	@Test
	public void testParameterValueMessageString() throws Exception {
		String sessionKey = "mySessionKey";
		String sessionMessage = "message goes here " + UUID.randomUUID();

		Parameter p = new Parameter();
		p.setName("readMyMessage");
		p.setSessionKey(sessionKey);
		p.configure();

		PipeLineSession session = new PipeLineSession();
		session.put(sessionKey, new Message(sessionMessage));

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, session, false);
		Message msg = assertInstanceOf(Message.class, result);
		assertEquals(sessionMessage, msg.asString());

		assertFalse(p.requiresInputValueForResolution());
		assertTrue(p.consumesSessionVariable(sessionKey));
		assertFalse(p.consumesSessionVariable("test"));
	}

	@Test
	public void testParameterValueMessage() throws Exception {
		String sessionKey = "mySessionKey";
		String sessionMessage = "message goes here " + UUID.randomUUID();
		ByteArrayInputStream is = new ByteArrayInputStream(sessionMessage.getBytes());

		Parameter p = new Parameter();
		p.setName("readMyMessage");
		p.setSessionKey(sessionKey);
		p.configure();

		PipeLineSession session = new PipeLineSession();
		session.put(sessionKey, new Message(is));

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertInstanceOf(Message.class, result);

		assertEquals(sessionMessage, Message.asMessage(result).asString());

		assertFalse(p.requiresInputValueForResolution());
		assertTrue(p.consumesSessionVariable(sessionKey));
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
		session.put(sessionKey, Arrays.asList(new String[] { "fiets", "bel", "appel" }));

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertInstanceOf(String.class, result);

		String stringResult = Message.asMessage(result).asString();
		assertEquals("fiets bel appel", stringResult);

		assertFalse(p.requiresInputValueForResolution());
		assertTrue(p.consumesSessionVariable(sessionKey));
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
	public void testParameterXPathToMessage() throws Exception {
		Parameter p = new Parameter();
		p.setName("number");
		p.setXpathExpression("/dummy");
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("<dummy>a</dummy>");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertEquals("a", Message.asMessage(result).asString());

		assertTrue(p.requiresInputValueForResolution());
		assertFalse(p.consumesSessionVariable("test"));
	}

	@Test
	public void testParameterXPathToMessageNoMatchOnXpath() throws Exception {
		Parameter p = new Parameter();
		p.setName("number");
		p.setDefaultValue("fakeDefault");
		p.setXpathExpression("/notfound");
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("<dummy>a</dummy>");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertEquals("fakeDefault", Message.asMessage(result).asString());

		assertTrue(p.requiresInputValueForResolution());
	}

	@Test
	public void testParameterInvalidXPathToMessage() throws Exception {
		Parameter p = new Parameter();
		p.setName("number");
		p.setXpathExpression("{/}not-an-xpath");

		assertThrows(ConfigurationException.class, () -> p.configure());
	}

	@Test
	public void testParameterXPathToNonXmlMessage() throws Exception {
		Parameter p = new Parameter();
		p.setName("number");
		p.setDefaultValue("fakeDefault");
		p.setXpathExpression("/dummy");
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("{\"dummy\": \"a\"}");

		assertThrows(ParameterException.class, () -> p.getValue(alreadyResolvedParameters, message, null, false));
	}

	@Test
	public void testParameterXPathToEmptyMessage() throws Exception {
		Parameter p = new Parameter();
		p.setName("number");
		p.setDefaultValue("fakeDefault");
		p.setXpathExpression("/dummy");
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = Message.nullMessage();

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertEquals("fakeDefault", Message.asMessage(result).asString());
	}

	@Test
	public void testParameterXPathToNullMessage() throws Exception {
		Parameter p = new Parameter();
		p.setName("number");
		p.setDefaultValue("fakeDefault");
		p.setXpathExpression("/dummy");
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();

		Object result = p.getValue(alreadyResolvedParameters, null, null, false);
		assertEquals("fakeDefault", Message.asMessage(result).asString());
	}

	@Test
	public void testParameterXPathUnknownSessionKey() throws Exception {
		Parameter p = new Parameter();
		p.setName("number");
		p.setSessionKey("unknownSessionKey");
		p.setXpathExpression("/dummy");
		p.setDefaultValue("fakeDefault");
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
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

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		PipeLineSession session = new PipeLineSession();
		session.put("emptySessionKey", "");

		Object result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertEquals("fakeDefault", Message.asMessage(result).asString());

		assertFalse(p.requiresInputValueForResolution());
		assertTrue(p.consumesSessionVariable("emptySessionKey"));
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

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertInstanceOf(String.class, result);

		String stringResult = Message.asMessage(result).asString();
		assertEquals("value value2 value4 value3", stringResult);
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
		assertInstanceOf(String.class, result);
		assertEquals("abcde", (String) result);
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
		assertInstanceOf(String.class, result);
		assertEquals("abcde", (String) result);
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
		assertInstanceOf(String.class, result);
		assertEquals("abcdefghijklmnop", (String) result);
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
		assertInstanceOf(String.class, result);
		assertEquals("abcde          ", (String) result);
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
		assertInstanceOf(String.class, result);
		assertEquals("abcdefg", (String) result);
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

			assertInstanceOf(String.class, result);
			assertEquals("abcdefg", (String) result);
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

			assertInstanceOf(byte[].class, result);
			assertEquals("abcdefghijklmnop", new String((byte[]) result));
		}
	}

	@Test
	public void testParameterFromURLToDomdocWithXpath() throws Exception {
		URL originalMessage = TestFileUtils.getTestFileURL("/Xslt/MultiNamespace/in.xml");
		String expectedResultContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><block><XDOC><REF_ID>0</REF_ID><XX>0</XX></XDOC><XDOC><REF_ID>1</REF_ID></XDOC><XDOC><REF_ID>2</REF_ID></XDOC></block>";
		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", new UrlMessage(originalMessage));

		Parameter parameter = new Parameter();
		parameter.setName("InputMessage");
		parameter.setSessionKey("originalMessage");
		parameter.setType(ParameterType.DOMDOC);
		parameter.setRemoveNamespaces(true);
		parameter.setXpathExpression("*");
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);
		assertThat(result, instanceOf(Document.class));

		String contents = XmlUtils.transformXml(TransformerFactory.newInstance().newTransformer(), new DOMSource((Document) result));
		assertEquals(expectedResultContents, contents);
	}

	@Test
	public void testParameterFromURLToNodeWithXpath() throws Exception {
		URL originalMessage = TestFileUtils.getTestFileURL("/Xslt/MultiNamespace/in.xml");
		String expectedResultContents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><block><XDOC><REF_ID>0</REF_ID><XX>0</XX></XDOC><XDOC><REF_ID>1</REF_ID></XDOC><XDOC><REF_ID>2</REF_ID></XDOC></block>";
		PipeLineSession session = new PipeLineSession();
		session.put("originalMessage", new UrlMessage(originalMessage));

		Parameter parameter = new Parameter();
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

		String contents = XmlUtils.transformXml(TransformerFactory.newInstance().newTransformer(), new DOMSource((Node) result));
		assertEquals(expectedResultContents, contents);
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

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);
		assertThat(result, instanceOf(Document.class));

		String contents = XmlUtils.transformXml(TransformerFactory.newInstance().newTransformer(), new DOMSource((Document) result));
		assertEquals(expectedResultContents, contents);

		assertFalse(parameter.requiresInputValueForResolution());
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

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);

		assertInstanceOf(Node.class, result);
		assertThat(result, not(instanceOf(Document.class)));

		String contents = XmlUtils.transformXml(TransformerFactory.newInstance().newTransformer(), new DOMSource((Node) result));
		assertEquals(expectedResultContents, contents);

		assertFalse(parameter.requiresInputValueForResolution());
		assertTrue(parameter.consumesSessionVariable("originalMessage"));
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
			Parameter p = new Parameter();
			p.setName("xmlMessageChild");
			p.setXpathExpression("Test/Child[1]");
			p.setType(ParameterType.DOMDOC);
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

			MatchUtils.assertXmlEquals(testMessageChild1, session.getString("xmlMessageChild"));
			assertEquals("X", session.getString("xmlMessageChild2"));
		}
	}

	@Test
	public void testPatternNowWithStringType() throws Exception {
		Parameter p = new Parameter();
		TimeProvider.setTime(LocalDateTime.of(2025, 3, 5, 22, 22, 22));
		try (PipeLineSession session = new PipeLineSession()) {
			p.setName("date");
			p.setPattern("{now}");
			p.configure();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false);
			assertInstanceOf(String.class, result);

			SimpleDateFormat sdf = new SimpleDateFormat(DateFormatUtils.FORMAT_FULL_GENERIC);
			String expectedDate = sdf.format(TimeProvider.nowAsDate());
			assertEquals(expectedDate.substring(0, 10), ((String) result).substring(0, 10));
		}
	}

	@Test
	public void testPatternNowWithDateFormatType() throws Exception {
		Parameter p = new Parameter();
		TimeProvider.setTime(LocalDateTime.of(2025, 3, 5, 22, 22, 22));
		try (PipeLineSession session = new PipeLineSession()) {
			p.setName("EsbSoapWrapperPipeTimestamp");
			p.setPattern("{now,date,yyyy-MM-dd'T'HH:mm:ss}");
			p.configure();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false);
			String resultString = assertInstanceOf(String.class, result);

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			String expectedDate = sdf.format(TimeProvider.nowAsDate());
			assertEquals(expectedDate, resultString);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "{uid}", "{uuid}" })
	public void testUidPattern(String pattern) throws Exception {
		Parameter p = new Parameter();
		p.setName(pattern);
		p.setPattern(pattern + "-message");
		p.configure();
		PipeLineSession session = new PipeLineSession();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertNotNull(result);
		assertInstanceOf(String.class, result, "class was not a String --> "+result.getClass());
		assertTrue(((String) result).length() > 40);
		assertTrue(((String) result).endsWith("-message"));
		assertFalse(p.requiresInputValueForResolution());
	}

	@Test
	public void testPatternFixedDateWithDateFormatType() throws Exception {
		String expectedDate = "2001-12-17T09:30";
		Parameter p = new Parameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try (PipeLineSession session = new PipeLineSession()) {
			p.setName("EsbSoapWrapperPipeTimestamp");
			p.setPattern("{fixeddate,date,yyyy-MM-dd'T'HH:mm}");
			p.configure();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false); // Should return PutSystemDateInSession.FIXEDDATETIME
			assertInstanceOf(String.class, result);
			assertEquals(expectedDate, result);
		}
	}

	@Test
	public void testPatternFixedDateWithUnixTimestamp() throws Exception {
		TimeProvider.setTime(1747401948_000L);
		Parameter p = new Parameter();
		try (PipeLineSession session = new PipeLineSession()) {
			p.setName("unixTimestamp");
			p.setPattern("{now,millis,#}");
			p.configure();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false);
			Long millis = assertDoesNotThrow(() -> Long.parseLong(result.toString()));
			assertEquals(1747401948_000L, millis);
		}
	}

	@Test
	public void testPatternFixedDateWithUnixTimestampNoHashInFormat() throws Exception {
		TimeProvider.setTime(1747401948_000L);
		Parameter p = new Parameter();
		try (PipeLineSession session = new PipeLineSession()) {
			p.setName("unixTimestamp");
			p.setPattern("{now,millis}");
			p.configure();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false);
			Long millis = assertDoesNotThrow(() -> Long.parseLong(result.toString()));
			assertEquals(1747401948_000L, millis);
		}
	}

	@Test
	public void testPatternFixedDateWithExtendedDateFormatType() throws Exception {
		String expectedDate = "2001-12-17 09:30:47.000";
		Parameter p = new Parameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try (PipeLineSession session = new PipeLineSession()) {
			p.setName("EsbSoapWrapperPipeTimestamp");
			p.setPattern("{fixeddate,date,yyyy-MM-dd HH:mm:ss.SSS}");
			p.configure();

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false); // Should return PutSystemDateInSession.FIXEDDATETIME
			assertInstanceOf(String.class, result);
			assertEquals(expectedDate, result);
		}
	}

	@Test
	public void testPatternFixedDateFromSession() throws Exception {
		Parameter p = new Parameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try (PipeLineSession session = new PipeLineSession()) {
			String patternFormatString = "yyyy-MM-dd HH:mm:ss.SSS";

			p.setName("EsbSoapWrapperPipeTimestamp");
			p.setPattern("{fixeddate,date," + patternFormatString + "}");
			p.configure();

			Date expectedDate = TimeProvider.nowAsDate();
			DateFormat df = new SimpleDateFormat(patternFormatString);
			String expectedDateAsString = df.format(expectedDate);

			session.put(PutSystemDateInSession.FIXEDDATE_STUB4TESTTOOL_KEY, expectedDate);

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false);
			assertInstanceOf(String.class, result);
			assertEquals(expectedDateAsString, result);
		}
	}

	@Test
	public void testPatternFixedDateAsStringFromSession() throws Exception {
		Parameter p = new Parameter();
		System.getProperties().setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		try (PipeLineSession session = new PipeLineSession()) {
			String patternFormatString = "yyyy-MM-dd HH:mm:ss.SSS";

			p.setName("EsbSoapWrapperPipeTimestamp");
			p.setPattern("{fixeddate,date," + patternFormatString + "}");
			p.configure();

			Date expectedDate = TimeProvider.nowAsDate();
			DateFormat df = new SimpleDateFormat(patternFormatString);
			String expectedDateAsString = df.format(expectedDate);

			session.put(PutSystemDateInSession.FIXEDDATE_STUB4TESTTOOL_KEY, expectedDateAsString);

			ParameterValueList alreadyResolvedParameters = new ParameterValueList();
			Message message = new Message("fakeMessage");

			Object result = p.getValue(alreadyResolvedParameters, message, session, false); // Should return PutSystemDateInSession.FIXEDDATETIME
			assertInstanceOf(String.class, result);
			assertEquals(expectedDateAsString, result);
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

		String result = (String) p.getValue(alreadyResolvedParameters, message, session, false);

		assertNull(result);
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

		String result = (String) p.getValue(alreadyResolvedParameters, message, session, false);

		assertEquals("fakeDefaultValue", result);

		assertFalse(p.requiresInputValueForResolution());
		assertTrue(p.consumesSessionVariable("sessionKeyForDefaultValue"));
		assertTrue(p.consumesSessionVariable("sessionKeyForPattern"));
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

		String result = (String) p.getValue(alreadyResolvedParameters, message, session, false);

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

		String result = (String) p.getValue(alreadyResolvedParameters, message, session, false);

		assertEquals("fakePatternSessionKey", result);

		assertFalse(p.requiresInputValueForResolution());
		assertTrue(p.consumesSessionVariable("sessionKeyForDefaultValue"));
		assertTrue(p.consumesSessionVariable("sessionKeyForPattern"));
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

		String result = (String) p.getValue(alreadyResolvedParameters, message, session, false);

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

		String result = (String) p.getValue(alreadyResolvedParameters, message, session, false);

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

		String result = (String) p.getValue(alreadyResolvedParameters, message, session, false);

		assertEquals("<doc/>", result);

		assertFalse(p.requiresInputValueForResolution());
		assertFalse(p.consumesSessionVariable("sessionKeyForDefaultValue"));
		assertTrue(p.consumesSessionVariable("sessionKeyForPattern"));
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

		String result = (String) p.getValue(alreadyResolvedParameters, message, session, false);

		assertEquals("<doc/>", result);
	}

	@Test
	public void testParameterFromNullMessage() throws Exception {
		Parameter p = ParameterBuilder.create().withName("parameter");
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();

		Object result = p.getValue(alreadyResolvedParameters, Message.nullMessage(), null, false);

		assertNull(result);
	}

	@Test
	// see https://github.com/frankframework/frankframework/issues/3232
	public void testPotentialProblematicSysId() {
		Parameter p = new Parameter();
		p.setName("pid");
		p.setXpathExpression("'#'"); // when this xpath expression is made part of the sysid, then an Exception
										// occurs: '(TransformerException) Did not find the stylesheet root!'
		p.setXsltVersion(1);

		assertDoesNotThrow(p::configure);
	}

	@Test
	public void testParameterFromStylesheetXsltVersion3() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setXsltVersion(3);
		p.setStyleSheetName("Param/ParameterXslt3.0.xsl");
		p.configure();

		Message input = new Message("<data>{\"pad\":{\"naar\":{\"wat\":{\"de\":{\"waarde\":{\"moet\":{\"zijn\":\"hallo\"}}}}}}}</data>");
		PipeLineSession session = new PipeLineSession();
		ParameterValueList alreadyResolvedParameters = new ParameterValueList();

		assertEquals("hallo", p.getValue(alreadyResolvedParameters, input, session, false));
	}

	@Test
	public void testParameterWithTimePattern() throws Exception {

		// Arrange
		Parameter parameter = new Parameter();
		parameter.setName("time");
		parameter.setPattern("{now,time,HH:mm}");
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");
		PipeLineSession session = new PipeLineSession();

		// Act
		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);

		// Assert
		String stringResult = assertInstanceOf(String.class, result);
		assertTrue((stringResult).matches("\\d{1,2}:\\d{2}"));
	}

	@Test
	public void testParameterWithDatePattern() throws Exception {

		// Arrange
		Parameter parameter = new Parameter();
		parameter.setName("time");
		parameter.setPattern("{now,date,HH:mm}");
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");
		PipeLineSession session = new PipeLineSession();

		// Act
		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);

		// Assert
		String stringResult = assertInstanceOf(String.class, result);
		assertTrue((stringResult).matches("\\d{1,2}:\\d{2}"));
	}

	@Test
	public void testParameterWithJsonPathExpressionValueFromSessionKey() throws Exception {
		// Arrange
		Parameter parameter = new Parameter();
		parameter.setName("p1");
		parameter.setJsonPathExpression("$.root.a");
		parameter.setSessionKey("sessionKey");
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");
		PipeLineSession session = new PipeLineSession();
		session.put("sessionKey", """
				{
				  "root": {
				    "a": "v1"
				  }
				}
				""");

		// Act
		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);

		// Assert
		assertEquals("v1", result);
	}

	@Test
	public void testParameterWithJsonPathExpressionValueFromSessionKeyAndContextKey() throws Exception {
		// Arrange
		Parameter parameter = new Parameter();
		parameter.setName("p1");
		parameter.setJsonPathExpression("$.root.a");
		parameter.setSessionKey("sessionKey");
		parameter.setContextKey("ctx");
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");
		message.getContext().put("ctx", """
				{
				  "root": {
				    "a": "v1"
				  }
				}
				""");
		PipeLineSession session = new PipeLineSession();
		session.put("sessionKey",message);

		// Act
		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);

		// Assert
		assertEquals("v1", result);
	}

	@Test
	public void testParameterWithJsonPathExpressionFromValue() throws Exception {
		// Arrange
		Parameter parameter = new Parameter();
		parameter.setName("p1");
		parameter.setJsonPathExpression("$.root.a");
		parameter.setSessionKey("sessionKey");
		parameter.setContextKey("ctx");
		parameter.setValue( """
				{
				  "root": {
				    "a": "v1"
				  }
				}
				""");
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");
		message.getContext().put("ctx", """
				{
				  "root": {
				    "a": "wrongOutcome"
				  }
				}
				""");
		PipeLineSession session = new PipeLineSession();
		session.put("sessionKey",message);

		// Act
		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);

		// Assert
		assertEquals("v1", result);
	}

	@Test
	public void testParameterWithJsonPathExpressionValueFromMessage() throws Exception {
		// Arrange
		Parameter parameter = new Parameter();
		parameter.setName("p1");
		parameter.setJsonPathExpression("$.root.a");
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("""
				{
				  "root": {
				    "a": "v1"
				  }
				}
				""");
		PipeLineSession session = new PipeLineSession();

		// Act
		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);

		// Assert
		assertEquals("v1", result);
	}

	@Test
	public void testParameterWithJsonPathExpressionValueIsJsonObject() throws Exception {
		// Arrange
		Parameter parameter = new Parameter();
		parameter.setName("p1");
		parameter.setJsonPathExpression("$.root");
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("""
				{
				  "root": {
				    "a": "v1"
				  }
				}
				""");
		PipeLineSession session = new PipeLineSession();

		// Act
		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);

		// Assert
		assertJsonEquals(
				"""
						{
							"a": "v1"
						}
						""", result.toString());
	}

	@Test
	public void testParameterWithJsonPathExpressionValueIsJsonArray() throws Exception {
		// Arrange
		Parameter parameter = new Parameter();
		parameter.setName("p1");
		parameter.setJsonPathExpression("$.root");
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("""
				{
				  "root": [{
				    "a": "v1"
				  }, {
				    "a": "v2"
				  }]
				}
				""");
		PipeLineSession session = new PipeLineSession();

		// Act
		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);

		// Assert
		assertJsonEquals(
				"""
						[{
							"a": "v1"
						}, {
							"a": "v2"
						}]
						""", result.toString());
	}

	@Test
	public void testParameterWithJsonPathExpressionValueFromMessageAndContextKey() throws Exception {
		// Arrange
		Parameter parameter = new Parameter();
		parameter.setName("p1");
		parameter.setJsonPathExpression("$.root.a");
		parameter.setContextKey("ctx");
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");
		message.getContext().put("ctx", """
				{
				  "root": {
				    "a": "v1"
				  }
				}
				""");
		PipeLineSession session = new PipeLineSession();

		// Act
		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);

		// Assert
		assertEquals("v1", result);
	}


	@Test
	public void testParameterWithJsonPathExpressionConvertsFromXml() throws Exception {
		// Arrange
		Parameter parameter = new Parameter();
		parameter.setName("p1");
		parameter.setJsonPathExpression("$.root.a");
		parameter.setSessionKey("sessionKey");
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");
		PipeLineSession session = new PipeLineSession();
		session.put("sessionKey", """
				<root>
					<a>v1</a>
				</root>
				""");

		// Act
		Object result = parameter.getValue(alreadyResolvedParameters, message, session, true);

		// Assert
		assertEquals("v1", result);
	}

	@Test
	public void testParameterCannotHaveBothXPathAndJsonPathExpression() {
		// Arrange
		Parameter parameter = new Parameter();
		parameter.setName("p1");
		parameter.setJsonPathExpression(".root.a");
		parameter.setXpathExpression("/root/a");

		// Act
		assertThrows(ConfigurationException.class, parameter::configure);
	}
}
