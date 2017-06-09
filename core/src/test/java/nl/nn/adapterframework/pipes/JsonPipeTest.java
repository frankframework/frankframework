package nl.nn.adapterframework.pipes;

import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;

import static org.junit.Assert.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.XML;
import org.junit.Rule;

public class JsonPipeTest {
	@Mock
	private IPipeLineSession<?, ?> session;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	private JsonPipe jsonPipe = new JsonPipe();

	@Test
	public void configureInputNull() throws ConfigurationException {
		exception.expect(ConfigurationException.class);
		jsonPipe.configure();
	}

	@Test
	public void configureInputWrong() throws ConfigurationException {
		jsonPipe.setDirection("foutje!");
		exception.expect(ConfigurationException.class);
		jsonPipe.configure();
	}

	@Test
	public void doPipeInputNull() throws Exception {
		exception.expect(PipeRunException.class);
		jsonPipe.doPipe(null, session);
	}

	@Test
	public void doPipeInputWrongObject() throws Exception {
		Integer input = new Integer(1);
		exception.expect(PipeRunException.class);
		jsonPipe.doPipe(input, session);
	}

	@Test
	public void doPipeInputObject() throws Exception {
		String stringResult = "{ name: Lars }";
		JSONTokener jsonTokener = new JSONTokener(stringResult);
		JSONObject jsonObject = new JSONObject(jsonTokener);
		stringResult = XML.toString(jsonObject);
		assertEquals(stringResult, "<name>Lars</name>");
	}

	@Test
	public void doPipeInputArray() throws Exception {
		String stringResult = "[ \"Wie\", \"dit leest\", \"is gek\" ]";
		JSONTokener jsonTokener = new JSONTokener(stringResult);
		JSONArray jsonArray = new JSONArray(jsonTokener);
		stringResult = XML.toString(jsonArray);
		assertEquals(stringResult,
				"<array>Wie</array><array>dit leest</array><array>is gek</array>");
	}
}
