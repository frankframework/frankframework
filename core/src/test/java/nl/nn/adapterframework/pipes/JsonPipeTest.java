package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.XML;
import org.junit.Test;
import org.mockito.Mock;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;

public class JsonPipeTest extends PipeTestBase<JsonPipe> {
	@Mock
	private IPipeLineSession session;

	@Override
	public JsonPipe createPipe() {
		return new JsonPipe();
	}

	@Test
	public void configureWrongDirection() throws ConfigurationException {
		pipe.setDirection("foutje!");
		exception.expect(ConfigurationException.class);
		pipe.configure();
	}

	@Test
	public void doPipeInputNull() throws Exception {
		pipe.configure();
		pipe.start();
		exception.expect(PipeRunException.class);
		pipe.doPipe(null, session);
	}

	@Test
	public void doPipeInputWrongObject() throws Exception {
		pipe.configure();
		pipe.start();
		Integer input = new Integer(1);
		exception.expect(PipeRunException.class);
		pipe.doPipe(input, session);
	}

	@Test
	public void doPipeInputObject() throws Exception {
		pipe.configure();
		pipe.start();
		String stringResult = "{ name: Lars }";
		JSONTokener jsonTokener = new JSONTokener(stringResult);
		JSONObject jsonObject = new JSONObject(jsonTokener);
		stringResult = XML.toString(jsonObject);
		assertEquals(stringResult, "<name>Lars</name>");
	}

	@Test
	public void doPipeInputArray() throws Exception {
		pipe.configure();
		pipe.start();
		String stringResult = "[ \"Wie\", \"dit leest\", \"is gek\" ]";
		JSONTokener jsonTokener = new JSONTokener(stringResult);
		JSONArray jsonArray = new JSONArray(jsonTokener);
		stringResult = XML.toString(jsonArray);
		assertEquals(stringResult,
				"<array>Wie</array><array>dit leest</array><array>is gek</array>");
	}

}
