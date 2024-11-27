package org.frankframework.console.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.function.Supplier;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import org.frankframework.management.bus.message.StringMessage;

@ContextConfiguration(classes = {WebTestConfiguration.class, Webservices.class})
public class WebservicesTest extends FrankApiTestBase {

	@Test
	public void testWebservices() throws Exception {
		URL resource = WebservicesTest.class.getResource("/management/web/testWebservices.json");
		String jsonString = FileUtils.readFileToString(new File(resource.toURI()), Charset.defaultCharset());

		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);

			assertEquals("WEBSERVICES", in.getHeaders().get("topic"));
			assertEquals("GET", in.getHeaders().get("action"));

			return new StringMessage(jsonString);
		});

		mockMvc.perform(MockMvcRequestBuilders
						.get("/webservices")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().string(jsonString));
	}

	@Test
	public void getOpenApiSpec() throws Exception {
		testActionAndTopicHeaders("/webservices/openapi.json", "WEBSERVICES", "DOWNLOAD");

		// Make sure uri is posted to the bus
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> in = i.getArgument(0);

			assertEquals("http://google.nl", in.getHeaders().get("meta-uri"));
			Supplier<String> stringSupplier = () -> "{\"topic\":\"WEBSERVICES\",\"action\":\"DOWNLOAD\"}";

			return mockResponseMessage(in, stringSupplier, 200, MediaType.APPLICATION_JSON);
		});

		testActionAndTopicHeaders("/webservices/openapi.json?uri=http://google.nl", "WEBSERVICES", "DOWNLOAD");
	}

	@Test
	public void getWsdl() throws Exception {
		testActionAndTopicHeaders("/webservices/configuration/resourceName", "WEBSERVICES", "DOWNLOAD");
	}

	@Test
	public void getWsdlWithEmptyResourceName() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/webservices/configuration/.wsdl"))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("error").value("no adapter specified"));
	}
}
