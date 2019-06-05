package nl.nn.adapterframework.xslt;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.LogUtil;

public abstract class XsltErrorTestBase<P extends IPipe> extends XsltTestBase<P> {

	protected TestAppender testAppender;
	private ErrorOutputStream errorOutputStream;
	public static int EXPECTED_CONFIG_WARNINGS_FOR_XSLT2_SETTING=1;
	
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

	protected class TestAppender extends AppenderSkeleton {
		public List<String> alerts = new ArrayList<String>();

		@Override
		public void doAppend(LoggingEvent event) {
			if (event.getLevel().toInt() >= Level.WARN_INT) {
				String msg=event.getLevel() + " " + event.getMessage().toString();
				alerts.add(msg);
//				System.out.println("recording alert: "+msg);
//				IllegalStateException e = new IllegalStateException(msg);
//				e.fillInStackTrace();
//				e.printStackTrace(System.out);
			}
		}

		@Override
		public void close() {
		}

		@Override
		public boolean requiresLayout() {
			return false;
		}

		@Override
		protected void append(LoggingEvent event) {
		}

		public int getNumberOfAlerts() {
			return alerts.size();
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (String alert : alerts) {
				sb.append(alert);
				sb.append("\n");
			}
			return sb.toString();
		}
	}

	@Before
	public void init() {
		System.setProperty("jdbc.convertFieldnamesToUppercase", "true");
		testAppender = new TestAppender();
		LogUtil.getRootLogger().addAppender(testAppender);
	}

	protected void redirectErrorOutput() {
		errorOutputStream = new ErrorOutputStream();
		System.setErr(new PrintStream(errorOutputStream));
	}
	
	
	protected void checkTestAppender(int expectedSize, String expectedString) {
		assertThat(testAppender.getNumberOfAlerts(),is(expectedSize));
		if (expectedString!=null) assertThat(testAppender.toString(),containsString(expectedString));
	}
	
	
	// detect duplicate imports in configure()
	@Test
	public void duplicateImportErrorAlertsXslt1() throws Exception {
		// this error only applies to XSLT 2.0
		redirectErrorOutput();
		setStyleSheetName("/Xslt/duplicateImport/root.xsl");
		setXslt2(false);
		pipe.configure();
		assertThat(errorOutputStream.toString(),isEmptyString());
		System.out.println(testAppender.toString());
		checkTestAppender(EXPECTED_CONFIG_WARNINGS_FOR_XSLT2_SETTING*getMultiplicity(),null);
	}

	// detect duplicate imports in configure()
	@Test
	public void duplicateImportErrorAlertsXslt2() throws Exception {
		redirectErrorOutput();
		setStyleSheetName("/Xslt/duplicateImport/root2.xsl");
		setXslt2(true);
		pipe.configure();
		pipe.start();
		assertThat(errorOutputStream.toString(),isEmptyString());
		checkTestAppender(getMultiplicity()*(1+EXPECTED_CONFIG_WARNINGS_FOR_XSLT2_SETTING),"is included or imported more than once");
	}

	public void duplicateImportErrorProcessing(boolean xslt2) throws SenderException, TimeOutException, ConfigurationException, IOException, PipeRunException, PipeStartException {
		setStyleSheetName("/Xslt/duplicateImport/root.xsl");
		setXslt2(xslt2);
		pipe.configure();
		pipe.start();

		String input=TestFileUtils.getTestFile("/Xslt/duplicateImport/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected=TestFileUtils.getTestFile("/Xslt/duplicateImport/out.xml");

		PipeRunResult prr=pipe.doPipe(input, session);

		//assertResultsAreCorrect(expected, prr.getResult().toString(),session);
		assertResultsAreCorrect(expected.replaceAll("\\s",""), prr.getResult().toString().replaceAll("\\s",""),session);
	}

	
	@Test
	public void duplicateImportErrorProcessingXslt1() throws SenderException, TimeOutException, ConfigurationException, IOException, PipeRunException, PipeStartException {
		duplicateImportErrorProcessing(false);
	}

	@Test
	public void duplicateImportErrorProcessingXslt2() throws SenderException, TimeOutException, ConfigurationException, IOException, PipeRunException, PipeStartException {
		duplicateImportErrorProcessing(true);
	}

	@Test
	public void documentNotFoundXslt1() throws Exception {
		redirectErrorOutput();
		setStyleSheetName("/Xslt/documentNotFound/root.xsl");
		setXslt2(false);
		setIndent(true);
		pipe.configure();
		pipe.start();
		String input = TestFileUtils.getTestFile("/Xslt/documentNotFound/in.xml");
		String errorMessage = null;
		try {
			pipe.doPipe(input, session);
		} catch (PipeRunException e) {
			errorMessage = e.getMessage();
			//System.out.println("ErrorMessage: "+errorMessage);
		}
		assertThat(errorOutputStream.toString(),isEmptyString());
		assertThat(errorMessage,containsString("java.io.FileNotFoundException"));
		System.out.println("alerts: "+testAppender);
		assertEquals(EXPECTED_CONFIG_WARNINGS_FOR_XSLT2_SETTING, testAppender.getNumberOfAlerts());
	}

	@Test
	public void documentNotFoundXslt2() throws Exception {
		// error not during configure(), but during doPipe()
		redirectErrorOutput();
		setStyleSheetName("/Xslt/documentNotFound/root2.xsl");
		setXslt2(true);
		pipe.configure();
		pipe.start();
		String input = TestFileUtils.getTestFile("/Xslt/documentNotFound/in.xml");
		String errorMessage = null;
		try {
			pipe.doPipe(input, session);
		} catch (PipeRunException e) {
			errorMessage = e.getMessage();
		}
		assertThat(errorOutputStream.toString(),isEmptyString());
		assertEquals(EXPECTED_CONFIG_WARNINGS_FOR_XSLT2_SETTING, testAppender.getNumberOfAlerts());
		assertThat(errorMessage,containsString("java.io.FileNotFoundException"));
	}

	@Test
	public void importNotFoundXslt1() throws Exception {
		redirectErrorOutput();
		setStyleSheetName("/Xslt/importNotFound/root.no-validate-xsl");
		setXslt2(false);
		pipe.configure();
		assertThat(errorOutputStream.toString(),not(isEmptyString()));
		assertThat(errorOutputStream.toString(),containsString("java.io.FileNotFoundException"));
		checkTestAppender(EXPECTED_CONFIG_WARNINGS_FOR_XSLT2_SETTING*getMultiplicity(),null);
	}

	@Test
	public void importNotFoundXslt2() throws Exception {
		redirectErrorOutput();
		setStyleSheetName("/Xslt/importNotFound/root2.no-validate-xsl");
		setXslt2(true);
		String errorMessage = null;
		try {
			pipe.configure();
		} catch (ConfigurationException e) {
			errorMessage = e.getMessage();
		}
		assertThat(errorOutputStream.toString(),isEmptyOrNullString());
		assertEquals(true, errorOutputStream.isEmpty());
		assertThat(testAppender.getNumberOfAlerts(),is(EXPECTED_CONFIG_WARNINGS_FOR_XSLT2_SETTING*getMultiplicity()));
		assertThat(errorMessage,containsString("Failed to compile stylesheet"));
	}

}