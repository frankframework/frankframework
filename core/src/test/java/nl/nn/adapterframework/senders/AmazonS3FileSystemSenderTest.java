package nl.nn.adapterframework.senders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.filesystem.AmazonS3FileSystem;
import nl.nn.adapterframework.filesystem.FileSystemException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AmazonS3FileSystemSenderTest
		extends FileSystemSenderTest<S3Object, AmazonS3FileSystem> {

	private String accessKey = "";
	private String secretKey = "";

	private boolean chunkedEncodingDisabled = false;
	private boolean accelerateModeEnabled = false; // this may involve some extra costs
	private boolean forceGlobalBucketAccessEnabled = false;

	private String bucketName = "iaf.s3sender.ali.test";
	private String bucketNameTobeCreatedAndDeleted = "bucket-name-tobe-created-and-deleted";
	AmazonS3FileSystem s3;
	AmazonS3 s3Client;
	AmazonS3FileSystemSender s3FileSystemSender;

	@Override
	@Before
	public void setup() throws ConfigurationException, IOException, FileSystemException {
		s3FileSystemSender = new AmazonS3FileSystemSender();
		s3FileSystemSender.setAccessKey(accessKey);
		s3FileSystemSender.setSecretKey(secretKey);
		s3FileSystemSender.setBucketName(bucketName);
		super.setup();
	}

	@Override
	protected AmazonS3FileSystem getFileSystem() throws ConfigurationException {
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);

		AmazonS3ClientBuilder s3ClientBuilder = AmazonS3ClientBuilder.standard()
				.withChunkedEncodingDisabled(chunkedEncodingDisabled)
				.withAccelerateModeEnabled(accelerateModeEnabled)
				.withForceGlobalBucketAccessEnabled(forceGlobalBucketAccessEnabled)
				.withRegion(Regions.EU_WEST_1.getName())
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds));

		s3Client = s3ClientBuilder.build();
		s3 = new AmazonS3FileSystem();
		s3.setAccessKey(accessKey);
		s3.setSecretKey(secretKey);
		s3.setBucketName(bucketName);
		return s3;
	}

	@Override
	protected boolean _fileExists(String filename) {
		return s3Client.doesObjectExist(bucketName, filename);
	}

	@Override
	protected void _deleteFile(String filename) {
		s3Client.deleteObject(bucketName, filename);
	}

	@Override
	protected OutputStream _createFile(final String filename) throws IOException {
		PipedOutputStream pos = new PipedOutputStream();
		final PipedInputStream pis = new PipedInputStream(pos);
		Thread putObjectThread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					s3Client.putObject(bucketName, filename, pis, new ObjectMetadata());
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					pis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		putObjectThread.start();
		FilterOutputStream fos = new FilterOutputStream(pos) {
			@Override
			public void close() throws IOException {
				super.close();
			}
		};
		return fos;
	}

	@Override
	protected InputStream _readFile(String filename) throws FileNotFoundException {
		final S3Object file = s3Client.getObject(bucketName, filename);
		InputStream is = file.getObjectContent();
		FilterInputStream fos = new FilterInputStream(is) {
			@Override
			public void close() throws IOException {
				super.close();
				file.close();
			}
		};

		return fos;
	}

	@Override
	public void _createFolder(String filename) throws IOException {
		s3Client.putObject(bucketName, filename, "");
	}

	@Test
	public void atestCreateBucket()
			throws SenderException, ConfigurationException, TimeOutException {
		s3FileSystemSender.setAction("createBucket");

		s3FileSystemSender.setBucketName(bucketNameTobeCreatedAndDeleted);
		s3FileSystemSender.configure();
		String result = s3FileSystemSender.sendMessage("<result>ok</result>",
				bucketNameTobeCreatedAndDeleted);

		boolean exists = s3Client.doesBucketExistV2(bucketNameTobeCreatedAndDeleted);
		assertTrue(exists);
		assertEquals(result, bucketNameTobeCreatedAndDeleted);
	}

	@Test
	public void btestRemoveBucket()
			throws SenderException, ConfigurationException, TimeOutException {
		s3FileSystemSender.setAction("deleteBucket");

		s3FileSystemSender.setBucketName(bucketNameTobeCreatedAndDeleted);
		s3FileSystemSender.configure();
		String result = s3FileSystemSender.sendMessage("<result>ok</result>",
				bucketNameTobeCreatedAndDeleted);

		boolean exists = s3Client.doesBucketExistV2(bucketNameTobeCreatedAndDeleted);
		assertFalse(exists);
		assertEquals(bucketNameTobeCreatedAndDeleted, result);

	}

	@Test
	public void copyObjectSuccess()
			throws ConfigurationException, SenderException, TimeOutException {

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

		String fileName = "testcopy/testCopy.txt";

		Parameter param = new Parameter();
		param.setName("destinationFileName");
		param.setValue("copiedObject.txt");
		s3FileSystemSender.addParameter(param);

		s3FileSystemSender.configure();
		String result = s3FileSystemSender.sendMessage("", fileName, prc);
		assertEquals(dest, result);
	}

	@Override
	@Ignore
	@Test
	public void testAppendFile() throws Exception {
		// Ignored because S3 does not support append.
		super.testAppendFile();
	}

	@Override
	protected boolean _folderExists(String folderName) throws Exception {
		return _fileExists(folderName);
	}

	@Override
	protected void _deleteFolder(String folderName) throws Exception {
		deleteFile(folderName);
	}

}
