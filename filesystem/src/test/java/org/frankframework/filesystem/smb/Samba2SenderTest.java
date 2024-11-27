package org.frankframework.filesystem.smb;

import org.junit.jupiter.api.BeforeEach;

import org.frankframework.filesystem.FileSystemSenderTest;
import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.filesystem.LocalFileSystemTestHelper;
import org.frankframework.filesystem.smb.Samba2FileSystem.Samba2AuthType;
import org.frankframework.senders.Samba2Sender;
import org.frankframework.testutil.junit.LocalFileServer;
import org.frankframework.testutil.junit.LocalFileServer.FileSystemType;
import org.frankframework.testutil.junit.LocalFileSystemMock;

public class Samba2SenderTest extends FileSystemSenderTest<Samba2Sender, SmbFileRef, Samba2FileSystem> {

	private final String username = "frankframework";
	private final String password = "pass_123";
	private final String host = "localhost";
	private int port = 445;

	private final String shareName = "home";
	private final String kdc = "localhost";
	private final String realm = "DUMMYDOMAIN.NL";
	private final String domain = "dummyDomain.nl";

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
