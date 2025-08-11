package org.frankframework.util.flow;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.frankframework.testutil.TestAssertions;
import org.frankframework.testutil.TestFileUtils;

class MermaidFlowGeneratorTest {

	private static final String BASE_DIR = "/FlowDiagram/Mermaid/";
	private static final String ORIGINAL_FILENAME = "/original.xml";
	private static final String EXPECTED_FILENAME = "/expected.txt";

	@ParameterizedTest
	@ValueSource(strings = {
			"ConfigWithLocalSenders", "MultipleRealisticAdapters", "ExtendedAdapterInfo",
			"CustomExtendedAdapterInfo", "NoFirstPipe", "GlobalForwardsAndSwitchPipes",
			"Exit0Validators0Wrappers0", "Exit0Validators0Wrappers1",
			"Exit0Validators1Wrappers0", "Exit0Validators1Wrappers1",
			"Exit1Validators0Wrappers0", "Exit1Validators0Wrappers1",
			"Exit1Validators1Wrappers0", "Exit1Validators1Wrappers1",
			"NoExplicitExitThenImplicitSuccessExit", "OnlyExplicitErrorExitThenImplicitSuccessExit",
			"OnlyExplicitSuccessThenNoImplicitError", "ExplicitSuccessAndErrorThenNothingImplicit",
			"CombineForwardsBetweenSameNodes"
	})

	void test(String directory) throws Exception {
		String testFileDir = BASE_DIR + directory;

		IFlowGenerator generator = new MermaidFlowGenerator();
		generator.afterPropertiesSet();

		String testFile = TestFileUtils.getTestFile(testFileDir + ORIGINAL_FILENAME);
		assertNotNull(testFile, "unable to find test file [" + testFileDir + ORIGINAL_FILENAME + "]");
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		generator.generateFlow(testFile, boas);

		String expectedMermaid = TestFileUtils.getTestFile(testFileDir + EXPECTED_FILENAME);
		TestAssertions.assertEqualsIgnoreCRLF(expectedMermaid, boas.toString(StandardCharsets.UTF_8));
		generator.destroy();
	}
}
