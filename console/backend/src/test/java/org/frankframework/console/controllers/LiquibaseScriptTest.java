package org.frankframework.console.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import org.frankframework.console.ApiException;
import org.frankframework.management.bus.BusMessageUtils;

@ContextConfiguration(classes = {WebTestConfiguration.class, LiquibaseScript.class})
public class LiquibaseScriptTest extends FrankApiTestBase {

	@Test
	public void downloadSingleScript() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<?> inputMessage = i.getArgument(0);
			MessageHeaders headers = inputMessage.getHeaders();
			assertEquals("JDBC_MIGRATION", headers.get("topic"));
			assertEquals("DOWNLOAD", headers.get("action"));
			assertEquals("IAF_Util", BusMessageUtils.getHeader(inputMessage, "configuration"));
			return mockResponseMessage(inputMessage, inputMessage::getPayload, 200, null);
		});

		mockMvc.perform(MockMvcRequestBuilders
				.get("/jdbc/liquibase?configuration=IAF_Util"))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	public void downloadAllScripts() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<?> inputMessage = i.getArgument(0);
			MessageHeaders headers = inputMessage.getHeaders();
			assertEquals("JDBC_MIGRATION", headers.get("topic"));
			assertEquals("DOWNLOAD", headers.get("action"));
			assertNull(headers.get("configuration"));

			return mockResponseMessage(inputMessage, inputMessage::getPayload, 200, null);
		});

		mockMvc.perform(MockMvcRequestBuilders
						.get("/jdbc/liquibase"))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	public void downloadAllScriptsWithConfig() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<?> inputMessage = i.getArgument(0);
			MessageHeaders headers = inputMessage.getHeaders();
			assertEquals("JDBC_MIGRATION", headers.get("topic"));
			assertEquals("DOWNLOAD", headers.get("action"));
			assertEquals("test123", BusMessageUtils.getHeader(inputMessage, "configuration"));

			return mockResponseMessage(inputMessage, inputMessage::getPayload, 200, null);
		});

		mockMvc.perform(MockMvcRequestBuilders
						.get("/jdbc/liquibase?configuration=test123"))
				.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	public void uploadScript() throws Exception {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<?> inputMessage = i.getArgument(0);
			MessageHeaders headers = inputMessage.getHeaders();
			assertEquals("JDBC_MIGRATION", headers.get("topic"));
			assertEquals("UPLOAD", headers.get("action"));
			assertEquals("script.xml", BusMessageUtils.getHeader(inputMessage, "filename"));

			return mockResponseMessage(inputMessage, inputMessage::getPayload, 200, null);
		});

		mockMvc.perform(MockMvcRequestBuilders
						.multipart("/jdbc/liquibase")
						.file(new MockMultipartFile("file", "script.xml", MediaType.TEXT_PLAIN_VALUE, new ByteArrayInputStream("dummy".getBytes())))
						.part(
								new MockPart("configuration", "TestConfiguration".getBytes())
						))
				.andExpect(MockMvcResultMatchers.status().isCreated())
				.andExpect(MockMvcResultMatchers.jsonPath("result").value("dummy"));
	}

	@Test
	public void uploadZipWithScripts() throws Exception {
		MvcResult mockResult = mockMvc.perform(MockMvcRequestBuilders
						.multipart("/jdbc/liquibase")
						.file(new MockMultipartFile("file", "script.zip", MediaType.APPLICATION_OCTET_STREAM_VALUE, new ByteArrayInputStream("dummy".getBytes())))
						.part(
								new MockPart("configuration", "TestConfiguration".getBytes())
						))
				.andExpect(MockMvcResultMatchers.status().isInternalServerError())
				.andExpect(MockMvcResultMatchers.jsonPath("error").value("uploading zip files is not supported!"))
				.andReturn();

		assertInstanceOf(ApiException.class, mockResult.getResolvedException());
	}

}
