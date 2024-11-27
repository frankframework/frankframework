package org.frankframework.management.bus.endpoints;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.frankframework.core.BytesResource;
import org.frankframework.core.Resource;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.message.MessageBase;
import org.frankframework.testutil.SpringRootInitializer;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.testutil.TestScopeProvider;
import org.frankframework.util.StreamUtil;

@SpringJUnitConfig(initializers = {SpringRootInitializer.class})
@WithMockUser(roles = { "IbisTester" })
public class TestDatabaseMigrator extends BusTestBase {

	@Test
	public void downloadSingleMigrationScript() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.JDBC_MIGRATION, BusAction.DOWNLOAD);
		request.setHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, getConfiguration().getName());
		Message<?> response = callSyncGateway(request);
		assertEquals("application/xml", BusMessageUtils.getHeader(response, MessageBase.MIMETYPE_KEY));
		InputStream resource = (InputStream) response.getPayload();
		String payload = StreamUtil.streamToString(resource);

		assertThat(payload, Matchers.startsWith("<databaseChangeLog"));
	}

	@Test
	public void downloadAllMigrationScripts() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.JDBC_MIGRATION, BusAction.DOWNLOAD);
		request.setHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, BusMessageUtils.ALL_CONFIGS_KEY);
		Message<?> response = callSyncGateway(request);
		assertEquals("application/octet-stream", BusMessageUtils.getHeader(response, MessageBase.MIMETYPE_KEY));
		Object cdk = BusMessageUtils.getHeader(response, MessageBase.CONTENT_DISPOSITION_KEY);
		assertNotNull(cdk);
		assertThat(""+cdk, Matchers.containsString("DatabaseChangelog.zip"));
		ByteArrayInputStream bais = (ByteArrayInputStream) response.getPayload();
		List<Resource> changelogs = new ArrayList<>();
		try (ZipInputStream is = new ZipInputStream(bais)) {
			ZipEntry entry;
			while((entry = is.getNextEntry()) != null) {
				String name = entry.getName();
				assertFalse(name.contains(":"));
				changelogs.add(new BytesResource(StreamUtil.dontClose(is), name, new TestScopeProvider()));
			}
		}
		assertEquals(1, changelogs.size());
		Resource first = changelogs.remove(0);
		assertEquals("TestConfiguration-DatabaseChangelog.xml", first.getName());
	}

	@Test
	public void uploadMigrationScript() throws Exception {
		String script = TestFileUtils.getTestFile("/Migrator/DatabaseChangelog.xml");
		MessageBuilder<String> request = createRequestMessage(script, BusTopic.JDBC_MIGRATION, BusAction.UPLOAD);
		request.setHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, getConfiguration().getName());
		request.setHeader("filename", "DatabaseChangelog.xml");
		Message<?> response = callSyncGateway(request);
		assertEquals("text/plain", BusMessageUtils.getHeader(response, MessageBase.MIMETYPE_KEY));
		String payload = (String) response.getPayload();

		assertEquals(script, payload);
	}

	@Test
	public void uploadMigrationScriptWithConfigNamePrefix() throws Exception {
		String script = TestFileUtils.getTestFile("/Migrator/DatabaseChangelog.xml");
		MessageBuilder<String> request = createRequestMessage(script, BusTopic.JDBC_MIGRATION, BusAction.UPLOAD);
		request.setHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, getConfiguration().getName());
		request.setHeader("filename", getConfiguration().getName()+"-DatabaseChangelog.xml");
		Message<?> response = callSyncGateway(request);
		assertEquals("text/plain", BusMessageUtils.getHeader(response, MessageBase.MIMETYPE_KEY));
		String payload = (String) response.getPayload();

		assertEquals(script, payload);
	}

	@Test
	public void uploadMigrationScriptNoName() throws Exception {
		String script = TestFileUtils.getTestFile("/Migrator/DatabaseChangelog.xml");
		MessageBuilder<String> request = createRequestMessage(script, BusTopic.JDBC_MIGRATION, BusAction.UPLOAD);
		request.setHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, getConfiguration().getName());
		MessageHandlingException mhe = assertThrows(MessageHandlingException.class, () -> { callSyncGateway(request); }, "expected: filename not provided exception");
		assertTrue(mhe.getCause() instanceof BusException);
	}

	@Test
	public void uploadMigrationScriptDifferentName() throws Exception {
		String script = TestFileUtils.getTestFile("/Migrator/DatabaseChangelog.xml");
		MessageBuilder<String> request = createRequestMessage(script, BusTopic.JDBC_MIGRATION, BusAction.UPLOAD);
		request.setHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, getConfiguration().getName());
		request.setHeader("filename", "wrong-name.xml");
		MessageHandlingException mhe = assertThrows(MessageHandlingException.class, () -> { callSyncGateway(request); }, "expected: filename not provided exception");
		assertTrue(mhe.getCause() instanceof BusException);
	}
}
