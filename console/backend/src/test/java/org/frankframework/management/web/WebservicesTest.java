package org.frankframework.management.web;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.apache.commons.io.FileUtils;
import org.frankframework.management.bus.message.StringMessage;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfiguration.class, Webservices.class})
public class WebservicesTest extends FrankApiTestBase {

	@Test
	public void testWebservices() throws Exception {
		URL resource = WebservicesTest.class.getResource("/management/web/testWebservices.json");
		String jsonString = FileUtils.readFileToString(new File(resource.toURI()), Charset.defaultCharset());

		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class)))
				.thenAnswer(i -> new StringMessage(jsonString));

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
