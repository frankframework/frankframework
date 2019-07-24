package nl.nn.adapterframework.filesystem;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import nl.nn.adapterframework.receivers.LocalFileSystemListener;

public class DirectoryListenerTest extends FileSystemListenerTest {

	public TemporaryFolder folder;
	
	private final boolean testForCompatibilityOfLocalFileSystemListenerToReplaceDirectoryListener=true;
	
	@Override
	public IFileSystemListener createFileSystemListener() {
		IFileSystemListener result;
		if (testForCompatibilityOfLocalFileSystemListenerToReplaceDirectoryListener) {
			LocalFileSystemListener localFileSystemListener=new LocalFileSystemListener();
			localFileSystemListener.setInputDirectory(folder.getRoot().getAbsolutePath());
			result=localFileSystemListener;
		} else {
			DirectoryListenerWrapper directoryListener=new DirectoryListenerWrapper();
			directoryListener.setInputDirectory(folder.getRoot().getAbsolutePath());
			directoryListener.setWildcard("*");
			result=directoryListener;
		}
		fileAndFolderPrefix=folder.getRoot().getAbsolutePath()+"\\";
		testFullErrorMessages=false;
		return result;
	}
	
	@Override
	public void setUp() throws Exception {
		folder = new TemporaryFolder();
		folder.create();
		super.setUp();
	}
	
	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new LocalFileSystemTestHelper(folder);
	}

	@Test
	@Ignore("messageType=contents not supported by original DirectoryListener")
	public void fileListenerTestGetStringFromRawMessageContents() throws Exception {
		super.fileListenerTestGetStringFromRawMessageContents();
	}

	@Test
	@Ignore("create of inProcessFolder not supported by original DirectoryListener")
	public void fileListenerTestCreateInProcessFolder() throws Exception {
		super.fileListenerTestCreateInProcessFolder();
	}

	@Test
	@Ignore("create of processedFolder not supported by original DirectoryListener")
	public void fileListenerTestCreateProcessedFolder() throws Exception {
		super.fileListenerTestCreateProcessedFolder();
	}

}
