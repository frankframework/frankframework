package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.io.TempDir;

public class LocalFileSystemUtilTest extends FileSystemUtilsTest <Path,LocalFileSystem>{

	@TempDir
	Path temp;

	@Override
	protected LocalFileSystem createFileSystem() {
		LocalFileSystem result=new LocalFileSystem();
		result.setRoot(temp.toString());
		return result;
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() throws IOException {
		return new LocalFileSystemTestHelper(temp);
	}
}
