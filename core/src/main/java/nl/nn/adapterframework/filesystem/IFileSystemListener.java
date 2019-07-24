package nl.nn.adapterframework.filesystem;

import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.doc.IbisDoc;

/**
 * temporary interface, to align testing of older listener classes with the new FileSystemListener-based classes.
 */
public interface IFileSystemListener<F> extends IPullingListener<F> {

	@Override
	@IbisDoc({"name of the listener", ""})
	public void setName(String name);

//	/**
//	 * @Deprecated replaced by inProcessFolder
//	 */
//	public void setInputDirectory(String inputDirectory) {
//		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
//		String msg = ClassUtils.nameOf(this) +"["+getName()+"]: attribute 'inputDirectory' has been replaced by 'inputFolder'";
//		configWarnings.add(log, msg);
//		setInputFolder(inputDirectory);
//	}

	@IbisDoc({"folder that is scanned for files. When not set, the root is scanned", ""})
	public void setInputFolder(String inputFolder);

//	/**
//	 * set the {@link nl.nn.adapterframework.util.WildCardFilter wildcard}  to look for files in the specifiek directory, e.g. "*.inp"
//	 */
//	@IbisDoc({"filter of files to look for in inputdirectory", ""})
//	public void setWildcard(String wildcard) {
//		this.wildcard = wildcard;
//	}
//	/**
//	* get the {@link nl.nn.adapterframework.util.WildCardFilter wildcard}  to look for files in the specifiek directory, e.g. "*.inp"
//	*/
//	public String getWildcard() {
//		return wildcard;
//	}
//
//	@IbisDoc({"filter of files to be excluded when looking in inputdirectory", ""})
//	public void setExcludeWildcard(String excludeWildcard) {
//		this.excludeWildcard = excludeWildcard;
//	}
//
//	public String getExcludeWildcard() {
//		return excludeWildcard;
//	}

//	@IbisDoc({"when set a list of files in xml format (&lt;files&gt;&lt;file&gt;/file/name&lt;/file&gt;&lt;file&gt;/another/file/name&lt;/file&gt;&lt;/files&gt;) is passed to the pipleline instead of 1 file name when the specified amount of files is present in the input directory. when set to -1 the list of files is passed to the pipleline whenever one of more files are present.", ""})
//	public void setFileList(Integer fileList) {
//		this.fileList = fileList;
//	}
//	public Integer getFileList() {
//		return fileList;
//	}
//	
//	@IbisDoc({"when set along with filelist a list of files is passed to the pipleline when the specified amount of ms has passed since the first file for a new list of files was found even if the amount of files specified by filelist isn't present in the input directory yet", ""})
//	public void setFileListForcedAfter(Long fileListForcedAfter) {
//		this.fileListForcedAfter = fileListForcedAfter;
//	}
//	public Long getFileListForcedAfter() {
//		return fileListForcedAfter;
//	}

//	/**
//	 * @Deprecated replaced by inProcessFolder
//	 */
//	public void setOutputDirectory(String outputDirectory) {
//		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
//		String msg = ClassUtils.nameOf(this) +"["+getName()+"]: attribute 'outputDirectory' has been replaced by 'inProcessFolder'";
//		configWarnings.add(log, msg);
//		setInProcessFolder(outputDirectory);
//	}

	@IbisDoc({"folder where files are stored <i>while</i> being processed", ""})
	public void setInProcessFolder(String inProcessFolder);


//	@IbisDoc({"pattern for the name using the messageformat.format method. params: 0=inputfilename, 1=inputfile extension, 2=unique uuid, 3=current date", ""})
//	public void setOutputFilenamePattern(String string) {
//		outputFilenamePattern = string;
//	}
//	public String getOutputFilenamePattern() {
//		return outputFilenamePattern;
//	}



	@IbisDoc({"minimal age of file in milliseconds, to avoid receiving a file while it is still being written", "1000 [ms]"})
	public void setMinStableTime(long minStableTime);


//	@IbisDoc({"pass the filename without the <code>outputdirectory</code> to the pipeline", "false"})
//	public void setPassWithoutDirectory(boolean b) {
//		passWithoutDirectory = b;
//	}
//
//	public boolean isPassWithoutDirectory() {
//		return passWithoutDirectory;
//	}

	@IbisDoc({"when set to <code>true</code>, the directory to look for files is created if it does not exist", "false"})
	public void setCreateInputDirectory(boolean b);
//
//	public boolean isCreateInputDirectory();
	
//	/**
//	 * @Deprecated replaced by processedFolder
//	 */
//	public void setProcessedDirectory(String processedDirectory) {
//		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
//		String msg = ClassUtils.nameOf(this) +"["+getName()+"]: attribute 'processedDirectory' has been replaced by 'processedFolder'";
//		configWarnings.add(log, msg);
//		setProcessedFolder(processedDirectory);
//	}

	@IbisDoc({"folder where files are stored <i>after</i> being processed", ""})
	public void setProcessedFolder(String processedFolder);
	
//	@IbisDoc({"number of copies held of a file with the same name. backup files have a dot and a number suffixed to their name. if set to 0, no backups will be kept.", "5"})
//	public void setNumberOfBackups(int i) {
//		numberOfBackups = i;
//	}
//	public int getNumberOfBackups() {
//		return numberOfBackups;
//	}

//	@IbisDoc({"when set <code>true</code>, the destination file will be deleted if it already exists", "false"})
//	public void setOverwrite(boolean overwrite);
//
	@IbisDoc({"when set <code>true</code>, the file processed will deleted after being processed, and not stored", "false"})
	public void setDelete(boolean b);

	@IbisDoc({"when <code>true</code>, the file modification time is used in addition to the filename to determine if a file has been seen before", "false"})
	public void setFileTimeSensitive(boolean b);

	@IbisDoc({"determines the contents of the message that is sent to the pipeline. Can be 'name', for the filename, 'contents' for the contents of the file. For any other value, the attributes of the file are searched and used", "name"})
	public void setMessageType(String messageType);

//	public boolean isCreateFolders();

}
