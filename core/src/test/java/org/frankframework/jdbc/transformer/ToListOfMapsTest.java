package org.frankframework.jdbc.transformer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.frankframework.stream.FileMessage;
import org.frankframework.testutil.TestFileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(Parameterized.class)
public class ToListOfMapsTest {
	private static final String FOLDER = "/Jdbc.transformer/";
	private static final String SRC = "src/test/resources" + FOLDER;
	private final File xmlFile;
	private final File expectedFile;

	public ToListOfMapsTest(File xmlFile, File expectedFile) {
		this.xmlFile = xmlFile;
		this.expectedFile = expectedFile;
	}

	@Parameterized.Parameters
	public static Collection<Object[]> data() {
		List<Object[]> files = new ArrayList<>();
		int i = 0;
		File xml = new File(SRC, i + ".xml");
		File json = new File(SRC, i + ".map");
		System.out.println(xml.getAbsolutePath());
		while (xml.exists() && json.exists()) {
			System.out.println(String.format("Added [%s] and [%s]", xml.getName(), json.getName()));
			files.add(new File[] {xml, json});
			i++;
			xml = new File(SRC, i + ".xml");
			json = new File(SRC, i + ".map");
		}

		return files;
	}

	@Test
	public void doTest() throws IOException, SAXException {
		String expected = TestFileUtils.getTestFile(FOLDER + expectedFile.getName());
		ObjectMapper mapper = new ObjectMapper();

		QueryOutputToListOfMaps transformer = new QueryOutputToListOfMaps();
		List<Map<String, String>> output = transformer.parseMessage(new FileMessage(xmlFile));
		String out = mapper.writeValueAsString(output);

		Assert.assertEquals(expected, out);
	}
}
