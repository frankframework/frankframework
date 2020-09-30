package nl.nn.adapterframework.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import nl.nn.adapterframework.configuration.classloaders.ClassLoaderBase;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jdbc.dbms.GenericDbmsSupport;
import nl.nn.adapterframework.jms.JmsRealm;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.pipes.EchoPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.unmanaged.DefaultIbisManager;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.Misc;

@RunWith(Parameterized.class)
public class ClassLoaderManagerTest extends Mockito {

	private IbisContext ibisContext = spy(new IbisContext());
	private ClassLoaderManager manager;

	private final String BASE_DIR = "/ClassLoader";
	private final String JAR_FILE = BASE_DIR+ "/zip/classLoader-test.zip";
	private final String ADAPTER_SERVICE_NAME = "getJarFileAdapter";

	private final static String CONFIG_0_NAME = "config0";
	private final static String CONFIG_1_NAME = "config1";
	private final static String CONFIG_2_NAME = "config2";
	private final static String CONFIG_3_NAME = "config3";
	private final static String CONFIG_4_NAME = "config4";
	private final static String CONFIG_5_NAME = "config5";
	private final static String CONFIG_6_NAME = "config6";

	// declarations for parameterized tests
	private String type = null;
	private boolean skip = false;
	private String configurationName;
	private AppConstants appConstants;

	@Parameters(name = "{0} - {1}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ "", CONFIG_0_NAME },
				{ "WebAppClassLoader", CONFIG_1_NAME },
				{ "DirectoryClassLoader", CONFIG_2_NAME },
				{ "JarFileClassLoader", CONFIG_3_NAME },
				{ "ServiceClassLoader", CONFIG_4_NAME },
				{ "DatabaseClassLoader", CONFIG_5_NAME },
				{ "DummyClassLoader", CONFIG_6_NAME },
				{ "nl.nn.adapterframework.configuration.classloaders.WebAppClassLoader", CONFIG_1_NAME },
				{ "nl.nn.adapterframework.configuration.classloaders.DirectoryClassLoader", CONFIG_2_NAME },
				{ "nl.nn.adapterframework.configuration.classloaders.JarFileClassLoader", CONFIG_3_NAME },
				{ "nl.nn.adapterframework.configuration.classloaders.ServiceClassLoader", CONFIG_4_NAME },
				{ "nl.nn.adapterframework.configuration.classloaders.DatabaseClassLoader", CONFIG_5_NAME },
				{ "nl.nn.adapterframework.configuration.classloaders.DummyClassLoader", CONFIG_6_NAME },
				{ "nl.nn.adapterframework.configuration.classloaders.DefaultClassLoader", "tralla"}
		});
	}

	public ClassLoaderManagerTest(String type, String configurationName) throws Exception {
		AppConstants.removeInstance();
		appConstants = AppConstants.getInstance();

		if(type == null || type.equals("DummyClassLoader"))
			skip = true;
		else if(type.isEmpty()) //If empty string, it's a WebAppClassLoader
			type = "WebAppClassLoader";

		this.type = type;
		this.configurationName = configurationName;

		if(type.endsWith("DirectoryClassLoader")) {
			String directory = getTestClassesLocation()+"ClassLoader/DirectoryClassLoaderRoot/";
			setLocalProperty("configurations."+configurationName+".directory", directory);
			setLocalProperty("configurations."+configurationName+".basePath", ".");
		}

		if(type.endsWith("JarFileClassLoader")) {
			URL file = this.getClass().getResource(JAR_FILE);
			setLocalProperty("configurations."+configurationName+".jar", file.getFile());
		}

		if(type.endsWith("ServiceClassLoader")) {
			setLocalProperty("configurations."+configurationName+".adapterName", ADAPTER_SERVICE_NAME);
		}
	}

	/**
	 * In order to test the DirectoryClassloader we need the absolute path of where it can find it's configuration(s)
	 * @return the path to the mvn generated test-classes folder
	 */
	private String getTestClassesLocation() {
		String file = "test1.xml";
		String testPath = this.getClass().getResource("/"+file).getPath();
		testPath = testPath.substring(0, testPath.indexOf(file));
		return testPath;
	}

	@Before
	public void setUp() throws ConfigurationException, Exception {
		String configurationsNames = "";
		for(Object[] o: data()) {
			configurationsNames += o[1]+",";
			if(o[0] != null) {
				String value = (String) o[0];
				setLocalProperty("configurations."+o[1]+".classLoaderType", value);
			}
		}
		setLocalProperty("configurations.names", configurationsNames);

		createAdapter4ServiceClassLoader(ADAPTER_SERVICE_NAME);
		mockDatabase();

		manager = new ClassLoaderManager(ibisContext);
	}

	private void mockDatabase() throws Exception {
		// Mock a FixedQuerySender
		JmsRealm jmsRealm = spy(new JmsRealm());
		jmsRealm.setDatasourceName("fake");
		jmsRealm.setRealmName("myRealm");
		JmsRealmFactory.getInstance().registerJmsRealm(jmsRealm);
		FixedQuerySender fq = mock(FixedQuerySender.class);
		doReturn(new GenericDbmsSupport()).when(fq).getDbmsSupport();

		Connection conn = mock(Connection.class);
		doReturn(conn).when(fq).getConnection();
		PreparedStatement stmt = mock(PreparedStatement.class);
		doReturn(stmt).when(conn).prepareStatement(anyString());
		ResultSet rs = mock(ResultSet.class);
		doReturn(true).when(rs).next();
		doReturn("dummy").when(rs).getString(anyInt());
		URL file = this.getClass().getResource(JAR_FILE);
		doReturn(Misc.streamToBytes(file.openStream())).when(rs).getBytes(anyInt());
		doReturn(rs).when(stmt).executeQuery();
		doReturn(fq).when(ibisContext).createBeanAutowireByName(FixedQuerySender.class);
	}

	private void createAdapter4ServiceClassLoader(String config4Adaptername) throws ConfigurationException {
		// Mock a configuration with an adapter in it
		IbisManager ibisManager = spy(new DefaultIbisManager());
		ibisManager.setIbisContext(ibisContext);
		Configuration configuration = new Configuration(new DummyAdapterService());
		configuration.setName("dummyConfiguration");
		configuration.setVersion("1");
		configuration.setIbisManager(ibisManager);

		IAdapter adapter = spy(new Adapter());
		adapter.setName(config4Adaptername);
		PipeLine pl = new PipeLine();
		pl.setFirstPipe("dummy");
		EchoPipe pipe = new EchoPipe();
		pipe.setName("dummy");
		pl.addPipe(pipe);
		PipeLineExit ple = new PipeLineExit();
		ple.setPath("success");
		ple.setState("success");
		pl.registerPipeLineExit(ple);
		adapter.registerPipeLine(pl);

		doAnswer(new Answer<PipeLineResult>() {
			@Override
			public PipeLineResult answer(InvocationOnMock invocation) throws Throwable {
				IPipeLineSession session = (IPipeLineSession) invocation.getArguments()[2];
				URL file = this.getClass().getResource(JAR_FILE);
				session.put("configurationJar", Misc.streamToBytes(file.openStream()));
				return new PipeLineResult();
			}
		}).when(adapter).processMessage(anyString(), any(Message.class), any(IPipeLineSession.class));

		adapter.setConfiguration(configuration);
		configuration.registerAdapter(adapter);

		ibisManager.addConfiguration(configuration);
		when(ibisContext.getIbisManager()).thenReturn(ibisManager);
	}

	private ClassLoader getClassLoader() throws ConfigurationException {
		return getClassLoader(configurationName);
	}

	private ClassLoader getClassLoader(String testConfiguration) throws ConfigurationException {
		ClassLoader config = manager.get(testConfiguration);
		if(config instanceof ClassLoaderBase) {
			((ClassLoaderBase)config).setBasePath(".");
		}
		return config;
	}

	@Test
	public void properClassLoaderType() throws ConfigurationException {
		assertNull(appConstants.get("configurations."+configurationName+".parentConfig"));
		ClassLoader config = getClassLoader();

		//in case the FQDN has been used, strip it
		String name = type;
		if(type.indexOf(".") > 0)
			name = type.substring(type.lastIndexOf(".")+1);
		assertEquals(name, config.getClass().getSimpleName());
	}

	@Test
	public void retrieveTestFileNotInClassLoader() throws ConfigurationException, IOException {
		assertNull(appConstants.get("configurations."+configurationName+".parentConfig"));
		ClassLoader config = getClassLoader();
		URL resource = config.getResource("test1.xml");

		MatchUtils.assertTestFileEquals("/test1.xml", resource);
	}

	@Test
	public void retrieveTestFileInClassLoaderRoot() throws ConfigurationException, IOException {
		if(skip) return; //This ClassLoader can't actually retrieve files...

		assertNull(appConstants.get("configurations."+configurationName+".parentConfig"));
		ClassLoader config = getClassLoader();
		URL resource = config.getResource("ClassLoaderTestFile.xml");

		MatchUtils.assertTestFileEquals("/ClassLoaderTestFile.xml", resource);
	}

	@Test
	public void retrieveTestFileInSubFolder() throws ConfigurationException, IOException {
		if(skip) return; //This ClassLoader can't actually retrieve files...

		assertNull(appConstants.get("configurations."+configurationName+".parentConfig"));
		ClassLoader config = getClassLoader();
		URL resource = config.getResource(BASE_DIR.substring(1)+"/ClassLoaderTestFile.xml");

		MatchUtils.assertTestFileEquals(BASE_DIR+"/ClassLoaderTestFile.xml", resource);
	}

	@Test
	public void retrieveNonExistingTestFile() throws ConfigurationException, IOException {
		assertNull(appConstants.get("configurations."+configurationName+".parentConfig"));
		ClassLoader config = getClassLoader();
		URL resource = config.getResource("dummy-test-file.xml");

		assertNull(resource);
	}

	@Test
	public void testInheritanceMakeSureFileIsFoundInBothParentAndChild() throws ConfigurationException, IOException {
		if(skip) return; //This ClassLoader can't actually retrieve files...

		String testConfiguration = "myNewClassLoader";
		setLocalProperty("configurations."+testConfiguration+".classLoaderType", "DirectoryClassLoader");
		setLocalProperty("configurations."+testConfiguration+".basePath", ".");
		setLocalProperty("configurations."+configurationName+".parentConfig", testConfiguration);
		String directory = getTestClassesLocation()+"ClassLoader/";
		setLocalProperty("configurations."+testConfiguration+".directory", directory);
		setLocalProperty("configurations.names", appConstants.get("configurations.names") + ","+testConfiguration);

		String testFile = "fileOnlyOnLocalClassPath.txt";
		ClassLoader parentClassloader = getClassLoader(testConfiguration);

		assertEquals("DirectoryClassLoader", parentClassloader.getClass().getSimpleName());
		URL parentResource = parentClassloader.getResource(testFile);
		assertNotNull(parentResource);

		ClassLoader config = getClassLoader();
		URL resource = config.getResource(testFile);

		assertNotNull(resource);
		appConstants.remove("configurations."+configurationName+".parentConfig");
	}

	@Test
	public void reloadString() throws ConfigurationException, IOException {
		assertNull(appConstants.get("configurations."+configurationName+".parentConfig"));
		ClassLoader config1 = manager.get(configurationName);

		manager.reload(configurationName);

		//Make sure the manager returns the same classloader after reloading it
		ClassLoader config2 = manager.get(configurationName);
		assertEquals(config1.toString(), config2.toString());
	}

	@Test
	public void reloadClassLoader() throws ConfigurationException, IOException {
		assertNull(appConstants.get("configurations."+configurationName+".parentConfig"));
		ClassLoader config1 = manager.get(configurationName);

		manager.reload(config1);

		//Make sure the manager returns the same classloader after reloading it
		ClassLoader config2 = manager.get(configurationName);
		assertEquals(config1.toString(), config2.toString());
	}

	/**This method makes sure the property is only set in the local AppConstants instance.
	 * When you use the default put/setProperty it stores the property in the additionalProperties and
	 * upon creation of a new AppConstants instance it will set all the previously set properties
	 */
	@SuppressWarnings("deprecation")
	private void setLocalProperty(String key, String value) {
		appConstants.put((Object) key, (Object) value);
	}
}
