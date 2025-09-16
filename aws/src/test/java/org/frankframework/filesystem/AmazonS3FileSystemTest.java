package org.frankframework.filesystem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.adobe.testing.s3mock.testcontainers.S3MockContainer;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.stream.Message;
import org.frankframework.testutil.PropertyUtil;
import org.frankframework.testutil.ThrowingAfterCloseInputStream;

@Testcontainers(disabledWithoutDocker = true)
@Tag("integration") // Requires Docker; exclude with '-DexcludedGroups=integration'
public class AmazonS3FileSystemTest extends FileSystemTest<S3FileRef, AmazonS3FileSystem> {

	private static final int WAIT_TIMEOUT_MILLIS = PropertyUtil.getProperty("AmazonS3.properties", "waitTimeout", 50);

	@Container
	private static final S3MockContainer s3Mock = new S3MockContainer("latest");

	@TempDir
	private Path tempdir;

	@Override
	protected AmazonS3FileSystemTestHelper getFileSystemTestHelper() {
		return new AmazonS3FileSystemTestHelper(tempdir, s3Mock.getHttpEndpoint());
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

		s3.setBucketName(awsHelper.getDefaultBucketName());
		return s3;
	}

	@Test
	public void assertMoveToSameDirectoryNotPossible() throws Exception {
		String filename = "file.txt";
		String contents = "regeltje tekst";

		fileSystem.configure();
		fileSystem.open();

		String rootFolder = null;
		createFile(rootFolder, filename, contents);

		waitForActionToFinish();

		S3FileRef file = fileSystem.toFile(filename);

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> fileSystem.moveFile(file, rootFolder, false));
		assertEquals("Cannot rename/move a file to the same name: [file.txt]", e.getMessage());
	}

	@Test
	public void writableFileSystemTestRenameToOtherFolderWithBucketNamePrefix() throws Exception {
		String destinationFile = "fileRenamed.txt";
		String bucketName = "other-bucket123";
		String filename = bucketName+"|"+destinationFile;
		String contents = "regeltje tekst";

		fileSystem.configure();
		fileSystem.open();

		AmazonS3FileSystemTestHelper awsHelper = (AmazonS3FileSystemTestHelper) helper;
		S3Client s3Client = awsHelper.getS3Client();
		try {
			s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());

			assertFalse(awsHelper._fileExistsInBucket(destinationFile, bucketName)); // Actual check to see if the file is there or not
			assertFalse(fileSystem.exists(new S3FileRef(destinationFile, bucketName))); // Extra sanity check that this also works correctly

			S3FileRef file = fileSystem.toFile(filename);
			assertEquals(bucketName, file.getBucketName());

			fileSystem.createFile(file, new ThrowingAfterCloseInputStream(new ByteArrayInputStream(contents.getBytes())));
			waitForActionToFinish();

			// test
			assertFalse(awsHelper._fileExistsInBucket(destinationFile, awsHelper.getDefaultBucketName()));
			assertFalse(fileSystem.exists(new S3FileRef(destinationFile, awsHelper.getDefaultBucketName())));
			assertTrue(awsHelper._fileExistsInBucket(destinationFile, bucketName));
			assertTrue(fileSystem.exists(new S3FileRef(destinationFile, bucketName)));
		} finally {
			try {
				awsHelper.cleanUpFolder(bucketName, null);
				s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
			} catch (Exception e) {
				log.error("unable to remove bucket", e);
			}
		}
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
		assertEquals("invalid or empty bucketName [tr/89/**-alala] please visit AWS documentation to see correct bucket naming", e.getMessage());
	}

	@Test
	@Override
	public void writableFileSystemTestCreateNewFile() throws Exception {
		super.writableFileSystemTestCreateNewFile();

		// After the 'normal' test, also verify the content-type
		String filename = "create" + FILE1;
		S3FileRef file = fileSystem.toFile(filename);
		Message result = fileSystem.readFile(file, null);
		assertNotNull(result.getContext().getMimeType(), "no mimetype present");
		assertEquals("text/plain", result.getContext().getMimeType().toString());
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
		S3FileRef ref = fileSystem.toFile(combinedFilename);

		// assert
		assertEquals(bucketname, ref.getBucketName());
		assertEquals(filename, ref.getKey());
	}

	@Test
	public void testToFileWithBucketnameInFilenameWithFolder() {
		// arrange
		String foldername="fakeFolder";
		String filename="fakeFile";
		String bucketname="fakeBucket";

		String combinedFilename = bucketname +"|" + foldername +"/"+ filename;

		// act
		S3FileRef ref = fileSystem.toFile(combinedFilename);

		// assert
		assertEquals(bucketname, ref.getBucketName());
		assertEquals(foldername +"/"+ filename, ref.getKey());
	}

	@Test
	public void basicFileSystemTestGetCanonicalFileName() throws Exception {
		String filename = "readName" + FILE1;
		String contents = "Tekst om te lezen";

		fileSystem.configure();
		fileSystem.open();

		createFile(null, filename, contents);
		waitForActionToFinish();

		S3FileRef file = fileSystem.toFile(filename);
		// test
		assertEquals(file.getBucketName()+"|"+filename, fileSystem.getCanonicalName(file));
	}

	@Test
	public void basicFileSystemTestGetCanonicalFolderName() throws Exception {
		String foldername = "dummy/folder/";

		fileSystem.configure();
		fileSystem.open();

		_createFolder(foldername);
		waitForActionToFinish();

		S3FileRef file = fileSystem.toFile(foldername);
		// test
		assertEquals(file.getBucketName()+"|"+foldername, fileSystem.getCanonicalName(file));
	}
}
