/*
 * $Log: FileRecordListener.java,v $
 * Revision 1.7  2006-01-05 14:42:25  europe\L190409
 * updated javadoc and reordered code
 *
 * Revision 1.6  2004/08/23 13:10:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated JavaDoc
 *
 * Revision 1.5  2004/03/26 10:43:03  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.4  2004/03/23 18:16:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 */
package nl.nn.adapterframework.receivers;

import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.WildCardFilter;
import nl.nn.adapterframework.util.Misc;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * File {@link nl.nn.adapterframework.core.IPullingListener listener} that looks in a directory for files according to a wildcard. When a file is
 * found, it is read in a String object and parsed to records. When used in a receiver like 
 * {@link FileRecordReceiver } these records are to be processed by the adapter.
 * After reading the file, the file is renamed and moved to a directory.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.receivers.FileRecordListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the listener as known to the adapter.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInputDirectory(String) inputDirectory}</td><td>set the directory name to look in for files.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setWildcard(String) wildcard}</td><td>the {@link nl.nn.adapterframework.util.WildCardFilter wildcard} to look for files in the specified directory, e.g. "*.inp"</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setResponseTime(long) responseTime}</td><td>set the time to delay when no records are to be processed and this class has to look for the arrival of a new file</td><td>1000 [ms]</td></tr>
 * <tr><td>{@link #setDirectoryProcessedFiles(String) directoryProcessedFiles}</td><td>the directory to store processed files in</td><td>&nbsp;</td></tr>
 * </table>
 * 
 * @version Id
 * @author  Johan Verrips
 */
public class FileRecordListener implements IPullingListener, INamedObject {
	public static final String version="$RCSfile: FileRecordListener.java,v $ $Revision: 1.7 $ $Date: 2006-01-05 14:42:25 $";
	protected Logger log = Logger.getLogger(this.getClass());

	private String name;
	private String inputDirectory;
	private String wildcard;
	private long responseTime = 1000;
	private String directoryProcessedFiles;

	private long recordNo = 0; // the current record number;
	private String inputFileName; // the name of the file currently in process
	private ISender sender;
	private Iterator recordIterator = null;

	public void afterMessageProcessed(
		PipeLineResult processResult,
		Object rawMessage,
		HashMap threadContext)
		throws ListenerException {
		String cid = (String) threadContext.get("cid");
		if (sender != null) {
			if (processResult.getState().equalsIgnoreCase("success")) {
				try {
					sender.sendMessage(
						(String) threadContext.get("cid"),
						processResult.getResult());
				} catch (Exception e) {
					throw new ListenerException(
						"error sending message with correlationId["
							+ cid
							+ " msg ["
							+ processResult.getResult()
							+ "]",
						e);
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
			log.warn(
				getName()
					+ " error retrieving canonical path of file ["
					+ file.getName()
					+ "]");
			fullFilePath = file.getName();
		}

		if (!dir.isDirectory()) {
			throw new ListenerException(
				getName()
					+ " error renaming directory: The directory ["
					+ directoryTo
					+ "] to move the file ["
					+ fullFilePath
					+ "] is not a directory!");
		}
		// Move file to new directory
		String newFileName = Misc.createSimpleUUID() + "-" + file.getName();

		int dotPosition = file.getName().lastIndexOf(".");
		if (dotPosition > 0)
			newFileName =
				file.getName().substring(0, dotPosition)
					+ "-"
					+ Misc.createSimpleUUID()
					+ file.getName().substring(
						dotPosition,
						file.getName().length());

		success = file.renameTo(new File(dir, newFileName));
		if (!success) {
			log.error(
				getName()
					+ " was unable to move file ["
					+ fullFilePath
					+ "] to ["
					+ directoryTo
					+ "]");
			throw new ListenerException(
				"unable to move file ["
					+ fullFilePath
					+ "] to ["
					+ directoryTo
					+ "]");
		} else
			log.info(
				getName()
					+ " moved file ["
					+ fullFilePath
					+ "] to ["
					+ directoryTo
					+ "]");

		String result = null;
		try {
			result = new File(dir, newFileName).getCanonicalPath();
		} catch (IOException e) {
			throw new ListenerException(
				"error retrieving canonical path of renamed file ["
					+ file.getName()
					+ "]",
				e);
		}
		return result;

	}
	
	public void close() throws ListenerException {
		try {
			if (sender != null)
				sender.close();
		} catch (SenderException e) {
			throw new ListenerException(
				"Error closing sender [" + sender.getName() + "]",
				e);
		}
	}
	public void closeThread(HashMap threadContext) throws ListenerException {
		// nothing special
	}
	
	/**
	 * Configure does some basic checks (directoryProcessedFiles is a directory,  inputDirectory is a directory, wildcard is filled etc.);
	 *
	 */
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getInputDirectory()))
			throw new ConfigurationException("no value specified for [inputDirectory]");
		if (StringUtils.isEmpty(getWildcard()))
			throw new ConfigurationException("no value specified for [wildcard]");
		if (StringUtils.isEmpty(getDirectoryProcessedFiles()))
			throw new ConfigurationException("no value specified for [directoryProcessedFiles]");
		File dir = new File(getDirectoryProcessedFiles());
		if (!dir.isDirectory()) {
			throw new ConfigurationException(
				"The value for [directoryProcessedFiles] :[ "
					+ getDirectoryProcessedFiles()
					+ "] is invalid. It is not a directory ");
		}
		File inp = new File(getInputDirectory());
		if (!inp.isDirectory()) {
			throw new ConfigurationException(
				"The value for [inputDirectory] :[ "
					+ getInputDirectory()
					+ "] is invalid. It is not a directory ");

		}
		try {
			if (sender != null)
				sender.configure();
		} catch (ConfigurationException e) {
			throw new ConfigurationException(
				"error opening sender [" + sender.getName() + "]",
				e);
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
	public String getIdFromRawMessage(Object rawMessage, HashMap threadContext)
		throws ListenerException {
		String correlationId = inputFileName + "-" + recordNo;
		threadContext.put("cid", correlationId);
		return correlationId;
	}
	/**
	 * Retrieves a single record from a file. If the file is empty or fully processed, it looks wether there
	 * is a new file to process and returns the first record.
	 */
	public synchronized Object getRawMessage(HashMap threadContext)
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
				" processing file ["
					+ inputFile.getName()
					+ "] size ["
					+ inputFile.length()
					+ "]");
			String fileContent = "";
			try {
				fullInputFileName = inputFile.getCanonicalPath();
				fileContent = Misc.fileToString(fullInputFileName, "\n");
				inputFileName = archiveFile(inputFile);

			} catch (IOException e) {
				throw new ListenerException(
					" got exception opening " + inputFile.getName(),
					e);
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
	
	/**
	 * Returns a string of the rawMessage
	 */
	public String getStringFromRawMessage(Object rawMessage, HashMap threadContext) throws ListenerException {
		return rawMessage.toString();
	}

	public void open() throws ListenerException {
		try {
			if (sender != null)
				sender.open();
		} catch (SenderException e) {
			throw new ListenerException(
				"error opening sender [" + sender.getName() + "]",
				e);
		}
		return;
	}

	public HashMap openThread() throws ListenerException {
		return null;
	}
	/**
	 * Parse a String to an Iterator with objects (records). This method
	 * currently uses the end-of-line character ("\n") as a seperator. 
	 * This method is easy to extend to satisfy your project needs.
	 */
	protected Iterator parseToRecords(String input) {
		StringTokenizer t = new StringTokenizer(input, "\n");
		ArrayList array = new ArrayList();
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
	
	public String toString() {
		String result = super.toString();
		ToStringBuilder ts = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
		ts.append("name", getName());
		ts.append("inputDirectory", getInputDirectory());
		ts.append("wildcard", getWildcard());
		result += ts.toString();
		return result;

	}
	
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	
	/**
	 * set the directory name to look in for files.
	 * @see #setWildcard(String)
	 */
	public void setInputDirectory(String inputDirectory) {
		this.inputDirectory = inputDirectory;
	}
	public String getInputDirectory() {
		return inputDirectory;
	}

	/**
	 * set the {@link nl.nn.adapterframework.util.WildCardFilter wildcard} to look for files in the specified directory, e.g. "*.inp"
	 */
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
	public void setDirectoryProcessedFiles(String directoryProcessedFiles) {
		this.directoryProcessedFiles = directoryProcessedFiles;
	}
	public String getDirectoryProcessedFiles() {
		return directoryProcessedFiles;
	}

	/**
	 * set the time to delay when no records are to be processed and this class has to look for the arrival of a new file
	 */
	public void setResponseTime(long responseTime) {
		this.responseTime = responseTime;
	}
	public long getResponseTime() {
		return responseTime;
	}

}
