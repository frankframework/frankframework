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
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.FileUtils;

/**
 * Pipe for deleting files.
 *
 *
 * @author John Dekker
 * @since  4.2
 */
public class CleanupOldFilesPipe extends FixedForwardPipe {

	private String filePattern;
	private String filePatternSessionKey;
	private boolean subdirectories=false;
	private long lastModifiedDelta=0;
	private boolean deleteEmptySubdirectories=false;
	private String wildcard;
	private String excludeWildcard;
	private long minStableTime = 1000;

	private _FileFilter fileFilter = new _FileFilter();
	private _DirFilter dirFilter = new _DirFilter();

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		try {
			String filename;
			if (StringUtils.isNotEmpty(getFilePattern())) {
				filename = FileUtils.getFilename(getParameterList(), session, "", getFilePattern());
			} else {
				if (StringUtils.isNotEmpty(getFilePatternSessionKey())) {
					filename = FileUtils.getFilename(getParameterList(), session, "", session.getString(getFilePatternSessionKey()));
				} else {
					if (StringUtils.isEmpty(message.asString())) {
						throw new PipeRunException(this, "input empty, but should contain filename to delete");
					}
					File in = new File(message.asString());
					filename = in.getName();
				}
			}

			List<File> delFiles = getFilesForDeletion(filename);
			if (delFiles != null && !delFiles.isEmpty()) {
				for (Iterator<File> fileIt = delFiles.iterator(); fileIt.hasNext();) {
					File file = fileIt.next();
					if (file.delete()) {
						log.info("deleted file [{}]", file::getAbsolutePath);
					} else {
						log.warn("could not delete file [{}]", file::getAbsolutePath);
					}
				}
			} else {
				log.info("no files match pattern [{}]", filename);
			}

			if (isDeleteEmptySubdirectories()) {
				File file = new File(filename);
				if (file.exists()) {
					deleteEmptySubdirectories(file, 0);
				}
			}

			return new PipeRunResult(getSuccessForward(), message);
		}
		catch(Exception e) {
			throw new PipeRunException(this, "Error while deleting file(s)", e);
		}
	}

	private List<File> getFilesForDeletion(String filename) {
		File file = new File(filename);
		if (file.exists()) {
			List<File> result = new ArrayList<>();
			if (file.isDirectory()) {
				getFilesForDeletion(result, file);
			}
			else {
				if (fileFilter.accept(file))
					result.add(file);
			}
			return result;
		}
		return null;
	}

	private void getFilesForDeletion(List<File> result, File directory) {
		File[] files;
		if (getWildcard()!=null) {
			//WildCardFilter filter = new WildCardFilter(getWildcard());
			//files = directory.listFiles(filter);
			files=FileUtils.getFiles(directory.getPath(), getWildcard(), getExcludeWildcard(), getMinStableTime());
			for (int i = 0; i < files.length; i++) {
				if (getLastModifiedDelta() < 0 || FileUtils.getLastModifiedDelta(files[i]) > getLastModifiedDelta()) {
					result.add(files[i]);
				}
			}
		} else {
			files = directory.listFiles(fileFilter);
			result.addAll(Arrays.asList(files));
		}

		if (isSubdirectories()) {
			files = directory.listFiles(dirFilter);
			for (int i = 0; i < files.length; i++) {
				getFilesForDeletion(result, files[i]);
			}
		}
	}

	private void deleteEmptySubdirectories(File directory, int level) {
		if (directory.isDirectory()) {
			File[] dirs = directory.listFiles(dirFilter);
			for (int i = 0; i < dirs.length; i++) {
				deleteEmptySubdirectories(dirs[i], level+1);
			}
			if (level>0 && directory.list().length==0) {
				if (directory.delete()) {
					log.info("deleted empty directory [{}]", directory::getAbsolutePath);
				} else {
					log.warn("could not delete empty directory [{}]", directory::getAbsolutePath);
				}
			}
		} else {
			log.warn("file [{}] is not a directory, cannot delete subdirectories", directory::getAbsolutePath);
		}
	}

	private class _FileFilter implements FileFilter {
		@Override
		public boolean accept(File file) {
			if (file.isFile()) {
				if (getLastModifiedDelta() < 0
						|| FileUtils.getLastModifiedDelta(file) > getLastModifiedDelta()) {
					return true;
				}
			}
			return false;
		}
	}

	private class _DirFilter implements FileFilter {
		@Override
		public boolean accept(File file) {
			return file.isDirectory();
		}
	}

	/** files that match this pattern will be deleted. parameters of the pipe are applied to this pattern. if this attribute is not set, the input of the pipe is interpreted as the file to be removed */
	public void setFilePattern(String string) {
		filePattern = string;
	}
	public String getFilePattern() {
		return filePattern;
	}

	/** session key that contains the pattern of files to be deleted, only used if filePattern is not set */
	public void setFilePatternSessionKey(String string) {
		filePatternSessionKey = string;
	}
	public String getFilePatternSessionKey() {
		return filePatternSessionKey;
	}

	/**
	 * time in milliseconds after last modification that must have passed at least before a file will be deleted (set to negative value to disable)
	 * @ff.default 0
	 */
	public void setLastModifiedDelta(long l) {
		lastModifiedDelta = l;
	}
	public long getLastModifiedDelta() {
		return lastModifiedDelta;
	}

	/**
	 * when <code>true</code>, files  in subdirectories will be deleted, too
	 * @ff.default false
	 */
	public void setSubdirectories(boolean b) {
		subdirectories = b;
	}
	public boolean isSubdirectories() {
		return subdirectories;
	}

	/**
	 * when <code>true</code>, empty subdirectories will be deleted, too
	 * @ff.default false
	 */
	public void setDeleteEmptySubdirectories(boolean b) {
		deleteEmptySubdirectories = b;
	}
	public boolean isDeleteEmptySubdirectories() {
		return deleteEmptySubdirectories;
	}

	/** filter of files to delete. if not set and a directory is specified, all files in the directory are interpreted to be deleted */
	public void setWildcard(String string) {
		wildcard = string;
	}
	public String getWildcard() {
		return wildcard;
	}

	/** filter of files to be excluded for deletion */
	public void setExcludeWildcard(String excludeWildcard) {
		this.excludeWildcard = excludeWildcard;
	}
	public String getExcludeWildcard() {
		return excludeWildcard;
	}

	/**
	 * Minimal age of file <i>in milliseconds</i>, to avoid deleting a file while it is still being written (only used when wildcard is set) (set to 0 to disable)
	 * @ff.default 1000
	 */
	public void setMinStableTime(long minStableTime) {
		this.minStableTime = minStableTime;
	}
	public long getMinStableTime() {
		return minStableTime;
	}
}
