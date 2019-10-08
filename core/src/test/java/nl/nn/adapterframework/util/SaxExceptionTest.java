package nl.nn.adapterframework.util;

import static org.hamcrest.core.IsInstanceOf.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import static org.hamcrest.core.IsEqual.*;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.StringContains;
import org.junit.Test;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.xml.SaxException;

public class SaxExceptionTest {

	private final int LINENUMBER=100;
	private final int COLUMNNUMBER=10;
	private final String SYSTEM_ID="fakeSystemId";
	private final String PUBLIC_ID="fakePublicId";
	private final String EXPECTED_LOCATION_MESSAGE_PART="publicId ["+PUBLIC_ID+"] systemId["+SYSTEM_ID+"] lineNumber["+LINENUMBER+"] columnNumber["+COLUMNNUMBER+"]";

	@Test
	public void catchAndRethrowAsSaxExceptionWithMessage() {
		Exception e = createCause();
		String message = "fakeSaxMessage";
		SAXException se = new SaxException(message,e);
		inspectSAXException(se,message);
	}

	@Test
	public void catchAndRethrowAsSaxExceptionNoMessage() {
		Exception e = createCause();
		SAXException se = new SaxException(e);
		inspectSAXException(se);
	}

	@Test
	public void testCreateSaxExceptionWithLocator() {
		Exception e = createCause();
		Locator locator = createLocator();
		SAXException se = SaxException.createSaxException(null, locator, e);
		System.out.println("original stacktrace");
		se.printStackTrace();
		inspectSAXException(se);
		assertThat(se,IsInstanceOf.instanceOf(SAXParseException.class));
		assertThat(se.toString(), StringContains.containsString(EXPECTED_LOCATION_MESSAGE_PART));
	}

	@Test
	public void catchAndRethrowAsSaxExceptionWithMessageAndLocator() {
		Exception e = createCause();
		String message = "fakeSaxMessage";
		Locator locator = createLocator();
		SAXException se = SaxException.createSaxException(message,locator,e);
		assertThat(se.toString(), StringContains.containsString(EXPECTED_LOCATION_MESSAGE_PART));
		assertThat(se,IsInstanceOf.instanceOf(SAXParseException.class));
		inspectSAXException(se,message, locator);
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
	
	public void inspectSAXException(SAXException saxException) {
		inspectSAXException(saxException,null,null);
	}
		
	public void inspectSAXException(SAXException saxException, String expectedInSaxExceptionMessage) {
		inspectSAXException(saxException, expectedInSaxExceptionMessage, null);
	}
	public void inspectSAXException(SAXException saxException, String expectedInSaxExceptionMessage, Locator locator) {
		try {
			throw new SenderException(saxException);
		} catch (SenderException senderException) {
			inspectSenderException(senderException,saxException,expectedInSaxExceptionMessage,locator);
		}
	}

	public void inspectSenderException(SenderException e, SAXException originalException, String expectedInSaxExceptionMessage, Locator locator) {
		e.printStackTrace();
		Throwable cause3 = e.getCause();
		assertThat(cause3, IsInstanceOf.instanceOf(SAXException.class));
		assertThat("SaxException toString() must show itself",cause3.toString(),StringContains.containsString(originalException.getClass().getSimpleName()));
		assertThat("SaxException toString() must only show itself, not also its cause",cause3.toString(),IsNot.not(StringContains.containsString("java.io.IOException")));
		if (StringUtils.isNotEmpty(expectedInSaxExceptionMessage)) {
			assertThat(cause3.getMessage(),StringContains.containsString(expectedInSaxExceptionMessage));
		}
		assertThat(cause3,instanceOf(SAXException.class));
		assertThat((SAXException)cause3, equalTo(originalException));
		if (locator!=null) {
			assertThat(cause3,instanceOf(SAXParseException.class));
			SAXParseException spe = (SAXParseException)cause3;
			assertThat(spe.getColumnNumber(), equalTo(COLUMNNUMBER));
			assertThat(spe.getLineNumber(), equalTo(LINENUMBER));
			assertThat(spe.getSystemId(), equalTo(SYSTEM_ID));
			assertThat(spe.getPublicId(), equalTo(PUBLIC_ID));
		}
		Throwable cause2 = cause3.getCause();
		assertNotNull("SaxException should have proper cause",cause2);
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
