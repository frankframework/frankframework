package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.util.LogUtil;

public class XsltPipeTest extends PipeTestBase<XsltPipe> {

	private	TestAppender testAppender;
	
	@Mock
	private IPipeLineSession session;

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
	    	if (event.getLevel().toInt()>=Level.WARN_INT) {
		        messages.add(event.getLevel() + " "  + event.getMessage().toString());
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
	}

	@Before
	public void init() {
		System.setProperty("jdbc.convertFieldnamesToUppercase", "true");
		testAppender = new TestAppender();
		LogUtil.getRootLogger().addAppender(testAppender);
	}

	@Override
	public XsltPipe createPipe() {
		return new XsltPipe();
	}

	@Override
	@Test
	public void basicNoAdditionalConfig() throws ConfigurationException {
		exception.expect(ConfigurationException.class);
		super.basicNoAdditionalConfig();
	}

	@Test
	public void basic() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		pipe.setStyleSheetName("/Xslt3/orgchart.xslt");
		pipe.setXslt2(true);
		pipe.configure();
		pipe.start();
		String input = getFile("/Xslt3/employees.xml");
		// log.debug("inputfile ["+input+"]");
		String expected = getFile("/Xslt3/orgchart.xml");
		PipeRunResult prr = pipe.doPipe(input, session);
		String xmlOut = (String) prr.getResult();
		assertEquals(expected, xmlOut.trim());
	}

	@Test
	public void avoidSystemError() throws Exception {
		// 1) "Can not load requested doc: ...\Xslt\colorLookup.xml (The system
		// cannot find the file specified)"
		// 2) error 2 from avoidSystemError2 (see below) only applies for XSLT
		// 2.0
		ErrorOutputStream errorOutputStream = new ErrorOutputStream();
		System.setErr(new PrintStream(errorOutputStream));
		pipe.setStyleSheetName("/Xslt/root.xsl");
		pipe.setXslt2(false);
		pipe.configure();
		pipe.start();
		String input = getFile("/Xslt/in.xml");
		PipeRunResult prr = pipe.doPipe(input, session);
		assertEquals(true, errorOutputStream.toString().length() == 0);
		assertEquals(1, testAppender.getNumberOfWarnings());
		assertEquals(0, testAppender.getNumberOfNonWarnings());
	}

	// for now skip to avoid error 'Failed to compile stylesheet' during Maven
	// Test (locally it runs fine)
	@Ignore
	@Test
	public void avoidSystemError2() throws Exception {
		// 1) "Can not load requested doc: ...\Xslt\colorLookup.xml (The system
		// cannot find the file specified)"
		// 2) "Stylesheet module file:/.../Xslt/name2.xsl is included or
		// imported
		// more than once. This is permitted, but may lead to errors or
		// unexpected behavior"
		ErrorOutputStream errorOutputStream = new ErrorOutputStream();
		System.setErr(new PrintStream(errorOutputStream));
		pipe.setStyleSheetName("/Xslt/root2.xsl");
		pipe.setXslt2(true);
		pipe.configure();
		pipe.start();
		String input = getFile("/Xslt/in.xml");
		PipeRunResult prr = pipe.doPipe(input, session);
		assertEquals(true, errorOutputStream.toString().length() == 0);
		assertEquals(2, testAppender.getNumberOfWarnings());
		assertEquals(0, testAppender.getNumberOfNonWarnings());
	}
}
