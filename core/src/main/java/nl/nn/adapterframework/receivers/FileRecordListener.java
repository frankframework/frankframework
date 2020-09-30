/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.receivers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.WildCardFilter;

/**
 * File {@link IPullingListener listener} that looks in a directory for files according to a wildcard. When a file is
 * found, it is read in a String object and parsed to records. 
 * After reading the file, the file is renamed and moved to a directory.
 * 
 * @author  Johan Verrips
 */
@Deprecated
@ConfigurationWarning("Please replace with DirectoryListener, in combination with a FileLineIteratorPipe")
public class FileRecordListener implements IPullingListener, INamedObject {
	protected Logger log = LogUtil.getLogger(this);

	private String name;
	private String inputDirectory;
	private String wildcard;
	private long responseTime = 1000;
	private String directoryProcessedFiles;
	private String storeFileNameInSessionKey;

	private long recordNo = 0; // the current record number;
	private String inputFileName; // the name of the file currently in process
	private ISender sender;
	private Iterator<String> recordIterator = null;

	@Override
	public void afterMessageProcessed(PipeLineResult processResult,	Object rawMessage, Map threadContext) throws ListenerException {
		String tcid = (String) threadContext.get(IPipeLineSession.technicalCorrelationIdKey);
		if (sender != null) {
			if (processResult.isSuccessful()) {
				try {
					sender.sendMessage(processResult.getResult(), null);
				} catch (Exception e) {
					throw new ListenerException("error sending message with technical correlationId [" + tcid + " msg [" + processResult.getResult() + "]", e);
				}
			}
		}
	}
	/**
	 * Moves a file to another directory and places a UUID in the name.
	 * @return String with the name of the (renamed and moved) file
	 * 
	 */
	protected String archiveFile(File file) throws ListenerException {
		boolean success = false;
		String directoryTo = getDirectoryProcessedFiles();
		String fullFilePath = "";
		// Destination directory
		File dir = new File(directoryTo);
		try {
			fullFilePath = file.getCanonicalPath();
		} catch (IOException e) {
			log.warn(getName() + " error retrieving canonical path of file [" + file.getName() + "]");
			fullFilePath = file.getName();
		}

		if (!dir.isDirectory()) {
			throw new ListenerException(getName() + " error renaming directory: The directory [" + directoryTo + "] to move the file [" + fullFilePath + "] is not a directory!");
		}
		// Move file to new directory
		String newFileName = Misc.createSimpleUUID() + "-" + file.getName();

		int dotPosition = file.getName().lastIndexOf(".");
		if (dotPosition > 0)
			newFileName = file.getName().substring(0, dotPosition) + "-" + Misc.createSimpleUUID() + file.getName().substring(dotPosition, file.getName().length());

		success = file.renameTo(new File(dir, newFileName));
		if (!success) {
			log.error(getName() + " was unable to move file [" + fullFilePath + "] to [" + directoryTo + "]");
			throw new ListenerException("unable to move file [" + fullFilePath + "] to [" + directoryTo + "]");
		} else
			log.info(getName() + " moved file [" + fullFilePath + "] to [" + directoryTo + "]");

		String result = null;
		try {
			result = new File(dir, newFileName).getCanonicalPath();
		} catch (IOException e) {
			throw new ListenerException("error retrieving canonical path of renamed file [" + file.getName() + "]", e);
		}
		return result;

	}
	
	@Override
	public void close() throws ListenerException {
		try {
			if (sender != null)
				sender.close();
		} catch (SenderException e) {
			throw new ListenerException("Error closing sender [" + sender.getName() + "]", e);
		}
	}
	@Override
	public void closeThread(Map threadContext) throws ListenerException {
		// nothing special
	}
	
	/**
	 * Configure does some basic checks (directoryProcessedFiles is a directory,  inputDirectory is a directory, wildcard is filled etc.);
	 *
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
		File files[] = dir.listFiles(filter);
		int count = (files == null ? 0 : files.length);
		for (int i = 0; i < count; i++) {
			File file = files[i];
			if (file.isDirectory()) {
				continue;
			}
			return (file);
		}
		return null;
	}
	/**
	 * Returns the name of the file in process (the {@link #archiveFile(File) archived} file) concatenated with the
	 * record number. As te {@link #archiveFile(File) archivedFile} method always renames to a 
	 * unique file, the combination of this filename and the recordnumber is unique, enabling tracing in case of errors
	 * in the processing of the file.
	 * Override this method for your specific needs! 
	 */
	@Override
	public String getIdFromRawMessage(Object rawMessage, Map threadContext) throws ListenerException {
		String correlationId = inputFileName + "-" + recordNo;
		PipeLineSessionBase.setListenerParameters(threadContext, correlationId, correlationId, null, null);
		return correlationId;
	}
	/**
	 * Retrieves a single record from a file. If the file is empty or fully processed, it looks wether there
	 * is a new file to process and returns the first record.
	 */
	@Override
	public synchronized Object getRawMessage(Map threadContext)
		throws ListenerException {
		String fullInputFileName = null;
		if (recordIterator != null) {
			if (recordIterator.hasNext()) {
				recordNo += 1;
				return recordIterator.next();
			}
		}
		if (getFileToProcess() != null) {
			File inputFile = getFileToProcess();
			log.info(
				" processing file [" + inputFile.getName() + "] size [" + inputFile.length() + "]");

			if (StringUtils.isNotEmpty(getStoreFileNameInSessionKey())) {
				threadContext.put(getStoreFileNameInSessionKey(),inputFile.getName());
			}

			String fileContent = "";
			try {
				fullInputFileName = inputFile.getCanonicalPath();
				fileContent = Misc.fileToString(fullInputFileName, "\n");
				inputFileName = archiveFile(inputFile);

			} catch (IOException e) {
				throw new ListenerException(" got exception opening " + inputFile.getName(), e);
			} finally {
				recordNo = 0;
			}
			log.info(" processing file [" + fullInputFileName + "]");
			recordIterator = parseToRecords(fileContent);
			if (recordIterator.hasNext()) {
				recordNo += 1;
				return recordIterator.next();
			}
		}

		// if nothing was found, just sleep thight.
		try {
			Thread.sleep(responseTime);
		} catch (InterruptedException e) {
			throw new ListenerException("interupted...", e);
		}

		return null;
	}
	
	@Override
	public Message extractMessage(Object rawMessage, Map threadContext) throws ListenerException {
		return Message.asMessage(rawMessage);
	}

	@Override
	public void open() throws ListenerException {
		try {
			if (sender != null)
				sender.open();
		} catch (SenderException e) {
			throw new ListenerException("error opening sender [" + sender.getName() + "]", e);
		}
		return;
	}

	@Override
	public Map openThread() throws ListenerException {
		return null;
	}
	/**
	 * Parse a String to an Iterator with objects (records). This method
	 * currently uses the end-of-line character ("\n") as a seperator. 
	 * This method is easy to extend to satisfy your project needs.
	 */
	protected Iterator<String> parseToRecords(String input) {
		StringTokenizer t = new StringTokenizer(input, "\n");
		List<String> array = new ArrayList<String>();
		while (t.hasMoreTokens()) {
			array.add(t.nextToken());
		}
		return array.iterator();
	}

	public void setSender(ISender sender) {
		this.sender = sender;
	}
	public ISender getSender() {
		return sender;
	}
	
	@Override
	public String toString() {
		String result = super.toString();
		ToStringBuilder ts = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
		ts.append("name", getName());
		ts.append("inputDirectory", getInputDirectory());
		ts.append("wildcard", getWildcard());
		result += ts.toString();
		return result;

	}
	
	@Override
	@IbisDoc({"name of the listener as known to the adapter.", ""})
	public void setName(String name) {
		this.name = name;
	}
	@Override
	public String getName() {
		return name;
	}
	
	/**
	 * set the directory name to look in for files.
	 * @see #setWildcard(String)
	 */
	@IbisDoc({"set the directory name to look in for files.", ""})
	public void setInputDirectory(String inputDirectory) {
		this.inputDirectory = inputDirectory;
	}
	public String getInputDirectory() {
		return inputDirectory;
	}

	/**
	 * set the {@link WildCardFilter wildcard} to look for files in the specified directory, e.g. "*.inp"
	 */
	@IbisDoc({"the {@link nl.nn.adapterframework.util.wildcardfilter wildcard} to look for files in the specified directory, e.g. \"*.inp\"", ""})
	public void setWildcard(String wildcard) {
		this.wildcard = wildcard;
	}
	public String getWildcard() {
		return wildcard;
	}

	/**
	 * Sets the directory to store processed files in
	 * @param directoryProcessedFiles The directoryProcessedFiles to set
	 */
	@IbisDoc({"the directory to store processed files in", ""})
	public void setDirectoryProcessedFiles(String directoryProcessedFiles) {
		this.directoryProcessedFiles = directoryProcessedFiles;
	}
	public String getDirectoryProcessedFiles() {
		return directoryProcessedFiles;
	}

	/**
	 * set the time to delay when no records are to be processed and this class has to look for the arrival of a new file
	 */
	@IbisDoc({"set the time to delay when no records are to be processed and this class has to look for the arrival of a new file", "1000 [ms]"})
	public void setResponseTime(long responseTime) {
		this.responseTime = responseTime;
	}
	public long getResponseTime() {
		return responseTime;
	}

	@IbisDoc({"when set, the name of the read file is stored under this session key", ""})
	public void setStoreFileNameInSessionKey(String storeFileNameInSessionKey) {
		this.storeFileNameInSessionKey = storeFileNameInSessionKey;
	}
	public String getStoreFileNameInSessionKey() {
		return storeFileNameInSessionKey;
	}
}
