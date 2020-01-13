package nl.nn.adapterframework.configuration.classloaders;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.util.AppConstants;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

@RunWith(Parameterized.class)
public class ResolveConfigurationFileTest extends Mockito {
	private String configurationName;
	private String basePath;
	private String configurationFile;
	private ClassLoaderBase classLoader;
	private AppConstants appConstants;
	private IbisContext ibisContext = spy(new IbisContext());

	@Parameters(name = "{0} - {1}") //Name - BasePath - ConfigurationFile
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ "Config", null, "Config/Configuration.xml" }, //No basepath should be derrived from configurationFile
				{ "Config", null, "Configuration.xml" }, //No basepath should be derrived from configurationName
				{ "Config", "Config/", "Configuration.xml" }, //setting both shouldn't matter
				{ "Config", "Config/", "Config/Configuration.xml" }, //configurationFile with basepath should be stripped

				{ "Config", "Config/", null }, //no configurationFile should default to Configuration.xml
				{ "Config", null, null }, //no basePath should use configurationName

				{ "Config", null, "Config/NonDefaultConfiguration.xml" },
				{ "Config", null, "NonDefaultConfiguration.xml" },
				{ "Config", "Config/", "NonDefaultConfiguration.xml" },
				{ "Config", "Config/", "Config/NonDefaultConfiguration.xml" },
		});
	}

	public ResolveConfigurationFileTest(String configName, String basePath, String configFile) {
		this.configurationName = configName;
		this.basePath = basePath;
		this.configurationFile = configFile;
	}

	@Before
	public void setUp() throws Exception {
		AppConstants.removeInstance();
		appConstants = AppConstants.getInstance();
		createAndConfigure();
	}
	protected void createAndConfigure() throws Exception {
		ClassLoader parent = new ClassLoaderMock();
		classLoader = createClassLoader(parent);

		if(basePath != null) {
			classLoader.setBasePath(basePath);
		}
		if(configurationFile != null) {
			classLoader.setConfigurationFile(configurationFile);
			appConstants.put("configurations."+getConfigurationName()+".configurationFile", configurationFile);
		} else {
			appConstants.put("configurations."+getConfigurationName()+".configurationFile", "");
		}

		appConstants.put("configurations."+getConfigurationName()+".classLoaderType", classLoader.getClass().getSimpleName());
		classLoader.configure(ibisContext, getConfigurationName());
	}
	private String getConfigurationName() {
		return configurationName;
	}

	private ClassLoaderBase createClassLoader(ClassLoader parent) throws Exception {
		URL file = this.getClass().getResource("/ClassLoader/DirectoryClassLoaderRoot");

		DirectoryClassLoader cl = new DirectoryClassLoader(parent);
		cl.setDirectory(file.getFile());
		appConstants.put("configurations."+getConfigurationName()+".directory", file.getFile());
		return cl;
	}

	@Test
	public void properBasePathAndConfigurationFile() {
		String configFile = ConfigurationUtils.getConfigurationFile(classLoader, getConfigurationName());

		assertTrue("configurationFile should not contain a BasePath ["+configFile+"]", (configFile.indexOf('/') == -1));

		URL configurationFileURL = classLoader.getResource(configFile);
		assertNotNull("configurationFile cannot be found", configurationFileURL);

		String filePath = configurationFileURL.getPath();
		String root = classLoader.getBasePath();
		System.out.println(root);
		assertTrue("filePath ["+filePath+"] should consists of basePath ["+root+"] and configFile ["+configFile+"]", filePath.endsWith(root+configFile));
	}
}
