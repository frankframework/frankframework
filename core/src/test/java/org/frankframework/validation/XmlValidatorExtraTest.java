package org.frankframework.validation;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.PipeTestBase;
import org.frankframework.pipes.XmlValidator;
import org.frankframework.testutil.TestFileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class XmlValidatorExtraTest extends PipeTestBase<XmlValidator> {

	private String xsdLocation;
	private String xmlLocation;
	private String expectedOutcome;
	private String xmlSchemaVersion;

	@Override
	public XmlValidator createPipe() {
		return new XmlValidator();
	}

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		// The order for the arrays should be as follows:
		// Test Name, XSD location, XML location, expected outcome, XSD version.
		// Expected outcome can be success, configurationException (for exceptions
		// thrown during configurations stage)
		// and any class that extends Exception that might be thrown during doPipe
		// stage.

		// Note: the tests in Xsd1.1 folder are taken from
		// https://www.w3.org/TR/xmlschema-guide2versioning/ examples
		return Arrays.asList(new Object[][] {
			{ "Test Override Default", 	"/Validation/OverrideAndRedefine/Override.xsd", 			"/Validation/OverrideAndRedefine/in_OK.xml", "success", null },
			{ "Test Override", 			"/Validation/OverrideAndRedefine/Override.xsd", 			"/Validation/OverrideAndRedefine/in_OK.xml", "success", "1.1" },
			{ "Test Override Fail", 	"/Validation/OverrideAndRedefine/Override.xsd", 			"/Validation/OverrideAndRedefine/in_OK.xml", "configurationException", "1.0" },
			{ "Test Redefine", 			"/Validation/OverrideAndRedefine/Redefine.xsd", 			"/Validation/OverrideAndRedefine/in_OK.xml", "success", "1.1" },
			{ "Test Wildcard", 			"/Validation/Xsd1.1/Wildcard/wildcard.xsd", 				"/Validation/Xsd1.1/Wildcard/in_OK.xml", "success", "1.1" },
			{ "Test Negative Wildcard", "/Validation/Xsd1.1/NegativeWildcard/negativeWildcard.xsd", "/Validation/Xsd1.1/NegativeWildcard/in_OK.xml", "org.frankframework.core.PipeRunException", "1.1" },
			{ "Test Assert", 			"/Validation/Xsd1.1/Assert/assert.xsd", 					"/Validation/Xsd1.1/Assert/in_OK.xml", "success", "1.1" },
			{ "Test Assert Fail", 		"/Validation/Xsd1.1/Assert/assert.xsd", 					"/Validation/Xsd1.1/Assert/in_ERROR.xml", "java.lang.Exception", "1.1" },
			{ "Test Year 0000", 		"/Validation/Xsd1.1/Year0000/dateTime.xsd", 				"/Validation/Xsd1.1/Year0000/in_OK.xml", "success", "1.1" }, });
	}

	public XmlValidatorExtraTest(String testName, String xsdLocation, String xmlLocation, String expectedOutcome, String xmlSchemaVersion) {
		this.xsdLocation = xsdLocation;
		this.xmlLocation = xmlLocation;
		this.expectedOutcome = expectedOutcome;
		this.xmlSchemaVersion = xmlSchemaVersion;
	}

	@Test
	public void testValidator() throws Exception {
		// Set properties for the pipe.
		pipe.setSchema(xsdLocation);
		pipe.setThrowException(true);
		pipe.setXmlSchemaVersion(xmlSchemaVersion);

		try {
			// Configure the pipe.
			pipe.configure();
			pipe.start();
		} catch (Exception e) {
			// If there are configuration exceptions st. having minVersion="1.1" on schema when running on XSD 1.0
			// exceptions will be thrown here.
			e.printStackTrace();
			assertEquals("configurationException", expectedOutcome);
			return;
		}

		PipeLineSession session = new PipeLineSession();
		String input = TestFileUtils.getTestFile(xmlLocation);

		try {
			// Run test and expect success
			PipeRunResult prr = doPipe(pipe, input, session);
			assertEquals(expectedOutcome, prr.getPipeForward().getName());
		} catch (Exception e) {
			// Case of an error while expecting a success
			if (expectedOutcome.equalsIgnoreCase("success"))
				throw e;

			// Make sure the full stack trace is printed for easy debugging.
			// Check if the error is the same type we were expecting.
			e.printStackTrace();
			Class expectedError = Class.forName(expectedOutcome);
			assertThat(e, instanceOf(expectedError));
		}
	}
}
