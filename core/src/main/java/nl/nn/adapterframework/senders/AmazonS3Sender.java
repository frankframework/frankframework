package nl.nn.adapterframework.senders;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.amazonaws.services.s3.internal.BucketNameUtils;
import com.amazonaws.services.s3.model.S3Object;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDescription; 
import nl.nn.adapterframework.filesystem.AmazonS3FileSystem;
import nl.nn.adapterframework.filesystem.FileSystemSender;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;

@IbisDescription(
	"Sender to work with Amazon S3. " + 
	"<p><b>S3 File System specific Actions:</b></p>" + 
	"<p>The <code>createBucket</code> action requires bucket name as input. Bucket region must be specified.</p>" + 
	"<p>The <code>deleteBucket</code> action requires bucket name as input. </p>" + 
	"<p>The <code>copy</code> action requires the destinationFileName parameter to be set which should contain the name of the destination file. Destination bucket name must be specified. </p>" + 
	"<p>The <code>restore</code> action restores an archived copy of an object back into Amazon S3, requires object name as input. Tier must be specified. </p>" + 
	"<br/>" 
)
public class AmazonS3Sender extends FileSystemSender<S3Object, AmazonS3FileSystem> {

	private List<String> specificActions = Arrays.asList("createBucket", "deleteBucket", "copy", "restore");

	public AmazonS3Sender() {
		setFileSystem(new AmazonS3FileSystem());
		addActions(specificActions);
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (getAction().equalsIgnoreCase("createBucket") && getFileSystem().isForceGlobalBucketAccessEnabled() 
				&& (StringUtils.isEmpty(getFileSystem().getBucketRegion())
						|| !AmazonS3FileSystem.AVAILABLE_REGIONS.contains(getFileSystem().getBucketRegion()))) {
			throw new ConfigurationException(" invalid bucketRegion [" + getFileSystem().getBucketRegion()
					+ "] please use following supported regions " + AmazonS3FileSystem.AVAILABLE_REGIONS.toString());
		}
		else if (getAction().equalsIgnoreCase("copy")) {
			if (StringUtils.isEmpty(getFileSystem().getDestinationBucketName())
					|| !BucketNameUtils.isValidV2BucketName(getFileSystem().getDestinationBucketName()))
				throw new ConfigurationException(
						" invalid or empty destinationBucketName [" + getFileSystem().getDestinationBucketName()
								+ "] please visit AWS to see correct bucket naming");
			if (getParameterList().findParameter("destinationFileName") == null)
				throw new ConfigurationException(" destinationFileName parameter requires to be present to perform ["
						+ getAction() + "] action");
			if (getFileSystem().isStorageClassEnabled() && (StringUtils.isEmpty(getFileSystem().getStorageClass())
					|| !AmazonS3FileSystem.STORAGE_CLASSES.contains(getFileSystem().getStorageClass())))
				throw new ConfigurationException(" invalid storage class [" + getFileSystem().getStorageClass()
						+ "] please use following supported storage classes "
						+ AmazonS3FileSystem.STORAGE_CLASSES.toString());
		} else if (getAction().equalsIgnoreCase("restore") && (StringUtils.isEmpty(getFileSystem().getTier())
				|| !AmazonS3FileSystem.TIERS.contains(getFileSystem().getTier()))) {
			throw new ConfigurationException(
					" invalid tier when restoring an object from Amazon S3 Glacier, please use one of the following supported tiers: "
							+ AmazonS3FileSystem.TIERS.toString());
		}
	}

	@Override
	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc)
			throws SenderException, TimeOutException {
		if (!specificActions.contains(getAction())) {
			return super.sendMessage(correlationID, message, prc);
		}

		String result = null;
		String fileName = message;

		ParameterValueList pvl = null;
		if (prc != null && paramList != null) {
			try {
				pvl = prc.getValues(paramList);
			} catch (ParameterException e) {
				throw new SenderException(
						getLogPrefix() + "Sender [" + getName() + "] caught exception evaluating parameters", e);
			}
		}

		if (getAction().equalsIgnoreCase("createBucket")) { //createBucket block
			result = getFileSystem().createBucket(getFileSystem().getBucketName(), getFileSystem().isBucketExistsThrowException());
		} else if (getAction().equalsIgnoreCase("deleteBucket")) { //deleteBucket block
			result = getFileSystem().deleteBucket();
		} else if (getAction().equalsIgnoreCase("copy")) { //copy file block
			if (pvl.getParameterValue("destinationFileName") != null)
				if (pvl.getParameterValue("destinationFileName").getValue() != null) {
					String destinationFileName = pvl.getParameterValue("destinationFileName").getValue().toString();
					result = getFileSystem().copyObject(fileName, destinationFileName);
				} else
					throw new SenderException(getLogPrefix() + " no value in destinationFileName parameter found, please assing value to the parameter to perfom [copy] action");
			else
				throw new SenderException(getLogPrefix() + " no destinationFileName parameter found, it must be used to perform [copy] action");
		} else if (getAction().equalsIgnoreCase("restore")) //restore block
			result = getFileSystem().restoreObject(fileName);
		
		return result;
	}
	
	@IbisDoc({ "access key to access to the AWS resources owned by the account", "" })
	public void setAccessKey(String accessKey) {
		getFileSystem().setAccessKey(accessKey);
	}

	@IbisDoc({ "secret key to access to the AWS resources owned by the account", "" })
	public void setSecretKey(String secretKey) {
		getFileSystem().setSecretKey(secretKey);
	}

	@IbisDoc({ "alias used to obtain AWS credentials ", "" })
	public void setAuthAlias(String authAlias) {
		getFileSystem().setAuthAlias(authAlias);
	}

	@IbisDoc({ "setting this flag will result in disabling chunked encoding for all requests.", "false" })
	public void setChunkedEncodingDisabled(boolean chunkedEncodingDisabled) {
		getFileSystem().setChunkedEncodingDisabled(chunkedEncodingDisabled);
	}

	@IbisDoc({ "set whether the client should be configured with global bucket access enabled.", "false" }) 
	public void setForceGlobalBucketAccessEnabled(boolean forceGlobalBucketAccessEnabled) {
		getFileSystem().setForceGlobalBucketAccessEnabled(forceGlobalBucketAccessEnabled);
	}

	@IbisDoc({ "name of the region that the client will be created from", "eu-west-1" }) 
	public void setClientRegion(String clientRegion) {
		getFileSystem().setClientRegion(clientRegion);
	}

	@IbisDoc({ "name of the bucket to access", "" }) 
	public void setBucketName(String bucketName) {
		getFileSystem().setBucketName(bucketName);
	}
	
	@IbisDoc({ "name of the destination bucket name can be used for copy action", "" }) 
	public void setDestinationBucketName(String destinationBucketName) {
		getFileSystem().setDestinationBucketName(destinationBucketName);
	}

	@IbisDoc({ "name of the bucket region for create action", "" }) 
	public void setBucketRegion(String bucketRegion) {
		getFileSystem().setBucketRegion(bucketRegion);
	}

	@IbisDoc({ "name of the storage class for copy action. If storage class is enabled must be specified", "" }) 
	public void setStorageClass(String storageClass) {
		getFileSystem().setStorageClass(storageClass);
	}
	
	@IbisDoc({ "name of tier for restore action", "" }) 
	public void setTier(String tier) {
		getFileSystem().setTier(tier);
	}

	@IbisDoc({ "the time, in days, between when an object is restored to thebucket and when it expires", "" }) 
	public void setExpirationInDays(int expirationInDays) {
		getFileSystem().setExpirationInDays(expirationInDays);
	}
	
	@IbisDoc({ "enables storage class for copy action", "false" }) 
	public void setStorageClassEnabled(boolean storageClassEnabled) {
		getFileSystem().setStorageClassEnabled(storageClassEnabled);
	}

	@IbisDoc({ "enables creating bucket by upload and copy action if the bucket does not exist", "false" }) 
	public void setBucketCreationEnabled(boolean bucketCreationEnabled) {
		getFileSystem().setBucketCreationEnabled(bucketCreationEnabled);
	}
	
}
