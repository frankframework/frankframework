/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2021 WeAreFrank!

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
package nl.nn.adapterframework.pipes;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Assumes input to be the file name of a ZIP archive, and unzips it to a
 * directory and/or an XML message.
 * <br>
 * The output of each unzipped item is returned in XML as follows when
 * collectFileContents is false:
 * <pre>
 *  &lt;results count="num_of_items"&gt;
 *    &lt;result item="1"&gt;
 *      &lt;zipEntry&gt;name in ZIP archive of first item&lt;/zipEntry&gt;
 *      &lt;fileName&gt;filename of first item&lt;/fileName&gt;
 *    &lt;/result&gt;
 *    &lt;result item="2"&gt;
 *      &lt;zipEntry&gt;name in ZIP archive of second item&lt;/zipEntry&gt;
 *      &lt;fileName&gt;filename of second item&lt;/fileName&gt;
 *    &lt;/result&gt;
 *       ...
 *  &lt;/results&gt;
 * </pre>
 *
 * <br>
 * The output of each unzipped item is returned in XML as follows when
 * collectFileContents is true:
 * <pre>
 *  &lt;results count="num_of_items"&gt;
 *    &lt;result item="1"&gt;
 *      &lt;zipEntry&gt;name in ZIP archive of first item&lt;/zipEntry&gt;
 *      &lt;fileContent&gt;content of first item&lt;/fileContent&gt;
 *    &lt;/result&gt;
 *    &lt;result item="2"&gt;
 *      &lt;zipEntry&gt;name in ZIP archive of second item&lt;/zipEntry&gt;
 *      &lt;fileContent&gt;content of second item&lt;/fileContent&gt;
 *    &lt;/result&gt;
 *       ...
 *  &lt;/results&gt;
 * </pre>
 * <br>
 * By default, this pipe takes care
 * to produce unique file names, as follows. When the filename within
 * the archive is:
 * <pre>&lt;basename&gt; + "." + &lt;extension&gt;</pre>
 * then the extracted filename (path omitted) becomes
 * <pre>&lt;basename&gt; + &lt;unique number&gt; + "." + &lt;extension&gt;</pre>
 * <br>
 * 
 * @since   4.9
 * @author  Gerrit van Brakel
 */
public class UnzipPipe extends FixedForwardPipe {
	
	private String directory;
	private String directorySessionKey;
	private boolean deleteOnExit=true;
	private boolean collectResults=true;
	private boolean collectFileContents=false;
	private String collectFileContentsBase64Encoded;
	private boolean keepOriginalFileName=false;
	private boolean createSubdirectories=false;

	private File dir; // File representation of directory
	private List<String> base64Extensions;

	private boolean assumeDirectoryExists=false;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getDirectory())) {
			if (StringUtils.isEmpty(getDirectorySessionKey()) && !isCollectFileContents()) {
				throw new ConfigurationException("directory or directorySessionKey must be specified");
			}
		} else {
			dir = new File(getDirectory());
			if(!isAssumeDirectoryExists()) {
				if (!dir.exists()) {
					throw new ConfigurationException("directory ["+getDirectory()+"] does not exist");
				}
				if (!dir.isDirectory()) {
					throw new ConfigurationException("directory ["+getDirectory()+"] is not a directory");
				}
			}
		}
		if (StringUtils.isEmpty(getCollectFileContentsBase64Encoded())) {
			base64Extensions = new ArrayList<String>();
		} else {
			base64Extensions = Arrays.asList(getCollectFileContentsBase64Encoded().split(","));
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		InputStream in;
		if (!(message.asObject() instanceof String)) {
			try {
				in=message.asInputStream();
			} catch (IOException e) {
				throw new PipeRunException(this, getLogPrefix(session)+"cannot open stream", e);
			}
		} else {
			String filename=(String)message.asObject();
			try {
				in=new FileInputStream(filename);
			} catch (FileNotFoundException e) {
				throw new PipeRunException(this, "could not find file ["+filename+"]",e);
			}
		}

		File dir = this.dir;
		if (StringUtils.isEmpty(getDirectory())) {
			String directory = (String) session.get(getDirectorySessionKey());
			if (StringUtils.isEmpty(directory)) {
				if (!isCollectFileContents()) {
					throw new PipeRunException(this, "directorySessionKey is empty");
				}
			} else {
				dir = new File(directory);
				if (!dir.exists()) {
					if (!isCollectFileContents()) {
						throw new PipeRunException(this, "Directory ["+directory+"] does not exist");
					}
				} else if (!dir.isDirectory()) {
					throw new PipeRunException(this, "Directory ["+directory+"] is not a directory");
				}
			}
		}

		String entryResults = "";
		int count = 0;
		try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(in))) {
			ZipEntry ze;
			while ((ze=zis.getNextEntry())!=null) {
				if (ze.isDirectory()) {
					if (isCreateSubdirectories() && dir != null) {
						File tmpFile = new File(dir, ze.getName());
						if (!tmpFile.exists()) {
							if (tmpFile.mkdirs()) {
								log.debug(getLogPrefix(session)+"created directory ["+tmpFile.getPath()+"]");
							} else {
								log.warn(getLogPrefix(session)+"directory ["+tmpFile.getPath()+"] could not be created");
							}
						} else {
							log.debug(getLogPrefix(session)+"directory entry ["+tmpFile.getPath()+"] already exists");
						}
					} else {
						log.debug(getLogPrefix(session)+"skipping directory entry ["+ze.getName()+"]");
					}
				} else {
					String entryname=ze.getName();
					String basename=null;
					String extension=null;
					int dotPos=entryname.lastIndexOf('.');
					if (dotPos>=0) {
						extension=entryname.substring(dotPos);
						basename=entryname.substring(0,dotPos);
						log.debug(getLogPrefix(session)+"parsed filename ["+basename+"] extension ["+extension+"]");
					} else {
						basename=entryname;
					}
					InputStream inputStream = StreamUtil.dontClose(zis); 
					byte[] fileContentBytes = null;
					if (isCollectFileContents()) {
						fileContentBytes = Misc.streamToBytes(inputStream);
						inputStream = new ByteArrayInputStream(fileContentBytes);
					}
					File tmpFile = null;
					if (dir != null) {
						if (isKeepOriginalFileName()) {
							tmpFile = new File(dir, entryname);
							if (tmpFile.exists()) {
								throw new PipeRunException(this, "file [" + tmpFile.getAbsolutePath() + "] already exists"); 
							}
						} else {
							String filename = new File(basename).getName();
							String zipEntryPath = entryname.substring(0, ze.getName().indexOf(filename));
							String basePath = dir.getPath() + File.separator + zipEntryPath;
							if(filename.length() < 3) filename += ".tmp.";	//filename here is a prefix to create a unique filename and that prefix must be at least 3 chars long
							tmpFile = File.createTempFile(filename, extension, new File(basePath));
						}
						if (isDeleteOnExit()) {
							tmpFile.deleteOnExit();
						}
						if (isCreateSubdirectories()) {
							//extra check
							File tmpDir = tmpFile.getParentFile();
							if (!tmpDir.exists()) {
								if (tmpDir.mkdirs()) {
									log.debug(getLogPrefix(session)+"created directory ["+tmpDir.getPath()+"]");
								} else {
									log.warn(getLogPrefix(session)+"directory ["+tmpDir.getPath()+"] could not be created");
								}
							}
						}
						try (FileOutputStream fileOutputStream = new FileOutputStream(tmpFile)) {
							log.debug(getLogPrefix(session)+"writing ZipEntry ["+entryname+"] to file ["+tmpFile.getPath()+"]");
							count++;
							Misc.streamToStream(inputStream, fileOutputStream);
						}
					}
					if (isCollectResults()) {
						entryResults += "<result item=\"" + count + "\"><zipEntry>" + XmlUtils.encodeCharsAndReplaceNonValidXmlCharacters(entryname) + "</zipEntry>";
						if (dir != null) {
							entryResults += "<fileName>" + XmlUtils.encodeCharsAndReplaceNonValidXmlCharacters(tmpFile.getPath()) + "</fileName>";
						}
						if (isCollectFileContents()) {
							String fileContent;
							if (base64Extensions.contains(extension)) {
								fileContent = new String(Base64.encodeBase64Chunked(fileContentBytes));
							} else {
								fileContent = new String(fileContentBytes, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
								fileContent = XmlUtils.encodeCharsAndReplaceNonValidXmlCharacters(fileContent);
							}
							entryResults += "<fileContent>" + fileContent + "</fileContent>";
						}
						entryResults += "</result>";
					}
				}
			}
		} catch (IOException e) {
			throw new PipeRunException(this,"cannot unzip",e);
		}
		String result = "<results count=\"" + count + "\">" + entryResults + "</results>";
		return new PipeRunResult(getSuccessForward(),result);
	}

	@IbisDoc({"1", "Directory to extract the archive to", ""})
	public void setDirectory(String string) {
		directory = string;
	}
	public String getDirectory() {
		return directory;
	}

	@IbisDoc({"2", "Sessionkey with a directory value to extract the archive to", ""})
	public void setDirectorySessionKey(String directorySessionKey) {
		this.directorySessionKey = directorySessionKey;
	}
	public String getDirectorySessionKey() {
		return directorySessionKey;
	}

	@IbisDoc({"3", "If true, file is automatically deleted upon normal JVM termination", "true"})
	public void setDeleteOnExit(boolean b) {
		deleteOnExit = b;
	}
	public boolean isDeleteOnExit() {
		return deleteOnExit;
	}

	@IbisDoc({"4", "If set <code>false</code>, only a small summary (count of items in zip) is returned", "true"})
	public void setCollectResults(boolean b) {
		collectResults = b;
	}
	public boolean isCollectResults() {
		return collectResults;
	}

	@IbisDoc({"5", "If set <code>true</code>, the contents of the files in the zip are returned in the result xml message of this pipe. Please note this can consume a lot of memory for large files or a large number of files", "false"})
	public void setCollectFileContents(boolean b) {
		collectFileContents = b;
	}
	public boolean isCollectFileContents() {
		return collectFileContents;
	}

	@IbisDoc({"6", "Comma separated list of file extensions. Files with an extension which is part of this list will be base64 encoded. All other files are assumed to have UTF-8 when reading it from the zip and are added as escaped xml with non-unicode-characters being replaced by inverted question mark appended with #, the character number and ;", "false"})
	public void setCollectFileContentsBase64Encoded(String string) {
		collectFileContentsBase64Encoded = string;
	}
	public String getCollectFileContentsBase64Encoded() {
		return collectFileContentsBase64Encoded;
	}

	@IbisDoc({"7", "If set <code>false</code>, a suffix is added to the original filename to be sure it is unique", "false"})
	public void setKeepOriginalFileName(boolean b) {
		keepOriginalFileName = b;
	}
	public boolean isKeepOriginalFileName() {
		return keepOriginalFileName;
	}

	@IbisDoc({"8", "If set <code>true</code>, subdirectories in the zip file are created in the directory to extract the archive to", "false"})
	public void setCreateSubdirectories(boolean b) {
		createSubdirectories = b;
	}
	public boolean isCreateSubdirectories() {
		return createSubdirectories;
	}

	@IbisDoc({"9", "if set <code>true</code>, validation of directory is ignored", "false"})
	public void setAssumeDirectoryExists(boolean assumeDirectoryExists) {
		this.assumeDirectoryExists = assumeDirectoryExists;
	}
	public boolean isAssumeDirectoryExists() {
		return assumeDirectoryExists;
	}

	@Deprecated
	@ConfigurationWarning("the attribute 'checkDirectory' has been renamed to 'assumeDirectoryExists'")
	public void setCheckDirectory(boolean checkDirectory) {
		this.assumeDirectoryExists = checkDirectory;
	}
}
