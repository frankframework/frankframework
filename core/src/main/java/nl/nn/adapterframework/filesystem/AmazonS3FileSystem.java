package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.internal.BucketNameUtils;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.DeleteBucketRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.RestoreObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.XmlBuilder;

public class AmazonS3FileSystem implements IFileSystem<S3Object> {
	private AmazonS3 s3Client;
	private String bucketName;
	private String bucketRegion;
	private String storageClass;
	private String tier;

	private boolean forceGlobalBucketAccessEnabled = false;
	private boolean bucketCreationEnabled = false;
	private boolean storageClassEnabled = false;
	private boolean bucketExistsThrowException = true;

	private String logPrefix;
	private Logger log;

	public AmazonS3FileSystem(AmazonS3 s3Client, String bucketName) {
		this.bucketName = bucketName;
		this.s3Client = s3Client;
	}

	@Override
	public void configure() throws ConfigurationException {

	}

	@Override
	public S3Object toFile(String filename) throws FileSystemException {
		S3Object object = new S3Object();
		object.setKey(filename);
		return object;
	}

	@Override
	public Iterator<S3Object> listFiles() throws FileSystemException {
		List<S3ObjectSummary> summaries = null;
		try {
			ObjectListing listing = s3Client.listObjects(bucketName);
			summaries = listing.getObjectSummaries();
			while (listing.isTruncated()) {
				listing = s3Client.listNextBatchOfObjects(listing);
				summaries.addAll(listing.getObjectSummaries());
			}
		} catch (AmazonServiceException e) {
			e.printStackTrace();
			throw new FileSystemException();
		}

		List<S3Object> list = new ArrayList<S3Object>();
		for (S3ObjectSummary summary : summaries) {
			S3Object object = new S3Object();
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(summary.getSize());

			object.setBucketName(summary.getBucketName());
			object.setKey(summary.getKey());
			object.setObjectMetadata(metadata);

			list.add(object);
		}

		return list.iterator();
	}

	@Override
	public boolean exists(S3Object f) throws FileSystemException {
		return s3Client.doesObjectExist(bucketName, f.getKey());
	}

	@Override
	public OutputStream createFile(S3Object f) throws FileSystemException, IOException {
		PipedOutputStream outputStream = new PipedOutputStream();
		PipedInputStream inputStream = new PipedInputStream(outputStream);
		new Thread(() -> {
			try {
				PutObjectResult result = s3Client.putObject(bucketName, f.getKey(), inputStream,
						f.getObjectMetadata());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				inputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}).start();

		return outputStream;
	}

	@Override
	public OutputStream appendFile(S3Object f) throws FileSystemException, IOException {

		// Amazon S3 doesn't support append operation
		return null;
	}

	@Override
	public InputStream readFile(S3Object f) throws FileSystemException, IOException {
		try {
			GetObjectRequest getObjectrequest = new GetObjectRequest(bucketName, f.getKey());
			return s3Client.getObject(getObjectrequest).getObjectContent();
		} catch (AmazonServiceException e) {
			e.printStackTrace();
			throw new FileSystemException();
		}
	}

	@Override
	public void deleteFile(S3Object f) throws FileSystemException {
		try {
			DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(bucketName,
					f.getKey());
			s3Client.deleteObject(deleteObjectRequest);
		} catch (AmazonServiceException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getInfo(S3Object f) throws FileSystemException {
		return getFileAsXmlBuilder(f).toXML();
	}

	@Override
	public boolean isFolder(S3Object f) throws FileSystemException {
		return f.getKey().endsWith("/");
	}

	@Override
	public void createFolder(S3Object f) throws FileSystemException {
		s3Client.putObject(bucketName, f.getKey(), "");

	}

	@Override
	public void removeFolder(S3Object f) throws FileSystemException {
		if (exists(f)) {
			if (isFolder(f)) {
				s3Client.deleteObject(bucketName, f.getKey());
			} else {
				throw new FileSystemException("trying to remove file [" + f.getKey()
						+ "] which is a file instead of a directory");
			}
		} else {
			throw new FileSystemException(
					"trying to remove file [" + f.getKey() + "] which does not exist");
		}

	}

	@Override
	public void renameTo(S3Object f, String destination) throws FileSystemException {
		s3Client.copyObject(bucketName, f.getKey(), bucketName, destination);
		s3Client.deleteObject(bucketName, f.getKey());

	}

	@Override
	public XmlBuilder getFileAsXmlBuilder(S3Object f) throws FileSystemException {
		S3Object fObject = s3Client.getObject(bucketName, f.getKey());
		XmlBuilder fileXml = new XmlBuilder("file");
		fileXml.addAttribute("bucketName", fObject.getBucketName());
		fileXml.addAttribute("name", fObject.getKey());
		fileXml.addAttribute("size", fObject.getObjectMetadata().getContentLength());

		// Get the modification date of the file
		Date modificationDate = fObject.getObjectMetadata().getLastModified();
		//add date
		String date = DateUtils.format(modificationDate, DateUtils.FORMAT_DATE);
		fileXml.addAttribute("modificationDate", date);

		// add the time
		String time = DateUtils.format(modificationDate, DateUtils.FORMAT_TIME_HMS);
		fileXml.addAttribute("modificationTime", time);
		return fileXml;
	}

	@Override
	public void augmentDirectoryInfo(XmlBuilder dirInfo, S3Object f) {
		// TODO Auto-generated method stub

	}

	@Override
	public void finalizeAction() {
		//		for (int i = 0; i < 50000; i++) {
		//			System.err.println("asdadasdas");
		//		}
	}

	/**
	* Creates a bucket on Amazon S3.
	*
	* @param bucketName
	*            The desired name for a bucket that is about to be created. The class {@link BucketNameUtils} 
	*            provides a method that can check if the bucketName is valid. This is done just before the bucketName is used here.
	* @param bucketExistsThrowException
	* 			  This parameter is used for controlling the behavior for weather an exception has to be thrown or not. 
	* 			  In case of upload action being configured to be able to create a bucket, an exception will not be thrown when a bucket with assigned bucketName already exists.
	*/
	public String createBucket(String bucketName, boolean bucketExistsThrowException)
			throws SenderException {
		try {
			if (!s3Client.doesBucketExistV2(bucketName)) {
				CreateBucketRequest createBucketRequest = null;
				if (isForceGlobalBucketAccessEnabled())
					createBucketRequest = new CreateBucketRequest(bucketName, getBucketRegion());
				else
					createBucketRequest = new CreateBucketRequest(bucketName);
				s3Client.createBucket(createBucketRequest);
				log.debug("Bucket with bucketName: [" + bucketName + "] is created.");
			} else if (bucketExistsThrowException)
				throw new SenderException(getLogPrefix() + " bucket with bucketName [" + bucketName
						+ "] already exists, please specify a unique bucketName");

		} catch (AmazonServiceException e) {
			log.warn("Failed to create bucket with bucketName [" + bucketName + "].");
			throw new SenderException(
					"Failed to create bucket with bucketName [" + bucketName + "]." + e);
		}

		return bucketName;
	}

	/**
	 * Deletes a bucket on Amazon S3.
	 *
	 * @param bucketName
	 *            The name for a bucket that is desired to be deleted.
	 */
	public String deleteBucket(String bucketName) throws SenderException {
		try {
			bucketDoesNotExist(bucketName);
			DeleteBucketRequest deleteBucketRequest = new DeleteBucketRequest(bucketName);
			s3Client.deleteBucket(deleteBucketRequest);
			log.debug("Bucket with bucketName [" + bucketName + "] is deleted.");
		} catch (AmazonServiceException e) {
			log.warn("Failed to delete bucket with bucketName [" + bucketName + "].");
			throw new SenderException(
					"Failed to delete bucket with bucketName [" + bucketName + "].");
		}

		return bucketName;
	}

	/**
	 * Copies a file from one Amazon S3 bucket to another one. 
	 *
	 * @param bucketName
	 *            The name of the bucket where the file is stored in.
	 * @param fileName
	 * 			  This is the name of the file that is desired to be copied.
	 * @param pvl
	 * 			  This object is given in order to get the contents of destinationFileName parameter for naming the new object within bucket where the file is copied to.
	 */
	public String copyObject(String bucketName, String fileName, String destinationBucketName,
			String destinationFileName) throws SenderException {
		try {
			bucketDoesNotExist(bucketName); //if bucket does not exists this method throws and exception
			fileDoesNotExist(bucketName, fileName); //if object does not exists this method throws and exception
			if (!s3Client.doesBucketExistV2(destinationBucketName))
				bucketCreationWithObjectAction(destinationBucketName);
			if (!s3Client.doesObjectExist(destinationBucketName, destinationFileName)) {
				CopyObjectRequest copyObjectRequest = new CopyObjectRequest(bucketName, fileName,
						destinationBucketName, destinationFileName);
				if (isStorageClassEnabled())
					copyObjectRequest.setStorageClass(getStorageClass());
				s3Client.copyObject(copyObjectRequest);
				log.debug("Object with fileName [" + fileName
						+ "] copied from bucket with bucketName [" + bucketName
						+ "] into bucket with bucketName [" + destinationBucketName
						+ "] and new fileName [" + destinationFileName + "]");
			} else
				throw new SenderException(getLogPrefix()
						+ " file with given name already exists, please specify a new name");
		} catch (AmazonServiceException e) {
			log.error("Failed to perform [copy] action on object with fileName [" + fileName + "]");
			throw new SenderException(
					"Failed to perform [copy] action on object with fileName [" + fileName + "]");
		}

		return destinationFileName;
	}

	public String restoreObject(String bucketName, String fileName, int experationInDays)
			throws SenderException {
		Boolean restoreFlag;
		try {
			bucketDoesNotExist(bucketName);
			fileDoesNotExist(bucketName, fileName);
			RestoreObjectRequest requestRestore = new RestoreObjectRequest(bucketName, fileName,
					experationInDays).withTier(tier);
			s3Client.restoreObjectV2(requestRestore);
			log.debug("Object with fileName [" + fileName + "] and bucketName [" + bucketName
					+ "] restored from Amazon S3 Glacier");

			ObjectMetadata response = s3Client.getObjectMetadata(bucketName, fileName);
			restoreFlag = response.getOngoingRestore();
			System.out.format("Restoration status: %s.\n",
					restoreFlag ? "in progress" : "not in progress (finished or failed)");

		} catch (AmazonServiceException e) {
			log.error("Failed to perform [restore] action, and restore object with fileName ["
					+ fileName + "] from Amazon S3 Glacier");
			throw new SenderException(
					"Failed to perform [restore] action, and restore object with fileName ["
							+ fileName + "] from Amazon S3 Glacier");
		}

		String prefix = "Restoration status: %s.\n";
		return restoreFlag ? prefix + "in progress"
				: prefix + "not in progress (finished or failed)";
	}

	/**
	 * This method is wrapper which makes it possible for upload and copy actions to create a bucket and 
	 * incase a bucket already exists the operation will proceed without throwing an exception. 
	 *
	 * @param bucketName
	 *            The name of the bucket that is addressed. 
	 */
	public void bucketCreationWithObjectAction(String bucketName) throws SenderException {
		if (isBucketCreationEnabled())
			createBucket(bucketName, !bucketExistsThrowException);
		else
			throw new SenderException(getLogPrefix()
					+ " failed to create a bucket, to create a bucket bucketCreationEnabled attribute must be assinged to [true]");
	}

	/**
	 * This is a help method which throws an exception if a bucket does not exist.
	 *
	 * @param bucketName
	 *            The name of the bucket that is processed. 
	 */
	public void bucketDoesNotExist(String bucketName) throws SenderException {
		if (!s3Client.doesBucketExistV2(bucketName))
			throw new SenderException(getLogPrefix() + " bucket with bucketName [" + bucketName
					+ "] does not exist, please specify the name of an existing bucket");
	}

	/**
	 * This is a help method which throws an exception if a file does not exist.
	 *
	 * @param bucketName
	 *            The name of the bucket where the file is stored in.
	 * @param fileName
	 * 			  The name of the file that is processed. 
	 */
	public void fileDoesNotExist(String bucketName, String fileName) throws SenderException {
		if (!s3Client.doesObjectExist(bucketName, fileName))
			throw new SenderException(getLogPrefix() + " file with fileName [" + fileName
					+ "] does not exist, please specify the name of an existing file");
	}

	public boolean isForceGlobalBucketAccessEnabled() {
		return forceGlobalBucketAccessEnabled;
	}

	public void setForceGlobalBucketAccessEnabled(boolean forceGlobalBucketAccessEnabled) {
		this.forceGlobalBucketAccessEnabled = forceGlobalBucketAccessEnabled;
	}

	public boolean isBucketCreationEnabled() {
		return bucketCreationEnabled;
	}

	public void setBucketCreationEnabled(boolean bucketCreationEnabled) {
		this.bucketCreationEnabled = bucketCreationEnabled;
	}

	public boolean isStorageClassEnabled() {
		return storageClassEnabled;
	}

	public void setStorageClassEnabled(boolean storageClassEnabled) {
		this.storageClassEnabled = storageClassEnabled;
	}

	public String getBucketRegion() {
		return bucketRegion;
	}

	public void setBucketRegion(String bucketRegion) {
		this.bucketRegion = bucketRegion;
	}

	public boolean isBucketExistsThrowException() {
		return bucketExistsThrowException;
	}

	public void setBucketExistsThrowException(boolean bucketExistsThrowException) {
		this.bucketExistsThrowException = bucketExistsThrowException;
	}

	public String getLogPrefix() {
		return logPrefix;
	}

	public void setLogPrefix(String logPrefix) {
		this.logPrefix = logPrefix;
	}

	public void setLog(Logger log) {
		this.log = log;

	}

	public String getStorageClass() {
		return storageClass;
	}

	public void setStorageClass(String storageClass) {
		this.storageClass = storageClass;
	}

	public void setTier(String tier) {
		this.tier = tier;
	}

}
