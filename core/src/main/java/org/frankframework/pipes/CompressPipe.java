/*
   Copyright 2013 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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
package org.frankframework.pipes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.FileUtils;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.StringUtil;

/**
 * Pipe to zip or unzip a message or file.
 *
 * @author John Dekker
 * @author Jaco de Groot
 */
public class CompressPipe extends FixedForwardPipe {

	private static final int CHUNK_SIZE = 16384;

	private @Getter boolean messageIsContent = false;
	private Boolean resultIsContent;
	private @Getter String outputDirectory;
	private @Getter String filenamePattern;
	private @Getter String zipEntryPattern;
	private @Getter boolean compress;
	private @Getter FileFormat fileFormat;

	public enum FileFormat {
		/**
		 * Gzip format. It is used when compressing ({@code compress="true"}) and {@code resultIsContent="true"},
 * or when decompressing ({@code compress="false"}) and {@code messageIsContent="true"}.
		 */
		GZ,
		/**
		 * Zip format. It is also used when compressing ({@code compress="true"}) and {@code resultIsContent="false"},
		 * or when decompressing ({@code compress="false"}) and {@code messageIsContent="false"}.
		 */
		ZIP
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		// Defaults to true, only when outputDirectory has not been set
		if(resultIsContent == null) {
			resultIsContent = StringUtils.isEmpty(outputDirectory);
		}

		if (!resultIsContent && !messageIsContent && StringUtils.isEmpty(outputDirectory)) {
			throw new ConfigurationException("outputDirectory must be set");
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		InputStream in = null;
		try {
			boolean zipMultipleFiles = false;
			String filename = null;

			if (messageIsContent) {
				in = message.asInputStream();
			} else {
				filename = message.asString();
				zipMultipleFiles = StringUtils.contains(filename, ";");
				if (!compress || !zipMultipleFiles) {
					in = Files.newInputStream(Paths.get(filename));
				}
			}

			if (resultIsContent) {
				MessageBuilder messageBuilder = new MessageBuilder();
				try (OutputStream stream = messageBuilder.asOutputStream()) {
					processStream(stream, in, zipMultipleFiles, filename, session);
				}
				return new PipeRunResult(getSuccessForward(), messageBuilder.build());
			}

			String outFilename;
			if (messageIsContent) {
				outFilename = FileUtils.getFilename(getParameterList(), session, null, filenamePattern);
			} else {
				outFilename = FileUtils.getFilename(getParameterList(), session, new File(Objects.requireNonNull(filename)), filenamePattern);
			}

			File outFile = new File(outputDirectory, outFilename);
			String result = outFile.getAbsolutePath();
			try (OutputStream stream = Files.newOutputStream(outFile.toPath())) {
				processStream(stream, in, zipMultipleFiles, filename, session);
			}

			return new PipeRunResult(getSuccessForward(), result);
		} catch (Exception e) {
			throw new PipeRunException(this, "Unexpected exception during compression", e);
		} finally {
			if(in != null) {
				CloseUtils.closeSilently(in);
			}
		}
	}

	private void processStream(final OutputStream out, final InputStream in, boolean zipMultipleFiles, String filename, PipeLineSession session) throws ParameterException, IOException {
		if (zipMultipleFiles) {
			zipMultipleFiles(out, filename, session);
		} else {
			if (compress) {
				compressSingleFile(out, in, filename, session);
			} else {
				decompressSingleFile(out, in, filename, session);
			}
		}
	}

	private void decompressSingleFile(OutputStream out, InputStream in, String filename, PipeLineSession session) throws IOException, ParameterException {
		if ((getFileFormat() == FileFormat.GZ) || ((getFileFormat() == null) && messageIsContent)) {
			try (InputStream copyFrom = new GZIPInputStream(in); OutputStream copyTo = out) {
				StreamUtil.copyStream(copyFrom, copyTo, CHUNK_SIZE);
			}
		} else {
			try (ZipInputStream zipper = new ZipInputStream(in); OutputStream copyTo = out) {
				String zipEntryName = getZipEntryName(filename, session);
				if (zipEntryName.isEmpty()) {
					// Use first entry found
					zipper.getNextEntry();
				} else {
					// Position the stream at the specified entry
					ZipEntry zipEntry = zipper.getNextEntry();
					while (zipEntry != null && !zipEntry.getName().equals(zipEntryName)) {
						zipEntry = zipper.getNextEntry();
					}
				}
				StreamUtil.copyStream(zipper, copyTo, CHUNK_SIZE);
			}
		}
	}

	private void compressSingleFile(OutputStream out, InputStream in, String filename, PipeLineSession session) throws IOException, ParameterException {
		if ((getFileFormat() == FileFormat.GZ) || ((getFileFormat() == null) && resultIsContent)) {
			try (OutputStream copyTo = new GZIPOutputStream(out); InputStream copyFrom = in) {
				StreamUtil.copyStream(copyFrom, copyTo, CHUNK_SIZE);
			}
		} else {
			try (ZipOutputStream zipper = new ZipOutputStream(out); InputStream copyFrom = in) {
				String zipEntryName = getZipEntryName(filename, session);
				zipper.putNextEntry(new ZipEntry(zipEntryName));
				StreamUtil.copyStream(copyFrom, zipper, CHUNK_SIZE);
			}
		}
	}

	private void zipMultipleFiles(OutputStream out, String filename, PipeLineSession session) throws IOException, ParameterException {
		try (ZipOutputStream zipper = new ZipOutputStream(out)) {
			for (String s : StringUtil.split(filename, ";")) {
				String zipEntryName = getZipEntryName(s, session);
				zipper.putNextEntry(new ZipEntry(zipEntryName));
				try (InputStream fin = Files.newInputStream(Paths.get(s))) {
					StreamUtil.copyStream(fin, zipper, CHUNK_SIZE);
				} finally {
					zipper.closeEntry();
				}
			}
		}
	}

	private String getZipEntryName(String input, PipeLineSession session) throws ParameterException, IOException {
		try (Message pvlInput = new Message(input)) {
			ParameterValueList pvl = getParameterList().getValues(pvlInput, session);
			String value = ParameterValueList.getValue(pvl, "zipEntryPattern", (String) null);
			if(value != null) {
				return value;
			}
		}

		if (messageIsContent) {
			return FileUtils.getFilename(getParameterList(), session, null, zipEntryPattern);
		}
		return FileUtils.getFilename(getParameterList(), session, new File(input), zipEntryPattern);
	}

	/**
	 * If {@code true}, the pipe compresses; otherwise, it decompresses.
	 * @ff.default false
	 */
	public void setCompress(boolean b) {
		compress = b;
	}

	/** required if result is a file, the pattern for the result filename. Can be set with variables e.g. {file}.{ext}.zip in this example the {file} and {ext} variables are resolved with sessionKeys with the same name */
	@Deprecated(forRemoval = true, since = "8.1")
	@ConfigurationWarning("Please use a LocalFileSystemPipe with filename parameter (and optionally a pattern)")
	public void setFilenamePattern(String string) {
		filenamePattern = string;
	}

	/**
	 * A flag that indicates whether the message is the content or the path to a file with the contents. For multiple files, use ';' as a delimiter.
	 * @ff.default false
	 */
	public void setMessageIsContent(boolean b) {
		messageIsContent = b;
	}

	/** Required if the result is a file. The directory in which to store the result file. */
	@Deprecated(forRemoval = true, since = "8.1")
	@ConfigurationWarning("Please use resultIsContent=true in combination with a LocalFileSystemPipe")
	public void setOutputDirectory(String string) {
		outputDirectory = string;
	}

	/**
	 * A flag that indicates whether the result must be written to the message or to a file (filename = message). 
	 * @ff.default true when outputDirectory is not set.
	 */
	public void setResultIsContent(boolean b) {
		resultIsContent = b;
	}
	public boolean isResultIsContent() {
		return resultIsContent != null && resultIsContent;
	}

	/** The pattern for the zip entry name in case a zip file is read or written. */
	@Deprecated(forRemoval = true, since = "8.1")
	@ConfigurationWarning("Please use parameter zipEntryPattern (in combination with the pattern attribute)")
	public void setZipEntryPattern(String string) {
		zipEntryPattern = string;
	}

	public void setFileFormat(FileFormat format) {
		fileFormat = format;
	}

}
