package nl.nn.adapterframework.xslt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.logging.log4j.Level;
import org.junit.After;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.StreamingPipe;
import nl.nn.adapterframework.testutil.TestAppender;
import nl.nn.adapterframework.testutil.TestFileUtils;

public abstract class XsltErrorTestBase<P extends StreamingPipe> extends XsltTestBase<P> {

	protected TestAppender testAppender;
	private ErrorOutputStream errorOutputStream;
	private PrintStream prevStdErr;
	public static int EXPECTED_NUMBER_OF_DUPLICATE_LOGGINGS=0;

	private final String FILE_NOT_FOUND_EXCEPTION="Cannot get resource for href [";
	private final String FILE_NOT_FOUND_EXCEPTION_SAXON_10="WARN Fatal transformation error: Exception thrown by URIResolver resolving `";

	private final boolean testForEmptyOutputStream=false;

	protected int getMultiplicity() {
		return 1;
	}

	private class ErrorOutputStream extends OutputStream {
		private StringBuilder line = new StringBuilder();

		@Override
		public void write(int b) throws IOException {
			line.append((char) b);
		}

		@Override
		public String toString() {
			return line.toString();
		}

		public boolean isEmpty() {
			return toString().length() == 0;
		}
	}

	@Before
	public void init() {
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
	@After
	public void tearDown() throws Exception {
		TestAppender.removeAppender(testAppender);
		if (testForEmptyOutputStream) {
			// Xslt processing should not log to stderr
			System.setErr(prevStdErr);
			System.err.println("ErrorStream:"+errorOutputStream);
			assertThat(errorOutputStream.toString(), isEmptyString());
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
	public void duplicateImportErrorAlertsXslt1() throws Exception {
		// this condition appears to result in a warning only for XSLT 2.0 using Saxon
		setStyleSheetName("/Xslt/duplicateImport/root.xsl");
		setXslt2(false);
		pipe.configure();
		checkTestAppender(0,null);
	}

	// detect duplicate imports in configure()
	@Test
	public void duplicateImportErrorAlertsXslt2() throws Exception {
		setStyleSheetName("/Xslt/duplicateImport/root2.xsl");
		setXslt2(true);
		pipe.configure();
		pipe.start();
		checkTestAppender(getMultiplicity()*1,"is included or imported more than once");
	}

	public void duplicateImportErrorProcessing(boolean xslt2) throws Exception {
		setStyleSheetName("/Xslt/duplicateImport/root.xsl");
		setXslt2(xslt2);
		setIndent(true);
		pipe.configure();
		pipe.start();

		String input=TestFileUtils.getTestFile("/Xslt/duplicateImport/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected=TestFileUtils.getTestFile("/Xslt/duplicateImport/out.xml");

		PipeRunResult prr=doPipe(pipe, input, session);
		String result = Message.asString(prr.getResult());

		assertResultsAreCorrect(expected, result, session);
	}


	@Test
	public void duplicateImportErrorProcessingXslt1() throws Exception {
		duplicateImportErrorProcessing(false);
	}

	@Test
	public void duplicateImportErrorProcessingXslt2() throws Exception {
		duplicateImportErrorProcessing(true);
	}

	@Test
	public void documentIncludedInSourceNotFoundXslt1() throws Exception {
		setStyleSheetName("/Xslt/importDocument/importNotFound1.xsl");
		setXslt2(false);
		setIndent(true);
		pipe.configure();
		pipe.start();
		String input = TestFileUtils.getTestFile("/Xslt/importDocument/in.xml");
		String errorMessage = null;
		try {
			doPipe(pipe, input, session);
		} catch (AssumptionViolatedException e) {
			assumeTrue("assumption violated:"+e.getMessage(),false);
		} catch (Exception e) {
			errorMessage = e.getMessage();
			//System.out.println("ErrorMessage: "+errorMessage);
			assertThat(errorMessage,containsString(FILE_NOT_FOUND_EXCEPTION));
		}
		assertThat(testAppender.toString(),containsString(FILE_NOT_FOUND_EXCEPTION));
		System.out.println("ErrorMessage: "+errorMessage);
		if (testForEmptyOutputStream) {
			System.out.println("ErrorStream(=stderr): "+errorOutputStream.toString());
			System.out.println("Clearing ErrorStream, as I am currently unable to catch it");
			errorOutputStream=new ErrorOutputStream();
		}
	}

	@Test
	public void documentIncludedInSourceNotFoundXslt2() throws Exception {
		// error not during configure(), but during doPipe()
		setStyleSheetName("/Xslt/importDocument/importNotFound2.xsl");
		setXslt2(true);
		pipe.configure();
		pipe.start();
		String input = TestFileUtils.getTestFile("/Xslt/importDocument/in.xml");
		String errorMessage = null;
		try {
			doPipe(pipe, input, session);
		} catch (AssumptionViolatedException e) {
			assumeTrue("assumption violated:"+e.getMessage(),false);
		} catch (Exception e) {
			errorMessage = e.getMessage();
			assertThat(errorMessage,containsString(FILE_NOT_FOUND_EXCEPTION));
		}
		// Saxon 9.8 no longer considers a missing import to be fatal. This is similar to Xalan
		assertThat(testAppender.toString(),containsString(FILE_NOT_FOUND_EXCEPTION_SAXON_10));

		System.out.println("ErrorMessage: "+errorMessage);
		if (testForEmptyOutputStream) {
			System.out.println("ErrorStream(=stderr): "+errorOutputStream.toString());
			System.out.println("Clearing ErrorStream, as I am currently unable to catch it");
			errorOutputStream=new ErrorOutputStream();
		}
	}

	@Test
	public void importNotFoundXslt1() throws Exception {
		setStyleSheetName("/Xslt/importNotFound/root.no-validate-xsl");
		setXslt2(false);
		String errorMessage = null;
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
	public void importNotFoundXslt2() throws Exception {
		setStyleSheetName("/Xslt/importNotFound/root2.no-validate-xsl");
		setXslt2(true);
		String errorMessage = null;
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
	public void notifyXalanExtensionsIllegalForSaxon() throws SenderException, TimeoutException, ConfigurationException, IOException, PipeRunException, PipeStartException {
		setStyleSheetName("/Xslt/XalanExtension/XalanExtension.xsl");
		setXslt2(true);
		String errorMessage = null;
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
	public void illegalXPathExpressionXslt2() throws Exception {
		// error not during configure(), but during doPipe()
		setXpathExpression("position()='1'");
		setXslt2(true);
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
			errorOutputStream=new ErrorOutputStream();
		}
	}

	@Test
	public void illegalXPathExpression2Xslt1() throws Exception {
		// error not during configure(), but during doPipe()
		setXpathExpression("<result><status>invalid</status><message>$failureReason</message></result>");
		setXslt2(false);
		String errorMessage = null;
		try {
			pipe.configure();
			fail("Expected to run into an exception");
		} catch (Exception e) {
			errorMessage = e.getMessage();
			assertThat(errorMessage,containsString("<result><status>invalid</status><message>$failureReason</message></result>"));
			assertThat(errorMessage,containsString("A location path was expected, but the following token was encountered:  <"));
		}
		checkTestAppender(2,null);
		System.out.println("ErrorMessage: "+errorMessage);
		if (testForEmptyOutputStream) {
			System.out.println("ErrorStream(=stderr): "+errorOutputStream.toString());
			System.out.println("Clearing ErrorStream, as I am currently unable to catch it");
			errorOutputStream=new ErrorOutputStream();
		}
	}

	@Test
	public void illegalXPathExpression2Xslt2() throws Exception {
		// error not during configure(), but during doPipe()
		setXpathExpression("<result><status>invalid</status><message>$failureReason</message></result>");
		setXslt2(true);
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
			errorOutputStream=new ErrorOutputStream();
		}
	}
}
