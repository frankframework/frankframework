/*
   Copyright 2020 WeAreFrank!

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
package nl.nn.adapterframework.filesystem;

import java.util.Date;
import java.util.Iterator;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;

public class FileSystemUtils {
	protected static Logger log = LogUtil.getLogger(FileSystemUtils.class);

	public static <F> void prepareDestination(IBasicFileSystem<F> fileSystem, F file, String destinationFolder, boolean overwrite, int numOfBackups, boolean createFolders) throws FileSystemException {
		if (overwrite) {
			F destinationFile = fileSystem.toFile(destinationFolder, fileSystem.getName(file));
			if (fileSystem.exists(destinationFile)) {
				log.debug("removing current destination file ["+fileSystem.getCanonicalName(destinationFile)+"]");
				fileSystem.deleteFile(destinationFile);
			}
		} else {
			if (numOfBackups>0) {
				FileSystemUtils.rolloverByNumber((IWritableFileSystem<F>)fileSystem, destinationFolder, fileSystem.getName(file), numOfBackups);
			}
		}
	}
	
	public static <F> F moveFile(IBasicFileSystem<F> fileSystem, F file, String destinationFolder, boolean overwrite, int numOfBackups, boolean createFolders) throws FileSystemException {
		prepareDestination(fileSystem, file, destinationFolder, overwrite, numOfBackups, createFolders);
		F newFile = fileSystem.moveFile(file, destinationFolder, createFolders);
		if (newFile == null) {
			throw new FileSystemException("cannot move file [" + fileSystem.getName(file) + "] to [" + destinationFolder + "]");
		}
		return newFile;
	}
	
	public static <F> F copyFile(IBasicFileSystem<F> fileSystem, F file, String destinationFolder, boolean overwrite, int numOfBackups, boolean createFolders) throws FileSystemException {
		prepareDestination(fileSystem, file, destinationFolder, overwrite, numOfBackups, createFolders);
		F newFile = fileSystem.copyFile(file, destinationFolder, createFolders);
		if (newFile == null) {
			throw new FileSystemException("cannot copy file [" + fileSystem.getName(file) + "] to [" + destinationFolder + "]");
		}
		return newFile;
	}

	public static <F> void rolloverByNumber(IWritableFileSystem<F> fileSystem, F file, int numberOfBackups) throws FileSystemException {
		rolloverByNumber(fileSystem, null, fileSystem.getName(file), numberOfBackups);
	}
	
	public static <F> void rolloverByNumber(IWritableFileSystem<F> fileSystem, String folder, String filename, int numberOfBackups) throws FileSystemException {
		F file = fileSystem.toFile(folder, filename);
		if (!fileSystem.exists(file)) {
			return;
		}
		
		if (log.isDebugEnabled()) log.debug("Rotating files with a name starting with ["+filename+"] and keeping ["+numberOfBackups+"] backups");
		F lastFile=fileSystem.toFile(folder, filename+"."+numberOfBackups);
		if (fileSystem.exists(lastFile)) {
			if (log.isDebugEnabled()) log.debug("deleting file  ["+filename+"."+numberOfBackups+"]");
			fileSystem.deleteFile(lastFile);
		}
		
		for(int i=numberOfBackups-1;i>0;i--) {
			F source=fileSystem.toFile(folder, filename+"."+i);
			if (fileSystem.exists(source)) {
				if (log.isDebugEnabled()) log.debug("moving file ["+filename+"."+i+"] to file ["+filename+"."+(i+1)+"]");
				fileSystem.renameFile(source, filename+"."+(i+1), true);
			} else {
				if (log.isDebugEnabled()) log.debug("file ["+filename+"."+i+"] does not exist, no need to move");
			}
		}
		if (log.isDebugEnabled()) log.debug("moving file ["+filename+"] to file ["+filename+".1]");
		fileSystem.renameFile(file, filename+".1", true);
	}
	

	public static <F> void rolloverBySize(IWritableFileSystem<F> fileSystem, F file, int rotateSize, int numberOfBackups) throws FileSystemException {
		rolloverBySize(fileSystem, file, null, rotateSize, numberOfBackups);
	}
	
	public static <F> void rolloverBySize(IWritableFileSystem<F> fileSystem, F file, String folder, int rotateSize, int numberOfBackups) throws FileSystemException {
		if (!fileSystem.exists(file)) {
			return;
		}
		if (fileSystem.getFileSize(file) > rotateSize) {
			rolloverByNumber(fileSystem, folder, fileSystem.getName(file), numberOfBackups);
		}
	}

	public static <F> void rolloverByDay(IWritableFileSystem<F> fileSystem, F file, String folder, int rotateDays) throws FileSystemException {
		final long millisPerDay = 24 * 60 * 60 * 1000;
		
		Date lastModified = fileSystem.getModificationTime(file);
		Date sysTime = new Date();
		if (DateUtils.isSameDay(lastModified, sysTime) || lastModified.after(sysTime)) {
			return;
		}
		String srcFilename = fileSystem.getName(file);
		
		if (log.isDebugEnabled()) log.debug("Deleting files in folder ["+folder+"] that have a name starting with ["+srcFilename+"] and are older than ["+rotateDays+"] days");
		long threshold = sysTime.getTime()- rotateDays*millisPerDay;
		Iterator<F> it = fileSystem.listFiles(folder);
		while(it.hasNext()) {
			F f=it.next();
			String filename=fileSystem.getName(f);
			if (filename!=null && filename.startsWith(srcFilename) && fileSystem.getModificationTime(f).getTime()<threshold) {
				if (log.isDebugEnabled()) log.debug("deleting file ["+filename+"]");
				fileSystem.deleteFile(f);
			}
		}

		String tgtFilename = srcFilename+"."+DateUtils.format(fileSystem.getModificationTime(file), DateUtils.shortIsoFormat);
		((IWritableFileSystem<F>)fileSystem).renameFile(file, tgtFilename, true);
	}

}
