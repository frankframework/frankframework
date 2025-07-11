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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.frankframework.util.AppConstants;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.TemporaryDirectoryUtils;

/**
 * Wrap a {@link InputStream} to provide repeatable access to its contents.
 * <p>
 *     As data is read from the source {@link InputStream} it is first buffered into memory, and once
 *     the limit of {@value Message#MESSAGE_MAX_IN_MEMORY_PROPERTY} is reached the memory buffer is
 *     flushed to disk and the remainder of the stream, as it is being read, is further streamed to disk.
 * </p>
 * <p>
 *     Reading the data is first read from the memory buffer, and once the data has been transferred to disk,
 *     it is read from disk.
 * </p>
 * <p>
 *     When reading the data from either memory or disk buffer, the implementation first ensures that the
 *     requested amount of data is available from the source stream.
 * </p>
 * <p>
 *     The implementation ensures that this works even when multiple independent streams are open and reading
 *     the data at the same time from different threads.
 * </p>
 */
public class RepeatableInputStreamWrapper implements RequestBuffer, AutoCloseable {
	private static final long MAX_IN_MEMORY_SIZE = AppConstants.getInstance().getLong(Message.MESSAGE_MAX_IN_MEMORY_PROPERTY, Message.MESSAGE_MAX_IN_MEMORY_DEFAULT);

	private final InputStream source;
	private boolean isEof = false; // True when EOF has been reached on the source InputStream
	private boolean closed = false;
	private final List<ByteBufferBlock> buffers; // temporary buffer, once full, write to disk
	private ByteBufferBlock currentBuffer;

	private OutputStream outputStream;
	private Path tempFileLocation;
	private boolean tempFileOwner = true;

	private long bytesReadTotal = 0L;

	public RepeatableInputStreamWrapper(@Nonnull InputStream source) {
		this.source = source;
		this.buffers = new ArrayList<>();
		this.currentBuffer = new ByteBufferBlock();
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
			byte[] data = new byte[size];
			int bytesRead = source.read(data, 0, size);
			if (bytesRead == -1) {
				isEof = true;
				source.close();
				outputStream.close();
				outputStream = null;
				return false;
			}
			bytesReadTotal += bytesRead;
			outputStream.write(data, 0, bytesRead);
			outputStream.flush(); // Flush, b/c we will instantly read from the file we write to
			return true;
		}

		// Buffer to memory
		int toRead = size;
		while (toRead > 0) {
			if (currentBuffer.isFull()) {
				currentBuffer = new ByteBufferBlock();
				buffers.add(currentBuffer);
			}
			ByteBufferBlock buffer = currentBuffer;
			int bytesRead = buffer.addFromStream(source, toRead);
			if (bytesRead == -1) {
				isEof = true;
				source.close();
				break;
			}
			bytesReadTotal += bytesRead;
			toRead -= bytesRead;
		}
		if (bytesReadTotal > MAX_IN_MEMORY_SIZE) {
			tempFileLocation = allocateTemporaryFile();
			outputStream = transferBuffersToFile(tempFileLocation, buffers);
			buffers.clear();
			currentBuffer = null;
		}
		return true;
	}

	private @Nullable OutputStream transferBuffersToFile(@Nonnull Path path, @Nonnull List<ByteBufferBlock> buffers) throws IOException {
		OutputStream out = Files.newOutputStream(path);
		try {
			for (ByteBufferBlock buffer: buffers) {
				buffer.transferToStream(out);
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
		CloseUtils.closeSilently(source, outputStream);
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
		return bytesReadTotal;
	}

	@Override
	public boolean isEmpty() throws IOException {
		// Read some data from source so that we can check if it has data
		bufferDataFromSource(StreamUtil.BUFFER_SIZE);
		if (isBufferedOnDisk()) {
			// When we are already buffering to disk, then for sure the input was not empty
			return false;
		}
		return bytesReadTotal == 0L;
	}

	@Override
	public boolean isBinary() {
		return true;
	}

	@Override
	public synchronized @Nonnull Serializable asSerializable() throws IOException {
		//noinspection StatementWithEmptyBody
		while (bufferDataFromSource(StreamUtil.BUFFER_SIZE)) ; // Empty while because of side-effects in the condition
		if (isBufferedOnDisk()) {
			tempFileOwner = false;
			return new SerializableFileReference(tempFileLocation, true);
		}

		byte[] out = new byte[Math.toIntExact(bytesReadTotal)];
		int offset = 0;
		for (ByteBufferBlock buffer: buffers) {
			System.arraycopy(buffer.buffer, 0, out, offset, buffer.count);
			offset += buffer.count;
		}
		return out;
	}

	@Override
	public @Nonnull InputStream asInputStream() throws IOException {
		return new BufferReadingInputStream();
	}

	@Override
	public @Nonnull InputStream asInputStream(@Nonnull Charset encodingCharset) throws IOException {
		// Ignore the encodingCharset
		return new BufferReadingInputStream();
	}

	@Override
	public @Nonnull Reader asReader() throws IOException {
		return StreamUtil.getCharsetDetectingInputStreamReader(asInputStream());
	}

	@Override
	public @Nonnull Reader asReader(@Nonnull Charset decodingCharset) throws IOException {
		return StreamUtil.getCharsetDetectingInputStreamReader(asInputStream(), decodingCharset.name());
	}

	private class BufferReadingInputStream extends InputStream {
		private long position = 0L;
		private long markedPosition = -1L;
		private InputStream fileInputStream;

		BufferReadingInputStream() throws IOException {
			if (tempFileLocation != null) {
				fileInputStream = openFileInputStream(tempFileLocation, position);
			}
		}

		@Override
		public void close() throws IOException {
			if (fileInputStream != null) {
				fileInputStream.close();
			}
		}

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
			if (available() == 0) {
				bufferDataFromSource(Math.max(minToBuffer, StreamUtil.BUFFER_SIZE));
			}
			if (isBufferedOnDisk() && fileInputStream == null) {
				// In this case, our previous read was from memory but now we have to continue reading from disk
				fileInputStream = openFileInputStream(tempFileLocation, position);
			}
		}

		@Nonnull
		private InputStream openFileInputStream(Path file, long skipTo) throws IOException {
			InputStream fis = Files.newInputStream(file, StandardOpenOption.READ);
			try {
				if (skipTo > 0L) {
					long skipped = fis.skip(skipTo);
					if (skipped != skipTo) {
						throw new IllegalStateException("Skipped file position " + skipped + " != " + skipTo);
					}
				}
			} catch (IOException | RuntimeException e) {
				// Close the file on exception
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

		private @Nullable ByteBufferBlock getBufferForPosition(long position) {
			int index = calculateBufferIndex(position);
			if (index > buffers.size()) {
				return null;
			}
			return buffers.get(index);
		}

		private int readSingleByteFromBuffer() {
			int positionInBuffer = calculatePositionInBuffer(position);
			ByteBufferBlock bufferBlock = getBufferForPosition(position);
			if (bufferBlock == null || positionInBuffer >= bufferBlock.count) {
				return -1; // EOF reached for this stream
			}
			++position;
			return bufferBlock.buffer[positionInBuffer] & 0xFF; // Ensure byte is converted to int as unsigned number
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
			if  (bufferBlock == null || positionInBuffer >= bufferBlock.count) {
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
}
