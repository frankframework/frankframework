/*
   Copyright 2013 Nationale-Nederlanden, 2020-2022 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.functional.ThrowingRunnable;

/**
 * Functions to read and write from one stream to another.
 *
 * @author  Gerrit van Brakel
 */
//Be careful: UTIL classes should NOT depend on the Servlet-API
public class StreamUtil {

	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
	public static final String DEFAULT_INPUT_STREAM_ENCODING=DEFAULT_CHARSET.displayName();
	public static final String AUTO_DETECT_CHARSET = "auto";
	public static final int BUFFERSIZE=20000;

	// DEFAULT_CHARSET and DEFAULT_INPUT_STREAM_ENCODING must be defined before LogUtil.getLogger() is called, otherwise DEFAULT_CHARSET returns null.
	protected static Logger log = LogManager.getLogger(StreamUtil.class);

	@Deprecated
	public static OutputStream getOutputStream(Object target) throws IOException {
		if (target instanceof OutputStream) {
			return (OutputStream) target;
		}
		if (target instanceof String) {
			return getFileOutputStream((String)target);
		}
		if (target instanceof StringDataSource) {
			StringDataSource stringDataSource = (StringDataSource) target;
			if(stringDataSource.isStringNative()) {
				return getFileOutputStream(stringDataSource.asString());
			}
		}
		return null;
	}

	@Deprecated
	private static OutputStream getFileOutputStream(String filename) throws IOException {
		if (StringUtils.isEmpty(filename)) {
			throw new IOException("target string cannot be empty but must contain a filename");
		}
		try {
			return new FileOutputStream(filename);
		} catch (FileNotFoundException e) {
			FileNotFoundException fnfe = new FileNotFoundException("cannot create file ["+filename+"]");
			fnfe.initCause(e);
			throw fnfe;
		}
	}

	public static InputStream dontClose(InputStream stream) {
		class NonClosingInputStreamFilter extends FilterInputStream {
			public NonClosingInputStreamFilter(InputStream in) {
				super(in);
			}
			@Override
			public void close() throws IOException {
				// do not close
			}
		}

		return new NonClosingInputStreamFilter(stream);
	}

	public static Reader dontClose(Reader reader) {
		class NonClosingReaderFilter extends FilterReader {
			public NonClosingReaderFilter(Reader in) {
				super(in);
			}
			@Override
			public void close() throws IOException {
				// do not close
			}
		}

		return new NonClosingReaderFilter(reader);
	}

	public static OutputStream dontClose(OutputStream stream) {
		class NonClosingOutputStreamFilter extends FilterOutputStream {
			public NonClosingOutputStreamFilter(OutputStream out) {
				super(out);
			}
			@Override
			public void close() throws IOException {
				// do not close
			}
		}

		return new NonClosingOutputStreamFilter(stream);
	}

	public static String readerToString(Reader reader, String endOfLineString) throws IOException {
		try {
			StringBuilder sb = new StringBuilder();
			int curChar = -1;
			int prevChar = -1;
			while ((curChar = reader.read()) != -1 || prevChar == '\r') {
				if (prevChar == '\r' || curChar == '\n') {
					if (endOfLineString == null) {
						if (prevChar == '\r')
							sb.append((char) prevChar);
						if (curChar == '\n')
							sb.append((char) curChar);
					}
					else {
						sb.append(endOfLineString);
					}
				}
				if (curChar != '\r' && curChar != '\n' && curChar != -1) {
					String appendStr =""+(char) curChar;
					sb.append(appendStr);
				}
				prevChar = curChar;
			}
			return sb.toString();
		} finally {
			reader.close();
		}
	}

	public static String streamToString(InputStream stream, String endOfLineString, String streamEncoding) throws IOException {
		return readerToString(StreamUtil.getCharsetDetectingInputStreamReader(stream,streamEncoding), endOfLineString);
	}

	public static byte[] streamToByteArray(InputStream inputStream, boolean skipBOM) throws IOException {
		BOMInputStream bOMInputStream = new BOMInputStream(inputStream, !skipBOM, ByteOrderMark.UTF_8, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE);
		return streamToBytes(bOMInputStream);
	}

	/**
	 * Return a Reader that reads the InputStream in the character set specified by the BOM. If no BOM is found, the default character set UTF-8 is used.
	 */
	public static Reader getCharsetDetectingInputStreamReader(InputStream inputStream) throws IOException {
		return getCharsetDetectingInputStreamReader(inputStream, DEFAULT_INPUT_STREAM_ENCODING);
	}

	/**
	 * Return a Reader that reads the InputStream in the character set specified by the BOM. If no BOM is found, a default character set is used.
	 */
	public static Reader getCharsetDetectingInputStreamReader(InputStream inputStream, String defaultCharset) throws IOException {
		BOMInputStream bOMInputStream = new BOMInputStream(inputStream,ByteOrderMark.UTF_8, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE);
		ByteOrderMark bom = bOMInputStream.getBOM();
		String charsetName = bom == null ? defaultCharset : bom.getCharsetName();

		if(StringUtils.isEmpty(charsetName)) {
			charsetName = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
		}

		return new InputStreamReader(new BufferedInputStream(bOMInputStream), charsetName);
	}

	public static void copyStream(InputStream in, OutputStream out, int chunkSize) throws IOException {
		if (in!=null) {
			byte[] buffer=new byte[chunkSize];

			int bytesRead=1;
			while (bytesRead>0) {
				bytesRead=in.read(buffer,0,chunkSize);
				if (bytesRead>0) {
					out.write(buffer,0,bytesRead);
				}
			}
			in.close();
		}
	}

	public static void copyReaderToWriter(Reader reader, Writer writer, int chunkSize) throws IOException {
		if (reader!=null) {
			char[] buffer=new char[chunkSize];

			int charsRead=1;
			while (charsRead>0) {
				charsRead=reader.read(buffer,0,chunkSize);
				if (charsRead>0) {
					writer.write(buffer,0,charsRead);
				}
			}
			reader.close();
		}
	}

	public static InputStream onClose(InputStream stream, ThrowingRunnable<IOException> onClose) {
		return new FilterInputStream(stream) {
			@Override
			public void close() throws IOException {
				try {
					super.close();
				} finally {
					onClose.run();
				}
			}
		};
	}

	public static Reader onClose(Reader reader, ThrowingRunnable<IOException> onClose) {
		return new FilterReader(reader) {
			@Override
			public void close() throws IOException {
				try {
					super.close();
				} finally {
					onClose.run();
				}
			}
		};
	}

	/**
	 * Copies the content of the specified file to an output stream.
	 * <p>
	 *     Example:
	 *     <pre>
	 *         OutputStream os = new ByteArrayOutputStream
	 *         Misc.fileToStream(someFileName, os);
	 *         System.out.println(os.toString) // prints the content of the output stream
	 *         				   // that's copied from the file.
	 *     </pre>
	 * </p>
	 * @throws IOException exception to be thrown if an I/O exception occurs
	 */
	public static void fileToStream(String filename, OutputStream output) throws IOException {
		streamToStream(Files.newInputStream(Paths.get(filename)), output);
	}

	public static void streamToStream(InputStream input, OutputStream output) throws IOException {
		streamToStream(input, output, null);
	}

	/**
	 * Writes the content of an input stream to an output stream by copying the buffer of input stream to the buffer of the output stream.
	 * If eof is specified, appends the eof(could represent a new line) to the outputstream
	 * Closes the input stream if specified.
	 * <p>
	 *     Example:
	 *     <pre>
	 *         String test = "test";
	 *         ByteArrayInputStream bais = new ByteArrayInputStream(test.getBytes());
	 *         OutputStream baos = new ByteArrayOutputStream();
	 *         Misc.streamToStream(bais, baos);
	 *         System.out.println(baos.toString()); // prints "test"
	 *     </pre>
	 * </p>
	 * @throws IOException  exception to be thrown if an I/O exception occurs
	 */
	public static void streamToStream(InputStream input, OutputStream output, byte[] eof) throws IOException {
		if (input!=null) {
			try {
				byte[] buffer=new byte[BUFFERSIZE];
				int bytesRead;
				while ((bytesRead=input.read(buffer,0,BUFFERSIZE))>-1) {
					output.write(buffer,0,bytesRead);
				}
				if(eof != null) {
					output.write(eof);
				}
			} finally {
				input.close();
			}
		}
	}

	/**
	 * Writes the content of an input stream to a specified file.
	 * <p>
	 *     Example:
	 *     <pre>
	 *         String test = "test";
	 *         ByteArrayInputStream bais = new ByteArrayInputStream(test.getBytes());
	 *         Misc.streamToFile(bais, file); // "test" copied inside the file.
	 *     </pre>
	 * </p>
	 * @throws IOException exception to be thrown if an I/O exception occurs
	 */
	public static void streamToFile(InputStream inputStream, File file) throws IOException {
		try (OutputStream fileOut = Files.newOutputStream(file.toPath())) {
			streamToStream(inputStream, fileOut);
		}
	}

	/**
	 * Writes the content of an input stream to a byte array.
	 * <p>
	 *     Example:
	 *     <pre>
	 *         String test = "test";
	 *         ByteArrayInputStream bais = new ByteArrayInputStream(test.getBytes());
	 *         byte[] arr = Misc.streamToBytes(bais);
	 *         System.out.println(new String(arr, StandardCharsets.UTF_8)); // prints "test"
	 *     </pre>
	 * </p>
	 */
	public static byte[] streamToBytes(InputStream inputStream) throws IOException {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			while (true) {
				int r = inputStream.read(buffer);
				if (r == -1) {
					break;
				}
				out.write(buffer, 0, r);
			}

			return out.toByteArray();
		} finally {
			inputStream.close();
		}
	}

	/**
	 * Copies the content of a reader to the buffer of a writer.
	 * <p>
	 *     Example:
	 *     <pre>
	 *         Reader reader = new StringReader("test");
	 *         Writer writer = new StringWriter();
	 *         Misc.readerToWriter(reader, writer, true);
	 *         System.out.println(writer.toString)); // prints "test"
	 *     </pre>
	 * </p>
	 */
	public static void readerToWriter(Reader reader, Writer writer) throws IOException {
		if (reader!=null) {
			try {
				char[] buffer=new char[BUFFERSIZE];
				int charsRead;
				while ((charsRead=reader.read(buffer,0,BUFFERSIZE))>-1) {
					writer.write(buffer,0,charsRead);
				}
			} finally {
				reader.close();
			}
		}
	}

	/**
	 * Please consider using resourceToString() instead of relying on files.
	 */
	@Deprecated
	static String fileToString(String fileName) throws IOException {
		return fileToString(fileName, null, false);
	}

	/**
	 * Please consider using resourceToString() instead of relying on files.
	 */
	@Deprecated
	public static String fileToString(String fileName, String endOfLineString) throws IOException {
		return fileToString(fileName, endOfLineString, false);
	}

	/**
	 * Please consider using resourceToString() instead of relying on files.
	 */
	@Deprecated
	public static String fileToString(String fileName, String endOfLineString, boolean xmlEncode) throws IOException {
		try (FileReader reader = new FileReader(fileName)) {
			return readerToString(reader, endOfLineString, xmlEncode);
		}
	}

	/**
	 * Copies the content of a reader into a string, adds specified string to the end of the line, if specified.
	 * <p>
	 *     Example:
	 *     <pre>
	 *         Reader r = new StringReader("<root> WeAreFrank'</root> \n";
	 *         String s = Misc.readerToString(r, "!!", true);
	 *         System.out.println(s);
	 *         // prints "&lt;root&gt; WeAreFrank!!&lt;/root&gt;"
	 *     </pre>
	 * </p>
	 * @param xmlEncode if set to true, applies XML encodings to the content of the reader
	 */
	public static String readerToString(Reader reader, String endOfLineString, boolean xmlEncode) throws IOException {
		StringBuilder sb = new StringBuilder();
		int curChar = -1;
		int prevChar = -1;
		try {
			while ((curChar = reader.read()) != -1 || prevChar == '\r') {
				if (prevChar == '\r' || curChar == '\n') {
					if (endOfLineString == null) {
						if (prevChar == '\r')
							sb.append((char) prevChar);
						if (curChar == '\n')
							sb.append((char) curChar);
					}
					else {
						sb.append(endOfLineString);
					}
				}
				if (curChar != '\r' && curChar != '\n' && curChar != -1) {
					String appendStr =""+(char) curChar;
					sb.append(xmlEncode ? XmlEncodingUtils.encodeChars(appendStr) : appendStr);
				}
				prevChar = curChar;
			}
			return sb.toString();
		} finally {
			reader.close();
		}
	}

	/**
	 * @see #streamToString(InputStream, String, boolean)
	 * @return String that's included in the stream
	 */
	public static String streamToString(InputStream stream) throws IOException {
		return streamToString(stream, null, false);
	}

	/**
	 * @see #streamToString(InputStream, String, String, boolean)
	 */
	public static String streamToString(InputStream stream, String streamEncoding) throws IOException {
		return streamToString(stream, null, streamEncoding, false);
	}

	/**
	 * @see #streamToString(InputStream, String, String, boolean)
	 */
	public static String streamToString(InputStream stream, String endOfLineString, boolean xmlEncode) throws IOException {
		return streamToString(stream,endOfLineString, DEFAULT_INPUT_STREAM_ENCODING, xmlEncode);
	}

	/**
	 * @see #readerToString(Reader, String, boolean)
	 */
	public static String streamToString(InputStream stream, String endOfLineString, String streamEncoding, boolean xmlEncode) throws IOException {
		return readerToString(getCharsetDetectingInputStreamReader(stream, streamEncoding), endOfLineString, xmlEncode);
	}

	/**
	 * @see StreamUtil#streamToString(InputStream, String, boolean)
	 */
	public static String resourceToString(URL resource, String endOfLineString, boolean xmlEncode) throws IOException {
		InputStream stream = resource.openStream();
		return streamToString(stream, endOfLineString, xmlEncode);
	}

	/**
	 * @see StreamUtil#streamToString(InputStream, String, boolean)
	 */
	public static String resourceToString(URL resource) throws IOException {
		return resourceToString(resource, null, false);
	}

	/**
	 * @see StreamUtil#streamToString(InputStream, String, boolean)
	 */
	public static String resourceToString(URL resource, String endOfLineString) throws IOException {
		return resourceToString(resource, endOfLineString, false);
	}

	/**
	 * Writes the string to a file.
	 */
	public static void stringToFile(String string, String fileName) throws IOException {
		try (FileWriter fw = new FileWriter(fileName)) {
			fw.write(string);
		}
	}
}
