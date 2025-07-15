/*
   Copyright 2013, 2016-2020 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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
package org.frankframework.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import org.frankframework.configuration.classloaders.DatabaseClassLoader;
import org.frankframework.configuration.classloaders.DirectoryClassLoader;
import org.frankframework.configuration.classloaders.IConfigurationClassLoader;
import org.frankframework.configuration.classloaders.WebAppClassLoader;
import org.frankframework.core.IbisTransaction;
import org.frankframework.dbms.JdbcException;
import org.frankframework.jdbc.FixedQuerySender;
import org.frankframework.jdbc.IDataSourceFactory;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.lifecycle.events.ApplicationMessageEvent;
import org.frankframework.lifecycle.events.MessageEventLevel;
import org.frankframework.util.AppConstants;
import org.frankframework.util.JdbcUtil;
import org.frankframework.util.LogUtil;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.StringUtil;

/**
 * Functions to manipulate the configuration.
 *
 * @author  Peter Leeuwenburgh
 * @author  Jaco de Groot
 */
public class ConfigurationUtils {
	private static final Logger log = LogUtil.getLogger(ConfigurationUtils.class);

	public static final String STUB4TESTTOOL_CONFIGURATION_KEY = "stub4testtool.configuration";
	public static final String STUB4TESTTOOL_VALIDATORS_DISABLED_KEY = "validators.disabled";
	public static final String STUB4TESTTOOL_XSLT_VALIDATORS_PARAM = "disableValidators";
	public static final String STUB4TESTTOOL_XSLT_KEY = "stub4testtool.xsl";
	public static final String STUB4TESTTOOL_XSLT_DEFAULT = "/xml/xsl/stub4testtool.xsl";

	public static final String FRANK_CONFIG_XSD = "/xml/xsd/FrankConfig-compatibility.xsd";
	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();
	private static final boolean CONFIG_AUTO_DB_CLASSLOADER = APP_CONSTANTS.getBoolean("configurations.database.autoLoad", false);
	private static final boolean CONFIG_AUTO_FS_CLASSLOADER = APP_CONSTANTS.getBoolean("configurations.directory.autoLoad", false);
	private static final String DEFAULT_AUTO_DB_CLASSLOADER_TYPE = APP_CONSTANTS.getString("configurations.directory.classLoaderType", DirectoryClassLoader.class.getCanonicalName());
	private static final String INSTANCE_NAME = AppConstants.getInstance().getProperty("instance.name", null);
	private static final String CONFIGURATIONS = APP_CONSTANTS.getProperty("configurations.names.application");
	public static final String DEFAULT_CONFIGURATION_FILE = "Configuration.xml";

	private static final String DUMMY_SELECT_QUERY = "SELECT COUNT(*) FROM IBISCONFIG";

	/**
	 * Checks if a configuration is stubbed or not
	 */
	public static boolean isConfigurationStubbed(ClassLoader classLoader) {
		return AppConstants.getInstance(classLoader).getBoolean(STUB4TESTTOOL_CONFIGURATION_KEY, false);
	}

	public static boolean isConfigurationXmlOptional(Configuration configuration) {
		return CONFIG_AUTO_FS_CLASSLOADER &&
				configuration.getClassLoader() instanceof WebAppClassLoader &&
				configuration.getName().equals(INSTANCE_NAME);
	}

	public static String getConfigurationFile(ClassLoader classLoader, String currentConfigurationName) {
		String configFileKey = "configurations." + currentConfigurationName + ".configurationFile";
		String configurationFile = AppConstants.getInstance(classLoader).getProperty(configFileKey);
		if (StringUtils.isEmpty(configurationFile)) {
			configurationFile = AppConstants.getInstance(classLoader.getParent()).getProperty(configFileKey);
		}
		if (StringUtils.isEmpty(configurationFile)) {
			configurationFile = DEFAULT_CONFIGURATION_FILE;
		} else {
			int i = configurationFile.lastIndexOf('/');
			if (i != -1) { //Trim the BasePath, why is it even here!?
				configurationFile = configurationFile.substring(i + 1);
			}
		}
		return configurationFile;
	}

	/**
	 * Get the version (configuration.version + configuration.timestamp) from the configuration's AppConstants.
	 */
	public static String getConfigurationVersion(ClassLoader classLoader) {
		return getConfigurationVersion(AppConstants.getInstance(classLoader));
	}

	protected static String getConfigurationVersion(Properties properties) {
		return getVersion(properties, "configuration.version", "configuration.timestamp");
	}

	/**
	 * Get the application version (instance.version + instance.timestamp)
	 */
	public static String getApplicationVersion() {
		return getVersion(AppConstants.getInstance(), "instance.version", "instance.timestamp");
	}

	private static String getVersion(Properties properties, String versionKey, String timestampKey) {
		String version = null;
		if (StringUtils.isNotEmpty(properties.getProperty(versionKey))) {
			version = properties.getProperty(versionKey);
			if (StringUtils.isNotEmpty(properties.getProperty(timestampKey))) {
				version = version + "_" + properties.getProperty(timestampKey);
			}
		}
		return version;
	}

	public static List<Map<String, Object>> getActiveConfigsFromDatabase(ApplicationContext applicationContext, String dataSourceName) throws ConfigurationException {
		String workdataSourceName = dataSourceName;
		if (StringUtils.isEmpty(workdataSourceName)) {
			workdataSourceName = IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME;
		}

		if(log.isInfoEnabled()) log.info("trying to fetch all active configurations from database with dataSourceName [{}]", workdataSourceName);

		FixedQuerySender qs = SpringUtils.createBean(applicationContext);
		qs.setDatasourceName(workdataSourceName);
		qs.setQuery(DUMMY_SELECT_QUERY);
		qs.configure();
		try {
			qs.start();
			try(Connection conn = qs.getConnection()) {
				String query = "SELECT CONFIG, VERSION, FILENAME, CRE_TYDST, RUSER FROM IBISCONFIG WHERE ACTIVECONFIG="+(qs.getDbmsSupport().getBooleanValue(true));
				try (PreparedStatement stmt = conn.prepareStatement(query)) {
					return extractConfigurationsFromResultSet(stmt);
				}
			}
		} catch (LifecycleException | JdbcException | SQLException e) {
			throw new ConfigurationException(e);
		} finally {
			qs.stop();
		}
	}

	public static Map<String, Object> getActiveConfigFromDatabase(ApplicationContext applicationContext, String name, String dataSourceName) throws ConfigurationException {
		return getConfigFromDatabase(applicationContext, name, dataSourceName, null);
	}

	public static Map<String, Object> getConfigFromDatabase(ApplicationContext applicationContext, String name, String dataSourceName, String version) throws ConfigurationException {
		String workdataSourceName = dataSourceName;
		if (StringUtils.isEmpty(workdataSourceName)) {
			workdataSourceName = IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME;
		}
		if (StringUtils.isEmpty(version)) {
			version = null; //Make sure this is null when empty!
		}
		if(log.isInfoEnabled()) log.info("trying to fetch configuration [{}] version [{}] from database with dataSourceName [{}]", name, version, workdataSourceName);

		FixedQuerySender qs = SpringUtils.createBean(applicationContext);
		qs.setDatasourceName(workdataSourceName);
		qs.setQuery(DUMMY_SELECT_QUERY);
		qs.configure();
		try {
			qs.start();
			try(Connection conn = qs.getConnection()) {
				if (version == null) { // Return active config
					String query = "SELECT CONFIG, VERSION, FILENAME, CRE_TYDST, RUSER FROM IBISCONFIG WHERE NAME=? AND ACTIVECONFIG="+(qs.getDbmsSupport().getBooleanValue(true));
					try (PreparedStatement stmt = conn.prepareStatement(query)) {
						stmt.setString(1, name);
						return extractConfigurationFromResultSet(stmt, name, null);
					}
				}
				else {
					String query = "SELECT CONFIG, VERSION, FILENAME, CRE_TYDST, RUSER FROM IBISCONFIG WHERE NAME=? AND VERSION=?";
					try (PreparedStatement stmt = conn.prepareStatement(query)) {
						stmt.setString(1, name);
						stmt.setString(2, version);
						return extractConfigurationFromResultSet(stmt, name, version);
					}
				}
			}
		} catch (LifecycleException | JdbcException | SQLException e) {
			throw new ConfigurationException(e);
		} finally {
			qs.stop();
		}
	}

	private static Map<String, Object> extractConfigurationFromResultSetRow (ResultSet rs) throws SQLException {
		Map<String, Object> configuration = new HashMap<>(5);
		byte[] jarBytes = rs.getBytes(1);
		if (jarBytes == null) return null;

		configuration.put("CONFIG", jarBytes);
		configuration.put("VERSION", rs.getString(2));
		configuration.put("FILENAME", rs.getString(3));
		configuration.put("CREATED", rs.getString(4));
		configuration.put("USER", rs.getString(5));
		return configuration;
	}

	private static List<Map<String, Object>> extractConfigurationsFromResultSet(PreparedStatement stmt) throws SQLException {
		try(ResultSet rs = stmt.executeQuery()) {
			List<Map<String, Object>> configs = new ArrayList<>();

			while (rs.next()) {
				Map<String, Object> rowConfig = extractConfigurationFromResultSetRow(rs);
				if(rowConfig != null) {
					configs.add(rowConfig);
				}
			}

			return configs;
		}
	}

	private static Map<String, Object> extractConfigurationFromResultSet(PreparedStatement stmt, String name, String version) throws SQLException {
		try(ResultSet rs = stmt.executeQuery()) {
			if (!rs.next()) {
				log.error("no configuration found in database with name [{}] {}", name, version != null ? "version [" + version + "]" : "activeconfig [TRUE]");
				return null;
			}

			return extractConfigurationFromResultSetRow(rs);
		}
	}

	/**
	 * @return name of the configuration if successful SQL update
	 * @throws ConfigurationException when SQL error occurs or filename validation fails
	 */
	public static String addConfigToDatabase(ApplicationContext applicationContext, String datasource, boolean activate_config, boolean automatic_reload, String fileName, InputStream file, String ruser) throws ConfigurationException {
		BuildInfoValidator configDetails = new BuildInfoValidator(file);
		if(addConfigToDatabase(applicationContext, datasource, activate_config, automatic_reload, configDetails.getName(), configDetails.getVersion(), fileName, configDetails.getJar(), ruser)) {
			return configDetails.getName() +": "+ configDetails.getVersion();
		}
		return null;
	}

	public static Map<String, String> processMultiConfigZipFile(ApplicationContext applicationContext, String datasource, boolean activate_config, boolean automatic_reload, InputStream file, String ruser) throws IOException, ConfigurationException {
		Map<String, String> result = new LinkedHashMap<>();
		if (file.available() > 0) {
			try (ZipInputStream zipInputStream = new ZipInputStream(file)) {
				ZipEntry zipEntry;
				while ((zipEntry = zipInputStream.getNextEntry()) != null) {
					String entryName = zipEntry.getName();
					try {
						String configName = ConfigurationUtils.addConfigToDatabase(applicationContext, datasource, activate_config, automatic_reload, entryName, StreamUtil.dontClose(zipInputStream), ruser);
						result.put(configName, "loaded");
					} catch (ConfigurationException e) {
						log.error("an error occurred while trying to store new configuration using datasource [{}]", datasource, e);
						result.put(entryName, e.getMessage());
					}
				}
			}
		}
		return result;
	}

	public static boolean addConfigToDatabase(ApplicationContext applicationContext, String dataSourceName, boolean activateConfig, boolean automaticReload, String name, String version, String fileName, InputStream file, String ruser) throws ConfigurationException {
		String workdataSourceName = dataSourceName;
		if (StringUtils.isEmpty(workdataSourceName)) {
			workdataSourceName = IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME;
		}

		Connection conn = null;
		FixedQuerySender qs = SpringUtils.createBean(applicationContext);
		qs.setDatasourceName(workdataSourceName);
		qs.setQuery(DUMMY_SELECT_QUERY);
		qs.configure();

		PlatformTransactionManager txManager = applicationContext.getBean("txManager", PlatformTransactionManager.class);
		TransactionDefinition txDef = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		IbisTransaction itx = new IbisTransaction(txManager , txDef, "add config ["+name+"] to database");
		try {
			qs.start();
			conn = qs.getConnection();
			int updated = 0;

			if (activateConfig) {
				String query = "UPDATE IBISCONFIG SET ACTIVECONFIG="+(qs.getDbmsSupport().getBooleanValue(false))+" WHERE NAME=?";
				try (PreparedStatement stmt = conn.prepareStatement(query)) {
					stmt.setString(1, name);
					updated = stmt.executeUpdate();
				}
			}
			if (updated > 0) {
				String query = "DELETE FROM IBISCONFIG WHERE NAME=? AND VERSION = ?";
				try (PreparedStatement stmt = conn.prepareStatement(query)) {
					stmt.setString(1, name);
					stmt.setString(2, version);
					stmt.execute();
				}
			}

			String activeBool = qs.getDbmsSupport().getBooleanValue(activateConfig);
			String reloadBool = qs.getDbmsSupport().getBooleanValue(automaticReload);
			String query = "INSERT INTO IBISCONFIG (NAME, VERSION, FILENAME, CONFIG, CRE_TYDST, RUSER, ACTIVECONFIG, AUTORELOAD) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?, "+activeBool+", "+reloadBool+")";
			try (PreparedStatement stmt = conn.prepareStatement(query)) {
				stmt.setString(1, name);
				stmt.setString(2, version);
				stmt.setString(3, fileName);
				stmt.setBinaryStream(4, file);
				if (StringUtils.isEmpty(ruser)) {
					stmt.setNull(5, Types.VARCHAR);
				} else {
					stmt.setString(5, ruser);
				}

				return stmt.executeUpdate() > 0;
			}
		} catch (LifecycleException | JdbcException | SQLException e) {
			itx.setRollbackOnly();
			throw new ConfigurationException(e);
		} finally {
			itx.complete();
			JdbcUtil.close(conn);
			qs.stop();
		}
	}

	public static void removeConfigFromDatabase(ApplicationContext applicationContext, String name, String datasourceName, String version) throws ConfigurationException {
		String workdataSourceName = datasourceName;
		if (StringUtils.isEmpty(workdataSourceName)) {
			workdataSourceName = IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME;
		}

		FixedQuerySender qs = SpringUtils.createBean(applicationContext);
		qs.setDatasourceName(workdataSourceName);
		qs.setQuery(DUMMY_SELECT_QUERY);
		qs.configure();
		try {
			qs.start();
			try (Connection conn = qs.getConnection()) {
				String query = "DELETE FROM IBISCONFIG WHERE NAME=? AND VERSION=?";
				try (PreparedStatement stmt = conn.prepareStatement(query)) {
					stmt.setString(1, name);
					stmt.setString(2, version);
					stmt.execute();
				}
			}
		} catch (LifecycleException | JdbcException | SQLException e) {
			throw new ConfigurationException(e);
		} finally {
			qs.stop();
		}
	}

	/**
	 * Set the all ACTIVECONFIG to false and specified version to true
	 */
	public static boolean activateConfig(ApplicationContext applicationContext, String name, String version, String dataSourceName) throws ConfigurationException, JdbcException, SQLException {
		String workdataSourceName = dataSourceName;
		if (StringUtils.isEmpty(workdataSourceName)) {
			workdataSourceName = IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME;
		}

		Connection conn = null;
		FixedQuerySender qs = SpringUtils.createBean(applicationContext);
		qs.setDatasourceName(workdataSourceName);
		qs.setQuery(DUMMY_SELECT_QUERY);
		qs.configure();
		String booleanValueFalse = qs.getDbmsSupport().getBooleanValue(false);
		String booleanValueTrue = qs.getDbmsSupport().getBooleanValue(true);

		try {
			qs.start();
			conn = qs.getConnection();
			int updated;

			String selectQuery = "SELECT NAME FROM IBISCONFIG WHERE NAME=? AND VERSION=?";
			try (PreparedStatement selectStmt = conn.prepareStatement(selectQuery)) {
				selectStmt.setString(1, name);
				selectStmt.setString(2, version);
				try (ResultSet rs = selectStmt.executeQuery()) {
					if (rs.next()) {
						String query = "UPDATE IBISCONFIG SET ACTIVECONFIG=" + booleanValueFalse + " WHERE NAME=?";

						try (PreparedStatement stmt = conn.prepareStatement(query)) {
							stmt.setString(1, name);
							updated = stmt.executeUpdate();
						}
						if (updated > 0) {
							String query2 = "UPDATE IBISCONFIG SET ACTIVECONFIG=" + booleanValueTrue + " WHERE NAME=? AND VERSION=?";
							try (PreparedStatement stmt2 = conn.prepareStatement(query2)) {
								stmt2.setString(1, name);
								stmt2.setString(2, version);
								return stmt2.executeUpdate() > 0;
							}
						}
					}
				}
			}
		} finally {
			JdbcUtil.close(conn);
			qs.stop();
		}
		return false;
	}

	/**
	 * Toggle AUTORELOAD
	 */
	public static boolean autoReloadConfig(ApplicationContext applicationContext, String name, String version, boolean booleanValue, String dataSourceName) throws ConfigurationException, JdbcException, SQLException {
		String workdataSourceName = dataSourceName;
		if (StringUtils.isEmpty(workdataSourceName)) {
			workdataSourceName = IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME;
		}

		FixedQuerySender qs =  SpringUtils.createBean(applicationContext);
		qs.setDatasourceName(workdataSourceName);
		qs.setQuery(DUMMY_SELECT_QUERY);
		qs.configure();

		try {
			qs.start();
			try (Connection conn = qs.getConnection()) {
				String selectQuery = "SELECT NAME FROM IBISCONFIG WHERE NAME=? AND VERSION=?";
				try (PreparedStatement selectStmt = conn.prepareStatement(selectQuery)) {
					selectStmt.setString(1, name);
					selectStmt.setString(2, version);
					try (ResultSet rs = selectStmt.executeQuery()) {
						if (rs.next()) {
							String query = "UPDATE IBISCONFIG SET AUTORELOAD=" + qs.getDbmsSupport().getBooleanValue(booleanValue) + " WHERE NAME=? AND VERSION=?";

							try (PreparedStatement stmt = conn.prepareStatement(query)) {
								stmt.setString(1, name);
								stmt.setString(2, version);
								return stmt.executeUpdate() > 0;
							}
						}
					}
				}
			}
		} finally {
			qs.stop();
		}
		return false;
	}

	/**
	 * @return A map with all configurations to load (KEY = ConfigurationName, VALUE = ClassLoaderType)
	 */
	public static Map<String, Class<? extends IConfigurationClassLoader>> retrieveAllConfigNames(ApplicationContext applicationContext) {
		return retrieveAllConfigNames(applicationContext, CONFIG_AUTO_FS_CLASSLOADER, CONFIG_AUTO_DB_CLASSLOADER);
	}

	// protected because of jUnit tests
	protected static Map<String, Class<? extends IConfigurationClassLoader>> retrieveAllConfigNames(ApplicationContext applicationContext, boolean directoryConfigurations, boolean databaseConfigurations) {
		Map<String, Class<? extends IConfigurationClassLoader>> allConfigNameItems = new LinkedHashMap<>();

		if (CONFIGURATIONS != null) {
			for (String configFileName : StringUtil.split(CONFIGURATIONS)) {
				allConfigNameItems.put(configFileName, null);
			}
		}

		if (directoryConfigurations) {
			String configDir = AppConstants.getInstance().getProperty("configurations.directory");
			log.info("scanning directory [{}] for configurations", configDir);
			try {
			Class<DirectoryClassLoader> classLoaderType = getDefaultDirectoryClassLoaderType(DEFAULT_AUTO_DB_CLASSLOADER_TYPE);
				for(String name : retrieveDirectoryConfigNames(configDir)) {
					if (allConfigNameItems.get(name) == null) {
						allConfigNameItems.put(name, classLoaderType);
					} else {
						log.warn("config [{}] already exists in {}, cannot add same config twice", name, allConfigNameItems);
					}
				}
			} catch (IOException e) {
				applicationContext.publishEvent(new ApplicationMessageEvent(applicationContext, "failed to autoload configurations", MessageEventLevel.WARN, e));
			}
		}
		if (databaseConfigurations) {
			log.info("scanning database for configurations");
			try {
				List<String> dbConfigNames = ConfigurationUtils.retrieveConfigNamesFromDatabase(applicationContext);
				for (String dbConfigName : dbConfigNames) {
					if (allConfigNameItems.get(dbConfigName) == null) {
						allConfigNameItems.put(dbConfigName, DatabaseClassLoader.class);
					} else {
						log.warn("config [{}] already exists in {}, cannot add same config twice", dbConfigName, allConfigNameItems);
					}
				}
			}
			catch (ConfigurationException e) {
				applicationContext.publishEvent(new ApplicationMessageEvent(applicationContext, "error retrieving database configurations", MessageEventLevel.WARN, e));
			}
		}

		log.info("found configurations to load [{}]", allConfigNameItems);

		return sort(allConfigNameItems);
	}

	@SuppressWarnings("unchecked")
	protected static Class<DirectoryClassLoader> getDefaultDirectoryClassLoaderType(String classLoaderType) {
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
	private static List<String> retrieveDirectoryConfigNames(String configDir) throws IOException {
		List<String> configurationNames = new ArrayList<>();
		if(StringUtils.isEmpty(configDir))
			throw new IOException("property [configurations.directory] not set");

		Path directory = Paths.get(configDir);
		if(!Files.exists(directory))
			throw new IOException("failed to open configurations.directory ["+configDir+"]");
		if(!Files.isDirectory(directory))
			throw new IOException("configurations.directory ["+configDir+"] is not a valid directory");

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, Files::isDirectory)) {
			for (Path path : stream) {
				configurationNames.add(path.getFileName().toString());
			}
		}

		log.debug("found directory configurations {}", configurationNames);
		return configurationNames;
	}

	@Nonnull
	public static List<String> retrieveConfigNamesFromDatabase(ApplicationContext applicationContext) throws ConfigurationException {
		FixedQuerySender qs = SpringUtils.createBean(applicationContext);
		qs.setDatasourceName(IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME);
		qs.setQuery(DUMMY_SELECT_QUERY);
		qs.configure();
		try {
			qs.start();
			try (Connection conn = qs.getConnection()) {
				if(!qs.getDbmsSupport().isTablePresent(conn, "IBISCONFIG")) {
					log.warn("unable to load configurations from database, table [IBISCONFIG] is not present");
					return Collections.emptyList();
				}

				String query = "SELECT DISTINCT(NAME) FROM IBISCONFIG WHERE ACTIVECONFIG="+(qs.getDbmsSupport().getBooleanValue(true));
				try (PreparedStatement stmt = conn.prepareStatement(query); ResultSet rs = stmt.executeQuery()) {
					List<String> configurationNames = new ArrayList<>();
					while (rs.next()) {
						configurationNames.add(rs.getString(1));
					}

					log.debug("found database configurations {}", configurationNames);
					return Collections.unmodifiableList(configurationNames);
				}
			}
		} catch (LifecycleException | JdbcException | SQLException e) {
			throw new ConfigurationException(e);
		} finally {
			qs.stop();
		}
	}
}
