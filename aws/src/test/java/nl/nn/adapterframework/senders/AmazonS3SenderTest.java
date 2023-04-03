package nl.nn.adapterframework.senders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.amazonaws.services.s3.model.S3Object;

import nl.nn.adapterframework.filesystem.AmazonS3FileSystem;
import nl.nn.adapterframework.filesystem.AmazonS3FileSystemTestHelper;
import nl.nn.adapterframework.filesystem.FileSystemSenderTest;
import nl.nn.adapterframework.filesystem.IFileSystemTestHelper;
import nl.nn.adapterframework.testutil.PropertyUtil;


/**
 * AmazonS3Sender tests.
 *
 * @author alisihab
 *
 */
public class AmazonS3SenderTest extends FileSystemSenderTest<AmazonS3Sender, S3Object, AmazonS3FileSystem> {

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
	public AmazonS3Sender createFileSystemSender() {
		AmazonS3FileSystem s3 = spy(AmazonS3FileSystem.class);
		AmazonS3FileSystemTestHelper awsHelper = (AmazonS3FileSystemTestHelper) this.helper;
		doReturn(awsHelper.getS3Client()).when(s3).createS3Client();

		AmazonS3Sender sender = new AmazonS3Sender();
		sender.setFileSystem(s3);
		sender.setAuthAlias("dummy");
		sender.setBucketName(awsHelper.getBucketName());
		return sender;
	}

	@Test
	public void testS3FileSystemDelegator() {
		assertEquals("dummy", fileSystemSender.getFileSystem().getAuthAlias());

		fileSystemSender.setAccessKey("123");
		assertEquals("123", fileSystemSender.getFileSystem().getAccessKey());

		fileSystemSender.setSecretKey("456");
		assertEquals("456", fileSystemSender.getFileSystem().getSecretKey());

		fileSystemSender.setClientRegion("dummy-region");
		assertEquals("dummy-region", fileSystemSender.getFileSystem().getClientRegion());

		fileSystemSender.setChunkedEncodingDisabled(true);
		assertTrue(fileSystemSender.getFileSystem().isChunkedEncodingDisabled());

		fileSystemSender.setForceGlobalBucketAccessEnabled(true);
		assertTrue(fileSystemSender.getFileSystem().isForceGlobalBucketAccessEnabled());
	}

//	@Test
//	public void amazonS3SenderTestCreateBucket() throws SenderException, ConfigurationException, TimeOutException, IOException, FileSystemException {
//		fileSystemSender.setAction("createBucket");
//
//		fileSystemSender.setBucketName(bucketNameTobeCreatedAndDeleted);
//		fileSystemSender.configure();
//		fileSystemSender.getFileSystem().open();
//		String result = fileSystemSender.sendMessage(new Message(bucketNameTobeCreatedAndDeleted), null).asString();
//
//		boolean exists = ((AmazonS3FileSystemTestHelper)helper).getS3Client().doesBucketExistV2(bucketNameTobeCreatedAndDeleted);
//		assertTrue(exists);
//		assertEquals(result, bucketNameTobeCreatedAndDeleted);
//	}

//	@Test
//	public void amazonS3SenderTestRemoveBucket() throws SenderException, ConfigurationException, TimeOutException, IOException, FileSystemException {
//		fileSystemSender.setAction("deleteBucket");
//
//		fileSystemSender.setBucketName(bucketNameTobeCreatedAndDeleted);
//		fileSystemSender.configure();
//		fileSystemSender.getFileSystem().open();
//		String result = fileSystemSender.sendMessage(new Message(bucketNameTobeCreatedAndDeleted), null).asString();
//
//		boolean exists = ((AmazonS3FileSystemTestHelper)helper).getS3Client().doesBucketExistV2(bucketNameTobeCreatedAndDeleted);
//		assertFalse(exists);
//		assertEquals(bucketNameTobeCreatedAndDeleted, result);
//
//	}

//	@Test
//	public void amazonS3SenderTestCopyObjectSuccess() throws Exception {
//
//		fileSystemSender.setBucketName(bucketName);
//		fileSystemSender.setDestinationBucketName(bucketName);
//
//		fileSystemSender.setAction(FileSystemAction.COPY.toString());
//		fileSystemSender.setForceGlobalBucketAccessEnabled(true);
//		PipeLineSession session = new PipeLineSession();
//		String dest = "copiedObject.txt";
//		session.put("destinationFileName", dest);
//
////		Parameter p = new Parameter();
////		p.setName("destinationFileName");
////		p.setSessionKey("destinationFileName");
//
//		if (_fileExists(dest)) {
//			_deleteFile(null, dest);
//		}
//		String fileName = "testcopy/testCopy.txt";
//
//		Parameter param = new Parameter();
//		param.setName("destinationFileName");
//		param.setValue(dest);
//		fileSystemSender.addParameter(param);
//
//		fileSystemSender.configure();
//		fileSystemSender.getFileSystem().open();
//		S3Object objectTobeCopied = new S3Object();
//		objectTobeCopied.setKey(fileName);
//		OutputStream out = fileSystemSender.getFileSystem().createFile(objectTobeCopied);
//		out.close();
//		String result = fileSystemSender.sendMessage(new Message(fileName), session).asString();
//		assertEquals(dest, result);
//	}

}
