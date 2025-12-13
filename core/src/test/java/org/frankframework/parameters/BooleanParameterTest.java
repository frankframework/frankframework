package org.frankframework.parameters;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;

public class BooleanParameterTest {

	@Test
	public void testBooleanParameterNumber() throws Exception {
		BooleanParameter p = new BooleanParameter();
		p.setName("number");
		p.setValue("1");
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertInstanceOf(Boolean.class, result);
		assertFalse((Boolean) result);
	}

	@ParameterizedTest
	@ValueSource(strings = {"true", "!false"})
	public void testBooleanParameterConvert(String bool) throws Exception {
		BooleanParameter p = new BooleanParameter();
		p.setName("boolean");
		p.setValue(bool);
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertInstanceOf(Boolean.class, result);
		assertTrue((Boolean) result);
	}

	@ParameterizedTest
	@ValueSource(strings = {"false", "!true"})
	public void testFalseBooleanParameterConvert(String bool) throws Exception {
		BooleanParameter p = new BooleanParameter();
		p.setName("boolean");
		p.setValue(bool);
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertInstanceOf(Boolean.class, result);
		assertFalse((Boolean) result);
	}

	@ParameterizedTest
	@ValueSource(strings = {"true", "!false"})
	public void testBooleanParameterAsInput(String bool) throws Exception {
		BooleanParameter p = new BooleanParameter();
		p.setName("boolean");
		p.setSessionKey("poeh");
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = Message.asMessage(true);

		PipeLineSession session = new PipeLineSession();
		session.put("poeh", bool);
		Object result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertInstanceOf(Boolean.class, result);
		assertTrue((Boolean) result);

		assertFalse(p.requiresInputValueForResolution());
		assertTrue(p.consumesSessionVariable("poeh"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"false", "!true"})
	public void testFalseBooleanParameterAsInput(String bool) throws Exception {
		BooleanParameter p = new BooleanParameter();
		p.setName("boolean");
		p.setSessionKey("poeh");
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = Message.asMessage(true);

		PipeLineSession session = new PipeLineSession();
		session.put("poeh", bool);
		Object result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertInstanceOf(Boolean.class, result);
		assertFalse((Boolean) result);

		assertFalse(p.requiresInputValueForResolution());
		assertTrue(p.consumesSessionVariable("poeh"));
	}

	@Test
	public void testXpath() throws Exception {
		BooleanParameter p = new BooleanParameter();
		p.setName("boolean");
		p.setXpathExpression("/root");
		p.configure();

		Message input = new Message("<root>true</root>");
		PipeLineSession session = new PipeLineSession();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Object result = p.getValue(alreadyResolvedParameters, input, session, false);
		Boolean bool = assertInstanceOf(Boolean.class, result);
		assertTrue(bool);
	}

	@Test
	public void testMaxLength() throws ConfigurationException, ParameterException {
		BooleanParameter parameter = new BooleanParameter();
		parameter.setName("boolean");
		parameter.setMaxLength(4);
		parameter.setXpathExpression("/root");
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message input = new Message("<root>trueaaaaaa</root>");

		Object result = parameter.getValue(alreadyResolvedParameters, input, null, true);
		Boolean bool = assertInstanceOf(Boolean.class, result);
		assertTrue(bool);
	}

	@Test
	public void testMaxLengthEdgeCase() throws ConfigurationException, ParameterException {
		BooleanParameter parameter = new BooleanParameter();
		parameter.setName("boolean");
		parameter.setMaxLength(4);
		parameter.setXpathExpression("/root");
		parameter.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message input = new Message("<root>false</root>");

		Object result = parameter.getValue(alreadyResolvedParameters, input, null, true);
		Boolean bool = assertInstanceOf(Boolean.class, result);
		assertFalse(bool);
	}

}
