package nl.nn.adapterframework.management.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.management.bus.ResponseMessage;

public class TestShowLiquibaseScript extends FrankApiTestBase<ShowLiquibaseScript>{

	@Override
	public ShowLiquibaseScript createJaxRsResource() {
		return new ShowLiquibaseScript();
	}

	@Test
	public void downloadSingleScript() throws Exception {
		Mockito.doAnswer((i) -> {
			RequestMessageBuilder inputMessage = i.getArgument(0);
			inputMessage.addHeader(ResponseMessage.STATUS_KEY, 200);
			Message<?> msg = inputMessage.build();
			MessageHeaders headers = msg.getHeaders();
			assertEquals("JDBC_MIGRATION", headers.get("topic"));
			assertEquals("DOWNLOAD", headers.get("action"));
			assertEquals("IAF_Util", headers.get("configuration"));

			return msg;
		}).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		dispatcher.dispatchRequest(HttpMethod.GET, "/jdbc/liquibase?configuration=IAF_Util");
	}

	@Test
	public void downloadAllScripts() throws Exception {
		Mockito.doAnswer((i) -> {
			RequestMessageBuilder inputMessage = i.getArgument(0);
			inputMessage.addHeader(ResponseMessage.STATUS_KEY, 200);
			Message<?> msg = inputMessage.build();
			MessageHeaders headers = msg.getHeaders();
			assertEquals("JDBC_MIGRATION", headers.get("topic"));
			assertEquals("DOWNLOAD", headers.get("action"));
			assertEquals("*ALL*", headers.get("configuration"));

			return msg;
		}).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		dispatcher.dispatchRequest(HttpMethod.GET, "/jdbc/liquibase");
	}

	@Test
	public void uploadScript() throws ConfigurationException, IOException {
		List<Attachment> attachments = new ArrayList<Attachment>();
		attachments.add(new StringAttachment("configuration", "TestConfiguration"));
		attachments.add(new FileAttachment("file", new ByteArrayInputStream("dummy".getBytes()), "script.xml"));

		Mockito.doAnswer((i) -> {
			RequestMessageBuilder inputMessage = i.getArgument(0);
			inputMessage.addHeader(ResponseMessage.STATUS_KEY, 200);
			Message<?> msg = inputMessage.build();
			MessageHeaders headers = msg.getHeaders();
			assertEquals("JDBC_MIGRATION", headers.get("topic"));
			assertEquals("UPLOAD", headers.get("action"));
			assertEquals("script.xml", headers.get("filename"));

			return msg;
		}).when(jaxRsResource).sendSyncMessage(any(RequestMessageBuilder.class));

		Response response = dispatcher.dispatchRequest(HttpMethod.POST, "/jdbc/liquibase", attachments);
		assertEquals("{\"result\":\"dummy\"}", response.getEntity().toString());
	}

	@Test
	public void uploadZipWithScripts() throws ConfigurationException, IOException {
		List<Attachment> attachments = new ArrayList<Attachment>();
		attachments.add(new StringAttachment("configuration", "TestConfiguration"));
		attachments.add(new FileAttachment("file", new ByteArrayInputStream("dummy".getBytes()), "script.zip"));

		ApiException ex = assertThrows(ApiException.class, ()->dispatcher.dispatchRequest(HttpMethod.POST, "/jdbc/liquibase", attachments));
		assertEquals("uploading zip files is not supported!", ex.getMessage());
	}
}
