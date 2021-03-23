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

import static nl.nn.adapterframework.testutil.MatchUtils.jsonPretty;
import static org.junit.Assume.assumeTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import javax.json.JsonObject;
import javax.xml.XMLConstants;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository;
import nl.nn.adapterframework.frankdoc.model.FrankDocModel;
import nl.nn.adapterframework.frankdoc.model.FrankElement;
import nl.nn.adapterframework.frankdoc.model.FrankElementStatistics;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.util.LogUtil;

public class DocWriterNewIntegrationTest {
	private static Logger log = LogUtil.getLogger(DocWriterNewIntegrationTest.class);
	private static final String TEST_CONFIGURATION_FILE = "testConfiguration.xml";

	private FrankClassRepository classRepository;

	@Before
	public void setUp() {
		classRepository = FrankClassRepository.getReflectInstance();
	}

	@Test
	public void testStrict() throws Exception {
		assumeTrue(TestAssertions.isTestRunningOnCI());
		String schemaFileName = generateXsd(XsdVersion.STRICT, AttributeTypeStrategy.ALLOW_PROPERTY_REF);
		validate(schemaFileName, TEST_CONFIGURATION_FILE);
	}

	@Test
	public void testCompatibility() throws Exception {
		assumeTrue(TestAssertions.isTestRunningOnCI());
		String schemaFileName = generateXsd(XsdVersion.COMPATIBILITY, AttributeTypeStrategy.VALUES_ONLY);
		validate(schemaFileName, TEST_CONFIGURATION_FILE);
	}

	String generateXsd(XsdVersion version, AttributeTypeStrategy attributeTypeStrategy) throws IOException {
		FrankDocModel model = FrankDocModel.populate(classRepository);
		DocWriterNew docWriter = new DocWriterNew(model, attributeTypeStrategy);
		docWriter.init(version);
		String xsdString = docWriter.getSchema();
		File output = new File("FrankConfig-" + docWriter.getOutputFileName());
		log.info("Output file of test xsd: " + output.getAbsolutePath());
		Writer writer = new BufferedWriter(new FileWriter(output));
		try {
			writer.append(xsdString);
		}
		finally {
			writer.close();
		}
		return output.getAbsolutePath();
	}

	private void validate(String schemaFileName, String testConfigurationFileName) throws Exception {
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = schemaFactory.newSchema(new File(schemaFileName));
		Validator validator = schema.newValidator();
		String resourcePath = "/doc/testConfigs/" + testConfigurationFileName;
		Resource resource = Resource.getResource(resourcePath);
		validator.validate(new SAXSource(resource.asInputSource()));
		log.info("Validation of XML document against schema [{}] succeeded", schemaFileName);
	}

	@Test
	public void testExotic() throws Exception {
		String outputFileName = generateXsd(
				XsdVersion.STRICT, "/doc/exotic-digester-rules.xml", "nl.nn.adapterframework.frankdoc.testtarget.exotic.Master", "exotic.xsd", AttributeTypeStrategy.ALLOW_PROPERTY_REF);
		validate(outputFileName, "testExotic.xml");
	}

	private String generateXsd(
			XsdVersion version, final String digesterRulesFileName, final String rootClassName, String outputSchemaFileName, AttributeTypeStrategy attributeTypeStrategy) throws IOException {
		FrankDocModel model = FrankDocModel.populate(digesterRulesFileName, rootClassName, classRepository);
		DocWriterNew docWriter = new DocWriterNew(model, attributeTypeStrategy);
		docWriter.init(rootClassName, version);
		String xsdString = docWriter.getSchema();
		File output = new File(outputSchemaFileName);
		log.info("Output file of test xsd: " + output.getAbsolutePath());
		Writer writer = new BufferedWriter(new FileWriter(output));
		try {
			writer.append(xsdString);
		}
		finally {
			writer.close();
		}
		return output.getAbsolutePath();
	}

	@Ignore
	@Test
	public void testJsonForWebsite() throws Exception {
		FrankDocModel model = FrankDocModel.populate(classRepository);
		FrankDocJsonFactory jsonFactory = new FrankDocJsonFactory(model);
		JsonObject jsonObject = jsonFactory.getJson();
		String jsonText = jsonPretty(jsonObject.toString());
		File output = new File("FrankConfig.json");
		log.info("Output file of test json: " + output.getAbsolutePath());
		Writer writer = new BufferedWriter(new FileWriter(output));
		try {
			writer.append(jsonText);
		}
		finally {
			writer.close();
		}
	}

	@Ignore
	@Test
	public void testStatistics() throws IOException {
		FrankDocModel model = FrankDocModel.populate(classRepository);
		File output = new File("testStatistics.csv");
		System.out.println("Output file of test statistics: " + output.getAbsolutePath());
		Writer writer = new BufferedWriter(new FileWriter(output));
		try {
			writer.append(FrankElementStatistics.header() + "\n");
			for(FrankElement elem: model.getAllElements().values()) {
				writer.append(elem.getStatistics().toString() + "\n");
			}
		}
		finally {
			writer.close();
		}
	}
}
