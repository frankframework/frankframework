/*
   Copyright 2024 WeAreFrank!

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

import java.io.BufferedOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.frankframework.util.StreamUtil;

import lombok.extern.log4j.Log4j2;

/**
 * Writes to an in-memory buffer until it 'overflows', after which a file on disk will be created and the in-memory buffer will be flushed to it.
 */
@Log4j2
public class OverflowToDiskOutputStream extends OutputStream implements AutoCloseable, Flushable {
	private List<BufferBlock> buffers; // temporary buffer, once full, write to disk
	private OutputStream outputStream;

	private final Path tempDirectory;
	private Path fileLocation;
	private boolean closed = false;

	/**
	 * The number of bytes in the buffer. This value is always in the range
	 * {@code 0} through {@code buf.length}; elements {@code buf[0]} through
	 * {@code buf[count-1]} contain valid byte data.
	 * 
	 * When count equals {@link #MAX_IN_MEMORY_SIZE} the buffer will be flushed to the {@link OutputStream OutputStream out}.
	 */
	private int currentBufferSize = 0;
	private final int maxBufferSize;

	public OverflowToDiskOutputStream(int maxSize, Path tempDirectory) throws IOException {
		this.tempDirectory = tempDirectory;

		// either the buffer or outputStream exists, but not both at the same time.
		if (maxSize > 0) {
			buffers = new ArrayList<>();
			buffers.add(new BufferBlock());
			this.maxBufferSize = maxSize;
		} else {
			outputStream = createFileOnDisk();
			this.maxBufferSize = 0;
		}
	}

	private OutputStream createFileOnDisk() throws IOException {
		fileLocation = Files.createTempFile(tempDirectory, "msg", "dat");
		OutputStream fos = Files.newOutputStream(fileLocation);
		return new BufferedOutputStream(fos, StreamUtil.BUFFER_SIZE);
	}

	private OutputStream flushBufferToDisk() throws IOException {
		if (outputStream != null) { //buffer has been reset, and fos exists.
			return outputStream;
		}
		log.info("flushing buffer to disk");

		// create the OutputStream and write the buffer to it.
		OutputStream overflow = createFileOnDisk();
		for (BufferBlock b : buffers) {
			overflow.write(b.buffer, 0, b.count);
		}

		// empty the buffer, there is no need to keep this in memory any longer.
		buffers = null;
		currentBufferSize = 0;

		return overflow;
	}

	@Override
	public void write(int b) throws IOException {
		write(new byte[] {(byte) b}, 0, 1);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if(closed) throw new IllegalStateException("stream already closed");

		// Buffer has already been flushed
		if(outputStream != null) {
			outputStream.write(b, off, len);
			return;
		}

		// If the request length exceeds the size of the output buffer, flush the output buffer and then write the data directly.
		if (len >= maxBufferSize - currentBufferSize) {
			log.trace("size in memory exceeded");

			outputStream = flushBufferToDisk();
			outputStream.write(b, off, len);
			return;
		}

		// Write to the buffer
		currentBufferSize += len;
		while (len > 0) {
			BufferBlock s = last();
			if (s.isFull()) {
				s = new BufferBlock();
				buffers.add(s);
			}
			final int n = Math.min(s.buffer.length - s.count, len);
			System.arraycopy(b, off, s.buffer, s.count, n);
			s.count += n;
			len -= n;
			off += n;
		}
	}

	/**
	 * Returns the last block
	 */
	private BufferBlock last() {
		return buffers.get(buffers.size() - 1);
	}

	static class BufferBlock {

		final byte[] buffer = new byte[StreamUtil.BUFFER_SIZE];

		int count;

		boolean isFull() {
			return count == buffer.length;
		}
	}

	/**
	 * If the contents was small enough to be kept in memory a ByteArray-message will be returned.
	 * If the contents was written to disk a {@link PathMessage TemporaryMessage} will be returned.
	 * Once read the buffer will be removed.
	 * @return A new {@link Message} object representing the contents written to this {@link OutputStream}.
	 */
	public Message toMessage() {
		if(!closed) throw new IllegalStateException("stream has not yet been closed");
		if(fileLocation == null && buffers == null) throw new IllegalStateException("stream has already been read");

		if(fileLocation != null) {
			log.trace("creating message from reference on disk");
			return PathMessage.asTemporaryMessage(fileLocation);
		} else {
			log.trace("creating message from in-memory buffer");
			final byte[] out = new byte[currentBufferSize];

			int outPtr = 0;
			Iterator<BufferBlock> i = buffers.iterator();
			while (i.hasNext()) {
				BufferBlock b = i.next();
				System.arraycopy(b.buffer, 0, out, outPtr, b.count);
				outPtr += b.count;
				i.remove();
			}

			buffers = null; // clear everything that's kept in memory
			return new Message(out);
		}
	}

	/** 
	 * Doesn't do anything if the message is kept in memory
	 */
	@Override
	public void flush() throws IOException {
		flush(false);
	}

	/**
	 * Doesn't do anything if the message is kept in memory unless parameter {@code writeBufferToDisk} is true.
	 * @param writeBufferToDisk Forces the in-memory buffer to be written to disk.
	 */
	public void flush(boolean writeBufferToDisk) throws IOException {
		if(writeBufferToDisk) {
			outputStream = flushBufferToDisk();
		}
		if(outputStream != null) {
			outputStream.flush();
		}
	}

	/**
	 * Flushes and closes the OutputStream, clears all resources, does not remove the file if has been created on disk.
	 */
	@Override
	public void close() throws IOException {
		if(closed) {
			log.debug("already closed");
			return;
		}
		closed = true;

		if(outputStream != null) {
			outputStream.close();
			outputStream = null;
		}
	}
}