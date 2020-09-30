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
import nl.nn.adapterframework.util.Misc;

public class FileSystemUtils {
	protected static Logger log = LogUtil.getLogger(FileSystemUtils.class);

	/**
	 * Prepares the destination of a file:
	 * - if the file exists, checks overwrite, or performs rollover
	 * TODO: - if the file does not exist, checks if the parent folder exists
	 */
	public static <F> void prepareDestination(IBasicFileSystem<F> fileSystem, F destination, boolean overwrite, int numOfBackups, boolean createFolders) throws FileSystemException {
		if (fileSystem.exists(destination)) {
			if (overwrite) {
				log.debug("removing current destination file ["+fileSystem.getCanonicalName(destination)+"]");
				fileSystem.deleteFile(destination);
			} else {
				if (numOfBackups>0) {
					FileSystemUtils.rolloverByNumber((IWritableFileSystem<F>)fileSystem, destination, numOfBackups);
				} else {
					throw new FileSystemException("Cannot rename file to ["+fileSystem.getName(destination)+"]. Destination file ["+fileSystem.getCanonicalName(destination)+"] already exists.");
				}
			}
		}
	}
	
	public static <F> void prepareDestination(IBasicFileSystem<F> fileSystem, F file, String destinationFolder, boolean overwrite, int numOfBackups, boolean createFolders) throws FileSystemException {
		if (!fileSystem.folderExists(destinationFolder)) {
			if (createFolders) {
				fileSystem.createFolder(destinationFolder);
			} else {
				throw new FileSystemException("destination folder ["+destinationFolder+"] does not exist");
			}
		}
		F destinationFile = fileSystem.toFile(destinationFolder, fileSystem.getName(file));
		if (overwrite) {
			if (fileSystem.exists(destinationFile)) {
				log.debug("removing current destination file ["+fileSystem.getCanonicalName(destinationFile)+"]");
				fileSystem.deleteFile(destinationFile);
			}
		} else {
			if (numOfBackups>0) {
				FileSystemUtils.rolloverByNumber((IWritableFileSystem<F>)fileSystem, destinationFile, numOfBackups);
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

	
	private static <F> void checkFilename(String stage, IWritableFileSystem<F> fileSystem, F file, String expectedName) throws FileSystemException {
		if (!expectedName.endsWith(fileSystem.getName(file))) {
			System.out.println("---> at ["+stage+"]: filename mismatch expected ["+expectedName+"]  ["+fileSystem.getName(file)+"]");
		} else {
			System.out.println("--> at ["+stage+"]: filename OK expected ["+expectedName+"]  ["+fileSystem.getName(file)+"]");
		}
		for (Iterator<F> it= fileSystem.listFiles(null); it.hasNext();) {
			F ff = it.next();
			System.out.println("- "+fileSystem.getCanonicalName(ff)+" - "+fileSystem.getName(ff));
		}
	}
	
	public static <F> void rolloverByNumber(IWritableFileSystem<F> fileSystem, F file, int numberOfBackups) throws FileSystemException {
		if (!fileSystem.exists(file)) {
			return;
		}
		String filename = fileSystem.getCanonicalName(file);
		
		checkFilename("enter rolloverByNumber", fileSystem, file, filename);

		String tmpFilename = filename+".tmp"+Misc.createUUID();
		F tmpFile = fileSystem.toFile(tmpFilename);
		tmpFile = fileSystem.renameFile(file, tmpFile);

		checkFilename("moved destination to tempfile", fileSystem, tmpFile, tmpFilename);

		
		if (log.isDebugEnabled()) log.debug("Rotating files with a name starting with ["+filename+"] and keeping ["+numberOfBackups+"] backups");
		F lastFile=fileSystem.toFile(filename+"."+numberOfBackups);
		if (fileSystem.exists(lastFile)) {
			if (log.isDebugEnabled()) log.debug("deleting file ["+filename+"."+numberOfBackups+"]");
			fileSystem.deleteFile(lastFile);
		}
		
		for(int i=numberOfBackups-1;i>0;i--) {
			String sourceFilename=filename+"."+i;
			String destinationFilename=filename+"."+(i+1);
			F source=fileSystem.toFile(sourceFilename);
			if (!sourceFilename.endsWith(fileSystem.getName(source))) {
				System.out.println("---> ["+sourceFilename+"]  ["+fileSystem.getName(source)+"]");
			}
			F destination=fileSystem.toFile(destinationFilename);
			
			if (fileSystem.exists(source)) {
				if (log.isDebugEnabled()) log.debug("moving file ["+sourceFilename+"] to file ["+destinationFilename+"]");
				destination = fileSystem.renameFile(source, destination);
				checkFilename("did a backupstep", fileSystem, tmpFile, destinationFilename);
			} else {
				if (log.isDebugEnabled()) log.debug("file ["+sourceFilename+"] does not exist, no need to move");
			}
		}
		String destinationFilename=filename+".1";
		F destination=fileSystem.toFile(destinationFilename);
		if (log.isDebugEnabled()) log.debug("moving file ["+tmpFilename+"] to file ["+destinationFilename+"]");
		destination = fileSystem.renameFile(tmpFile, destination);
		checkFilename("renamed tempfile to destination", fileSystem, destination, destinationFilename);
	}


	public static <F> void rolloverBySize(IWritableFileSystem<F> fileSystem, F file, int rotateSize, int numberOfBackups) throws FileSystemException {
		if (!fileSystem.exists(file)) {
			return;
		}
		if (fileSystem.getFileSize(file) > rotateSize) {
			rolloverByNumber(fileSystem, file, numberOfBackups);
		}
	}


	public static <F> void rolloverByDay(IWritableFileSystem<F> fileSystem, F file, String folder, int rotateDays) throws FileSystemException {
		final long millisPerDay = 24 * 60 * 60 * 1000;
		
		Date lastModified = fileSystem.getModificationTime(file);
		Date sysTime = new Date();
		if (DateUtils.isSameDay(lastModified, sysTime) || lastModified.after(sysTime)) {
			return;
		}
		String srcFilename = fileSystem.getCanonicalName(file);
		F tgtFilename = fileSystem.toFile(srcFilename+"."+DateUtils.format(lastModified, DateUtils.shortIsoFormat));
		((IWritableFileSystem<F>)fileSystem).renameFile(file, tgtFilename);
		
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
	}

}
