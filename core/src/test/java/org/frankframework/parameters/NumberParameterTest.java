package org.frankframework.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.frankframework.stream.Message;
import org.junit.jupiter.api.Test;

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
	public void testLargeNumberWithMinLength() throws Exception {
		NumberParameter p = new NumberParameter();
		p.setName("number");
		p.setValue("8000");
		p.setMinLength(2);
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertInstanceOf(Long.class, result);
		assertEquals(8_000L, result);
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
		assertFalse(p.requiresInputValueOrContextForResolution());
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
		p.setName("number");
		p.setValue("5,6.00");
		p.configure();

		ParameterValueList alreadyResolvedParameters = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object result = p.getValue(alreadyResolvedParameters, message, null, false);
		assertInstanceOf(Double.class, result);
		assertEquals(5.6, result);
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
}
