package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.LogUtil;

public class MockFileSystem<M extends MockFile> extends MockFolder implements IWritableFileSystem<M> {
	
	protected Logger log = LogUtil.getLogger(this);

	private boolean configured=false;
	private boolean opened=false;
	

	public MockFileSystem() {
		super("MOCKFILESYSTEM",null);
	}

	@Override
	public void configure() throws ConfigurationException {
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
		M result = (M)getFiles().get(filename);
		if (result!=null) {
			return result;
		}
		return (M)new MockFile(filename,this);
	}

	@Override
	public M toFile(String folderName, String filename) throws FileSystemException {
		checkOpen();
		MockFolder destFolder= folderName==null?this:getFolders().get(folderName);
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
	public Iterator<M> listFiles(String folderName) throws FileSystemException {
		checkOpen();
		MockFolder folder=folderName==null?this:getFolders().get(folderName);
		if (folder==null) {
			throw new FileSystemException("folder ["+folderName+"] is null");
		}
		Map<String,MockFile>files = folder.getFiles();
		if (files==null) {
			throw new FileSystemException("files in folder ["+folderName+"] is null");
		}
		return (Iterator<M>)files.values().iterator();
	}

	@Override
	public boolean exists(MockFile f) throws FileSystemException {
		checkOpen();
		return f.getOwner()!=null 
				&& (f.getOwner().getFiles().containsKey(f.getName()) 
						|| f.getOwner().getFolders().containsKey(f.getName()));
	}

	@Override
	public OutputStream createFile(MockFile f) throws FileSystemException, IOException {
		checkOpen();
		getFiles().put(f.getName(), f);
		f.setOwner(this);
		return f.getOutputStream(true);
	}

	@Override
	public OutputStream appendFile(MockFile f) throws FileSystemException, IOException {
		checkOpen();
		if (getOwner()!=null && getOwner().getFiles().containsKey(f.getName())) {
			f=getFiles().get(f.getName()); // append to existing file
		} else {
			getFiles().put(f.getName(), f); // create new file
			f.setOwner(this);
		}
		return f.getOutputStream(false);
	}

	@Override
	public InputStream readFile(MockFile f) throws FileSystemException, IOException {
		checkOpenAndExists(null,f);
		return f.getInputStream();
	}

	@Override
	public void deleteFile(MockFile f) throws FileSystemException {
		checkOpenAndExists(null,f);
		getFiles().remove(f.getName());
		f.setOwner(null);
	}

	@Override
	public M renameFile(M f, String newName, boolean force) throws FileSystemException {
		checkOpenAndExists(null,f);
		if (getFiles().containsKey(newName)) {
			throw new FileSystemException("Cannot rename file. Destination file already exists.");
		}
		
		getFiles().put(newName,getFiles().remove(f.getName()));
		f.setName(newName);
		return f;
	}

	@Override
	public M moveFile(M f, String destinationFolderName, boolean createFolder) throws FileSystemException {
		//checkOpenAndExists(f.getOwner().getName(),f);
		MockFolder destFolder= destinationFolderName==null?this:getFolders().get(destinationFolderName);
		if (destFolder==null) {
			if (!createFolder) {
				throw new FileSystemException("folder ["+destinationFolderName+"] does not exist");
			} 
			destFolder = new MockFolder(destinationFolderName,this);
			getFolders().put(destinationFolderName,destFolder);
		}
		destFolder.getFiles().put(f.getName(), f.getOwner().getFiles().remove(f.getName()));
		f.setOwner(destFolder);
		return f;
	}

	@Override
	public long getFileSize(M f) throws FileSystemException {
//		checkOpenAndExists(f);
		byte[] contents = f.getContents();
		return contents==null?0:contents.length;
	}

	@Override
	public String getName(M f) {
//		checkOpenAndExists(f);
		return f.getName();
	}

	@Override
	public String getCanonicalName(M f) throws FileSystemException {
		//checkOpenAndExists(null,f); // cannot check this anymore, canonical name is now used in error messages
		return f.getName();
	}

	@Override
	public Date getModificationTime(M f) throws FileSystemException {
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
	public void removeFolder(String folder) throws FileSystemException {
		checkOpen();
		MockFolder cur = getFolders().get(folder);
		if (cur==null) {
			throw new FileSystemException("Directory does not exist.");
		}
		if (!(cur instanceof MockFolder)) {
				throw new FileSystemException("Entry is not a directory");
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
