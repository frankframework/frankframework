/*
   Copyright 2013, 2020 Nationale-Nederlanden

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

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.FileUtils;

/**
 * Pipe for moving files to another directory.
 *
 * 
 * @author  John Dekker
 * @author  Jaco de Groot (***@dynasol.nl)
 * @author  Gerrit van Brakel
 * 
 * @deprecated Please use LocalFileSystemPipe with action="move"
 */
@Deprecated
@ConfigurationWarning("Please replace with LocalFileSystemPipe and action=\"move\"")
public class MoveFilePipe extends FixedForwardPipe {

	private String directory;
	private String filename;
	private String wildcard;
	private String wildcardSessionKey;
	private String move2dir;
	private String move2file;
	protected String move2fileSessionKey;
	private int numberOfAttempts = 10;
	private long waitBeforeRetry = 1000;
	private int numberOfBackups = 5;
	private boolean overwrite = false;
	private boolean append = false;
	private boolean deleteEmptyDirectory = false;
	private boolean createDirectory = false;
	private String prefix;
	private String suffix;
	private boolean throwException = false;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (isAppend()) {
			setOverwrite(false);
		}
		if (StringUtils.isEmpty(getMove2dir())) {
			throw new ConfigurationException("Property [move2dir] is not set");
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		File srcFile=null;
		File dstFile=null;

		try {
			if (StringUtils.isEmpty(getWildcard()) && StringUtils.isEmpty(getWildcardSessionKey())) {
				if (StringUtils.isEmpty(getDirectory())) {
					if (StringUtils.isEmpty(getFilename())) {
						srcFile = new File(message.asString());
					} else {
						srcFile = new File(getFilename());
					}
				} else {
					if (StringUtils.isEmpty(getFilename())) {
						srcFile = new File(getDirectory(), message.asString());
					} else {
						srcFile = new File(getDirectory(), getFilename());
					}
				}
				if (StringUtils.isEmpty(getMove2file())) {
					if (StringUtils.isEmpty(getMove2fileSessionKey())) {
						dstFile = new File(getMove2dir(), retrieveDestinationChild(srcFile.getName()));
					} else {
						dstFile = new File(getMove2dir(), retrieveDestinationChild(session.getMessage(getMove2fileSessionKey()).asString()));
					}
				} else {
					dstFile = new File(getMove2dir(), retrieveDestinationChild(getMove2file()));
				}
				moveFile(session, srcFile, dstFile);
			} else {
				if (StringUtils.isEmpty(getDirectory())) {
					if (StringUtils.isEmpty(getFilename())) {
						srcFile = new File(message.asString());
					} else {
						srcFile = new File(getFilename());
					}
				} else {
					srcFile = new File(getDirectory());
				}
				String wc;
				if (StringUtils.isEmpty(getWildcardSessionKey())) {
					wc = getWildcard();
				} else {
					wc = session.getMessage(getWildcardSessionKey()).asString();
				}
				//WildCardFilter filter = new WildCardFilter(wc);
				//File[] srcFiles = srcFile.listFiles(filter);
				File[] srcFiles = FileUtils.getFiles(srcFile.getPath(), wc, null, -1);
				int count = (srcFiles == null ? 0 : srcFiles.length);
				if (count==0) {
					log.info(getLogPrefix(session) + "no files with wildcard [" + wc + "] found in directory [" + srcFile.getAbsolutePath() +"]");
				}
				for (int i = 0; i < count; i++) {
					dstFile = new File(getMove2dir(), retrieveDestinationChild(srcFiles[i].getName()));
					moveFile(session, srcFiles[i], dstFile);
				}
			}
		} catch (IOException e) {
			throw new PipeRunException(this, getLogPrefix(session)+"cannot open stream", e);
		}

		/* if parent source directory is empty, delete the directory */
		if (isDeleteEmptyDirectory()) {
			File srcDirectory;
			if (StringUtils.isEmpty(getWildcard()) && StringUtils.isEmpty(getWildcardSessionKey())) {
				srcDirectory = srcFile.getParentFile();
			} else {
				srcDirectory = srcFile.getAbsoluteFile();
			}
			log.debug("srcFile ["+srcFile.getPath()+"] srcDirectory ["+srcDirectory.getPath()+"]");
			if (srcDirectory.exists()) {
				if (srcDirectory.list().length==0) {
					boolean success = srcDirectory.delete();
					if(!success) {
						log.warn(getLogPrefix(session) + "could not delete directory [" + srcDirectory.getAbsolutePath() + "]");
					} else {
						log.info(getLogPrefix(session) + "deleted directory [" + srcDirectory.getAbsolutePath() + "]");
					}
				} else {
					log.info(getLogPrefix(session) + "directory [" + srcDirectory.getAbsolutePath() + "] is not empty");
				}
			} else {
				log.info(getLogPrefix(session) + "directory [" + srcDirectory.getAbsolutePath() + "] doesn't exist");
			}
		}

		return new PipeRunResult(getSuccessForward(), (dstFile==null?srcFile.getAbsolutePath():dstFile.getAbsolutePath()));
	}

	private String retrieveDestinationChild(String child) {
		String newChild;
		if (StringUtils.isNotEmpty(getPrefix())) {
			newChild = getPrefix() + child;
		} else {
			newChild =  child;
		}
		if (StringUtils.isNotEmpty(getSuffix())) {
			String baseName = FileUtils.getBaseName(newChild);
			newChild = baseName + getSuffix();
		}
		return newChild;
	}

	private void moveFile(PipeLineSession session, File srcFile, File dstFile) throws PipeRunException {
		try {
			if (!dstFile.getParentFile().exists()) {
				if (isCreateDirectory()) {
					if (dstFile.getParentFile().mkdirs()) {
						log.debug( getLogPrefix(session) + "created directory [" + dstFile.getParent() +"]");
					} else {
						log.warn( getLogPrefix(session) + "directory [" + dstFile.getParent() +"] could not be created");
					}
				} else {
					log.warn( getLogPrefix(session) + "directory [" + dstFile.getParent() +"] does not exists");
				}
			}

			if (isAppend()) {
				if (FileUtils.appendFile(srcFile,dstFile,getNumberOfAttempts(), getWaitBeforeRetry()) == null) {
					throw new PipeRunException(this, "Could not move file [" + srcFile.getAbsolutePath() + "] to file ["+dstFile.getAbsolutePath()+"]");
				} else {
					srcFile.delete();
					log.info(getLogPrefix(session)+"moved file ["+srcFile.getAbsolutePath()+"] to file ["+dstFile.getAbsolutePath()+"]");
				}
			} else {
				if (!isOverwrite() && getNumberOfBackups()==0) {
					if (dstFile.exists() && isThrowException()) {
						throw new PipeRunException(this, "Could not move file [" + srcFile.getAbsolutePath() + "] to file ["+dstFile.getAbsolutePath()+"] because it already exists");
					} else {
						dstFile = FileUtils.getFreeFile(dstFile);
					}
				}
				if (FileUtils.moveFile(srcFile, dstFile, isOverwrite(), getNumberOfBackups(), getNumberOfAttempts(), getWaitBeforeRetry()) == null) {
					throw new PipeRunException(this, "Could not move file [" + srcFile.getAbsolutePath() + "] to file ["+dstFile.getAbsolutePath()+"]");
				} else {
					log.info(getLogPrefix(session)+"moved file ["+srcFile.getAbsolutePath()+"] to file ["+dstFile.getAbsolutePath()+"]");
				}
			}
		} catch(Exception e) {
			throw new PipeRunException(this, "Error while moving file [" + srcFile.getAbsolutePath() + "] to file ["+dstFile.getAbsolutePath()+"]", e);
		}
	}

	@IbisDoc({"name of the file to move (if not specified, the input for this pipe is assumed to be the name of the file", ""})
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public String getFilename() {
		return filename;
	}

	@IbisDoc({"base directory where files are moved from", ""})
	public void setDirectory(String string) {
		directory = string;
	}
	public String getDirectory() {
		return directory;
	}

	@IbisDoc({"session key that contains the name of the filter to use (only used if wildcard is not set)", ""})
	public void setWildcardSessionKey(String string) {
		wildcardSessionKey = string;
	}
	public String getWildcardSessionKey() {
		return wildcardSessionKey;
	}

	@IbisDoc({"filter of files to replace", ""})
	public void setWildcard(String string) {
		wildcard = string;
	}
	public String getWildcard() {
		return wildcard;
	}

	@IbisDoc({"destination directory", ""})
	public void setMove2dir(String string) {
		move2dir = string;
	}
	public String getMove2dir() {
		return move2dir;
	}

	@IbisDoc({"name of the destination file (if not specified, the name of the file to move is taken)", ""})
	public void setMove2file(String string) {
		move2file = string;
	}
	public String getMove2file() {
		return move2file;
	}

	@IbisDoc({"session key that contains the name of the file to use (only used if move2file is not set)", ""})
	public void setMove2fileSessionKey(String move2fileSessionKey) {
		this.move2fileSessionKey = move2fileSessionKey;
	}
	public String getMove2fileSessionKey() {
		return move2fileSessionKey;
	}

	@IbisDoc({"maximum number of attempts before throwing an exception", "10"})
	public void setNumberOfAttempts(int i) {
		numberOfAttempts = i;
	}
	public int getNumberOfAttempts() {
		return numberOfAttempts;
	}

	@IbisDoc({"Time <i>in milliseconds</i> between attempts", "1000"})
	public void setWaitBeforeRetry(long l) {
		waitBeforeRetry = l;
	}
	public long getWaitBeforeRetry() {
		return waitBeforeRetry;
	}

	@IbisDoc({"number of copies held of a file with the same name. backup files have a dot and a number suffixed to their name. if set to 0, no backups will be kept.", "5"})
	public void setNumberOfBackups(int i) {
		numberOfBackups = i;
	}
	public int getNumberOfBackups() {
		return numberOfBackups;
	}

	@IbisDoc({"when set <code>true</code>, the destination file will be deleted if it already exists. when set <code>false</code> and <code>numberofbackups</code> set to 0, a counter is added to the destination filename ('basename_###.ext')", "false"})
	public void setOverwrite(boolean b) {
		overwrite = b;
	}
	public boolean isOverwrite() {
		return overwrite;
	}

	@IbisDoc({" when set <code>true</code> and the destination file already exists, the content of the file to move is written to the end of the destination file. this implies <code>overwrite=false</code>", "false"})
	public void setAppend(boolean b) {
		append = b;
	}
	public boolean isAppend() {
		return append;
	}

	@IbisDoc({"when set to <code>true</code>, the directory from which a file is moved is deleted when it contains no other files", "false"})
	public void setDeleteEmptyDirectory(boolean b) {
		deleteEmptyDirectory = b;
	}
	public boolean isDeleteEmptyDirectory() {
		return deleteEmptyDirectory;
	}

	@IbisDoc({"when set to <code>true</code>, the directory to move to is created if it does not exist", "false"})
	public void setCreateDirectory(boolean b) {
		createDirectory = b;
	}
	public boolean isCreateDirectory() {
		return createDirectory;
	}

	@IbisDoc({"string which is inserted at the start of the destination file", ""})
	public void setPrefix(String string) {
		prefix = string;
	}
	public String getPrefix() {
		return prefix;
	}

	@IbisDoc({"string which is inserted at the end of the destination file (and replaces the extension if present)", ""})
	public void setSuffix(String string) {
		suffix = string;
	}
	public String getSuffix() {
		return suffix;
	}

	@IbisDoc({"when <code>true</code>, <code>numberofbackups</code> is set to 0 and the destination file already exists a piperunexception is thrown (instead of adding a counter to the destination filename)", "false"})
	public void setThrowException(boolean b) {
		throwException = b;
	}
	public boolean isThrowException() {
		return throwException;
	}
}