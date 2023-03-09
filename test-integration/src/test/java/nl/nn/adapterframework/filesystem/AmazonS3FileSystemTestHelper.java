package nl.nn.adapterframework.filesystem;

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
import java.nio.file.Path;
import java.util.Iterator;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.Misc;

public class AmazonS3FileSystemTestHelper implements IFileSystemTestHelper{

	private String accessKey;
	private String secretKey;
	private String proxyHost = null;
	private Integer proxyPort = null;
	private boolean chunkedEncodingDisabled;
	private boolean accelerateModeEnabled; // this may involve some extra costs
	private boolean forceGlobalBucketAccessEnabled;
	private String bucketName;
	private Regions clientRegion;
	private AmazonS3 s3Client;

	public Path tempFolder;

	public AmazonS3FileSystemTestHelper(Path tempFolder, String accessKey, String secretKey, boolean chunkedEncodingDisabled,
			boolean accelerateModeEnabled, boolean forceGlobalBucketAccessEnabled, String bucketName,
			Regions clientRegion) {
		this.tempFolder = tempFolder;
		this.accessKey = accessKey;
		this.secretKey = secretKey;
		this.chunkedEncodingDisabled = chunkedEncodingDisabled;
		this.accelerateModeEnabled = accelerateModeEnabled;
		this.forceGlobalBucketAccessEnabled = forceGlobalBucketAccessEnabled;
		this.bucketName = bucketName;
		this.clientRegion = clientRegion;
	}

	@Override
	public void setUp() throws ConfigurationException, IOException, FileSystemException {
		open();
		if (!s3Client.doesBucketExist(bucketName)) {
			s3Client.createBucket(bucketName);
		}
	}

	@Override
	public void tearDown() throws Exception {
		cleanUpBucketAndShutDown(s3Client);
	}

	public void cleanUpBucketAndShutDown(AmazonS3 s3Client) {
		if(s3Client.doesBucketExistV2(bucketName)) {
			cleanUpFolder(null);
			//s3Client.deleteBucket(bucketName);
		}
		if(s3Client != null) {
			s3Client.shutdown();
		}
	}

	private void open() {
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
		AmazonS3ClientBuilder s3ClientBuilder = AmazonS3ClientBuilder.standard()
				.withChunkedEncodingDisabled(chunkedEncodingDisabled)
				.withAccelerateModeEnabled(accelerateModeEnabled)
				.withForceGlobalBucketAccessEnabled(forceGlobalBucketAccessEnabled)
				.withRegion(clientRegion.getName())
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds))
				.withClientConfiguration(this.getProxyConfig());
		s3Client = s3ClientBuilder.build();
	}

	@Override
	public boolean _fileExists(String folder, String filename) {
		String objectName;
		if(filename == null) {
			objectName = folder;
		}else {
			objectName = folder == null ? filename : folder +"/"+ filename;
		}
		return s3Client.doesObjectExist(bucketName, objectName);
	}

	@Override
	public void _deleteFile(String folder, String filename) {
		String filePath = folder == null ? filename : folder +"/" + filename;
		s3Client.deleteObject(bucketName, filePath);
	}

	@Override
	public OutputStream _createFile(final String foldername, final String filename) throws IOException {

		String fileName = tempFolder.toAbsolutePath()+"tempFile";

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
				String filePath = foldername == null ? filename : foldername + "/" + filename;
				s3Client.putObject(bucketName, filePath, fis, metaData);

				fis.close();
				file.delete();
			}
		};
		return filterOutputStream;
	}

	@Override
	public InputStream _readFile(String folder, String filename) throws FileNotFoundException {
		String path = Misc.concatStrings(folder, "/", filename);
		final S3Object file = s3Client.getObject(bucketName, path);
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
	public void _createFolder(String folderName) throws IOException {
		String foldername = folderName.endsWith("/") ? folderName : folderName +"/";
		s3Client.putObject(bucketName, foldername, "");
	}

	@Override
	public boolean _folderExists(String folderName) throws Exception {
		String foldername = folderName.endsWith("/") ? folderName : folderName + "/";
		return _fileExists(foldername, null);
	}

	@Override
	public void _deleteFolder(String folderName) throws Exception {
		String foldername = folderName.endsWith("/") ? folderName : folderName + "/";
		cleanUpFolder(foldername);
	}

	private void cleanUpFolder(String foldername) {
		ObjectListing objectListing = foldername!=null ? s3Client.listObjects(bucketName, foldername) : s3Client.listObjects(bucketName);
		while (true) {
			Iterator<S3ObjectSummary> objIter = objectListing.getObjectSummaries().iterator();
			while (objIter.hasNext()) {
				s3Client.deleteObject(bucketName, objIter.next().getKey());
			}

			// If the bucket contains many objects, the listObjects() call
			// might not return all of the objects in the first listing. Check to
			// see whether the listing was truncated. If so, retrieve the next page of objects
			// and delete them.
			if (objectListing.isTruncated()) {
				objectListing = s3Client.listNextBatchOfObjects(objectListing);
			} else {
				break;
			}
		}
	}


	public AmazonS3 getS3Client() {
		return s3Client;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public Integer getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(Integer proxyPort) {
		this.proxyPort = proxyPort;
	}

	public ClientConfiguration getProxyConfig() {
		ClientConfiguration proxyConfig = null;
		if (this.getProxyHost() != null && this.getProxyPort() != null) {
			proxyConfig = new ClientConfiguration();
			proxyConfig.setProtocol(Protocol.HTTPS);
			proxyConfig.setProxyHost(this.getProxyHost());
			proxyConfig.setProxyPort(this.getProxyPort());
		}
		return proxyConfig;
	}


}
