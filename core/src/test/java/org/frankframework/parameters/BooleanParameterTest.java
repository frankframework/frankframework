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

		Message message = new Message("fakeMessage");

		ParameterValue result = p.getValue(message, null);
		assertInstanceOf(Boolean.class, result.getValue());
		assertFalse((Boolean) result.getValue());
	}

	@ParameterizedTest
	@ValueSource(strings = {"true", "!false"})
	public void testBooleanParameterConvert(String bool) throws Exception {
		BooleanParameter p = new BooleanParameter();
		p.setName("boolean");
		p.setValue(bool);
		p.configure();

		Message message = new Message("fakeMessage");

		ParameterValue result = p.getValue(message, null);
		assertInstanceOf(Boolean.class, result.getValue());
		assertTrue((Boolean) result.getValue());
	}

	@ParameterizedTest
	@ValueSource(strings = {"false", "!true"})
	public void testFalseBooleanParameterConvert(String bool) throws Exception {
		BooleanParameter p = new BooleanParameter();
		p.setName("boolean");
		p.setValue(bool);
		p.configure();

		Message message = new Message("fakeMessage");

		ParameterValue result = p.getValue(message, null);
		assertInstanceOf(Boolean.class, result.getValue());
		assertFalse((Boolean) result.getValue());
	}

	@ParameterizedTest
	@ValueSource(strings = {"true", "!false"})
	public void testBooleanParameterAsInput(String bool) throws Exception {
		BooleanParameter p = new BooleanParameter();
		p.setName("boolean");
		p.setSessionKey("poeh");
		p.configure();

		Message message = Message.asMessage(true);

		PipeLineSession session = new PipeLineSession();
		session.put("poeh", bool);
		ParameterValue result = p.getValue(message, session);
		assertInstanceOf(Boolean.class, result.getValue());
		assertTrue((Boolean) result.getValue());

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

		Message message = Message.asMessage(true);

		PipeLineSession session = new PipeLineSession();
		session.put("poeh", bool);
		ParameterValue result = p.getValue(message, session);
		assertInstanceOf(Boolean.class, result.getValue());
		assertFalse((Boolean) result.getValue());

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

		ParameterValue result = p.getValue(input, session);
		Boolean bool = assertInstanceOf(Boolean.class, result.getValue());
		assertTrue(bool);
	}

	@Test
	public void testMaxLength() throws ConfigurationException, ParameterException {
		BooleanParameter parameter = new BooleanParameter();
		parameter.setName("boolean");
		parameter.setMaxLength(4);
		parameter.setXpathExpression("/root");
		parameter.configure();

		Message input = new Message("<root>trueaaaaaa</root>");

		ParameterValue result = parameter.getValue(input, null);
		Boolean bool = assertInstanceOf(Boolean.class, result.getValue());
		assertTrue(bool);
	}

	@Test
	public void testMaxLengthEdgeCase() throws ConfigurationException, ParameterException {
		BooleanParameter parameter = new BooleanParameter();
		parameter.setName("boolean");
		parameter.setMaxLength(4);
		parameter.setXpathExpression("/root");
		parameter.configure();

		Message input = new Message("<root>false</root>");

		ParameterValue result = parameter.getValue(input, null);
		Boolean bool = assertInstanceOf(Boolean.class, result.getValue());
		assertFalse(bool);
	}

}
