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
package nl.nn.adapterframework.pipes;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;

/**
 * Assumes input to be a ZIP archive, and unzips it to a directory.
 *
 * <br>
 * The output of each unzipped item is returned in XML as follows:
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
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.FixedResult</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(java.lang.Object, nl.nn.adapterframework.core.IPipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td><td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setDirectory(String) directory}</td> <td>directory to extract the archive to</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDirectorySessionKey(String) directorySessionKey}</td> <td>SessionKey with a directory value to extract the archive to</td><td>&nbsp;</td></tr> 
 * <tr><td>{@link #setDeleteOnExit(boolean) deleteOnExit}</td><td>when true, file is automatically deleted upon normal JVM termination</td><td>true</td></tr>
 * <tr><td>{@link #setCollectResults(boolean) collectResults}</td><td>if set <code>false</code>, only a small summary is returned</td><td>true</td></tr>
 * <tr><td>{@link #setKeepOriginalFileName(boolean) keepOriginalFileName}</td><td>if set <code>false</code>, a suffix is added to the original filename to be sure it is unique</td><td>false</td></tr>
 * <tr><td>{@link #setCreateSubdirectories(boolean) createSubdirectories}</td><td>if set <code>true</code>, subdirectories in the zip file are supported</td><td>false</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * 
 * @since   4.9
 * @author  Gerrit van Brakel
 */
public class UnzipPipe extends FixedForwardPipe {
	
	private String directory;
	private String directorySessionKey;
	private boolean deleteOnExit=true;
	private boolean collectResults=true;
	private boolean keepOriginalFileName=false;
	private boolean createSubdirectories=false;

	private File dir; // File representation of directory


	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getDirectory())) {
			if (StringUtils.isEmpty(getDirectorySessionKey())) {
				throw new ConfigurationException(getLogPrefix(null)+"directory or directorySessionKey must be specified");
			}
		}
		else {
			dir = new File(getDirectory());
			if (!dir.exists()) {
				throw new ConfigurationException(getLogPrefix(null)+"directory ["+getDirectory()+"] does not exist");
			}
			if (!dir.isDirectory()) {
				throw new ConfigurationException(getLogPrefix(null)+"directory ["+getDirectory()+"] is not a directory");
			}
		}
	}

	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		InputStream in;
		if (input instanceof InputStream) {
			in=(InputStream)input;
		} else {
			String filename=(String)input;
			try {
				in=new FileInputStream(filename);
			} catch (FileNotFoundException e) {
				throw new PipeRunException(this, "could not find file ["+filename+"]",e);
			}
		}

		if (StringUtils.isEmpty(getDirectory())) {
			String directory = (String) session.get(getDirectorySessionKey());
			if(StringUtils.isEmpty(directory))
				throw new PipeRunException(this, "directorySessionKey is empty");

			dir = new File(directory);
			if (!dir.exists()) {
				throw new PipeRunException(this, "directorySessionKey ["+directory+"] does not exist");
			}
			if (!dir.isDirectory()) {
				throw new PipeRunException(this, "directorySessionKey ["+directory+"] is not a directory");
			}
		}

		String entryResults = "";
		int count = 0;
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(in));
		try {
			ZipEntry ze;
			while ((ze=zis.getNextEntry())!=null) {
				if (ze.isDirectory()) {
					if (isCreateSubdirectories()) {
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
						log.warn(getLogPrefix(session)+"skipping directory entry ["+ze.getName()+"]");
					}
				} else {
					String filename=ze.getName();
					String basename=null;
					String extension=null;
					int dotPos=filename.indexOf('.');
					if (dotPos>=0) {
						extension=filename.substring(dotPos);
						basename=filename.substring(0,dotPos);
						log.debug(getLogPrefix(session)+"parsed filename ["+basename+"] extension ["+extension+"]");
					} else {
						basename=filename;
					}
					File tmpFile;
					if (isKeepOriginalFileName()) {
						tmpFile = new File(dir, filename);
						if (tmpFile.exists()) {
							throw new PipeRunException(this, "file [" + tmpFile.getAbsolutePath() + "] already exists"); 
						}
					} else {
						tmpFile = File.createTempFile(basename, extension, dir);
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
					FileOutputStream fos = new FileOutputStream(tmpFile);
					log.debug(getLogPrefix(session)+"writing ZipEntry ["+ze.getName()+"] to file ["+tmpFile.getPath()+"]");
					count++;
					Misc.streamToStream(zis,fos,false);
					fos.close();
					if (isCollectResults()) {
						entryResults += "<result item=\"" + count + "\"><zipEntry>" + XmlUtils.encodeCdataString(ze.getName()) + "</zipEntry><fileName>" + XmlUtils.encodeCdataString(tmpFile.getPath()) + "</fileName></result>";
					}
				}
			}
		} catch (IOException e) {
			throw new PipeRunException(this,"cannot unzip",e);
		} finally {
			try {
				zis.close();
			} catch (IOException e1) {
				log.warn(getLogPrefix(session)+"exception closing zip",e1);
			}
		}
		String result = "<results count=\"" + count + "\">" + entryResults + "</results>";
		return new PipeRunResult(getForward(),result);
	}

	public void setDirectory(String string) {
		directory = string;
	}
	public String getDirectory() {
		return directory;
	}
	
	public void setDirectorySessionKey(String directorySessionKey) {
		this.directorySessionKey = directorySessionKey;
	}
	public String getDirectorySessionKey() {
		return directorySessionKey;
	}
	
	public void setDeleteOnExit(boolean b) {
		deleteOnExit = b;
	}
	public boolean isDeleteOnExit() {
		return deleteOnExit;
	}

	public void setCollectResults(boolean b) {
		collectResults = b;
	}
	public boolean isCollectResults() {
		return collectResults;
	}

	public void setKeepOriginalFileName(boolean b) {
		keepOriginalFileName = b;
	}
	public boolean isKeepOriginalFileName() {
		return keepOriginalFileName;
	}

	public void setCreateSubdirectories(boolean b) {
		createSubdirectories = b;
	}
	public boolean isCreateSubdirectories() {
		return createSubdirectories;
	}
}
