package org.frankframework.senders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.nio.file.Path;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.adobe.testing.s3mock.testcontainers.S3MockContainer;

import org.frankframework.filesystem.AmazonS3FileSystem;
import org.frankframework.filesystem.AmazonS3FileSystemTestHelper;
import org.frankframework.filesystem.FileSystemActor.FileSystemAction;
import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.filesystem.S3FileRef;
import org.frankframework.filesystem.TypeFilter;
import org.frankframework.filesystem.WritableFileSystemSenderTest;
import org.frankframework.stream.Message;
import org.frankframework.testutil.ParameterBuilder;
import org.frankframework.testutil.PropertyUtil;
import org.frankframework.util.StreamUtil;

/**
 * AmazonS3Sender tests.
 *
 * @author alisihab
 *
 */
@Testcontainers(disabledWithoutDocker = true)
@Tag("integration") // Requires Docker; exclude with '-DexcludedGroups=integration'
public class AmazonS3SenderTest extends WritableFileSystemSenderTest<AmazonS3Sender, S3FileRef, AmazonS3FileSystem> {

	@Container
	private static final S3MockContainer s3Mock = new S3MockContainer("latest");

	private final int waitMillis = PropertyUtil.getProperty("AmazonS3.properties", "waitTimeout", 50);

	{
		setWaitMillis(waitMillis);
	}

	@TempDir
	private Path tempdir;

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new AmazonS3FileSystemTestHelper(tempdir, s3Mock.getHttpEndpoint());
	}

	@Override
	public AmazonS3Sender createFileSystemSender() {
		AmazonS3FileSystem s3 = spy(AmazonS3FileSystem.class);
		AmazonS3FileSystemTestHelper awsHelper = (AmazonS3FileSystemTestHelper) this.helper;
		doReturn(awsHelper.getS3Client()).when(s3).createS3Client();

		AmazonS3Sender sender = new AmazonS3Sender();
		sender.setFileSystem(s3);
		sender.setBucketName(awsHelper.getDefaultBucketName());
		return sender;
	}

	@Test
	public void testS3FileSystemDelegator() {
		fileSystemSender.setAuthAlias("dummy");
		assertEquals("dummy", fileSystemSender.getFileSystem().getAuthAlias());

		fileSystemSender.setAccessKey("123");
		assertEquals("123", fileSystemSender.getFileSystem().getAccessKey());

		fileSystemSender.setSecretKey("456");
		assertEquals("456", fileSystemSender.getFileSystem().getSecretKey());

		fileSystemSender.setClientRegion("dummy-region");
		assertEquals("dummy-region", fileSystemSender.getFileSystem().getClientRegion().toString());

		fileSystemSender.setChunkedEncodingDisabled(true);
		assertTrue(fileSystemSender.getFileSystem().isChunkedEncodingDisabled());

		fileSystemSender.setForceGlobalBucketAccessEnabled(true);
		assertTrue(fileSystemSender.getFileSystem().isForceGlobalBucketAccessEnabled());
	}

	@Test
	public void fileSystemSenderTestReadMultipleTimes() throws Exception {
		// Arrange
		String filename = FILE1;
		String inputFolder = "read";

		if(_folderExists(inputFolder)) {
			_deleteFolder(inputFolder);
		}
		_createFolder(inputFolder);

		createFile(inputFolder, filename, "some content");

		waitForActionToFinish();

		fileSystemSender.addParameter(ParameterBuilder.create("filename", inputFolder +"/"+ filename));
		fileSystemSender.setAction(FileSystemAction.READ);
		fileSystemSender.configure();
		fileSystemSender.start();

		// Act
		assertTrue(_fileExists(inputFolder, filename), "File ["+filename+"] expected to be present");

		Message message = new Message("not-used");
		Message result = fileSystemSender.sendMessageOrThrow(message, session);
		waitForActionToFinish();

		// Assert
		assertTrue(_fileExists(inputFolder, FILE1), "File ["+FILE1+"] should still be there after READ action");
		assertEquals("some content", StreamUtil.streamToString(result.asInputStream()));
		// Read a 2nd time -- this should now work.
		assertEquals("some content", StreamUtil.streamToString(result.asInputStream()));
	}

	@Test
	public void fileSystemSenderTestListFolders() throws Exception {
		fileSystemSender.setAction(FileSystemAction.LIST);
		fileSystemSender.setTypeFilter(TypeFilter.FOLDERS_ONLY);

		fileSystemSender.configure();
		fileSystemSender.start();

		createFile("OtherFolder", "OtherFile", "Content of the file!");

		result = fileSystemSender.sendMessageOrThrow(new Message(""), session);

		assertFolderCountEquals(result, 1);
		assertFileCountEquals(result, 0);
	}

}
