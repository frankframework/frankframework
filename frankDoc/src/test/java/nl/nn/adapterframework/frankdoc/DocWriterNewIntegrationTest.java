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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository;
import nl.nn.adapterframework.frankdoc.doclet.TestUtil;
import nl.nn.adapterframework.frankdoc.model.FrankDocModel;
import nl.nn.adapterframework.frankdoc.model.FrankElement;
import nl.nn.adapterframework.frankdoc.model.FrankElementStatistics;
import nl.nn.adapterframework.util.LogUtil;

public class DocWriterNewIntegrationTest {
	private static Logger log = LogUtil.getLogger(DocWriterNewIntegrationTest.class);
	private static final String EXOTIC_PACKAGE = "nl.nn.adapterframework.frankdoc.testtarget.exotic.";
	private static final String TEST_CONFIGURATION_FILE = "testConfiguration.xml";

	private FrankClassRepository classRepository;

	@ClassRule
	public static TemporaryFolder testFolder = new TemporaryFolder();

	@Before
	public void setUp() {
		classRepository = TestUtil.getFrankClassRepositoryDoclet(EXOTIC_PACKAGE);
	}

	@Ignore
	@Test
	public void testStrict() throws Exception {
		String schemaFileName = "xml/xsd/FrankConfig-strict.xsd";
		validate(getSchemaFromClasspathResource(schemaFileName), TEST_CONFIGURATION_FILE);
		log.info("Validation of XML document against schema [{}] succeeded", schemaFileName);
	}

	@Ignore
	@Test
	public void testCompatibility() throws Exception {
		String schemaFileName = "xml/xsd/FrankConfig-compatibility.xsd";
		validate(getSchemaFromClasspathResource(schemaFileName), TEST_CONFIGURATION_FILE);
		log.info("Validation of XML document against schema [{}] succeeded", schemaFileName);
	}

	private void validate(Schema schema, String testConfigurationFileName) throws Exception {
		Validator validator = schema.newValidator();
		String resourcePath = "/doc/testConfigs/" + testConfigurationFileName;
		Resource resource = Resource.getResource(resourcePath);
		validator.validate(new SAXSource(resource.asInputSource()));
	}

	private Schema getSchemaFromClasspathResource(String schemaFileName) throws Exception {
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Resource schemaResource = Resource.getResource(schemaFileName);
		Source schemaSource = schemaResource.asSource();
		return schemaFactory.newSchema(schemaSource);
	}

	private Schema getSchemaFromSimpleFile(String schemaFileName) throws Exception {
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		return schemaFactory.newSchema(new File(schemaFileName));
	}

	@Test
	public void testExotic() throws Exception {
		String outputFileName = generateXsd(
				XsdVersion.STRICT, "/doc/exotic-digester-rules.xml", EXOTIC_PACKAGE + "Master", "exotic.xsd", AttributeTypeStrategy.ALLOW_PROPERTY_REF);
		validate(getSchemaFromSimpleFile(outputFileName), "testExotic.xml");
		log.info("Validation of XML document against schema [{}] succeeded", outputFileName);
	}

	private String generateXsd(
			XsdVersion version, final String digesterRulesFileName, final String rootClassName, String outputSchemaFileName, AttributeTypeStrategy attributeTypeStrategy) throws IOException {
		FrankDocModel model = FrankDocModel.populate(digesterRulesFileName, rootClassName, classRepository);
		DocWriterNew docWriter = new DocWriterNew(model, attributeTypeStrategy);
		docWriter.init(rootClassName, version);
		String xsdString = docWriter.getSchema();

		File output = new File(testFolder.getRoot(), outputSchemaFileName);
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
