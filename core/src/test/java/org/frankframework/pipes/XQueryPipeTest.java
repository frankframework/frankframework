package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;

public class XQueryPipeTest extends PipeTestBase<XQueryPipe> {

	@Override
	public XQueryPipe createPipe() throws ConfigurationException {
		return new XQueryPipe();
	}

	@Test
	public void testMatch() throws ConfigurationException, PipeRunException, IOException {
		pipe.setXqueryName("Pipes/XQueryPipe/xpathExpression.txt");
		pipe.configure();

		final String input = "<user id=\"2\"> <name>Jan</name> <age>48</age> </user>";
		PipeRunResult result = doPipe(pipe, input, session);

		assertEquals("<age>48</age>\n", result.getResult().asString());
	}

	@Test
	public void testNoMatch() throws ConfigurationException, PipeRunException, IOException {
		pipe.setXqueryName("Pipes/XQueryPipe/xpathExpression.txt");
		pipe.configure();

		final String input = "<users> <user id=\"2\"> <name>Jan</name> <age>48</age> </user> </users>";
		PipeRunResult result = doPipe(pipe, input, session);

		assertEquals("", result.getResult().asString());
	}

}
