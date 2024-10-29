package org.frankframework.xml;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import org.frankframework.core.SenderException;

public class SaxExceptionTest {

	private final int LINENUMBER=100;
	private final int COLUMNNUMBER=10;
	private final String SYSTEM_ID="fakeSystemId";
	private final String PUBLIC_ID="fakePublicId";
	private final String EXPECTED_LOCATION_MESSAGE_PART1="line ["+LINENUMBER+"] column ["+COLUMNNUMBER+"]";
	private final String EXPECTED_LOCATION_MESSAGE_PART="publicId ["+PUBLIC_ID+"] systemId ["+SYSTEM_ID+"] "+EXPECTED_LOCATION_MESSAGE_PART1;

	@Test
	public void testSaxExceptionWithMessage() {
		Exception cause = createCause();
		String message = "fakeSaxMessage";
		SAXException se = new SaxException(message, cause);
		inspectSAXException(se,message,null);
	}

	@Test
	public void testSaxExceptionNoMessage() {
		Exception cause = createCause();
		SAXException se = new SaxException(cause);
		inspectSAXException(se,null,null);
	}

	@Test
	public void testCreateSaxExceptionWithLocator() {
		Exception cause = createCause();
		Locator locator = createLocator();
		SAXException se = SaxException.createSaxException(null, locator, cause);
		se.printStackTrace();
		inspectSAXException(se,null,locator);
		assertThat(se.toString(), StringContains.containsString(EXPECTED_LOCATION_MESSAGE_PART));
	}


	@Test
	public void testSaxExceptionWithMessageWrappedInSenderException() {
		Exception cause = createCause();
		String message = "fakeSaxMessage";
		SAXException se = new SaxException(message, cause);
		SenderException senderException = new SenderException(se);
		inspectViaSenderException(senderException, se, message, null);
	}

	@Test
	public void testSaxExceptionNoMessageWrappedInSenderException() {
		Exception e = createCause();
		SAXException se = new SaxException(e);
		SenderException senderException = new SenderException(se);
		inspectViaSenderException(senderException, se, null, null);
	}

	@Test
	public void testCreateSaxExceptionWithLocatorWrappedInSenderException() {
		Exception e = createCause();
		Locator locator = createLocator();
		SAXException se = SaxException.createSaxException(null, locator, e);
		SenderException senderException = new SenderException(se);
		inspectViaSenderException(senderException, se, null, locator);
		assertThat(se.toString(), StringContains.containsString(EXPECTED_LOCATION_MESSAGE_PART));
	}

	@Test
	public void testCreateSaxExceptionWithMessageAndLocatorWrappedInSenderException() {
		Exception e = createCause();
		String message = "fakeSaxMessage";
		Locator locator = createLocator();
		SAXException se = SaxException.createSaxException(message,locator,e);
		SenderException senderException = new SenderException(se);
		inspectViaSenderException(senderException, se, message, locator);
		assertThat(se.toString(), StringContains.containsString(EXPECTED_LOCATION_MESSAGE_PART));
	}


	@Test
	public void testSaxExceptionWithMessageWrappedInTransformerException() {
		Exception cause = createCause();
		String message = "fakeSaxMessage";
		SAXException se = new SaxException(message, cause);
		TransformerException transformerException = new TransformerException(se);
		inspectViaTransformerException(transformerException, se, message, null);
	}

	@Test
	public void testSaxExceptionNoMessageWrappedInTransformerException() {
		Exception e = createCause();
		SAXException se = new SaxException(e);
		TransformerException transformerException = new TransformerException(se);
		inspectViaTransformerException(transformerException, se, null, null);
	}

	@Test
	public void testCreateSaxExceptionWithLocatorWrappedInTransformerException() {
		Exception e = createCause();
		Locator locator = createLocator();
		SAXException se = SaxException.createSaxException(null, locator, e);
		TransformerException transformerException = new TransformerException(se);
		inspectViaTransformerException(transformerException, se, null, locator);
		assertThat(se.toString(), StringContains.containsString(EXPECTED_LOCATION_MESSAGE_PART));
	}

	@Test
	public void testCreateSaxExceptionWithMessageAndLocatorWrappedInTransformerException() {
		Exception e = createCause();
		String message = "fakeSaxMessage";
		Locator locator = createLocator();
		SAXException se = SaxException.createSaxException(message,locator,e);
		TransformerException transformerException = new TransformerException(se);
		inspectViaTransformerException(transformerException, se, message, locator);
		assertThat(se.toString(), StringContains.containsString(EXPECTED_LOCATION_MESSAGE_PART));
	}


	@Test
	public void testSaxExceptionWithMessageWrappedInSenderAndTransformerException() {
		Exception cause = createCause();
		String message = "fakeSaxMessage";
		SAXException se = new SaxException(message, cause);
		TransformerException transformerException = new TransformerException(se);
		SenderException senderException = new SenderException(transformerException);
		inspectViaSenderAndTransformerException(senderException, se, message, null);
	}

	@Test
	public void testSaxExceptionNoMessageWrappedInSenderAndTransformerException() {
		Exception e = createCause();
		SAXException se = new SaxException(e);
		TransformerException transformerException = new TransformerException(se);
		SenderException senderException = new SenderException(transformerException);
		inspectViaSenderAndTransformerException(senderException, se, null, null);
	}

	@Test
	public void testCreateSaxExceptionWithLocatorWrappedInSenderAndTransformerException() {
		Exception e = createCause();
		Locator locator = createLocator();
		SAXException se = SaxException.createSaxException(null, locator, e);
		TransformerException transformerException = new TransformerException(se);
		SenderException senderException = new SenderException(transformerException);
		inspectViaSenderAndTransformerException(senderException, se, null, locator);
		assertThat(se.toString(), StringContains.containsString(EXPECTED_LOCATION_MESSAGE_PART));
	}

	@Test
	public void testCreateSaxExceptionWithMessageAndLocatorWrappedInSenderAndTransformerException() {
		Exception e = createCause();
		String message = "fakeSaxMessage";
		Locator locator = createLocator();
		SAXException se = SaxException.createSaxException(message,locator,e);
		TransformerException transformerException = new TransformerException(se);
		SenderException senderException = new SenderException(transformerException);
		inspectViaSenderAndTransformerException(senderException, se, message, locator);
		assertThat(se.toString(), StringContains.containsString(EXPECTED_LOCATION_MESSAGE_PART));
	}

	public Exception createCause() {
		try {
			catchAndRethrow();
			fail("Expected exception");
		} catch (Exception e) {
			return e;
		}
		fail("Expected exception");
		return null;
	}

	public void inspectViaSenderAndTransformerException(SenderException e, SAXException originalException, String expectedInSaxExceptionMessage, Locator locator) {
		if (locator!=null) {
			String message= e.getMessage();
			assertThat(message, StringContains.containsString(EXPECTED_LOCATION_MESSAGE_PART1));
			int locationInfoPos=message.indexOf(EXPECTED_LOCATION_MESSAGE_PART1);
			String messageTail=message.substring(locationInfoPos+EXPECTED_LOCATION_MESSAGE_PART1.length());
			assertThat("part of message after location info should not contain location info",messageTail, not(StringContains.containsString(EXPECTED_LOCATION_MESSAGE_PART1)));
		}

		Throwable cause = e.getCause();
		assertThat(cause, IsInstanceOf.instanceOf(TransformerException.class));
		TransformerException tCause = (TransformerException)cause;
		inspectViaTransformerException(tCause, originalException, expectedInSaxExceptionMessage, locator);
	}

	public void inspectViaTransformerException(TransformerException e, SAXException originalException, String expectedInSaxExceptionMessage, Locator locator) {
		Throwable cause = e.getCause();
		assertThat(cause, IsInstanceOf.instanceOf(SAXException.class));
		SAXException saxCause = (SAXException)cause;
		inspectSAXException(saxCause, expectedInSaxExceptionMessage, locator);
	}

	public void inspectViaSenderException(SenderException e, SAXException originalException, String expectedInSaxExceptionMessage, Locator locator) {
		if (locator!=null) {
			assertThat(e.getMessage(), StringContains.containsString(EXPECTED_LOCATION_MESSAGE_PART));
		}
		Throwable cause = e.getCause();
		assertThat(cause, IsInstanceOf.instanceOf(SAXException.class));
		SAXException saxCause = (SAXException)cause;
		inspectSAXException(saxCause, expectedInSaxExceptionMessage, locator);
	}

	public void inspectSAXException(SAXException e, String expectedInMessage, Locator locator) {
		assertThat("SaxException toString() must show itself",e.toString(),StringContains.containsString(e.getClass().getSimpleName()));
		assertThat("SaxException toString() must only show itself, not also its cause",e.toString(),not(StringContains.containsString("java.io.IOException")));
		if (StringUtils.isNotEmpty(expectedInMessage)) {
			assertThat(e.getMessage(),StringContains.containsString(expectedInMessage));
		}
		assertThat(e,instanceOf(SAXException.class));
		if (locator!=null) {
			assertThat("location info must be shown", e.getMessage(),StringContains.containsString(EXPECTED_LOCATION_MESSAGE_PART));
		}
		Throwable cause2 = e.getCause();
		assertNotNull(cause2, "SaxException should have proper cause");
		assertThat(cause2, IsInstanceOf.instanceOf(IOException.class));
		Throwable cause1 = cause2.getCause();
		assertThat(cause1, IsInstanceOf.instanceOf(NullPointerException.class));
		StackTraceElement[] causeTrace=cause1.getStackTrace();
		assertNotNull(causeTrace);
		assertEquals("throwNullPointer", causeTrace[0].getMethodName());
	}

	public Locator createLocator() {
		Locator locator = new Locator() {

			@Override
			public int getColumnNumber() {
				return COLUMNNUMBER;
			}

			@Override
			public int getLineNumber() {
				return LINENUMBER;
			}

			@Override
			public String getPublicId() {
				return PUBLIC_ID;
			}

			@Override
			public String getSystemId() {
				return SYSTEM_ID;
			}
		};
		return locator;
	}

	public void catchAndRethrow() throws IOException {
		try {
			throwNullPointer();
		} catch (Exception e) {
			throw new IOException("caught you!",e);
		}
	}

	@SuppressWarnings("null")
	public void throwNullPointer() {
		String testString=null;
		testString.toString();
	}
}
