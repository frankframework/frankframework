package org.frankframework.pipes;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.JsonPipe.Direction;
import org.frankframework.testutil.MatchUtils;

public class JsonPipeTest extends PipeTestBase<JsonPipe> {

	@Override
	public JsonPipe createPipe() {
		return new JsonPipe();
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
		Integer input = Integer.valueOf(1);
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
		MatchUtils.assertXmlEquals("<root><occupation nil=\"true\"/><name>Lars</name><female>false</female><age>15</age><male>true</male></root>", result);
	}

	@Test
	public void doPipeInputArray() throws Exception {
		pipe.configure();
		pipe.start();
		String input = "[ \"Wie\", \"dit leest\", \"is gek\" ]";
		PipeRunResult prr = doPipe(pipe, input, session);

		String result = prr.getResult().asString();
		String expected = "<root><item>Wie</item><item>dit leest</item><item>is gek</item></root>";
		assertEquals("application/xml", prr.getResult().getContext().getMimeType().toString());
		MatchUtils.assertXmlEquals(expected, result);
	}

	@Test
	public void doPipeInputArrayWithoutRootElement() throws Exception {
		pipe.setAddXmlRootElement(false);
		pipe.configure();
		pipe.start();
		String input = "[ \"Wie\", \"dit leest\", \"is gek\" ]";
		PipeRunResult prr = doPipe(pipe, input, session);

		String result = prr.getResult().asString();
		String expected = "<item>Wie</item><item>dit leest</item><item>is gek</item>";
		assertEquals(expected, result);
	}

	@Test
	public void testEmptyXmlElement() throws Exception {
		pipe.setDirection(Direction.XML2JSON);
		pipe.configure();
		pipe.start();

		String input = "<root><value>a</value><empty1></empty1><empty2/><null nil=\"true\"/></root>";
		String expected ="{\"value\":\"a\",\"empty1\":\"\",\"empty2\":\"\",\"null\":null}";

		PipeRunResult prr = doPipe(pipe, input, session);

		String result = prr.getResult().asString();
		assertEquals(expected, result);
		assertEquals("application/json", prr.getResult().getContext().getMimeType().toString());
	}

	@Test
	public void testXmlArray2Json() throws Exception {
		pipe.setDirection(Direction.XML2JSON);
		pipe.configure();
		pipe.start();

		String input = "<root><values><value>a</value><value>a</value><value>a</value></values></root>";
		String expected ="{\"values\":{\"value\":[\"a\",\"a\",\"a\"]}}";

		PipeRunResult prr = doPipe(pipe, input, session);

		String result = prr.getResult().asString();
		assertEquals(expected, result);
	}

	@Test
	public void testXmlArray2JsonWithRoot() throws Exception {
		pipe.setAddXmlRootElement(true);
		pipe.setDirection(Direction.XML2JSON);
		pipe.configure();
		pipe.start();

		String input = "<root><values><value>a</value><value>a</value><value>a</value></values></root>";
		String expected = "{\"root\":{\"values\":{\"value\":[\"a\",\"a\",\"a\"]}}}";

		PipeRunResult prr = doPipe(pipe, input, session);

		String result = prr.getResult().asString();
		assertEquals(expected, result);
	}

	@Test
	public void testXmlArray2JsonWithVersion() throws Exception {
		pipe.setVersion("1");
		pipe.setDirection(Direction.XML2JSON);
		pipe.configure();
		pipe.start();

		String input = "<root><values><value>a</value><value>a</value><value>a</value></values></root>";
		String expected = "{\"root\":{\"values\":{\"value\":[\"a\",\"a\",\"a\"]}}}";

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
		MatchUtils.assertXmlEquals(expected, result);
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
		MatchUtils.assertXmlEquals(expected, result);
	}

	@Test
	public void testJsonWithoutRootTag2XmlArray() throws Exception {
		pipe.configure();
		pipe.start();

		String input ="{\"value\":[\"a\",\"a\",\"a\"]}";
		String expected = "<root><value>a</value><value>a</value><value>a</value></root>";

		PipeRunResult prr = doPipe(pipe, input, session);

		String result = prr.getResult().asString();
		MatchUtils.assertXmlEquals(expected, result);
	}
}
