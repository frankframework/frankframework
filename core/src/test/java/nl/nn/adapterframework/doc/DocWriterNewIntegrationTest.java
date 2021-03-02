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

import static org.junit.Assume.assumeTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import javax.xml.XMLConstants;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.doc.model.FrankDocModel;
import nl.nn.adapterframework.doc.model.FrankElement;
import nl.nn.adapterframework.doc.model.FrankElementStatistics;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.util.LogUtil;

public class DocWriterNewIntegrationTest {
	private static Logger log = LogUtil.getLogger(DocWriterNewIntegrationTest.class);
	private static final String TEST_CONFIGURATION_FILE = "testConfiguration.xml";

	@Test
	public void testStrict() throws Exception {
		assumeTrue(TestAssertions.isTestRunningOnCI());
		String schemaFileName = generateXsd(XsdVersion.STRICT);
		validate(schemaFileName, TEST_CONFIGURATION_FILE);
	}

	@Test
	public void testCompatibility() throws Exception {
		assumeTrue(TestAssertions.isTestRunningOnCI());
		String schemaFileName = generateXsd(XsdVersion.COMPATIBILITY);
		validate(schemaFileName, TEST_CONFIGURATION_FILE);
	}

	String generateXsd(XsdVersion version) throws IOException {
		FrankDocModel model = FrankDocModel.populate();
		DocWriterNew docWriter = new DocWriterNew(model);
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
				XsdVersion.STRICT, "/doc/exotic-digester-rules.xml", "nl.nn.adapterframework.doc.testtarget.exotic.Master", "exotic.xsd");
		validate(outputFileName, "testExotic.xml");
	}

	private String generateXsd(XsdVersion version, final String digesterRulesFileName, final String rootClassName, String outputSchemaFileName) throws IOException {
		FrankDocModel model = FrankDocModel.populate(digesterRulesFileName, rootClassName);
		DocWriterNew docWriter = new DocWriterNew(model);
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
	public void testStatistics() throws IOException {
		FrankDocModel model = FrankDocModel.populate();
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

	/**
	 * Martijn wrote this test because he wanted to test something about logging.
	 * He knew that you could add an exception argument e after a log message, like:
	 * <code>log.error("Some message", e);</code>.
	 * But he did not know whether an extra exception argument was possible in
	 * combination with placeholders, like done in this test. In addition, he
	 * wanted to test this in combination with lambdas.
	 * <p>
	 * As it stands, the test does not check the resulting log message automatically.
	 * To use this test, remove the ignore annotation and run it in Eclipse.
	 */
	@Ignore
	@Test
	public void testLoggingWithExtraExceptionArgument() {
		try {
			throw new IllegalStateException();
		} catch(IllegalStateException e) {
			log.error("This is an error with placehold \"{}\"", "placeholder", e);
			log.error("It also works with lambdas: {}", () -> "placeholder", () -> e);
		}
	}
}
