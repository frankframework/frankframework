package nl.nn.adapterframework.filesystem.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import nl.nn.adapterframework.filesystem.IFileSystemTestHelperFullControl;

public class MockFileSystemTestHelper<F extends MockFile> implements IFileSystemTestHelperFullControl {

	private MockFileSystem<F> fileSystem;

	protected MockFileSystemTestHelper(MockFileSystem<F> fileSystem) {
		this.fileSystem=fileSystem;
	}

	public MockFileSystemTestHelper() {
		this(new MockFileSystem<F>());
	}

	@BeforeEach
	@Override
	public void setUp() throws Exception {
		// not necessary
	}

	@AfterEach
	@Override
	public void tearDown() throws Exception {
		// not necessary
	}

	@Override
	public boolean _fileExists(String folderName, String filename) throws Exception {
		if (folderName==null) {
			return fileSystem.getFiles().containsKey(filename);
		}
		MockFolder folder = (MockFolder)fileSystem.getFolders().get(folderName);
		return folder!=null && folder.getFiles().containsKey(filename);
	}

	@Override
	public boolean _folderExists(String folderName) throws Exception {
		MockFile mf = fileSystem.getFolders().get(folderName);
		return (mf!=null && mf instanceof MockFolder);
	}

	@Override
	public void _deleteFile(String folderName, String filename) throws Exception {
		MockFolder folder = folderName==null?fileSystem:fileSystem.getFolders().get(folderName);
		if (folder==null) {
			return;
		}
		folder.getFiles().remove(filename);
	}

	protected F createNewFile(MockFolder folder, String filename) {
		return (F)new MockFile(filename,folder);
	}

	@Override
	public OutputStream _createFile(String folderName, String filename) throws Exception {
		MockFolder folder = folderName==null?fileSystem:fileSystem.getFolders().get(folderName);
		if (folder==null) {
			folder=new MockFolder(folderName,fileSystem);
			fileSystem.getFolders().put(folderName,folder);
		}
		final MockFile mf = createNewFile(folder,filename);

		//log.debug("created file ["+filename+"] in folder ["+folderName+"]");

		folder.getFiles().put(filename, mf);

		return new ByteArrayOutputStream() {

			@Override
			public void close() throws IOException {
				super.close();
				mf.setContents(toByteArray());
			}

		};
	}

	@Override
	public InputStream _readFile(String folderName, String filename) throws Exception {
		MockFolder folder = folderName==null?fileSystem:fileSystem.getFolders().get(folderName);
		if (folder==null) {
			return null;
		}
		MockFile mf = folder.getFiles().get(filename);
		if (mf==null || mf.getContents()==null) {
			return null;
		}
		return new ByteArrayInputStream( mf.getContents());
	}


	@Override
	public void setFileDate(String folderName, String filename, Date modifiedDate) throws Exception {
		MockFolder folder = folderName==null?fileSystem:fileSystem.getFolders().get(folderName);
		if (folder==null) {
			throw new IllegalStateException("folder ["+folderName+"] for file ["+filename+"] does not exist");
		}
		MockFile mf = folder.getFiles().get(filename);
		if (mf==null || mf.getContents()==null) {
			throw new IllegalStateException("file ["+filename+"] in folder ["+folderName+"] does not exist");
		}
		mf.setLastModified(modifiedDate);
	}


	@Override
	public void _createFolder(String filename) throws Exception {
		MockFolder mf = new MockFolder(filename,fileSystem);
		fileSystem.getFolders().put(filename,mf);
	}

	@Override
	public void _deleteFolder(String folderName) throws Exception {
		fileSystem.getFolders().remove(folderName);
	}

	public MockFileSystem<F> getFileSystem() {
		return fileSystem;
	}
}
