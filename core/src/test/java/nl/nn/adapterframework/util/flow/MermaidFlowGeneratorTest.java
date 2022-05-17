package nl.nn.adapterframework.util.flow;

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;

import org.junit.Test;

import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class MermaidFlowGeneratorTest {

	private static final String BASE_DIR = "/FlowDiagram/Mermaid/";
	private static final String ORIGINAL_FILENAME = "/original.xml";
	private static final String EXPECTED_FILENAME = "/expected.txt";


	@Test
	public void ConfigWithLocalSenders() throws Exception {
		Test("ConfigWithLocalSenders");
	}
	@Test
	public void MultipleRealisticAdapters() throws Exception {
		Test("MultipleRealisticAdapters");
	}

	@Test
	public void ExtendedAdapterInfo() throws Exception {
		Test("ExtendedAdapterInfo");
	}

	@Test
	public void CustomExtendedAdapterInfo() throws Exception {
		Test("CustomExtendedAdapterInfo");
	}

	@Test
	public void NoFirstPipe() throws Exception {
		Test("NoFirstPipe");
	}

	@Test
	public void GlobalForwardsAndSwitchPipes() throws Exception {
		Test("GlobalForwardsAndSwitchPipes");
	}

	@Test
	public void Exit0Validators0Wrappers0() throws Exception {
		Test("Exit0Validators0Wrappers0");
	}

	@Test
	public void Exit0Validators0Wrappers1() throws Exception {
		Test("Exit0Validators0Wrappers1");
	}

	@Test
	public void Exit0Validators1Wrappers0() throws Exception {
		Test("Exit0Validators1Wrappers0");
	}

	@Test
	public void Exit0Validators1Wrappers1() throws Exception {
		Test("Exit0Validators1Wrappers1");
	}

	@Test
	public void Exit1Validators0Wrappers0() throws Exception {
		Test("Exit1Validators0Wrappers0");
	}

	@Test
	public void Exit1Validators0Wrappers1() throws Exception {
		Test("Exit1Validators0Wrappers1");
	}

	@Test
	public void Exit1Validators1Wrappers0() throws Exception {
		Test("Exit1Validators1Wrappers0");
	}

	@Test
	public void Exit1Validators1Wrappers1() throws Exception {
		Test("Exit1Validators1Wrappers1");
	}

	private void Test(String directory) throws Exception {
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
