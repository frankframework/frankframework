/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.stream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.io.input.ReaderInputStream;

import org.frankframework.util.AppConstants;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.TemporaryDirectoryUtils;

/**
 * Wrap a {@link Reader} to provide repeatable access to its contents.
 */
public class RepeatableReaderWrapper implements RequestBuffer, AutoCloseable {
	private final long maxInMemorySize = AppConstants.getInstance().getLong(Message.MESSAGE_MAX_IN_MEMORY_PROPERTY, Message.MESSAGE_MAX_IN_MEMORY_DEFAULT);

	private final Reader source;
	private boolean isEof = false; // True when EOF has been reached on the source Reader
	private boolean closed = false;
	private final List<CharBufferBlock> buffers; // temporary buffer, once full, write to disk
	private CharBufferBlock currentBuffer;

	private Writer writer;
	private Path tempFileLocation;
	private boolean tempFileOwner = true;

	private long charsReadTotal = 0L;

	public RepeatableReaderWrapper(@Nonnull Reader source) {
		this.source = source;
		this.buffers = new ArrayList<>();
		this.currentBuffer = new CharBufferBlock();
		this.buffers.add(currentBuffer);
	}

	private boolean isBufferedOnDisk() {
		return tempFileLocation != null;
	}

	private synchronized boolean bufferDataFromSource(int size) throws IOException {
		if (size <= 0 || isEof || closed) {
			return false;
		}

		// Already more bytes read than should be buffered in memory
		if (isBufferedOnDisk()) {
			char[] data = new char[size];
			int charsRead = source.read(data, 0, size);
			if (charsRead == -1) {
				isEof = true;
				source.close();
				writer.close();
				writer = null;
				return false;
			}
			charsReadTotal += charsRead;
			writer.write(data, 0, charsRead);
			writer.flush(); // Flush, b/c we will instantly read from the file we write to
			return true;
		}

		// Buffer to memory
		int toRead = size;
		while (toRead > 0) {
			if (currentBuffer.isFull()) {
				currentBuffer = new CharBufferBlock();
				buffers.add(currentBuffer);
			}
			CharBufferBlock buffer = currentBuffer;
			int charsRead = buffer.addFromReader(source, toRead);
			if (charsRead == -1) {
				isEof = true;
				source.close();
				break;
			}
			charsReadTotal += charsRead;
			toRead -= charsRead;
		}
		if (charsReadTotal > maxInMemorySize) {
			tempFileLocation = allocateTemporaryFile();
			writer = transferBuffersToFile(tempFileLocation, buffers);
			buffers.clear();
			currentBuffer = null;
		}
		return true;
	}

	private @Nullable Writer transferBuffersToFile(@Nonnull Path path, @Nonnull List<CharBufferBlock> buffers) throws IOException {
		Writer out = Files.newBufferedWriter(path, StreamUtil.DEFAULT_CHARSET);
		try {
			for (CharBufferBlock buffer: buffers) {
				buffer.transferToWriter(out);
			}
			out.flush();
			if (isEof) {
				out.close();
				return null;
			}
		} catch (IOException | RuntimeException e) {
			out.close();
			throw e;
		}

		return out;
	}

	private @Nonnull Path allocateTemporaryFile() throws IOException {
		Path tempDirectory = TemporaryDirectoryUtils.getTempDirectory(SerializableFileReference.TEMP_MESSAGE_DIRECTORY);
		return Files.createTempFile(tempDirectory, "msg", "dat");
	}

	@Override
	public void close() throws IOException {
		closed = true;
		CloseUtils.closeSilently(source, writer);
		buffers.clear();
		currentBuffer = null;
		if (isBufferedOnDisk() && tempFileOwner) {
			Files.deleteIfExists(tempFileLocation);
		}
	}

	@Override
	public long size() {
		if (!isEof) {
			// Cannot yet know the full size
			return Message.MESSAGE_SIZE_UNKNOWN;
		}
		return charsReadTotal;
	}

	@Override
	public boolean isEmpty() throws IOException {
		bufferDataFromSource(StreamUtil.BUFFER_SIZE);
		if (isBufferedOnDisk()) {
			return false;
		}
		return charsReadTotal == 0L;
	}

	@Override
	public boolean isBinary() {
		return false;
	}

	@Override
	public synchronized @Nonnull Serializable asSerializable() throws IOException {
		//noinspection StatementWithEmptyBody
		while (bufferDataFromSource(StreamUtil.BUFFER_SIZE)) ; // Empty while because of side-effects in the condition
		if (isBufferedOnDisk()) {
			tempFileOwner = false;
			return new SerializableFileReference(tempFileLocation, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING, true);
		}

		char[] out = new char[Math.toIntExact(charsReadTotal)];
		int offset = 0;
		for (CharBufferBlock buffer: buffers) {
			System.arraycopy(buffer.buffer, 0, out, offset, buffer.count);
			offset += buffer.count;
		}
		return new String(out);
	}

	@Override
	public @Nonnull InputStream asInputStream() throws IOException {
		return new BufferedInputStream(ReaderInputStream.builder()
				.setReader(new BufferReadingReader())
				.get());
	}

	@Override
	public @Nonnull InputStream asInputStream(@Nonnull Charset encodingCharset) throws IOException {
		return new BufferedInputStream(ReaderInputStream.builder()
				.setReader(new BufferReadingReader())
				.setCharset(encodingCharset)
				.get());
	}

	@Override
	public @Nonnull Reader asReader() throws IOException {
		return new BufferReadingReader();
	}

	@Override
	public @Nonnull Reader asReader(@Nonnull Charset decodingCharset) throws IOException {
		// Ignore the decoding charset
		return new BufferReadingReader();
	}

	private class BufferReadingReader extends Reader {
		private long position = 0L;
		private long markedPosition = -1L;
		private Reader fileReader;

		@Override
		public void close() throws IOException {
			if (fileReader != null) {
				fileReader.close();
			}
		}

		private int available() {
			return Math.toIntExact(charsReadTotal - position);
		}

		/**
		 * Ensure that data is available (unless the source has reached EOF).
		 */
		private void ensureDataAvailable(int minToBuffer) throws IOException {
			if (available() == 0) {
				bufferDataFromSource(Math.max(minToBuffer, StreamUtil.BUFFER_SIZE));
			}
			if (isBufferedOnDisk() && fileReader == null) {
				fileReader = openFileInputStream(tempFileLocation, position);
			}
		}

		@Nonnull
		private Reader openFileInputStream(Path file, long skipTo) throws IOException {
			Reader fis = Files.newBufferedReader(file, StandardCharsets.UTF_8);
			try {
				if (skipTo > 0L) {
					long skipped = fis.skip(skipTo);
					if (skipped != skipTo) {
						throw new IllegalStateException("Skipped file position " + skipped + " != " + skipTo);
					}
				}
			} catch (IOException | RuntimeException e) {
				fis.close();
				throw e;
			}
			return fis;
		}

		private int calculateBufferIndex(long position) {
			return Math.toIntExact(position / StreamUtil.BUFFER_SIZE);
		}

		private int calculatePositionInBuffer(long position) {
			return Math.toIntExact(position % StreamUtil.BUFFER_SIZE);
		}

		private @Nullable CharBufferBlock getBufferForPosition(long position) {
			int index = calculateBufferIndex(position);
			if (index > buffers.size()) {
				return null;
			}
			return buffers.get(index);
		}

		private int readSingleCharFromBuffer() {
			int positionInBuffer = calculatePositionInBuffer(position);
			CharBufferBlock bufferBlock = getBufferForPosition(position);
			if (bufferBlock == null || positionInBuffer >= bufferBlock.count) {
				return -1; // EOF reached for this stream
			}
			++position;
			return bufferBlock.buffer[positionInBuffer] & 0xFFFF; // Ensure char is converted to int as unsigned number
		}

		@Override
		public int read() throws IOException {
			ensureDataAvailable(1);
			if (fileReader == null) {
				return readSingleCharFromBuffer();
			}
			int read = fileReader.read();
			if (read != -1) {
				++position;
			}
			return read;
		}

		@Override
		public int read(@Nonnull char[] b, int off, int len) throws IOException {
			ensureDataAvailable(len);
			if (fileReader == null) {
				return readBytesFromBuffer(b, off, len);
			}
			int read = fileReader.read(b, off, len);
			if (read != -1) {
				position += read;
			}
			return read;
		}

		private int readBytesFromBuffer(char[] b, int off, int len) {
			CharBufferBlock bufferBlock = getBufferForPosition(position);
			int positionInBuffer = calculatePositionInBuffer(position);
			if  (bufferBlock == null || positionInBuffer >= bufferBlock.count) {
				return -1;
			}
			int maxFromBuffer = Math.min(len, bufferBlock.count-positionInBuffer);
			position += maxFromBuffer;
			System.arraycopy(bufferBlock.buffer, positionInBuffer, b, off, maxFromBuffer);
			if (maxFromBuffer == len || position == charsReadTotal) {
				return maxFromBuffer;
			}
			return maxFromBuffer + readBytesFromBuffer(b, off + maxFromBuffer, len - maxFromBuffer);
		}

		@Override
		public boolean markSupported() {
			return true;
		}

		@Override
		public synchronized void mark(int readlimit) throws IOException {
			markedPosition = position;
			if (fileReader != null) {
				fileReader.mark(readlimit);
			}
		}

		@Override
		public synchronized void reset() throws IOException {
			position = markedPosition;
			if (fileReader != null) {
				fileReader.reset();
			}
		}
	}
}
