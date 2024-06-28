/*
   Copyright 2018-2024 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.aws.AwsUtil;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.doc.Mandatory;
import org.frankframework.filesystem.utils.AmazonEncodingUtils;
import org.frankframework.stream.Message;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.FileUtils;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.StringUtil;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ProxyConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.internal.BucketUtils;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;


public class AmazonS3FileSystem extends FileSystemBase<S3Object> implements IWritableFileSystem<S3Object>, ISupportsCustomFileAttributes<S3Object> {
	private final @Getter String domain = "Amazon";

	private static final String BUCKET_OBJECT_SEPARATOR = "|";
	private static final String FILE_DELIMITER = "/";

	private @Getter String accessKey;
	private @Getter String secretKey;
	private @Getter String authAlias;

	private String serviceEndpoint = null;

	private @Getter boolean chunkedEncodingDisabled = false;
	private @Getter boolean forceGlobalBucketAccessEnabled = false;
	private @Getter Region clientRegion = Region.EU_WEST_1;

	private @Getter String bucketName;
	private @Getter String proxyHost = null;
	private @Getter Integer proxyPort = null;
	private @Getter int maxConnections = 50;

	private @Getter StorageClass storageClass = StorageClass.STANDARD;

	private S3Client s3Client;
	private AwsCredentialsProvider credentialProvider;

	@Override
	public void configure() throws ConfigurationException {
		if((StringUtils.isNotEmpty(getAccessKey()) && StringUtils.isEmpty(getSecretKey())) || (StringUtils.isEmpty(getAccessKey()) && StringUtils.isNotEmpty(getSecretKey()))) {
			throw new ConfigurationException("invalid credential fields, please provide AWS credentials (accessKey and secretKey)");
		}

		CredentialFactory cf = null;
		if (StringUtils.isNotEmpty(getAuthAlias()) || (StringUtils.isNotEmpty(getAccessKey()) && StringUtils.isNotEmpty(getSecretKey()))) {
			cf = new CredentialFactory(getAuthAlias(), getAccessKey(), getSecretKey());
		}
		credentialProvider = AwsUtil.createCredentialProviderChain(cf);

		if (getClientRegion() == null || !Region.regions().contains(getClientRegion())) {
			throw new ConfigurationException("invalid region [" + getClientRegion() + "] please use one of the following supported regions " + Region.regions());
		}

		if (StringUtils.isEmpty(getBucketName()) || BucketUtils.isValidDnsBucketName(getBucketName(), false)) {
			throw new ConfigurationException("invalid or empty bucketName [" + getBucketName() + "] please visit AWS documentation to see correct bucket naming");
		}
	}

	@Override
	public void open() throws FileSystemException {
		s3Client = createS3Client();
		super.open();
	}

	//For testing purposes
	protected AwsCredentialsProvider getCredentialProvider() {
		return credentialProvider;
	}

	public S3Client createS3Client() {
		S3Configuration.Builder s3Configuration = S3Configuration.builder()
				.chunkedEncodingEnabled(!isChunkedEncodingDisabled())
				.multiRegionEnabled(isForceGlobalBucketAccessEnabled());

		S3ClientBuilder s3ClientBuilder = S3Client.builder()
				.credentialsProvider(credentialProvider)
				.forcePathStyle(true)
				.serviceConfiguration(s3Configuration.build())
				.httpClientBuilder(getHttpClientBuilder())
				.region(getClientRegion());

		if (StringUtils.isNotBlank(serviceEndpoint)) {
			s3ClientBuilder.endpointOverride(URI.create(serviceEndpoint));
		}

		return s3ClientBuilder.build();
	}

	@Override
	public void close() throws FileSystemException {
		if (s3Client != null) {
			s3Client.close();
		}
		super.close();
	}

	/**
	 * Creates a local S3Object pointer, not representative with what is stored in the S3 Bucket.
	 * This method may be used to upload a file to S3.
	 */
	@Override
	public S3Object toFile(@Nullable String filename) {
		if (filename == null) {
			return null;
		}
		S3Object.Builder builder = S3Object.builder();
		int separatorPos = filename.indexOf(BUCKET_OBJECT_SEPARATOR);
		if (separatorPos < 0) {
			builder.key(filename);
		} else {
			builder.key(filename.substring(separatorPos + 1));
		}
		return builder.build();
	}

	@Override
	public S3Object toFile(@Nullable String folder, @Nullable String filename) {
		return toFile(StringUtil.concatStrings(folder, FILE_DELIMITER, filename));
	}

	@Override
	public int getNumberOfFilesInFolder(String folder) throws FileSystemException {
		String nextContinuationToken = null;
		int totalObjects = 0;

		String prefix;
		if (folder != null && !folder.endsWith(FILE_DELIMITER)) {
			prefix = folder + FILE_DELIMITER;
		} else prefix = folder;

		try {
			do {
				ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
						.bucket(bucketName)
						.prefix(prefix)
						.continuationToken(nextContinuationToken);

				ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());
				nextContinuationToken = response.nextContinuationToken();

				totalObjects += response.contents().stream()
						.reduce(0, (subtotal, element) -> subtotal + 1, Integer::sum);
			} while (nextContinuationToken != null && (getMaxNumberOfMessagesToList() < 0 || getMaxNumberOfMessagesToList() > totalObjects));
		} catch (AwsServiceException e) {
			throw new FileSystemException("Cannot process listObjectsV2 action", e);
		}
		return totalObjects;
	}

	@Override
	public DirectoryStream<S3Object> list(String folder, TypeFilter filter) throws FileSystemException {
		List<S3Object> s3Objects = new ArrayList<>();
		List<CommonPrefix> subFolders = new ArrayList<>();
		try {
			int totalObjects = 0;
			String nextContinuationToken = null;
			do {
				ListObjectsV2Request request = createListRequestV2(folder, nextContinuationToken);
				ListObjectsV2Response listing = s3Client.listObjectsV2(request);

				nextContinuationToken = listing.nextContinuationToken();
				if (filter.includeFiles()) {
					s3Objects.addAll(listing.contents()); // Files
					totalObjects += listing.contents().size();
				}
				if (filter.includeFolders()) {
					subFolders.addAll(listing.commonPrefixes()); // Folders
					totalObjects += listing.commonPrefixes().size();
				}
			} while (nextContinuationToken != null && (getMaxNumberOfMessagesToList() <= 0 || getMaxNumberOfMessagesToList() > totalObjects));
			if (totalObjects > getMaxNumberOfMessagesToList()) {
				log.warn("unable to list all files in S3 bucket [{}] and folder [{}]. Continued with the amount 'maxNumberOfMessagesToList'.", bucketName, folder);
			}
		} catch (AwsServiceException e) {
			throw new FileSystemException("Cannot process requested action", e);
		}

		List<S3Object> list = new ArrayList<>();
		for (CommonPrefix folderName : subFolders) {
			list.add(createS3FolderObject(folderName.prefix()));
		}
		for (S3Object summary : s3Objects) {
			if (summary.key().endsWith(FILE_DELIMITER)) { // Omit the 'search' folder
				continue;
			}
			list.add(summary);
		}

		return FileSystemUtils.getDirectoryStream(list.iterator());
	}

	private S3Object createS3FolderObject(String folderName) {
		return S3Object.builder()
				.key(folderName.endsWith(FILE_DELIMITER) ? folderName : folderName + FILE_DELIMITER)
				.size(0L)
				.build();
	}

	@Override
	public boolean exists(S3Object f) throws FileSystemException {
		try {
			return s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(f.key()).build()) != null;
		} catch (NoSuchKeyException e) {
			return false;
		} catch (AwsServiceException e) {
			throw new FileSystemException("Cannot process requested action for S3Object with key [" + f.key() + "]", e);
		}
	}

	@Override
	public boolean isFolder(S3Object s3Object) {
		return s3Object.key().endsWith(FILE_DELIMITER);
	}

	@Override
	public void createFile(S3Object f, InputStream content) throws FileSystemException, IOException {
		String folder = getParentFolder(f);
		if (folder != null && !folderExists(folder)) { //AWS Supports the creation of folders, this check is purely here so all FileSystems have the same behavior
			throw new FolderNotFoundException("folder ["+folder+"] does not exist");
		}

		// The inputStream content also be directly send to the s3Client.putObject(), when the File length is available.
		// When uploading of unknown size is needed, the S3AsyncClient or S3TransferManager can be used in the future.
		final File file = FileUtils.createTempFile(".s3-upload"); //The lesser evil to allow streaming uploads
		try(FileOutputStream fos = new FileOutputStream(file)) {
			StreamUtil.streamToStream(content, fos);
		}

		try (FileInputStream fis = new FileInputStream(file)) {
			PutObjectRequest por = PutObjectRequest.builder().bucket(bucketName).key(f.key()).storageClass(storageClass).build(); //metadata???
			s3Client.putObject(por, RequestBody.fromInputStream(fis, file.length()));
		} finally {
			Files.delete(file.toPath());
		}
	}

	@Override
	public OutputStream createFile(S3Object f) {
		throw new NotImplementedException();
	}

	@Override
	public OutputStream appendFile(S3Object f) {
		// Amazon S3 doesn't support append operation
		return null;
	}

	/**
	 * If you retrieve an S3Object, you should close this input stream as soon as possible,
	 * because the object content is not buffered in memory and streams directly from Amazon S3.
	 * Failure to close this stream can cause the request pool to become blocked.
	 */
	@Override
	public Message readFile(S3Object file, String charset) throws FileSystemException {
		try {
			GetObjectRequest objectRequest = GetObjectRequest.builder().bucket(bucketName).key(file.key()).build();
			ResponseBytes<GetObjectResponse> s3ClientObject = s3Client.getObjectAsBytes(objectRequest);// Fetch a new copy
			byte[] data = s3ClientObject.asByteArray();
			return new Message(data, FileSystemUtils.getContext(this, file, charset));
		} catch (AwsServiceException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public void deleteFile(S3Object f) throws FileSystemException {
		try {
			DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(bucketName).key(f.key()).build();
			s3Client.deleteObject(deleteObjectRequest);
		} catch (AwsServiceException e) {
			throw new FileSystemException("Could not delete object [" + getCanonicalNameOrErrorMessage(f) + "]: " + e.getMessage());
		}
	}

	private ListObjectsV2Request createListRequestV2(String folder, String continuationToken) {
		String prefix = folder != null ? FilenameUtils.normalizeNoEndSeparator(folder, true) + FILE_DELIMITER : null;
		return ListObjectsV2Request.builder()
				.bucket(bucketName)
				.delimiter(FILE_DELIMITER)
				.prefix(prefix)
				.continuationToken(continuationToken)
				.build();
	}

	@Override
	public boolean folderExists(String folder) throws FileSystemException {
		try {
			ListObjectsV2Response listing = null;
			int iterations = 0;
			do {
				ListObjectsV2Request request = createListRequestV2(folder, listing == null ? null : listing.nextContinuationToken());
				if (iterations > 20) {
					log.warn("unable to list all files in folder [{}]", folder);
					break;
				}
				listing = s3Client.listObjectsV2(request);
				if (listing.keyCount() > 0) { //If more then 1 result is returned, files also exist in this folder
					return true;
				}
				iterations++;
			} while (listing.isTruncated());
		} catch (AwsServiceException e) {
			throw new FileSystemException("Cannot process requested action", e);
		}
		return false;
	}

	// Note that S3 will create the folder asynchronously, so it may not be immediately available
	@Override
	public void createFolder(String folder) throws FileSystemException {
		String folderName = folder.endsWith(FILE_DELIMITER) ? folder : folder + FILE_DELIMITER;
		if (folderExists(folder)) {
			throw new FolderAlreadyExistsException("Create directory for [" + folderName + "] has failed. Directory already exists.");
		}
		PutObjectRequest request = PutObjectRequest.builder()
				.bucket(bucketName)
				.key(folderName)
				.build();
		s3Client.putObject(request, RequestBody.empty());
	}

	@Override
	public void removeFolder(String folder, boolean removeNonEmptyFolder) throws FileSystemException {
		if (!folderExists(folder)) {
			throw new FolderNotFoundException("Cannot remove folder [" + folder + "]. Folder does not exist.");
		}
		// Check if there are files or folders, and not allowed to remove non-empty folder
		if (!removeNonEmptyFolder) {
			try (DirectoryStream<S3Object> stream = list(folder, TypeFilter.FILES_AND_FOLDERS)) {
				if (stream.iterator().hasNext()) {
					throw new FileSystemException("Cannot remove folder [" + folder + "]. Folder not empty.");
				}
			} catch (IOException e) {
				throw new FileSystemException("Cannot remove folder [" + folder + "]. " + e.getMessage());
			}
		}

		final String absFolder = folder.endsWith(FILE_DELIMITER) ? folder : folder + FILE_DELIMITER; //Ensure it's a folder that's being removed
		s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(absFolder).build());
	}

	// rename is implemented via copy & delete
	@Override
	public S3Object renameFile(S3Object source, S3Object destination) {
		CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
				.sourceBucket(bucketName)
				.sourceKey(source.key())
				.destinationBucket(bucketName)
				.destinationKey(destination.key())
				.storageClass(getStorageClass())
				.build();
		s3Client.copyObject(copyObjectRequest);

		DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
				.bucket(bucketName)
				.key(source.key())
				.build();
		s3Client.deleteObject(deleteObjectRequest);
		return destination;
	}

	@Override
	public S3Object copyFile(S3Object s3Object, String destinationFolder, boolean createFolder) throws FileSystemException {
		if (!createFolder && !folderExists(destinationFolder)) {
			throw new FolderNotFoundException("folder ["+destinationFolder+"] does not exist");
		}
		String destinationFile = destinationFolder+FILE_DELIMITER+getName(s3Object);

		CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
				.sourceBucket(bucketName)
				.sourceKey(s3Object.key())
				.destinationBucket(bucketName)
				.destinationKey(destinationFile)
				.storageClass(getStorageClass())
				.build();
		CopyObjectResponse copyObjectResponse = s3Client.copyObject(copyObjectRequest);
		if (copyObjectResponse == null || copyObjectResponse.copyObjectResult().eTag() == null) {
			throw new FileSystemException("Could not copy object [" + getCanonicalNameOrErrorMessage(s3Object) + "]");
		}
		return toFile(destinationFile);
	}

	@Override
	// move is actually implemented via copy and delete
	public S3Object moveFile(S3Object f, String destinationFolder, boolean createFolder) {
		return renameFile(f, toFile(destinationFolder, getName(f)));
	}

	@Override
	public Map<String, Object> getAdditionalFileProperties(S3Object f) throws FileSystemException {
		Map<String, Object> attributes = new LinkedHashMap<>();
		attributes.put("bucketName", bucketName); // default attribute to identify the bucket

		try {
			GetObjectTaggingRequest taggingRequest = GetObjectTaggingRequest.builder().bucket(bucketName).key(f.key()).build();
			s3Client.getObjectTagging(taggingRequest).tagSet().forEach(tag -> attributes.put(tag.key(), AmazonEncodingUtils.rfc2047Decode(tag.value())));
		} catch (AwsServiceException e) {
			throw new FileSystemException("Could not retrieve tags for object [" + f.key() + "], message: " + e.getMessage());
		}
		return attributes;
	}

	@Override
	public String getName(S3Object f) {
		String key = f.key();
		int lastSlashPos;
		if (key.endsWith("/")) { // Folder: take part before last slash
			lastSlashPos = key.lastIndexOf('/', key.length() - 2);
		} else { // File
			lastSlashPos = key.lastIndexOf('/');
		}
		return key.substring(lastSlashPos + 1);
	}

	@Override
	public String getParentFolder(S3Object f) {
		int lastSlashPos = f.key().lastIndexOf('/');
		return lastSlashPos > 1 ? f.key().substring(0, lastSlashPos) : null;
	}

	@Override
	public String getCanonicalName(S3Object f) {
		return bucketName + BUCKET_OBJECT_SEPARATOR + f.key();
	}

	@Override
	public long getFileSize(S3Object f) {
		return f.size();
	}

	@Override
	public Date getModificationTime(S3Object f) {
		return Date.from(f.lastModified());
	}

	protected ApacheHttpClient.Builder getHttpClientBuilder() {
		ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder()
				.maxConnections(getMaxConnections());

		if (getProxyHost() != null && getProxyPort() != null) {
			httpClientBuilder.proxyConfiguration(ProxyConfiguration.builder()
					.endpoint(URI.create("https://" + getProxyHost() + ":" + getProxyPort()))
					.build());
		}
		return httpClientBuilder;
	}

	//TODO: verify if setting a new attribute will overwrite the existing ones
	@Override
	public void setCustomFileAttribute(@Nonnull S3Object f, @Nonnull String key, @Nonnull String value) {
		Tagging tag = Tagging.builder().tagSet(Tag.builder().key(key).value(AmazonEncodingUtils.rfc2047Encode(value)).build()).build();
		s3Client.putObjectTagging(PutObjectTaggingRequest.builder().bucket(bucketName).key(f.key()).tagging(tag).build());
	}

	@Override
	public String getPhysicalDestinationName() {
		return "bucket ["+getBucketName()+"]";
	}

	/** Access key to access to the AWS resources owned by the account */
	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	/** Secret key to access to the AWS resources owned by the account */
	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	/** Alias used to obtain AWS credentials  */
	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}

	/**
	 * Setting this flag will result in disabling chunked encoding for all requests.
	 * @ff.default false
	 */
	public void setChunkedEncodingDisabled(boolean chunkedEncodingDisabled) {
		this.chunkedEncodingDisabled = chunkedEncodingDisabled;
	}

	/**
	 * Option to enable or disable the usage of multi-region access point ARNs.
	 * @ff.default false
	 */
	public void setForceGlobalBucketAccessEnabled(boolean forceGlobalBucketAccessEnabled) {
		this.forceGlobalBucketAccessEnabled = forceGlobalBucketAccessEnabled;
	}

	/**
	 * The S3 service endpoint, either with or without the protocol. (e.g. https://sns.us-west-1.amazonaws.com or sns.us-west-1.amazonaws.com)
	 */
	public void setServiceEndpoint(String serviceEndpoint) {
		this.serviceEndpoint = serviceEndpoint;
	}

	/**
	 * Name of the AWS region that the client is using.
	 * @ff.default eu-west-1
	 */
	@Mandatory
	public void setClientRegion(String clientRegion) {
		this.clientRegion = Region.of(clientRegion);
	}

	/** Name of the bucket to access. The bucketName can also be specified by prefixing it to the object name, separated from it by {@value #BUCKET_OBJECT_SEPARATOR} */
	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	/** Proxy host */
	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	/** Proxy port */
	public void setProxyPort(Integer proxyPort) {
		this.proxyPort = proxyPort;
	}

	/**
	 * Set the desired storage class for the S3 object when action is move,copy or write.
	 * More info on storage classes can be found on the AWS S3 docs: https://aws.amazon.com/s3/storage-classes/
	 * @ff.default STANDARD
	 */
	public void setStorageClass(StorageClass storageClass) {
		this.storageClass = storageClass;
	}

	/**
	 * Maximum concurrent connections towards S3
	 * @ff.default 50
	 */
	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

}
