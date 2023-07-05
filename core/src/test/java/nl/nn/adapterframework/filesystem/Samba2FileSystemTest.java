package nl.nn.adapterframework.filesystem;

public class Samba2FileSystemTest extends FileSystemTest<String, Samba2FileSystem> {

	private String shareName = "share";
	private String userName = "wearefrank";
	private String password = "pass_123";
	private String host = "localhost";
	private Integer port = 139;
	private String kdc = "localhost";
	private String realm = "WEAREFRANK.NL";
	
	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new Samba2FileSystemTestHelper(host, port, shareName, userName, password, domain);
	}


	@Override
	public Samba2FileSystem createFileSystem() {
		Samba2FileSystem result = new Samba2FileSystem();
		result.setShare(shareName);
		result.setUsername(userName);
		result.setPassword(password);
		result.setHostname(host);
		result.setPort(port);
		result.setKdc(kdc);
		result.setRealm(realm);
		return result;
	}


}
