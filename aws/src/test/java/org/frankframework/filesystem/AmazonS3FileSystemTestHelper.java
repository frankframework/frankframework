package org.frankframework.filesystem;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.testutil.PropertyUtil;
import org.frankframework.util.StringUtil;

import lombok.Getter;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

public class AmazonS3FileSystemTestHelper implements IFileSystemTestHelper {

	protected String accessKey = PropertyUtil.getProperty("AmazonS3.properties", "accessKey");
	protected String secretKey = PropertyUtil.getProperty("AmazonS3.properties", "secretKey");

	protected @Getter String defaultBucketName = PropertyUtil.getProperty("AmazonS3.properties", "bucketName");

	private final Region clientRegion = Region.EU_WEST_1;
	private final String serviceEndpoint;
	private final boolean runLocalStub = StringUtils.isBlank(accessKey) && StringUtils.isBlank(secretKey);

	private @Getter S3Client s3Client;

	public Path tempFolder;

	public AmazonS3FileSystemTestHelper(Path tempFolder, String serviceEndpoint) {
		this.tempFolder = tempFolder;
		this.serviceEndpoint = serviceEndpoint;
	}

	@Override
	public void setUp() {
		s3Client = createS3Client();

		try {
			// Test whether the bucket exists
			s3Client.headBucket(HeadBucketRequest.builder().bucket(defaultBucketName).build());

			// If the bucket exists, clean up the folder
			cleanUpFolder(null);
		} catch (NoSuchBucketException noSuchBucketException) {
			// Create the bucket if it doesn't exist
			s3Client.createBucket(CreateBucketRequest.builder().bucket(defaultBucketName).build());
		}
	}

	/**
	 * @return a configured S3Client based on the settings found in AmazonS3.properties or a local testcontainer instance if not present
	 */
	public S3Client createS3Client() {
		S3Configuration.Builder s3Configuration = S3Configuration.builder()
				.chunkedEncodingEnabled(true)
				.multiRegionEnabled(false);

		S3ClientBuilder s3ClientBuilder = S3Client.builder()
				.forcePathStyle(true)
				.serviceConfiguration(s3Configuration.build())
				.httpClientBuilder(ApacheHttpClient.builder().socketTimeout(Duration.ofMillis(1000L)).connectionTimeout(Duration.ofMillis(1000L)))
				.region(clientRegion);

		AwsCredentials awsCredentials;
		if (runLocalStub) {
			awsCredentials = AwsBasicCredentials.create("user", "pass");

			s3ClientBuilder.endpointOverride(URI.create(serviceEndpoint));
		} else {
			awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
		}
		s3ClientBuilder.credentialsProvider(StaticCredentialsProvider.create(awsCredentials));

		return s3ClientBuilder.build();
	}

	@Override
	public void tearDown() {
		// not needed
	}

	@Override
	public boolean _fileExists(String folder, String filename) {
		String objectName;
		if (filename == null) {
			objectName = folder;
		} else {
			objectName = folder == null ? filename : folder + "/" + filename;
		}
		return _fileExistsInBucket(objectName, defaultBucketName);
	}

	public boolean _fileExistsInBucket(String objectName, String bucketName) {
		try {
			HeadObjectResponse headObjectResponse = s3Client.headObject(HeadObjectRequest.builder()
					.key(objectName)
					.bucket(bucketName)
					.build());
			return headObjectResponse != null;
		} catch (NoSuchKeyException e) {
			return false;
		}
	}

	@Override
	public void _deleteFile(String folder, String filename) {
		String filePath = folder == null ? filename : folder + "/" + filename;
		s3Client.deleteObject(DeleteObjectRequest.builder().bucket(defaultBucketName).key(filePath).build());
	}

	@Override
	public String createFile(final String folderName, final String filename, String contents) throws IOException {
		String filePath = folderName == null ? filename : folderName + "/" + filename;

		s3Client.putObject(PutObjectRequest.builder()
				.bucket(defaultBucketName)
				.key(filePath)
				.metadata(Map.of("Content-Length", ""+contents.length()))
				.build(), RequestBody.fromString(contents));

		return filePath;
	}

	@Override
	public InputStream _readFile(String folder, String filename) {
		String path = StringUtil.concatStrings(folder, "/", filename);
		ResponseInputStream<GetObjectResponse> inputStream = s3Client.getObject(GetObjectRequest.builder()
				.bucket(defaultBucketName)
				.key(path)
				.build());

		// Workaround for https://github.com/aws/aws-sdk-java-v2/issues/3538
		if (inputStream.response().contentLength() == 0) {
			inputStream = new ResponseInputStream<>(inputStream.response(), InputStream.nullInputStream());
		}

		ResponseInputStream<GetObjectResponse> finalInputStream = inputStream;

		return new FilterInputStream(finalInputStream) {
			@Override
			public void close() throws IOException {
				super.close();
				finalInputStream.close();
			}
		};
	}

	@Override
	public void _createFolder(String folderNameInput) {
		String folderName = folderNameInput.endsWith("/") ? folderNameInput : folderNameInput + "/";
		PutObjectRequest request = PutObjectRequest.builder()
				.bucket(defaultBucketName)
				.key(folderName)
				.build();
		s3Client.putObject(request, RequestBody.empty());
	}

	@Override
	public boolean _folderExists(String folderName) {
		String foldername = folderName.endsWith("/") ? folderName : folderName + "/";
		return _fileExists(foldername, null);
	}

	@Override
	public void _deleteFolder(String folderName) throws Exception {
		String folder = null;
		if (folderName != null) {
			folder = folderName.endsWith("/") ? folderName : folderName + "/";
		}
		cleanUpFolder(folder);
	}

	public void cleanUpFolder(String foldername) {
		cleanUpFolder(defaultBucketName, foldername);
	}

	public void cleanUpFolder(String bucketName, String foldername) {
		ListObjectsResponse listResponse;
		do {
			// If the bucket contains many objects, the listObjects() call might not return all the objects in the first listing. Check to see whether
			// the listing was truncated. If so, continue until all objects have been deleted.
			listResponse = s3Client.listObjects(ListObjectsRequest.builder().bucket(bucketName).prefix(foldername).build());
			List<S3Object> listObjects = listResponse.contents();

			if (listObjects == null || listObjects.isEmpty()) {
				return;
			}

			List<ObjectIdentifier> objectsToDelete = new ArrayList<>();
			for (S3Object s3Object : listObjects) {
				objectsToDelete.add(ObjectIdentifier.builder().key(s3Object.key()).build());
			}

			DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
					.bucket(bucketName)
					.delete(Delete.builder().objects(objectsToDelete).build())
					.build();

			s3Client.deleteObjects(deleteObjectsRequest);
		} while (listResponse.isTruncated());
	}
}
