package nl.nn.adapterframework.filesystem;

import java.util.Properties;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.model.S3Object;

import nl.nn.adapterframework.senders.AmazonS3Sender;
import nl.nn.adapterframework.util.ClassUtils;


/**
 * AmazonS3Sender tests.
 * 
 * @author alisihab
 *
 */
public class AmazonS3SenderTest extends FileSystemSenderTest<AmazonS3Sender, S3Object, AmazonS3FileSystem> {

	public static final String AMAZONS3_PROPERTIES = "amazonS3.properties";
	private static Properties properties;
	
	@Rule
	public TestName name = new TestName();
	
	private String accessKey = "";
	private String secretKey = "";
	private String proxyHost = null;
	private Integer proxyPort = null;

	private boolean chunkedEncodingDisabled = false;
	private boolean accelerateModeEnabled = false; // this may involve some extra costs
	private boolean forceGlobalBucketAccessEnabled = false;

	private String bucketName = UUID.randomUUID().toString();//"iaf.s3sender.ali.test";
//	private String bucketNameTobeCreatedAndDeleted = "bucket-name-tobe-created-and-deleted";
	private Regions clientRegion = Regions.EU_WEST_1;
	
	private int waitMilis = 1000;

	{
		setWaitMillis(waitMilis);
	}
	
	private void setAttributesFromPropertiesFile() throws Exception {
		try {
			if (properties == null) {
				properties = new Properties();
			}
			properties.load(ClassUtils.getResourceURL(AMAZONS3_PROPERTIES).openStream());	 
			accessKey = properties.getProperty("accessKey");
			secretKey = properties.getProperty("secretKey");
			proxyHost = properties.getProperty("proxyHost");
			if (StringUtils.isNotEmpty(properties.getProperty("proxyPort"))) {
				proxyPort = Integer.parseInt(properties.getProperty("proxyPort"));
			}
		} catch (Exception e) {
			log.error("There was an error reading propertie file: {} ", e.getMessage());
			throw e;
		}
	}	
	
	@Override
	@Before
	public void setUp() throws Exception {
		setAttributesFromPropertiesFile();
		super.setUp();
	}
	
	@Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();
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
