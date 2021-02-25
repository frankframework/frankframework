/* 
Copyright 2021 WeAreFrank! 

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
package nl.nn.adapterframework.doc;

import java.util.Arrays;
import java.util.Collection;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import nl.nn.adapterframework.doc.model.FrankDocModel;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;

@RunWith(Parameterized.class)
public class DocWriterNewAndJsonGenerationExamplesTest {
	@Parameters(name = "{1}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
			{"examples-simple-digester-rules.xml", "nl.nn.adapterframework.doc.testtarget.examples.simple.Start", "simple.xsd", "simple.json"}
		});
	}

	@Parameter(0)
	public String digesterRulesFileName;

	@Parameter(1)
	public String startClassName;

	@Parameter(2)
	public String expectedXsdFileName;

	@Parameter(3)
	public String expectedJsonFileName;
	
	@Test
	public void testXsd() throws Exception {
		FrankDocModel model = createModel();
		DocWriterNew docWriter = new DocWriterNew(model);
		docWriter.init(startClassName, XsdVersion.STRICT);
		String actualXsd = docWriter.getSchema();
		System.out.println(actualXsd);
		String expectedXsd = TestFileUtils.getTestFile("/doc/examplesExpected/" + expectedXsdFileName);
		TestAssertions.assertEqualsIgnoreCRLF(expectedXsd, actualXsd);
	}

	private FrankDocModel createModel() throws Exception {
		return FrankDocModel.populate(
				getDigesterRulesPath(digesterRulesFileName), startClassName);
	}

	private String getDigesterRulesPath(String fileName) {
		return "doc/" + fileName;
	}

	@Test
	public void testJson() throws Exception {
		FrankDocModel model = createModel();
		FrankDocJsonFactory jsonFactory = new FrankDocJsonFactory(model);
		JSONObject jsonObject = jsonFactory.getJson();
		String actual = jsonObject.toString(2);
		String expectedJson = TestFileUtils.getTestFile("/doc/examplesExpected/" + expectedJsonFileName);
		TestAssertions.assertEqualsIgnoreCRLF(expectedJson, actual);
	}
}
