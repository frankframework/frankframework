package org.frankframework.configuration.classloaders;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.net.URL;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.configuration.IbisContext;
import org.frankframework.configuration.util.ConfigurationUtils;
import org.frankframework.util.AppConstants;

public class ResolveConfigurationFileTest {
	private String configurationName;
	private String basePath;
	private String configurationFile;
	private AbstractClassLoader classLoader;
	private AppConstants appConstants;

	private static Stream<Arguments> data() {
		return Stream.of(
			Arguments.of("Config", null, "Config/Configuration.xml"), //No basepath should be derived from configurationFile
			Arguments.of("Config", null, "Configuration.xml"), //No basepath should be derrived from configurationName
			Arguments.of("Config", "Config/", "Configuration.xml"), //setting both shouldn't matter
			Arguments.of("Config", "Config/", "Config/Configuration.xml"), //configurationFile with basepath should be stripped

			Arguments.of("Config", "Config/", null), //no configurationFile should default to Configuration.xml
			Arguments.of("Config", null, null), //no basePath should use configurationName

			Arguments.of("Config", null, "Config/NonDefaultConfiguration.xml"),
			Arguments.of("Config", null, "NonDefaultConfiguration.xml"),
			Arguments.of("Config", "Config/", "NonDefaultConfiguration.xml"),
			Arguments.of("Config", "Config/", "Config/NonDefaultConfiguration.xml")
		);
	}

	@BeforeEach
	public void setUp() throws Exception {
		AppConstants.removeInstance();
		appConstants = AppConstants.getInstance();
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
		IbisContext ibisContext = mock(IbisContext.class);
		classLoader.configure(ibisContext, getConfigurationName());
	}
	private String getConfigurationName() {
		return configurationName;
	}

	private AbstractClassLoader createClassLoader(ClassLoader parent) throws Exception {
		URL file = this.getClass().getResource("/ClassLoader/DirectoryClassLoaderRoot");

		DirectoryClassLoader cl = new DirectoryClassLoader(parent);
		cl.setDirectory(file.getFile());
		appConstants.put("configurations."+getConfigurationName()+".directory", file.getFile());
		return cl;
	}

	@ParameterizedTest
	@MethodSource("data")
	public void properBasePathAndConfigurationFile(String configName, String basePath, String configurationFile) throws Exception {
		this.configurationName = configName;
		this.basePath = basePath;
		this.configurationFile = configurationFile;
		createAndConfigure();

		String configFile = ConfigurationUtils.getConfigurationFile(classLoader, getConfigurationName());

		assertTrue((configFile.indexOf('/') == -1), "configurationFile should not contain a BasePath ["+configFile+"]");

		URL configurationFileURL = classLoader.getResource(configFile);
		assertNotNull(configurationFileURL, "configurationFile cannot be found");

		String filePath = configurationFileURL.getPath();
		String root = classLoader.getBasePath();
		assertTrue(filePath.endsWith(root+configFile), "filePath ["+filePath+"] should consists of basePath ["+root+"] and configFile ["+configFile+"]");
	}
}
