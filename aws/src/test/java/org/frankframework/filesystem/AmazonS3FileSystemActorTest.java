package org.frankframework.filesystem;

import java.nio.file.Path;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Object;

public class AmazonS3FileSystemActorTest extends FileSystemActorCustomFileAttributesTest<S3Object, AmazonS3FileSystem> {

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

		s3.setBucketName(awsHelper.getBucketName());
		return s3;
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new AmazonS3FileSystemTestHelper(tempDir);
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
