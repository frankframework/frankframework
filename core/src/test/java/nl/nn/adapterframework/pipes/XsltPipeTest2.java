package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;

public class XsltPipeTest2 {

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
	}

	private class TestAppender extends AppenderSkeleton {
		public List<String> messages = new ArrayList<String>();

		public void doAppend(LoggingEvent event) {
			if (event.getLevel().toInt() >= Level.WARN_INT) {
				messages.add(event.getLevel() + " "
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

		public int getNumberOfWarnings() {
			int count = 0;
			for (String message : messages) {
				if (message.startsWith(Level.WARN.toString())) {
					count++;
				}
			}
			return count;
		}

		public int getNumberOfNonWarnings() {
			int count = 0;
			for (String message : messages) {
				if (!message.startsWith(Level.WARN.toString())) {
					count++;
				}
			}
			return count;
		}

		public void printLog() {
			for (String message : messages) {
				System.out.println(message);
			}
		}
	}

	@Before
	public void init() {
		System.setProperty("jdbc.convertFieldnamesToUppercase", "true");
		testAppender = new TestAppender();
		LogUtil.getRootLogger().addAppender(testAppender);
	}

	@Ignore
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
		xsltPipe.start();
		String input = getFile("/Xslt/duplicateImport/in.xml");
		PipeRunResult prr = xsltPipe.doPipe(input, session);

		System.out.println("log:");
		testAppender.printLog();
		System.out.println("systemErr:");
		System.out.println("" + errorOutputStream);
		System.out.println("result:");
		System.out.println("" + prr.getResult());

		assertEquals(true, errorOutputStream.toString().length() == 0);
		assertEquals(0, testAppender.getNumberOfWarnings());
		assertEquals(0, testAppender.getNumberOfNonWarnings());
	}

	@Ignore
	@Test
	public void duplicateImportError2() throws Exception {
		// WARN Nonfatal transformation warning: Stylesheet module
		// file:/.../Xslt/duplicateImport/name2.xsl is included or imported more
		// than once. This is permitted, but may lead to errors or unexpected
		// behavior
		ErrorOutputStream errorOutputStream = new ErrorOutputStream();
		System.setErr(new PrintStream(errorOutputStream));
		XsltPipe xsltPipe = new XsltPipe();
		xsltPipe.registerForward(createPipeSuccessForward());
		xsltPipe.setStyleSheetName("/Xslt/duplicateImport/root2.xsl");
		xsltPipe.setXslt2(true);
		xsltPipe.configure();
		xsltPipe.start();
		String input = getFile("/Xslt/duplicateImport/in.xml");
		PipeRunResult prr = xsltPipe.doPipe(input, session);
		assertEquals(true, errorOutputStream.toString().length() == 0);
		assertEquals(1, testAppender.getNumberOfWarnings());
		assertEquals(0, testAppender.getNumberOfNonWarnings());
	}

	@Ignore
	@Test
	public void documentNotFound() throws Exception {
		// WARN Nonfatal transformation warning: Can not load requested doc:
		// ...\Xslt\documentNotFound\colorLookup.xml (The system cannot find the
		// file specified)
		ErrorOutputStream errorOutputStream = new ErrorOutputStream();
		System.setErr(new PrintStream(errorOutputStream));
		XsltPipe xsltPipe = new XsltPipe();
		xsltPipe.registerForward(createPipeSuccessForward());
		xsltPipe.setStyleSheetName("/Xslt/documentNotFound/root.xsl");
		xsltPipe.setXslt2(false);
		xsltPipe.configure();
		xsltPipe.start();
		String input = getFile("/Xslt/documentNotFound/in.xml");
		PipeRunResult prr = xsltPipe.doPipe(input, session);
		assertEquals(true, errorOutputStream.toString().length() == 0);
		assertEquals(1, testAppender.getNumberOfWarnings());
		assertEquals(0, testAppender.getNumberOfNonWarnings());
	}

	@Ignore
	@Test
	public void documentNotFound2() throws Exception {
		// WARN Nonfatal transformation error: I/O error reported by XML parser
		// processing file:/.../Xslt/documentNotFound/colorLookup.xml:
		// ...\Xslt\documentNotFound\colorLookup.xml (The system cannot find the
		// file specified)
		ErrorOutputStream errorOutputStream = new ErrorOutputStream();
		System.setErr(new PrintStream(errorOutputStream));
		XsltPipe xsltPipe = new XsltPipe();
		xsltPipe.registerForward(createPipeSuccessForward());
		xsltPipe.setStyleSheetName("/Xslt/documentNotFound/root2.xsl");
		xsltPipe.setXslt2(true);
		xsltPipe.configure();
		xsltPipe.start();
		String input = getFile("/Xslt/documentNotFound/in.xml");
		PipeRunResult prr = xsltPipe.doPipe(input, session);
		assertEquals(true, errorOutputStream.toString().length() == 0);
		assertEquals(1, testAppender.getNumberOfWarnings());
		assertEquals(0, testAppender.getNumberOfNonWarnings());
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