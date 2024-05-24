package org.frankframework.management.web.spring;

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
}
