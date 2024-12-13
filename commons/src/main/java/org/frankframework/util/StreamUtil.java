/*
   Copyright 2013 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
package org.frankframework.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Functions to read and write from one stream to another.
 * Be careful: Util classes should NOT depend on the Servlet-API
 *
 * @author Gerrit van Brakel
 */
public class StreamUtil {

	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
	public static final String DEFAULT_INPUT_STREAM_ENCODING = DEFAULT_CHARSET.displayName();
	public static final String AUTO_DETECT_CHARSET = "auto";
	public static final int BUFFER_SIZE = 64 * 1024;

	// DEFAULT_CHARSET and DEFAULT_INPUT_STREAM_ENCODING must be defined before LogUtil.getLogger() is called, otherwise DEFAULT_CHARSET returns null.
	protected static Logger log = LogManager.getLogger(StreamUtil.class);

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

	public static InputStream urlToStream(URL url, int timeoutMs) throws IOException {
		URLConnection conn = url.openConnection();
		if (timeoutMs == 0) {
			timeoutMs = 10000;
		}
		if (timeoutMs > 0) {
			conn.setConnectTimeout(timeoutMs);
			conn.setReadTimeout(timeoutMs);
		}
		return conn.getInputStream(); //SCRV_269S#072 //SCRV_286S#077
	}

	public static String readerToString(Reader reader, String endOfLineString) throws IOException {
		return readerToString(reader, endOfLineString, false);
	}

	public static String streamToString(InputStream stream, String endOfLineString, String streamEncoding) throws IOException {
		return readerToString(StreamUtil.getCharsetDetectingInputStreamReader(stream, streamEncoding), endOfLineString);
	}

	public static byte[] streamToByteArray(InputStream inputStream, boolean skipBOM) throws IOException {
		return skipBOM ? streamToBytes(new BOMInputStream(inputStream, !skipBOM, ByteOrderMark.UTF_8, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE)) : streamToBytes(inputStream);
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
		BOMInputStream bOMInputStream = new BOMInputStream(inputStream, ByteOrderMark.UTF_8, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE);
		ByteOrderMark bom = bOMInputStream.getBOM();
		String charsetName = bom == null ? defaultCharset : bom.getCharsetName();

		if (StringUtils.isEmpty(charsetName)) {
			charsetName = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
		}

		return new InputStreamReader(new BufferedInputStream(bOMInputStream), charsetName);
	}

	/**
	 * Copy an {@link InputStream} to the {@link OutputStream}. After copying, the {@link InputStream} is closed but
	 * the {@link OutputStream} is not.
	 *
	 * @param in        The {@link InputStream} from which to read bytes to copy.
	 * @param out       The {@link OutputStream} target to which to write the byte from {@code in}.
	 * @param chunkSize The size of the buffer used for copying.
	 * @return The number of bytes copied.
	 * @throws IOException Thrown if any exception occurs while reading or writing from either stream.
	 */
	public static long copyStream(InputStream in, OutputStream out, int chunkSize) throws IOException {
		if (in == null) {
			return 0L;
		}
		byte[] buffer = new byte[chunkSize];
		long totalBytesCopied = 0L;
		int bytesRead=1;
		try (InputStream is = in){
			if (is instanceof ByteArrayInputStream bis) {
				// Optimise the from-memory reading case
				return bis.transferTo(out);
			}
			// Could also use `is.transferTo(Out);` here but that uses small default buffer size
			while (bytesRead>0) {
				bytesRead=is.read(buffer,0,chunkSize);
				if (bytesRead>0) {
					out.write(buffer,0,bytesRead);
					totalBytesCopied += bytesRead;
				}
			}
		}
		return totalBytesCopied;
	}

	/**
	 * Copy a maximum of {@code maxBytesToCopy} of an {@link InputStream} to the {@link OutputStream}. If a negative number is passed, then the
	 * full stream is copied.
	 * The {@link InputStream} is not closed.
	 *
	 * @param in             The {@link InputStream} from which to read bytes to copy.
	 * @param out            The {@link OutputStream} target to which to write the byte from {@code in}.
	 * @param maxBytesToCopy The maximum nr of bytes to copy. If a negative number, then the entire InputStream will be copied.
	 * @param chunkSize      The size of the buffer used for copying.
	 * @throws IOException Thrown if any exception occurs while reading or writing from either stream.
	 */
	public static void copyPartialStream(InputStream in, OutputStream out, long maxBytesToCopy, int chunkSize) throws IOException {
		if (in == null || maxBytesToCopy == 0L) {
			return;
		}

		if (maxBytesToCopy < 0L) {
			in.transferTo(out);
			return;
		}

		byte[] buffer = new byte[chunkSize];
		long bytesLeft = maxBytesToCopy;
		int bytesRead;
		while (bytesLeft != 0L) {
			int toRead = (int) Math.min(chunkSize, bytesLeft);
			bytesRead = in.read(buffer, 0, toRead);
			if (bytesRead <= 0) {
				break;
			}
			out.write(buffer, 0, bytesRead);
			bytesLeft -= bytesRead;
		}
	}

	public static void copyReaderToWriter(Reader reader, Writer writer, int chunkSize) throws IOException {
		if (reader == null) {
			return;
		}

		int charsRead;
		try (Reader r = reader){
			if (reader instanceof StringReader sr) {
				// Optimise the from-memory reading case
				sr.transferTo(writer);
				return;
			}
			// Could also use `is.transferTo(Out);` here but that uses small default buffer size
			char[] buffer = new char[chunkSize];
			while (true) {
				charsRead = r.read(buffer, 0, chunkSize);
				if (charsRead <= 0) {
					break;
				}
				writer.write(buffer, 0, charsRead);
			}
		}
	}

	/**
	 * Copies the content of the specified file to an output stream.
	 * <p>
	 * Example:
	 * <pre>
	 *         OutputStream os = new ByteArrayOutputStream
	 *         Misc.fileToStream(someFileName, os);
	 *         System.out.println(os.toString) // prints the content of the output stream
	 *         				   // that's copied from the file.
	 *     </pre>
	 * </p>
	 *
	 * @throws IOException exception to be thrown if an I/O exception occurs
	 */
	public static void fileToStream(String filename, OutputStream output) throws IOException {
		streamToStream(Files.newInputStream(Paths.get(filename)), output);
	}

	public static void streamToStream(@Nullable InputStream input, @Nonnull OutputStream output) throws IOException {
		streamToStream(input, output, null);
	}

	/**
	 * Writes the content of an input stream to an output stream by copying the buffer of input stream to the buffer of the output stream.
	 * If eof is specified, appends the eof(could represent a new line) to the outputstream
	 * Closes the input stream if specified.
	 * <p>
	 * Example:
	 * <pre>
	 *         String test = "test";
	 *         ByteArrayInputStream bais = new ByteArrayInputStream(test.getBytes());
	 *         OutputStream baos = new ByteArrayOutputStream();
	 *         Misc.streamToStream(bais, baos);
	 *         System.out.println(baos.toString()); // prints "test"
	 *     </pre>
	 * </p>
	 *
	 * @throws IOException exception to be thrown if an I/O exception occurs
	 */
	public static void streamToStream(InputStream input, OutputStream output, byte[] eof) throws IOException {
		if (input == null) {
			return;
		}
		try (InputStream is = input) {
			is.transferTo(output);
			if(eof != null) {
				output.write(eof);
			}
		}
	}

	/**
	 * Writes the content of an input stream to a specified file.
	 * <p>
	 * Example:
	 * <pre>
	 *         String test = "test";
	 *         ByteArrayInputStream bais = new ByteArrayInputStream(test.getBytes());
	 *         Misc.streamToFile(bais, file); // "test" copied inside the file.
	 *     </pre>
	 * </p>
	 *
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
	 * Example:
	 * <pre>
	 *         String test = "test";
	 *         ByteArrayInputStream bais = new ByteArrayInputStream(test.getBytes());
	 *         byte[] arr = Misc.streamToBytes(bais);
	 *         System.out.println(new String(arr, StandardCharsets.UTF_8)); // prints "test"
	 *     </pre>
	 * </p>
	 */
	public static byte[] streamToBytes(InputStream inputStream) throws IOException {
		try (InputStream is = inputStream) {
			return is.readAllBytes();
		}
	}

	/**
	 * Copies the content of a reader to the buffer of a writer.
	 * <p>
	 * Example:
	 * <pre>
	 *         Reader reader = new StringReader("test");
	 *         Writer writer = new StringWriter();
	 *         Misc.readerToWriter(reader, writer, true);
	 *         System.out.println(writer.toString)); // prints "test"
	 *     </pre>
	 * </p>
	 */
	public static void readerToWriter(Reader reader, Writer writer) throws IOException {
		copyReaderToWriter(reader, writer, BUFFER_SIZE);
	}

	/**
	 * Copies the content of a reader into a string, adds specified string to the end of the line, if specified.
	 * <p>
	 * Example:
	 * <pre>
	 *         Reader r = new StringReader("<root> WeAreFrank'</root> \n";
	 *         String s = Misc.readerToString(r, "!!", true);
	 *         System.out.println(s);
	 *         // prints "&lt;root&gt; WeAreFrank!!&lt;/root&gt;"
	 *     </pre>
	 * </p>
	 *
	 * @param xmlEncode if set to true, applies XML encodings to the content of the reader
	 */
	public static String readerToString(Reader reader, String endOfLineString, boolean xmlEncode) throws IOException {
		return readerToString(reader, endOfLineString, xmlEncode, 0);
	}

	public static String readerToString(Reader reader, String endOfLineString, boolean xmlEncode, int initialCapacity) throws IOException {
		StringBuilder sb = new StringBuilder(initialCapacity > 0 ? initialCapacity + 32 : 8192);
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
					} else {
						sb.append(endOfLineString);
					}
				}
				if (curChar != '\r' && curChar != '\n' && curChar != -1) {
					if (xmlEncode) {
						sb.append(org.frankframework.util.XmlEncodingUtils.encodeChars(String.valueOf((char) curChar)));
					} else {
						sb.append((char) curChar);
					}
				}
				prevChar = curChar;
			}
			return sb.toString();
		} finally {
			reader.close();
		}
	}

	/**
	 * @return String that's included in the stream
	 * @see #streamToString(InputStream, String, boolean)
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
		return streamToString(stream, endOfLineString, DEFAULT_INPUT_STREAM_ENCODING, xmlEncode);
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
		try (Writer fw = Files.newBufferedWriter(Paths.get(fileName), Charset.defaultCharset())) {
			fw.write(string);
		}
	}
}
