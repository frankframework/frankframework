package org.frankframework.filesystem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.testutil.PropertyUtil;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Object;

public class AmazonS3FileSystemTest extends FileSystemTest<S3Object, AmazonS3FileSystem> {

	private static final int WAIT_TIMEOUT_MILLIS = PropertyUtil.getProperty("AmazonS3.properties", "waitTimeout", 50);

	@TempDir
	private Path tempdir;

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new AmazonS3FileSystemTestHelper(tempdir);
	}

	@Override
	public AmazonS3FileSystem createFileSystem() {
		setWaitMillis(WAIT_TIMEOUT_MILLIS);
		AmazonS3FileSystemTestHelper awsHelper = (AmazonS3FileSystemTestHelper) this.helper;
		AmazonS3FileSystem s3 = new AmazonS3FileSystem() {
			@Override
			public S3Client createS3Client() {
				return awsHelper.createS3Client();
			}
		};

		s3.setBucketName(awsHelper.getBucketName());
		return s3;
	}

	@Test
	public void testConfigureAccessKey() {
		fileSystem.setAuthAlias(null);
		fileSystem.setAccessKey("123");
		fileSystem.setSecretKey(null);

		ConfigurationException e = assertThrows(ConfigurationException.class, fileSystem::configure);
		assertEquals("invalid credential fields, please provide AWS credentials (accessKey and secretKey)", e.getMessage());
	}

	@Test
	public void testConfigureSecretKey() {
		fileSystem.setAuthAlias(null);
		fileSystem.setAccessKey(null);
		fileSystem.setSecretKey("123");

		ConfigurationException e = assertThrows(ConfigurationException.class, fileSystem::configure);
		assertEquals("invalid credential fields, please provide AWS credentials (accessKey and secretKey)", e.getMessage());
	}

	@Test
	public void testConfigureAccessAndSecretKey() throws Exception {
		fileSystem.setAuthAlias(null);
		fileSystem.setAccessKey("123");
		fileSystem.setSecretKey("456");

		fileSystem.configure();
		AwsCredentials credentials = fileSystem.getCredentialProvider().resolveCredentials();
		assertEquals("123", credentials.accessKeyId());
		assertEquals("456", credentials.secretAccessKey());
	}

	@Test
	public void testConfigureAuthAlias() throws Exception {
		fileSystem.setAuthAlias("alias1");

		fileSystem.configure();
		AwsCredentials credentials = fileSystem.getCredentialProvider().resolveCredentials();
		assertEquals("username1", credentials.accessKeyId());
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
	public void testToFileWithBucketnameInFilename() {
		// arrange
		String filename="fakeFile";
		String bucketname="fakeBucket";

		String combinedFilename = bucketname +"|" + filename;

		// act
		S3Object f = fileSystem.toFile(combinedFilename);

		// assert
		assertEquals(filename, f.key());
	}

	@Test
	public void testToFileWithBucketnameInFilenameWithFolder() {
		// arrange
		String foldername="fakeFolder";
		String filename="fakeFile";
		String bucketname="fakeBucket";

		String combinedFilename = bucketname +"|" + foldername +"/"+ filename;

		// act
		S3Object ref = fileSystem.toFile(combinedFilename);

		// assert
		assertEquals(foldername +"/"+ filename, ref.key());
	}
}
