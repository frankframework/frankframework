package nl.nn.adapterframework.filesystem;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.senders.Samba2Sender;
import nl.nn.adapterframework.stream.Message;

/**
 *  This test class is created to test both SambaFileSystem and SambaFileSystemSender classes.
 * @author alisihab
 *
 */

public class Samba2FileSystemSenderTest extends FileSystemSenderTest<Samba2Sender, String, Samba2FileSystem> {

	private String shareName = "share";
	private String username = "wearefrank";
	private String password = "pass_123";
	private String host = "localhost";
	private Integer port = 139;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new Samba2FileSystemTestHelper(shareName,username,password,host,port);
	}

	@Override
	public Samba2Sender createFileSystemSender() {
		Samba2Sender result = new Samba2Sender();
		result.setShare(shareName);
		result.setUsername(username);
		result.setPassword(password);
		result.setDomain(host);
		result.setPort(port);
		return result;
	}

	@Test
	public void createFile() throws SenderException, ConfigurationException, TimeOutException, IOException, FileSystemException {
		fileSystemSender.setAction("upload");
		fileSystemSender.setFilename("text6.txt");
		fileSystemSender.configure();
		fileSystemSender.getFileSystem().open();
		fileSystemSender.sendMessage(new Message("text6.txt"), null);
		System.out.println("exist file text5.txt = " + fileSystemSender.getFileSystem().exists("text5.txt"));
		System.out.println("exist folder folder1 = " + fileSystemSender.getFileSystem().exists("folder1"));
		System.out.println("exist folder folder2 = " + fileSystemSender.getFileSystem().exists("folder2"));
	}


}
