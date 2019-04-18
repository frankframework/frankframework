package it.nl.nn.adapterframework.senders;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.rules.TemporaryFolder;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.filesystem.FileSystemException;
import nl.nn.adapterframework.filesystem.IFileSystemTestHelper;
import nl.nn.adapterframework.filesystem.IWritableFileSystem;
import nl.nn.adapterframework.senders.AmazonS3FileSystemSender;

public class AmazonS3FileSystemTestHelper implements IFileSystemTestHelper{

	private String accessKey = "";
	private String secretKey = "";

	private boolean chunkedEncodingDisabled = false;
	private boolean accelerateModeEnabled = false; // this may involve some extra costs
	private boolean forceGlobalBucketAccessEnabled = false;

	private String bucketName = "iaf.s3sender.ali.test";
	private Regions clientRegion = Regions.EU_WEST_1;
	
	private AmazonS3 s3Client;
	private AmazonS3FileSystemSender s3FileSystemSender;
	
	public AmazonS3FileSystemTestHelper(String accessKey, String secretKey, boolean chunkedEncodingDisabled,
			boolean accelerateModeEnabled, boolean forceGlobalBucketAccessEnabled, String bucketName,
			Regions clientRegion) {
		this.accessKey = accessKey;
		this.secretKey = secretKey;
		this.chunkedEncodingDisabled = chunkedEncodingDisabled;
		this.accelerateModeEnabled = accelerateModeEnabled;
		this.forceGlobalBucketAccessEnabled = forceGlobalBucketAccessEnabled;
		this.bucketName = bucketName;
		this.clientRegion = clientRegion;
	}

	@Override
	@Before
	public void setUp() throws ConfigurationException, IOException, FileSystemException {
		s3FileSystemSender = new AmazonS3FileSystemSender();
		s3FileSystemSender.setAccessKey(accessKey);
		s3FileSystemSender.setSecretKey(secretKey);
		open();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		if(s3Client != null) {
			s3Client.shutdown();
		}
	}
	
	private void open() {
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);

		AmazonS3ClientBuilder s3ClientBuilder = AmazonS3ClientBuilder.standard()
				.withChunkedEncodingDisabled(chunkedEncodingDisabled).withAccelerateModeEnabled(accelerateModeEnabled)
				.withForceGlobalBucketAccessEnabled(forceGlobalBucketAccessEnabled)
				.withRegion(clientRegion.getName()).withCredentials(new AWSStaticCredentialsProvider(awsCreds));

		s3Client = s3ClientBuilder.build();
	}
	
	@Override
	public boolean _fileExists(String folder, String filename) {
		String objectName=folder==null?filename:folder+"/"+filename;
		return s3Client.doesObjectExist(bucketName, objectName);
	}

	@Override
	public void _deleteFile(String folder, String filename) {
		s3Client.deleteObject(bucketName, filename);
	}

	@Override
	public OutputStream _createFile(String foldername, final String filename) throws IOException {
		TemporaryFolder folder = new TemporaryFolder();
		folder.create();

		String fileName = folder.getRoot().getAbsolutePath()+"tempFile";

		final File file = new File(fileName);
		final FileOutputStream fos = new FileOutputStream(file);
		final BufferedOutputStream bos = new BufferedOutputStream(fos);
		
		FilterOutputStream filterOutputStream = new FilterOutputStream(bos) {
			@Override
			public void close() throws IOException {
				super.close();
				bos.close();

				FileInputStream fis = new FileInputStream(file);
				ObjectMetadata metaData = new ObjectMetadata();
				metaData.setContentLength(file.length());
				
				s3Client.putObject(bucketName, filename, fis, metaData);
				
				fis.close();
				file.delete();
			}
		};
		return filterOutputStream;
	}

	@Override
	public InputStream _readFile(String folder, String filename) throws FileNotFoundException {
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
	
	@Override
	public boolean _folderExists(String folderName) throws Exception {
		return _fileExists(folderName, folderName);
	}

	@Override
	public void _deleteFolder(String folderName) throws Exception {
		_deleteFile(null, folderName);
	}
}
