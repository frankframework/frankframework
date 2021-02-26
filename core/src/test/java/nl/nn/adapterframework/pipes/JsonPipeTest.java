package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;

public class JsonPipeTest extends PipeTestBase<JsonPipe> {

	@Override
	public JsonPipe createPipe() {
		return new JsonPipe();
	}

	@Test
	public void configureWrongDirection() throws ConfigurationException {
		pipe.setDirection("foutje!");
		exception.expect(ConfigurationException.class);
		exception.expectMessage("illegal value for direction [foutje!], must be 'xml2json' or 'json2xml'");
		pipe.configure();
	}

	@Test
	public void doPipeInputNull() throws Exception {
		pipe.configure();
		pipe.start();
		exception.expectMessage("Pipe [JsonPipe under test] msgId [null] got null input");
		doPipe(pipe, null, session);
	}

	@Test
	public void doPipeIntegerInput() throws Exception {
		pipe.configure();
		pipe.start();
		Integer input = new Integer(1);
		//exception.expect(PipeRunException.class);
		PipeRunResult prr = doPipe(pipe, input, session);

		String result = null;
		try {
			result = Message.asString(prr.getResult());
		} catch (IOException e) {
			fail("cannot open stream: " + e.getMessage());
		}
		assertEquals("<root>1</root>", result);
	}

	@Test
	public void doPipeInputObject() throws Exception {
		pipe.configure();
		pipe.start();
		
		String input = "{ name: Lars }";
		PipeRunResult prr = doPipe(pipe, input, session);

		String result = null;
		try {
			result = Message.asString(prr.getResult());
		} catch (IOException e) {
			fail("cannot open stream: " + e.getMessage());
		}
		assertEquals("<name>Lars</name>", result);

	}

	@Test
	public void doPipeInputArray() throws Exception {
		pipe.configure();
		pipe.start();
		String input = "[ \"Wie\", \"dit leest\", \"is gek\" ]";
		PipeRunResult prr = doPipe(pipe, input, session);

		String result = null;
		try {
			result = Message.asString(prr.getResult());
		} catch (IOException e) {
			fail("cannot open stream: " + e.getMessage());
		}
		assertEquals("<root><array>Wie</array><array>dit leest</array><array>is gek</array></root>", result);
	}

}
