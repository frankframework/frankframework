package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.senders.Samba2Sender;

/**
 *  This test class is created to test both SambaFileSystem and SambaFileSystemSender classes.
 * @author alisihab
 *
 */

public class Samba2FileSystemSenderTest extends FileSystemSenderTest<Samba2Sender, String, Samba2FileSystem> {
	private String shareName = "share";
	private String username = "carlo";
	private String password = "carlo";
	private String domain = "172.17.0.2";


	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new Samba2FileSystemTestHelper(shareName,username,password,domain);
	}

	@Override
	public Samba2Sender createFileSystemSender() {
		Samba2Sender result = new Samba2Sender();
		result.setShare(shareName);
		result.setUsername(username);
		result.setPassword(password);
		result.setDomain(domain);
		
		return result;
	}
	
	public void writeToNDM(String textToWrite) throws MalformedURLException, SmbException, UnknownHostException, IOException {
	    
		NtlmPasswordAuthentication authentication = new NtlmPasswordAuthentication(domain, username, password);
		String url = "sdm://localhost:139/share";
	    try (OutputStream out = new SmbFileOutputStream(new SmbFile(url, authentication))) {
	        byte[] bytesToWrite = textToWrite.getBytes();
	        if (out != null && bytesToWrite != null && bytesToWrite.length > 0) {
	            out.write(bytesToWrite);
	        }
	    }
	    ;
	}

	@Test
	public void sambaSenderTest() throws SenderException, ConfigurationException, TimeOutException, IOException, FileSystemException {

		writeToNDM("textToWrite");
		
		fileSystemSender.setAction("list");
		fileSystemSender.configure();
		fileSystemSender.getFileSystem().open();
		fileSystemSender.getFileSystem().exists("text.txt");
		
		//String result = fileSystemSender.sendMessage(new Message(bucketNameTobeCreatedAndDeleted), null).asString();
	}


}
