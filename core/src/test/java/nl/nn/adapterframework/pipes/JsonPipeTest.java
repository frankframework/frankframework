package nl.nn.adapterframework.pipes;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.JsonPipe.Direction;

//@RunWith(Parameterized.class)
public class JsonPipeTest extends PipeTestBase<JsonPipe> {

	@Parameterized.Parameter(0)
	public int version = 3;

	@Parameters(name = "Version: {0}")
	public static Collection<Object> data() {
		return Arrays.asList(new Object[] {1, 2, 3});
	}

	@Override
	public JsonPipe createPipe() {
		JsonPipe pipe = new JsonPipe();
//		pipe.setVersion(version);
		return pipe;
	}

	@Test
	public void testEmptyInput() throws Exception {
		pipe.configure();
		pipe.start();

		PipeRunException e = assertThrows(PipeRunException.class, ()-> doPipe(pipe, null, session));
		assertThat(e.getMessage(), containsString("got null input"));
	}

	@Test
	public void testNullInput() throws Exception {
		pipe.configure();
		pipe.start();

		PipeRunException e = assertThrows(PipeRunException.class, ()-> doPipe(pipe, null, session));
		assertThat(e.getMessage(), containsString("got null input"));
	}

	@Test
	public void doPipeIntegerInput() throws Exception {
		pipe.configure();
		pipe.start();
		Integer input = new Integer(1);
		PipeRunResult prr = doPipe(pipe, input, session);

		String result = prr.getResult().asString();
		assertEquals("<root>1</root>", result);
	}

	@Test
	public void doPipeInputObjectSingleField() throws Exception {
		pipe.setAddXmlRootElement(false);
		pipe.configure();
		pipe.start();

		String input = "{ \"name\": \"Lars\" }";
		PipeRunResult prr = doPipe(pipe, input, session);

		String result = prr.getResult().asString();
		assertEquals("<name>Lars</name>", result);
	}

	@Test
	public void doPipeInputObject() throws Exception {
		pipe.configure();
		pipe.start();

		String input = "{ \"occupation\": null, \"name\": \"Lars\", \"female\":false, \"age\": 15, \"male\":true  }";
		PipeRunResult prr = doPipe(pipe, input, session);

		String result = prr.getResult().asString();
		String expectedNullOccupation = version==3 ? "<occupation nil=\"true\"/>" : "<occupation>null</occupation>";
		assertEquals("<root>"+expectedNullOccupation+"<name>Lars</name><female>false</female><age>15</age><male>true</male></root>", result);
	}

	@Test
	public void doPipeInputArray() throws Exception {
		pipe.configure();
		pipe.start();
		String input = "[ \"Wie\", \"dit leest\", \"is gek\" ]";
		PipeRunResult prr = doPipe(pipe, input, session);

		String result = prr.getResult().asString();
		String expected = "<root><array>Wie</array><array>dit leest</array><array>is gek</array></root>";
		if (version == 3) {
			expected = expected.replaceAll("array>", "item>");
		}
		assertEquals(expected, result);
	}

	@Test
	public void doPipeInputArrayWithoutRootElement() throws Exception {
		pipe.setAddXmlRootElement(false);
		pipe.configure();
		pipe.start();
		String input = "[ \"Wie\", \"dit leest\", \"is gek\" ]";
		PipeRunResult prr = doPipe(pipe, input, session);

		String result = prr.getResult().asString();
		String expected = "<array>Wie</array><array>dit leest</array><array>is gek</array>";
		if (version == 3) {
			expected = expected.replaceAll("array>", "item>");
		}
		assertEquals(expected, result);
	}

	@Test
	public void testEmptyXmlElement() throws Exception {
		pipe.setDirection(Direction.XML2JSON);
		pipe.configure();
		pipe.start();

		String input = "<root><value>a</value><empty1></empty1><empty2/></root>";
		String expected ="{\"value\":\"a\",\"empty1\":\"\",\"empty2\":\"\"}";

		if(version == 1 || version == 3) {
			expected = "{\"root\":{\"empty1\":\"\",\"empty2\":{},\"value\":\"a\"}}"; //empty 2 is an Json Object!
		}
		PipeRunResult prr = doPipe(pipe, input, session);

		String result = prr.getResult().asString();
		assertEquals(expected, result);
	}

	@Test
	public void testXmlArray2Json() throws Exception {
		pipe.setDirection(Direction.XML2JSON);
		pipe.configure();
		pipe.start();

		String input = "<root><values><value>a</value><value>a</value><value>a</value></values></root>";
		String expected ="{\"values\":{\"value\":[\"a\",\"a\",\"a\"]}}";

		if(version == 1 || version == 3) {
			expected = "{\"root\":"+expected+"}";
		}
		PipeRunResult prr = doPipe(pipe, input, session);

		String result = prr.getResult().asString();
		assertEquals(expected, result);
	}

	@Test
	public void testJson2XmlArray() throws Exception {
		pipe.setAddXmlRootElement(false);
		pipe.configure();
		pipe.start();

		String input ="{\"values\":{\"value\":[\"a\",\"a\",\"a\"]}}";
		String expected = "<values><value>a</value><value>a</value><value>a</value></values>";

		PipeRunResult prr = doPipe(pipe, input, session);

		String result = prr.getResult().asString();
		assertEquals(expected, result);
	}

	@Test
	@Ignore("Structure is lost in version 1 and 2")
	public void testJson2XmlNestedArray() throws Exception {
		pipe.setAddXmlRootElement(false);
		pipe.configure();
		pipe.start();

		String input ="{\"values\":{\"value\":[[\"a\",\"a\",\"a\"],[\"b\",\"b\",\"b\"]]}}";
		String expected = "<values><value>a</value><value>a</value><value>a</value><value>b</value><value>b</value><value>b</value></values>";

		PipeRunResult prr = doPipe(pipe, input, session);

		String result = prr.getResult().asString();
		assertEquals(expected, result);
	}

	@Test
	public void testJson2XmlArrayWith1Value() throws Exception {
		pipe.setAddXmlRootElement(false);
		pipe.configure();
		pipe.start();

		String input ="{\"values\":{\"value\":[\"a\"]}}";
		String expected = "<values><value>a</value></values>";

		PipeRunResult prr = doPipe(pipe, input, session);

		String result = prr.getResult().asString();
		assertEquals(expected, result);
	}

	@Test
	public void testJsonWithoutRootTag2XmlArray() throws Exception {
		pipe.configure();
		pipe.start();

		String input ="{\"value\":[\"a\",\"a\",\"a\"]}";
		String expected = "<root><value>a</value><value>a</value><value>a</value></root>";

		PipeRunResult prr = doPipe(pipe, input, session);

		String result = prr.getResult().asString();
		assertEquals(expected, result);
	}
}
