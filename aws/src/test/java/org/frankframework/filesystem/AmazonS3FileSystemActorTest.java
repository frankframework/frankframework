package org.frankframework.filesystem;

import java.nio.file.Path;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.adobe.testing.s3mock.testcontainers.S3MockContainer;

import software.amazon.awssdk.services.s3.S3Client;

@Testcontainers(disabledWithoutDocker = true)
public class AmazonS3FileSystemActorTest extends FileSystemActorCustomFileAttributesTest<S3FileRef, AmazonS3FileSystem> {

	@Container
	private static final S3MockContainer s3Mock = new S3MockContainer("latest");

	@TempDir
	private Path tempDir;

	@Override
	protected AmazonS3FileSystem createFileSystem() {
		AmazonS3FileSystemTestHelper awsHelper = (AmazonS3FileSystemTestHelper) this.helper;
		AmazonS3FileSystem s3 = new AmazonS3FileSystem() { //can't use Mockito as it mucks up the JDWP
			@Override
			public S3Client createS3Client() {
				return awsHelper.getS3Client();
			}
		};

		s3.setBucketName(awsHelper.getDefaultBucketName());
		return s3;
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new AmazonS3FileSystemTestHelper(tempDir, s3Mock.getHttpEndpoint());
	}

	@Disabled("Amazon S3 does not support the APPEND action")
	@Test
	@Override
	public void fileSystemActorAppendActionWithRolloverBySize() {
		//Ignore this test
	}

	@Disabled("Amazon S3 does not support the APPEND action")
	@Test
	@Override
	public void fileSystemActorAppendActionWriteLineSeparatorDisabled() {
		//Ignore this test
	}

	@Disabled("Amazon S3 does not support the APPEND action")
	@Test
	@Override
	public void fileSystemActorAppendActionWriteLineSeparatorEnabled() {
		//Ignore this test
	}
}
