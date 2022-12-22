package nl.nn.adapterframework.filesystem;

import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.model.S3Object;

public class AmazonS3FileSystemTest extends FileSystemTest<S3Object, AmazonS3FileSystem> {
	private String accessKey = "";
	private String secretKey = "";

	private boolean chunkedEncodingDisabled = false;
	private boolean accelerateModeEnabled = false; // this may involve some extra costs
	private boolean forceGlobalBucketAccessEnabled = false;

	private String bucketName = UUID.randomUUID().toString();//"iaf.s3sender.ali.test";
	private Regions clientRegion = Regions.EU_WEST_1;
	
	
	private int waitMilis = 1000;

	{
		setWaitMillis(waitMilis);
	}
	
	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new AmazonS3FileSystemTestHelper(accessKey, secretKey, chunkedEncodingDisabled, accelerateModeEnabled, forceGlobalBucketAccessEnabled, bucketName, clientRegion);
	}
	
	@Override
	public AmazonS3FileSystem createFileSystem(){
		AmazonS3FileSystem s3 = new AmazonS3FileSystem();
		s3.setAccessKey(accessKey);
		s3.setSecretKey(secretKey);
		s3.setBucketName(bucketName);
		return s3;
	}

	@Ignore
	@Test
	@Override
	public void writableFileSystemTestAppendExistingFile() throws Exception {
		// TODO Auto-generated method stub
		super.writableFileSystemTestAppendExistingFile();
	}
	
	@Ignore
	@Test
	@Override
	public void writableFileSystemTestAppendNewFile() throws Exception {
		// TODO Auto-generated method stub
		super.writableFileSystemTestAppendNewFile();
	}
}
