package org.frankframework.jdbc.transformer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

import org.frankframework.stream.FileMessage;
import org.frankframework.testutil.TestFileUtils;

@Log4j2
class ToListOfMapsTest {
	private static final String FOLDER = "/Jdbc.transformer/";
	private static final String SRC = "src/test/resources" + FOLDER;

	private static Stream<Arguments> data() {
		int i = 0;
		File xml = new File(SRC, i + ".xml");
		File json = new File(SRC, i + ".map");
		Stream.Builder<Arguments> files = Stream.builder();
		while (xml.exists() && json.exists()) {
			log.debug("Added [{}] and [{}]", xml.getName(), json.getName());
			files.add(Arguments.of(xml, json));
			i++;
			xml = new File(SRC, i + ".xml");
			json = new File(SRC, i + ".map");
		}
		return files.build();
	}

	@MethodSource("data")
	@ParameterizedTest
	void doTest(File xmlFile, File expectedFile) throws IOException, SAXException {
		String expected = TestFileUtils.getTestFile(FOLDER + expectedFile.getName());
		ObjectMapper mapper = new ObjectMapper();

		QueryOutputToListOfMaps transformer = new QueryOutputToListOfMaps();
		List<Map<String, String>> output = transformer.parseMessage(new FileMessage(xmlFile));
		String out = mapper.writeValueAsString(output);

		Assertions.assertEquals(expected, out);
	}
}
