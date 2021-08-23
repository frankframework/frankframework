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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository;
import nl.nn.adapterframework.frankdoc.doclet.TestUtil;
import nl.nn.adapterframework.frankdoc.model.FrankDocModel;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.Misc;

@RunWith(Parameterized.class)
public class DocWriterNewAndJsonGenerationExamplesTest {
	@Parameters(name = "{0}-{3}-{4}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
			{XsdVersion.STRICT, "examples-simple-digester-rules.xml", "nl.nn.adapterframework.frankdoc.testtarget.examples.simple.Start", "simple.xsd", "simple.json"},
			{XsdVersion.COMPATIBILITY, "examples-simple-digester-rules.xml", "nl.nn.adapterframework.frankdoc.testtarget.examples.simple.Start", "simpleForCompatibility.xsd", "simple.json"},
			{XsdVersion.STRICT, "examples-sequence-digester-rules.xml", "nl.nn.adapterframework.frankdoc.testtarget.examples.sequence.Master", "sequence.xsd", "sequence.json"},
			{XsdVersion.STRICT, "examples-simple-digester-rules.xml", "nl.nn.adapterframework.frankdoc.testtarget.examples.deprecated.Master", null, "deprecated.json"},
			{XsdVersion.STRICT, "general-test-digester-rules.xml", "nl.nn.adapterframework.frankdoc.testtarget.examples.compatibility.fortype.Start", "compatibility-test-expected-strict.xsd", null},
			{XsdVersion.COMPATIBILITY, "general-test-digester-rules.xml", "nl.nn.adapterframework.frankdoc.testtarget.examples.compatibility.fortype.Start", "compatibility-test-expected-compatibility.xsd", null},
			{XsdVersion.STRICT, "general-test-digester-rules.xml", "nl.nn.adapterframework.frankdoc.testtarget.examples.compatibility.multiple.Start", "compatibility-multiple-test-expected-strict.xsd", null},
			{XsdVersion.COMPATIBILITY, "general-test-digester-rules.xml", "nl.nn.adapterframework.frankdoc.testtarget.examples.compatibility.multiple.Start", "compatibility-multiple-test-expected-compatibility.xsd", null},
			{XsdVersion.STRICT, "general-test-digester-rules.xml", "nl.nn.adapterframework.frankdoc.testtarget.textconfig.Start", "textconfig-expected.xsd", "textconfig-expected.json"},
			{XsdVersion.COMPATIBILITY, "general-test-digester-rules.xml", "nl.nn.adapterframework.frankdoc.testtarget.textconfig.Start", "textconfig-expected-compatibility.xsd", null},
			{XsdVersion.STRICT, "general-test-digester-rules.xml", "nl.nn.adapterframework.frankdoc.testtarget.textconfig.plural.Start", "textconfig-expected-strict-plural.xsd", null},
			{XsdVersion.STRICT, "general-test-digester-rules.xml", "nl.nn.adapterframework.frankdoc.testtarget.examples.ignore.attributes.Master", "ignoreattr.xsd", "ignoreattr.json"}
		});
	}

	@Parameter(0)
	public XsdVersion xsdVersion;

	@Parameter(1)
	public String digesterRulesFileName;

	@Parameter(2)
	public String startClassName;

	@Parameter(3)
	public String expectedXsdFileName;

	@Parameter(4)
	public String expectedJsonFileName;

	private String packageOfClasses;

	@Before
	public void setUp() {
		int idx = startClassName.lastIndexOf(".");
		packageOfClasses = startClassName.substring(0, idx);
	}

	@Test
	public void testXsd() throws Exception {
		assumeNotNull(expectedXsdFileName);
		FrankDocModel model = createModel();
		DocWriterNew docWriter = new DocWriterNew(model, AttributeTypeStrategy.ALLOW_PROPERTY_REF);
		docWriter.init(startClassName, xsdVersion);
		String actualXsd = docWriter.getSchema();
		System.out.println(actualXsd);
		String expectedXsd = TestFileUtils.getTestFile("/doc/examplesExpected/" + expectedXsdFileName);
		TestAssertions.assertEqualsIgnoreCRLF(expectedXsd, actualXsd);
	}

	private FrankDocModel createModel() throws Exception {
		FrankClassRepository classRepository = TestUtil.getFrankClassRepositoryDoclet(packageOfClasses);
		return FrankDocModel.populate(
				getDigesterRulesPath(digesterRulesFileName), startClassName, classRepository);
	}

	private String getDigesterRulesPath(String fileName) {
		return "doc/" + fileName;
	}

	@Test
	public void testJson() throws Exception {
		assumeNotNull(expectedJsonFileName);
		FrankDocModel model = createModel();
		FrankDocJsonFactory jsonFactory = new FrankDocJsonFactory(model);
		JsonObject jsonObject = jsonFactory.getJson();
		String actual = jsonObject.toString();
		System.out.println(Misc.jsonPretty(actual));
		String expectedJson = TestFileUtils.getTestFile("/doc/examplesExpected/" + expectedJsonFileName);
		assertJsonEqual("Comparing JSON", expectedJson, actual);
	}
}
