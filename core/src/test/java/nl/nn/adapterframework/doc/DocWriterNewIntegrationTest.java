package nl.nn.adapterframework.doc;

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
import nl.nn.adapterframework.util.LogUtil;

public class DocWriterNewIntegrationTest {
	private static Logger log = LogUtil.getLogger(DocWriterNewIntegrationTest.class);
	private static final String TEST_CONFIGURATION_FILE = "testConfiguration.xml";

	@Ignore("This test takes too long.")
	@Test
	public void testStrict() throws Exception {
		String schemaFileName = generateXsd(XsdVersion.STRICT);
		validate(schemaFileName, TEST_CONFIGURATION_FILE);
	}

	@Ignore("This test takes too long.")
	@Test
	public void testCompatibility() throws Exception {
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
}
