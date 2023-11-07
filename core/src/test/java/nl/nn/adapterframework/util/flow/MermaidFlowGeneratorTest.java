package nl.nn.adapterframework.util.flow;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class MermaidFlowGeneratorTest {

	private static final String BASE_DIR = "/FlowDiagram/Mermaid/";
	private static final String ORIGINAL_FILENAME = "/original.xml";
	private static final String EXPECTED_FILENAME = "/expected.txt";


	@Test
	public void configWithLocalSenders() throws Exception {
		test("ConfigWithLocalSenders");
	}
	@Test
	public void multipleRealisticAdapters() throws Exception {
		test("MultipleRealisticAdapters");
	}

	@Test
	public void extendedAdapterInfo() throws Exception {
		test("ExtendedAdapterInfo");
	}

	@Test
	public void customExtendedAdapterInfo() throws Exception {
		test("CustomExtendedAdapterInfo");
	}

	@Test
	public void noFirstPipe() throws Exception {
		test("NoFirstPipe");
	}

	@Test
	public void globalForwardsAndSwitchPipes() throws Exception {
		test("GlobalForwardsAndSwitchPipes");
	}

	@Test
	public void exit0Validators0Wrappers0() throws Exception {
		test("Exit0Validators0Wrappers0");
	}

	@Test
	public void exit0Validators0Wrappers1() throws Exception {
		test("Exit0Validators0Wrappers1");
	}

	@Test
	public void exit0Validators1Wrappers0() throws Exception {
		test("Exit0Validators1Wrappers0");
	}

	@Test
	public void exit0Validators1Wrappers1() throws Exception {
		test("Exit0Validators1Wrappers1");
	}

	@Test
	public void exit1Validators0Wrappers0() throws Exception {
		test("Exit1Validators0Wrappers0");
	}

	@Test
	public void exit1Validators0Wrappers1() throws Exception {
		test("Exit1Validators0Wrappers1");
	}

	@Test
	public void exit1Validators1Wrappers0() throws Exception {
		test("Exit1Validators1Wrappers0");
	}

	@Test
	public void exit1Validators1Wrappers1() throws Exception {
		test("Exit1Validators1Wrappers1");
	}

	private void test(String directory) throws Exception {
		String testFileDir = BASE_DIR + directory;

		IFlowGenerator generator = new MermaidFlowGenerator();
		generator.afterPropertiesSet();

		String testFile = TestFileUtils.getTestFile(testFileDir + ORIGINAL_FILENAME);
		assertNotNull("unable to find test file ["+testFileDir + ORIGINAL_FILENAME+"]", testFile);
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		generator.generateFlow(testFile, boas);

		String expectedMermaid = TestFileUtils.getTestFile(testFileDir + EXPECTED_FILENAME);
		TestAssertions.assertEqualsIgnoreCRLF(expectedMermaid, boas.toString("UTF-8"));
		generator.destroy();
	}
}
