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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.annotation.Nullable;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseInputStream;
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
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.StorageClass;

import org.frankframework.aws.AwsUtil;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.doc.Mandatory;
import org.frankframework.filesystem.utils.AmazonEncodingUtils;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.StringUtil;

public class AmazonS3FileSystem extends AbstractFileSystem<S3FileRef> implements IWritableFileSystem<S3FileRef>, ISupportsCustomFileAttributes<S3FileRef> {
	private final @Getter String domain = "Amazon";

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

	private TypeFilter typeFilter = TypeFilter.FILES_ONLY;

	public AmazonS3FileSystem() {
		super();
	}

	public void setTypeFilter(TypeFilter typeFilter) {
		this.typeFilter = typeFilter;
	}

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

		if (StringUtils.isEmpty(getBucketName()) || !BucketUtils.isValidDnsBucketName(getBucketName(), false)) {
			throw new ConfigurationException("invalid or empty bucketName [" + getBucketName() + "] please visit AWS documentation to see correct bucket naming");
		}
	}

	@Override
	public void open() throws FileSystemException {
		s3Client = createS3Client();
		super.open();
	}

	// For testing purposes
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
	public S3FileRef toFile(@Nullable String filename) {
		return new S3FileRef(filename, bucketName);
	}

	@Override
	public S3FileRef toFile(@Nullable String folder, @Nullable String filename) {
		return toFile(StringUtil.concatStrings(folder, FILE_DELIMITER, filename));
	}

	@Override
	public int getNumberOfFilesInFolder(String folder) throws FileSystemException {
		try (DirectoryStream<S3FileRef> files = list(folder, TypeFilter.FILES_ONLY)) {
			return Math.toIntExact(StreamSupport.stream(files.spliterator(), false).count());
		} catch (IOException e) {
			throw new FileSystemException("Exception while counting number of files in [" + folder + "]. " + e.getMessage());
		}
	}

	@Override
	public DirectoryStream<S3FileRef> list(String folder, TypeFilter filter) throws FileSystemException {
		List<S3Object> files = new ArrayList<>();
		List<CommonPrefix> subFolders = new ArrayList<>();

		try {
			ListObjectsV2Request.Builder request = createListRequestV2(folder);
			ListObjectsV2Response listing;
			int iterations = 0;

			do {
				if (iterations > 20) {
					log.warn("unable to list all files in folder [{}]", folder);
					break;
				}

				listing = s3Client.listObjectsV2(request.build());
				if (filter.includeFiles()) {
					files.addAll(listing.contents()); // Files
				}
				if (filter.includeFolders()) {
					subFolders.addAll(listing.commonPrefixes()); // Folders
				}

				request.continuationToken(listing.nextContinuationToken());
				iterations++;
			} while (listing.isTruncated());
		} catch (AwsServiceException e) {
			throw new FileSystemException("Cannot process requested action", e);
		}

		List<S3FileRef> list = new ArrayList<>();
		for (CommonPrefix folderName : subFolders) {
			list.add(createS3FolderObject(bucketName, folderName.prefix()));
		}
		for (S3Object summary : files) {
			if (summary.key().endsWith(FILE_DELIMITER)) { // Omit the 'search' folder
				continue;
			}
			list.add(new S3FileRef(summary, bucketName));
		}

		return FileSystemUtils.getDirectoryStream(list.iterator());
	}

	private static S3FileRef createS3FolderObject(String bucketName, String folderName) {
		return new S3FileRef(null, folderName, bucketName);
	}

	@Override
	public boolean exists(S3FileRef f) throws FileSystemException {
		try {
			return getFileAttributes(f) != null;
		} catch (NoSuchKeyException e) {
			return false;
		} catch (AwsServiceException e) {
			throw new FileSystemException("Cannot process requested action for S3Object with key [" + f.getKey() + "]", e);
		}
	}

	@Override
	public boolean isFolder(S3FileRef s3Object) {
		return s3Object.getKey().endsWith(FILE_DELIMITER);
	}

	@Override
	public void createFile(S3FileRef f, InputStream content) throws FileSystemException, IOException {
		createFile(f, content, Collections.emptyMap());
	}

	@Override
	public void createFile(S3FileRef f, InputStream content, Map<String, String> customFileAttributes) throws FileSystemException, IOException {
		String folder = getParentFolder(f);
		if (folder != null && !folderExists(folder)) {
			// AWS Supports the creation of (sub)folders when creating files, this check is purely here so all FileSystems have the same behavior
			throw new FolderNotFoundException("folder [" + folder + "] does not exist");
		}

		// The inputStream content also be directly send to the s3Client.putObject(), when the File length is available.
		// When uploading of unknown size is needed, the S3AsyncClient or S3TransferManager can be used in the future.
		MessageBuilder messageBuilder = new MessageBuilder();
		try (OutputStream fos = messageBuilder.asOutputStream()) {
			StreamUtil.streamToStream(content, fos);
		}

		try (Message message = messageBuilder.build()) {
			PutObjectRequest.Builder por = PutObjectRequest.builder()
					.bucket(f.getBucketName())
					.key(f.getKey())
					.contentEncoding("UTF-8")
					.storageClass(storageClass);

			addMetadata(por, customFileAttributes);

			RequestBody requestBody = (Message.isEmpty(message)) ? RequestBody.empty() : RequestBody.fromInputStream(message.asInputStream(), message.size());

			s3Client.putObject(por.build(), requestBody);
		}
	}

	private void addMetadata(PutObjectRequest.Builder por, Map<String, String> userMetadata) {
		Map<String, String> metadata = new HashMap<>();

		if (userMetadata != null && !userMetadata.isEmpty()) {
			// Prefix the keys and encode the values according to rfc2047
			// see https://docs.aws.amazon.com/AmazonS3/latest/userguide/UsingMetadata.html#UserMetadata
			metadata.putAll(
					userMetadata.entrySet().stream()
					.collect(Collectors.toMap(
							entry -> "x-amz-meta-" + entry.getKey(),
							entry -> AmazonEncodingUtils.rfc2047Encode(entry.getValue()))));
		}

		por.metadata(metadata);
	}

	@Override
	public OutputStream createFile(S3FileRef f) {
		throw new NotImplementedException();
	}

	@Override
	public OutputStream appendFile(S3FileRef f) {
		// Amazon S3 doesn't support append operation
		return null;
	}

	/**
	 * If you retrieve an S3Object, you should close this input stream as soon as possible,
	 * because the object content is not buffered in memory and streams directly from Amazon S3.
	 * Failure to close this stream can cause the request pool to become blocked.
	 */
	@Override
	public Message readFile(S3FileRef file, String charset) throws FileSystemException {
		try {
			GetObjectRequest objectRequest = GetObjectRequest.builder()
					.bucket(file.getBucketName())
					.key(file.getKey())
					.build();
			ResponseInputStream<GetObjectResponse> s3ClientObject = s3Client.getObject(objectRequest);// Fetch a new copy
			file.updateObject(s3ClientObject.response());

			// Workaround for https://github.com/aws/aws-sdk-java-v2/issues/3538
			if (s3ClientObject.response().contentLength() == 0) {
				// Expects an empty message
				return Message.nullMessage(FileSystemUtils.getContext(this, file, charset));
			}

			return new Message(s3ClientObject, FileSystemUtils.getContext(this, file, charset));
		} catch (AwsServiceException e) {
			throw new FileSystemException(e);
		}
	}

	/**
	 * Attempts to update the Local S3 Pointer created by the {@link #toFile(String) toFile} method.
	 * Updates the Metadata context but does not retrieve the actual file handle.
	 * @throws FileSystemException if it cannot find the resource in S3.
	 */
	private void updateFileAttributes(S3FileRef f) throws FileSystemException {
		if (f.getContentLength() == null && f.hasName()) {
			try {
				getFileAttributes(f);
			} catch (AwsServiceException e) {
				throw new FileSystemException("Could not retrieve tags for object [" + f.getKey() + "] in bucket [" + f.getBucketName() + "]", e);
			}
		}
	}

	/**
	 * Attempts to update the Local S3 Pointer created by the {@link #toFile(String) toFile} method.
	 * @throws AwsServiceException if it cannot find the resource in S3.
	 */
	private S3FileRef getFileAttributes(S3FileRef f) {
		HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
				.bucket(f.getBucketName())
				.key(f.getKey())
				.build();
		HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);
		f.updateObject(headObjectResponse);
		return f;
	}

	@Override
	public void deleteFile(S3FileRef f) throws FileSystemException {
		try {
			DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
					.bucket(f.getBucketName())
					.key(f.getKey())
					.build();

			s3Client.deleteObject(deleteObjectRequest);
		} catch (AwsServiceException e) {
			throw new FileSystemException("Could not delete object [" + getCanonicalNameOrErrorMessage(f) + "]: " + e.getMessage());
		}
	}

	private ListObjectsV2Request.Builder createListRequestV2(String folder) {
		String prefix = folder != null ? FilenameUtils.normalizeNoEndSeparator(folder, true) + FILE_DELIMITER : null;
		return ListObjectsV2Request.builder()
				.bucket(bucketName)
				.delimiter(FILE_DELIMITER)
				.prefix(prefix);
	}

	@Override
	public boolean folderExists(String folder) throws FileSystemException {
		try {
			ListObjectsV2Request.Builder request = createListRequestV2(folder);
			ListObjectsV2Response listing;
			int iterations = 0;

			do {
				if (iterations > 20) {
					log.warn("unable to list all files in folder [{}]", folder);
					break;
				}

				listing = s3Client.listObjectsV2(request.build());
				if (listing.keyCount() > 0) { // If more then 1 result is returned, files also exist in this folder
					return true;
				}

				request.continuationToken(listing.continuationToken());
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
			throw new FolderNotFoundException("Cannot remove folder [" + folder + "]. Directory does not exist.");
		}
		// Check if there are files or folders, and not allowed to remove non-empty folder
		if (!removeNonEmptyFolder) {
			try (DirectoryStream<S3FileRef> stream = list(folder, TypeFilter.FILES_AND_FOLDERS)) {
				if (stream.iterator().hasNext()) {
					throw new FileSystemException("Cannot remove folder [" + folder + "]. Folder not empty.");
				}
			} catch (IOException e) {
				throw new FileSystemException("Cannot remove folder [" + folder + "]. " + e.getMessage());
			}
		}

		final String absFolder = folder.endsWith(FILE_DELIMITER) ? folder : folder + FILE_DELIMITER; //Ensure it's a folder that's being removed
		s3Client.deleteObject(DeleteObjectRequest.builder()
				.bucket(bucketName)
				.key(absFolder)
				.build());
	}

	// rename is implemented via copy & delete
	@Override
	public S3FileRef renameFile(S3FileRef source, S3FileRef destination) {
		CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
				.sourceBucket(source.getBucketName())
				.sourceKey(source.getKey())
				.destinationBucket(destination.getBucketName())
				.destinationKey(destination.getKey())
				.storageClass(getStorageClass())
				.build();
		s3Client.copyObject(copyObjectRequest);

		DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
				.bucket(source.getBucketName())
				.key(source.getKey())
				.build();
		s3Client.deleteObject(deleteObjectRequest);
		return destination;
	}

	@Override
	public S3FileRef copyFile(S3FileRef s3Object, String destinationFolder, boolean createFolder) throws FileSystemException {
		if (!createFolder && !folderExists(destinationFolder)) {
			throw new FolderNotFoundException("folder [" + destinationFolder + "] does not exist");
		}
		String destinationFile = destinationFolder+FILE_DELIMITER+getName(s3Object);

		CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
				.sourceBucket(s3Object.getBucketName())
				.sourceKey(s3Object.getKey())
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
	public S3FileRef moveFile(S3FileRef f, String destinationFolder, boolean createFolder) {
		return renameFile(f, toFile(destinationFolder, getName(f)));
	}

	@Override
	@Nullable
	public Map<String, Object> getAdditionalFileProperties(S3FileRef f) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		attributes.put("bucketName", f.getBucketName());
		f.getUserMetadata().forEach((key, value) -> attributes.put(key, AmazonEncodingUtils.rfc2047Decode(value)));

		return attributes;
	}

	@Override
	public String getName(S3FileRef f) {
		return f.getName();
	}

	@Override
	public String getParentFolder(S3FileRef f) {
		int lastSlashPos = f.getKey().lastIndexOf('/');
		return lastSlashPos > 1 ? f.getKey().substring(0, lastSlashPos) : null;
	}

	@Override
	public String getCanonicalName(S3FileRef f) {
		return f.getBucketName() + S3FileRef.BUCKET_OBJECT_SEPARATOR + f.getKey();
	}

	@Override
	public long getFileSize(S3FileRef f) throws FileSystemException {
		// Probably a folder, which cannot have a size.
		if (!f.hasName()) {
			return 0;
		}

		updateFileAttributes(f);
		return f.getContentLength();
	}

	@Override
	public Date getModificationTime(S3FileRef f) throws FileSystemException {
		updateFileAttributes(f);
		if (f.getLastModified() == null) {
			return null;
		}
		return Date.from(f.getLastModified());
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

	@Override
	public String getPhysicalDestinationName() {
		return "bucket [" + getBucketName() + "]";
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

	/** Name of the bucket to access. The bucketName can also be specified by prefixing it to the object name, separated from it by {@value S3FileRef#BUCKET_OBJECT_SEPARATOR} */
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
