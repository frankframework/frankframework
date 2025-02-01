/*
   Copyright 2023-2025 WeAreFrank!

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

import java.io.FilterOutputStream;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.function.IOConsumer;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.commons.io.input.TeeReader;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.commons.io.output.TeeWriter;
import org.apache.commons.io.output.ThresholdingOutputStream;

import lombok.extern.log4j.Log4j2;

import org.frankframework.functional.ThrowingSupplier;

@Log4j2
public class StreamCaptureUtils {
	public static final int DEFAULT_STREAM_CAPTURE_LIMIT = 10_000;

	/**
	 * Triggers the next byte after the threshold has been reached.
	 * If bytes are written in chunks it triggers after processing the entire chunk.
	 */
	public static OutputStream limitSize(OutputStream stream, int maxSize) {
		AtomicBoolean closed = new AtomicBoolean(false);
		return new ThresholdingOutputStream(maxSize, IOConsumer.noop(), tos -> {
			if (tos.isThresholdExceeded()) {
				if (!closed.getAndSet(true)) {
					stream.close();
				}

				return OutputStream.nullOutputStream();
			}
			return stream;
		});
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

	public static InputStream captureInputStream(InputStream in, OutputStream capture, int maxSize) {
		CountingInputStream counter = new CountingInputStream(in);
		MarkCompensatingOutputStream markCompensatingOutputStream = new MarkCompensatingOutputStream(limitSize(capture, maxSize));
		return new TeeInputStream(counter, markCompensatingOutputStream, true) {

			@Override
			public void close() throws IOException {
				try {
					if (counter.getByteCount()<maxSize && available()>0) {
						// Make the bytes available for debugger even when the stream was not used (might be because the
						// pipe or sender that normally consumes the stream is stubbed by the debugger)
						int len = read(new byte[maxSize]);
						log.trace("{} bytes available at close", len);
					}
				} catch (Exception e) {
					log.debug("Exception checking remaining bytes at close, ignoring: {}", e::getMessage);
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

	public static OutputStream captureOutputStream(OutputStream stream, OutputStream capture, int maxSize) {
		return new TeeOutputStream(stream, limitSize(capture,maxSize));
	}

	public static Reader captureReader(Reader in, Writer capture, int maxSize) {
		MarkCompensatingWriter markCompensatingWriter =  new MarkCompensatingWriter(limitSize(capture, maxSize));

		return new TeeReader(in, markCompensatingWriter, true) {

			private int charsRead;

			private int readCounted(ThrowingSupplier<Integer, IOException> reader) throws IOException {
				int len = reader.get();
				if (len>0) {
					charsRead+=len;
				}
				return len;
			}

			@Override
			public int read() throws IOException {
				char[] cb = new char[1];
				if (read(cb, 0, 1) == -1)
					return -1;
				else
					return cb[0];
			}

			@Override
			public int read(char[] chr) throws IOException {
				return read(chr, 0, chr.length);
			}

			@Override
			public int read(char[] chr, int st, int end) throws IOException {
				return readCounted(() -> super.read(chr, st, end));
			}

			@Override
			public void close() throws IOException {
				try {
					if (charsRead < maxSize && ready()) {
						// Make the bytes available for debugger even when the stream was not used (might be because the
						// pipe or sender that normally consumes the stream is stubbed by the debugger)
						int len = read(new char[maxSize]);
						log.trace("{} characters available at close", len);
					}
				} catch (Exception e) {
					log.debug("Exception checking remaining characters at close, ignoring: {}", e::getMessage);
				} finally {
					super.close();
				}
			}

			@Override
			public synchronized void mark(int readLimit) throws IOException {
				markCompensatingWriter.mark(readLimit);
				super.mark(readLimit);
			}

			@Override
			public synchronized void reset() throws IOException {
				markCompensatingWriter.reset();
				super.reset();
			}
		};
	}

	public static Writer captureWriter(Writer writer, Writer capture, int maxSize) {
		return new TeeWriter(writer, limitSize(capture, maxSize));
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

	static class MarkCompensatingWriter extends FilterWriter {
		private int charsToSkip = 0;

		public MarkCompensatingWriter(Writer out) {
			super(out);
		}

		@Override
		public synchronized void write(int b) throws IOException {
			if (charsToSkip > 0) {
				--charsToSkip;
				return;
			}

			out.write(b);
		}

		@Override
		public synchronized void write(char[] c, int off, int len) throws IOException {
			if (charsToSkip == 0) {
				out.write(c, off, len);
				return;
			}

			int sizeToRead = Math.abs(off - len);
			if (charsToSkip < sizeToRead) {
				out.write(c, off + charsToSkip, len - charsToSkip);
				reset();
			} else {
				charsToSkip -= sizeToRead;
			}
		}

		public synchronized void mark(int charsToSkip) {
			this.charsToSkip = charsToSkip;
		}

		public void reset() {
			mark(0);
		}
	}
}
