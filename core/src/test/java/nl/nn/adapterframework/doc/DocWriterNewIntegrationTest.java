package nl.nn.adapterframework.doc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
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
import org.xml.sax.InputSource;

import nl.nn.adapterframework.doc.model.FrankDocModel;
import nl.nn.adapterframework.doc.model.FrankElement;
import nl.nn.adapterframework.doc.model.FrankElementStatistics;
import nl.nn.adapterframework.doc.model.XsdVersion;
import nl.nn.adapterframework.util.LogUtil;

@Ignore("This test takes too long.")
public class DocWriterNewIntegrationTest {
	private static Logger log = LogUtil.getLogger(DocWriterNewIntegrationTest.class);

	@Test
	public void testStrict() throws Exception {
		String schemaFileName = generateXsd(XsdVersion.STRICT);
		validate(schemaFileName);
	}

	@Test
	public void testCompatibility() throws Exception {
		String schemaFileName = generateXsd(XsdVersion.COMPATIBILITY);
		validate(schemaFileName);
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

	private void validate(String schemaFileName) throws Exception {
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = schemaFactory.newSchema(new File(schemaFileName));
		Validator validator = schema.newValidator();
		validator.validate(new SAXSource(new InputSource(new FileInputStream(new File("testConfiguration.xml")))));
		log.info("Validation of XML document against schema [{}] succeeded", schemaFileName);
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
