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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.util.Date;
import java.util.Iterator;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

public class FileSystemUtils {
	protected static Logger log = LogUtil.getLogger(FileSystemUtils.class);

	/**
	 * Check if a source file exists.
	 */
	public static <F> void checkSource(IBasicFileSystem<F> fileSystem, F source, String action) throws FileNotFoundException, FileSystemException {
		if (!fileSystem.exists(source)) {
			throw new FileNotFoundException("file to "+action+" ["+fileSystem.getName(source)+"], canonical name ["+fileSystem.getCanonicalName(source)+"], does not exist");
		}
	}
	
	/**
	 * Prepares the destination of a file:
	 * - if the file exists, checks overwrite, or performs rollover
	 */
	public static <F> void prepareDestination(IWritableFileSystem<F> fileSystem, F destination, boolean overwrite, int numOfBackups, String action) throws FileSystemException {
		if (fileSystem.exists(destination)) {
			if (overwrite) {
				log.debug("removing current destination file ["+fileSystem.getCanonicalName(destination)+"]");
				fileSystem.deleteFile(destination);
			} else {
				if (numOfBackups>0) {
					FileSystemUtils.rolloverByNumber((IWritableFileSystem<F>)fileSystem, destination, numOfBackups);
				} else {
					throw new FileSystemException("Cannot "+action+" file to ["+fileSystem.getName(destination)+"]. Destination file ["+fileSystem.getCanonicalName(destination)+"] already exists.");
				}
			}
		}
	}
	
	/**
	 * Prepares the destination folder, e.g. for move or copy.
	 */
	public static <F> void prepareDestination(IBasicFileSystem<F> fileSystem, F source, String destinationFolder, boolean overwrite, int numOfBackups, boolean createFolders, String action) throws FileSystemException {
		if (!fileSystem.folderExists(destinationFolder)) {
			if (fileSystem.exists(fileSystem.toFile(destinationFolder))) {
				throw new FileSystemException("destination ["+destinationFolder+"] exists but is not a folder");
			}
			if (createFolders) {
				fileSystem.createFolder(destinationFolder);
			} else {
				throw new FileSystemException("destination folder ["+destinationFolder+"] does not exist");
			}
		}
		if (fileSystem instanceof IWritableFileSystem) {
			F destinationFile = fileSystem.toFile(destinationFolder, fileSystem.getName(source));
			prepareDestination((IWritableFileSystem<F>)fileSystem, destinationFile, overwrite, numOfBackups, action);
		}
	}
	
	public static <F> F renameFile(IWritableFileSystem<F> fileSystem, F source, F destination, boolean overwrite, int numOfBackups) throws FileSystemException {
		checkSource(fileSystem, source, "rename");
		prepareDestination(fileSystem, destination, overwrite, numOfBackups, "rename");
		F newFile = fileSystem.renameFile(source, destination);
		if (newFile == null) {
			throw new FileSystemException("cannot rename file [" + fileSystem.getName(source) + "] to [" + fileSystem.getName(destination) + "]");
		}
		return newFile;
	}

	public static <F> F moveFile(IBasicFileSystem<F> fileSystem, F file, String destinationFolder, boolean overwrite, int numOfBackups, boolean createFolders) throws FileSystemException {
		checkSource(fileSystem, file, "move");
		prepareDestination(fileSystem, file, destinationFolder, overwrite, numOfBackups, createFolders, "move");
		F newFile = fileSystem.moveFile(file, destinationFolder, createFolders);
		if (newFile == null) {
			throw new FileSystemException("cannot move file [" + fileSystem.getName(file) + "] to [" + destinationFolder + "]");
		}
		return newFile;
	}
	
	public static <F> F copyFile(IBasicFileSystem<F> fileSystem, F file, String destinationFolder, boolean overwrite, int numOfBackups, boolean createFolders) throws FileSystemException {
		checkSource(fileSystem, file, "copy");
		prepareDestination(fileSystem, file, destinationFolder, overwrite, numOfBackups, createFolders, "copy");
		F newFile = fileSystem.copyFile(file, destinationFolder, createFolders);
		if (newFile == null) {
			throw new FileSystemException("cannot copy file [" + fileSystem.getName(file) + "] to [" + destinationFolder + "]");
		}
		return newFile;
	}

	
	public static <F> void rolloverByNumber(IWritableFileSystem<F> fileSystem, F file, int numberOfBackups) throws FileSystemException {
		if (!fileSystem.exists(file)) {
			return;
		}
		String filename = fileSystem.getCanonicalName(file);
		
		String tmpFilename = filename+".tmp-"+Misc.createUUID();
		F tmpFile = fileSystem.toFile(tmpFilename);
		tmpFile = fileSystem.renameFile(file, tmpFile);

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
			F destination=fileSystem.toFile(destinationFilename);
			
			if (fileSystem.exists(source)) {
				if (log.isDebugEnabled()) log.debug("moving file ["+sourceFilename+"] to file ["+destinationFilename+"]");
				destination = fileSystem.renameFile(source, destination);
			} else {
				if (log.isDebugEnabled()) log.debug("file ["+sourceFilename+"] does not exist, no need to move");
			}
		}
		String destinationFilename=filename+".1";
		F destination=fileSystem.toFile(destinationFilename);
		if (log.isDebugEnabled()) log.debug("moving file ["+tmpFilename+"] to file ["+destinationFilename+"]");
		destination = fileSystem.renameFile(tmpFile, destination);
	}


	public static <F> void rolloverBySize(IWritableFileSystem<F> fileSystem, F file, int rotateSize, int numberOfBackups) throws FileSystemException {
		if (!fileSystem.exists(file)) {
			return;
		}
		if (fileSystem.getFileSize(file) > rotateSize) {
			rolloverByNumber(fileSystem, file, numberOfBackups);
		}
	}

	public static <F> DirectoryStream<F> getDirectoryStream(Iterable<F> iterable){
		final DirectoryStream<F> ds = new DirectoryStream<F>() {

			@Override
			public void close() throws IOException {
				if (iterable instanceof AutoCloseable) {
					try {
						((AutoCloseable)iterable).close();
					} catch (IOException e) {
						throw e;
					} catch (Exception e) {
						throw new IOException(e);
					}
				}
			}

			@Override
			public Iterator<F> iterator() {
				return iterable.iterator();
			}
			
		};

		return ds;
	}
	
	public static <F> DirectoryStream<F> getDirectoryStream(Iterator<F> iterator){
		return getDirectoryStream(iterator, (Supplier<IOException>)null);
	}

	public static <F> DirectoryStream<F> getDirectoryStream(Iterator<F> iterator, Runnable onClose) {
		return getDirectoryStream(iterator, (Supplier<IOException>)() -> { 
			if (onClose!=null) {
				onClose.run();
			}
			return null;
		});
	}
	
	public static <F> DirectoryStream<F> getDirectoryStream(Iterator<F> iterator, AutoCloseable resourceToCloseOnClose){
		return getDirectoryStream(iterator, (Supplier<IOException>)() -> { 
			if (resourceToCloseOnClose!=null) {
				try {
					resourceToCloseOnClose.close();
					return null;
				} catch (IOException e) {
					return e;
				} catch (Exception e) {
					return new IOException(e);
				}
			}
			return null;
		});
	}
		
	public static <F> DirectoryStream<F> getDirectoryStream(Iterator<F> iterator, Supplier<IOException> onClose){
		final DirectoryStream<F> ds = new DirectoryStream<F>() {

			@Override
			public void close() throws IOException {
				if (onClose!=null) {
					IOException result = onClose.get();
					if (result!=null) {
						throw result;
					}
				}
			}

			@Override
			public Iterator<F> iterator() {
				return iterator;
			}
			
		};

		return ds;
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
		fileSystem.renameFile(file, tgtFilename);
		
		if (log.isDebugEnabled()) log.debug("Deleting files in folder ["+folder+"] that have a name starting with ["+srcFilename+"] and are older than ["+rotateDays+"] days");
		long threshold = sysTime.getTime()- rotateDays*millisPerDay;
		try(DirectoryStream<F> ds = fileSystem.listFiles(folder)) {
			Iterator<F> it = ds.iterator();
			while(it.hasNext()) {
				F f=it.next();
				String filename=fileSystem.getName(f);
				if (filename!=null && filename.startsWith(srcFilename) && fileSystem.getModificationTime(f).getTime()<threshold) {
					if (log.isDebugEnabled()) log.debug("deleting file ["+filename+"]");
					fileSystem.deleteFile(f);
				}
			}
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

}
