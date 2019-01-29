/*
   Copyright 2019 Integration Partners B.V.
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.internal.BucketNameUtils;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.DeleteBucketRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.RestoreObjectRequest;
import com.amazonaws.services.s3.model.StorageClass;
import com.amazonaws.services.s3.model.Tier;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.CredentialFactory;

/**
 * <p>
 * S3Sender, makes possible for Ibis developer to interact with Amazon Simple Storage Service (Amazon S3). It allows to create
 * and delete buckets(directories). More so it makes possible for you to upload file(s) into a bucket, delete file(s) from a bucket and 
 * copy file(s) from one bucket too another.
 * </p>
 * 
 * <p>
 * <b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>This attribute is used for naming your S3 sender.</td><td></td></tr>
 * <tr><td>{@link #setAccessKey(String) accessKey}</td><td>This attribute is used for aws access key.</td><td></td></tr>
 * <tr><td>{@link #setSecretKey(String) secretKey}</td><td>This attribute is used for aws secret key.</td><td></td></tr>
 * <tr><td>{@link #setChunkedEncodingDisabled(boolean) chunkedEncodingDisabled}</td><td>Configures the client to disable chunked encoding for all requests.</td><td>false</td></tr>
 * <tr><td>{@link #setAccelerateModeEnabled(boolean) accelerateModeEnabled}</td><td>Configures the client to use S3 accelerate endpoint for all requests. (Making use of this service involves extra costs)</td><td>false</td></tr>
 * <tr><td>{@link #setForceGlobalBucketAccessEnabled(boolean) forceGlobalBucketAccessEnabled}</td><td>Configure whether global bucket access is enabled for this client. When enabled client in a specified region is allowed to create buckets in other regions.</td><td>false</td></tr>
 * <tr><td>{@link #setBucketCreationEnabled(boolean) bucketCreationEnabled}</td><td>Configuring this attribute and setting it to 'true' allows bucket creation when uploading/copying an file to a non-existent bucket. Otherwise an exception will be thrown.</td><td>false</td></tr>
 * <tr><td>{@link #setStorageClassEnabled(boolean) storageClassEnabled}</td><td>Configuring this attribute and setting it to 'true' allows changing storage class by using [copy] action.</td><td>false</td></tr>
 *
 * <tr><td>{@link #setClientRegion(String) clientRegion}</td><td>Set a region endpoint for this client to work with:
 * <ul><li>us-gov-west-1, us-east-1, us-east-2, us-west-1, us-west-2</li>
 * <li>eu-west-1, eu-west-2, eu-west-3, eu-central-1</li>
 * <li>ap-south-1, ap-southeast-1, ap-southeast-2, ap-northeast-1, ap-northeast-2</li>
 * <li>sa-east-1, cn-north-1, cn-northwest-1, ca-central-1</li></ul>Requests that perform operations on S3 resources can only be done on set region unless forceGlobalBucketAccessEnabled is set to 'true'.</td><td>"eu-central-1"</td></tr>
 * <tr><td>{@link #setBucketRegion(String) bucketRegion}</td><td>This attribute is used when forceGlobalBucketAccessEnabled is set to 'true' in order to access or create a bucket that is different location then the clientRegion.</td><td></td></tr>
 * <tr><td>{@link #setBucketName(String) bucketName}</td><td>Set a name for a new or an existing bucket. (this is dependent on the action)</td><td></td></tr>
 * <tr><td>{@link #setDestinationBucketName(String) destinationBucketName}</td><td>Set a name for a new or an existing destination bucket while performing [copy] action.</td><td></td></tr>
 * <tr><td>{@link #setStorageClass(String) storageClass}</td><td>When {@link isStorageClassEnabled} is set to 'true' a storage class needs to be selected. Available storage classes:
 * <ul><li>STANDARD: The default Amazon S3 storage class. This storage class is recommended for critical, non-reproducible data.  The standard storage class is a highly available and highly redundant storage option provided for an affordable price.</li>
 * <li>REDUCED_REDUNDANCY: The reduced redundancy storage class. This storage class allows customers to reduce their storage costs in return for a reduced level of data redundancy. Customers who are using Amazon S3 for storing non-critical, reproducible data can choose this low cost and highly available, but less redundant, storage option.</li>
 * <li>GLACIER: The Amazon Glacier storage class. This storage class means your object's data is stored in Amazon Glacier, and Amazon S3 stores a reference to the data in the Amazon S3 bucket.</li>
 * <li>STANDARD_IA: Standard Infrequent Access storage class.</li>
 * <li>ONEZONE_IA: One Zone Infrequent Access storage class stores object data in only one Availability Zone at a lower price than STANDARD_IA.</li></ul></td><td></td></tr>
 * <tr><td>{@link #setTier(String) tier}</td><td>When restoring an object from Amazon S3 Glacier, you can give a retrieval option. Available tiers:
 * <ul><li>Expedited: Quicky access data below 250mb, data is made available within 1-5min. For more information search for Provisioned Capacity</li>
 * <li>Standard: Allows to access any archived data within 3-5 hours. Default option.</li>
 * <li>Bulk: Lowest cost retrieval option, enabling restore large amounts of data, even petabytes with low cost within a day. 5-12hours duration.</li></ul></td><td>Standard</td></tr>
 * <tr><td>{@link #setActions(String) actions}</td><td>Available actions are:
 * <ul><li>createBucket: create a new bucket</li>
 * <li>deleteBucket: delete an existing bucket</li>
 * <li>upload: uploads a file into a bucket, when bucket doesn't exist bucketCreationEnabled can be set to 'true' so this action can also create a bucket (file parameter required)</li>
 * <li>download: download a file from a S3 bucket and safe the InputStream in storeResultInSessionKey</li>
 * <li>copy: copies a file from one bucket to another, when destination bucket doesn't exist bucketCreationEnabled can be set to 'true' so this action also creates a destination bucket (destinationBucketName and destinationFileName parameter required)</li>
 * <li>delete: delete a file from inside a S3 bucket</li>
 * <li>restore: restore an object that is stored within Amazon S3 Glacier</li></ul></td><td></td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>Set a value for sessionKey in which result will be stored.</td><td></td></tr>
 * </table>
 * </p>
 * 
 * <p>
 * <b>Parameters:</b>
 * <table border="1">
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>fileName</td><td><i>String</i></td><td>(Optional) When an parameter with name fileName is configured, it is used instead of the message</td></tr>
 * <tr><td>file</td><td><i>Stream</i></td><td>This parameter contains InputStream, it must be present when performing upload action</td></tr>
 * <tr><td>destinationFileName</td><td><i>String</i></td><td>This parameter specifies the name of the copied file, it must be present when performing copy action</td></tr>
 * </table>
 * </p>
 * 
 * <p>
 * This is a list containing configurations that exist in AmazonS3 API and are not used in this sender:
 * <ul>
 * <li>bucket analytics</li>
 * <li>bucket cross origin configuration</li>
 * <li>bucket encryption</li>
 * <li>bucket inventory configuration</li>
 * <li>bucket lifecycle configuration</li>
 * <li>bucket metric configuration</li>
 * <li>bucket policy</li>
 * <li>bucket replication configuration</li>
 * <li>bucket tagging configuration</li>
 * <li>bucket website configuration</li>
 * <li>bucket requester configuration</li>
 * </ul>
 * </p>
 * 
 * @author R. Karajev
 */

public class AmazonS3Sender extends SenderWithParametersBase
{
	private static final List<String> AVAILABLE_REGIONS = getAvailableRegions();
	private static final List<String> STORAGE_CLASSES = getStorageClasses();
	private static final List<String> TIERS = getTiers();

	private List<String> availableActions = Arrays.asList("createBucket", "deleteBucket", "upload", "download", "copy", "delete", "restore");

	private String accessKey;
	private String secretKey;
	private String authAlias;

	private String name;
	private AmazonS3 s3Client;
	private boolean chunkedEncodingDisabled = false;
	private boolean accelerateModeEnabled = false; // this may involve some extra costs
	private boolean forceGlobalBucketAccessEnabled = false;
	private boolean bucketCreationEnabled = false;
	private boolean storageClassEnabled = false;

	private String actions;
	private String clientRegion = Regions.US_EAST_1.getName();
	private String bucketName;
	private String bucketRegion;
	private String destinationBucketName;

	private String storageClass;
	private String tier = Tier.Standard.toString();
	private int experationInDays = -1;

	private String storeResultInSessionKey;
	private boolean bucketExistsThrowException = true;


	@Override
	public void configure() throws ConfigurationException
	{
		super.configure();
		if(StringUtils.isEmpty(getAccessKey()) || StringUtils.isEmpty(getSecretKey()))
			throw new ConfigurationException(getLogPrefix()+" empty credential fields, please prodive aws credentials");

		if(StringUtils.isEmpty(getClientRegion()) || !AVAILABLE_REGIONS.contains(getClientRegion()))
			throw new ConfigurationException(getLogPrefix() + " invalid region [" + getClientRegion() + "] please use one of the following supported regions " + AVAILABLE_REGIONS.toString());

		if(StringUtils.isEmpty(getBucketName()) || !BucketNameUtils.isValidV2BucketName(getBucketName()))
			throw new ConfigurationException(getLogPrefix() + " invalid or empty bucketName [" + getBucketName() + "] please visit AWS to see correct bucket naming");

		StringTokenizer tokenizer = new StringTokenizer(getActions(), " ,\t\n\r\f");
		while (tokenizer.hasMoreTokens()) 
		{
			String action = tokenizer.nextToken();

			if(StringUtils.isEmpty(action) || !availableActions.contains(action))
				throw new ConfigurationException(getLogPrefix()+" invalid action [" + action + "] please use one of the following supported actions " + availableActions.toString());	

			if(action.equalsIgnoreCase("createBucket") && isForceGlobalBucketAccessEnabled() && (StringUtils.isEmpty(getBucketRegion()) || !AVAILABLE_REGIONS.contains(getBucketRegion())))
				throw new ConfigurationException(getLogPrefix()+" invalid bucketRegion [" + getBucketRegion() + "] please use following supported regions " + AVAILABLE_REGIONS.toString());

			if(action.equalsIgnoreCase("upload") || action.equalsIgnoreCase("copy"))
			{
				ParameterList parameterList = getParameterList();

				if(action.equalsIgnoreCase("upload") && parameterList.findParameter("file") == null)
					throw new ConfigurationException(getLogPrefix()+" file parameter requires to be present to perform [" + action + "] action");

				if(action.equalsIgnoreCase("copy"))
				{
					if(StringUtils.isEmpty(getDestinationBucketName()) || !BucketNameUtils.isValidV2BucketName(getDestinationBucketName()))
						throw new ConfigurationException(getLogPrefix() + " invalid or empty destinationBucketName [" + getDestinationBucketName() + "] please visit AWS to see correct bucket naming");
					if(parameterList.findParameter("destinationFileName") == null)
						throw new ConfigurationException(getLogPrefix()+" destinationFileName parameter requires to be present to perform [" + action + "] action");
					if(isStorageClassEnabled() && (StringUtils.isEmpty(getStorageClass()) || !STORAGE_CLASSES.contains(getStorageClass())))
						throw new ConfigurationException(getLogPrefix()+" invalid storage class ["+getStorageClass()+"] please use following supported storage classes " + STORAGE_CLASSES.toString());
				
				}
			}

/*			if(action.equalsIgnoreCase("accelerateMode"))
				if(!isAccelerateModeEnabled())
					throw new ConfigurationException(getLogPrefix()+" when performing ["+action+"] action, accelerateModeEnabled attribute should be set to 'true'");
*/			
			if(action.equalsIgnoreCase("restore") && (StringUtils.isEmpty(getTier()) || !TIERS.contains(getTier())))
				throw new ConfigurationException(getLogPrefix()+" invalid tier when restoring an object from Amazon S3 Glacier, please use one of the following supported tiers: "+ TIERS.toString());
	    }
	}

	@Override
	public void open()
	{
		
		CredentialFactory cf = new CredentialFactory(getAuthAlias(), getAccessKey(), getSecretKey());
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(cf.getUsername(), cf.getPassword());
		AmazonS3ClientBuilder s3ClientBuilder = AmazonS3ClientBuilder.standard()
				.withChunkedEncodingDisabled(isChunkedEncodingDisabled())
				.withAccelerateModeEnabled(isAccelerateModeEnabled())
				.withForceGlobalBucketAccessEnabled(isForceGlobalBucketAccessEnabled())
				.withRegion(getClientRegion())
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds));

		s3Client = s3ClientBuilder.build();
	}

	@Override
	public void close()
	{
		s3Client.shutdown();
	}

	@Override
	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException
	{
		//fills ParameterValueList pvl with the set parameters from S3Sender
		ParameterValueList pvl = null;
		String generalFileName = null;
		try
		{
			if (prc != null && paramList != null)
				pvl = prc.getValues(paramList);

			if(pvl == null || pvl.getParameterValue("fileName") == null)
				generalFileName = message;
			else
				generalFileName = pvl.getParameterValue("fileName").getValue().toString(); //DONE this need to be fixed! When fileName parameter not assigned generalFileName is null somehow, how?
		}
		catch (ParameterException e)
		{
			throw new SenderException(getLogPrefix() + "Sender [" + getName() + "] caught exception evaluating parameters", e);
		}
		catch (NullPointerException e)
		{
			throw new SenderException(getLogPrefix() + "Sender [" + getName() + "] caught NullPointerException");
		}

		StringTokenizer tokenizer = new StringTokenizer(getActions(), " ,\t\n\r\f");
		String result = null;
		while (tokenizer.hasMoreTokens())
		{
			String action = tokenizer.nextToken();
			if(!(action.equalsIgnoreCase("createBucket") || action.equalsIgnoreCase("deleteBucket")) && StringUtils.isEmpty(generalFileName) && StringUtils.isEmpty(message))
				throw new SenderException(getLogPrefix() + " no value found for the fileName and message parameter, atleast one value has to be assigned");

			if(action.equalsIgnoreCase("createBucket"))												//createBucket block
				result = createBucket(getBucketName(), bucketExistsThrowException);
			else if(action.equalsIgnoreCase("deleteBucket"))										//deleteBucket block
				result = deleteBucket(getBucketName());
			else if(action.equalsIgnoreCase("upload"))												//upload file block
				if(pvl.getParameterValue("file") != null)
					if(pvl.getParameterValue("file").getValue() != null)
					{
						InputStream inputStream = (InputStream) pvl.getParameterValue("file").getValue();
						result = uploadObject(getBucketName(), generalFileName, inputStream);
					}
					else
						throw new SenderException(getLogPrefix() + " no value was assinged for file parameter");
				else
					throw new SenderException(getLogPrefix() + " file parameter doesn't exist, please use file parameter to perform [upload] action");
			else if(action.equalsIgnoreCase("download"))											//download file block
				result = downloadObject(getBucketName(), generalFileName, prc);
			else if(action.equalsIgnoreCase("copy"))												//copy file block
				if(pvl.getParameterValue("destinationFileName") != null)
					if(pvl.getParameterValue("destinationFileName").getValue() != null)
					{
						String destinationFileName = pvl.getParameterValue("destinationFileName").getValue().toString();
						result = copyObject(getBucketName(), generalFileName, getDestinationBucketName(), destinationFileName);
					}
					else
						throw new SenderException(getLogPrefix() + " no value in destinationFileName parameter found, please assing value to the parameter to perfom [copy] action");
				else
					throw new SenderException(getLogPrefix() + " no destinationFileName parameter found, it must be used to perform [copy] action");
			else if(action.equalsIgnoreCase("delete"))												//delete file block
				result = deleteObject(getBucketName(), generalFileName);
			else if(action.equalsIgnoreCase("restore"))
				result = restoreObject(getBucketName(), generalFileName, getExperationInDays());
	    }

		System.out.println("Return message: "+result);
		return result;
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
	protected String createBucket(String bucketName, boolean bucketExistsThrowException) throws SenderException
	{
		try
		{
			if(!s3Client.doesBucketExistV2(bucketName))
			{
				CreateBucketRequest createBucketRequest = null;
				if(isForceGlobalBucketAccessEnabled())
					createBucketRequest = new CreateBucketRequest(bucketName, getBucketRegion());
				else
					createBucketRequest = new CreateBucketRequest(bucketName);			
				s3Client.createBucket(createBucketRequest);
				log.debug("Bucket with bucketName: ["+bucketName+"] is created.");
			}
			else
				if(bucketExistsThrowException)
					throw new SenderException(getLogPrefix() + " bucket with bucketName [" + bucketName + "] already exists, please specify a unique bucketName");

		}
		catch(AmazonServiceException e)
		{
			log.warn("Failed to create bucket with bucketName ["+bucketName+"].");
			throw new SenderException("Failed to create bucket with bucketName ["+bucketName+"].");
		}

		return bucketName;
	}

	/**
     * Deletes a bucket on Amazon S3.
     *
     * @param bucketName
     *            The name for a bucket that is desired to be deleted.
     */
	protected String deleteBucket(String bucketName) throws SenderException
	{
		try
		{
			bucketDoesNotExist(bucketName);
			DeleteBucketRequest deleteBucketRequest = new DeleteBucketRequest(bucketName);
			s3Client.deleteBucket(deleteBucketRequest);
			log.debug("Bucket with bucketName [" + bucketName + "] is deleted.");
		}
		catch(AmazonServiceException e)
		{
			log.warn("Failed to delete bucket with bucketName [" + bucketName + "].");
			throw new SenderException("Failed to delete bucket with bucketName [" + bucketName + "].");
		}

		return bucketName;
	}


	/**
     * Uploads a file to Amazon S3 bucket.
     *
     * @param bucketName
     *            The name of the bucket where the file shall be stored in.
     * @param fileName
     * 			  The name that shall be given to the file that is uploaded to Amazon S3 bucket. 
     * @param pvl
     * 			  This object is given in order to get the contents of the file that is assigned to be used.
     */
	protected String uploadObject(String bucketName, String fileName, InputStream inputStream) throws SenderException
	{	
		try
		{
			if(!s3Client.doesBucketExistV2(bucketName))
				bucketCreationWithObjectAction(bucketName);
			if(!s3Client.doesObjectExist(bucketName, fileName))
			{
				ObjectMetadata metadata = new ObjectMetadata();
				metadata.setContentType("application/octet-stream");	
				PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, inputStream, metadata);
				s3Client.putObject(putObjectRequest);
				log.debug("Object with fileName [" + fileName + "] uploaded into bucket with bucketName [" + bucketName + "]");
			}
			else
				throw new SenderException(getLogPrefix() + " file with given name already exists, please specify a new name for your file");			
		}
		catch(AmazonServiceException e)
		{
			log.warn("Failed to upload object with fileName [" + fileName + "] into bucket with bucketName [" + bucketName + "]");
			throw new SenderException("Failed to upload object with fileName [" + fileName + "] into bucket with bucketName [" + bucketName + "]");
		}

		return fileName;
	}

	/**
     * Downloads a file from Amazon S3 bucket.
     *
     * @param bucketName
     *            The name of the bucket where the file is stored in.
     * @param fileName
     * 			  This parameter is used for controlling the behavior for weather an exception has to be thrown or not. 
     * 			  In case of upload action being configured to be able to create a bucket, an exception will not be thrown when a bucket with assigned bucketName already exists.
     */
	protected String downloadObject(String bucketName, String fileName, ParameterResolutionContext prc) throws SenderException
	{
		//TODO this method needs to be changed, first of all the attribute storeResultInSessionKey should be removed
		//TODO as it creates confusion with the generic storeResultInSessionKey attribute, the file transfer type (is it possible for different type?)
		S3ObjectInputStreamCloser s3InputStream = null;
		try
		{
			bucketDoesNotExist(bucketName);
			fileDoesNotExist(bucketName, fileName);
			GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, fileName);
			s3InputStream = new S3ObjectInputStreamCloser(s3Client.getObject(getObjectRequest).getObjectContent());
			log.debug("Object with fileName [" + fileName + "] downloaded from bucket with bucketName [" + bucketName + "]");
		}
		catch(AmazonServiceException e)
		{
			log.error("Failed to download object with fileName [" + fileName + "] from bucket with bucketName [" + bucketName + "]");			
			throw new SenderException("Failed to perform copy action from bucket ["+bucketName+"]");
		}

		try 
		{
			IPipeLineSession session=null;
			if (prc!=null)
			{
				session=prc.getSession();
				session.put(getStoreResultInSessionKey(), s3InputStream);				
			}
		}
		catch(Exception e) 
		{
			throw new SenderException("Error during processing of the inputStream ", e);
		}

		return getStoreResultInSessionKey();
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
	protected String copyObject(String bucketName, String fileName, String destinationBucketName, String destinationFileName) throws SenderException
	{				
		try
		{
			bucketDoesNotExist(bucketName);					//if bucket does not exists this method throws and exception
			fileDoesNotExist(bucketName, fileName);			//if object does not exists this method throws and exception
			if(!s3Client.doesBucketExistV2(destinationBucketName))
				bucketCreationWithObjectAction(destinationBucketName);
			if(!s3Client.doesObjectExist(destinationBucketName, destinationFileName))
			{
				CopyObjectRequest copyObjectRequest = new CopyObjectRequest(bucketName, fileName, destinationBucketName, destinationFileName);
				if(isStorageClassEnabled())
					copyObjectRequest.setStorageClass(getStorageClass());
				s3Client.copyObject(copyObjectRequest);
				log.debug("Object with fileName [" + fileName + "] copied from bucket with bucketName [" + bucketName + "] into bucket with bucketName [" + destinationBucketName + "] and new fileName [" + destinationFileName + "]");
			}
			else
				throw new SenderException(getLogPrefix() + " file with given name already exists, please specify a new name");
		}
		catch(AmazonServiceException e)
		{
			log.error("Failed to perform [copy] action on object with fileName ["+fileName+"]");
			throw new SenderException("Failed to perform [copy] action on object with fileName ["+fileName+"]");
		}

		return destinationFileName;
	}

	/**
     * Deletes a file from Amazon S3 bucket.
     *
     * @param bucketName
     *            The name of the bucket where the file is stored in.
     * @param fileName
     * 			   
     */
	protected String deleteObject(String bucketName, String fileName) throws SenderException
	{
		try
		{
			bucketDoesNotExist(bucketName);
			fileDoesNotExist(bucketName, fileName);
			DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(bucketName, fileName);
			s3Client.deleteObject(deleteObjectRequest);
			log.debug("Object with fileName [" + fileName + "] deleted from bucket with bucketName [" + bucketName + "]");
		}
		catch(AmazonServiceException e)
		{
			log.error("Failed to perform [delete] action on object with fileName [" + fileName + "]");
			throw new SenderException("Failed to perform [delete] action on object with fileName [" + fileName + "]");
		}

		return fileName;
	}


	protected String restoreObject(String bucketName, String fileName, int experationInDays) throws SenderException
	{
		Boolean restoreFlag;
		try 
		{
			bucketDoesNotExist(bucketName);
			fileDoesNotExist(bucketName, fileName);
            RestoreObjectRequest requestRestore = new RestoreObjectRequest(bucketName, fileName, experationInDays).withTier(getTier());
            s3Client.restoreObjectV2(requestRestore);
            log.debug("Object with fileName [" + fileName + "] and bucketName [" + bucketName + "] restored from Amazon S3 Glacier");

            ObjectMetadata response = s3Client.getObjectMetadata(bucketName, fileName);
            restoreFlag = response.getOngoingRestore();
            System.out.format("Restoration status: %s.\n", restoreFlag ? "in progress" : "not in progress (finished or failed)");

        }
        catch(AmazonServiceException e) 
		{
			log.error("Failed to perform [restore] action, and restore object with fileName ["+fileName+"] from Amazon S3 Glacier");
			throw new SenderException("Failed to perform [restore] action, and restore object with fileName ["+fileName+"] from Amazon S3 Glacier");        
        }

		String prefix = "Restoration status: %s.\n";
		return restoreFlag ? prefix + "in progress" : prefix + "not in progress (finished or failed)";
	}

	/**
     * This method is wrapper which makes it possible for upload and copy actions to create a bucket and 
     * incase a bucket already exists the operation will proceed without throwing an exception. 
     *
     * @param bucketName
     *            The name of the bucket that is addressed. 
     */
	public void bucketCreationWithObjectAction(String bucketName) throws SenderException
	{		
		if(isBucketCreationEnabled())
			createBucket(bucketName, !bucketExistsThrowException);
		else
			throw new SenderException(getLogPrefix() + " failed to create a bucket, to create a bucket bucketCreationEnabled attribute must be assinged to [true]");	
	}

	/**
     * This is a help method which throws an exception if a bucket does not exist.
     *
     * @param bucketName
     *            The name of the bucket that is processed. 
     */
	public void bucketDoesNotExist(String bucketName) throws SenderException
	{
		if(!s3Client.doesBucketExistV2(bucketName))
			throw new SenderException(getLogPrefix() + " bucket with bucketName [" + bucketName + "] does not exist, please specify the name of an existing bucket");
	}

	/**
     * This is a help method which throws an exception if a file does not exist.
     *
     * @param bucketName
     *            The name of the bucket where the file is stored in.
     * @param fileName
     * 			  The name of the file that is processed. 
     */
	public void fileDoesNotExist(String bucketName, String fileName) throws SenderException
	{
		if(!s3Client.doesObjectExist(bucketName, fileName))
			throw new SenderException(getLogPrefix() + " file with fileName ["+ fileName +"] does not exist, please specify the name of an existing file");
	}

	public static List<String> getAvailableRegions()
	{
		List<String> availableRegions = new ArrayList<String>(Regions.values().length);
		for (Regions region : Regions.values())
			availableRegions.add(region.getName());

		return availableRegions;
	}

	public static List<String> getStorageClasses()
	{
		List<String> storageClasses = new ArrayList<String>(StorageClass.values().length);
		for (StorageClass storageClass : StorageClass.values())
			storageClasses.add(storageClass.toString());

		return storageClasses;
	}

	public static List<String> getTiers()
	{
		List<String> tiers = new ArrayList<String>(Tier.values().length);
		for (Tier tier : Tier.values())
			tiers.add(tier.toString());

		return tiers;
	}

	public String getAccessKey()
	{
		return accessKey;
	}

	public void setAccessKey(String accessKey)
	{
		this.accessKey = accessKey;
	}

	public String getSecretKey()
	{
		return secretKey;
	}

	public void setSecretKey(String secretKey)
	{
		this.secretKey = secretKey;
	}

	public String getAuthAlias()
	{
		return authAlias;
	}

	public void setAuthAlias(String authAlias)
	{
		this.authAlias = authAlias;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public boolean isChunkedEncodingDisabled()
	{
		return chunkedEncodingDisabled;
	}

	public void setChunkedEncodingDisabled(boolean chunkedEncodingDisabled)
	{
		this.chunkedEncodingDisabled = chunkedEncodingDisabled;
	}

	public boolean isAccelerateModeEnabled()
	{
		return accelerateModeEnabled;
	}

	public void setAccelerateModeEnabled(boolean accelerateModeEnabled)
	{
		this.accelerateModeEnabled = accelerateModeEnabled;
	}

	public boolean isForceGlobalBucketAccessEnabled()
	{
		return forceGlobalBucketAccessEnabled;
	}

	public void setForceGlobalBucketAccessEnabled(boolean forceGlobalBucketAccessEnabled)
	{
		this.forceGlobalBucketAccessEnabled = forceGlobalBucketAccessEnabled;
	}

	public boolean isBucketCreationEnabled()
	{
		return bucketCreationEnabled;
	}

	public void setBucketCreationEnabled(boolean bucketCreationEnabled)
	{
		this.bucketCreationEnabled = bucketCreationEnabled;
	}

	public boolean isStorageClassEnabled()
	{
		return storageClassEnabled;
	}

	public void setStorageClassEnabled(boolean storageClassEnabled)
	{
		this.storageClassEnabled = storageClassEnabled;
	}

	public String getActions()
	{
		return actions;
	}

	public void setActions(String actions)
	{
		this.actions = actions;
	}

	public String getClientRegion()
	{
		return clientRegion;
	}

	public void setClientRegion(String clientRegion)
	{
		this.clientRegion = clientRegion;
	}

	public String getBucketName()
	{
		return bucketName;
	}

	public void setBucketName(String bucketName)
	{
		this.bucketName = bucketName;
	}

	public String getBucketRegion()
	{
		return bucketRegion;
	}

	public void setBucketRegion(String bucketRegion)
	{
		this.bucketRegion = bucketRegion;
	}

	public String getDestinationBucketName()
	{
		return destinationBucketName;
	}

	public void setDestinationBucketName(String destinationBucketName)
	{
		this.destinationBucketName = destinationBucketName;
	}

	public String getStorageClass()
	{
		return storageClass;
	}

	public void setStorageClass(String storageClass)
	{
		this.storageClass = storageClass;
	}

	public String getTier()
	{
		return tier;
	}

	public void setTier(String tier)
	{
		this.tier = tier;
	}

	public int getExperationInDays()
	{
		return experationInDays;
	}

	public void setExperationInDays(int experationInDays)
	{
		this.experationInDays = experationInDays;
	}

	public String getStoreResultInSessionKey()
	{
		return storeResultInSessionKey;
	}

	public void setStoreResultInSessionKey(String storeResultInSessionKey)
	{
		this.storeResultInSessionKey = storeResultInSessionKey;
	}

} 