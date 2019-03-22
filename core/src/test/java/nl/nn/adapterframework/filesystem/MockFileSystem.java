package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.LogUtil;

public class MockFileSystem implements IFileSystem<MockFile> {
	protected Logger log = LogUtil.getLogger(this);

	private boolean configured=false;
	private boolean opened=false;
	
	private Map<String,MockFile> files = new HashMap<String,MockFile>();
	
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

	private void checkOpenAndExists(MockFile f) throws FileSystemException {
		checkOpen();
		if (!files.containsKey(f.getName())) {
			throw new FileSystemException("Source file ["+f.getName()+"] does not exist in filesystem");
		}
	}

	@Override
	public MockFile toFile(String filename) throws FileSystemException {
		checkOpen();
		MockFile result = files.get(filename);
		if (result!=null) {
			return result;
		}
		return new MockFile(filename);
	}

	@Override
	public Iterator<MockFile> listFiles() throws FileSystemException {
		checkOpen();
		return files.values().iterator();
	}

	@Override
	public boolean exists(MockFile f) throws FileSystemException {
		checkOpen();
		return files.containsKey(f.getName());
	}

	@Override
	public OutputStream createFile(MockFile f) throws FileSystemException, IOException {
		checkOpen();
		files.put(f.getName(), f);
		return f.getOutputStream(true);
	}

	@Override
	public OutputStream appendFile(MockFile f) throws FileSystemException, IOException {
		checkOpen();
		if (files.containsKey(f.getName())) {
			f=files.get(f.getName());
		} else {
			files.put(f.getName(), f);
		}
		return f.getOutputStream(false);
	}

	@Override
	public InputStream readFile(MockFile f) throws FileSystemException, IOException {
		checkOpenAndExists(f);
		return f.getInputStream();
	}

	@Override
	public void deleteFile(MockFile f) throws FileSystemException {
		checkOpenAndExists(f);
		files.remove(f.getName());
	}

	@Override
	public void renameTo(MockFile f, String destination) throws FileSystemException {
		checkOpenAndExists(f);
		if (files.containsKey(destination)) {
			throw new FileSystemException("Cannot rename file. Destination file already exists.");
		}
		
		files.put(destination,files.remove(f.getName()));
		f.setName(destination);
	}

	@Override
	public long getFileSize(MockFile f, boolean isFolder) throws FileSystemException {
//		checkOpenAndExists(f);
		byte[] contents = f.getContents();
		return contents==null?0:contents.length;
	}

	@Override
	public String getName(MockFile f) throws FileSystemException {
//		checkOpenAndExists(f);
		return f.getName();
	}

	@Override
	public String getCanonicalName(MockFile f, boolean isFolder) throws FileSystemException {
		checkOpenAndExists(f);
		return f.getName();
	}

	@Override
	public Date getModificationTime(MockFile f, boolean isFolder) throws FileSystemException {
//		checkOpenAndExists(f);
		return f.getLastModified();
	}

	@Override
	public boolean isFolder(MockFile f) throws FileSystemException {
//		checkOpenAndExists(f);
		return f instanceof MockFolder;
	}

	@Override
	public void createFolder(MockFile f) throws FileSystemException {
		checkOpen();
		MockFile cur = files.get(f.getName());
		if (cur!=null) {
			if (cur instanceof MockFolder) {
				throw new FileSystemException("Directory already exists.");
			}
			throw new FileSystemException("Entry already exists.");
		}
		MockFolder d = new MockFolder(f.getName());
		files.put(f.getName(), d);
	}

	@Override
	public void removeFolder(MockFile f) throws FileSystemException {
		checkOpen();
		MockFile cur = files.get(f.getName());
		if (cur==null) {
			throw new FileSystemException("Directory does not exist.");
		}
		if (!(cur instanceof MockFolder)) {
				throw new FileSystemException("Entry is not a directory");
		}
		files.remove(f.getName());
	}

	@Override
	public Map<String, Object> getAdditionalFileProperties(MockFile f) {
		checkOpen();
		// TODO Auto-generated method stub
		return null;
	}

	public Map<String, MockFile> getFiles() {
		return files;
	}
}
