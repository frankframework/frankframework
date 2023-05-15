/*
   Copyright 2013 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
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

	private @Getter String directory;
	private @Getter String directorySessionKey;
	private @Getter @Deprecated boolean deleteOnExit=true;
	private @Getter boolean collectResults=true;
	private @Getter boolean collectFileContents=false;
	private @Getter String collectFileContentsBase64Encoded;
	private @Getter boolean keepOriginalFileName=false;
	private @Getter boolean keepOriginalFilePath=false;
	private @Getter boolean assumeDirectoryExists=false;
	private @Getter boolean processFile=false;

	private File dir; // File representation of directory
	private List<String> base64Extensions;



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

	protected InputStream getInputStream(Message message, PipeLineSession session) throws PipeRunException {
		try {
			if (isProcessFile()) {
				String filename= message.asString();
				try {
					return new FileInputStream(filename);
				} catch (FileNotFoundException e) {
					throw new PipeRunException(this, "could not find file ["+filename+"]",e);
				}
			} else if (!message.isBinary()) {
				log.warn("expected binary message, encountered character data. Do you need to set processFile=\"true\"?");
			}
			return message.asInputStream();
		} catch (IOException e) {
			throw new PipeRunException(this, getLogPrefix(session)+"cannot open stream", e);
		}

	}
	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		try (InputStream in = getInputStream(message, session)) {
			File targetDirectory = this.dir;
			if (StringUtils.isEmpty(getDirectory())) {
				String directory;
				try {
					directory = session.getMessage(getDirectorySessionKey()).asString();
				} catch (IOException e) {
					throw new PipeRunException(this, "cannot resolve ["+getDirectorySessionKey()+"] session key", e);
				}
				if (StringUtils.isEmpty(directory)) {
					if (!isCollectFileContents()) {
						throw new PipeRunException(this, "directorySessionKey is empty");
					}
				} else {
					targetDirectory = new File(directory);
					if (!targetDirectory.exists()) {
						if (!isCollectFileContents()) {
							throw new PipeRunException(this, "Directory ["+directory+"] does not exist");
						}
					} else if (!targetDirectory.isDirectory()) {
						throw new PipeRunException(this, "Directory ["+directory+"] is not a directory");
					}
				}
			}

			String entryResults = "";
			int count = 0;
			try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(in))) {
				ZipEntry ze;
				while ((ze=zis.getNextEntry())!=null) {
					String entryname = ze.getName();
					if(isKeepOriginalFilePath() && targetDirectory != null) {
						File tmpFile = new File(targetDirectory, entryname);
						tmpFile = tmpFile.isDirectory() ? tmpFile : tmpFile.getParentFile();
						if (!tmpFile.exists()) {
							if (tmpFile.mkdirs()) {	// Create directories included in the path
								log.debug(getLogPrefix(session)+"created directory ["+tmpFile.getPath()+"]");
							} else {
								log.warn(getLogPrefix(session)+"directory ["+tmpFile.getPath()+"] could not be created");
							}
						} else {
							log.debug(getLogPrefix(session)+"directory entry ["+tmpFile.getPath()+"] already exists");
						}
					}

					if(!ze.isDirectory()) {
						// split the entry name and the extension
						String entryNameWithoutExtension=null;
						String extension=null;
						int dotPos=entryname.lastIndexOf('.');
						if (dotPos>=0) {
							extension=entryname.substring(dotPos);
							entryNameWithoutExtension=entryname.substring(0,dotPos);
							log.debug(getLogPrefix(session)+"parsed filename ["+entryNameWithoutExtension+"] extension ["+extension+"]");
						} else {
							entryNameWithoutExtension=entryname;
						}
						InputStream inputStream = StreamUtil.dontClose(zis);
						byte[] fileContentBytes = null;
						if (isCollectFileContents()) {
							fileContentBytes = Misc.streamToBytes(inputStream);
							inputStream = new ByteArrayInputStream(fileContentBytes);
						}

						File tmpFile = null;
						if (targetDirectory != null) {
							if (isKeepOriginalFileName()) {
								String filename = isKeepOriginalFilePath() ? entryname : new File(entryname).getName();
								tmpFile = new File(targetDirectory, filename);
								if (tmpFile.exists()) {
									throw new PipeRunException(this, "file [" + tmpFile.getAbsolutePath() + "] already exists");
								}
							} else {
								if (isKeepOriginalFilePath()) {
									String filename = new File(entryNameWithoutExtension).getName();
									String zipEntryPath = entryname.substring(0, ze.getName().indexOf(filename));
									if(filename.length() < 3) filename += ".tmp.";	//filename here is a prefix to create a unique filename and that prefix must be at least 3 chars long
									tmpFile = File.createTempFile(filename, extension, new File(targetDirectory, zipEntryPath));
								} else {
									if(entryNameWithoutExtension.length() < 3) entryNameWithoutExtension += ".tmp.";
									tmpFile = File.createTempFile(entryNameWithoutExtension, extension, targetDirectory);
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
							if (targetDirectory != null) {
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
			}
			String result = "<results count=\"" + count + "\">" + entryResults + "</results>";
			return new PipeRunResult(getSuccessForward(),result);
		} catch (IOException e) {
			throw new PipeRunException(this,"cannot unzip",e);
		}
	}

	@IbisDoc({"Directory to extract the archive to", ""})
	public void setDirectory(String string) {
		directory = string;
	}

	@IbisDoc({"Sessionkey with a directory value to extract the archive to", ""})
	public void setDirectorySessionKey(String directorySessionKey) {
		this.directorySessionKey = directorySessionKey;
	}

	@IbisDoc({"This flag is no longer supported as it was leaking server memory. Temporary files should be removed by other means.", "true"})
	@Deprecated
	@ConfigurationWarning("This flag is no longer supported as it leaks server memory. Temporary files should be removed by other means.")
	public void setDeleteOnExit(boolean b) {
		deleteOnExit = b;
	}

	@IbisDoc({"If set <code>false</code>, only a small summary (count of items in zip) is returned", "true"})
	public void setCollectResults(boolean b) {
		collectResults = b;
	}

	@IbisDoc({"If set <code>true</code>, the contents of the files in the zip are returned in the result xml message of this pipe. Please note this can consume a lot of memory for large files or a large number of files", "false"})
	public void setCollectFileContents(boolean b) {
		collectFileContents = b;
	}

	@IbisDoc({"Comma separated list of file extensions. Files with an extension which is part of this list will be base64 encoded. All other files are assumed to have UTF-8 when reading it from the zip and are added as escaped xml with non-unicode-characters being replaced by inverted question mark appended with #, the character number and ;", "false"})
	public void setCollectFileContentsBase64Encoded(String string) {
		collectFileContentsBase64Encoded = string;
	}

	@IbisDoc({"If set <code>false</code>, a suffix is added to the original filename to be sure it is unique", "false"})
	public void setKeepOriginalFileName(boolean b) {
		keepOriginalFileName = b;
	}

	@Deprecated
	@ConfigurationWarning("the attribute 'createSubDirectories' has been renamed to 'keepOriginalFilePath'")
	public void setCreateSubDirectories(boolean b) {
		setKeepOriginalFilePath(b);
	}

	@IbisDoc({"If set <code>true</code>, the path of the zip entry will be preserved. Otherwise, the zip entries will be extracted to the root folder", "false"})
	public void setKeepOriginalFilePath(boolean b) {
		keepOriginalFilePath = b;
	}

	@IbisDoc({"If set <code>true</code>, validation of directory is ignored", "false"})
	public void setAssumeDirectoryExists(boolean assumeDirectoryExists) {
		this.assumeDirectoryExists = assumeDirectoryExists;
	}

	@Deprecated
	@ConfigurationWarning("the attribute 'checkDirectory' has been renamed to 'assumeDirectoryExists'")
	public void setCheckDirectory(boolean checkDirectory) {
		this.assumeDirectoryExists = checkDirectory;
	}

	@IbisDoc({"If set <code>true</code>, the input is assumed to be the name of a file to be processed. Otherwise, the input itself is used.", "false"})
	@Deprecated
	@ConfigurationWarning("Please add a LocalFileSystemPipe with action=read in front of this pipe instead")
	public void setProcessFile(boolean b) {
		processFile = b;
	}
}
