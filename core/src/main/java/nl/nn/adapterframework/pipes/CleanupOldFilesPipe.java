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

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.FileUtils;

import org.apache.commons.lang.StringUtils;

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
		
		
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		try {
			String filename;
			if (StringUtils.isNotEmpty(getFilePattern())) {
				filename = FileUtils.getFilename(getParameterList(), session, "", getFilePattern());
			} else {
				if (StringUtils.isNotEmpty(getFilePatternSessionKey())) {
					filename = FileUtils.getFilename(getParameterList(), session, "", (String)session.get(getFilePatternSessionKey()));
				} else {
					if (StringUtils.isEmpty((String)input)) {
						throw new PipeRunException(this, "input empty, but should contain filename to delete");
					} else {
						File in = new File(input.toString());
						filename = in.getName();
					}
				}
			}
			
			List delFiles = getFilesForDeletion(filename);
			if (delFiles != null && delFiles.size() > 0) {
				for (Iterator fileIt = delFiles.iterator(); fileIt.hasNext();) {
					File file = (File)fileIt.next();
					String curfilename=file.getName();
					if (file.delete()) {
						log.info(getLogPrefix(session)+"deleted file ["+file.getAbsolutePath()+"]");
					} else {
						log.warn(getLogPrefix(session)+"could not delete file ["+file.getAbsolutePath()+"]");
					}
				}
			} else {
				log.info(getLogPrefix(session)+"no files match pattern ["+filename+"]");
			}

			if (isDeleteEmptySubdirectories()) {
				File file = new File(filename);
				if (file.exists()) {
					deleteEmptySubdirectories(getLogPrefix(session), file, 0);
				}
			}
			
			return new PipeRunResult(getForward(), input);
		}
		catch(Exception e) {
			throw new PipeRunException(this, "Error while deleting file(s)", e); 
		}
	}

	private List getFilesForDeletion(String filename) {
		File file = new File(filename);
		if (file.exists()) {
			List result = new ArrayList();
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

	private void getFilesForDeletion(List result, File directory) {
		File[] files;
		if (getWildcard()!=null) {
			//WildCardFilter filter = new WildCardFilter(getWildcard());
			//files = directory.listFiles(filter);
			files=FileUtils.getFiles(directory.getPath(), getWildcard(), getExcludeWildcard(), getMinStableTime());
			for (int i = 0; i < files.length; i++) {
				if (getLastModifiedDelta() < 0
						|| FileUtils.getLastModifiedDelta(files[i]) > getLastModifiedDelta()) {
					result.add(files[i]);
				}
			}
		} else {
			files = directory.listFiles(fileFilter);
			for (int i = 0; i < files.length; i++) {
				result.add(files[i]);
			}
		}
		
		if (isSubdirectories()) {
			files = directory.listFiles(dirFilter);
			for (int i = 0; i < files.length; i++) {
				getFilesForDeletion(result, files[i]);
			}		
		}
	}

	private void deleteEmptySubdirectories(String logPrefix, File directory, int level) {
		if (directory.isDirectory()) {
			File[] dirs = directory.listFiles(dirFilter);
			for (int i = 0; i < dirs.length; i++) {
				deleteEmptySubdirectories(logPrefix, dirs[i], level+1);
			}
			if (level>0 && directory.list().length==0) {
				if (directory.delete()) {
					log.info(logPrefix+"deleted empty directory ["+directory.getAbsolutePath()+"]");
				} else {
					log.warn(logPrefix+"could not delete empty directory ["+directory.getAbsolutePath()+"]");
				}
			}
		} else {
			log.warn(logPrefix+"file ["+directory.getAbsolutePath()+"] is not a directory, cannot delete subdirectories");
		}
	}

	private class _FileFilter implements FileFilter {
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
		public boolean accept(File file) {
			return file.isDirectory();
		}
	}

	@IbisDoc({"files that match this pattern will be deleted. parameters of the pipe are applied to this pattern. if this attribute is not set, the input of the pipe is interpreted as the file to be removed", ""})
	public void setFilePattern(String string) {
		filePattern = string;
	}
	public String getFilePattern() {
		return filePattern;
	}

	@IbisDoc({"", " "})
	public void setFilePatternSessionKey(String string) {
		filePatternSessionKey = string;
	}
	public String getFilePatternSessionKey() {
		return filePatternSessionKey;
	}

	@IbisDoc({"time in milliseconds after last modification that must have passed at least before a file will be deleted (set to negative value to disable)", "0"})
	public void setLastModifiedDelta(long l) {
		lastModifiedDelta = l;
	}
	public long getLastModifiedDelta() {
		return lastModifiedDelta;
	}

	@IbisDoc({"when <code>true</code>, files  in subdirectories will be deleted, too", "false"})
	public void setSubdirectories(boolean b) {
		subdirectories = b;
	}
	public boolean isSubdirectories() {
		return subdirectories;
	}

	@IbisDoc({"when <code>true</code>, empty subdirectories will be deleted, too", "false"})
	public void setDeleteEmptySubdirectories(boolean b) {
		deleteEmptySubdirectories = b;
	}
	public boolean isDeleteEmptySubdirectories() {
		return deleteEmptySubdirectories;
	}

	@IbisDoc({"filter of files to delete. if not set and a directory is specified, all files in the directory are interpreted to be deleted", ""})
	public void setWildcard(String string) {
		wildcard = string;
	}
	public String getWildcard() {
		return wildcard;
	}

	@IbisDoc({"filter of files to be excluded for deletion", ""})
	public void setExcludeWildcard(String excludeWildcard) {
		this.excludeWildcard = excludeWildcard;
	}
	public String getExcludeWildcard() {
		return excludeWildcard;
	}

	@IbisDoc({"minimal age of file in milliseconds, to avoid deleting a file while it is still being written (only used when wildcard is set) (set to 0 or negative value to disable)", "1000 [ms]"})
	public void setMinStableTime(long minStableTime) {
		this.minStableTime = minStableTime;
	}
	public long getMinStableTime() {
		return minStableTime;
	}
}