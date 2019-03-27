package nl.nn.adapterframework.filesystem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nl.nn.adapterframework.configuration.ConfigurationException;

public class MockFileSystemSenderTest extends FileSystemSenderTest <MockFile,MockFileSystem>{

	
	@Override
	protected MockFileSystem getFileSystem() throws ConfigurationException {
		return new MockFileSystem();
	}

	@Override
	protected boolean _fileExists(String filename) throws Exception {
		return fileSystem.getFiles().containsKey(filename);
	}

	@Override
	protected boolean _folderExists(String folderName) throws Exception {
		MockFile mf = fileSystem.getFiles().get(folderName);
		return (mf!=null && mf instanceof MockFolder);
	}

	@Override
	protected void _deleteFile(String filename) throws Exception {
		fileSystem.getFiles().remove(filename);	
	}

	@Override
	protected OutputStream _createFile(String filename) throws Exception {
		final MockFile mf = new MockFile(filename,fileSystem);
		fileSystem.getFiles().put(filename, mf);
		
		return new ByteArrayOutputStream() {

			@Override
			public void close() throws IOException {
				super.close();
				mf.setContents(toByteArray());
			}
			
		};
	}

	@Override
	protected InputStream _readFile(String filename) throws Exception {
		MockFile mf = fileSystem.getFiles().get(filename);
		if (mf==null || mf.getContents()==null) {
			return null;
		}
		return new ByteArrayInputStream( mf.getContents());
	}

	@Override
	protected void _createFolder(String filename) throws Exception {
		MockFolder mf = new MockFolder(filename,fileSystem);
		fileSystem.getFiles().put(filename,mf);
		
	}

	@Override
	protected void _deleteFolder(String folderName) throws Exception {
		fileSystem.getFiles().remove(folderName);
	}

}
