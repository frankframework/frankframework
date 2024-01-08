package org.frankframework.filesystem.mock;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.filesystem.FileSystemException;
import org.frankframework.filesystem.FileSystemUtils;
import org.frankframework.filesystem.IWritableFileSystem;
import org.frankframework.stream.Message;
import org.frankframework.util.LogUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import lombok.Getter;
import lombok.Setter;

public class MockFileSystem<M extends MockFile> extends MockFolder implements IWritableFileSystem<M>, ApplicationContextAware {
	private final @Getter(onMethod = @__(@Override)) String domain = "MockFilesystem";
	protected Logger log = LogUtil.getLogger(this);

	private boolean configured=false;
	private boolean opened=false;

	private @Getter @Setter ApplicationContext applicationContext;

	public MockFileSystem() {
		super("MOCKFILESYSTEM",null);
	}

	@Override
	public void configure() throws ConfigurationException {
		assertNotNull(applicationContext);
		configured=true;
	}

	@Override
	public void open() throws FileSystemException {
		if (!configured) {
			throw new IllegalStateException("Not yet configured");
		}
		if (opened) {
			throw new IllegalStateException("Already open");
		}
		opened=true;
	}

	@Override
	public void close() throws FileSystemException {
		if (!opened) {
			log.warn("closed before opened");
		}
		opened=false;
	}

	private void checkOpen() {
		if (!configured) {
			throw new IllegalStateException("Not yet configured");
		}
		if (!opened) {
			throw new IllegalStateException("Not yet open");
		}

	}

	@Override
	public boolean isOpen() {
		return opened;
	}


	private void checkOpenAndExists(MockFile f) throws FileSystemException {
		checkOpen();
		MockFolder folder = f.getOwner();
		if (folder==null) {
			throw new FileSystemException("file ["+f.getName()+"] has no owner");
		}
		String folderName = folder instanceof MockFileSystem ? null : folder.getName();
		checkOpenAndExists(folderName, f);
	}

	private void checkOpenAndExists(String folderName, MockFile f) throws FileSystemException {
		checkOpen();
		MockFolder folder=folderName==null?this:getFolders().get(folderName);
		if (folder==null) {
			throw new FileSystemException("folder ["+folderName+"] does not exist");
		}
		if (f.getOwner()==null) {
			throw new FileSystemException("file ["+f.getName()+"] has no owner");
		}
		if (!folder.getFiles().containsKey(f.getName())) {
			throw new FileSystemException("file ["+f.getName()+"] does not exist in folder ["+folderName+"]");
		}
	}

	@Override
	public M toFile(String filename) throws FileSystemException {
		checkOpen();
		int slashPos = filename.lastIndexOf('/');
		if (slashPos>=0) {
			return toFile(filename.substring(0,slashPos),filename.substring(slashPos+1));
		}
		M result = (M)getFiles().get(filename);
		if (result!=null) {
			return result;
		}
		return (M)new MockFile(filename,this);
	}

	@Override
	public M toFile(String folderName, String filename) throws FileSystemException {
		checkOpen();
		MockFolder destFolder= folderName==null || folderName.equals("MOCKFILESYSTEM")?this:getFolders().get(folderName);
		if (destFolder==null) {
			throw new FileSystemException("folder ["+folderName+"] does not exist");
		}
		M result = (M)destFolder.getFiles().get(filename);
		if (result!=null) {
			return result;
		}
		return (M)new MockFile(filename,destFolder);
	}

	@Override
	public int getNumberOfFilesInFolder(String folderName) throws FileSystemException {
		checkOpen();
		MockFolder folder=folderName==null?this:getFolders().get(folderName);
		if (folder==null) {
			throw new FileSystemException("folder ["+folderName+"] is null");
		}
		Map<String,MockFile>files = folder.getFiles();
		if (files==null) {
			throw new FileSystemException("files in folder ["+folderName+"] is null");
		}
		return files.size();
	}
	@Override
	public DirectoryStream<M> listFiles(String folderName) throws FileSystemException {
		checkOpen();
		MockFolder folder = folderName==null ? this : getFolders().get(folderName);
		if (folder==null) {
			throw new FileSystemException("folder ["+folderName+"] is null");
		}
		Map<String,MockFile> files = folder.getFiles();
		if (files==null) {
			throw new FileSystemException("files in folder ["+folderName+"] is null");
		}
		List<M> fileList = new ArrayList<>();
		fileList.addAll((Collection<? extends M>) files.values());
		return FileSystemUtils.getDirectoryStream(fileList.iterator());
	}

	@Override
	public boolean exists(M f) {
		checkOpen();
		return f.getOwner()!=null
				&& (f.getOwner().getFiles().containsKey(f.getName())
						|| f.getOwner().getFolders().containsKey(f.getName()));
	}

	@Override
	public OutputStream createFile(MockFile f) throws IOException {
		checkOpen();
		f.getOwner().getFiles().put(f.getName(), f);
		return f.getOutputStream(true);
	}

	@Override
	public OutputStream appendFile(MockFile f) throws IOException {
		checkOpen();
		if (f.getOwner()!=null && f.getOwner().getFiles().containsKey(f.getName())) {
			f=f.getOwner().getFiles().get(f.getName()); // append to existing file
		} else {
			f.getOwner().getFiles().put(f.getName(), f); // create new file
			f.setOwner(this);
		}
		return f.getOutputStream(false);
	}

	@Override
	public Message readFile(MockFile f, String charset) throws FileSystemException {
		checkOpenAndExists(f);
		return new Message(f.getInputStream(), charset);
	}

	@Override
	public void deleteFile(MockFile f) throws FileSystemException {
		checkOpenAndExists(f);
		f.getOwner().getFiles().remove(f.getName());
	}

	@Override
	public M renameFile(M source, M destination) throws FileSystemException {
		checkOpenAndExists(source);
		String sourceName = source.getName();
		String destinationName = destination.getName();
		source.getOwner().getFiles().remove(sourceName);
		destination.getOwner().getFiles().put(destinationName,destination);
		destination.setContents(source.getContents());
		return destination;
	}

	@Override
	public M moveFile(M f, String destinationFolderName, boolean createFolder, boolean resultantMustBeReturned) throws FileSystemException {
		checkOpenAndExists(f);
		MockFolder destFolder= destinationFolderName==null?this:getFolders().get(destinationFolderName);
		if (destFolder==null) {
			if (!createFolder) {
				throw new FileSystemException("destination folder ["+destinationFolderName+"] does not exist");
			}
			destFolder = new MockFolder(destinationFolderName,this);
			getFolders().put(destinationFolderName,destFolder);
		}
		M destFile = (M)new MockFile(f.getName(),destFolder);
		if (destFolder.getFiles().containsKey(f.getName())) {
			throw new FileSystemException("file ["+f.getName()+"] does already exist in folder ["+destFolder+"]");
		}
		destFile.setAdditionalProperties(f.getAdditionalProperties());
		destFile.setContents(f.getContents());
		destFile.setLastModified(f.getLastModified());
		destFolder.getFiles().put(f.getName(), destFile);
		f.getOwner().getFiles().remove(f.getName());
		return destFile;
	}

	@Override
	public M copyFile(M f, String destinationFolderName, boolean createFolder, boolean resultantMustBeReturned) throws FileSystemException {
		checkOpenAndExists(f);
		MockFolder destFolder= destinationFolderName==null?this:getFolders().get(destinationFolderName);
		if (destFolder==null) {
			if (!createFolder) {
				throw new FileSystemException("folder ["+destinationFolderName+"] does not exist");
			}
			destFolder = new MockFolder(destinationFolderName,this);
			getFolders().put(destinationFolderName,destFolder);
		}
		M fileDuplicate = (M)new MockFile(f.getName(), destFolder);
		fileDuplicate.setContents(Arrays.copyOf(f.getContents(),f.getContents().length));
		if (f.getAdditionalProperties()!=null) {
			Map<String,Object> propDup = new HashMap<>();
			propDup.putAll(f.getAdditionalProperties());
			fileDuplicate.setAdditionalProperties(propDup);
		}
		fileDuplicate.setLastModified(f.getLastModified());
		destFolder.getFiles().put(fileDuplicate.getName(), fileDuplicate);
		fileDuplicate.setOwner(destFolder);
		return fileDuplicate;
	}

	@Override
	public long getFileSize(M f) throws FileSystemException {
		checkOpenAndExists(f);
		byte[] contents = f.getContents();
		return contents==null?0:contents.length;
	}

	@Override
	public String getName(M f) {
//		checkOpenAndExists(f);
		return f.getName();
	}

	@Override
	public String getParentFolder(M f) {
		return f.getOwner().getName();
	}

	@Override
	public String getCanonicalName(M f) {
		//checkOpenAndExists(null,f); // cannot check this anymore, canonical name is now used in error messages
		return f.getOwner().getName()+"/"+f.getName();
	}

	@Override
	public Date getModificationTime(M f) {
//		checkOpenAndExists(f);
		return f.getLastModified();
	}

	@Override
	public boolean folderExists(String folder) throws FileSystemException {
		return getFolders().containsKey(folder);
	}

	@Override
	public void createFolder(String folder) throws FileSystemException {
		checkOpen();
		MockFolder cur = getFolders().get(folder);
		if (cur!=null) {
			if (cur instanceof MockFolder) {
				throw new FileSystemException("Directory already exists.");
			}
			throw new FileSystemException("Entry already exists.");
		}
		MockFolder d = new MockFolder(folder,this);
		getFolders().put(folder, d);
	}

	@Override
	public void removeFolder(String folder, boolean removeNonEmptyFolder) throws FileSystemException {
		checkOpen();
		MockFolder cur = getFolders().get(folder);
		if (cur==null) {
			throw new FileSystemException("Directory does not exist.");
		}
		if (!(cur instanceof MockFolder)) {
				throw new FileSystemException("Entry is not a directory");
		}
		if(!removeNonEmptyFolder && !cur.getFiles().isEmpty() || !cur.getFolders().isEmpty()) {
			throw new FileSystemException("Cannot remove folder");
		}
		getFolders().remove(folder);
	}

	@Override
	public Map<String, Object> getAdditionalFileProperties(M f) {
		checkOpen();
		return f.getAdditionalProperties();
	}

	@Override
	public String getPhysicalDestinationName() {
		return "Mock!";
	}

}
