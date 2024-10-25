package org.frankframework.testutil;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;

import lombok.SneakyThrows;

import org.frankframework.util.LogUtil;
import org.frankframework.util.StreamUtil;

public class TestFileUtils {
	private static final Logger LOG = LogUtil.getLogger(TestFileUtils.class);

	public static String getTestFile(String file) throws IOException {
		return getTestFile(file, "UTF-8");
	}

	public static String getTestFile(String file, String charset) throws IOException {
		URL url = getTestFileURL(file);
		if (url == null) {
			LOG.error("file [" + file + "] not found");
			return null;
		}
		return getTestFile(url, charset);
	}

	public static URL getTestFileURL(String file) {
		String normalizedFilename = FilenameUtils.normalize(file, true);
		URL url = TestFileUtils.class.getResource(normalizedFilename);
		if(url == null) LOG.warn("unable to find testfile [{}]", normalizedFilename);
		return url;
	}

	public static String getTestFile(URL url, String charset) throws IOException {
		if (url == null) {
			return null;
		}
		return readLines(StreamUtil.getCharsetDetectingInputStreamReader(url.openStream(), charset));
	}

	public static String readLines(Reader reader) throws IOException {
		BufferedReader buf = new BufferedReader(reader);
		StringBuilder string = new StringBuilder();
		String line = buf.readLine();
		while (line != null) {
			string.append(line);
			line = buf.readLine();
			if (line != null) {
				string.append("\n");
			}
		}
		return string.toString();
	}

	@SneakyThrows(URISyntaxException.class)
	public static String getTestFilePath(String string) {
		URL url = getTestFileURL(string);
		assertNotNull(url);
		return Paths.get(url.toURI()).toString();
	}
}
