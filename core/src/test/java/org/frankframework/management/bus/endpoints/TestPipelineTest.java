package org.frankframework.management.bus.endpoints;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.frankframework.configuration.Configuration;
import org.frankframework.core.Adapter;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.testutil.SpringRootInitializer;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.StreamUtil;

@SpringJUnitConfig(initializers = {SpringRootInitializer.class})
public class TestPipelineTest extends BusTestBase {
	private static final String TEST_PIPELINE_ADAPER_NAME = "TestPipelineAdapter";
	private Adapter adapter;

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		adapter = registerTestPipelineAdapter(getConfiguration());
	}

	@AfterEach
	@Override
	public void tearDown() {
		if(adapter != null) {
			adapter.stopRunning();
			getConfiguration().getAdapterManager().removeAdapter(adapter);
		}
		super.tearDown();
	}

	protected Adapter registerTestPipelineAdapter(Configuration configuration) {
		Adapter adapter = new TestPipelineSessionAdapter();
		SpringUtils.autowireByName(configuration, adapter);
		adapter.setName(TEST_PIPELINE_ADAPER_NAME);

		getConfiguration().addAdapter(adapter);
		return adapter;
	}

	public static class TestPipelineSessionAdapter extends Adapter {
		@Override
		public PipeLineResult processMessageDirect(String messageId, org.frankframework.stream.Message message, PipeLineSession session) {
			try {
				String action = message.asString();
				if(action.startsWith("sessionKey")) {
					assertEquals("sessionKeyValue", session.get("sessionKeyName"));
				}
				else if(action.startsWith("<?ibiscontext")) {
					assertEquals("piValue", session.get("piName"));
				}
			} catch (IOException e) {
				fail(e.getMessage());
			}
			PipeLineResult plr = new PipeLineResult();
			plr.setResult(message);
			plr.setState(ExitState.SUCCESS);
			return plr;
		}
	}

	@Test
	public void testCreateContextWithXmlDeclarationAndProcessingInstruction() {
		TestPipeline tp = new TestPipeline();
		String input = """
				<?xml version="1.0" encoding="UTF-8"?>
				<?ibiscontext key1=whitespace is allowed ?>
				<test/>\
				""";
		Map<String, String> context = tp.getSessionKeysFromPayload(input);
		assertThat(context.keySet(), IsIterableContainingInOrder.contains("key1"));
		assertEquals("whitespace is allowed", context.get("key1"));
	}

	@Test
	public void testInvalidXml() {
		TestPipeline tp = new TestPipeline();
		String input = """
				<?ibiscontext key1=whitespace is allowed ?>
				<?xml version="1.0" encoding="UTF-8"?>
				<test/>\
				""";
		Map<String, String> context = tp.getSessionKeysFromPayload(input);
		assertTrue(context.isEmpty());
	}

	@Test
	public void testCreateContextLargeMessage() throws Exception {
		TestPipeline tp = new TestPipeline();
		String message = TestFileUtils.getTestFile("/test1.xml"); //File without xml declaration
		String input = "<?ibiscontext key1=whitespace is allowed ?>"+message;
		Map<String, String> context = tp.getSessionKeysFromPayload(input);
		assertThat(context.keySet(), IsIterableContainingInOrder.contains("key1"));
		assertEquals("whitespace is allowed", context.get("key1"));
	}

	@Test
	public void testTwoProcessingInstructions() {
		TestPipeline tp = new TestPipeline();
		String input = """
				<?xml version="1.0" encoding="UTF-8"?>
				<?ibiscontext key1=whitespace is allowed ?>
				<?ibiscontext key2=whitespace is allowed ?>
				<test/>\
				""";
		Map<String, String> context = tp.getSessionKeysFromPayload(input);
		assertThat(context.keySet(), IsIterableContainingInOrder.contains("key1", "key2"));
		assertEquals("whitespace is allowed", context.get("key1"));
		assertEquals("whitespace is allowed", context.get("key2"));
	}

	@Test
	public void testCreateContextMessageInbetweenProcessingInstructions() {
		TestPipeline tp = new TestPipeline();
		String input = "<?ibiscontext key1=whitespace is allowed ?> <test/> <?ibiscontext key2=whitespace is allowed ?>";
		Map<String, String> context = tp.getSessionKeysFromPayload(input);
		assertThat(context.keySet(), IsIterableContainingInOrder.contains("key1", "key2"));
		assertEquals("whitespace is allowed", context.get("key1"));
		assertEquals("whitespace is allowed", context.get("key2"));
	}

	@Test
	@WithMockUser(roles = { "IbisTester" })
	public void testWithoutThreadContext() throws Exception {
		MessageBuilder<String> request = createRequestMessage("normal payload", BusTopic.TEST_PIPELINE, BusAction.UPLOAD);
		request.setHeader("configuration", getConfiguration().getName());
		request.setHeader("adapter", TEST_PIPELINE_ADAPER_NAME);
		Message<?> response = callSyncGateway(request);
		assertEquals("normal payload", responseToString(response.getPayload()));
	}

	@Test
	@WithMockUser(roles = { "IbisTester" })
	public void testWithSession() throws Exception {
		MessageBuilder<String> request = createRequestMessage("sessionKey", BusTopic.TEST_PIPELINE, BusAction.UPLOAD);
		request.setHeader("configuration", getConfiguration().getName());
		request.setHeader("adapter", TEST_PIPELINE_ADAPER_NAME);
		request.setHeader("sessionKeys", "[{\"key\":\"sessionKeyName\",\"value\":\"sessionKeyValue\"}]");
		Message<?> response = callSyncGateway(request);
		assertEquals("sessionKey", responseToString(response.getPayload()));
	}

	@Test
	@WithMockUser(roles = { "IbisTester" })
	public void testWithProcessingInstruction() throws Exception {
		MessageBuilder<String> request = createRequestMessage("<?ibiscontext piName=piValue ?>\n<dummy/>", BusTopic.TEST_PIPELINE, BusAction.UPLOAD);
		request.setHeader("configuration", getConfiguration().getName());
		request.setHeader("adapter", TEST_PIPELINE_ADAPER_NAME);
		Message<?> response = callSyncGateway(request);
		assertEquals("<?ibiscontext piName=piValue ?>\n<dummy/>", responseToString(response.getPayload()));
	}

	private String responseToString(Object payload) throws IOException {
		if(payload instanceof String string) {
			return string;
		}

		return StreamUtil.streamToString((InputStream) payload);
	}
}
