package org.frankframework.jdbc.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xml.sax.SAXException;

import lombok.extern.log4j.Log4j2;

import org.frankframework.stream.FileMessage;
import org.frankframework.testutil.TestFileUtils;

@Log4j2
class ToCsvTest {
	private static final String FOLDER = "/Jdbc.transformer/";
	private static final String SRC = "src/test/resources" + FOLDER;

	private static Stream<Arguments> data() {
		int i = 0;
		File xml = new File(SRC, i + ".xml");
		File csv = new File(SRC, i + ".csv");
		Stream.Builder<Arguments> files = Stream.builder();
		while (xml.exists() && csv.exists()) {
			log.debug("Added [{}] and [{}]", xml.getName(), csv.getName());
			files.add(Arguments.of(xml, csv));
			i++;
			xml = new File(SRC, i + ".xml");
			csv = new File(SRC, i + ".csv");
		}
		return files.build();
	}

	@MethodSource("data")
	@ParameterizedTest
	void doTest(File xmlFile, File expectedFile) throws IOException, SAXException {
		String expected = TestFileUtils.getTestFile(FOLDER + expectedFile.getName());

		QueryOutputToCSV transformer = new QueryOutputToCSV();
		String output = transformer.parse(new FileMessage(xmlFile));

		assertEquals(expected, output);
	}
}
