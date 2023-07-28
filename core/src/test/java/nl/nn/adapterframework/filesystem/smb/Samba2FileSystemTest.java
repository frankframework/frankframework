package nl.nn.adapterframework.filesystem.smb;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.EnumSet;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.filesys.debug.ConsoleDebug;
import org.filesys.debug.DebugConfigSection;
import org.filesys.server.auth.EnterpriseSMBAuthenticator;
import org.filesys.server.auth.ISMBAuthenticator.AuthMode;
import org.filesys.server.auth.UserAccount;
import org.filesys.server.auth.UserAccountList;
import org.filesys.server.config.CoreServerConfigSection;
import org.filesys.server.config.GlobalConfigSection;
import org.filesys.server.config.LicenceConfigSection;
import org.filesys.server.config.SecurityConfigSection;
import org.filesys.server.config.ServerConfiguration;
import org.filesys.server.filesys.DiskDeviceContext;
import org.filesys.server.filesys.DiskSharedDevice;
import org.filesys.server.filesys.FilesystemsConfigSection;
import org.filesys.smb.Dialect;
import org.filesys.smb.DialectSelector;
import org.filesys.smb.server.SMBConfigSection;
import org.filesys.smb.server.SMBServer;
import org.filesys.smb.server.SMBSrvSession.Dbg;
import org.filesys.smb.server.disk.original.JavaFileDiskDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.extensions.config.ConfigElement;

import nl.nn.adapterframework.filesystem.FileSystemTest;
import nl.nn.adapterframework.filesystem.IFileSystemTestHelper;
import nl.nn.adapterframework.filesystem.Samba2FileSystem;
import nl.nn.adapterframework.filesystem.Samba2FileSystem.Samba2AuthType;
import nl.nn.adapterframework.util.ClassUtils;

public class Samba2FileSystemTest extends FileSystemTest<SmbFileRef, Samba2FileSystem> {

	private String username = "frankframework";
	private String password = "pass_123";
	private String host = "localhost";
	private int port = 4450;

	private String shareName = "dummyDomain.nl";
	private String kdc = "localhost";
	private String realm = "DUMMYDOMAIN.NL";
	private String domain = "dummyDomain.nl";

	// Default memory pool settings
	private static final int[] DefaultMemoryPoolBufSizes = { 256, 4096, 16384, 65536 };
	private static final int[] DefaultMemoryPoolInitAlloc = { 20, 20, 5, 5 };
	private static final int[] DefaultMemoryPoolMaxAlloc = { 100, 50, 50, 50 };

	// Default thread pool size
	private static final int DefaultThreadPoolInit = 25;
	private static final int DefaultThreadPoolMax = 50;

	private static SMBServer smbServer;
	private static final boolean DEBUG = false;

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
//		return new LocalFileSystemTestHelper(getTestDirectoryFS());
		return new Samba2FileSystemTestHelper(host, port, shareName, username, password, domain);
	}

	/**
	 * Creates the folder '../target/sftpTestFS' in which the tests will be executed.
	 * This 'virtual FS' will pretend that the mentioned folder is the SFTP HOME directory.
	 */
	private static Path getTestDirectoryFS() {
		try {
			File targetFolder = new File(".", "target");
			File sftpTestFS = new File(targetFolder.getCanonicalPath(), "smb2TestFS");
			sftpTestFS.mkdir();
			assertTrue(sftpTestFS.exists());

			return sftpTestFS.toPath();
		} catch (Exception e) {
			throw new IllegalStateException("invalid path");
		}
	}

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		if("localhost".equals(host) && smbServer == null) {
			String license = getLicense();
			assumeTrue(license != null && ClassUtils.isClassPresent("org.filesys.smb.server.EnterpriseSMBServer")); //Only run test when a license is present!
			setWaitMillis(1000);

			ServerConfiguration serverConfig = new ServerConfiguration("dummySmbServer");
			FilesystemsConfigSection fsConfig = new FilesystemsConfigSection(serverConfig);

			JavaFileDiskDriver diskDriver = new JavaFileDiskDriver();
			DiskDeviceContext ctx = new DiskDeviceContext(getTestDirectoryFS().toAbsolutePath().toString(), shareName);
			DiskSharedDevice dsd = new DiskSharedDevice(shareName, diskDriver, ctx);
			fsConfig.addShare(dsd);

			SecurityConfigSection secConfig = new SecurityConfigSection(serverConfig);
			UserAccountList ual = new UserAccountList();
			ual.addUser(new UserAccount(username, password));
			secConfig.setUsersInterface("org.filesys.server.auth.DefaultUsersInterface", null);
			secConfig.setUserAccounts(ual);
			secConfig.setJCEProvider(BouncyCastleProvider.class.getCanonicalName());

			DebugConfigSection ds = new DebugConfigSection(serverConfig);
			ds.setDebug(ConsoleDebug.class.getCanonicalName(), mock(ConfigElement.class));

			GlobalConfigSection gcs = new GlobalConfigSection(serverConfig);
			gcs.setTimeZoneOffset(0);
			CoreServerConfigSection coreConfig = new CoreServerConfigSection(serverConfig);
			coreConfig.setMemoryPool(DefaultMemoryPoolBufSizes, DefaultMemoryPoolInitAlloc, DefaultMemoryPoolMaxAlloc);
			coreConfig.setThreadPool( DefaultThreadPoolInit, DefaultThreadPoolMax);

			SMBConfigSection cifsConfig = new SMBConfigSection(serverConfig);
			cifsConfig.setHostAnnouncer(false);
			cifsConfig.setWin32HostAnnouncer(false);
			cifsConfig.setNetBIOSSMB(false);
			cifsConfig.setWin32NetBIOS(false);

			DialectSelector dialects = new DialectSelector();
			dialects.AddDialect(Dialect.SMB2_202);
			dialects.AddDialect(Dialect.SMB2_210);
			dialects.AddDialect(Dialect.SMB3_311);
			dialects.AddDialect(Dialect.SMB3_302);
			dialects.AddDialect(Dialect.SMB3_311);
			cifsConfig.setEnabledDialects(dialects);

			ConfigElement configParams = mock(ConfigElement.class);
			when(configParams.getChild("useSPNEGO")).thenReturn(mock(ConfigElement.class)); //Assertions are done on the presence of the element, not the contents

			EnterpriseSMBAuthenticator auth = new EnterpriseSMBAuthenticator();
			auth.setAccessMode(AuthMode.SHARE);
			if(DEBUG) auth.setDebug(true);
			cifsConfig.setAuthenticator(auth);

			cifsConfig.setTcpipSMB(true);
			cifsConfig.setSMBBindAddress(InetAddress.getByName("localhost"));
			cifsConfig.setTcpipSMBPort(port);
			cifsConfig.setServerName("localhost");
			cifsConfig.setDomainName(domain);
			cifsConfig.setSocketTimeout(0);
			cifsConfig.setSessionDebugFlags(DEBUG ? EnumSet.allOf(Dbg.class) : EnumSet.noneOf(Dbg.class));
			auth.initialize(serverConfig, configParams);

			LicenceConfigSection licenseSection = new LicenceConfigSection(serverConfig);
			licenseSection.setLicenceKey(license);

			Class<?> clazz = ClassUtils.loadClass("org.filesys.smb.server.EnterpriseSMBServer");
			Constructor<?> con = clazz.getConstructor(serverConfig.getClass());
			smbServer = (SMBServer) con.newInstance(serverConfig);
			smbServer.startServer();
		}

		super.setUp();
	}

	private String getLicense() {
		String license = System.getenv("samba2licensekey");
		return (StringUtils.isBlank(license)) ? null : license;
	}

	@AfterAll
	public static void removeSmbServer() throws Exception {
		if(smbServer != null) {
			smbServer.shutdownServer(true);
			smbServer = null;
		}
	}

	@Override
	public Samba2FileSystem createFileSystem() {
		Samba2FileSystem result = new Samba2FileSystem();
		result.setShare(shareName.toUpperCase());
		result.setUsername(username);
		result.setPassword(password);
		if("localhost".equals(host)) { // test stub only works with NTLM
			result.setAuthType(Samba2AuthType.NTLM);
		}
		result.setHostname(host);
		result.setPort(port);
		result.setKdc(kdc);
		result.setRealm(realm);
		result.setDomainName(domain);
		return result;
	}

	@Test
	@Override
	public void basicFileSystemTestCopyFile() throws Exception {
		assumeFalse("localhost".equals(host)); //Returns 'STATUS_NOT_SUPPORTED (0xc00000bb): IOCTL failed' in combination with JFileServer
		super.basicFileSystemTestCopyFile();
	}

	@Test
	@Override
	public void writableFileSystemTestCopyFileToNonExistentDirectoryCreateFolderTrue() throws Exception {
		assumeFalse("localhost".equals(host)); //Returns 'STATUS_NOT_SUPPORTED (0xc00000bb): IOCTL failed' in combination with JFileServer
		super.writableFileSystemTestCopyFileToNonExistentDirectoryCreateFolderTrue();
	}
}
