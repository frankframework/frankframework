package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.pipes.DataSonnetPipe.DataSonnetOutputType;
import org.frankframework.stream.Message;
import org.frankframework.testutil.DateParameterBuilder;
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
	public void mappingWithParams() throws Exception {
		pipe.setStyleSheetName("/Pipes/DataSonnet/one-param.jsonnet");
		pipe.addParameter(ParameterBuilder.create("foo", "{\"bar\":123}")); // text, not interpreted as JSON
		configureAndStartPipe();

		// Act
		Message result = doPipe("Hello World").getResult();

		// Assert
		assertEquals(MediaType.APPLICATION_JSON, result.getContext().getMimeType());
		assertEquals("""
				{"greetings":"Hello World","param-one":"{\\"bar\\":123}"}""", result.asString());
	}

	@Test
	public void computeMappingWithParams() throws Exception {
		pipe.setComputeMimeType(true);
		pipe.setStyleSheetName("/Pipes/DataSonnet/one-param.jsonnet");
		pipe.addParameter(ParameterBuilder.create("foo", "{\"bar\":123}")); // text, not interpreted as JSON
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
