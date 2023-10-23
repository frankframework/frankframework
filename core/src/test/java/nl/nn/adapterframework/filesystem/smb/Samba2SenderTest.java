package nl.nn.adapterframework.filesystem.smb;

import org.junit.jupiter.api.BeforeEach;

import nl.nn.adapterframework.filesystem.FileSystemSenderTest;
import nl.nn.adapterframework.filesystem.IFileSystemTestHelper;
import nl.nn.adapterframework.filesystem.LocalFileServer;
import nl.nn.adapterframework.filesystem.LocalFileSystemTestHelper;
import nl.nn.adapterframework.filesystem.Samba2FileSystem;
import nl.nn.adapterframework.filesystem.LocalFileServer.FileSystemType;
import nl.nn.adapterframework.filesystem.Samba2FileSystem.Samba2AuthType;
import nl.nn.adapterframework.senders.Samba2Sender;
import nl.nn.adapterframework.testutil.junit.LocalFileSystemMock;

public class Samba2SenderTest extends FileSystemSenderTest<Samba2Sender, SmbFileRef, Samba2FileSystem> {

	private String username = "frankframework";
	private String password = "pass_123";
	private String host = "localhost";
	private int port = 445;

	private String shareName = "home";
	private String kdc = "localhost";
	private String realm = "DUMMYDOMAIN.NL";
	private String domain = "dummyDomain.nl";

	@LocalFileSystemMock
	private static LocalFileServer fs;

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		if("localhost".equals(host)) {
			return new LocalFileSystemTestHelper(fs.getTestDirectory());
		}
		return new Samba2FileSystemTestHelper(host, port, shareName, username, password, domain);
	}

	@BeforeEach
	@Override
	public void setUp() throws Exception {
		if("localhost".equals(host)) {
			fs.startServer(FileSystemType.SMB2);
			port = fs.getPort();
		}
		super.setUp();
	}

	@Override
	public Samba2Sender createFileSystemSender() {
		Samba2Sender sender = new Samba2Sender();
		sender.setShare(shareName);
		sender.setUsername(username);
		sender.setPassword(password);
		if("localhost".equals(host)) { // test stub only works with NTLM
			sender.setAuthType(Samba2AuthType.NTLM);
		}
		sender.setHostname(host);
		sender.setPort(port);
		sender.setKdc(kdc);
		sender.setRealm(realm);
		sender.setDomainName(domain);
		return sender;
	}
}
