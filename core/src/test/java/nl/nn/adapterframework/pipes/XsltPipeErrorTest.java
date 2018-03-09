package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

public class XsltPipeErrorTest {

	private IPipeLineSession session = new PipeLineSessionBase();
	private TestAppender testAppender;

	private class ErrorOutputStream extends OutputStream {
		private StringBuilder line = new StringBuilder();

		@Override
		public void write(int b) throws IOException {
			line.append((char) b);
		}

		public String toString() {
			return line.toString();
		}

		public boolean isEmpty() {
			return toString().length() == 0;
		}
	}

	private class TestAppender extends AppenderSkeleton {
		public List<String> alerts = new ArrayList<String>();

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
		System.setProperty("jdbc.convertFieldnamesToUppercase", "true");
		testAppender = new TestAppender();
		LogUtil.getRootLogger().addAppender(testAppender);
	}

	@Test
	public void duplicateImportError() throws Exception {
		// this error only applies to XSLT 2.0
		ErrorOutputStream errorOutputStream = new ErrorOutputStream();
		System.setErr(new PrintStream(errorOutputStream));
		XsltPipe xsltPipe = new XsltPipe();
		xsltPipe.registerForward(createPipeSuccessForward());
		xsltPipe.setStyleSheetName("/Xslt/duplicateImport/root.xsl");
		xsltPipe.setXslt2(false);
		xsltPipe.configure();
		assertEquals(true, errorOutputStream.isEmpty());
		assertEquals(0, testAppender.getNumberOfAlerts());
	}

	@Test
	public void duplicateImportError2() throws Exception {
		ErrorOutputStream errorOutputStream = new ErrorOutputStream();
		System.setErr(new PrintStream(errorOutputStream));
		XsltPipe xsltPipe = new XsltPipe();
		xsltPipe.registerForward(createPipeSuccessForward());
		xsltPipe.setStyleSheetName("/Xslt/duplicateImport/root2.xsl");
		xsltPipe.setXslt2(true);
		xsltPipe.configure();
		assertEquals(true, errorOutputStream.isEmpty());
		assertEquals(1, testAppender.getNumberOfAlerts());
		assertEquals(
				true,
				testAppender.toString().contains(
						"is included or imported more than once"));
	}

	@Test
	public void documentNotFound() throws Exception {
		// error not during configure(), but during doPipe()
		ErrorOutputStream errorOutputStream = new ErrorOutputStream();
		System.setErr(new PrintStream(errorOutputStream));
		XsltPipe xsltPipe = new XsltPipe();
		xsltPipe.registerForward(createPipeSuccessForward());
		xsltPipe.setStyleSheetName("/Xslt/documentNotFound/root.xsl");
		xsltPipe.setXslt2(false);
		xsltPipe.configure();
		xsltPipe.start();
		String input = getFile("/Xslt/documentNotFound/in.xml");
		String errorMessage = null;
		try {
			xsltPipe.doPipe(input, session);
		} catch (PipeRunException e) {
			errorMessage = e.getMessage();
		}
		assertEquals(true, errorOutputStream.isEmpty());
		assertEquals(0, testAppender.getNumberOfAlerts());
		assertEquals(true,
				errorMessage.contains("java.io.FileNotFoundException"));
	}

	@Test
	public void documentNotFound2() throws Exception {
		// error not during configure(), but during doPipe()
		ErrorOutputStream errorOutputStream = new ErrorOutputStream();
		System.setErr(new PrintStream(errorOutputStream));
		XsltPipe xsltPipe = new XsltPipe();
		xsltPipe.registerForward(createPipeSuccessForward());
		xsltPipe.setStyleSheetName("/Xslt/documentNotFound/root2.xsl");
		xsltPipe.setXslt2(true);
		xsltPipe.configure();
		xsltPipe.start();
		String input = getFile("/Xslt/documentNotFound/in.xml");
		String errorMessage = null;
		try {
			xsltPipe.doPipe(input, session);
		} catch (PipeRunException e) {
			errorMessage = e.getMessage();
		}
		assertEquals(true, errorOutputStream.isEmpty());
		assertEquals(0, testAppender.getNumberOfAlerts());
		assertEquals(true,
				errorMessage.contains("java.io.FileNotFoundException"));
	}

	@Test
	public void importNotFound() throws Exception {
		ErrorOutputStream errorOutputStream = new ErrorOutputStream();
		System.setErr(new PrintStream(errorOutputStream));
		XsltPipe xsltPipe = new XsltPipe();
		xsltPipe.registerForward(createPipeSuccessForward());
		xsltPipe.setStyleSheetName("/Xslt/importNotFound/root.xsl");
		xsltPipe.setXslt2(false);
		xsltPipe.configure();
		assertEquals(false, errorOutputStream.isEmpty());
		assertEquals(
				true,
				errorOutputStream.toString().contains(
						"java.io.FileNotFoundException"));
		assertEquals(0, testAppender.getNumberOfAlerts());
	}

	@Test
	public void importNotFound2() throws Exception {
		ErrorOutputStream errorOutputStream = new ErrorOutputStream();
		System.setErr(new PrintStream(errorOutputStream));
		XsltPipe xsltPipe = new XsltPipe();
		xsltPipe.registerForward(createPipeSuccessForward());
		xsltPipe.setStyleSheetName("/Xslt/importNotFound/root2.xsl");
		xsltPipe.setXslt2(true);
		String errorMessage = null;
		try {
			xsltPipe.configure();
		} catch (ConfigurationException e) {
			errorMessage = e.getMessage();
		}
		assertEquals(true, errorOutputStream.isEmpty());
		assertEquals(0, testAppender.getNumberOfAlerts());
		assertEquals(true,
				errorMessage.contains("Failed to compile stylesheet"));
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