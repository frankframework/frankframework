package org.frankframework.xslt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.logging.log4j.Level;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestAppender;
import org.frankframework.testutil.TestFileUtils;
import org.junit.AssumptionViolatedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class XsltErrorTestBase<P extends FixedForwardPipe> extends XsltTestBase<P> {

	protected TestAppender testAppender;
	private ErrorOutputStream errorOutputStream;
	private PrintStream prevStdErr;
	public static int EXPECTED_NUMBER_OF_DUPLICATE_LOGGINGS = 0;
	private final String FILE_NOT_FOUND_EXCEPTION = "Cannot get resource for href [";
	private final boolean testForEmptyOutputStream = false;
	protected int getMultiplicity() {
		return 1;
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

	@BeforeEach
	public void setup() {
		// Force reconfigure to clean list appender.
		testAppender = TestAppender.newBuilder()
			.useIbisPatternLayout("%level %m")
			.minLogLevel(Level.WARN)
			.build();
		TestAppender.addToRootLogger(testAppender);

		if (testForEmptyOutputStream) {
			errorOutputStream = new ErrorOutputStream();
			prevStdErr=System.err;
			System.setErr(new PrintStream(errorOutputStream));
		}
	}

	@Override
	@AfterEach
	public void tearDown() throws Exception {
		TestAppender.removeAppender(testAppender);
		if (testForEmptyOutputStream) {
			// Xslt processing should not log to stderr
			System.setErr(prevStdErr);
			System.err.println("ErrorStream:"+errorOutputStream);
			assertEquals("", errorOutputStream.toString());
		}
		super.tearDown();
	}

	protected void checkTestAppender(int expectedSize, String expectedString) {
		System.out.println("Log Appender:"+testAppender.toString());
		assertThat("number of alerts in logging " + testAppender.getLogLines(), testAppender.getNumberOfAlerts(), is(expectedSize));
		if (expectedString!=null) assertThat(testAppender.toString(),containsString(expectedString));
	}

	// detect duplicate imports in configure()
	@Test
	void duplicateImportErrorAlertsXslt1() throws Exception {
		// this condition appears to result in a warning only for XSLT 2.0 using Saxon
		setStyleSheetName("/Xslt/duplicateImport/root.xsl");
		setXsltVersion(1);
		pipe.configure();
		checkTestAppender(0,null);
	}

	// detect duplicate imports in configure()
	@Test
	void duplicateImportErrorAlertsXslt2() throws Exception {
		setStyleSheetName("/Xslt/duplicateImport/root2.xsl");
		setXsltVersion(2);
		pipe.configure();
		pipe.start();
		checkTestAppender(getMultiplicity(),"is included or imported more than once");
	}

	public void duplicateImportErrorProcessing(boolean xslt2) throws Exception {
		setStyleSheetName("/Xslt/duplicateImport/root.xsl");
		setXsltVersion(xslt2 ? 2 : 1);
		setIndent(true);
		pipe.configure();
		pipe.start();

		String input= TestFileUtils.getTestFile("/Xslt/duplicateImport/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected=TestFileUtils.getTestFile("/Xslt/duplicateImport/out.xml");

		PipeRunResult prr=doPipe(pipe, input, session);
		String result = Message.asString(prr.getResult());

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
		setStyleSheetName("/Xslt/importDocument/importNotFound1.xsl");
		setXsltVersion(1);
		setIndent(true);
		pipe.configure();
		pipe.start();
		String input = TestFileUtils.getTestFile("/Xslt/importDocument/in.xml");
		String errorMessage = null;
		try {
			doPipe(pipe, input, session);
		} catch (AssumptionViolatedException e) {
			throw e;
		} catch (Exception e) {
			errorMessage = e.getMessage();
			assertThat(errorMessage,containsString(FILE_NOT_FOUND_EXCEPTION));
		}
		assertThat(testAppender.toString(),containsString(FILE_NOT_FOUND_EXCEPTION));
		System.out.println("ErrorMessage: "+errorMessage);
		if (testForEmptyOutputStream) {
			System.out.println("ErrorStream(=stderr): "+errorOutputStream.toString());
			System.out.println("Clearing ErrorStream, as I am currently unable to catch it");
			errorOutputStream= new ErrorOutputStream();
		}
	}

	@Test
	void documentIncludedInSourceNotFoundXslt2() throws Exception {
		// error not during configure(), but during doPipe()
		setStyleSheetName("/Xslt/importDocument/importNotFound2.xsl");
		setXsltVersion(2);
		pipe.configure();
		pipe.start();
		String input = TestFileUtils.getTestFile("/Xslt/importDocument/in.xml");
		String errorMessage = null;
		try {
			doPipe(pipe, input, session);
		} catch (AssumptionViolatedException e) {
			throw e;
		} catch (Exception e) {
			errorMessage = e.getMessage();
			assertThat(errorMessage,containsString(FILE_NOT_FOUND_EXCEPTION));
		}
		// Saxon 9.8 no longer considers a missing import to be fatal. This is similar to Xalan
		String FILE_NOT_FOUND_EXCEPTION_SAXON_10 = "WARN Fatal transformation error: Exception thrown by URIResolver resolving `";
		assertThat(testAppender.toString(), containsString(FILE_NOT_FOUND_EXCEPTION_SAXON_10));

		System.out.println("ErrorMessage: "+errorMessage);
		if (testForEmptyOutputStream) {
			System.out.println("ErrorStream(=stderr): "+errorOutputStream.toString());
			System.out.println("Clearing ErrorStream, as I am currently unable to catch it");
			errorOutputStream= new ErrorOutputStream();
		}
	}

	@Test
	void importNotFoundXslt1() {
		setStyleSheetName("/Xslt/importNotFound/root.no-validate-xsl");
		setXsltVersion(1);
		String errorMessage;
		try {
			pipe.configure();
			fail("Expected to run into an exception");
		} catch (ConfigurationException e) {
			errorMessage = e.getMessage();
			assertThat(errorMessage,containsString(FILE_NOT_FOUND_EXCEPTION));
		}
		checkTestAppender(1,FILE_NOT_FOUND_EXCEPTION);
	}

	@Test
	void importNotFoundXslt2() {
		setStyleSheetName("/Xslt/importNotFound/root2.no-validate-xsl");
		setXsltVersion(2);
		String errorMessage;
		try {
			pipe.configure();
			fail("expected configuration to fail because an import could not be found");
		} catch (ConfigurationException e) {
			errorMessage = e.getMessage();
			assertThat(errorMessage,containsString(FILE_NOT_FOUND_EXCEPTION));
		}
		checkTestAppender(1,FILE_NOT_FOUND_EXCEPTION);
	}

	@Test
	void notifyXalanExtensionsIllegalForSaxon() {
		setStyleSheetName("/Xslt/XalanExtension/XalanExtension.xsl");
		setXsltVersion(2);
		String errorMessage;
		try {
			pipe.configure();
			fail("expected configuration to fail");
		} catch (ConfigurationException e) {
			log.warn("final exception: "+e.getMessage());
			errorMessage = e.getMessage();
			assertThat(errorMessage,containsString("Cannot find a 2-argument function named Q{http://exslt.org/strings}tokenize()"));
		}

		assertThat("number of alerts in logging " + testAppender.getLogLines(), testAppender.getNumberOfAlerts(), is(2+EXPECTED_NUMBER_OF_DUPLICATE_LOGGINGS));
	}

	@Test
	void illegalXPathExpressionXslt2() {
		// error not during configure(), but during doPipe()
		setXpathExpression("position()='1'");
		setXsltVersion(2);
		String errorMessage = null;
		try {
			pipe.configure();
			fail("Expected to run into an exception");
		} catch (Exception e) {
			errorMessage = e.getMessage();
			assertThat(errorMessage,containsString("cannot compare xs:integer to xs:string"));
		}
		checkTestAppender(1,null);
		System.out.println("ErrorMessage: "+errorMessage);
		if (testForEmptyOutputStream) {
			System.out.println("ErrorStream(=stderr): "+errorOutputStream.toString());
			System.out.println("Clearing ErrorStream, as I am currently unable to catch it");
			errorOutputStream= new ErrorOutputStream();
		}
	}

	@Test
	void illegalXPathExpression2Xslt1() {
		// error not during configure(), but during doPipe()
		setXpathExpression("<result><status>invalid</status><message>$failureReason</message></result>");
		setXsltVersion(1);
		String errorMessage = null;
		try {
			pipe.configure();
			fail("Expected to run into an exception");
		} catch (Exception e) {
			errorMessage = e.getMessage();
			assertThat(errorMessage,containsString("<result><status>invalid</status><message>$failureReason</message></result>"));
			assertThat(errorMessage,containsString("A location path was expected, but the following token was encountered:  <"));
		}
		checkTestAppender(2, null);
		System.out.println("ErrorMessage: "+errorMessage);
		if (testForEmptyOutputStream) {
			System.out.println("ErrorStream(=stderr): "+errorOutputStream.toString());
			System.out.println("Clearing ErrorStream, as I am currently unable to catch it");
			errorOutputStream= new ErrorOutputStream();
		}
	}

	@Test
	void illegalXPathExpression2Xslt2() {
		// error not during configure(), but during doPipe()
		setXpathExpression("<result><status>invalid</status><message>$failureReason</message></result>");
		setXsltVersion(2);
		String errorMessage = null;
		try {
			pipe.configure();
			fail("Expected to run into an exception");
		} catch (Exception e) {
			errorMessage = e.getMessage();
			assertThat(errorMessage,containsString("<result><status>invalid</status><message>$failureReason</message></result>"));
			assertThat(errorMessage,containsString("Unexpected token \"<\" at start of expression"));
		}
		checkTestAppender(1,null);
		System.out.println("ErrorMessage: "+errorMessage);
		if (testForEmptyOutputStream) {
			System.out.println("ErrorStream(=stderr): "+errorOutputStream.toString());
			System.out.println("Clearing ErrorStream, as I am currently unable to catch it");
			errorOutputStream= new ErrorOutputStream();
		}
	}
}
