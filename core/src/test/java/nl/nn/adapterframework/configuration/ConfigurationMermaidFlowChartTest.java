package nl.nn.adapterframework.configuration;

import java.util.Hashtable;
import java.util.Map;

import org.junit.Test;

import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class ConfigurationMermaidFlowChartTest {

	private static final String STUB4TESTTOOL_XSLT_VALIDATORS_PARAM = "disableValidators";
	private static final String STUB4TESTTOOL_XSLT_MERMAID = "/xml/xsl/adapter2mermaid.xsl";

	private static final String STUB4TESTTOOL_DIRECTORY = "/MermaidFlowChart";
	private static final String STUB4TESTTOOL_ORIGINAL_FILENAME = "original.xml";
	private static final String STUB4TESTTOOL_EXPECTED_FILENAME = "expected.txt";

	@Test
	public void stub4testtoolSwitchPipes() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/SwitchPipes";
		stub4testtoolTest(directory, true);
	}

	@Test
	public void stub4testtoolMultipleRealisticAdapters() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/MultipleRealisticAdapters";
		stub4testtoolTest(directory, true);
	}

	@Test
	public void stub4testtoolValidatorsAndWrappers() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/ValidatorsAndWrappers";
		stub4testtoolTest(directory, true);
	}

	private void stub4testtoolTest(String baseDirectory, boolean disableValidators) throws Exception {
		Map<String, Object> parameters = new Hashtable<String, Object>();
		parameters.put(STUB4TESTTOOL_XSLT_VALIDATORS_PARAM, disableValidators);

		String originalConfiguration = TestFileUtils.getTestFile(baseDirectory + "/" + STUB4TESTTOOL_ORIGINAL_FILENAME);
		String mermaid = ConfigurationUtils.transformConfiguration(originalConfiguration, STUB4TESTTOOL_XSLT_MERMAID, parameters);
		String expectedMermaid = TestFileUtils.getTestFile(baseDirectory + "/" + STUB4TESTTOOL_EXPECTED_FILENAME);

		TestAssertions.assertEqualsIgnoreCRLF(expectedMermaid, mermaid);
	}
}