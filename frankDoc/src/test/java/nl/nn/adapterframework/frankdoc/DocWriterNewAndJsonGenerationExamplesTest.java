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
package nl.nn.adapterframework.frankdoc;

import static nl.nn.adapterframework.testutil.MatchUtils.assertJsonEqual;
import static org.junit.Assume.assumeNotNull;

import java.util.Arrays;
import java.util.Collection;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository;
import nl.nn.adapterframework.frankdoc.model.FrankDocModel;
import nl.nn.adapterframework.frankdoc.model.FrankElementFilters;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;

@RunWith(Parameterized.class)
public class DocWriterNewAndJsonGenerationExamplesTest {
	@Parameters(name = "{1}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
			{"examples-simple-digester-rules.xml", "nl.nn.adapterframework.frankdoc.testtarget.examples.simple.Start", "simple.xsd", "simple.json"},
			{"examples-sequence-digester-rules.xml", "nl.nn.adapterframework.frankdoc.testtarget.examples.sequence.Master", "sequence.xsd", "sequence.json"},
			{"examples-simple-digester-rules.xml", "nl.nn.adapterframework.frankdoc.testtarget.examples.deprecated.Master", null, "deprecated.json"}
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
		assumeNotNull(expectedXsdFileName);
		FrankDocModel model = createModel();
		DocWriterNew docWriter = new DocWriterNew(model, AttributeTypeStrategy.ALLOW_PROPERTY_REF);
		docWriter.init(startClassName, XsdVersion.STRICT);
		String actualXsd = docWriter.getSchema();
		System.out.println(actualXsd);
		String expectedXsd = TestFileUtils.getTestFile("/doc/examplesExpected/" + expectedXsdFileName);
		TestAssertions.assertEqualsIgnoreCRLF(expectedXsd, actualXsd);
	}

	private FrankDocModel createModel() throws Exception {
		FrankClassRepository classRepository = FrankClassRepository.getReflectInstance(
				FrankElementFilters.getIncludeFilter(), FrankElementFilters.getExcludeFilter(), FrankElementFilters.getExcludeFiltersForSuperclass());
		return FrankDocModel.populate(
				getDigesterRulesPath(digesterRulesFileName), startClassName, classRepository);
	}

	private String getDigesterRulesPath(String fileName) {
		return "doc/" + fileName;
	}

	@Test
	public void testJson() throws Exception {
		FrankDocModel model = createModel();
		FrankDocJsonFactory jsonFactory = new FrankDocJsonFactory(model);
		JsonObject jsonObject = jsonFactory.getJson();
		String actual = jsonObject.toString();
		String expectedJson = TestFileUtils.getTestFile("/doc/examplesExpected/" + expectedJsonFileName);
		assertJsonEqual("Comparing JSON", expectedJson, actual);
	}
}
