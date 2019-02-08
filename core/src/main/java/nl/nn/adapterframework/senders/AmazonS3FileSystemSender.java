package nl.nn.adapterframework.senders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.internal.BucketNameUtils;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.StorageClass;
import com.amazonaws.services.s3.model.Tier;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.filesystem.AmazonS3FileSystem;
import nl.nn.adapterframework.filesystem.FileSystemSender;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.CredentialFactory;

public class AmazonS3FileSystemSender extends FileSystemSender<S3Object, AmazonS3FileSystem> {

	private static final List<String> AVAILABLE_REGIONS = getAvailableRegions();
	private static final List<String> STORAGE_CLASSES = getStorageClasses();
	private static final List<String> TIERS = getTiers();

	private List<String> availableActions = Arrays.asList("createBucket", "deleteBucket", "upload",
			"download", "copy", "delete", "restore", "mkdir", "rmdir", "rename", "list");

	private String accessKey;
	private String secretKey;
	private String authAlias;

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

	private String action;
	private String storageClass;
	private String tier = Tier.Standard.toString();
	private int experationInDays = -1;

	private String storeResultInSessionKey;
	private boolean bucketExistsThrowException = true;

	private AmazonS3FileSystem fileSystem;

	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getAccessKey()) || StringUtils.isEmpty(getSecretKey()))
			throw new ConfigurationException(
					getLogPrefix() + " empty credential fields, please prodive aws credentials");

		if (StringUtils.isEmpty(getClientRegion())
				|| !AVAILABLE_REGIONS.contains(getClientRegion()))
			throw new ConfigurationException(getLogPrefix() + " invalid region ["
					+ getClientRegion() + "] please use one of the following supported regions "
					+ AVAILABLE_REGIONS.toString());

		if (StringUtils.isEmpty(getBucketName())
				|| !BucketNameUtils.isValidV2BucketName(getBucketName()))
			throw new ConfigurationException(getLogPrefix() + " invalid or empty bucketName ["
					+ getBucketName() + "] please visit AWS to see correct bucket naming");

		StringTokenizer tokenizer = new StringTokenizer(getActions(), " ,\t\n\r\f");
		while (tokenizer.hasMoreTokens()) {
			String action = tokenizer.nextToken();

			if (StringUtils.isEmpty(action) || !availableActions.contains(action))
				throw new ConfigurationException(getLogPrefix() + " invalid action [" + action
						+ "] please use one of the following supported actions "
						+ availableActions.toString());

			if (action.equalsIgnoreCase("createBucket") && isForceGlobalBucketAccessEnabled()
					&& (StringUtils.isEmpty(getBucketRegion())
							|| !AVAILABLE_REGIONS.contains(getBucketRegion())))
				throw new ConfigurationException(getLogPrefix() + " invalid bucketRegion ["
						+ getBucketRegion() + "] please use following supported regions "
						+ AVAILABLE_REGIONS.toString());

			if (action.equalsIgnoreCase("upload") || action.equalsIgnoreCase("copy")
					|| action.equalsIgnoreCase("rename")) {
				ParameterList parameterList = getParameterList();

				if (action.equalsIgnoreCase("upload")
						&& parameterList.findParameter("file") == null)
					throw new ConfigurationException(
							getLogPrefix() + " file parameter requires to be present to perform ["
									+ action + "] action");

				else if (action.equalsIgnoreCase("copy")) {
					if (StringUtils.isEmpty(getDestinationBucketName())
							|| !BucketNameUtils.isValidV2BucketName(getDestinationBucketName()))
						throw new ConfigurationException(
								getLogPrefix() + " invalid or empty destinationBucketName ["
										+ getDestinationBucketName()
										+ "] please visit AWS to see correct bucket naming");
					if (parameterList.findParameter("destinationFileName") == null)
						throw new ConfigurationException(getLogPrefix()
								+ " destinationFileName parameter requires to be present to perform ["
								+ action + "] action");
					if (isStorageClassEnabled() && (StringUtils.isEmpty(getStorageClass())
							|| !STORAGE_CLASSES.contains(getStorageClass())))
						throw new ConfigurationException(
								getLogPrefix() + " invalid storage class [" + getStorageClass()
										+ "] please use following supported storage classes "
										+ STORAGE_CLASSES.toString());

				} else if (action.equalsIgnoreCase("rename")
						&& parameterList.findParameter("destination") == null) {
					throw new ConfigurationException(getLogPrefix()
							+ " destination parameter requires to be present to perform [" + action
							+ "] action");
				}
			}

			/*			if(action.equalsIgnoreCase("accelerateMode"))
							if(!isAccelerateModeEnabled())
								throw new ConfigurationException(getLogPrefix()+" when performing ["+action+"] action, accelerateModeEnabled attribute should be set to 'true'");
			*/
			if (action.equalsIgnoreCase("restore")
					&& (StringUtils.isEmpty(getTier()) || !TIERS.contains(getTier()))) {
				throw new ConfigurationException(getLogPrefix()
						+ " invalid tier when restoring an object from Amazon S3 Glacier, please use one of the following supported tiers: "
						+ TIERS.toString());
			}
		}
		CredentialFactory cf = new CredentialFactory(getAuthAlias(), getAccessKey(),
				getSecretKey());
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(cf.getUsername(), cf.getPassword());
		AmazonS3ClientBuilder s3ClientBuilder = AmazonS3ClientBuilder.standard()
				.withChunkedEncodingDisabled(isChunkedEncodingDisabled())
				.withAccelerateModeEnabled(isAccelerateModeEnabled())
				.withForceGlobalBucketAccessEnabled(isForceGlobalBucketAccessEnabled())
				.withRegion(getClientRegion())
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds));

		s3Client = s3ClientBuilder.build();
		super.setActionArray(actions.split(","));
		fileSystem = new AmazonS3FileSystem(s3Client, bucketName);
		fileSystem.setBucketRegion(bucketRegion);
		fileSystem.setLogPrefix(getLogPrefix());
		fileSystem.setStorageClassEnabled(storageClassEnabled);
		fileSystem.setForceGlobalBucketAccessEnabled(forceGlobalBucketAccessEnabled);
		fileSystem.setBucketCreationEnabled(bucketCreationEnabled);
		fileSystem.setBucketExistsThrowException(bucketExistsThrowException);
		fileSystem.setLog(log);
		fileSystem.setTier(tier);
		setFileSystem(fileSystem);
	}

	@Override
	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc)
			throws SenderException, TimeOutException {
		String result = super.sendMessage(correlationID, message, prc);
		String fileName = message;
		ParameterValueList pvl = null;
		if (prc != null && paramList != null) {
			try {
				pvl = prc.getValues(paramList);
			} catch (ParameterException e) {
				throw new SenderException(getLogPrefix() + "Sender [" + getName()
						+ "] caught exception evaluating parameters", e);
			}
		}
		String action = getActionArray()[getLastProcessedActionIndex()];
		if (action.equalsIgnoreCase("createBucket")) { //createBucket block
			result = fileSystem.createBucket(getBucketName(), bucketExistsThrowException);
		} else if (action.equalsIgnoreCase("deleteBucket")) //deleteBucket block
			result = fileSystem.deleteBucket(getBucketName());
		else if (action.equalsIgnoreCase("copy")) //copy file block
			if (pvl.getParameterValue("destinationFileName") != null)
				if (pvl.getParameterValue("destinationFileName").getValue() != null) {
					String destinationFileName = pvl.getParameterValue("destinationFileName")
							.getValue().toString();
					result = fileSystem.copyObject(getBucketName(), fileName,
							getDestinationBucketName(), destinationFileName);
				} else
					throw new SenderException(getLogPrefix()
							+ " no value in destinationFileName parameter found, please assing value to the parameter to perfom [copy] action");
			else
				throw new SenderException(getLogPrefix()
						+ " no destinationFileName parameter found, it must be used to perform [copy] action");
		else if (action.equalsIgnoreCase("restore"))
			result = fileSystem.restoreObject(getBucketName(), fileName, getExperationInDays());
		return result;
	}

	public static List<String> getAvailableRegions() {
		List<String> availableRegions = new ArrayList<String>(Regions.values().length);
		for (Regions region : Regions.values())
			availableRegions.add(region.getName());

		return availableRegions;
	}

	public static List<String> getStorageClasses() {
		List<String> storageClasses = new ArrayList<String>(StorageClass.values().length);
		for (StorageClass storageClass : StorageClass.values())
			storageClasses.add(storageClass.toString());

		return storageClasses;
	}

	public static List<String> getTiers() {
		List<String> tiers = new ArrayList<String>(Tier.values().length);
		for (Tier tier : Tier.values())
			tiers.add(tier.toString());

		return tiers;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action.toLowerCase();
		super.setAction(action);
	}

	public String getAccessKey() {
		return accessKey;
	}

	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getAuthAlias() {
		return authAlias;
	}

	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}

	public boolean isChunkedEncodingDisabled() {
		return chunkedEncodingDisabled;
	}

	public void setChunkedEncodingDisabled(boolean chunkedEncodingDisabled) {
		this.chunkedEncodingDisabled = chunkedEncodingDisabled;
	}

	public boolean isAccelerateModeEnabled() {
		return accelerateModeEnabled;
	}

	public void setAccelerateModeEnabled(boolean accelerateModeEnabled) {
		this.accelerateModeEnabled = accelerateModeEnabled;
	}

	public boolean isForceGlobalBucketAccessEnabled() {
		return forceGlobalBucketAccessEnabled;
	}

	public void setForceGlobalBucketAccessEnabled(boolean forceGlobalBucketAccessEnabled) {
		this.forceGlobalBucketAccessEnabled = forceGlobalBucketAccessEnabled;
	}

	public boolean isStorageClassEnabled() {
		return storageClassEnabled;
	}

	public void setStorageClassEnabled(boolean storageClassEnabled) {
		this.storageClassEnabled = storageClassEnabled;
	}

	public String getActions() {
		return actions;
	}

	public void setActions(String actions) {
		this.actions = actions;
	}

	public String getClientRegion() {
		return clientRegion;
	}

	public void setClientRegion(String clientRegion) {
		this.clientRegion = clientRegion;
	}

	public String getBucketName() {
		return bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	public String getBucketRegion() {
		return bucketRegion;
	}

	public void setBucketRegion(String bucketRegion) {
		this.bucketRegion = bucketRegion;
	}

	public String getDestinationBucketName() {
		return destinationBucketName;
	}

	public void setDestinationBucketName(String destinationBucketName) {
		this.destinationBucketName = destinationBucketName;
	}

	public String getStorageClass() {
		return storageClass;
	}

	public void setStorageClass(String storageClass) {
		this.storageClass = storageClass;
	}

	public String getTier() {
		return tier;
	}

	public void setTier(String tier) {
		this.tier = tier;
	}

	public int getExperationInDays() {
		return experationInDays;
	}

	public void setExperationInDays(int experationInDays) {
		this.experationInDays = experationInDays;
	}

	public String getStoreResultInSessionKey() {
		return storeResultInSessionKey;
	}

	public void setStoreResultInSessionKey(String storeResultInSessionKey) {
		this.storeResultInSessionKey = storeResultInSessionKey;
	}

}
