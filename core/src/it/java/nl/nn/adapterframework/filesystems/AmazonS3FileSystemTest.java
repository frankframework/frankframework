package nl.nn.adapterframework.filesystems;

import java.io.FileNotFoundException;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.junit.Ignore;
import org.junit.Test;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.filesystem.AmazonS3FileSystem;
import nl.nn.adapterframework.filesystem.FileSystemException;

public class AmazonS3FileSystemTest extends FileSystemTest<S3Object, AmazonS3FileSystem> {

	private String accessKey = "";
	private String secretKey = "";
	private boolean chunkedEncodingDisabled = false;
	private boolean accelerateModeEnabled = false; // this may involve some extra costs
	private boolean forceGlobalBucketAccessEnabled = false;
	private String bucketName = "iaf.s3sender.ali.test";
	private AmazonS3 s3Client;

	private int waitMilis = 1000;

	@Override
	public void setup() throws IOException, ConfigurationException, FileSystemException {
		super.setup();
		setWaitMilis(waitMilis);
	}

	@Override
	protected AmazonS3FileSystem getFileSystem() throws ConfigurationException {

		BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);

		AmazonS3ClientBuilder s3ClientBuilder = AmazonS3ClientBuilder.standard()
				.withChunkedEncodingDisabled(chunkedEncodingDisabled).withAccelerateModeEnabled(accelerateModeEnabled)
				.withForceGlobalBucketAccessEnabled(forceGlobalBucketAccessEnabled)
				.withRegion(Regions.EU_WEST_1.getName()).withCredentials(new AWSStaticCredentialsProvider(awsCreds));

		s3Client = s3ClientBuilder.build();
		AmazonS3FileSystem s3 = new AmazonS3FileSystem();
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
	protected OutputStream _createFile(final String filename) throws IOException, FileSystemException {
		PipedOutputStream pos = new PipedOutputStream();
		final PipedInputStream pis = new PipedInputStream(pos);
		final Thread putObjectThread = new Thread(new Runnable() {

			@Override
			public void run() {
				s3Client.putObject(bucketName, filename, pis, new ObjectMetadata());
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
		return s3Client.getObject(bucketName, filename).getObjectContent();
	}

	@Override
	public void _createFolder(String filename) throws IOException {
		s3Client.putObject(bucketName, filename, "");
	}

	@Override
	protected boolean _folderExists(String folderName) throws Exception {
		return _fileExists(folderName);
	}

	@Override
	protected void _deleteFolder(String folderName) throws Exception {
		deleteFile(folderName);
	}

	@Override
	@Ignore
	@Test
	public void testAppendFile() throws Exception {
		super.testAppendFile();
	}

}
