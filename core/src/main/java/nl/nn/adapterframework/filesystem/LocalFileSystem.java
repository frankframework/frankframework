/*
   Copyright 2019 Integration Partners

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

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;

public class LocalFileSystem implements IWritableFileSystem<File> {

	private String root;

	private class FilePathIterator implements Iterator<File> {

		private File files[];
		int i = 0;

		FilePathIterator(File files[]) {
			this.files = files;
		}

		@Override
		public boolean hasNext() {
			return files != null && i < files.length;
		}

		@Override
		public File next() {
			return files[i++];
		}

		@Override
		public void remove() {
			deleteFile(files[i++]);
		}

	}

	@Override
	public void configure() throws ConfigurationException {
		// No Action is required
	}

	@Override
	public void open() {
		// No Action is required
	}

	@Override
	public void close() {
		// No Action is required
	}

	@Override
	public File toFile(String filename) {
		return new File(getRoot(), filename);
	}

	@Override
	public Iterator<File> listFiles(String folder) {
		String path=getRoot();
		if (StringUtils.isEmpty(path)) {
			path=folder;
		} else {
			if (StringUtils.isNotEmpty(folder)) {
				path+="/"+folder;
			}
		}
		File dir = StringUtils.isNotEmpty(path)?new File(path):new File("/");
		FileFilter filter = new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return pathname.isFile();
			}
			
		};
		return new FilePathIterator(dir.listFiles(filter));
	}

	@Override
	public boolean exists(File f) {
		return f.exists();
	}

	@Override
	public OutputStream createFile(File f) throws IOException {
		return new FileOutputStream(f, false);
	}

	@Override
	public OutputStream appendFile(File f) throws FileNotFoundException {
		return new FileOutputStream(f, true);
	}

	@Override
	public InputStream readFile(File f) throws FileNotFoundException {
		return new FileInputStream(f);
	}

	@Override
	public void deleteFile(File f) {
		f.delete();
	}

	public boolean isFolder(File f) {
		return f.isDirectory();
	}
	@Override
	public boolean folderExists(String folder) throws FileSystemException {
		return isFolder(toFile(folder));
	}

	@Override
	public void createFolder(File f) throws FileSystemException {
		if (!f.exists()) {
			f.mkdir();
		}else {
			throw new FileSystemException("Create directory for [" + f + "] has failed. Directory already exists.");
		}
	}

	@Override
	public void removeFolder(File f) throws FileSystemException {
		if (f.exists()) {
			f.delete();
		}else {
			throw new FileSystemException("Remove directory for [" + f.getName() + "] has failed. Directory does not exist.");
		}
	}

	public String getRoot() {
		return root;
	}

	public void setRoot(String root) {
		this.root = root;
	}

	@Override
	public File renameFile(File f, String newName, boolean force) throws FileSystemException {
		File dest;

		dest = new File(newName);
		if (dest.exists()) {
			if (force)
				dest.delete();
			else {
				throw new FileSystemException("Cannot rename file. Destination file already exists.");
			}
		}
		f.renameTo(dest);
		return dest;
	}
	@Override
	public File moveFile(File f, String destinationFolder, boolean createFolder) throws FileSystemException {
		File toFolder = toFile(destinationFolder);
		if (toFolder.exists()) {
			if (!toFolder.isDirectory()) {
				throw new FileSystemException("Cannot move file. Destination file ["+toFolder.getName()+"] is not a folder.");
			}
		} else {
			if (createFolder)
				createFolder(toFolder);
			else {
				throw new FileSystemException("Cannot move file. Destination folder ["+toFolder.getName()+"] does not exist.");
			}
		}
		File target=new File(toFolder,f.getName());
		if (!f.renameTo(target)) {
			return f;
		}
		return target;

	}


	@Override
	public long getFileSize(File f) throws FileSystemException {
		return f.length();
	}

	@Override
	public String getName(File f) {
		return f.getName();
	}

	@Override
	public String getCanonicalName(File f) throws FileSystemException {
		try {
			return f.getCanonicalPath();
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public Date getModificationTime(File f) throws FileSystemException {
		return new Date(f.lastModified());
	}

	@Override
	public Map<String, Object> getAdditionalFileProperties(File f) {
		return null;
	}

}
