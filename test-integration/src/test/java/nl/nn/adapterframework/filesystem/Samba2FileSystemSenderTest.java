package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import jcifs.smb.SmbFile;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.senders.AmazonS3Sender;
import nl.nn.adapterframework.senders.Samba2Sender;
import nl.nn.adapterframework.stream.Message;

/**
 *  This test class is created to test both SambaFileSystem and SambaFileSystemSender classes.
 * @author alisihab
 *
 */

public class Samba2FileSystemSenderTest extends FileSystemSenderTest<Samba2Sender, String, Samba2FileSystem> {
	private String shareName = "share";
	private String username = "carol";
	private String password = "carol1";
	private String domain = "SAMBAALPINE";


	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new SambaFileSystemTestHelper(shareName,username,password,domain);
	}

	@Override
	public Samba2Sender createFileSystemSender() {
		Samba2Sender result = new Samba2Sender();
		result.setShare(shareName);
		result.setUsername(username);
		result.setPassword(password);
		result.setDomain("172.17.0.2");
		return result;
	}
	

	@Test
	public void sambaSenderTest() throws SenderException, ConfigurationException, TimeOutException, IOException, FileSystemException {
		fileSystemSender.setAction("list");
		fileSystemSender.configure();
		fileSystemSender.getFileSystem().open();
		fileSystemSender.getFileSystem().exists("text.txt");
		
		//String result = fileSystemSender.sendMessage(new Message(bucketNameTobeCreatedAndDeleted), null).asString();
	}


}
