package org.frankframework.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;

public class NumberParameterTest {

	@Test
	public void testNumberWithMinLength() throws Exception {
		NumberParameter p = new NumberParameter();
		p.setName("number");
		p.setValue("8");
		p.setMinLength(10);
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertInstanceOf(String.class, result);
		assertEquals("0000000008", (String) result);
	}

	@Test
	public void testSmallNumberWithMaxLengthShouldLeftTrim() throws Exception {
		NumberParameter p = new NumberParameter();
		p.setName("number");
		p.setValue("0008"); //Smaller then max-length
		p.setMaxLength(5);
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertInstanceOf(Integer.class, result);
		assertEquals(8, result);
	}

	@Test
	public void testSmallNumberWithMaxLength() throws Exception {
		NumberParameter p = new NumberParameter();
		p.setName("number");
		p.setValue("0000008"); //Larger then max-length
		p.setMaxLength(5);
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertInstanceOf(Integer.class, result);
		assertEquals(0, result);
	}

	@Test
	public void testLargeNumberWithMaxLength() throws Exception {
		NumberParameter p = new NumberParameter();
		p.setName("number");
		p.setValue("0008123000");
		p.setMaxLength(5);
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertInstanceOf(Integer.class, result);
		assertEquals(81, result);
	}

	@Test
	public void testLargeNumberWithMinLength() throws Exception {
		NumberParameter p = new NumberParameter();
		p.setName("number");
		p.setValue("8000");
		p.setMinLength(2);
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertInstanceOf(Integer.class, result);
		assertEquals(8_000, result);
	}

	@Test
	public void testNumberWithLeftPaddingAndMinExclusive() throws Exception {
		NumberParameter p = new NumberParameter();
		p.setName("number");
		p.setValue("3");
		p.setMinLength(10);
		p.setMinInclusive("5");
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertInstanceOf(String.class, result);
		assertEquals("0000000005", (String) result);
	}

	@Test
	public void testNumberWithLeftPaddingAndMaxExclusive() throws Exception {
		NumberParameter p = new NumberParameter();
		p.setName("number");
		p.setValue("8");
		p.setMinLength(10);
		p.setMaxInclusive("5");
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertInstanceOf(String.class, result);
		assertEquals("0000000005", (String) result);

		assertFalse(p.requiresInputValueForResolution());
	}

	@Test
	public void testNumberWithLeftPaddingAndMaxExclusiveNotExceeding() throws Exception {
		NumberParameter p = new NumberParameter();
		p.setName("number");
		p.setValue("3");
		p.setMinLength(10);
		p.setMaxInclusive("5");
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertInstanceOf(String.class, result);
		assertEquals("0000000003", (String) result);
	}

	@Test
	public void testNumberWithLeftPaddingAndMinExclusiveNotExceeding() throws Exception {
		NumberParameter p = new NumberParameter();
		p.setName("number");
		p.setValue("5");
		p.setMinLength(10);
		p.setMinInclusive("3");
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertInstanceOf(String.class, result);
		assertEquals("0000000005", (String) result);
	}

	@Test
	public void testDecimalSeparator() throws Exception {
		NumberParameter p = new NumberParameter();
		p.setDecimalSeparator("#");
		p.setName("number");
		p.setValue("5#8");
		p.setMinLength(10);
		p.setMinInclusive("3");
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertInstanceOf(String.class, result);
		assertEquals("00000005.8", (String) result);
	}

	@Test
	public void testDecimalSeparatorWithMinMaxInclusiveString() throws Exception {
		NumberParameter p = new NumberParameter();
		p.setDecimalSeparator(",.");
		p.setName("number");
		p.setValue("5,6.00");
		p.setMinInclusive("5");
		p.setMaxInclusive("6");
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertInstanceOf(Double.class, result);
		assertEquals(5.6, result);
	}

	@Test
	public void testGroupingSeparator() throws Exception {
		NumberParameter p = new NumberParameter();
		p.setGroupingSeparator(",.");
		p.setDecimalSeparator("."); //Unfortunately required when building on American servers...
		p.setName("number");
		p.setValue("5,6.00");
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertInstanceOf(Long.class, result);
		assertEquals(56L, result);
	}

	@Test
	public void testDecimalAndGroupingSeparator() throws Exception {
		NumberParameter p = new NumberParameter();
		p.setDecimalSeparator(",");
		p.setGroupingSeparator(".");
		p.setName("number");
		p.setValue("5.000,00");
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertInstanceOf(Long.class, result);
		assertEquals(5000L, result);
	}

	@Test
	public void testParameterNumberParseException() throws Exception {
		NumberParameter p = new NumberParameter();
		p.setName("number");
		p.setValue("a");
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
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
		NumberParameter p = new NumberParameter();
		p.setName("integer");
		p.setValue("8");
		p.setType(type);
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertTrue(c.isAssignableFrom(result.getClass()), c + " is expected type but was: " + result.getClass());

		PipeLineSession session = new PipeLineSession();
		session.put("sessionkey", 8);
		p = new NumberParameter();
		p.setName("integer");
		p.setSessionKey("sessionkey");
		p.setType(type);
		p.configure();

		result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(c.isAssignableFrom(result.getClass()), c + " is expected type but was: " + result.getClass());

		session = new PipeLineSession();
		session.put("sessionkey", "8");

		result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(c.isAssignableFrom(result.getClass()), c + " is expected type but was: " + result.getClass());

		session = new PipeLineSession();
		session.put("sessionkey", Message.asMessage(8));

		result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(c.isAssignableFrom(result.getClass()), c + " is expected type but was: " + result.getClass());

		session = new PipeLineSession();
		session.put("sessionkey", "8".getBytes());

		result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(c.isAssignableFrom(result.getClass()), c + " is expected type but was: " + result.getClass());

		session = new PipeLineSession();
		session.put("sessionkey", new Message("8".getBytes()));

		result = p.getValue(alreadyResolvedParameters, message, session, false);
		assertTrue(c.isAssignableFrom(result.getClass()), c + " is expected type but was: " + result.getClass());

	}
}
