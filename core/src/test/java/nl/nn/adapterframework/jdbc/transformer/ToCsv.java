package nl.nn.adapterframework.jdbc.transformer;

import nl.nn.adapterframework.stream.Message;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class toCsv {
	private final static String FOLDER = "src/test/resources/Jdbc.transformer";
	private File xmlFile, expectedFile;

	public toCsv(File xmlFile, File expectedFile) {
		this.xmlFile = xmlFile;
		this.expectedFile = expectedFile;
	}

	@Parameterized.Parameters
	public static Collection<Object[]> data() {
		List<Object[]> files = new ArrayList<>();
		int i = 0;
		File xml = new File(FOLDER, i + ".xml");
		File csv = new File(FOLDER, i + ".csv");
		System.out.println(xml.getAbsolutePath());
		while (xml.exists() && csv.exists()) {
			System.out.println(String.format("Added [%s] and [%s]", xml.getName(), csv.getName()));
			files.add(new File[] {xml, csv});
			i++;
			xml = new File(FOLDER, i + ".xml");
			csv = new File(FOLDER, i + ".csv");
		}

		return files;
	}

	@Test
	public void doTest() throws IOException, SAXException {
		String expected = new String(Files.readAllBytes(Paths.get(expectedFile.getAbsolutePath())));

		QueryOutputToCSV transformer = new QueryOutputToCSV();
		String output = transformer.parse(new Message(xmlFile));

		Assert.assertEquals(expected, output);
	}
}
