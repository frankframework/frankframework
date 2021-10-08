package nl.nn.adapterframework.testutil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.StreamUtil;

public class TestFileUtils {
	
	public static Message getTestFileMessage(String file) throws IOException {
		return new Message(new StringReader(getTestFile(file)));
	}
	public static Message getTestFileMessage(String file, String charset) throws IOException {
		return new Message(new StringReader(getTestFile(file, charset)));
	}

	public static String getTestFile(String file) throws IOException {
		return getTestFile(file, "UTF-8");
	}

	public static String getTestFile(String file, String charset) throws IOException {
		URL url = getTestFileURL(file);
		if (url == null) {
			System.out.println("file [" + file + "] not found");
			return null;
		}
		return getTestFile(url, charset);
	}

	public static URL getTestFileURL(String file) throws IOException {
		return TestFileUtils.class.getResource(file);
	}

	public static Message getNonRepeatableTestFileMessage(String file) throws IOException {
		return new Message(getTestFileURL(file).openStream());
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

}
