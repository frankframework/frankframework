package org.frankframework.filesystem;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;

import io.findify.s3mock.S3Mock;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.testutil.PropertyUtil;
import org.frankframework.util.StringUtil;
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
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

public class AmazonS3FileSystemTestHelper implements IFileSystemTestHelper {

	protected String accessKey = PropertyUtil.getProperty("AmazonS3.properties", "accessKey");
	protected String secretKey = PropertyUtil.getProperty("AmazonS3.properties", "secretKey");

	protected @Getter String bucketName = PropertyUtil.getProperty("AmazonS3.properties", "bucketName");

	private final Region clientRegion = Region.EU_WEST_1;
	public static final int S3_PORT = 19090;
	private final String serviceEndpoint = "http://localhost:" + S3_PORT;
	private final boolean runLocalStub = StringUtils.isBlank(accessKey) && StringUtils.isBlank(secretKey);

	private @Getter S3Client s3Client;

	public Path tempFolder;

	private S3Mock s3Mock;

	public AmazonS3FileSystemTestHelper(Path tempFolder) {
		this.tempFolder = tempFolder;
	}

	@Override
	public void setUp() {
		if (runLocalStub) {
			s3Mock = new S3Mock.Builder().withPort(S3_PORT).withInMemoryBackend().build();
			s3Mock.start();
		}

		s3Client = createS3Client();
		if (!runLocalStub) {
			cleanUpFolder(null);
		}

		if (s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build()) != null) {
			s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
		}
	}

	//For testing purposes
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
			awsCredentials = createAwsCredentials("user", "pass");
			s3ClientBuilder.endpointOverride(new URI(serviceEndpoint)). // TODO: check how this should work with v2
					s3ClientBuilder.endpointProvider(new AwsClientBuilder.EndpointConfiguration(serviceEndpoint, clientRegion.id()));
		} else {
			awsCredentials = createAwsCredentials(accessKey, secretKey);
		}
		s3ClientBuilder.credentialsProvider(StaticCredentialsProvider.create(awsCredentials));

		return s3ClientBuilder.build();
	}

	private AwsCredentials createAwsCredentials(String accessKey, String secretKey) {
		return new AwsCredentials() {
			@Override
			public String accessKeyId() {
				return accessKey;
			}

			@Override
			public String secretAccessKey() {
				return secretKey;
			}
		};
	}

	@Override
	public void tearDown() {
		if (s3Mock != null) {
			s3Mock.shutdown();
		}
	}

	@Override
	public boolean _fileExists(String folder, String filename) {
		String objectName;
		if (filename == null) {
			objectName = folder;
		} else {
			objectName = folder == null ? filename : folder + "/" + filename;
		}
		try {
			return s3Client.headObject(HeadObjectRequest.builder().key(objectName).bucket(bucketName).build()) != null;
		} catch (AmazonS3Exception e) {
			return false;
		}
	}

	@Override
	public void _deleteFile(String folder, String filename) {
		String filePath = folder == null ? filename : folder + "/" + filename;
		s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(filePath).build());
	}

	@Override
	public OutputStream _createFile(final String folderName, final String filename) throws IOException {
		String fileName = tempFolder.toAbsolutePath() + "tempFile";

		final File file = new File(fileName);
		final FileOutputStream fos = new FileOutputStream(file);
		return new BufferedOutputStream(fos) {
			@Override
			public void close() throws IOException {
				super.close();

				FileInputStream fis = new FileInputStream(file);
				String filePath = folderName == null ? filename : folderName + "/" + filename;
				s3Client.putObject(PutObjectRequest.builder().bucket(bucketName).key(filePath).build(), RequestBody.fromInputStream(fis, file.length()));

				fis.close();
				Files.delete(file.toPath());
			}
		};
	}

	@Override
	public InputStream _readFile(String folder, String filename) {
		String path = StringUtil.concatStrings(folder, "/", filename);
		ResponseInputStream<GetObjectResponse> inputStream = s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(path).build());
		return new FilterInputStream(inputStream) {
			@Override
			public void close() throws IOException {
				super.close();
				inputStream.close();
			}
		};
	}

	@Override
	public void _createFolder(String folderNameInput) {
		String folderName = folderNameInput.endsWith("/") ? folderNameInput : folderNameInput + "/";
		PutObjectRequest request = PutObjectRequest.builder()
				.bucket(bucketName)
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

	private void cleanUpFolder(String foldername) {
		ListObjectsResponse listResponse;
		do {
			// If the bucket contains many objects, the listObjects() call might not return all the objects in the first listing. Check to see whether
			// the listing was truncated. If so, continue until all objects have been deleted.
			listResponse = s3Client.listObjects(ListObjectsRequest.builder().bucket(bucketName).prefix(foldername).build());
			List<S3Object> listObjects = listResponse.contents();
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
