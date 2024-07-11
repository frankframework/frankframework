package org.frankframework.filesystem;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
public class AmazonS3FileSystemActorTest extends FileSystemActorCustomFileAttributesTest<S3Object, AmazonS3FileSystem> {

	@TempDir
	private Path tempdir;

	@Container
	private static final S3MockContainer s3Mock = new S3MockContainer("latest");

	@Override
	protected AmazonS3FileSystem createFileSystem() {
		AmazonS3FileSystemTestHelper awsHelper = (AmazonS3FileSystemTestHelper) this.helper;
		AmazonS3FileSystem s3 = new AmazonS3FileSystem() { //can't use Mockito as it mucks up the JDWP
			@Override
			public AmazonS3 createS3Client() {
				return awsHelper.getS3Client();
			}
		};

		s3.setBucketName(awsHelper.getBucketName());
		return s3;
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() throws IOException {
		return new AmazonS3FileSystemTestHelper(tempdir, s3Mock.getHttpEndpoint());
	}

	@Disabled("Amazon S3 does not support the APPEND action")
	@Test
	@Override
	public void fileSystemActorAppendActionWithRolloverBySize() throws Exception {
		//Ignore this test
	}

	@Disabled("Amazon S3 does not support the APPEND action")
	@Test
	@Override
	public void fileSystemActorAppendActionWriteLineSeparatorDisabled() throws Exception {
		//Ignore this test
	}

	@Disabled("Amazon S3 does not support the APPEND action")
	@Test
	@Override
	public void fileSystemActorAppendActionWriteLineSeparatorEnabled() throws Exception {
		//Ignore this test
	}
}
