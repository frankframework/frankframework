package nl.nn.adapterframework.logging;

import static org.junit.Assert.assertTrue;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Test;

import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.pipes.CompareIntegerPipe;
import nl.nn.adapterframework.pipes.UnzipPipe;
import nl.nn.adapterframework.pipes.XmlSwitch;
import nl.nn.adapterframework.pipes.XsltPipe;

public class IbisThrowablePatternConverterTest {

	@Test
	public void testPatternOfaNestedEx() throws PipeRunException {
		final String[] options = {};
		final IbisThrowablePatternConverter converter = IbisThrowablePatternConverter.newInstance(null, options);
		Throwable parent;
		try {
			try {
				try {
					try {
						throw new PipeRunException(new UnzipPipe(), "UnzipPipe");
					} catch (final PipeRunException e) {
						throw new PipeRunException(new XmlSwitch(), "XmlSwitch",e);
					}
				} catch (final PipeRunException e) {
					throw new PipeRunException(new XsltPipe(), "XsltPipe",e);
				}
			} catch (final PipeRunException e) {
				throw new PipeRunException(new CompareIntegerPipe(), "CompareIntegerPipe",e);
			}
		} catch (final PipeRunException e) {
			parent = e;
		}
		final LogEvent event = Log4jLogEvent.newBuilder()
				.setLoggerName("testLogger")
				.setLoggerFqcn(this.getClass().getName())
				.setLevel(Level.ERROR)
				.setMessage(new SimpleMessage("test exception"))
				.setThrown(parent).build();
		final StringBuilder sb = new StringBuilder();
		converter.format(event, sb);
		final String result = sb.toString();
		assertTrue(result.startsWith("nl.nn.adapterframework.core.PipeRunException: CompareIntegerPipe: XsltPipe: XmlSwitch: UnzipPipe"));
	}
}
