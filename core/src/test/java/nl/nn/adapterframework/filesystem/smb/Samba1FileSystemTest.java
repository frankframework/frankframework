package nl.nn.adapterframework.filesystem.smb;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.EnumSet;

import org.filesys.debug.ConsoleDebug;
import org.filesys.debug.DebugConfigSection;
import org.filesys.server.SrvSession;
import org.filesys.server.auth.ClientInfo;
import org.filesys.server.auth.ISMBAuthenticator.AuthMode;
import org.filesys.server.auth.LocalAuthenticator;
import org.filesys.server.auth.UserAccount;
import org.filesys.server.auth.UserAccountList;
import org.filesys.server.config.CoreServerConfigSection;
import org.filesys.server.config.GlobalConfigSection;
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
import org.springframework.extensions.config.ConfigElement;

import com.hierynomus.security.jce.messagedigest.MD4;

import jcifs.smb.SmbFile;
import nl.nn.adapterframework.filesystem.FileSystemTest;
import nl.nn.adapterframework.filesystem.IFileSystemTestHelper;
import nl.nn.adapterframework.filesystem.LocalFileSystemTestHelper;
import nl.nn.adapterframework.filesystem.Samba1FileSystem;

public class Samba1FileSystemTest extends FileSystemTest<SmbFile, Samba1FileSystem> {

	private String username = "wearefrank";
	private String password = "pass_123";
	private String host = "localhost";
	private int port = 4450;

	private String shareName = "test";
	private String domain = "DUMMYDOMAIN.NL";

	// Default memory pool settings
	private static final int[] DefaultMemoryPoolBufSizes = { 256, 4096, 16384, 65536 };
	private static final int[] DefaultMemoryPoolInitAlloc = { 20, 20, 5, 5 };
	private static final int[] DefaultMemoryPoolMaxAlloc = { 100, 50, 50, 50 };

	// Default thread pool size
	private static final int DefaultThreadPoolInit = 25;
	private static final int DefaultThreadPoolMax = 50;

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new LocalFileSystemTestHelper(getTestDirectoryFS());
	}

	/**
	 * Creates the folder '../target/sftpTestFS' in which the tests will be executed.
	 * This 'virtual FS' will pretend that the mentioned folder is the SFTP HOME directory.
	 */
	private static Path getTestDirectoryFS() {
		try {
			File targetFolder = new File(".", "target");
			File sftpTestFS = new File(targetFolder.getCanonicalPath(), "smb1TestFS");
			sftpTestFS.mkdir();
			assertTrue(sftpTestFS.exists());

			return sftpTestFS.toPath();
		} catch (Exception e) {
			throw new IllegalStateException("invalid path");
		}
	}

	private static SMBServer smbServer;

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		if("localhost".equals(host) && smbServer == null) {
			setWaitMillis(1000);
			ServerConfiguration serverConfig = new ServerConfiguration("dummySmbServer");
			FilesystemsConfigSection fsConfig = new FilesystemsConfigSection(serverConfig);

			JavaFileDiskDriver diskDriver = new JavaFileDiskDriver();
			DiskDeviceContext ctx = new DiskDeviceContext(getTestDirectoryFS().toAbsolutePath().toString(), shareName);
			DiskSharedDevice dsd = new DiskSharedDevice(shareName, diskDriver, ctx);
			fsConfig.addShare(dsd);
			serverConfig.addConfigSection(fsConfig);

			SecurityConfigSection secConfig = new SecurityConfigSection(serverConfig);
			UserAccountList ual = new UserAccountList();
			UserAccount ua = new UserAccount(username, password);
			MD4 md4 = new MD4();
			ua.setMD4Password(md4.digest(password.getBytes()));
			ual.addUser(ua);
			secConfig.setUsersInterface("org.filesys.server.auth.DefaultUsersInterface", null);
			secConfig.setUserAccounts(ual);
			serverConfig.addConfigSection(secConfig);

			DebugConfigSection ds = new DebugConfigSection(serverConfig);
			ds.setDebug(ConsoleDebug.class.getCanonicalName(), mock(ConfigElement.class));
			serverConfig.addConfigSection(ds);
			GlobalConfigSection gcs = new GlobalConfigSection(serverConfig);
			gcs.setTimeZoneOffset(0);
			serverConfig.addConfigSection(gcs);
			CoreServerConfigSection coreConfig = new CoreServerConfigSection(serverConfig);
			coreConfig.setMemoryPool(DefaultMemoryPoolBufSizes, DefaultMemoryPoolInitAlloc, DefaultMemoryPoolMaxAlloc);
			coreConfig.setThreadPool( DefaultThreadPoolInit, DefaultThreadPoolMax);
			serverConfig.addConfigSection(coreConfig);

//			https://www.javatips.net/api/OurVirt-master/src/org/ourgrid/virt/strategies/qemu/EmbeddedCifsServer.java
			SMBConfigSection cifsConfig = new SMBConfigSection(serverConfig);
			cifsConfig.setHostAnnouncer(false);
			cifsConfig.setWin32HostAnnouncer(false);
			cifsConfig.setNetBIOSSMB(false);
			cifsConfig.setWin32NetBIOS(false);

			DialectSelector dialects = new DialectSelector();
			dialects.AddDialect(Dialect.NT);
			cifsConfig.setEnabledDialects(dialects);
			cifsConfig.setSocketKeepAlive(true);

			ConfigElement configParams = mock(ConfigElement.class);
			AllAuthenticatedAuthenticator auth = new AllAuthenticatedAuthenticator();
			auth.setAllowGuest(true);
			auth.setAccessMode(AuthMode.USER);
			cifsConfig.setAuthenticator(auth);
			cifsConfig.setTcpipSMB(true);
			cifsConfig.setSMBBindAddress(InetAddress.getByName("localhost"));
			cifsConfig.setTcpipSMBPort(port);
			cifsConfig.setServerName("localhost");
			cifsConfig.setDomainName(domain);
			cifsConfig.setSocketTimeout(0);
			cifsConfig.setSessionDebugFlags(EnumSet.allOf(Dbg.class));
			serverConfig.addConfigSection(cifsConfig);
			auth.initialize(serverConfig, configParams);

			smbServer = new SMBServer(serverConfig);
			smbServer.startServer();
		}

		super.setUp();
	}

	public static class AllAuthenticatedAuthenticator extends LocalAuthenticator {
		@Override
		public AuthStatus authenticateUser(ClientInfo client, SrvSession sess, PasswordAlgorithm alg) {
			return AuthStatus.AUTHENTICATED;
		}
	}

	@AfterAll
	public static void removeSmbServer() throws Exception {
		if(smbServer != null) {
			smbServer.shutdownServer(true);
			smbServer = null;
		}
	}

	@Override
	public Samba1FileSystem createFileSystem() {
		Samba1FileSystem result = new Samba1FileSystem();
		result.setShare("smb://localhost:"+port+"/"+shareName);
		result.setUsername(username);
		result.setPassword(password);
		result.setAuthenticationDomain(domain);
		return result;
	}
}
