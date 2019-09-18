package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.fail;

import org.junit.Test;

public abstract class FileSystemActorExtraTest<F,FS extends IWritableFileSystem<F>> extends FileSystemActorTest<F, FS> {

	@Override
	protected abstract IFileSystemTestHelperFullControl getFileSystemTestHelper(); 

	@Test
	public void fileSystemActorAppendActionWithDailyRollover() throws Exception {
		fail("test needs implementation");
	}


}
