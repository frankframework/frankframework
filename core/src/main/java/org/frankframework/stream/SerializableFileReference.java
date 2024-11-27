/*
   Copyright 2023-2024 WeAreFrank!

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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serial;
import java.io.Serializable;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.util.ClassUtils;
import org.frankframework.util.CleanerProvider;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.TemporaryDirectoryUtils;

/**
 * A reference to a file {@link Path} that can be serialized. When serialized it will write all the file data to
 * the serialization stream.
 * When deserialized, it will copy all file-data to a new temporary file.
 */
@Log4j2
public class SerializableFileReference implements Serializable, AutoCloseable {
	public static final String TEMP_MESSAGE_DIRECTORY = "temp-messages";

	@Serial private static final long serialVersionUID = 1L;
	private static final long customSerializationVersion = 1L;

	private long size = Message.MESSAGE_SIZE_UNKNOWN;
	@Getter private boolean binary;
	private String charset;
	@Getter private transient Path path;
	private transient CleanupFileAction cleanupFileAction;


	/**
	 * Create a new {@link SerializableFileReference} from the given {@link InputStream}. The {@link InputStream} will be copied
	 * to a temporary file on disk and will be closed afterward. The {@link SerializableFileReference} will be treated as binary data.
	 * <p>
	 * The temporary file will be deleted on calling {@link SerializableFileReference#close()}.
	 * </p>
	 *
	 * @param in The {@link InputStream} from which to create the {@link SerializableFileReference}. Will be closed after completion of this method.
	 * @return A new binary {@link SerializableFileReference}.
	 * @throws IOException If the {@link InputStream} cannot be read or a temporary file cannot be created / written to.
	 */
	public static SerializableFileReference of(InputStream in) throws IOException {
		try (InputStream ignored = in) {
			return new SerializableFileReference(true, null, true, copyToTempFile(in, -1));
		}
	}

	/**
	 * Create a new {@link SerializableFileReference} from the given {@code byte[]}. The {@code byte[]} will be copied
	 * to a temporary file on disk. The {@link SerializableFileReference} will be treated as binary data.
	 * <p>
	 * The temporary file will be deleted on calling {@link SerializableFileReference#close()}.
	 * </p>
	 *
	 * @param data The {@code byte[]} from which to create the {@link SerializableFileReference}.
	 * @return A new binary {@link SerializableFileReference}.
	 * @throws IOException If the {@code byte[]} cannot be written to a temporary file.
	 */
	public static SerializableFileReference of(byte[] data) throws IOException {
		return of(new ByteArrayInputStream(data));
	}

	/**
	 * Create a new {@link SerializableFileReference} from the given {@link Reader}. The {@link Reader} will be copied
	 * to a temporary file on disk and will be closed afterward. The {@link SerializableFileReference} will be treated as character data.
	 * <p>
	 * The temporary file will be deleted on calling {@link SerializableFileReference#close()}.
	 * </p>
	 *
	 * @param in The {@link Reader} from which to create the {@link SerializableFileReference}. Will be closed after completion of this method.
	 * @param charset Character set of the data in the {@link Reader}.
	 * @return A new character-data {@link SerializableFileReference}.
	 * @throws IOException If the {@link Reader} cannot be read or a temporary file cannot be created / written to.
	 */
	public static SerializableFileReference of(Reader in, String charset) throws IOException {
		try (InputStream is = ReaderInputStream.builder().setReader(in).setCharset(charset).get()) {
			return new SerializableFileReference(false, charset, true, copyToTempFile(is, -1));
		}
	}

	/**
	 * Create a new {@link SerializableFileReference} from the given {@link String}. The {@link String} will be copied
	 * to a temporary file on disk. The {@link SerializableFileReference} will be treated as character data.
	 * <p>
	 * The temporary file will be deleted on calling {@link SerializableFileReference#close()}.
	 * </p>
	 *
	 * @param data The {@link String} from which to create the {@link SerializableFileReference}.
	 * @param charset Character used for writing out and reading back the data.
	 * @return A new character-data {@link SerializableFileReference}.
	 * @throws IOException If the {@link String} data cannot be written to a temporary file.
	 */
	public static SerializableFileReference of(String data, String charset) throws IOException {
		return of(new StringReader(data), charset);
	}

	/**
	 * Create a binary {@code SerializableFileReference} for the file at given path.
	 *
	 * @param path {@link Path} to the file being referenced.
	 * @param deleteOnClose if the temporary file will be deleted on calling {@link SerializableFileReference#close()}.
	 */
	public SerializableFileReference(Path path, boolean deleteOnClose) {
		this(true, null, deleteOnClose, path);
	}

	/**
	 * Create a character-data {@code SerializableFileReference} for the file at given path.
	 * <p>
	 * The file will be not deleted on calling {@link SerializableFileReference#close()}.
	 * </p>
	 *
	 * @param charset Character set to be used when reading the file.
	 * @param path {@link Path} to the file being referenced.
	 */
	public SerializableFileReference(String charset, Path path) {
		this(charset == null, charset, false, path);
	}

	private SerializableFileReference(boolean binary, String charset, boolean isFileOwner, Path path) {
		this.binary = binary;
		this.charset = charset;
		this.path = path;
		if (isFileOwner) {
			createCleanerAction(path);
		}
	}

	private void createCleanerAction(final Path path) {
		cleanupFileAction = new CleanupFileAction(path);
		CleanerProvider.register(this, cleanupFileAction);
	}

	private static class CleanupFileAction implements Runnable {
		private final Path fileToClean;

		private CleanupFileAction(Path fileToClean) {
			this.fileToClean = fileToClean;
		}

		@Override
		public void run() {
			try {
				Files.deleteIfExists(fileToClean);
			} catch (Exception e) {
				log.warn("failed to remove file reference [{}]. Exception message: {}", fileToClean, e.getMessage());
			}
		}
	}

	public long getSize() {
		if (size == Message.MESSAGE_SIZE_UNKNOWN) {
			size = getFileSize();
		}
		return size;
	}

	public BufferedReader getReader() throws IOException {
		if (StringUtils.isNotBlank(charset)) {
			return Files.newBufferedReader(path, Charset.forName(charset));
		} else {
			return Files.newBufferedReader(path);
		}
	}

	public InputStream getInputStream() throws IOException {
		return Files.newInputStream(path);
	}

	private long getFileSize() {
		try {
			return Files.size(path);
		} catch (IOException e) {
			log.warn("unable to determine size of stream [{}], error: {}", ClassUtils.nameOf(path), e.getMessage(), e);
			return -1;
		}
	}

	@Override
	public void close() {
		if (cleanupFileAction != null) {
			CleanerProvider.clean(cleanupFileAction);
		}
	}

	@Serial
	private void writeObject(ObjectOutputStream out) throws IOException {
		// If in future we need to make incompatible changes we can keep reading old version by selecting on version-nr
		out.writeLong(customSerializationVersion);
		out.writeLong(getSize());
		out.writeBoolean(binary);
		out.writeUTF(charset != null ? charset : "");
		try (InputStream fileInputStream = getInputStream()) {
			long bytesWritten = StreamUtil.copyStream(fileInputStream, out, 16384);
			if (bytesWritten != this.size) {
				throw new IllegalStateException("File size was reported as " + this.size + " bytes but size written to stream was " + bytesWritten + " bytes; this means the stream cannot be correctly read back!");
			}
		}
	}

	/**
	 * Method is called by the serialization framework (from java.io.ObjectStreamClass) when deserializing an object.
	 * @param in ObjectInputStream to create object from
	 * @throws IOException if there is a problem reading from the stream
	 */
	@Serial
	private void readObject(ObjectInputStream in) throws IOException {
		in.readLong(); // Custom serialization version; only version 1 yet so value can be ignored for now.
		size = in.readLong();
		binary = in.readBoolean();
		charset = in.readUTF();
		path = copyToTempFile(in, size);
		createCleanerAction(path);
	}

	/**
	 * Write {@link InputStream} to a newly created temporary file and return a {@link Path} to that temporary file. If
	 * {@code maxBytes} is a positive number, then only that many bytes are copied.
	 * The {@link InputStream} is not closed.
	 * See also {@link StreamUtil#copyPartialStream(InputStream, OutputStream, long, int)}.
	 *
	 * @param in The {@link InputStream} from which to read data.
	 * @param maxBytes Maximum number of bytes to be copied. If a negative number, all data will be copied.
	 * @return {@link Path} of the temporary file to which the data has been copied.
	 * @throws IOException Thrown if there was any exception reading or writing the data.
	 */
	private static Path copyToTempFile(InputStream in, long maxBytes) throws IOException {
		Path tmpMessagesFolder = TemporaryDirectoryUtils.getTempDirectory(TEMP_MESSAGE_DIRECTORY);
		Path destination = Files.createTempFile(tmpMessagesFolder, "msg", ".dat");
		try (OutputStream fileOutputStream = Files.newOutputStream(destination)) {
			StreamUtil.copyPartialStream(in, fileOutputStream, maxBytes, 16384);
		}
		return destination;
	}
}
