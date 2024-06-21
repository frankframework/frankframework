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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.internal.BucketNameUtils;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.StorageClass;

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


public class AmazonS3FileSystem extends FileSystemBase<S3Object> implements IWritableFileSystem<S3Object>, ISupportsCustomFileAttributes<S3Object> {
	private final @Getter String domain = "Amazon";
	private static final List<String> AVAILABLE_REGIONS = getAvailableRegions();

	private static final String BUCKET_OBJECT_SEPARATOR = "|";
	private static final String FILE_DELIMITER = "/";

	private @Getter String accessKey;
	private @Getter String secretKey;
	private @Getter String authAlias;

	private String serviceEndpoint = null;

	private @Getter boolean chunkedEncodingDisabled = false;
	private @Getter boolean forceGlobalBucketAccessEnabled = false;
	private @Getter String clientRegion = Regions.EU_WEST_1.getName();

	private @Getter String bucketName;
	private @Getter String proxyHost = null;
	private @Getter Integer proxyPort = null;
	private @Getter int maxConnections = 50;

	private @Getter StorageClass storageClass = StorageClass.Standard;

	private AmazonS3 s3Client;
	private AWSCredentialsProvider credentialProvider;

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


		if (StringUtils.isEmpty(getClientRegion()) || !AVAILABLE_REGIONS.contains(getClientRegion())) {
			throw new ConfigurationException("invalid region [" + getClientRegion() + "] please use one of the following supported regions " + AVAILABLE_REGIONS);
		}

		if (StringUtils.isEmpty(getBucketName()) || !BucketNameUtils.isValidV2BucketName(getBucketName())) {
			throw new ConfigurationException("invalid or empty bucketName [" + getBucketName() + "] please visit AWS to see correct bucket naming");
		}
	}

	@Override
	public void open() throws FileSystemException {
		s3Client = createS3Client();

		super.open();
	}

	//For testing purposes
	protected AWSCredentialsProvider getCredentialProvider() {
		return credentialProvider;
	}

	//For testing purposes
	public AmazonS3 createS3Client() {
		AmazonS3ClientBuilder s3ClientBuilder = AmazonS3ClientBuilder.standard()
				.withChunkedEncodingDisabled(isChunkedEncodingDisabled())
				.withForceGlobalBucketAccessEnabled(isForceGlobalBucketAccessEnabled())
				.withCredentials(credentialProvider)
				.withClientConfiguration(this.getClientConfig())
				.enablePathStyleAccess();

		if(StringUtils.isBlank(serviceEndpoint)) {
			s3ClientBuilder.withRegion(getClientRegion());
		} else {
			s3ClientBuilder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(serviceEndpoint, getClientRegion()));
		}

		return s3ClientBuilder.build();
	}

	@Override
	public void close() throws FileSystemException {
		if(s3Client != null) {
			s3Client.shutdown();
		}
		super.close();
	}

	/**
	 * Creates a local S3Object pointer, not representative with what is stored in the S3 Bucket.
	 * This method may be used to upload a file to S3.
	 */
	@Override
	public S3Object toFile(@Nullable String filename) {
		S3Object object = new S3Object();
		int separatorPos = filename.indexOf(BUCKET_OBJECT_SEPARATOR);
		if (separatorPos<0) {
			object.setBucketName(bucketName);
			object.setKey(filename);
		} else {
			object.setBucketName(filename.substring(0,separatorPos));
			object.setKey(filename.substring(separatorPos+1));
		}
		return object;
	}

	@Override
	public S3Object toFile(@Nullable String folder, @Nullable String filename) {
		return toFile(StringUtil.concatStrings(folder, FILE_DELIMITER, filename));
	}


	@Override
	public int getNumberOfFilesInFolder(String folder) throws FileSystemException {
		List<S3ObjectSummary> summaries;
		String prefix = folder != null ? folder + FILE_DELIMITER : "";
		try {
			ObjectListing listing = s3Client.listObjects(bucketName, prefix);
			summaries = listing.getObjectSummaries();
			int result = summaries.size() - (folder!=null ? 1 :0);
			while (listing.isTruncated() && (getMaxNumberOfMessagesToList()<0 || getMaxNumberOfMessagesToList() > result)) {
				listing = s3Client.listNextBatchOfObjects(listing);
				result += listing.getObjectSummaries().size();
			}
			return result;
		} catch (AmazonServiceException e) {
			throw new FileSystemException("Cannot process requested action", e);
		}
	}

	@Override
	public DirectoryStream<S3Object> list(String folder, TypeFilter filter) throws FileSystemException {
		List<S3ObjectSummary> summaries = new ArrayList<>();
		List<String> subFolders = new ArrayList<>();
		try {
			ListObjectsV2Request request = createListRequestV2(folder);
			ListObjectsV2Result listing;
			int iterations = 0;

			do {
				if(iterations > 20) {
					log.warn("unable to list all files in folder [{}]", folder);
					break;
				}

				listing = s3Client.listObjectsV2(request);
				if (filter.includeFiles()) {
					summaries.addAll(listing.getObjectSummaries()); // Files
				}
				if (filter.includeFolders()) {
					subFolders.addAll(listing.getCommonPrefixes()); // Folders
				}

				request.setContinuationToken(listing.getNextContinuationToken());
				iterations++;
			} while(listing.isTruncated());
		} catch (AmazonServiceException e) {
			throw new FileSystemException("Cannot process requested action", e);
		}

		List<S3Object> list = new ArrayList<>();
		for (String folderName : subFolders) {
			list.add(createS3FolderObject(bucketName, folderName));
		}
		for (S3ObjectSummary summary : summaries) {
			if (summary.getKey().endsWith(FILE_DELIMITER)) { // Omit the 'search' folder
				continue;
			}
			list.add(extractS3ObjectFromSummary(summary));
		}

		return FileSystemUtils.getDirectoryStream(list.iterator());
	}

	private static S3Object createS3FolderObject(String bucketName, String folderName) {
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(0); //Does not trigger updateFileAttributes
		S3Object object = new S3Object();
		object.setBucketName(bucketName);
		object.setKey(folderName.endsWith(FILE_DELIMITER) ? folderName : folderName + FILE_DELIMITER);
		object.setObjectMetadata(metadata);
		return object;
	}

	private static S3Object extractS3ObjectFromSummary(S3ObjectSummary summary) {
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(summary.getSize());
		metadata.setLastModified(summary.getLastModified());

		S3Object object = new S3Object();
		object.setBucketName(summary.getBucketName());
		object.setKey(summary.getKey());
		object.setObjectMetadata(metadata);
		return object;
	}

	@Override
	public boolean exists(S3Object f) {
		return s3Client.doesObjectExist(bucketName, f.getKey());
	}

	@Override
	public boolean isFolder(S3Object s3Object) {
		return s3Object.getKey().endsWith(FILE_DELIMITER);
	}

	@Override
	public void createFile(S3Object f, InputStream content) throws FileSystemException, IOException {
		String folder = getParentFolder(f);
		if (folder != null && !folderExists(folder)) { //AWS Supports the creation of folders, this check is purely here so all FileSystems have the same behavior
			throw new FolderNotFoundException("folder ["+folder+"] does not exist");
		}

		final File file = FileUtils.createTempFile(".s3-upload"); //The lesser evil to allow streaming uploads
		try(FileOutputStream fos = new FileOutputStream(file)) {
			StreamUtil.streamToStream(content, fos);
		}

		ObjectMetadata metaData = new ObjectMetadata();
		metaData.setContentLength(file.length());
		f.getObjectMetadata().getUserMetadata().forEach(metaData::addUserMetadata);

		try (FileInputStream fis = new FileInputStream(file)) {
			try(S3Object s3File = f) {
				PutObjectRequest por = new PutObjectRequest(bucketName, s3File.getKey(), fis, metaData);
				por.setStorageClass(getStorageClass());
				s3Client.putObject(por);
			}
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
	 * because the object contents aren't buffered in memory and stream directly from Amazon S3.
	 * Further, failure to close this stream can cause the request pool to become blocked.
	 */
	@Override
	public Message readFile(S3Object file, String charset) throws FileSystemException {
		try {
			if(file.getObjectContent() == null) { // We have a reference but not an actual object representing the S3 bucket.
				file = s3Client.getObject(bucketName, file.getKey()); // Fetch a new copy
			}
			return new Message(file.getObjectContent(), FileSystemUtils.getContext(this, file, charset));
		} catch (AmazonServiceException e) {
			throw new FileSystemException(e);
		}
	}

	/**
	 * Attempts to update the Local S3 Pointer created by the {@link #toFile(String) toFile} method.
	 * Updates the Metadata context but does not retrieve the actual file handle.
	 */
	private S3Object updateFileAttributes(S3Object f) {
		if(f.getObjectMetadata().getRawMetadataValue(Headers.CONTENT_LENGTH) == null) {
			ObjectMetadata omd = s3Client.getObjectMetadata(bucketName, f.getKey());
			f.setObjectMetadata(omd);
		}
		return f;
	}

	@Override
	public void deleteFile(S3Object f) throws FileSystemException {
		try {
			DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(bucketName, f.getKey());
			s3Client.deleteObject(deleteObjectRequest);
		} catch (AmazonServiceException e) {
			throw new FileSystemException("Could not delete object [" + getCanonicalNameOrErrorMessage(f) + "]: " + e.getMessage());
		}
	}

	private ListObjectsV2Request createListRequestV2(String folder) {
		String prefix = folder != null ? FilenameUtils.normalizeNoEndSeparator(folder, true) + FILE_DELIMITER : null;

		return new ListObjectsV2Request()
				.withBucketName(bucketName)
				.withDelimiter(FILE_DELIMITER)
				.withPrefix(prefix);
	}

	@Override
	public boolean folderExists(String folder) throws FileSystemException {
		try {
			ListObjectsV2Request request = createListRequestV2(folder);
			ListObjectsV2Result listing;
			int iterations = 0;

			do {
				if(iterations > 20) {
					log.warn("unable to list all files in folder [{}]", folder);
					break;
				}
				listing = s3Client.listObjectsV2(request);
				if(listing.getKeyCount() > 0) { //If more then 1 result is returned, files also exist in this folder
					return true;
				}
				request.setContinuationToken(listing.getNextContinuationToken());
				iterations++;
			} while(listing.isTruncated());
		} catch (AmazonServiceException e) {
			throw new FileSystemException("Cannot process requested action", e);
		}
		return false;
	}

	@Override
	public void createFolder(String folder) throws FileSystemException {
		String folderName = folder.endsWith(FILE_DELIMITER) ? folder : folder + FILE_DELIMITER;
		if (folderExists(folder)) {
			throw new FolderAlreadyExistsException("Create directory for [" + folderName + "] has failed. Directory already exists.");
		}
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(0);
		metadata.setContentType("binary/octet-stream");
		InputStream emptyContent = InputStream.nullInputStream();

		PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, folderName, emptyContent, metadata);
		s3Client.putObject(putObjectRequest);
	}

	@Override
	public void removeFolder(String folder, boolean removeNonEmptyFolder) throws FileSystemException {
		if (!folderExists(folder)) {
			throw new FolderNotFoundException("Cannot remove folder [" + folder + "]. Directory does not exist.");
		}
		// Check if there are files or folders, and not allowed to remove non-empty folder
		if (!removeNonEmptyFolder) {
			try (DirectoryStream<S3Object> stream = list(folder, TypeFilter.FILES_AND_FOLDERS)) {
				if (stream.iterator().hasNext()) {
					throw new FileSystemException("Cannot remove folder [" + folder + "]. Directory not empty.");
				}
			} catch (IOException e) {
				throw new FileSystemException("Cannot remove folder [" + folder + "]. " + e.getMessage());
			}
		}

		final String absFolder = folder.endsWith(FILE_DELIMITER) ? folder : folder + FILE_DELIMITER; //Ensure it's a folder that's being removed
		s3Client.deleteObject(bucketName, absFolder);
	}

	@Override
	// rename is actually implemented via copy
	public S3Object renameFile(S3Object source, S3Object destination) throws FileSystemException {
		CopyObjectRequest cor = new CopyObjectRequest(bucketName, source.getKey(), bucketName, destination.getKey());
		cor.setStorageClass(getStorageClass());
		s3Client.copyObject(cor);
		s3Client.deleteObject(bucketName, source.getKey());
		return destination;
	}

	@Override
	public S3Object copyFile(S3Object f, String destinationFolder, boolean createFolder) throws FileSystemException {
		if (!createFolder && !folderExists(destinationFolder)) {
			throw new FolderNotFoundException("folder ["+destinationFolder+"] does not exist");
		}
		String destinationFile = destinationFolder+FILE_DELIMITER+getName(f);
		CopyObjectRequest cor = new CopyObjectRequest(bucketName, f.getKey(), bucketName,destinationFile);
		cor.setStorageClass(getStorageClass());
		s3Client.copyObject(cor);
		return toFile(destinationFile);
	}

	@Override
	// move is actually implemented via copy and delete
	public S3Object moveFile(S3Object f, String destinationFolder, boolean createFolder) throws FileSystemException {
		return renameFile(f,toFile(destinationFolder,getName(f)));
	}


	@Override
	@Nullable
	public Map<String, Object> getAdditionalFileProperties(S3Object f) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		attributes.put("bucketName", bucketName);
		if (f.getObjectMetadata() != null) {
			f.getObjectMetadata().getUserMetadata().forEach((key, value) -> attributes.put(key, AmazonEncodingUtils.rfc2047Decode(value)));
		}
		return attributes;
	}

	@Override
	public String getName(S3Object f) {
		String key = f.getKey();
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
		int lastSlashPos = f.getKey().lastIndexOf('/');
		return lastSlashPos > 1 ? f.getKey().substring(0, lastSlashPos) : null;
	}

	@Override
	public String getCanonicalName(S3Object f) {
		return f.getBucketName() + BUCKET_OBJECT_SEPARATOR + f.getKey();
	}

	@Override
	public long getFileSize(S3Object f) {
		updateFileAttributes(f);
		return f.getObjectMetadata().getContentLength();
	}

	@Override
	public Date getModificationTime(S3Object f) {
		updateFileAttributes(f);
		return f.getObjectMetadata().getLastModified();
	}

	public ClientConfiguration getClientConfig() {
		ClientConfiguration clientConfiguration = new ClientConfiguration();
		clientConfiguration.setMaxConnections(getMaxConnections());

		if (this.getProxyHost() != null && this.getProxyPort() != null) {
			clientConfiguration.setProtocol(Protocol.HTTPS);
			clientConfiguration.setProxyHost(this.getProxyHost());
			clientConfiguration.setProxyPort(this.getProxyPort());
		}
		return clientConfiguration;
	}

	@Override
	public String getPhysicalDestinationName() {
		return "bucket ["+getBucketName()+"]";
	}

	public static List<String> getAvailableRegions() {
		List<String> availableRegions = new ArrayList<>(Regions.values().length);
		for (Regions region : Regions.values())
			availableRegions.add(region.getName());

		return availableRegions;
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
	 * Set whether the client should be configured with global bucket access enabled.
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
	 * Name of the region that the client will be created from
	 * @ff.default eu-west-1
	 */
	@Mandatory
	public void setClientRegion(String clientRegion) {
		this.clientRegion = clientRegion;
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

	/** Maximum concurrent connections towards S3 */
	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

	@Override
	public void setCustomFileAttribute(@Nonnull S3Object file, @Nonnull String key, @Nonnull String value) {
		file.getObjectMetadata().addUserMetadata(key, AmazonEncodingUtils.rfc2047Encode(value));
	}
}
