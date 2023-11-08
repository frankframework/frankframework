package nl.nn.adapterframework.filesystem.smb;

import org.junit.jupiter.api.BeforeEach;

import nl.nn.adapterframework.filesystem.FileSystemPipeTest;
import nl.nn.adapterframework.filesystem.IFileSystemTestHelper;
import nl.nn.adapterframework.filesystem.LocalFileSystemTestHelper;
import nl.nn.adapterframework.filesystem.Samba2FileSystem;
import nl.nn.adapterframework.filesystem.Samba2FileSystem.Samba2AuthType;
import nl.nn.adapterframework.pipes.Samba2Pipe;
import nl.nn.adapterframework.testutil.junit.LocalFileServer;
import nl.nn.adapterframework.testutil.junit.LocalFileSystemMock;
import nl.nn.adapterframework.testutil.junit.LocalFileServer.FileSystemType;

public class Samba2PipeTest extends FileSystemPipeTest<Samba2Pipe, SmbFileRef, Samba2FileSystem> {

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

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		if("localhost".equals(host)) {
			fs.startServer(FileSystemType.SMB2);
			port = fs.getPort();
		}
		super.setUp();
	}

	@Override
	public Samba2Pipe createFileSystemPipe() {
		Samba2Pipe pipe = new Samba2Pipe();
		pipe.setShare(shareName);
		pipe.setUsername(username);
		pipe.setPassword(password);
		if("localhost".equals(host)) { // test stub only works with NTLM
			pipe.setAuthType(Samba2AuthType.NTLM);
		}
		pipe.setHostname(host);
		pipe.setPort(port);
		pipe.setKdc(kdc);
		pipe.setRealm(realm);
		pipe.setDomainName(domain);
		return pipe;
	}
}
