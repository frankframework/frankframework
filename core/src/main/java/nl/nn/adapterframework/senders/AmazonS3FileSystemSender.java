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
import nl.nn.adapterframework.filesystem.AmazonS3FileSystem;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;

public class AmazonS3FileSystemSender extends FileSystemSender<S3Object, AmazonS3FileSystem> {

	private List<String> specificActions = Arrays.asList("createBucket", "deleteBucket", "copy", "restore");

	public AmazonS3FileSystemSender() {
		setFileSystem(new AmazonS3FileSystem());
		addActions(specificActions);
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (getAction().equalsIgnoreCase("createBucket") && getFileSystem().isForceGlobalBucketAccessEnabled()
				&& (StringUtils.isEmpty(getFileSystem().getBucketRegion())
						|| !AmazonS3FileSystem.AVAILABLE_REGIONS.contains(getFileSystem().getBucketRegion())))
			throw new ConfigurationException(" invalid bucketRegion [" + getFileSystem().getBucketRegion()
					+ "] please use following supported regions " + AmazonS3FileSystem.AVAILABLE_REGIONS.toString());
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
			result = getFileSystem().createBucket(getFileSystem().getBucketName(),
					getFileSystem().isBucketExistsThrowException());
		} else if (getAction().equalsIgnoreCase("deleteBucket")) //deleteBucket block
			result = getFileSystem().deleteBucket(getFileSystem().getBucketName());
		else if (getAction().equalsIgnoreCase("copy")) //copy file block
			if (pvl.getParameterValue("destinationFileName") != null)
				if (pvl.getParameterValue("destinationFileName").getValue() != null) {
					String destinationFileName = pvl.getParameterValue("destinationFileName").getValue().toString();
					result = getFileSystem().copyObject(getFileSystem().getBucketName(), fileName,
							getFileSystem().getDestinationBucketName(), destinationFileName);
				} else
					throw new SenderException(getLogPrefix()
							+ " no value in destinationFileName parameter found, please assing value to the parameter to perfom [copy] action");
			else
				throw new SenderException(getLogPrefix()
						+ " no destinationFileName parameter found, it must be used to perform [copy] action");
		else if (getAction().equalsIgnoreCase("restore")) //restore block
			result = getFileSystem().restoreObject(getFileSystem().getBucketName(), fileName,
					getFileSystem().getExperationInDays());
		return result;
	}

	public void setAccessKey(String accessKey) {
		getFileSystem().setAccessKey(accessKey);
	}

	public void setSecretKey(String secretKey) {
		getFileSystem().setSecretKey(secretKey);
	}

	public void setAuthAlias(String authAlias) {
		getFileSystem().setAuthAlias(authAlias);
	}

	public void setChunkedEncodingDisabled(boolean chunkedEncodingDisabled) {
		getFileSystem().setChunkedEncodingDisabled(chunkedEncodingDisabled);
	}

	public void setForceGlobalBucketAccessEnabled(boolean forceGlobalBucketAccessEnabled) {
		getFileSystem().setForceGlobalBucketAccessEnabled(forceGlobalBucketAccessEnabled);
	}

	public void setClientRegion(String clientRegion) {
		getFileSystem().setClientRegion(clientRegion);
	}

	public void setBucketName(String bucketName) {
		getFileSystem().setBucketName(bucketName);
	}

	public void setDestinationBucketName(String destinationBucketName) {
		getFileSystem().setDestinationBucketName(destinationBucketName);
	}

	public void setBucketRegion(String bucketRegion) {
		getFileSystem().setBucketRegion(bucketRegion);
	}

	public void setStorageClass(String storageClass) {
		getFileSystem().setStorageClass(storageClass);
	}

	public void setTier(String tier) {
		getFileSystem().setTier(tier);
	}

	public void setExperationInDays(int experationInDays) {
		getFileSystem().setExperationInDays(experationInDays);
	}

	public void setStorageClassEnabled(boolean storageClassEnabled) {
		getFileSystem().setStorageClassEnabled(storageClassEnabled);
	}

	public void setBucketCreationEnabled(boolean bucketCreationEnabled) {
		getFileSystem().setBucketCreationEnabled(bucketCreationEnabled);
	}

	public void setTimeout(long timeout) {
		getFileSystem().setTimeout(timeout);
	}

}
