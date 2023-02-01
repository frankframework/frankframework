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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.FilterReader;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.commons.io.input.TeeReader;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.commons.io.output.TeeWriter;
import org.apache.commons.io.output.ThresholdingOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import lombok.SneakyThrows;
import nl.nn.adapterframework.functional.ThrowingRunnable;
import nl.nn.adapterframework.stream.Message;

/**
 * Functions to read and write from one stream to another.
 *
 * @author  Gerrit van Brakel
 */
//Be careful: UTIL classes should NOT depend on the Servlet-API
public class StreamUtil {

	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
	public static final String DEFAULT_INPUT_STREAM_ENCODING=DEFAULT_CHARSET.displayName();
	public static final int DEFAULT_STREAM_CAPTURE_LIMIT=10000;
	public static final String AUTO_DETECT_CHARSET = "auto";

	// DEFAULT_CHARSET and DEFAULT_INPUT_STREAM_ENCODING must be defined before LogUtil.getLogger() is called, otherwise DEFAULT_CHARSET returns null.
	protected static Logger log = LogUtil.getLogger(StreamUtil.class);

	@Deprecated
	public static OutputStream getOutputStream(Object target) throws IOException {
		if (target instanceof OutputStream) {
			return (OutputStream) target;
		}
		if (target instanceof String) {
			return getFileOutputStream((String)target);
		}
		if (target instanceof Message) {
			if(((Message) target).asObject() instanceof String) {
				return getFileOutputStream(((Message)target).asString());
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
			StringBuffer sb = new StringBuffer();
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
		return Misc.streamToBytes(bOMInputStream);
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

	public static boolean hasDataAvailable(PushbackInputStream in) throws IOException {
		if (in == null) {
			return false;
		}
		int v = in.read();
		in.unread(v);
		return (v >= 0);
	}

	public static boolean hasDataAvailable(PushbackReader in) throws IOException {
		if (in == null) {
			return false;
		}
		int v = in.read();
		in.unread(v);
		return (v >= 0);
	}

	public static void copyStream(InputStream in, OutputStream out, int chunkSize) throws IOException {
		if (in!=null) {
			byte[] buffer=new byte[chunkSize];

			int bytesRead=1;
			while (bytesRead>0) {
				bytesRead=in.read(buffer,0,chunkSize);
				if (bytesRead>0) {
					out.write(buffer,0,bytesRead);
				} else {
					in.close();
				}
			}
		}
	}

	public static void copyReaderToWriter(Reader reader, Writer writer, int chunkSize, boolean resolve, boolean xmlEncode) throws IOException {
		if (reader!=null) {
			char[] buffer=new char[chunkSize];

			int charsRead=1;
			while (charsRead>0) {
				charsRead=reader.read(buffer,0,chunkSize);
				if (charsRead>0) {
					if (resolve) {
						String resolved = StringResolver.substVars(new String (buffer,0,charsRead),null);
						if (xmlEncode) {
							writer.write(XmlUtils.encodeChars(resolved));
						} else {
							writer.write(resolved);
						}
					} else {
						if (xmlEncode) {
							writer.write(XmlUtils.encodeChars(buffer,0,charsRead));
						} else {
							writer.write(buffer,0,charsRead);
						}
					}
				} else {
					reader.close();
				}
			}
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

	public static OutputStream onClose(OutputStream stream, ThrowingRunnable<IOException> onClose) {
		return new FilterOutputStream(stream) {
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

	public static Writer onClose(Writer writer, ThrowingRunnable<IOException> onClose) {
		return new FilterWriter(writer) {
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

	@SneakyThrows // throw the IOException thrown by resource.close(), without declaring it as a checked Exception (that would be incompatible with the use in lambda's below)
	private static void closeResource(AutoCloseable resource) {
		resource.close();
	}

	public static InputStream closeOnClose(InputStream stream, AutoCloseable resource) {
		return onClose(stream, () -> closeResource(resource));
	}

	public static OutputStream closeOnClose(OutputStream stream, AutoCloseable resource) {
		return onClose(stream, () -> closeResource(resource));
	}

	public static Reader closeOnClose(Reader reader, AutoCloseable resource) {
		return onClose(reader, () -> closeResource(resource));
	}

	public static Writer closeOnClose(Writer writer, AutoCloseable resource) {
		return onClose(writer, () -> closeResource(resource));
	}

	public static InputStream watch(InputStream stream, Runnable onClose, Runnable onException) {
		return watch(stream, onClose, (e) -> { if (onException!=null) onException.run(); return e; });
	}

	public static InputStream watch(InputStream stream, Runnable onClose, Function<IOException,IOException> onException) {
		class WatchedInputStream extends FilterInputStream {
			public WatchedInputStream(InputStream in) {
				super(in);
			}

			private IOException handleException(IOException e) {
				if (onException!=null) {
					IOException r = onException.apply(e);
					if (r!=null) {
						return r;
					}
				}
				return e;
			}

			@Override
			public void close() throws IOException {
				try {
					super.close();
				} catch (IOException e) {
					throw handleException(e);
				}
				if (onClose!=null) {
					onClose.run();
				}
			}

			@Override
			public int read() throws IOException {
				try {
					return super.read();
				} catch (IOException e) {
					throw handleException(e);
				}
			}

			@Override
			public int read(byte[] b) throws IOException {
				try {
					return super.read(b);
				} catch (IOException e) {
					throw handleException(e);
				}
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				try {
					return super.read(b, off, len);
				} catch (IOException e) {
					throw handleException(e);
				}
			}

			@Override
			public long skip(long n) throws IOException {
				try {
					return super.skip(n);
				} catch (IOException e) {
					throw handleException(e);
				}
			}

			@Override
			public int available() throws IOException {
				try {
					return super.available();
				} catch (IOException e) {
					throw handleException(e);
				}
			}

			@Override
			public synchronized void reset() throws IOException {
				try {
					super.reset();
				} catch (IOException e) {
					throw handleException(e);
				}
			}

		}

		return new WatchedInputStream(stream);
	}

	public static MarkCompensatingOutputStream markCompensatingOutputStream(OutputStream stream) {
		return new MarkCompensatingOutputStream(stream);
	}

	static class MarkCompensatingOutputStream extends FilterOutputStream {
		private int bytesToSkip = 0;

		public MarkCompensatingOutputStream(OutputStream out) {
			super(out);
		}

		@Override
		public synchronized void write(int b) throws IOException {
			if(bytesToSkip > 0) {
				--bytesToSkip;
				return;
			}

			out.write(b);
		}

		@Override
		public synchronized void write(byte[] b, int off, int len) throws IOException {
			if(bytesToSkip == 0) {
				out.write(b, off, len);
				return;
			}

			int sizeToRead = Math.abs(off - len);
			if(bytesToSkip < sizeToRead) {
				out.write(b, off + bytesToSkip, len - bytesToSkip);
				reset();
			} else {
				bytesToSkip -= sizeToRead;
			}
		}

		public synchronized void mark(int bytesToSkip) {
			this.bytesToSkip = bytesToSkip;
		}
		public void reset() {
			mark(0);
		}
	}

	/**
	 * Triggers the next byte after the threshold has been reached.
	 * If bytes are written in chunks it triggers after processing the entire chunk.
	 */
	public static OutputStream limitSize(OutputStream stream, int maxSize) {
		return new ThresholdingOutputStream(maxSize) {

			@Override
			protected void thresholdReached() throws IOException {
				stream.close();
			}

			@Override
			protected OutputStream getStream() throws IOException {
				if (isThresholdExceeded()) {
					return NullOutputStream.NULL_OUTPUT_STREAM;
				}
				return stream;
			}

		};
	}

	public static Writer limitSize(Writer writer, int maxSize) {
		return new Writer() {

			private long written;

			@Override
			public void write(char[] buffer, int offset, int length) throws IOException {
				if (written<maxSize) {
					writer.write(buffer, offset, length);
					if ((written+=length)>=maxSize) {
						writer.close();
					}
				}
			}

			@Override
			public void flush() throws IOException {
				writer.flush();
			}

			@Override
			public void close() throws IOException {
				if (written<maxSize) {
					writer.close();
				}
			}
		};
	}


	public static InputStream captureInputStream(InputStream in, OutputStream capture) {
		return captureInputStream(in, capture, 10000, true);
	}
	public static InputStream captureInputStream(InputStream in, OutputStream capture, int maxSize, boolean captureRemainingOnClose) {

		CountingInputStream counter = new CountingInputStream(in);
		MarkCompensatingOutputStream markCompensatingOutputStream = markCompensatingOutputStream(limitSize(capture, maxSize));
		return new TeeInputStream(counter, markCompensatingOutputStream, true) {

			@Override
			public void close() throws IOException {
				try {
					if (counter.getByteCount()<maxSize && available()>0) {
						// Make the bytes available for debugger even when the stream was not used (might be because the
						// pipe or sender that normally consumes the stream is stubbed by the debugger)
						int len = read(new byte[maxSize]);
						if (log.isTraceEnabled()) log.trace(len+" bytes available at close");
					}
				} finally {
					super.close();
				}
			}

			@Override
			public synchronized void mark(int readlimit) {
				markCompensatingOutputStream.mark(readlimit);
				super.mark(readlimit);
			}

			@Override
			public synchronized void reset() throws IOException {
				markCompensatingOutputStream.reset();
				super.reset();
			}
		};

	}

	public static OutputStream captureOutputStream(OutputStream stream, OutputStream capture) {
		return captureOutputStream(stream, capture, DEFAULT_STREAM_CAPTURE_LIMIT);
	}
	public static OutputStream captureOutputStream(OutputStream stream, OutputStream capture, int maxSize) {
		return new TeeOutputStream(stream, limitSize(capture,maxSize));
	}

	private static interface ReadMethod {
		int read() throws IOException;
	}

	public static Reader captureReader(Reader in, Writer capture) {
		return captureReader(in, capture, 10000, true);
	}
	public static Reader captureReader(Reader in, Writer capture, int maxSize, boolean captureRemainingOnClose) {
		return new TeeReader(in, limitSize(capture, maxSize), true) {

			private int charsRead;

			private int readCounted(ReadMethod reader) throws IOException {
				int len = reader.read();
				if (len>0) {
					charsRead+=len;
				}
				return len;
			}

			@Override
			public int read() throws IOException {
				return readCounted(() -> super.read());
			}

			@Override
			public int read(char[] chr) throws IOException {
				return readCounted(() -> super.read(chr));
			}

			@Override
			public int read(char[] chr, int st, int end) throws IOException {
				return readCounted(() -> super.read(chr, st, end));
			}

			@Override
			public int read(CharBuffer target) throws IOException {
				return readCounted(() -> super.read(target));
			}

			@Override
			public void close() throws IOException {
				try {
					if (charsRead<maxSize && ready()) {
						// Make the bytes available for debugger even when the stream was not used (might be because the
						// pipe or sender that normally consumes the stream is stubbed by the debugger)
						int len = read(new char[maxSize]);
						if (log.isTraceEnabled()) log.trace(len+" chararacters available at close");
					}
				} finally {
					super.close();
				}
			}

		};
	}

	public static Writer captureWriter(Writer writer, Writer capture) {
		return captureWriter(writer, capture, DEFAULT_STREAM_CAPTURE_LIMIT);
	}
	public static Writer captureWriter(Writer writer, Writer capture, int maxSize) {
		return new TeeWriter(writer, limitSize(capture,maxSize));
	}


}
