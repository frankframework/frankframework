package it.nl.nn.adapterframework.senders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.filesystem.AmazonS3FileSystem;
import nl.nn.adapterframework.filesystem.FileSystemSenderTest;
import nl.nn.adapterframework.filesystem.IFileSystemTestHelper;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.senders.AmazonS3Sender;


/**
 * AmazonS3Sender tests.
 * 
 * @author alisihab
 *
 */
public class AmazonS3SenderTest extends FileSystemSenderTest<S3Object, AmazonS3FileSystem> {

	@Rule
	public TestName name = new TestName();
	
	private String accessKey = "";
	private String secretKey = "";

	private boolean chunkedEncodingDisabled = false;
	private boolean accelerateModeEnabled = false; // this may involve some extra costs
	private boolean forceGlobalBucketAccessEnabled = false;

	private String bucketName = UUID.randomUUID().toString();//"iaf.s3sender.ali.test";
	private String bucketNameTobeCreatedAndDeleted = "bucket-name-tobe-created-and-deleted";
	private Regions clientRegion = Regions.EU_WEST_1;
	
	private AmazonS3 s3Client;
	private AmazonS3Sender s3FileSystemSender;
	
	private int waitMilis = 1000;

	{
		setWaitMillis(waitMilis);
	}
	
	@Override
	@Before
	public void setUp() throws Exception {
		if (name.getMethodName().startsWith("amazonS3Sender")) {
			s3FileSystemSender = new AmazonS3Sender();
			s3FileSystemSender.setAccessKey(accessKey);
			s3FileSystemSender.setSecretKey(secretKey);
			BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
	
			AmazonS3ClientBuilder s3ClientBuilder = AmazonS3ClientBuilder.standard()
					.withChunkedEncodingDisabled(chunkedEncodingDisabled).withAccelerateModeEnabled(accelerateModeEnabled)
					.withForceGlobalBucketAccessEnabled(forceGlobalBucketAccessEnabled)
					.withRegion(clientRegion.getName()).withCredentials(new AWSStaticCredentialsProvider(awsCreds));
	
			s3Client = s3ClientBuilder.build();
		}
		super.setUp();
	}
	
	@Override
	@After
	public void tearDown() throws Exception {
		if(s3Client != null) {
			((AmazonS3FileSystemTestHelper)helper).cleanUpBucketAndShutDown(s3Client);
		}
		super.tearDown();
	}
	
	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new AmazonS3FileSystemTestHelper(accessKey, secretKey, chunkedEncodingDisabled, accelerateModeEnabled, forceGlobalBucketAccessEnabled, bucketName, clientRegion);
	}
	
	@Override
	public AmazonS3Sender createFileSystemSender(){
		AmazonS3Sender s3 = new AmazonS3Sender();
		s3.setAccessKey(accessKey);
		s3.setSecretKey(secretKey);
		s3.setBucketName(bucketName);
		return s3;
	}

	@Test
	public void amazonS3SenderTestCreateBucket() throws SenderException, ConfigurationException, TimeOutException {
		s3FileSystemSender.setAction("createBucket");

		s3FileSystemSender.setBucketName(bucketNameTobeCreatedAndDeleted);
		s3FileSystemSender.configure();
		s3FileSystemSender.getFileSystem().open();
		String result = s3FileSystemSender.sendMessage("<result>ok</result>", bucketNameTobeCreatedAndDeleted);

		boolean exists = s3Client.doesBucketExistV2(bucketNameTobeCreatedAndDeleted);
		assertTrue(exists);
		assertEquals(result, bucketNameTobeCreatedAndDeleted);
	}

	@Test
	public void amazonS3SenderTestRemoveBucket() throws SenderException, ConfigurationException, TimeOutException {
		s3FileSystemSender.setAction("deleteBucket");

		s3FileSystemSender.setBucketName(bucketNameTobeCreatedAndDeleted);
		s3FileSystemSender.configure();
		s3FileSystemSender.getFileSystem().open();
		String result = s3FileSystemSender.sendMessage("<result>ok</result>", bucketNameTobeCreatedAndDeleted);

		boolean exists = s3Client.doesBucketExistV2(bucketNameTobeCreatedAndDeleted);
		assertFalse(exists);
		assertEquals(bucketNameTobeCreatedAndDeleted, result);

	}

	@Test
	public void amazonS3SenderTestCopyObjectSuccess() throws Exception {

		s3FileSystemSender.setBucketName(bucketName);
		s3FileSystemSender.setDestinationBucketName(bucketName);

		s3FileSystemSender.setAction("copy");
		s3FileSystemSender.setForceGlobalBucketAccessEnabled(true);
		PipeLineSessionBase session = new PipeLineSessionBase();
		String dest = "copiedObject.txt";
		session.put("destinationFileName", dest);

		Parameter p = new Parameter();
		p.setName("destinationFileName");
		p.setSessionKey("destinationFileName");

		ParameterResolutionContext prc = new ParameterResolutionContext();
		prc.setSession(session);
		if (_fileExists(dest)) {
			_deleteFile(null, dest);
		}
		String fileName = "testcopy/testCopy.txt";

		Parameter param = new Parameter();
		param.setName("destinationFileName");
		param.setValue(dest);
		s3FileSystemSender.addParameter(param);
		
		s3FileSystemSender.configure();
		s3FileSystemSender.getFileSystem().open();
		S3Object objectTobeCopied = new S3Object();
		objectTobeCopied.setKey(fileName);
		OutputStream out = s3FileSystemSender.getFileSystem().createFile(objectTobeCopied);
		out.close();
		String result = s3FileSystemSender.sendMessage("", fileName, prc);
		assertEquals(dest, result);
	}

}
