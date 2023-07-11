package nl.nn.adapterframework.filesystem.samba;

import org.junit.jupiter.api.Disabled;

import nl.nn.adapterframework.filesystem.FileSystemTest;
import nl.nn.adapterframework.filesystem.IFileSystemTestHelper;
import nl.nn.adapterframework.filesystem.Samba2FileSystem;
import nl.nn.adapterframework.filesystem.Samba2FileSystem.Samba2AuthType;
import nl.nn.adapterframework.filesystem.smb.SmbFileRef;

@Disabled
public class Samba2FileSystemTest extends FileSystemTest<SmbFileRef, Samba2FileSystem> {

	private String username = "wearefrank";
	private String password = "pass_123";
	private String host = "localhost";
	private int port = 4450;

	private String shareName = "dummyDomain.nl";
	private String kdc = "localhost";
	private String realm = "DUMMYDOMAIN.NL";
	private String domain = "dummyDomain.nl";

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new Samba2FileSystemTestHelper(host, port, shareName, username, password, domain);
	}

	@Override
	public Samba2FileSystem createFileSystem() {
		Samba2FileSystem result = new Samba2FileSystem();
		result.setShare(shareName);
		result.setUsername(username);
		result.setPassword(password);
		result.setAuthType(Samba2AuthType.NTLM);
		result.setHostname(host);
		result.setPort(port);
		result.setKdc(kdc);
		result.setRealm(realm);
		result.setDomainName(domain);
		return result;
	}
}
