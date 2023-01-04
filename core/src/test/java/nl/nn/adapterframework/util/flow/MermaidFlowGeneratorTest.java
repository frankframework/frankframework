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
	public void stub4testtoolSwitchPipes() throws Exception {
		stub4testtoolTest("SwitchPipes");
	}

	@Test
	public void stub4testtoolMultipleRealisticAdapters() throws Exception {
		stub4testtoolTest("MultipleRealisticAdapters");
	}

	@Test
	public void stub4testtoolValidatorsAndWrappers() throws Exception {
		stub4testtoolTest("ValidatorsAndWrappers");
	}

	private void stub4testtoolTest(String directory) throws Exception {
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