/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2023 WeAreFrank!

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
package org.frankframework.receivers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.IPullingListener;
import org.frankframework.core.ISender;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.stream.Message;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.StringUtil;
import org.frankframework.util.UUIDUtil;
import org.frankframework.util.WildCardFilter;

/**
 * File {@link IPullingListener listener} that looks in a directory for files according to a wildcard. When a file is
 * found, it is read in a String object and parsed to records.
 * After reading the file, the file is renamed and moved to a directory.
 *
 * @author Johan Verrips
 */
@Deprecated
@ConfigurationWarning("Please replace with DirectoryListener, in combination with a FileLineIteratorPipe")
public class FileRecordListener implements IPullingListener<String> {
	protected Logger log = LogUtil.getLogger(this);
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	private String name;
	private String inputDirectory;
	private String wildcard;
	private long responseTime = 1000;
	private String directoryProcessedFiles;
	private String storeFileNameInSessionKey;

	private long recordNo = 0; // the current record number
	private String inputFileName; // the name of the file currently in process
	private ISender sender;
	private Iterator<String> recordIterator = null;

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<String> rawMessage, PipeLineSession pipeLineSession) throws ListenerException {
		String cid = pipeLineSession.getCorrelationId();
		if (sender != null && processResult.isSuccessful()) {
			try(PipeLineSession session = new PipeLineSession()) {
				session.put(PipeLineSession.CORRELATION_ID_KEY, cid);
				sender.sendMessageOrThrow(processResult.getResult(), session).close();
			} catch (Exception e) {
				throw new ListenerException("error sending message with technical correlationId [" + cid + " msg [" + processResult.getResult() + "]", e);
			}
		}
	}

	/**
	 * Moves a file to another directory and places a UUID in the name.
	 *
	 * @return String with the name of the (renamed and moved) file
	 */
	protected String archiveFile(File file) throws ListenerException {
		String directoryTo = getDirectoryProcessedFiles();
		String fullFilePath = "";
		// Destination directory
		File dir = new File(directoryTo);
		try {
			fullFilePath = file.getCanonicalPath();
		} catch (IOException e) {
			log.warn("{} error retrieving canonical path of file [{}]", this::getName, file::getName);
			fullFilePath = file.getName();
		}

		if (!dir.isDirectory()) {
			throw new ListenerException(getName() + " error renaming directory: The directory [" + directoryTo + "] to move the file [" + fullFilePath + "] is not a directory!");
		}
		// Move file to new directory
		String newFileName;
		int dotPosition = file.getName().lastIndexOf(".");
		if (dotPosition > 0) {
			newFileName = file.getName().substring(0, dotPosition) + "-" + UUIDUtil.createSimpleUUID() + file.getName().substring(dotPosition);
		} else {
			newFileName = UUIDUtil.createSimpleUUID() + "-" + file.getName();
		}

		boolean success = file.renameTo(new File(dir, newFileName));
		if (!success) {
			log.error("{} was unable to move file [{}] to [{}]", getName(), fullFilePath, directoryTo);
			throw new ListenerException("unable to move file [" + fullFilePath + "] to [" + directoryTo + "]");
		} else {
			log.info("{} moved file [{}] to [{}]", getName(), fullFilePath, directoryTo);
		}

		try {
			return new File(dir, newFileName).getCanonicalPath();
		} catch (IOException e) {
			throw new ListenerException("error retrieving canonical path of renamed file [" + file.getName() + "]", e);
		}
	}

	@Override
	public void close() throws ListenerException {
		try {
			if (sender != null) {
				sender.close();
			}
		} catch (SenderException e) {
			throw new ListenerException("Error closing sender [" + sender.getName() + "]", e);
		}
	}

	@Override
	public void closeThread(@Nonnull Map<String, Object> threadContext) throws ListenerException {
		// nothing special
	}

	/**
	 * Configure does some basic checks (directoryProcessedFiles is a directory,  inputDirectory is a directory, wildcard is filled etc.);
	 */
	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getInputDirectory()))
			throw new ConfigurationException("no value specified for [inputDirectory]");
		if (StringUtils.isEmpty(getWildcard()))
			throw new ConfigurationException("no value specified for [wildcard]");
		if (StringUtils.isEmpty(getDirectoryProcessedFiles()))
			throw new ConfigurationException("no value specified for [directoryProcessedFiles]");
		File dir = new File(getDirectoryProcessedFiles());
		if (!dir.isDirectory()) {
			throw new ConfigurationException("The value for [directoryProcessedFiles] :[ " + getDirectoryProcessedFiles() + "] is invalid. It is not a directory ");
		}
		File inp = new File(getInputDirectory());
		if (!inp.isDirectory()) {
			throw new ConfigurationException("The value for [inputDirectory] :[ " + getInputDirectory() + "] is invalid. It is not a directory ");
		}
		try {
			if (sender != null)
				sender.configure();
		} catch (ConfigurationException e) {
			throw new ConfigurationException("error opening sender [" + sender.getName() + "]", e);
		}

	}

	/**
	 * Gets a file to process.
	 */
	protected File getFileToProcess() {
		WildCardFilter filter = new WildCardFilter(wildcard);
		File dir = new File(getInputDirectory());
		File[] files = dir.listFiles(filter);
		int count = files == null ? 0 : files.length;
		for (int i = 0; i < count; i++) {
			File file = files[i];
			if (file.isDirectory()) {
				continue;
			}
			return file;
		}
		return null;
	}

	private String constructMessageId() {
		return inputFileName + "-" + recordNo;
	}

	/**
	 * Retrieves a single record from a file. If the file is empty or fully processed, it looks wether there
	 * is a new file to process and returns the first record.
	 */
	@Override
	public synchronized RawMessageWrapper<String> getRawMessage(@Nonnull Map<String, Object> threadContext)
			throws ListenerException {
		String fullInputFileName;
		if (recordIterator != null && recordIterator.hasNext()) {
			recordNo += 1;
			String id = constructMessageId();
			PipeLineSession.updateListenerParameters(threadContext, id, id);
			return new RawMessageWrapper<>(recordIterator.next(), id, id);
		}
		if (getFileToProcess() != null) {
			File inputFile = getFileToProcess();
			log.info(" processing file [{}] size [{}]", inputFile::getName, inputFile::length);

			if (StringUtils.isNotEmpty(getStoreFileNameInSessionKey())) {
				threadContext.put(getStoreFileNameInSessionKey(), inputFile.getName());
			}

			String fileContent = "";
			try {
				fullInputFileName = inputFile.getCanonicalPath();
				fileContent = StreamUtil.fileToString(fullInputFileName, "\n");
				inputFileName = archiveFile(inputFile);

			} catch (IOException e) {
				throw new ListenerException(" got exception opening " + inputFile.getName(), e);
			} finally {
				recordNo = 0;
			}
			log.info(" processing file [{}]", fullInputFileName);
			recordIterator = parseToRecords(fileContent);
			if (recordIterator.hasNext()) {
				recordNo += 1;
				String id = constructMessageId();
				PipeLineSession.updateListenerParameters(threadContext, id, id);
				return new RawMessageWrapper<>(recordIterator.next(), id, id);
			}
		}

		// if nothing was found, just sleep tight.
		try {
			Thread.sleep(responseTime);
		} catch (InterruptedException e) {
			throw new ListenerException("interrupted...", e);
		}

		return null;
	}

	@Override
	public Message extractMessage(@Nonnull RawMessageWrapper<String> rawMessage, @Nonnull Map<String, Object> context) {
		return Message.asMessage(rawMessage.getRawMessage());
	}

	@Override
	public void open() throws ListenerException {
		try {
			if (sender != null) sender.open();
		} catch (SenderException e) {
			throw new ListenerException("error opening sender [" + sender.getName() + "]", e);
		}
	}

	@Nonnull
	@Override
	public Map<String, Object> openThread() throws ListenerException {
		return new HashMap<>();
	}

	/**
	 * Parse a String to an Iterator with objects (records). This method
	 * currently uses the end-of-line character ("\n") as a seperator.
	 * This method is easy to extend to satisfy your project needs.
	 */
	protected Iterator<String> parseToRecords(String input) {
		return StringUtil.splitToStream(input, "\n").iterator();
	}

	public void setSender(ISender sender) {
		this.sender = sender;
	}

	public ISender getSender() {
		return sender;
	}

	@Override
	public String toString() {
		ToStringBuilder ts = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
		ts.append("name", getName());
		ts.append("inputDirectory", getInputDirectory());
		ts.append("wildcard", getWildcard());

		return super.toString() + ts;
	}

	/** name of the listener as known to the adapter. */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	/** the directory name to look in for files. */
	public void setInputDirectory(String inputDirectory) {
		this.inputDirectory = inputDirectory;
	}

	public String getInputDirectory() {
		return inputDirectory;
	}

	/** the wildcard to look for files in the specified directory, e.g. \"*.inp\" */
	public void setWildcard(String wildcard) {
		this.wildcard = wildcard;
	}

	public String getWildcard() {
		return wildcard;
	}

	/** the directory to store processed files in */
	public void setDirectoryProcessedFiles(String directoryProcessedFiles) {
		this.directoryProcessedFiles = directoryProcessedFiles;
	}

	public String getDirectoryProcessedFiles() {
		return directoryProcessedFiles;
	}

	/**
	 * The time <i>in milliseconds</i> to delay when no records are to be processed, and this class has to look for the arrival of a new file
	 *
	 * @ff.default 1000
	 */
	public void setResponseTime(long responseTime) {
		this.responseTime = responseTime;
	}

	public long getResponseTime() {
		return responseTime;
	}

	/** when set, the name of the read file is stored under this session key */
	public void setStoreFileNameInSessionKey(String storeFileNameInSessionKey) {
		this.storeFileNameInSessionKey = storeFileNameInSessionKey;
	}

	public String getStoreFileNameInSessionKey() {
		return storeFileNameInSessionKey;
	}
}
