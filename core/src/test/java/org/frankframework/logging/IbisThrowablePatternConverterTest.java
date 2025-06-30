package org.frankframework.logging;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.lessThan;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeRunException;
import org.frankframework.pipes.CompareIntegerPipe;
import org.frankframework.pipes.SwitchPipe;
import org.frankframework.pipes.UnzipPipe;
import org.frankframework.pipes.XsltPipe;

public class IbisThrowablePatternConverterTest {

	@Test
	public void testPatternOfaNestedEx() throws PipeRunException {
//		final String[] options = {"filters(org.junit.runners)"};
//		final ThrowablePatternConverter converter = ExtendedThrowablePatternConverter.newInstance(null, options);
		final String[] options = {};
		final IbisThrowablePatternConverter converter = IbisThrowablePatternConverter.newInstance(null, options);
		Throwable parent;
		try {
			try {
				try {
					try {
						throw new PipeRunException(new UnzipPipe(), "UnzipPipe");
					} catch (final PipeRunException e) {
						throw new PipeRunException(new SwitchPipe(), "XmlSwitch",e);
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
		assertThat(result, startsWith("org.frankframework.core.PipeRunException: Pipe [null] CompareIntegerPipe: Pipe [null] XsltPipe: Pipe [null] XmlSwitch: Pipe [null] UnzipPipe"));
		assertThat(result, anyOf(
				containsString(") ~[junit"), // JDK23 and older print stacktraces looking like this
				containsString(") [junit") // JDK24 prints stacktraces looking like this
		)); // stacktrace must contain package information

		int firstCausedBy = result.indexOf("Caused by");
		assertThat("cannot find first 'Caused By'", firstCausedBy, greaterThan(0));
		int secondCausedBy =  result.indexOf("Caused by", firstCausedBy+10);
		assertThat("cannot find second 'Caused By'", secondCausedBy, greaterThan(0));

		assertThat("'Caused By's too far apart", secondCausedBy-firstCausedBy, lessThan(300));
	}
}
