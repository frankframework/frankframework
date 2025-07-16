/*
   Copyright 2024-2025 WeAreFrank!

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
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.Cleaner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;

import lombok.extern.log4j.Log4j2;

import org.frankframework.util.CleanerProvider;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.StreamUtil;

/**
 * Writes to an in-memory buffer until it 'overflows', after which a file on disk will be created and the in-memory buffer will be flushed to it.
 */
@Log4j2
public class OverflowToDiskOutputStream extends OutputStream implements AutoCloseable, Flushable {
	private List<ByteBufferBlock> buffers; // temporary buffer, once full, write to disk
	private ByteBufferBlock lastBlock;
	private OutputStream outputStream;

	private final Path tempDirectory;
	private Path fileLocation;
	private boolean closed = false;

	private CleanupFileAction cleanupFileAction;

	/**
	 * The number of bytes in the buffer. This value is always in the range
	 * {@code 0} through {@code buf.length}; elements {@code buf[0]} through
	 * {@code buf[count-1]} contain valid byte data.
	 * 
	 * When count equals {@link #maxBufferSize} the buffer will be flushed to the {@link OutputStream OutputStream out}.
	 */
	private int currentBufferSize = 0;
	private final int maxBufferSize;

	public OverflowToDiskOutputStream(int maxSize, Path tempDirectory) throws IOException {
		this.tempDirectory = tempDirectory;

		// either the buffer or outputStream exists, but not both at the same time.
		if (maxSize > 0) {
			buffers = new ArrayList<>();
			lastBlock = new ByteBufferBlock();
			buffers.add(lastBlock);
			this.maxBufferSize = maxSize;
		} else {
			outputStream = createFileOnDisk();
			this.maxBufferSize = 0;
		}
	}

	private OutputStream createFileOnDisk() throws IOException {
		fileLocation = Files.createTempFile(tempDirectory, "msg", "dat");

		OutputStream fos = Files.newOutputStream(fileLocation);
		createCleanerAction(fileLocation, fos);
		return new BufferedOutputStream(fos, StreamUtil.BUFFER_SIZE);
	}

	/**
	 * Register the newly create file with the {@link Cleaner} in case an exception occurs during file writing, we want the file to be removed.
	 */
	private void createCleanerAction(final Path path, final Closeable closable) {
		cleanupFileAction = new CleanupFileAction(path, closable);
		CleanerProvider.register(this, cleanupFileAction);
	}

	private static class CleanupFileAction implements Runnable {
		private final Path fileToClean;
		private final Closeable closable;
		private boolean shouldClean = true;

		private CleanupFileAction(Path fileToClean, Closeable closable) {
			this.fileToClean = fileToClean;
			this.closable = closable;
		}

		@Override
		public void run() {
			if (shouldClean) {
				LogManager.getLogger("LEAK_LOG").info("Leak detection: File [{}] was never converted to a Message", fileToClean);

				CloseUtils.closeSilently(closable);
				try {
					Files.deleteIfExists(fileToClean);
				} catch (Exception e) {
					log.warn("failed to remove file reference {}", fileToClean);
				}
			}
		}
	}

	private OutputStream flushBufferToDisk() throws IOException {
		if (outputStream != null) { //buffer has been reset, and fos exists.
			return outputStream;
		}
		log.info("flushing buffer to disk");

		// create the OutputStream and write the buffer to it.
		OutputStream overflow = createFileOnDisk();
		for (ByteBufferBlock b : buffers) {
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
			ByteBufferBlock s = lastBlock;
			if (s.isFull()) {
				s = new ByteBufferBlock();
				buffers.add(s);
				lastBlock = s;
			}
			final int n = Math.min(s.buffer.length - s.count, len);
			System.arraycopy(b, off, s.buffer, s.count, n);
			s.count += n;
			len -= n;
			off += n;
		}
	}

	/**
	 * If the contents was small enough to be kept in memory a ByteArray-message will be returned.
	 * If the contents was written to disk a {@link PathMessage TemporaryMessage} will be returned.
	 * Once read the buffer will be removed.
	 *
	 * @return A new {@link Message} object representing the contents written to this {@link OutputStream}.
	 */
	public Message toMessage() {
		return toMessage(true);
	}

	/**
	 * If the contents was small enough to be kept in memory a ByteArray-message will be returned.
	 * If the contents was written to disk a {@link PathMessage TemporaryMessage} will be returned.
	 * Once read the buffer will be removed.
	 * @return A new {@link Message} object representing the contents written to this {@link OutputStream}.
	 */
	public Message toMessage(boolean binary) {
		if(!closed) throw new IllegalStateException("stream has not yet been closed");
		if(fileLocation == null && buffers == null) throw new IllegalStateException("stream has already been read");

		if(fileLocation != null) {
			log.trace("creating message from reference on disk");
			try {
				return PathMessage.asTemporaryMessage(fileLocation);
			} finally {
				//Since we were successfully able to create a PathMessage (which will cleanup the file on close) remove the reference here.
				cleanupFileAction.shouldClean = false;
				CleanerProvider.clean(cleanupFileAction);
			}
		} else {
			log.trace("creating message from in-memory buffer");
			final byte[] out = new byte[currentBufferSize];

			int outPtr = 0;
			Iterator<ByteBufferBlock> i = buffers.iterator();
			while (i.hasNext()) {
				ByteBufferBlock b = i.next();
				System.arraycopy(b.buffer, 0, out, outPtr, b.count);
				outPtr += b.count;
				i.remove();
			}

			buffers = null; // clear everything that's kept in memory
			if (binary) {
				return new Message(out);
			} else {
				return new Message(new String(out));
			}
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
