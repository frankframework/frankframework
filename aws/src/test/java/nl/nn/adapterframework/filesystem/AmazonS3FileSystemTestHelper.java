package nl.nn.adapterframework.filesystem;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import io.findify.s3mock.S3Mock;
import lombok.Getter;
import nl.nn.adapterframework.testutil.PropertyUtil;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.StringUtil;

public class AmazonS3FileSystemTestHelper implements IFileSystemTestHelper {

	protected String accessKey = PropertyUtil.getProperty("AmazonS3.properties", "accessKey");
	protected String secretKey = PropertyUtil.getProperty("AmazonS3.properties", "secretKey");

	protected @Getter String bucketName = PropertyUtil.getProperty("AmazonS3.properties", "bucketName");

	private Regions clientRegion = Regions.EU_WEST_1;
	public static final int S3_PORT = 19090;
	private String serviceEndpoint = "http://localhost:"+S3_PORT;
	private boolean runLocalStub = StringUtils.isBlank(accessKey) && StringUtils.isBlank(secretKey);

	private @Getter AmazonS3 s3Client;

	public Path tempFolder;

	private S3Mock s3Mock;

	public AmazonS3FileSystemTestHelper(Path tempFolder) {
		this.tempFolder = tempFolder;
	}

	@Override
	public void setUp() throws Exception {
		if(runLocalStub) {
			s3Mock = new S3Mock.Builder().withPort(S3_PORT).withInMemoryBackend().build();
			s3Mock.start();
		}

		s3Client = createS3Client();

		if (!s3Client.doesBucketExistV2(bucketName)) {
			s3Client.createBucket(bucketName);
		}
	}

	//For testing purposes
	private AmazonS3 createS3Client() {
		AmazonS3ClientBuilder s3ClientBuilder = AmazonS3ClientBuilder.standard()
				.withChunkedEncodingDisabled(false)
				.withForceGlobalBucketAccessEnabled(false)
				.withClientConfiguration(new ClientConfiguration().withSocketTimeout(1000).withConnectionTimeout(1000))
				.enablePathStyleAccess();

		BasicAWSCredentials awsCreds;
		if(runLocalStub) {
			awsCreds = new BasicAWSCredentials("user", "pass");
			s3ClientBuilder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(serviceEndpoint, clientRegion.getName()));
		} else {
			awsCreds = new BasicAWSCredentials(accessKey, secretKey);
			s3ClientBuilder.withRegion(clientRegion);
		}
		s3ClientBuilder.withCredentials(new AWSStaticCredentialsProvider(awsCreds));

		return s3ClientBuilder.build();
	}

	@Override
	public void tearDown() throws Exception {
		if(s3Mock != null) {
			s3Mock.shutdown();
		}
	}

	@Override
	public boolean _fileExists(String folder, String filename) {
		String objectName;
		if(filename == null) {
			objectName = folder;
		}else {
			objectName = folder == null ? filename : folder +"/"+ filename;
		}
		try {
			return s3Client.doesObjectExist(bucketName, objectName);
		} catch(AmazonS3Exception e) {
			return false;
		}
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
		return new BufferedOutputStream(fos) {

			@Override
			public void close() throws IOException {
				super.close();

				FileInputStream fis = new FileInputStream(file);
				ObjectMetadata metaData = new ObjectMetadata();
				metaData.setContentLength(file.length());
				String filePath = foldername == null ? filename : foldername + "/" + filename;
				s3Client.putObject(bucketName, filePath, fis, metaData);

				fis.close();
				file.delete();
			}
		};
	}

	@Override
	public InputStream _readFile(String folder, String filename) throws FileNotFoundException {
		String path = StringUtil.concatStrings(folder, "/", filename);
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
		String folder = null;
		if(folderName != null) {
			folder = folderName.endsWith("/") ? folderName : folderName + "/";
		}
		cleanUpFolder(folder);
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
}
