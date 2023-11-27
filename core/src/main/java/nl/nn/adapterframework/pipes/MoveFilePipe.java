/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2023 WeAreFrank!

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
						dstFile = new File(getMove2dir(), retrieveDestinationChild(session.getString(getMove2fileSessionKey())));
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
					wc = session.getString(getWildcardSessionKey());
				}
				//WildCardFilter filter = new WildCardFilter(wc);
				//File[] srcFiles = srcFile.listFiles(filter);
				File[] srcFiles = FileUtils.getFiles(srcFile.getPath(), wc, null, -1);
				int count = srcFiles.length;
				if (count==0) {
					log.info("no files with wildcard [{}] found in directory [{}]", wc, srcFile.getAbsolutePath());
				}
				for (File file : srcFiles) {
					dstFile = new File(getMove2dir(), retrieveDestinationChild(file.getName()));
					moveFile(session, file, dstFile);
				}
			}
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot open stream", e);
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
						log.warn("could not delete directory [{}]", srcDirectory.getAbsolutePath());
					} else {
						log.info("deleted directory [{}]", srcDirectory.getAbsolutePath());
					}
				} else {
					log.info("directory [{}] is not empty", srcDirectory.getAbsolutePath());
				}
			} else {
				log.info("directory [{}] doesn't exist", srcDirectory.getAbsolutePath());
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
						log.debug( "created directory [{}]", dstFile.getParent());
					} else {
						log.warn( "directory [{}] could not be created", dstFile.getParent());
					}
				} else {
					log.warn( "directory [{}] does not exists", dstFile.getParent());
				}
			}

			if (isAppend()) {
				if (FileUtils.appendFile(srcFile,dstFile,getNumberOfAttempts(), getWaitBeforeRetry()) == null) {
					throw new PipeRunException(this, "Could not move file [" + srcFile.getAbsolutePath() + "] to file ["+dstFile.getAbsolutePath()+"]");
				}
				srcFile.delete();
				log.info("moved file [{}] to file [{}]", srcFile.getAbsolutePath(), dstFile.getAbsolutePath());
			} else {
				if (!isOverwrite() && getNumberOfBackups()==0) {
					if (dstFile.exists() && isThrowException()) {
						throw new PipeRunException(this, "Could not move file [" + srcFile.getAbsolutePath() + "] to file ["+dstFile.getAbsolutePath()+"] because it already exists");
					}
					dstFile = FileUtils.getFreeFile(dstFile);
				}
				if (FileUtils.moveFile(srcFile, dstFile, isOverwrite(), getNumberOfBackups(), getNumberOfAttempts(), getWaitBeforeRetry()) == null) {
					throw new PipeRunException(this, "Could not move file [" + srcFile.getAbsolutePath() + "] to file ["+dstFile.getAbsolutePath()+"]");
				}
				log.info("moved file [{}] to file [{}]", srcFile.getAbsolutePath(), dstFile.getAbsolutePath());
			}
		} catch(Exception e) {
			throw new PipeRunException(this, "Error while moving file [" + srcFile.getAbsolutePath() + "] to file ["+dstFile.getAbsolutePath()+"]", e);
		}
	}

	/** name of the file to move (if not specified, the input for this pipe is assumed to be the name of the file */
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public String getFilename() {
		return filename;
	}

	/** base directory where files are moved from */
	public void setDirectory(String string) {
		directory = string;
	}
	public String getDirectory() {
		return directory;
	}

	/** session key that contains the name of the filter to use (only used if wildcard is not set) */
	public void setWildcardSessionKey(String string) {
		wildcardSessionKey = string;
	}
	public String getWildcardSessionKey() {
		return wildcardSessionKey;
	}

	/** filter of files to replace */
	public void setWildcard(String string) {
		wildcard = string;
	}
	public String getWildcard() {
		return wildcard;
	}

	/** destination directory */
	public void setMove2dir(String string) {
		move2dir = string;
	}
	public String getMove2dir() {
		return move2dir;
	}

	/** name of the destination file (if not specified, the name of the file to move is taken) */
	public void setMove2file(String string) {
		move2file = string;
	}
	public String getMove2file() {
		return move2file;
	}

	/** session key that contains the name of the file to use (only used if move2file is not set) */
	public void setMove2fileSessionKey(String move2fileSessionKey) {
		this.move2fileSessionKey = move2fileSessionKey;
	}
	public String getMove2fileSessionKey() {
		return move2fileSessionKey;
	}

	/**
	 * maximum number of attempts before throwing an exception
	 * @ff.default 10
	 */
	public void setNumberOfAttempts(int i) {
		numberOfAttempts = i;
	}
	public int getNumberOfAttempts() {
		return numberOfAttempts;
	}

	/**
	 * Time <i>in milliseconds</i> between attempts
	 * @ff.default 1000
	 */
	public void setWaitBeforeRetry(long l) {
		waitBeforeRetry = l;
	}
	public long getWaitBeforeRetry() {
		return waitBeforeRetry;
	}

	/**
	 * number of copies held of a file with the same name. backup files have a dot and a number suffixed to their name. if set to 0, no backups will be kept.
	 * @ff.default 5
	 */
	public void setNumberOfBackups(int i) {
		numberOfBackups = i;
	}
	public int getNumberOfBackups() {
		return numberOfBackups;
	}

	/**
	 * when set <code>true</code>, the destination file will be deleted if it already exists. when set <code>false</code> and <code>numberofbackups</code> set to 0, a counter is added to the destination filename ('basename_###.ext')
	 * @ff.default false
	 */
	public void setOverwrite(boolean b) {
		overwrite = b;
	}
	public boolean isOverwrite() {
		return overwrite;
	}

	/**
	 *  when set <code>true</code> and the destination file already exists, the content of the file to move is written to the end of the destination file. this implies <code>overwrite=false</code>
	 * @ff.default false
	 */
	public void setAppend(boolean b) {
		append = b;
	}
	public boolean isAppend() {
		return append;
	}

	/**
	 * when set to <code>true</code>, the directory from which a file is moved is deleted when it contains no other files
	 * @ff.default false
	 */
	public void setDeleteEmptyDirectory(boolean b) {
		deleteEmptyDirectory = b;
	}
	public boolean isDeleteEmptyDirectory() {
		return deleteEmptyDirectory;
	}

	/**
	 * when set to <code>true</code>, the directory to move to is created if it does not exist
	 * @ff.default false
	 */
	public void setCreateDirectory(boolean b) {
		createDirectory = b;
	}
	public boolean isCreateDirectory() {
		return createDirectory;
	}

	/** string which is inserted at the start of the destination file */
	public void setPrefix(String string) {
		prefix = string;
	}
	public String getPrefix() {
		return prefix;
	}

	/** string which is inserted at the end of the destination file (and replaces the extension if present) */
	public void setSuffix(String string) {
		suffix = string;
	}
	public String getSuffix() {
		return suffix;
	}

	/**
	 * when <code>true</code>, <code>numberofbackups</code> is set to 0 and the destination file already exists a piperunexception is thrown (instead of adding a counter to the destination filename)
	 * @ff.default false
	 */
	public void setThrowException(boolean b) {
		throwException = b;
	}
	public boolean isThrowException() {
		return throwException;
	}
}
