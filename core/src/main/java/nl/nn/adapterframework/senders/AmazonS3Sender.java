/*
   Copyright 2019-2021 WeAreFrank!

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
package nl.nn.adapterframework.senders;

import com.amazonaws.services.s3.model.S3Object;

import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.filesystem.AmazonS3FileSystem;
import nl.nn.adapterframework.filesystem.FileSystemSender;

/**
 * Sender to work with Amazon S3. 
 * 
 * <p><b>S3 File System specific Actions:</b></p>
 * <p>The <code>createBucket</code> action requires bucket name as input. Bucket region must be specified.</p>
 * <p>The <code>deleteBucket</code> action requires bucket name as input. </p>
 * <p>The <code>copy</code> action requires the destinationFileName parameter to be set which should contain the name of the destination file. Destination bucket name must be specified. </p>
 * <p>The <code>restore</code> action restores an archived copy of an object back into Amazon S3, requires object name as input. Tier must be specified. </p>
 * 
 * <br/>
 */
public class AmazonS3Sender extends FileSystemSender<S3Object, AmazonS3FileSystem> {

//	private List<FileSystemAction> specificActions = Arrays.asList(FileSystemAction.CREATEBUCKET,FileSystemAction.DELETEBUCKET,FileSystemAction.RESTORE,FileSystemAction.COPYS3OBJECT);
	
	public AmazonS3Sender() {
		setFileSystem(new AmazonS3FileSystem());
//		addActions(specificActions);
	}

//	@Override
//	public void configure() throws ConfigurationException {
//		super.configure();
//		if (getActionEnum()==FileSystemAction.CREATEBUCKET && getFileSystem().isForceGlobalBucketAccessEnabled() 
//				&& (StringUtils.isEmpty(getFileSystem().getBucketRegion())
//						|| !AmazonS3FileSystem.AVAILABLE_REGIONS.contains(getFileSystem().getBucketRegion()))) {
//			throw new ConfigurationException(" invalid bucketRegion [" + getFileSystem().getBucketRegion()
//					+ "] please use following supported regions " + AmazonS3FileSystem.AVAILABLE_REGIONS.toString());
//		}
//		if (getActionEnum()==FileSystemAction.COPY) {
//			if (StringUtils.isEmpty(getFileSystem().getDestinationBucketName())
//					|| !BucketNameUtils.isValidV2BucketName(getFileSystem().getDestinationBucketName()))
//				throw new ConfigurationException(
//						" invalid or empty destinationBucketName [" + getFileSystem().getDestinationBucketName()
//								+ "] please visit AWS to see correct bucket naming");
//			if (getParameterList().findParameter("destinationFileName") == null)
//				throw new ConfigurationException(" destinationFileName parameter requires to be present to perform ["
//						+ getActionEnum() + "] action");
//			if (getFileSystem().isStorageClassEnabled() && (StringUtils.isEmpty(getFileSystem().getStorageClass())
//					|| !AmazonS3FileSystem.STORAGE_CLASSES.contains(getFileSystem().getStorageClass())))
//				throw new ConfigurationException(" invalid storage class [" + getFileSystem().getStorageClass()
//						+ "] please use following supported storage classes "
//						+ AmazonS3FileSystem.STORAGE_CLASSES.toString());
//		} 
//		else if (getActionEnum()==FileSystemAction.RESTORE && (StringUtils.isEmpty(getFileSystem().getTier())
//				|| !AmazonS3FileSystem.TIERS.contains(getFileSystem().getTier()))) {
//			throw new ConfigurationException(
//					" invalid tier when restoring an object from Amazon S3 Glacier, please use one of the following supported tiers: "
//							+ AmazonS3FileSystem.TIERS.toString());
//		}
//	}

//	@Override
//	public PipeRunResult sendMessage(Message message, PipeLineSession session, IForwardTarget next) throws SenderException, TimeOutException {
//
//		String result = null;
//		String fileName;
//		try {
//			fileName = message.asString();
//		} catch (IOException e) {
//			throw new SenderException(e);
//		}
//
//		ParameterValueList pvl = null;
//		if (getParameterList() != null) {
//			try {
//				pvl = getParameterList().getValues(message, session);
//			} catch (ParameterException e) {
//				throw new SenderException(getLogPrefix() + "Sender [" + getName() + "] caught exception evaluating parameters", e);
//			}
//		}

//		switch(getActionEnum()) {
//			case CREATEBUCKET: 
//				result = getFileSystem().createBucket(getFileSystem().getBucketName(), getFileSystem().isBucketExistsThrowException());
//				break;
//			case DELETEBUCKET:
//				result = getFileSystem().deleteBucket();
//				break;
//			case COPY:
//				if (pvl.getParameterValue("destinationFileName") != null) {
//					if (pvl.getParameterValue("destinationFileName").getValue() != null) {
//						String destinationFileName = pvl.getParameterValue("destinationFileName").getValue().toString();
//						result = getFileSystem().copyObject(fileName, destinationFileName);
//					} else {
//						throw new SenderException(getLogPrefix() + " no value in destinationFileName parameter found, please assing value to the parameter to perfom [copy] action");
//					}
//				} else {
//					throw new SenderException(getLogPrefix() + " no destinationFileName parameter found, it must be used to perform [copy] action");
//				}
//				break;
//			case RESTORE:
//				result = getFileSystem().restoreObject(fileName);
//				break;
//			default:
//				return super.sendMessage(message, session, next);
//		}

//		return super.sendMessage(message, session, next);
//	}
	
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
	
//	@IbisDoc({ "name of the destination bucket name can be used for copy action", "" }) 
//	public void setDestinationBucketName(String destinationBucketName) {
//		getFileSystem().setDestinationBucketName(destinationBucketName);
//	}

//	@IbisDoc({ "name of the bucket region for create action", "" }) 
//	public void setBucketRegion(String bucketRegion) {
//		getFileSystem().setBucketRegion(bucketRegion);
//	}

//	@IbisDoc({ "name of the storage class for copy action. If storage class is enabled must be specified", "" }) 
//	public void setStorageClass(String storageClass) {
//		getFileSystem().setStorageClass(storageClass);
//	}
	
//	@IbisDoc({ "name of tier for restore action", "" }) 
//	public void setTier(String tier) {
//		getFileSystem().setTier(tier);
//	}

//	@IbisDoc({ "the time, in days, between when an object is restored to thebucket and when it expires. Use <code>-1</code> never expire", "-1" }) 
//	public void setExpirationInDays(int expirationInDays) {
//		getFileSystem().setExpirationInDays(expirationInDays);
//	}
	
//	@IbisDoc({ "enables storage class for copy action", "false" }) 
//	public void setStorageClassEnabled(boolean storageClassEnabled) {
//		getFileSystem().setStorageClassEnabled(storageClassEnabled);
//	}

//	@IbisDoc({ "enables creating bucket by upload and copy action if the bucket does not exist", "false" }) 
//	public void setBucketCreationEnabled(boolean bucketCreationEnabled) {
//		getFileSystem().setBucketCreationEnabled(bucketCreationEnabled);
//	}
	
	@IbisDoc({ "setting proxy host", "" })
	public void setProxyHost(String proxyHost) {
		getFileSystem().setProxyHost(proxyHost);
	}
	
	@IbisDoc({ "setting proxy port", "" })
	public void setProxyPort(Integer proxyPort) {
		getFileSystem().setProxyPort(proxyPort);
	}

	@IbisDoc({ "maximum concurrent connections towards S3", "50" })
	public void setMaxConnections(int maxConnections) {
		getFileSystem().setMaxConnections(maxConnections);
	}

	@IbisDoc({ "name of the region that the client will be created from", "" })
	public void setServiceEndpoint(String bucketName) {
		getFileSystem().setServiceEndpoint(bucketName);
	}

	@IbisDoc({ "set the desired storage class for the S3 object when action is move,copy or write", "Standard" })
	public void setStorageClass(StorageClass storageClass) {
		getFileSystem().setStorageClass(storageClass);
	}
}
