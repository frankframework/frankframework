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
import nl.nn.adapterframework.util.FileUtils;

import org.apache.commons.lang.StringUtils;

/**
 * Pipe for deleting files.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.batch.CleanupOldFilesPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, nl.nn.adapterframework.core.PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setFilePattern(String) filePattern}</td><td>files that match this pattern will be deleted. Parameters of the pipe are applied to this pattern. If this attribute is not set, the input of the pipe is interpreted as the file to be removed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSubdirectories(boolean) subdirectories}</td><td>when <code>true</code>, files  in subdirectories will be deleted, too</td><td>false</td></tr>
 * <tr><td>{@link #setLastModifiedDelta(long) lastModifiedDelta}</td><td>time in milliseconds that must have passed at least before a file will be deleted</td><td>0</td></tr>
 * </table>
 * </p>
 * 
 * @author: John Dekker
 * @since:  4.2
 */
public class CleanupOldFilesPipe extends FixedForwardPipe {
	public static final String version = "$RCSfile: CleanupOldFilesPipe.java,v $  $Revision: 1.8 $ $Date: 2012-06-01 10:52:49 $";
	
	private String filePattern;
	private boolean subdirectories=false;
	private long lastModifiedDelta=0;

	private _FileFilter fileFilter = new _FileFilter();
	private _DirFilter dirFilter = new _DirFilter();
		
		
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		try {
			String filename;
			if (StringUtils.isNotEmpty(getFilePattern())) {
				filename = FileUtils.getFilename(getParameterList(), session, "", getFilePattern());
			} else {
				if (StringUtils.isEmpty((String)input)) {
					throw new PipeRunException(this, "input empty, but should contain filename to delete");
				} else {
					File in = new File(input.toString());
					filename = in.getName();
				}
			}
			
			List delFiles = getFilesForDeletion(filename);
			if (delFiles != null && delFiles.size() > 0) {
				for (Iterator fileIt = delFiles.iterator(); fileIt.hasNext();) {
					File file = (File)fileIt.next();
					String curfilename=file.getName();
					file.delete();
					log.info(getLogPrefix(session)+"deleted file ["+file.getAbsolutePath()+"]");
				}
			} else {
				log.info(getLogPrefix(session)+"no files match pattern ["+filename+"]");
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
		File[] files = directory.listFiles(fileFilter);
		for (int i = 0; i < files.length; i++) {
			result.add(files[i]);
		}
		
		if (isSubdirectories()) {
			files = directory.listFiles(dirFilter);
			for (int i = 0; i < files.length; i++) {
				getFilesForDeletion(result, files[i]);
			}		
		}
	}

	private class _FileFilter implements FileFilter {
		public boolean accept(File file) {
			if (file.isFile()) {
				if ((System.currentTimeMillis() - file.lastModified()) > getLastModifiedDelta()) {
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


	public void setFilePattern(String string) {
		filePattern = string;
	}
	public String getFilePattern() {
		return filePattern;
	}

	public void setLastModifiedDelta(long l) {
		lastModifiedDelta = l;
	}
	public long getLastModifiedDelta() {
		return lastModifiedDelta;
	}

	public void setSubdirectories(boolean b) {
		subdirectories = b;
	}
	public boolean isSubdirectories() {
		return subdirectories;
	}
}
