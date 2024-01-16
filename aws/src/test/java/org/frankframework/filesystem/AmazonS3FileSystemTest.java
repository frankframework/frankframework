package org.frankframework.filesystem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.nio.file.Path;

import org.frankframework.testutil.PropertyUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.model.S3Object;

import org.frankframework.configuration.ConfigurationException;

public class AmazonS3FileSystemTest extends FileSystemTest<S3Object, AmazonS3FileSystem> {

	private final int waitMilis = PropertyUtil.getProperty("AmazonS3.properties", "waitTimeout", 50);

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
		s3.setBucketName(awsHelper.getBucketName());
		return s3;
	}

	@Test
	public void testConfigureAccessKey() {
		fileSystem.setAuthAlias(null);
		fileSystem.setAccessKey("123");
		fileSystem.setSecretKey(null);

		ConfigurationException e = assertThrows(ConfigurationException.class, fileSystem::configure);
		assertEquals("invalid credential fields, please prodive AWS credentials (accessKey and secretKey)", e.getMessage());
	}

	@Test
	public void testConfigureSecretKey() {
		fileSystem.setAuthAlias(null);
		fileSystem.setAccessKey(null);
		fileSystem.setSecretKey("123");

		ConfigurationException e = assertThrows(ConfigurationException.class, fileSystem::configure);
		assertEquals("invalid credential fields, please prodive AWS credentials (accessKey and secretKey)", e.getMessage());
	}

	@Test
	public void testConfigureAccessAndSecretKey() throws Exception {
		fileSystem.setAuthAlias(null);
		fileSystem.setAccessKey("123");
		fileSystem.setSecretKey("456");

		fileSystem.configure();
		AWSCredentials credentials = fileSystem.getCredentialProvider().getCredentials();
		assertEquals("123", credentials.getAWSAccessKeyId());
		assertEquals("456", credentials.getAWSSecretKey());
	}

	@Test
	public void testConfigureAuthAlias() throws Exception {
		fileSystem.setAuthAlias("alias1");

		fileSystem.configure();
		AWSCredentials credentials = fileSystem.getCredentialProvider().getCredentials();
		assertEquals("username1", credentials.getAWSAccessKeyId());
	}

	@Test
	public void testInvalidRegion() {
		fileSystem.setClientRegion("tralala");

		ConfigurationException e = assertThrows(ConfigurationException.class, fileSystem::configure);
		assertThat(e.getMessage(), startsWith("invalid region [tralala] please use"));
	}

	@Test
	public void testInvalidBucketName() {
		fileSystem.setBucketName("tr/89/**-alala");

		ConfigurationException e = assertThrows(ConfigurationException.class, fileSystem::configure);
		assertEquals("invalid or empty bucketName [tr/89/**-alala] please visit AWS to see correct bucket naming", e.getMessage());
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
