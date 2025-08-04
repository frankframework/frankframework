/*
   Copyright 2025 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.configuration.util;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import jakarta.annotation.Nonnull;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ClassLoaderManager;
import org.frankframework.configuration.classloaders.DatabaseClassLoader;
import org.frankframework.configuration.classloaders.DirectoryClassLoader;
import org.frankframework.configuration.classloaders.IConfigurationClassLoader;
import org.frankframework.configuration.classloaders.JarFileClassLoader;
import org.frankframework.dbms.DbmsException;
import org.frankframework.dbms.DbmsSupportFactory;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.jdbc.IDataSourceFactory;
import org.frankframework.lifecycle.events.ApplicationMessageEvent;
import org.frankframework.lifecycle.events.MessageEventLevel;
import org.frankframework.util.AppConstants;
import org.frankframework.util.StringUtil;

@Log4j2
public class ConfigurationAutoDiscovery implements ApplicationContextAware {
	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();
	private static final String CONFIGURATIONS = APP_CONSTANTS.getProperty("configurations.names.application");
	private static final String DUPLICATE_CONFIG_WARNING = "config [%s] already exists, cannot add same config twice";

	private final Map<String, Class<? extends IConfigurationClassLoader>> configurations = new LinkedHashMap<>();

	private ApplicationContext applicationContext;

	public Path configurationsDirectory;
	public String datasourceName;

	public ConfigurationAutoDiscovery() {
	}

	public void withDirectoryScanner() throws IOException {
		withDirectoryScanner(null);
	}

	public void withDirectoryScanner(String configurationsDirectory) throws IOException {
		if (StringUtils.isBlank(configurationsDirectory)) {
			this.configurationsDirectory = ConfigurationUtils.getConfigurationDirectory();
			return;
		}

		Path configPath = Path.of(FilenameUtils.normalize(configurationsDirectory, true));
		if (!Files.isDirectory(configPath)) {
			throw new IOException("path ["+configurationsDirectory+"] is not a valid directory");
		}

		this.configurationsDirectory = configPath;
	}

	public void withDatabaseScanner() {
		withDatabaseScanner(null);
	}

	public void withDatabaseScanner(String datasourceName) {
		this.datasourceName = StringUtils.isNotBlank(datasourceName) ? datasourceName : IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * @return A map with all configurations to load (KEY = ConfigurationName, VALUE = ClassLoaderType)
	 */
	@Nonnull
	public Map<String, Class<? extends IConfigurationClassLoader>> scan(boolean includeExplicitlyDefinedConfigurations) {
		configurations.clear();

		if (includeExplicitlyDefinedConfigurations && CONFIGURATIONS != null) {
			for (String configFileName : StringUtil.split(CONFIGURATIONS)) {
				configurations.put(configFileName, null);
			}
		}

		try {
			if (configurationsDirectory != null) {
				configurations.putAll(scanDirectory(configurationsDirectory));
			}
			if (datasourceName != null) {
				configurations.putAll(scanDatabase(datasourceName));
			}
		} catch (Exception e) {
			applicationContext.publishEvent(new ApplicationMessageEvent(applicationContext, "failed to autoload configurations", MessageEventLevel.ERROR, e));
			configurations.clear();
			return Collections.emptyMap();
		}

		log.info("found configurations to load [{}]", configurations);
		return sort(configurations);
	}

	private Map<String, Class<? extends IConfigurationClassLoader>> scanDirectory(Path configDir) throws IOException {
		log.info("scanning directory [{}] for configurations", configDir);

		Map<String, Class<? extends IConfigurationClassLoader>> directoryConfigurations = new LinkedHashMap<>();
		Class<DirectoryClassLoader> defaultDirectoryClassLoaderType = getDefaultDirectoryClassLoaderType();
		for(String name : retrieveDirectoryConfigNames(configDir)) {
			if (directoryConfigurations.get(name) == null) {
				directoryConfigurations.put(name, defaultDirectoryClassLoaderType);
			} else {
				String message = DUPLICATE_CONFIG_WARNING.formatted(name);
				applicationContext.publishEvent(new ApplicationMessageEvent(applicationContext, message, MessageEventLevel.WARN));
			}
		}

		for(String name : retrieveJarFileConfigNames(configDir)) {
			if (directoryConfigurations.get(name) == null) {
				directoryConfigurations.put(name, JarFileClassLoader.class);
			} else {
				String message = DUPLICATE_CONFIG_WARNING.formatted(name);
				applicationContext.publishEvent(new ApplicationMessageEvent(applicationContext, message, MessageEventLevel.WARN));
			}
		}

		return directoryConfigurations;
	}

	private Map<String, Class<? extends IConfigurationClassLoader>> scanDatabase(String dataSourceName) throws DbmsException, SQLException {
		log.info("scanning database [{}] for configurations", dataSourceName);

		Map<String, Class<? extends IConfigurationClassLoader>> databaseConfigurations = new LinkedHashMap<>();
		for (String name : retrieveConfigNamesFromDatabase(dataSourceName)) {
			if (databaseConfigurations.get(name) == null) {
				databaseConfigurations.put(name, DatabaseClassLoader.class);
			} else {
				String message = DUPLICATE_CONFIG_WARNING.formatted(name);
				applicationContext.publishEvent(new ApplicationMessageEvent(applicationContext, message, MessageEventLevel.WARN));
			}
		}

		return databaseConfigurations;
	}

	@SuppressWarnings("unchecked")
	private Class<DirectoryClassLoader> getDefaultDirectoryClassLoaderType() {
		String classLoaderType = APP_CONSTANTS.getString("configurations.directory.classLoaderType", DirectoryClassLoader.class.getCanonicalName());
		try {
			String className = classLoaderType.contains(".") ? classLoaderType : ClassLoaderManager.CLASSLOADER_PACKAGE_LOCATION.formatted(classLoaderType);

			Class<?> clazz = Class.forName(className);
			if (DirectoryClassLoader.class.isAssignableFrom(clazz)) {
				return (Class<DirectoryClassLoader>) clazz;
			}
			log.fatal("incompatible classloader type provided for [configurations.directory.classLoaderType] value [{}]", classLoaderType);
		} catch (ClassNotFoundException e) {
			log.fatal("invalid classloader type provided for [configurations.directory.classLoaderType] value [{}]", classLoaderType);
		}
		return DirectoryClassLoader.class;
	}

	private static <T> Map<String, T> sort(final Map<String, T> allConfigNameItems) {
		List<String> sortedConfigurationNames = new ArrayList<>(allConfigNameItems.keySet());
		sortedConfigurationNames.sort(new ParentConfigComparator());

		Map<String, T> sortedConfigurations = new LinkedHashMap<>();

		sortedConfigurationNames.forEach(name -> sortedConfigurations.put(name, allConfigNameItems.get(name)) );

		return sortedConfigurations;
	}

	private static class ParentConfigComparator implements Comparator<String> {
		AppConstants constants = AppConstants.getInstance();

		@Override
		public int compare(String configName1, String configName2) {
			String parent = constants.getString("configurations." + configName2 + ".parentConfig", null);
			if(configName1.equals(parent)) {
				return -1;
			}
			return configName1.equals(configName2) ? 0 : 1;
		}
	}

	@Nonnull
	private static List<String> retrieveDirectoryConfigNames(Path directory) throws IOException {
		List<String> configurationNames = new ArrayList<>();

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, Files::isDirectory)) {
			for (Path path : stream) {
				configurationNames.add(path.getFileName().toString());
			}
		}

		log.debug("found directory configurations {}", configurationNames);
		return configurationNames;
	}

	@Nonnull
	private static List<String> retrieveJarFileConfigNames(Path directory) throws IOException {
		List<String> configurationNames = new ArrayList<>();

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, JarFileClassLoader::isJarFile)) {
			for (Path path : stream) {
				configurationNames.add(path.getFileName().toString());
			}
		}

		log.debug("found jarFile configurations {}", configurationNames);
		return configurationNames;
	}

	@Nonnull
	private List<String> retrieveConfigNamesFromDatabase(String dataSourceName) throws SQLException, DbmsException {
		IDataSourceFactory dsFactory = applicationContext.getBean(IDataSourceFactory.class);
		DataSource dataSource = dsFactory.getDataSource(dataSourceName);

		DbmsSupportFactory dbmsSupportFactory = applicationContext.getBean(DbmsSupportFactory.class);
		IDbmsSupport dbmsSupport = dbmsSupportFactory.getDbmsSupport(dataSource);
		try (Connection conn = dataSource.getConnection()) {
			if(!dbmsSupport.isTablePresent(conn, "IBISCONFIG")) {
				log.warn("unable to load configurations from database, table [IBISCONFIG] is not present");
				return Collections.emptyList();
			}

			String query = "SELECT DISTINCT(NAME) FROM IBISCONFIG WHERE ACTIVECONFIG="+dbmsSupport.getBooleanValue(true);
			try (PreparedStatement stmt = conn.prepareStatement(query); ResultSet rs = stmt.executeQuery()) {
				List<String> configurationNames = new ArrayList<>();
				while (rs.next()) {
					configurationNames.add(rs.getString(1));
				}

				log.debug("found database configurations {}", configurationNames);
				return Collections.unmodifiableList(configurationNames);
			}
		}
	}
}
