package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunResult;

public class JsonPipeTest extends PipeTestBase<JsonPipe> {

	@Override
	public JsonPipe createPipe() {
		return new JsonPipe();
	}

	@Test
	public void configureWrongDirection() throws ConfigurationException {
		exception.expect(Exception.class);
		exception.expectMessage("unknown direction value [foutje!]. Must be one of [JSON2XML, XML2JSON]");
		pipe.setDirection("foutje!");
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

		String result = prr.getResult().asString();
		assertEquals("<root>1</root>", result);
	}

	@Test
	public void doPipeInputObject() throws Exception {
		pipe.configure();
		pipe.start();
		
		String input = "{ name: Lars }";
		PipeRunResult prr = doPipe(pipe, input, session);

		String result = prr.getResult().asString();
		assertEquals("<name>Lars</name>", result);

	}

	@Test
	public void doPipeInputArray() throws Exception {
		pipe.configure();
		pipe.start();
		String input = "[ \"Wie\", \"dit leest\", \"is gek\" ]";
		PipeRunResult prr = doPipe(pipe, input, session);

		String result = prr.getResult().asString();
		assertEquals("<root><array>Wie</array><array>dit leest</array><array>is gek</array></root>", result);
	}

	@Test
	public void testEmptyXmlElement() throws Exception {
		pipe.setDirection("xml2json");
		pipe.configure();
		pipe.start();
		String input = "<root><value>a</value><empty1></empty1><empty2/></root>";
		String expected ="{\"value\":\"a\",\"empty1\":\"\",\"empty2\":\"\"}";
		PipeRunResult prr = doPipe(pipe, input, session);

		String result = prr.getResult().asString();
		assertEquals(expected, result);
	}
}
