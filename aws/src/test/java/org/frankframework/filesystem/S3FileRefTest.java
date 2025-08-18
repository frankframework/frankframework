package org.frankframework.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public class S3FileRefTest {

	@ParameterizedTest
	@NullAndEmptySource
	public void testNullAndEmptyKey(String key) {
		S3FileRef ref = new S3FileRef(key, null);
		assertEquals("", ref.getKey());
		assertNull(ref.getName());
		assertFalse(ref.hasName());
	}

	@Test
	public void file() {
		S3FileRef ref = new S3FileRef("file.txt", null);
		assertEquals("file.txt", ref.getKey());
		assertEquals("file.txt", ref.getName());
		assertNull(ref.getBucketName());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"defaultBucket"})
	public void fileWithBucket(String ignoreThisBucket) {
		S3FileRef ref = new S3FileRef("bucket|file.txt", ignoreThisBucket);
		assertEquals("file.txt", ref.getKey());
		assertEquals("file.txt", ref.getName());
		assertEquals("bucket", ref.getBucketName());
	}

	@Test
	public void fileWithDefaultBucket() {
		S3FileRef ref = new S3FileRef("file.txt", "bucket");
		assertEquals("file.txt", ref.getKey());
		assertEquals("file.txt", ref.getName());
		assertEquals("bucket", ref.getBucketName());
	}

	@Test
	public void folder() {
		S3FileRef ref = new S3FileRef("test/folder/", null);
		assertEquals("test/folder/", ref.getKey());
		assertEquals("folder", ref.getName());
		assertNull(ref.getBucketName());
	}

	@Test
	public void fileInFolder() {
		S3FileRef ref = new S3FileRef("test/folder/file.txt", null);
		assertEquals("test/folder/file.txt", ref.getKey());
		assertEquals("file.txt", ref.getName());
		assertNull(ref.getBucketName());
	}

	@Test
	public void fileInFolder2WithoutSlash() {
		S3FileRef ref = new S3FileRef("file.txt", "test/folder", null);
		assertEquals("test/folder/file.txt", ref.getKey());
		assertEquals("file.txt", ref.getName());
		assertNull(ref.getBucketName());
	}

	@Test
	public void fileInFolder2WithSlash() {
		S3FileRef ref = new S3FileRef("file.txt", "test/folder/", null);
		assertEquals("test/folder/file.txt", ref.getKey());
		assertEquals("file.txt", ref.getName());
		assertNull(ref.getBucketName());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"defaultBucket"})
	public void fileInFolder2WithBucket(String ignoreThisBucket) {
		S3FileRef ref = new S3FileRef("file.txt", "bucket|test/folder", ignoreThisBucket);
		assertEquals("test/folder/file.txt", ref.getKey());
		assertEquals("file.txt", ref.getName());
		assertEquals("bucket", ref.getBucketName());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"defaultBucket"})
	public void fileInFolder2WithBucketAndSlash(String ignoreThisBucket) {
		S3FileRef ref = new S3FileRef("file.txt", "bucket|/test/folder", ignoreThisBucket);
		assertEquals("/test/folder/file.txt", ref.getKey());
		assertEquals("file.txt", ref.getName());
		assertEquals("bucket", ref.getBucketName());
	}
}
