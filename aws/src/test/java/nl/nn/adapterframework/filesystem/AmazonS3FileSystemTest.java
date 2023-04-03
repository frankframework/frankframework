package nl.nn.adapterframework.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.nio.file.Path;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.amazonaws.services.s3.model.S3Object;

import nl.nn.adapterframework.testutil.PropertyUtil;

public class AmazonS3FileSystemTest extends FileSystemTest<S3Object, AmazonS3FileSystem> {

	private int waitMilis = PropertyUtil.getProperty("AmazonS3.properties", "waitTimeout", 50);

	{
		setWaitMillis(waitMilis);
	}

	@TempDir
	private Path tempdir;

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new AmazonS3FileSystemTestHelper(tempdir);
	}

	@Override
	public AmazonS3FileSystem createFileSystem() {
		AmazonS3FileSystem s3 = spy(AmazonS3FileSystem.class);
		AmazonS3FileSystemTestHelper awsHelper = (AmazonS3FileSystemTestHelper) this.helper;
		doReturn(awsHelper.getS3Client()).when(s3).createS3Client();
		s3.setAuthAlias("dummy");
		s3.setBucketName(awsHelper.getBucketName());
		return s3;
	}

	@Disabled
	@Test
	@Override
	public void writableFileSystemTestAppendExistingFile() throws Exception {
		super.writableFileSystemTestAppendExistingFile();
	}

	@Disabled
	@Test
	@Override
	public void writableFileSystemTestAppendNewFile() throws Exception {
		super.writableFileSystemTestAppendNewFile();
	}

	@Disabled
	@Test
	@Override
	public void writableFileSystemTestDeleteAppendedFile() throws Exception{
		super.writableFileSystemTestDeleteAppendedFile();
	}

	@Disabled // atomic move is not implemented. It could be possible to arrange this using ObjectLock.LegalHold
	@Test
	@Override
	public void basicFileSystemTestMoveFileMustFailWhenTargetAlreadyExists() throws Exception {
		super.basicFileSystemTestMoveFileMustFailWhenTargetAlreadyExists();
	}

	@Test
	public void testToFileWithBucketnameInFilename() throws FileSystemException {
		// arrange
		String filename="fakeFile";
		String bucketname="fakeBucket";

		String combinedFilename = bucketname +"|" + filename;

		// act
		S3Object ref = fileSystem.toFile(combinedFilename);

		// assert
		assertEquals(bucketname, ref.getBucketName());
		assertEquals(filename, ref.getKey());
	}

	@Test
	public void testToFileWithBucketnameInFilenameWithFolder() throws FileSystemException {
		// arrange
		String foldername="fakeFolder";
		String filename="fakeFile";
		String bucketname="fakeBucket";

		String combinedFilename = bucketname +"|" + foldername +"/"+ filename;

		// act
		S3Object ref = fileSystem.toFile(combinedFilename);

		// assert
		assertEquals(bucketname, ref.getBucketName());
		assertEquals(foldername +"/"+ filename, ref.getKey());
	}
}
