/*
   Copyright 2019, 2020 WeAreFrank!

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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.WildCardFilter;

/**
 * {@link IWritableFileSystem FileSystem} representation of the local filesystem.
 *  
 * @author Gerrit van Brakel
 *
 */
public class LocalFileSystem implements IWritableFileSystem<File> {

	private String root;
	private String wildcard;
	private String excludeWildcard;

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
		return toFile(null, filename);
	}

	@Override
	public File toFile(String folder, String filename) {
		if (StringUtils.isEmpty(folder)) {
			if (StringUtils.isEmpty(getRoot())) {
				return new File(filename);
			}
			return new File(getRoot(), filename);
		}
		if (StringUtils.isEmpty(getRoot())) {
			return new File(folder, filename);
		}
		return new File(getRoot()+"/"+folder, filename);
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
		final File dir = StringUtils.isNotEmpty(path)?new File(path):new File("/");
		final WildCardFilter wildcardfilter =  StringUtils.isEmpty(getWildcard()) ? null : new WildCardFilter(getWildcard());
		final WildCardFilter excludeFilter =  StringUtils.isEmpty(getExcludeWildcard()) ? null : new WildCardFilter(getExcludeWildcard());

		FileFilter filter = new FileFilter() {

			@Override
			public boolean accept(File file) {
				return file.isFile() 
						&& (wildcardfilter==null || wildcardfilter.accept(dir, file.getName()))
						&& (excludeFilter==null || !excludeFilter.accept(dir, file.getName()));
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
	public void createFolder(String folder) throws FileSystemException {
		if (!folderExists(folder)) {
			toFile(folder).mkdir();
		}else {
			throw new FileSystemException("Create directory for [" + folder + "] has failed. Directory already exists.");
		}
	}

	@Override
	public void removeFolder(String folder) throws FileSystemException {
		if (folderExists(folder)) {
			toFile(folder).delete();
		}else {
			throw new FileSystemException("Remove directory for [" + folder + "] has failed. Directory does not exist.");
		}
	}

	@Override
	public File renameFile(File f, String newName, boolean force) throws FileSystemException {
		File dest;

		dest = new File(f.getParentFile(), newName);
		if (dest.exists()) {
			if (force)
				dest.delete();
			else {
				throw new FileSystemException("Cannot rename file to ["+newName+"]. Destination file already exists.");
			}
		}
		f.renameTo(dest);
		return dest;
	}
	
	protected File getDestinationFile(File f, String destinationFolder, boolean createFolder) throws FileSystemException {
		File toFolder = toFile(destinationFolder);
		if (toFolder.exists()) {
			if (!toFolder.isDirectory()) {
				throw new FileSystemException("destination ["+toFolder.getPath()+"] is not a folder");
			}
		} else {
			if (createFolder)
				createFolder(destinationFolder);
			else {
				throw new FileSystemException("destination folder ["+toFolder.getPath()+"] does not exist");
			}
		}
		File target=new File(toFolder,f.getName());
		return target;
	}
	
	@Override
	public File moveFile(File f, String destinationFolder, boolean createFolder) throws FileSystemException {
		File target = getDestinationFile(f, destinationFolder, createFolder);
		if (!f.renameTo(target)) {
			throw new FileSystemException("cannot move file ["+f.getPath()+"] to ["+target.getPath()+"]");
		}
		return target;
	}
	@Override
	public File copyFile(File f, String destinationFolder, boolean createFolder) throws FileSystemException {
		File target = getDestinationFile(f, destinationFolder, createFolder);
		try {
			FileUtils.copyFile(f, target);
		} catch (IOException e) {
			throw new FileSystemException("cannot copy file ["+f.getPath()+"] to ["+target.getPath()+"]",e);
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

	@Override
	public String getPhysicalDestinationName() {
		return "root ["+(getRoot()==null?"":getRoot())+"]";
	}


	@IbisDoc({"1", "Path to the folder that serves as the root of this virtual filesystem. All specifications of folders or files are relative to this root. "+
			"When the root is left unspecified, absolute paths to files and folders can be used", "" })
	public void setRoot(String root) {
		this.root = root;
	}
	public String getRoot() {
		return root;
	}

	@IbisDoc({"2", "filter of files to look for in inputdirectory, e.g. '*.inp'", ""})
	public void setWildcard(String wildcard) {
		this.wildcard = wildcard;
	}
	public String getWildcard() {
		return wildcard;
	}

	@IbisDoc({"3", "filter of files to be excluded when looking in inputdirectory", ""})
	public void setExcludeWildcard(String excludeWildcard) {
		this.excludeWildcard = excludeWildcard;
	}
	public String getExcludeWildcard() {
		return excludeWildcard;
	}

}
