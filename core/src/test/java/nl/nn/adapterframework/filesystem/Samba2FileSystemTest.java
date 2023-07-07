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
			cifsConfig.setEnabledDialects(dialects);
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

	@Test
	public void testSmbFileRefSetRelative() {
		assertEquals("test123", new SmbFileRef("test123").getName());
		assertEquals("folder\\test123", new SmbFileRef("folder/test123").getName());
	}

	@Test
	public void testSmbFileRefSetFolder() {
		SmbFileRef ref1 = new SmbFileRef("test123");
		ref1.setFolder("folder");
		assertEquals("folder\\test123", ref1.getName());
	}

	@Test
	public void testSmbFileRefRelativeWithSetFolder() {
		SmbFileRef ref2 = new SmbFileRef("folder1/test123");
		ref2.setFolder("folder2");
		assertEquals("folder2\\test123", ref2.getName());
	}

	@Test
	public void testSmbFileRefWindowsSlash() {
		SmbFileRef ref2 = new SmbFileRef("folder1\\test123");
		ref2.setFolder("folder2");
		assertEquals("folder2\\test123", ref2.getName());
	}
}
