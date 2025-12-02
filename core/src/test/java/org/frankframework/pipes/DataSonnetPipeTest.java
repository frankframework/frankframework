package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunException;
import org.frankframework.json.DataSonnetOutputType;
import org.frankframework.parameters.JsonParameter;
import org.frankframework.senders.EchoSender;
import org.frankframework.stream.Message;
import org.frankframework.testutil.DateParameterBuilder;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.NumberParameterBuilder;
import org.frankframework.testutil.ParameterBuilder;

public class DataSonnetPipeTest extends PipeTestBase<DataSonnetPipe> {

	@Override
	public DataSonnetPipe createPipe() throws ConfigurationException {
		DataSonnetPipe pipe = new DataSonnetPipe();
		return pipe;
	}

	@Test
	public void simpleMapping() throws Exception {
		pipe.setStyleSheetName("/Pipes/DataSonnet/simple.jsonnet");
		configureAndStartPipe();

		// Act
		Message result = doPipe("Hello World").getResult();

		// Assert
		assertEquals(MediaType.APPLICATION_JSON, result.getContext().getMimeType());
		assertEquals("{\"greetings\":\"Hello World\"}", result.asString());
	}

	@Test
	public void toXmlMapping() throws Exception {
		pipe.setStyleSheetName("/Pipes/DataSonnet/toXml.jsonnet");
		pipe.setOutputType(DataSonnetOutputType.XML);
		configureAndStartPipe();

		// Act
		Message result = doPipe("Hello World").getResult();

		// Assert
		assertEquals(MediaType.APPLICATION_XML, result.getContext().getMimeType());
		assertEquals("<?xml version='1.0' encoding='UTF-8'?><root><my-element>Hello World</my-element></root>", result.asString());
	}

	@Test
	public void mappingWithJsonParams1() throws Exception {
		pipe.setStyleSheetName("/Pipes/DataSonnet/one-param.jsonnet");
		JsonParameter jsonParameter = new JsonParameter();
		jsonParameter.setName("foo");
		jsonParameter.setValue("123");
		pipe.addParameter(jsonParameter); // Value to be converted to JSON
		configureAndStartPipe();

		// Act
		Message result = doPipe("Hello World").getResult();

		// Assert
		assertEquals(MediaType.APPLICATION_JSON, result.getContext().getMimeType());
		assertEquals("""
				{"greetings":"Hello World","param-one":{"foo":123}}""", result.asString());
	}

	@Test
	public void mappingWithJsonParams2() throws Exception {
		pipe.setStyleSheetName("/Pipes/DataSonnet/one-param.jsonnet");
		JsonParameter jsonParameter = new JsonParameter();
		jsonParameter.setName("foo");
		jsonParameter.setValue("{\"bar\":123}");
		pipe.addParameter(jsonParameter); // Value to be converted to JSON
		configureAndStartPipe();

		// Act
		Message result = doPipe("Hello World").getResult();

		// Assert
		assertEquals(MediaType.APPLICATION_JSON, result.getContext().getMimeType());
		assertEquals("""
				{"greetings":"Hello World","param-one":{"bar":123}}""", result.asString());
	}

	@Test
	public void computeMappingWithParams() throws Exception {
		pipe.setStyleSheetName("/Pipes/DataSonnet/one-param.jsonnet");
		pipe.addParameter(ParameterBuilder.create("foo", "{\"bar\":123}")); // text, but b/c computeMimeType=true will be interpreted as JSON
		configureAndStartPipe();
		Message input = new Message("{\"foo\":456}");
		input.getContext().withMimeType(MediaType.APPLICATION_JSON);

		// Act
		Message result = doPipe(input).getResult();

		// Assert
		assertEquals(MediaType.APPLICATION_JSON, result.getContext().getMimeType());
		assertEquals("""
				{"greetings":{"foo":456},"param-one":{"bar":123}}""", result.asString());
	}

	@Test
	public void callSenderNoArgs() throws Exception {
		pipe.setStyleSheetName("/Pipes/DataSonnet/call-sender-no-args.jsonnet");

		EchoSender sender = new EchoSender();
		sender.setName("testName");
		pipe.addSender(sender);

		configureAndStartPipe();

		Message input = new Message("100");
		input.getContext().withMimeType(MediaType.APPLICATION_JSON);

		// Act
		PipeRunException ex = assertThrows(PipeRunException.class, () -> doPipe(input));
		assertTrue(ex.getMessage().contains("Function parameter std not bound in call"), "Exception was: " + ex.getMessage());
	}

	@Test
	public void callSenderOneArg() throws Exception {
		pipe.setStyleSheetName("/Pipes/DataSonnet/call-sender-one-arg.jsonnet");

		EchoSender sender = new EchoSender();
		sender.setName("testName");
		pipe.addSender(sender);

		configureAndStartPipe();

		Message input = new Message("100");
		input.getContext().withMimeType(MediaType.APPLICATION_JSON);

		// Act
		Message result = doPipe(input).getResult();

		// Assert
		assertEquals(MediaType.APPLICATION_JSON, result.getContext().getMimeType());
		MatchUtils.assertJsonEquals("""
				[
					{"string":"101","boolean":"true","number":"1"},
					{"string":"102","boolean":"true","number":"2"},
					{"string":"103","boolean":"true","number":"3"},
					{"string":"104","boolean":"true","number":"4"}
				]""", result.asString());
	}

	@Test
	public void callSenderTwoArgs() throws Exception {
		pipe.setStyleSheetName("/Pipes/DataSonnet/call-sender-two-args.jsonnet");

		EchoSender sender = new EchoSender();
		sender.setName("testName");
		pipe.addSender(sender);

		configureAndStartPipe();

		Message input = new Message("100");
		input.getContext().withMimeType(MediaType.APPLICATION_JSON);

		// Act
		PipeRunException ex = assertThrows(PipeRunException.class, () -> doPipe(input));
		assertTrue(ex.getMessage().contains("Too many args, function has 1 parameter"), "Exception was: " + ex.getMessage());
	}

	@Test
	public void mappingWithMultipleParams() throws Exception {
		pipe.setStyleSheetName("/Pipes/DataSonnet/multiple-params.jsonnet");

		Message jsonInput = new Message("{\"myvalue\":123}");
		jsonInput.getContext().withMimeType(MediaType.APPLICATION_JSON);

		Message jsonParamMessage = new Message("{\"bar\":456}");
		jsonParamMessage.getContext().withMimeType(MediaType.APPLICATION_JSON);
		session.put("jsonMimeType", jsonParamMessage);
		pipe.addParameter(ParameterBuilder.create().withName("foo").withSessionKey("jsonMimeType"));

		pipe.addParameter(NumberParameterBuilder.create("myNumber", 123));
		pipe.addParameter(DateParameterBuilder.create("myDate", "2024-09-02"));
		configureAndStartPipe();

		// Act
		Message result = doPipe(jsonInput).getResult();

		// Assert
		assertEquals(MediaType.APPLICATION_JSON, result.getContext().getMimeType());
		assertEquals("""
				{"greetings":123,"json-param":{"bar":456},"number-param":123,"date-param":"2024-09-02"}""", result.asString());
	}

	@Test
	public void mappingWithXmlParamToJson() throws Exception {
		pipe.setStyleSheetName("/Pipes/DataSonnet/xml-param.jsonnet");

		Message jsonInput = new Message("{\"myvalue\":123}");
		jsonInput.getContext().withMimeType(MediaType.APPLICATION_JSON);

		Message jsonParamMessage = new Message("<xml key=\"value\"><a/><b>c</b></xml>");
		jsonParamMessage.getContext().withMimeType(MediaType.APPLICATION_XML);
		session.put("xmlMimeType", jsonParamMessage);
		pipe.addParameter(ParameterBuilder.create().withName("foo").withSessionKey("xmlMimeType"));

		configureAndStartPipe();

		// Act
		Message result = doPipe(jsonInput).getResult();

		// Assert
		assertEquals(MediaType.APPLICATION_JSON, result.getContext().getMimeType());
		assertEquals("""
				{"root":{"greetings":{"myvalue":123},"param-one":{"xml":{"@key":"value","a":{"$":"","~":1},"b":{"$":"c","~":2},"~":1}}}}""", result.asString());
	}

	@Test
	public void mappingWithXmlParamtoXml() throws Exception {
		pipe.setStyleSheetName("/Pipes/DataSonnet/xml-param.jsonnet");
		pipe.setOutputType(DataSonnetOutputType.XML);

		Message jsonInput = new Message("{\"myvalue\":123}");
		jsonInput.getContext().withMimeType(MediaType.APPLICATION_JSON);

		Message jsonParamMessage = new Message("<xml key=\"value\"><a/><b>c</b></xml>");
		jsonParamMessage.getContext().withMimeType(MediaType.APPLICATION_XML);
		session.put("xmlMimeType", jsonParamMessage);
		pipe.addParameter(ParameterBuilder.create().withName("foo").withSessionKey("xmlMimeType"));

		configureAndStartPipe();

		// Act
		Message result = doPipe(jsonInput).getResult();

		// Assert
		assertEquals(MediaType.APPLICATION_XML, result.getContext().getMimeType());
		assertEquals("""
				<?xml version='1.0' encoding='UTF-8'?>
				<root>
				<greetings><myvalue>123</myvalue></greetings>
				<param-one><xml key="value"><a></a><b>c</b></xml></param-one>
				</root>""".replaceAll("\n", ""), result.asString());
	}
}
