package nl.nn.adapterframework.doc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.doc.model.FrankDocModel;
import nl.nn.adapterframework.doc.model.FrankElement;
import nl.nn.adapterframework.doc.model.FrankElementStatistics;
import nl.nn.adapterframework.doc.model.XsdVersion;

@Ignore("Test takes a long time to run, and gives little information")
public class DocWriterNewIntegrationTest {

	@Test
	public void testStrict() throws IOException {
		generateXsd(XsdVersion.STRICT);
	}

	@Test
	public void testCompatibility() throws IOException {
		generateXsd(XsdVersion.COMPATIBILITY);
	}

	private void generateXsd(XsdVersion version) throws IOException {
		FrankDocModel model = FrankDocModel.populate();
		DocWriterNew docWriter = new DocWriterNew(model);
		docWriter.init(version);
		String xsdString = docWriter.getSchema();
		File output = new File("test" + docWriter.getOutputFileName());
		System.out.println("Output file of test xsd: " + output.getAbsolutePath());
		Writer writer = new BufferedWriter(new FileWriter(output));
		try {
			writer.append(xsdString);
		}
		finally {
			writer.close();
		}
	}

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
