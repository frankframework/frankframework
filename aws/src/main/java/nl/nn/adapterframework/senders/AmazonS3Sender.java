/*
   Copyright 2019-2023 WeAreFrank!

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

import nl.nn.adapterframework.filesystem.AmazonS3FileSystem;
import nl.nn.adapterframework.filesystem.AmazonS3FileSystemDelegator;
import nl.nn.adapterframework.filesystem.FileSystemSender;

/**
 * Sender to work with Amazon S3.
 */
public class AmazonS3Sender extends FileSystemSender<S3Object, AmazonS3FileSystem> implements AmazonS3FileSystemDelegator {

//	 * <p><b>S3 File System specific Actions:</b></p>
//	 * <p>The <code>createBucket</code> action requires bucket name as input. Bucket region must be specified.</p>
//	 * <p>The <code>deleteBucket</code> action requires bucket name as input. </p>
//	 * <p>The <code>copy</code> action requires the destinationFileName parameter to be set which should contain the name of the destination file. Destination bucket name must be specified. </p>
//	 * <p>The <code>restore</code> action restores an archived copy of an object back into Amazon S3, requires object name as input. Tier must be specified. </p>

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

}
