package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.model.S3Object;

import nl.nn.adapterframework.testutil.PropertyUtil;

public class AmazonS3FileSystemTest extends FileSystemTest<S3Object, AmazonS3FileSystem> {
	protected String PROPERTY_FILE = "AmazonS3.properties";

	private boolean chunkedEncodingDisabled = false;
	private boolean accelerateModeEnabled = false; // this may involve some extra costs
	private boolean forceGlobalBucketAccessEnabled = false;

	private Regions clientRegion = Regions.EU_WEST_1;


	protected String accessKey    = PropertyUtil.getProperty(PROPERTY_FILE, "accessKey");
	protected String secretKey    = PropertyUtil.getProperty(PROPERTY_FILE, "secretKey");
	protected String bucketName    = PropertyUtil.getProperty(PROPERTY_FILE, "bucketName");

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
		super.writableFileSystemTestAppendExistingFile();
	}

	@Ignore
	@Test
	@Override
	public void writableFileSystemTestAppendNewFile() throws Exception {
		super.writableFileSystemTestAppendNewFile();
	}

	@Ignore
	@Test
	@Override
	public void writableFileSystemTestDeleteAppendedFile() throws Exception{
		super.writableFileSystemTestDeleteAppendedFile();
	}

	@Ignore // atomic move is not implemented. It could be possible to arrange this using ObjectLock.LegalHold
	@Test
	@Override
	public void basicFileSystemTestMoveFileMustFailWhenTargetAlreadyExists() throws Exception {
		super.basicFileSystemTestMoveFileMustFailWhenTargetAlreadyExists();
	}

	@Test
	public void testToFileWithBucketnameInFilename() throws FileSystemException {
		// arrange
		String filename="fakeFile";
		String bucketname="fakeBucket";
		
		String combinedFilename = bucketname +"|" + filename;
		
		// act
		S3Object ref = fileSystem.toFile(combinedFilename);
		
		// assert
		assertEquals(bucketname, ref.getBucketName());
		assertEquals(filename, ref.getKey());
	}

	@Test
	public void testToFileWithBucketnameInFilenameWithFolder() throws FileSystemException {
		// arrange
		String foldername="fakeFolder";
		String filename="fakeFile";
		String bucketname="fakeBucket";
		
		String combinedFilename = bucketname +"|" + foldername +"/"+ filename;
		
		// act
		S3Object ref = fileSystem.toFile(combinedFilename);
		
		// assert
		assertEquals(bucketname, ref.getBucketName());
		assertEquals(foldername +"/"+ filename, ref.getKey());
	}
}
