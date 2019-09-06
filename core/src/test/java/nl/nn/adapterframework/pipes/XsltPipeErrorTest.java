package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import static org.hamcrest.core.StringContains.*;
import static org.hamcrest.text.IsEmptyString.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.xslt.XsltErrorTestBase;

public class XsltPipeErrorTest {

	private IPipeLineSession session = new PipeLineSessionBase();
	private TestAppender testAppender;
	private ErrorOutputStream errorOutputStream;

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

	}

	private class TestAppender extends AppenderSkeleton {
		public List<String> alerts = new ArrayList<String>();

		@Override
		public void doAppend(LoggingEvent event) {
			if (event.getLevel().toInt() >= Level.WARN_INT) {
				alerts.add(event.getLevel() + " "
						+ event.getMessage().toString());
			}
		}

		@Override
		public void close() {
			// TODO Auto-generated method stub
		}

		@Override
		public boolean requiresLayout() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		protected void append(LoggingEvent event) {
			// TODO Auto-generated method stub
		}

		public int getNumberOfAlerts() {
			return alerts.size();
		}

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
		testAppender = new TestAppender();
		LogUtil.getRootLogger().addAppender(testAppender);
		errorOutputStream = new ErrorOutputStream();
		System.setErr(new PrintStream(errorOutputStream));
	}

	@After
	public void finalChecks() {
		// Xslt processing should not log to stderr
		assertThat(errorOutputStream.toString(), isEmptyString());
	}

	@Test
	public void duplicateImportErrorXslt1() throws Exception {
		// this condition appears to result in a warning only for XSLT 2.0 using Saxon
		XsltPipe xsltPipe = new XsltPipe();
		xsltPipe.registerForward(createPipeSuccessForward());
		xsltPipe.setStyleSheetName("/Xslt/duplicateImport/root.xsl");
		xsltPipe.configure();
		//System.out.println("LogAppender: "+testAppender);
		assertThat(testAppender.toString(),isEmptyOrNullString());
		//assertEquals(0, testAppender.getNumberOfAlerts());
	}

	@Test
	public void duplicateImportErrorXslt2() throws Exception {
		XsltPipe xsltPipe = new XsltPipe();
		xsltPipe.registerForward(createPipeSuccessForward());
		xsltPipe.setStyleSheetName("/Xslt/duplicateImport/root2.xsl");
		xsltPipe.configure();
		//System.out.println("LogAppender: "+testAppender);
		assertThat(testAppender.toString(),containsString("is included or imported more than once"));
		assertEquals(1, testAppender.getNumberOfAlerts());
	}

	@Test
	public void documentIncludedInSourceNotFoundXslt1() throws Exception {
		// error not during configure(), but during doPipe()
		XsltPipe xsltPipe = new XsltPipe();
		xsltPipe.registerForward(createPipeSuccessForward());
		xsltPipe.setStyleSheetName("/Xslt/documentNotFound/root.xsl");
		xsltPipe.configure();
		xsltPipe.start();
		String input = getFile("/Xslt/documentNotFound/in.xml");
		String errorMessage = null;
		try {
			PipeRunResult prr = xsltPipe.doPipe(input, session);
			System.out.print("PipeForward: "+prr.getPipeForward().getName());
			//fail("expected transformation to fail because an included document could not be found");
		} catch (PipeRunException e) {
			errorMessage = e.getMessage();
			assertThat(errorMessage,containsString("java.io.FileNotFoundException"));
		}
		assertThat(testAppender.toString(),containsString("java.io.FileNotFoundException"));
		//System.out.println("LogAppender: "+testAppender);
		assertEquals(1+XsltErrorTestBase.EXPECTED_NUMBER_OF_DUPLICATE_LOGGINGS, testAppender.getNumberOfAlerts());
	}

	@Test
	public void documentIncludedInSourceNotFoundXslt2() throws Exception {
		XsltPipe xsltPipe = new XsltPipe();
		xsltPipe.registerForward(createPipeSuccessForward());
		xsltPipe.setStyleSheetName("/Xslt/documentNotFound/root2.xsl");
		xsltPipe.configure();
		xsltPipe.start();
		String input = getFile("/Xslt/documentNotFound/in.xml");
		String errorMessage = null;
		try {
			PipeRunResult prr = xsltPipe.doPipe(input, session);
			System.out.print("PipeForward: "+prr.getPipeForward().getName());
			//fail("expected transformation to fail because an included document could not be found");
		} catch (PipeRunException e) {
			errorMessage = e.getMessage();
			assertThat(errorMessage,containsString("java.io.FileNotFoundException"));
		}
		// TODO fix that we get a proper error message in this case!
		//assertThat(testAppender.toString(),containsString("java.io.FileNotFoundException"));
		//assertEquals(1, testAppender.getNumberOfAlerts());
	}

	@Test
	public void importNotFoundXslt1() throws Exception {
		XsltPipe xsltPipe = new XsltPipe();
		xsltPipe.registerForward(createPipeSuccessForward());
		xsltPipe.setStyleSheetName("/Xslt/importNotFound/root.no-validate-xsl");
		String errorMessage = null;
		try {
			xsltPipe.configure();
			fail("expected configuration to fail because an import could not be found");
		} catch (ConfigurationException e) {
			errorMessage = e.getMessage();
			assertThat(errorMessage,containsString("java.io.FileNotFoundException"));
		}
		//assertEquals(true, errorOutputStream.toString().contains("java.io.FileNotFoundException"));
		assertThat(testAppender.toString(),containsString("java.io.FileNotFoundException"));
		//System.out.println("LogAppender: "+testAppender);
		assertEquals(1, testAppender.getNumberOfAlerts());
	}

	@Test
	public void importNotFoundXslt2() throws Exception {
		XsltPipe xsltPipe = new XsltPipe();
		xsltPipe.registerForward(createPipeSuccessForward());
		xsltPipe.setStyleSheetName("/Xslt/importNotFound/root2.no-validate-xsl");
		String errorMessage = null;
		try {
			xsltPipe.configure();
			fail("expected configuration to fail because an import could not be found");
		} catch (ConfigurationException e) {
			errorMessage = e.getMessage();
			assertThat(errorMessage,containsString("Failed to compile stylesheet"));
		}
		assertThat(testAppender.toString(),containsString("java.io.FileNotFoundException"));
		assertEquals(1, testAppender.getNumberOfAlerts());
	}

	private PipeForward createPipeSuccessForward() {
		PipeForward pipeForward = new PipeForward();
		pipeForward.setName("success");
		return pipeForward;
	}

	private String getFile(String filename) throws SAXException, IOException {
		URL url = ClassUtils.getResourceURL(this, filename);
		if (url == null) {
			throw new IOException("cannot find resource [" + filename + "]");
		}
		return Misc.resourceToString(url);
	}

}