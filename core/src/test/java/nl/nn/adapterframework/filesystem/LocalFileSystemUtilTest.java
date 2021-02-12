package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.Ignore;
import org.junit.rules.TemporaryFolder;
// needs to be fixed
@Ignore
public class LocalFileSystemUtilTest extends FileSystemUtilsTest <Path,LocalFileSystem>{

	@Override
	protected LocalFileSystem createFileSystem() {
		return new LocalFileSystem();
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() throws IOException {
		TemporaryFolder temp = new TemporaryFolder();
		temp.create();
		return new LocalFileSystemTestHelper(temp);
	}
}
