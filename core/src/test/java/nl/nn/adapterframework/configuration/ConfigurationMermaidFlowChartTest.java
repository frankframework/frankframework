/*
   Copyright 2019-2020 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.configuration;

import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import org.junit.Test;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

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
		
		MatchUtils.assertMapEquals(stringLinesToMap(expectedMermaid), stringLinesToMap(mermaid));
	}

	private HashMap<String, String> stringLinesToMap(String input) {
		HashMap<String, String> map = new HashMap<>();
		String[] lines = input.split("\n");
		for (int i = 0; i < lines.length; i++) {
			map.put(String.valueOf(i), lines[i]);
		}
		return map;
	}
}