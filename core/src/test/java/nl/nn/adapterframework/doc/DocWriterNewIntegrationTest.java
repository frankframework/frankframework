package nl.nn.adapterframework.doc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.doc.model.FrankDocModel;

public class DocWriterNewIntegrationTest {

	@Ignore
	@Test
	public void testXsd() throws IOException {
		FrankDocModel model = FrankDocModel.populate();
		DocWriterNew docWriter = new DocWriterNew(model);
		String xsdString = docWriter.getSchema();
		File output = new File("testFrankdoc.xsd");
		System.out.println("Output file of test xsd: " + output.getAbsolutePath());
		Writer writer = new BufferedWriter(new FileWriter(output));
		try {
			writer.append(xsdString);
		}
		finally {
			writer.close();
		}
	}
}
