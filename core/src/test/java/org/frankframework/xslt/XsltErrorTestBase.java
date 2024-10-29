package org.frankframework.xslt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.testutil.TestAppender;
import org.frankframework.testutil.TestFileUtils;

public abstract class XsltErrorTestBase<P extends FixedForwardPipe> extends XsltTestBase<P> {

	public static int EXPECTED_NUMBER_OF_DUPLICATE_LOGGINGS = 0;
	private final String FILE_NOT_FOUND_EXCEPTION = "Cannot get resource for href [";
	private final boolean testForEmptyOutputStream = false;
	private ErrorOutputStream errorOutputStream;
	private PrintStream prevStdErr;

	protected int getMultiplicity() {
		return 1;
	}

	private TestAppender getTestAppender() {
		return TestAppender.newBuilder()
				.useIbisPatternLayout("%level %m")
				.minLogLevel(Level.WARN)
				.build();
	}

	@BeforeEach
	public void setup() {
		if (testForEmptyOutputStream) {
			errorOutputStream = new ErrorOutputStream();
			prevStdErr = System.err;
			System.setErr(new PrintStream(errorOutputStream));
		}
	}

	@Override
	@AfterEach
	public void tearDown() {
		if (testForEmptyOutputStream) {
			// Xslt processing should not log to stderr
			System.setErr(prevStdErr);
			System.err.println("ErrorStream:" + errorOutputStream);
			assertEquals("", errorOutputStream.toString());
		}
		super.tearDown();
	}

	protected void checkTestAppender(int expectedSize, String expectedString, TestAppender appender) {
		log.debug("Log Appender: {}", appender);
		assertThat("number of alerts in logging " + appender.getLogLines(), appender.getNumberOfAlerts(), is(expectedSize));
		if (expectedString != null) assertThat(appender.toString(), containsString(expectedString));
	}

	// detect duplicate imports in configure()
	@Test
	void duplicateImportErrorAlertsXslt1() throws Exception {
		try (TestAppender appender = getTestAppender()) {
			// this condition appears to result in a warning only for XSLT 2.0 using Saxon
			setStyleSheetName("/Xslt/duplicateImport/root.xsl");
			setXsltVersion(1);
			pipe.configure();
			checkTestAppender(0, null, appender);
		}
	}

	// detect duplicate imports in configure()
	@Test
	void duplicateImportErrorAlertsXslt2() throws Exception {
		try (TestAppender appender = getTestAppender()) {
			setStyleSheetName("/Xslt/duplicateImport/root2.xsl");
			setXsltVersion(2);
			pipe.configure();
			pipe.start();
			checkTestAppender(getMultiplicity(), "is included or imported more than once", appender);
		}
	}

	public void duplicateImportErrorProcessing(boolean xslt2) throws Exception {
		setStyleSheetName("/Xslt/duplicateImport/root.xsl");
		setXsltVersion(xslt2 ? 2 : 1);
		setIndent(true);
		pipe.configure();
		pipe.start();

		String input = TestFileUtils.getTestFile("/Xslt/duplicateImport/in.xml");
		log.debug("inputfile [" + input + "]");
		String expected = TestFileUtils.getTestFile("/Xslt/duplicateImport/out.xml");

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = prr.getResult().asString();

		assertResultsAreCorrect(expected, result, session);
	}

	@Test
	void duplicateImportErrorProcessingXslt1() throws Exception {
		duplicateImportErrorProcessing(false);
	}

	@Test
	void duplicateImportErrorProcessingXslt2() throws Exception {
		duplicateImportErrorProcessing(true);
	}

	@Test
	void documentIncludedInSourceNotFoundXslt1() throws Exception {
		try (TestAppender appender = getTestAppender()) {
			setStyleSheetName("/Xslt/importDocument/importNotFound1.xsl");
			setXsltVersion(1);
			setIndent(true);
			pipe.configure();
			pipe.start();
			String input = TestFileUtils.getTestFile("/Xslt/importDocument/in.xml");
			String errorMessage = null;
			try {
				doPipe(pipe, input, session);
			} catch (Exception e) {
				errorMessage = e.getMessage();
				assertThat(errorMessage, containsString(FILE_NOT_FOUND_EXCEPTION));
			}
			assertThat(appender.toString(), containsString(FILE_NOT_FOUND_EXCEPTION));
			System.out.println("ErrorMessage: " + errorMessage);
			if (testForEmptyOutputStream) {
				System.out.println("ErrorStream(=stderr): " + errorOutputStream.toString());
				System.out.println("Clearing ErrorStream, as I am currently unable to catch it");
				errorOutputStream = new ErrorOutputStream();
			}
		}
	}

	@Test
	void documentIncludedInSourceNotFoundXslt2() throws Exception {
		try (TestAppender appender = getTestAppender()) {
			// error not during configure(), but during doPipe()
			setStyleSheetName("/Xslt/importDocument/importNotFound2.xsl");
			setXsltVersion(2);
			pipe.configure();
			pipe.start();
			String input = TestFileUtils.getTestFile("/Xslt/importDocument/in.xml");
			String errorMessage = null;
			try {
				doPipe(pipe, input, session);
			} catch (Exception e) {
				errorMessage = e.getMessage();
				assertThat(errorMessage, containsString(FILE_NOT_FOUND_EXCEPTION));
			}
			// Saxon 9.8 no longer considers a missing import to be fatal. This is similar to Xalan
			String FILE_NOT_FOUND_EXCEPTION_SAXON_10 = "WARN Fatal transformation error: Exception thrown by URIResolver resolving `";
			assertThat(appender.toString(), containsString(FILE_NOT_FOUND_EXCEPTION_SAXON_10));

			System.out.println("ErrorMessage: " + errorMessage);
			if (testForEmptyOutputStream) {
				System.out.println("ErrorStream(=stderr): " + errorOutputStream.toString());
				System.out.println("Clearing ErrorStream, as I am currently unable to catch it");
				errorOutputStream = new ErrorOutputStream();
			}
		}
	}

	@Test
	void importNotFoundXslt1() {
		try (TestAppender appender = getTestAppender()) {
			setStyleSheetName("/Xslt/importNotFound/root.no-validate-xsl");
			setXsltVersion(1);
			String errorMessage;
			try {
				pipe.configure();
				fail("Expected to run into an exception");
			} catch (ConfigurationException e) {
				errorMessage = e.getMessage();
				assertThat(errorMessage, containsString(FILE_NOT_FOUND_EXCEPTION));
			}
			checkTestAppender(1, FILE_NOT_FOUND_EXCEPTION, appender);
		}
	}

	@Test
	void importNotFoundXslt2() {
		try (TestAppender appender = getTestAppender()) {
			setStyleSheetName("/Xslt/importNotFound/root2.no-validate-xsl");
			setXsltVersion(2);
			String errorMessage;
			try {
				pipe.configure();
				fail("expected configuration to fail because an import could not be found");
			} catch (ConfigurationException e) {
				errorMessage = e.getMessage();
				assertThat(errorMessage, containsString(FILE_NOT_FOUND_EXCEPTION));
			}
			checkTestAppender(1, FILE_NOT_FOUND_EXCEPTION, appender);
		}
	}

	@Test
	void notifyXalanExtensionsIllegalForSaxon() {
		try (TestAppender appender = getTestAppender()) {
			setStyleSheetName("/Xslt/XalanExtension/XalanExtension.xsl");
			setXsltVersion(2);
			String errorMessage;
			try {
				pipe.configure();
				fail("expected configuration to fail");
			} catch (ConfigurationException e) {
				log.warn("final exception: " + e.getMessage());
				errorMessage = e.getMessage();
				assertThat(errorMessage, containsString("Cannot find a 2-argument function named Q{http://exslt.org/strings}tokenize()"));
			}

			assertThat("number of alerts in logging " + appender.getLogLines(), appender.getNumberOfAlerts(), is(2 + EXPECTED_NUMBER_OF_DUPLICATE_LOGGINGS));
		}
	}

	@Test
	void illegalXPathExpressionXslt2() {
		try (TestAppender appender = getTestAppender()) {
			// error not during configure(), but during doPipe()
			setXpathExpression("position()='1'");
			setXsltVersion(2);
			String errorMessage = null;
			try {
				pipe.configure();
				fail("Expected to run into an exception");
			} catch (Exception e) {
				errorMessage = e.getMessage();
				assertThat(errorMessage, containsString("cannot compare xs:integer to xs:string"));
			}
			checkTestAppender(1, null, appender);
			System.out.println("ErrorMessage: " + errorMessage);
			if (testForEmptyOutputStream) {
				System.out.println("ErrorStream(=stderr): " + errorOutputStream.toString());
				System.out.println("Clearing ErrorStream, as I am currently unable to catch it");
				errorOutputStream = new ErrorOutputStream();
			}
		}
	}

	@Test
	void illegalXPathExpression2Xslt1() {
		try (TestAppender appender = getTestAppender()) {
			// error not during configure(), but during doPipe()
			setXpathExpression("<result><status>invalid</status><message>$failureReason</message></result>");
			setXsltVersion(1);
			String errorMessage = null;
			try {
				pipe.configure();
				fail("Expected to run into an exception");
			} catch (Exception e) {
				errorMessage = e.getMessage();
				assertThat(errorMessage, containsString("<result><status>invalid</status><message>$failureReason</message></result>"));
				assertThat(errorMessage, containsString("A location path was expected, but the following token was encountered:  <"));
			}
			checkTestAppender(2, null, appender);
			System.out.println("ErrorMessage: " + errorMessage);
			if (testForEmptyOutputStream) {
				System.out.println("ErrorStream(=stderr): " + errorOutputStream.toString());
				System.out.println("Clearing ErrorStream, as I am currently unable to catch it");
				errorOutputStream = new ErrorOutputStream();
			}
		}
	}

	@Test
	void illegalXPathExpression2Xslt2() {
		try (TestAppender appender = getTestAppender()) {
			// error not during configure(), but during doPipe()
			setXpathExpression("<result><status>invalid</status><message>$failureReason</message></result>");
			setXsltVersion(2);
			String errorMessage = null;
			try {
				pipe.configure();
				fail("Expected to run into an exception");
			} catch (Exception e) {
				errorMessage = e.getMessage();
				assertThat(errorMessage, containsString("<result><status>invalid</status><message>$failureReason</message></result>"));
				assertThat(errorMessage, containsString("Unexpected token \"<\" at start of expression"));
			}
			checkTestAppender(1, null, appender);
			System.out.println("ErrorMessage: " + errorMessage);
			if (testForEmptyOutputStream) {
				System.out.println("ErrorStream(=stderr): " + errorOutputStream.toString());
				System.out.println("Clearing ErrorStream, as I am currently unable to catch it");
				errorOutputStream = new ErrorOutputStream();
			}
		}
	}

	private static class ErrorOutputStream extends OutputStream {
		private final StringBuilder line = new StringBuilder();

		@Override
		public void write(int b) {
			line.append((char) b);
		}

		@Override
		public String toString() {
			return line.toString();
		}
	}
}
