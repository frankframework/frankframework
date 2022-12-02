package nl.nn.adapterframework.management.bus.endpoints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.Test;

import nl.nn.adapterframework.management.bus.BusTestBase;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class TestPipelineTest extends BusTestBase {

	@Test
	public void testCreateContextWithXmlDeclarationAndProcessingInstruction() throws Exception {
		TestPipeline tp = new TestPipeline();
		String input = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<?ibiscontext key1=whitespace is allowed ?>\n"
				+ "<test/>";
		Map<String, String> context = tp.getThreadContextFromPayload(input);
		MatcherAssert.assertThat(context.keySet(), IsIterableContainingInOrder.contains("key1"));
		assertEquals("whitespace is allowed", context.get("key1"));
	}

	@Test
	public void testInvalidXml() throws Exception {
		TestPipeline tp = new TestPipeline();
		String input = "<?ibiscontext key1=whitespace is allowed ?>\n"
				+ "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //Declaration must be on the first line of the document
				+ "<test/>";
		Map<String, String> context = tp.getThreadContextFromPayload(input);
		assertTrue(context.isEmpty());
	}

	@Test
	public void testCreateContextLargeMessage() throws Exception {
		TestPipeline tp = new TestPipeline();
		String message = TestFileUtils.getTestFile("/test1.xml"); //File without xml declaration
		String input = "<?ibiscontext key1=whitespace is allowed ?>"+message;
		Map<String, String> context = tp.getThreadContextFromPayload(input);
		MatcherAssert.assertThat(context.keySet(), IsIterableContainingInOrder.contains("key1"));
		assertEquals("whitespace is allowed", context.get("key1"));
	}

	@Test
	public void testTwoProcessingInstructions() throws Exception {
		TestPipeline tp = new TestPipeline();
		String input = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<?ibiscontext key1=whitespace is allowed ?>\n"
				+ "<?ibiscontext key2=whitespace is allowed ?>\n"
				+ "<test/>";
		Map<String, String> context = tp.getThreadContextFromPayload(input);
		MatcherAssert.assertThat(context.keySet(), IsIterableContainingInOrder.contains("key1", "key2"));
		assertEquals("whitespace is allowed", context.get("key1"));
		assertEquals("whitespace is allowed", context.get("key2"));
	}

	@Test
	public void testCreateContextMessageInbetweenProcessingInstructions() throws Exception {
		TestPipeline tp = new TestPipeline();
		String input = "<?ibiscontext key1=whitespace is allowed ?> <test/> <?ibiscontext key2=whitespace is allowed ?>";
		Map<String, String> context = tp.getThreadContextFromPayload(input);
		MatcherAssert.assertThat(context.keySet(), IsIterableContainingInOrder.contains("key1", "key2"));
		assertEquals("whitespace is allowed", context.get("key1"));
		assertEquals("whitespace is allowed", context.get("key2"));
	}
}