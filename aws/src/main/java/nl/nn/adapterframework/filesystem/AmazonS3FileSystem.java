/*
   Copyright 2018-2023 WeAreFrank!

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
package nl.nn.adapterframework.filesystem;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.aws.AwsUtil;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.doc.Mandatory;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.StringUtil;


public class AmazonS3FileSystem extends FileSystemBase<S3Object> implements IWritableFileSystem<S3Object> {
	private final @Getter(onMethod = @__(@Override)) String domain = "Amazon";
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
//	private String bucketRegion;

//	private String storageClass;
//	private String tier = Tier.Standard.toString();
//	private int expirationInDays = -1;

//	private boolean storageClassEnabled = false;
//	private boolean bucketCreationEnabled = false;
//	private boolean bucketExistsThrowException = true;

	private @Getter String proxyHost = null;
	private @Getter Integer proxyPort = null;
	private @Getter int maxConnections = 50;

	private @Getter S3StorageClass storageClass = S3StorageClass.STANDARD;

	private AmazonS3 s3Client;
	private AWSCredentialsProvider credentialProvider;

	@Override
	public void configure() throws ConfigurationException {
		if((StringUtils.isNotEmpty(getAccessKey()) && StringUtils.isEmpty(getSecretKey())) || (StringUtils.isEmpty(getAccessKey()) && StringUtils.isNotEmpty(getSecretKey()))) {
			throw new ConfigurationException("invalid credential fields, please prodive AWS credentials (accessKey and secretKey)");
		}

		CredentialFactory cf = null;
		if (StringUtils.isNotEmpty(getAuthAlias()) || (StringUtils.isNotEmpty(getAccessKey()) && StringUtils.isNotEmpty(getSecretKey()))) {
			cf = new CredentialFactory(getAuthAlias(), getAccessKey(), getSecretKey());
		}
		credentialProvider = AwsUtil.createCredentialProviderChain(cf);


		if (StringUtils.isEmpty(getClientRegion()) || !AVAILABLE_REGIONS.contains(getClientRegion())) {
			throw new ConfigurationException("invalid region [" + getClientRegion() + "] please use one of the following supported regions " + AVAILABLE_REGIONS.toString());
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
		super.close();
		if(s3Client != null) {
			s3Client.shutdown();
		}
	}

	/**
	 * Creates a local S3Object pointer, not representative with what is stored in the S3 Bucket.
	 * This method may be used to upload a file to S3.
	 */
	@Override
	public S3Object toFile(String filename) throws FileSystemException {
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
	public S3Object toFile(String folder, String filename) throws FileSystemException {
		return toFile(StringUtil.concatStrings(folder, FILE_DELIMITER, filename));
	}


	@Override
	public int getNumberOfFilesInFolder(String folder) throws FileSystemException {
		List<S3ObjectSummary> summaries = null;
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
	public DirectoryStream<S3Object> listFiles(String folder) throws FileSystemException {
		List<S3ObjectSummary> summaries = new ArrayList<>();
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
				summaries.addAll(listing.getObjectSummaries());
				request.setContinuationToken(listing.getNextContinuationToken());
				iterations++;
			} while(listing.isTruncated());
		} catch (AmazonServiceException e) {
			throw new FileSystemException("Cannot process requested action", e);
		}

		List<S3Object> list = new ArrayList<>();
		for (S3ObjectSummary summary : summaries) {
			if(summary.getKey().endsWith("/")) { //Omit the root folder
				continue;
			}
			S3Object object = extractS3ObjectFromSummary(summary);
			list.add(object);
		}

		return FileSystemUtils.getDirectoryStream(list.iterator());
	}

	private static S3Object extractS3ObjectFromSummary(S3ObjectSummary summary) {
		S3Object object = new S3Object();
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(summary.getSize());
		metadata.setLastModified(summary.getLastModified());

		object.setBucketName(summary.getBucketName());
		object.setKey(summary.getKey());
		object.setObjectMetadata(metadata);
		return object;
	}

	@Override
	public boolean exists(S3Object f) throws FileSystemException {
		return s3Client.doesObjectExist(bucketName, f.getKey());
	}

	@Override
	public OutputStream createFile(final S3Object f) throws FileSystemException, IOException {
		String folder = getParentFolder(f);
		if (folder != null && !folderExists(folder)) { //AWS Supports the creation of folders, this check is purely here so all FileSystems have the same behavior
			throw new FileSystemException("folder ["+folder+"] does not exist");
		}

		final File file = FileUtils.createTempFile(".s3-upload");
		final FileOutputStream fos = new FileOutputStream(file);
		return new BufferedOutputStream(fos) {
			boolean isClosed = false;
			@Override
			public void close() throws IOException {
				super.close();
				if(!isClosed) {
					try (FileInputStream fis = new FileInputStream(file)) {
						ObjectMetadata metaData = new ObjectMetadata();
						metaData.setContentLength(file.length());

						try(S3Object file = f) {
							PutObjectRequest por = new PutObjectRequest(bucketName, file.getKey(), fis, metaData);
							por.setStorageClass(getStorageClass().getStorageClass());
							s3Client.putObject(por);
						}
					} finally {
						isClosed = true;
						Files.delete(file.toPath());
					}
				}
			}
		};
	}

	@Override
	public OutputStream appendFile(S3Object f) throws FileSystemException, IOException {
		// Amazon S3 doesn't support append operation
		return null;
	}

	/** 
	 * If you retrieve an S3Object, you should close this input stream as soon as possible,
	 * because the object contents aren't buffered in memory and stream directly from Amazon S3.
	 * Further, failure to close this stream can cause the request pool to become blocked. 
	 */
	@Override
	public Message readFile(S3Object file, String charset) throws FileSystemException, IOException {
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
			throw new FileSystemException(e);
		}
	}

	private ListObjectsV2Request createListRequestV2(String folder) {
		String prefix = folder != null ? folder + FILE_DELIMITER : null;
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
		String folderName = folder.endsWith("/") ? folder : folder + "/";
		if (!folderExists(folder)) {
			s3Client.putObject(bucketName, folderName, "");
		} else {
			throw new FileSystemException("Create directory for [" + folderName + "] has failed. Directory already exists.");
		}
	}

	@Override
	public void removeFolder(String folder, boolean removeNonEmptyFolder) throws FileSystemException {
		if (folderExists(folder)) {
			folder = folder.endsWith("/") ? folder : folder + "/";
			s3Client.deleteObject(bucketName, folder);
		} else {
			throw new FileSystemException("Remove directory for [" + folder + "] has failed. Directory does not exist.");
		}
	}

	@Override
	// rename is actually implemented via copy
	public S3Object renameFile(S3Object source, S3Object destination) throws FileSystemException {
		CopyObjectRequest cor = new CopyObjectRequest(bucketName, source.getKey(), bucketName, destination.getKey());
		cor.setStorageClass(getStorageClass().getStorageClass());
		s3Client.copyObject(cor);
		s3Client.deleteObject(bucketName, source.getKey());
		return destination;
	}

	@Override
	public S3Object copyFile(S3Object f, String destinationFolder, boolean createFolder, boolean resultantMustBeReturned) throws FileSystemException {
		if (!createFolder && !folderExists(destinationFolder)) {
			throw new FileSystemException("folder ["+destinationFolder+"] does not exist");
		}
		String destinationFile = destinationFolder+"/"+getName(f);
		CopyObjectRequest cor = new CopyObjectRequest(bucketName, f.getKey(), bucketName,destinationFile);
		cor.setStorageClass(getStorageClass().getStorageClass());
		s3Client.copyObject(cor);
		return toFile(destinationFile);
	}

	@Override
	// move is actually implemented via copy and delete
	public S3Object moveFile(S3Object f, String destinationFolder, boolean createFolder, boolean resultantMustBeReturned) throws FileSystemException {
		return renameFile(f,toFile(destinationFolder,getName(f)));
	}


	@Override
	public Map<String, Object> getAdditionalFileProperties(S3Object f) {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("bucketName", bucketName);
		return attributes;
	}

	@Override
	public String getName(S3Object f) {
		int lastSlashPos = f.getKey().lastIndexOf('/');
		return f.getKey().substring(lastSlashPos+1);
	}

	@Override
	public String getParentFolder(S3Object f) throws FileSystemException {
		int lastSlashPos = f.getKey().lastIndexOf('/');
		return lastSlashPos > 1 ? f.getKey().substring(0, lastSlashPos) : null;
	}

	@Override
	public String getCanonicalName(S3Object f) throws FileSystemException {
		return f.getBucketName() + BUCKET_OBJECT_SEPARATOR + f.getKey();
	}

	@Override
	public long getFileSize(S3Object f) throws FileSystemException {
		updateFileAttributes(f);
		return f.getObjectMetadata().getContentLength();
	}

	@Override
	public Date getModificationTime(S3Object f) throws FileSystemException {
		updateFileAttributes(f);
		return f.getObjectMetadata().getLastModified();
	}

//	/**
//	* Creates a bucket on Amazon S3.
//	*
//	* @param bucketName
//	*            The desired name for a bucket that is about to be created. The class {@link BucketNameUtils}
//	*            provides a method that can check if the bucketName is valid. This is done just before the bucketName is used here.
//	* @param bucketExistsThrowException
//	* 			  This parameter is used for controlling the behavior for whether an exception has to be thrown or not.
//	* 			  In case of upload action being configured to be able to create a bucket, an exception will not be thrown when a bucket with assigned bucketName already exists.
//	*/
//	public String createBucket(String bucketName, boolean bucketExistsThrowException) throws SenderException {
//		try {
//			if (!s3Client.doesBucketExistV2(bucketName)) {
//				CreateBucketRequest createBucketRequest = null;
//				if (isForceGlobalBucketAccessEnabled())
//					createBucketRequest = new CreateBucketRequest(bucketName, getBucketRegion());
//				else
//					createBucketRequest = new CreateBucketRequest(bucketName);
//				s3Client.createBucket(createBucketRequest);
//				log.debug("Bucket with bucketName: [" + bucketName + "] is created.");
//			} else if (bucketExistsThrowException)
//				throw new SenderException(" bucket with bucketName [" + bucketName + "] already exists, please specify a unique bucketName");
//
//		} catch (AmazonServiceException e) {
//			log.warn("Failed to create bucket with bucketName [" + bucketName + "].");
//			throw new SenderException("Failed to create bucket with bucketName [" + bucketName + "]." + e);
//		}
//
//		return bucketName;
//	}

//	/**
//	 * Deletes a bucket on Amazon S3.
//	 */
//	public String deleteBucket() throws SenderException {
//		try {
//			bucketDoesNotExist(bucketName);
//			DeleteBucketRequest deleteBucketRequest = new DeleteBucketRequest(bucketName);
//			s3Client.deleteBucket(deleteBucketRequest);
//			log.debug("Bucket with bucketName [" + bucketName + "] is deleted.");
//		} catch (AmazonServiceException e) {
//			log.warn("Failed to delete bucket with bucketName [" + bucketName + "].");
//			throw new SenderException("Failed to delete bucket with bucketName [" + bucketName + "].");
//		}
//
//		return bucketName;
//	}

//	/**
//	 * Copies a file from one Amazon S3 bucket to another one.
//	 *
//	 * @param fileName
//	 * 				This is the name of the file that is desired to be copied.
//	 *
//	 * @param destinationFileName
//	 * 				The name of the destination file
//	 */
//	public String copyObject(String fileName, String destinationFileName) throws SenderException {
//		try {
//			bucketDoesNotExist(bucketName); //if bucket does not exists this method throws an exception
//			fileDoesNotExist(bucketName, fileName); //if object does not exists this method throws an exception
//			if (!s3Client.doesBucketExistV2(destinationBucketName))
//				bucketCreationWithObjectAction(destinationBucketName);
//			if (!s3Client.doesObjectExist(destinationBucketName, destinationFileName)) {
//				CopyObjectRequest copyObjectRequest = new CopyObjectRequest(bucketName, fileName, destinationBucketName, destinationFileName);
//				if (isStorageClassEnabled())
//					copyObjectRequest.setStorageClass(getStorageClass());
//				s3Client.copyObject(copyObjectRequest);
//				log.debug("Object with fileName [" + fileName + "] copied from bucket with bucketName [" + bucketName
//						+ "] into bucket with bucketName [" + destinationBucketName + "] and new fileName ["
//						+ destinationFileName + "]");
//			} else
//				throw new SenderException(" file with given name already exists, please specify a new name");
//		} catch (AmazonServiceException e) {
//			log.error("Failed to perform [copy] action on object with fileName [" + fileName + "]");
//			throw new SenderException("Failed to perform [copy] action on object with fileName [" + fileName + "]");
//		}
//
//		return destinationFileName;
//	}

//	public String restoreObject(String fileName) throws SenderException {
//		Boolean restoreFlag;
//		try {
//			bucketDoesNotExist(bucketName);
//			fileDoesNotExist(bucketName, fileName);
//			RestoreObjectRequest requestRestore = new RestoreObjectRequest(bucketName, fileName, expirationInDays).withTier(tier);
//			s3Client.restoreObjectV2(requestRestore);
//			log.debug("Object with fileName [" + fileName + "] and bucketName [" + bucketName + "] restored from Amazon S3 Glacier");
//
//			ObjectMetadata response = s3Client.getObjectMetadata(bucketName, fileName);
//			restoreFlag = response.getOngoingRestore();
//			System.out.format("Restoration status: %s.\n", restoreFlag ? "in progress" : "not in progress (finished or failed)");
//
//		} catch (AmazonServiceException e) {
//			log.error("Failed to perform [restore] action, and restore object with fileName [" + fileName + "] from Amazon S3 Glacier");
//			throw new SenderException("Failed to perform [restore] action, and restore object with fileName [" + fileName + "] from Amazon S3 Glacier");
//		}
//
//		String prefix = "Restoration status: %s.\n";
//		return restoreFlag ? prefix + "in progress" : prefix + "not in progress (finished or failed)";
//	}

//	/**
//	 * This method is wrapper which makes it possible for upload and copy actions to create a bucket and
//	 * in case a bucket already exists the operation will proceed without throwing an exception.
//	 *
//	 * @param bucketName
//	 *            The name of the bucket that is addressed.
//	 */
//	public void bucketCreationWithObjectAction(String bucketName) throws SenderException {
//		if (isBucketCreationEnabled())
//			createBucket(bucketName, !bucketExistsThrowException);
//		else
//			throw new SenderException("Failed to create a bucket, to create a bucket bucketCreationEnabled attribute must be assinged to [true]");
//	}



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
		List<String> availableRegions = new ArrayList<String>(Regions.values().length);
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

//	public void setBucketRegion(String bucketRegion) {
//		this.bucketRegion = bucketRegion;
//	}

//	public void setStorageClass(String storageClass) {
//		this.storageClass = storageClass;
//	}

//	public void setTier(String tier) {
//		this.tier = tier;
//	}

//	public void setExpirationInDays(int experationInDays) {
//		this.expirationInDays = experationInDays;
//	}

//	public void setStorageClassEnabled(boolean storageClassEnabled) {
//		this.storageClassEnabled = storageClassEnabled;
//	}

//	public void setBucketCreationEnabled(boolean bucketCreationEnabled) {
//		this.bucketCreationEnabled = bucketCreationEnabled;
//	}

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
	 * More info on storage classes can be found on the <a href="https://aws.amazon.com/s3/storage-classes/">AWS S3 docs</a>.
	 *
	 * @ff.default {@literal STANDARD}
	 */
	public void setStorageClass(S3StorageClass storageClass) {
		this.storageClass = storageClass;
	}

	/** Maximum concurrent connections towards S3 */
	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

}
