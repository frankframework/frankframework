package org.frankframework.testutil.junit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.filesys.debug.ConsoleDebug;
import org.filesys.debug.DebugConfigSection;
import org.filesys.ftp.FTPConfigSection;
import org.filesys.ftp.FTPServer;
import org.filesys.ftp.FTPSrvSession;
import org.filesys.server.NetworkServer;
import org.filesys.server.auth.EnterpriseSMBAuthenticator;
import org.filesys.server.auth.ISMBAuthenticator.AuthMode;
import org.filesys.server.auth.UserAccount;
import org.filesys.server.auth.UserAccountList;
import org.filesys.server.config.CoreServerConfigSection;
import org.filesys.server.config.GlobalConfigSection;
import org.filesys.server.config.InvalidConfigurationException;
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
import org.filesys.smb.server.SMBSrvSession;
import org.filesys.smb.server.disk.original.JavaFileDiskDriver;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.springframework.extensions.config.ConfigElement;
import org.springframework.extensions.config.element.GenericConfigElement;

import org.frankframework.filesystem.smb.Samba2FileSystemTest;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StreamUtil;

/**
 * Will create a local FS with share 'home' @ 'localhost' : 'automatically-calculated-port'.
 * Username and password can be changed via the {@link LocalFileSystemMock @MockFileSystem} annotation.
 *
 * @author Niels Meijer
 */
public class LocalFileServer implements AutoCloseable, CloseableResource {
	private final Logger log = LogUtil.getLogger(this);

	// Default memory pool settings
	private static final int[] DefaultMemoryPoolBufSizes = { 256, 4096, 16384, 65536 };
	private static final int[] DefaultMemoryPoolInitAlloc = { 20, 20, 5, 5 };
	private static final int[] DefaultMemoryPoolMaxAlloc = { 100, 50, 50, 50 };

	// Default thread pool size
	private static final int DefaultThreadPoolInit = 25;
	private static final int DefaultThreadPoolMax = 50;

	private static final boolean DEBUG = false;

	private final String username;
	private final String password;
	private final InetAddress host = InetAddress.getByName("localhost");
	private final int port;
	private final String shareName = "home";
	private final String domain = "dummyDomain.org";
	private final String license;
	private final Path testDirectory; // path in which the tests are executed
	private final ServerConfiguration serverConfig;


	public enum FileSystemType {
		SMB1, SMB2, FTP
	}

	private NetworkServer server;

	public LocalFileServer(String serverName) throws IOException {
		this(serverName, "frankframework", "pass_123");
	}

	public LocalFileServer(String serverName, String username, String password) throws IOException {
		port = findUnusedPort();
		license = getLicense();
		serverConfig = new ServerConfiguration(serverName);
		final Path tempDirectory = findTempDirectory();
		testDirectory = Files.createTempDirectory(tempDirectory, "junit-" + serverConfig.getServerName());
		this.username = username;
		this.password = password;
	}

	private static Path findTempDirectory() {
		String directory = System.getProperty("java.io.tmpdir");

		if (StringUtils.isEmpty(directory)) {
			throw new IllegalStateException("unknown or invalid path [java.io.tmpdir]");
		}

		File file = new File(directory);
		if (!file.isAbsolute()) {
			String absPath = new File("").getAbsolutePath();
			if(absPath != null) {
				file = new File(absPath, directory);
			}
		}
		if(!file.exists()) {
			file.mkdirs();
		}
		String fileDir = file.getPath();
		if(StringUtils.isEmpty(fileDir) || !file.isDirectory()) {
			throw new IllegalStateException("unknown or invalid path ["+(StringUtils.isEmpty(fileDir)?"NULL":fileDir)+"]");
		}
		return file.toPath();
	}

	private void baseConfiguration() throws InvalidConfigurationException {
		FilesystemsConfigSection fsConfig = new FilesystemsConfigSection(serverConfig);

		JavaFileDiskDriver diskDriver = new JavaFileDiskDriver();
		DiskDeviceContext ctx = new DiskDeviceContext(testDirectory.toAbsolutePath().toString(), shareName);
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
	}

	private int findUnusedPort() throws IOException {
		try (ServerSocket s = new ServerSocket(0)) {
			return s.getLocalPort();
		}
	}

	private String getLicense() throws IOException {
		URL license = Samba2FileSystemTest.class.getResource("/jfileserver.lic");
		if(license != null && ClassUtils.isClassPresent("org.filesys.smb.server.EnterpriseSMBServer")) {
			return StreamUtil.streamToString(license.openStream());
		}

		return null;
	}

	private void setupSMBConfiguration(boolean isSmb2) throws InvalidConfigurationException {
		baseConfiguration();

		SMBConfigSection cifsConfig = new SMBConfigSection(serverConfig);
		cifsConfig.setHostAnnouncer(false);
		cifsConfig.setWin32HostAnnouncer(false);
		cifsConfig.setNetBIOSSMB(false);
		cifsConfig.setWin32NetBIOS(false);

		DialectSelector dialects = new DialectSelector();
		if(isSmb2) {
			dialects.AddDialect(Dialect.SMB2_202);
			dialects.AddDialect(Dialect.SMB2_210);
			dialects.AddDialect(Dialect.SMB3_300);
			dialects.AddDialect(Dialect.SMB3_302);
//			dialects.AddDialect(Dialect.SMB3_311); // This seems to be broken
		} else {
			dialects.AddDialect(Dialect.NT);
		}
		cifsConfig.setEnabledDialects(dialects);

		ConfigElement configParams = new GenericConfigElement("authenticator");
		configParams.addChild(new GenericConfigElement("useSPNEGO"));

		EnterpriseSMBAuthenticator auth = new EnterpriseSMBAuthenticator();
		auth.setAccessMode(AuthMode.SHARE);
		if(DEBUG) auth.setDebug(true);
		cifsConfig.setAuthenticator(auth);
		auth.initialize(serverConfig, configParams);

		cifsConfig.setTcpipSMB(true);
		cifsConfig.setSMBBindAddress(host);
		cifsConfig.setTcpipSMBPort(port);
		cifsConfig.setServerName(host.getHostName());
		cifsConfig.setDomainName(domain);
		cifsConfig.setSocketTimeout(0);
		cifsConfig.setSessionDebugFlags(DEBUG ? EnumSet.allOf(SMBSrvSession.Dbg.class) : EnumSet.noneOf(SMBSrvSession.Dbg.class));
	}

	private SMBServer createSMB1Server() throws Exception {
		setupSMBConfiguration(false);
		return new SMBServer(serverConfig);
	}

	private SMBServer createSMB2Server() throws Exception {
		if(license == null) {
			throw new IllegalStateException("No license key present!");
		}

		setupSMBConfiguration(true);
		try {
			LicenceConfigSection licenseSection = new LicenceConfigSection(serverConfig);
			licenseSection.setLicenceKey(license);

			Class<?> clazz = ClassUtils.loadClass("org.filesys.smb.server.EnterpriseSMBServer");
			Constructor<?> con = clazz.getConstructor(serverConfig.getClass());
			return (SMBServer) con.newInstance(serverConfig);
		} catch (Exception e) {
			log.error("unable to create SMB Server", e);
			throw e;
		}
	}

	private FTPServer createFTPServer() throws InvalidConfigurationException {
		baseConfiguration();

		FTPConfigSection config = new FTPConfigSection(serverConfig);
		config.setFTPDebug(DEBUG ? EnumSet.allOf(FTPSrvSession.Dbg.class) : EnumSet.noneOf(FTPSrvSession.Dbg.class));
		config.setFTPPort(port);
		config.setFTPRootPath("/" + shareName);
		config.setFTPBindAddress(host);
		serverConfig.addConfigSection(config);
		return new FTPServer(serverConfig);
	}

	public synchronized void startServer(FileSystemType fsType) throws Exception {
		if(server != null) {
			log.warn("skipping start server, already running");
			return;
		}

		switch (fsType) {
		case FTP:
			server = createFTPServer();
			break;
		case SMB1:
			server = createSMB1Server();
			break;
		case SMB2:
			assumeTrue(license != null); //Only run test when a license is present!
			server = createSMB2Server();
			break;

		default:
			break;
		}
		server.startServer();

		await().atMost(1000L, TimeUnit.MILLISECONDS).until(() -> server.isActive());
	}

	@Override
	public void close() throws Exception {
		if(server != null) {
			server.shutdownServer(true);
			server = null;
		}
		if(Files.exists(testDirectory)) {
			FileUtils.cleanDirectory(testDirectory.toFile());
			Files.delete(testDirectory);
		}
	}

	public Path getTestDirectory() {
		if(Files.exists(testDirectory)) {
			return testDirectory;
		}
		throw new IllegalStateException("Test Directory ["+testDirectory+"] does not exist");
	}

	public int getPort() {
		if(server == null) {
			throw new IllegalStateException("Server must be started first!");
		}
		return port;
	}
}
