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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nonnull;

import org.frankframework.util.AppConstants;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.TemporaryDirectoryUtils;

public class RepeatableInputStreamWrapper implements RequestBuffer, AutoCloseable {
	private static final long MAX_IN_MEMORY_SIZE = AppConstants.getInstance().getLong(Message.MESSAGE_MAX_IN_MEMORY_PROPERTY, Message.MESSAGE_MAX_IN_MEMORY_DEFAULT);

	private final InputStream source;
	private boolean isEof = false;

	private final List<ByteBufferBlock> buffers; // temporary buffer, once full, write to disk
	private ByteBufferBlock currentBuffer;

	private OutputStream outputStream;
	private Path fileLocation;

	private long bytesReadTotal = 0L;


	public RepeatableInputStreamWrapper(InputStream source) {
		this.source = source;
		this.buffers = new ArrayList<>();
		this.currentBuffer = new  ByteBufferBlock();
		this.buffers.add(currentBuffer);
	}

	private void bufferDataFromSource(int size) throws IOException {
		if (size <= 0 || isEof) {
			return;
		}

		// Already more bytes read than should be buffered in memory
		if (outputStream != null) {
			byte[] data = new byte[size];
			int bytesRead = source.read(data, 0, size);
			if (bytesRead == -1) {
				isEof = true;
				return;
			}
			bytesReadTotal += bytesRead;
			outputStream.write(data, 0, bytesRead);
			outputStream.flush();
			return;
		}

		// Buffer to memory
		int toRead = size;
		while (toRead > 0) {
			if (currentBuffer.isFull()) {
				currentBuffer = new  ByteBufferBlock();
				buffers.add(currentBuffer);
			}
			ByteBufferBlock buffer = currentBuffer;
			int bytesRead = buffer.addFromStream(source, toRead);
			if (bytesRead == -1) {
				isEof = true;
				break;
			}
			bytesReadTotal += bytesRead;
			toRead -= bytesRead;
		}
		if (bytesReadTotal > MAX_IN_MEMORY_SIZE) {
			fileLocation = allocateTemporaryFile();
			outputStream = transferBuffersToFile(fileLocation, buffers);
			buffers.clear();
			currentBuffer = null;
		}
	}

	private @Nonnull OutputStream transferBuffersToFile(Path fileLocation, List<ByteBufferBlock> buffers) throws IOException {
		OutputStream out = Files.newOutputStream(fileLocation);
		for (ByteBufferBlock buffer: buffers) {
			buffer.transferToStream(out);
		}
		return out;
	}

	private @Nonnull Path allocateTemporaryFile() throws IOException {
		Path tempDirectory = TemporaryDirectoryUtils.getTempDirectory(SerializableFileReference.TEMP_MESSAGE_DIRECTORY);
		return Files.createTempFile(tempDirectory, "msg", "dat");
	}

	@Override
	public void close() throws Exception {
		CloseUtils.closeSilently(source, outputStream);
		buffers.clear();
		currentBuffer = null;
		if (fileLocation != null) {
			Files.deleteIfExists(fileLocation);
		}
	}

	@Override
	public InputStream asInputStream() {
		return new BufferReadingInputStream();
	}

	@Override
	public Reader asReader() throws IOException {
		return StreamUtil.getCharsetDetectingInputStreamReader(asInputStream());
	}

	@Override
	public Reader asReader(Charset charset) {
		return new BufferedReader(new InputStreamReader(asInputStream(), charset));
	}

	private class BufferReadingInputStream extends InputStream {
		private long position = 0L;
		private long markedPosition = -1L;
		private InputStream fileInputStream;

		@Override
		public int available() throws IOException {
			if (fileInputStream != null) {
				return fileInputStream.available();
			}
			return Math.toIntExact(bytesReadTotal - position);
		}

		/**
		 * Ensure that data is available (unless the source has reached EOF).
		 */
		private void ensureDataAvailable(int minToBuffer) throws IOException {
			boolean inMemoryBefore = isBufferedInMemory();
			if (available() == 0) {
				bufferDataFromSource(Math.max(minToBuffer, StreamUtil.BUFFER_SIZE));
			}
			boolean inMemoryAfter = isBufferedInMemory();
			if (inMemoryBefore && !inMemoryAfter) {
				fileInputStream = Files.newInputStream(fileLocation, StandardOpenOption.READ);
				long skipped = fileInputStream.skip(position);
				if (skipped != position) {
					throw new IllegalStateException("Skipped file position " + skipped + " != " + position);
				}
			}
		}

		private int calculateBufferIndex(long position) {
			return Math.toIntExact(position / StreamUtil.BUFFER_SIZE);
		}

		private int calculatePositionInBuffer(long position) {
			return Math.toIntExact(position % StreamUtil.BUFFER_SIZE);
		}

		private ByteBufferBlock getBufferForPosition(long position) {
			return buffers.get(calculateBufferIndex(position));
		}

		private int readSingleByteFromBuffer() {
			int positionInBuffer = calculatePositionInBuffer(position);
			ByteBufferBlock bufferBlock = getBufferForPosition(position);
			if (positionInBuffer > bufferBlock.count) {
				return -1; // EOF reached for this stream
			}
			++position;
			return bufferBlock.buffer[positionInBuffer];
		}

		@Override
		public int read() throws IOException {
			ensureDataAvailable(1);
			if (fileInputStream == null) {
				return readSingleByteFromBuffer();
			}
			int read = fileInputStream.read();
			if (read != -1) {
				++position;
			}
			return read;
		}

		@Override
		public int read(@Nonnull byte[] b, int off, int len) throws IOException {
			ensureDataAvailable(len);
			if (fileInputStream == null) {
				return readBytesFromBuffer(b, off, len);
			}
			int read = fileInputStream.read(b, off, len);
			if (read != -1) {
				position += read;
			}
			return read;
		}

		private int readBytesFromBuffer(byte[] b, int off, int len) {
			ByteBufferBlock bufferBlock = getBufferForPosition(position);
			int positionInBuffer = calculatePositionInBuffer(position);
			if  (positionInBuffer >= bufferBlock.count) {
				return -1;
			}
			int maxFromBuffer = Math.min(len, bufferBlock.count-positionInBuffer);
			position += maxFromBuffer;
			System.arraycopy(bufferBlock.buffer, positionInBuffer, b, off, maxFromBuffer);
			if (maxFromBuffer == len || position == bytesReadTotal) {
				return maxFromBuffer;
			}
			return maxFromBuffer + readBytesFromBuffer(b, off + maxFromBuffer, len - maxFromBuffer);
		}

		@Override
		public boolean markSupported() {
			return true;
		}

		@Override
		public synchronized void mark(int readlimit) {
			markedPosition = position;
			if (fileInputStream != null) {
				fileInputStream.mark(readlimit);
			}
		}

		@Override
		public synchronized void reset() throws IOException {
			position = markedPosition;
			if (fileInputStream != null) {
				fileInputStream.reset();
			}
		}
	}

	private boolean isBufferedInMemory() {
		return fileLocation == null;
	}
}
