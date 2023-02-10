package nl.nn.adapterframework.filesystem;

import org.apache.commons.lang3.StringUtils;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.model.S3Object;

import nl.nn.adapterframework.senders.AmazonS3Sender;
import nl.nn.adapterframework.testutil.PropertyUtil;


/**
 * AmazonS3Sender tests.
 *
 * @author alisihab
 *
 */
public class AmazonS3SenderTest extends FileSystemSenderTest<AmazonS3Sender, S3Object, AmazonS3FileSystem> {

	protected String PROPERTY_FILE = "AmazonS3.properties";

	private boolean chunkedEncodingDisabled = false;
	private boolean accelerateModeEnabled = false; // this may involve some extra costs
	private boolean forceGlobalBucketAccessEnabled = false;

	protected String accessKey    = PropertyUtil.getProperty(PROPERTY_FILE, "accessKey");
	protected String secretKey    = PropertyUtil.getProperty(PROPERTY_FILE, "secretKey");
	protected String bucketName   = PropertyUtil.getProperty(PROPERTY_FILE, "bucketName");
	protected String proxyHost    = PropertyUtil.getProperty(PROPERTY_FILE, "proxyHost");
	protected int proxyPort;

	private Regions clientRegion = Regions.EU_WEST_1;

	private int waitMilis = 1000;

	{
		setWaitMillis(waitMilis);
		if (StringUtils.isNotEmpty(PropertyUtil.getProperty(PROPERTY_FILE, "proxyPort"))) {
			proxyPort = Integer.parseInt(PropertyUtil.getProperty(PROPERTY_FILE, "proxyPort"));
		}
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new AmazonS3FileSystemTestHelper(accessKey, secretKey, chunkedEncodingDisabled, accelerateModeEnabled,
				forceGlobalBucketAccessEnabled, bucketName, clientRegion);
	}

	@Override
	public AmazonS3Sender createFileSystemSender(){
		AmazonS3Sender s3 = new AmazonS3Sender();
		s3.setAccessKey(accessKey);
		s3.setSecretKey(secretKey);
		s3.setBucketName(bucketName);
		s3.setProxyHost(proxyHost);
		s3.setProxyPort(proxyPort);
		return s3;
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
