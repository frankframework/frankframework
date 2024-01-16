package org.frankframework.jdbc.transformer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.frankframework.stream.FileMessage;
import org.frankframework.testutil.TestFileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xml.sax.SAXException;

@RunWith(Parameterized.class)
public class ToCsvTest {
	private static final String FOLDER = "/Jdbc.transformer/";
	private static final String SRC = "src/test/resources" + FOLDER;
	private final File xmlFile;
	private final File expectedFile;

	public ToCsvTest(File xmlFile, File expectedFile) {
		this.xmlFile = xmlFile;
		this.expectedFile = expectedFile;
	}

	@Parameterized.Parameters
	public static Collection<Object[]> data() {
		List<Object[]> files = new ArrayList<>();
		int i = 0;
		File xml = new File(SRC, i + ".xml");
		File csv = new File(SRC, i + ".csv");
		System.out.println(xml.getAbsolutePath());
		while (xml.exists() && csv.exists()) {
			System.out.println(String.format("Added [%s] and [%s]", xml.getName(), csv.getName()));
			files.add(new File[] {xml, csv});
			i++;
			xml = new File(SRC, i + ".xml");
			csv = new File(SRC, i + ".csv");
		}

		return files;
	}

	@Test
	public void doTest() throws IOException, SAXException {
		String expected = TestFileUtils.getTestFile(FOLDER + expectedFile.getName());

		QueryOutputToCSV transformer = new QueryOutputToCSV();
		String output = transformer.parse(new FileMessage(xmlFile));

		Assert.assertEquals(expected, output);
	}
}
