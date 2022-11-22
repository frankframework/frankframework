package nl.nn.adapterframework.filesystem;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.filesystem.FileSystemActor.FileSystemAction;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;

public class MockFileSystemActorTest extends FileSystemActorExtraTest <MockFile,MockFileSystem<MockFile>>{


	@Override
	protected IFileSystemTestHelperFullControl getFileSystemTestHelper() {
		return new MockFileSystemTestHelper<MockFile>();
	}

	@Override
	protected MockFileSystem<MockFile> createFileSystem() {
		return ((MockFileSystemTestHelper<MockFile>)helper).getFileSystem();
	}

	@Ignore("does not support throwing exceptions by attempting to remove non empty folder.")
	@Override
	@Test
	public void fileSystemActorDeleteActionWithDeleteEmptyFolderRootContainsEmptyFoldersTest() throws Exception {

	}

	@Test
	public void testListStrangeFilenames() throws Exception {
		String filename = "list" + FILE1+"\tx\r\ny";
		String contents = "regeltje tekst";
		String normalizedfFilename="listfile1.txt x y";


		actor.setAction(FileSystemAction.LIST);
		actor.configure(fileSystem,null,owner);
		actor.open();

		createFile(null, filename, contents);
		waitForActionToFinish();

		Message message = new Message("");
		PipeLineSession session = new PipeLineSession();
		ParameterValueList pvl = null;
		String result = actor.doAction(message, pvl, session).getResult().asString();
		assertThat(result, containsString(normalizedfFilename));
	}

}
