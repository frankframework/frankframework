package nl.nn.adapterframework.filesystem.mock;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.filesystem.FileSystemActorExtraTest;
import nl.nn.adapterframework.filesystem.IFileSystemTestHelperFullControl;
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

	@Test
	@Override
	@Disabled("does not support throwing exceptions by attempting to remove non empty folder.")
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
		result = actor.doAction(message, pvl, session);
		String stringResult = result.asString();
		assertThat(stringResult, containsString(normalizedfFilename));
	}

}
